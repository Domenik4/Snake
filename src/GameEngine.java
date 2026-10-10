import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * Игровая логика «Neon Snake».
 * Работает в отдельном потоке (implements Runnable), управляет состоянием игры
 * и общается с панелью отрисовки через ссылку (без статики).
 */
public class GameEngine implements GameConstants, Runnable {

    private final GamePanel panel;              // ссылка на панель для repaint()
    private final Random random = new Random();

    private final Deque<GameObject> snake = new ArrayDeque<>();

    // Монитор для доступа к изменяемому состоянию из игрового потока
    // и из потока отрисовки (EDT) — устраняет ConcurrentModificationException.
    private final Object lock = new Object();

    // volatile — потому что поля читаются/пишутся из разных потоков
    private volatile boolean running;
    private volatile boolean paused;
    private volatile boolean gameOver;

    private Thread gameThread;

    private GameObject food;
    private GameObject bonus;
    private long bonusSpawnTime;

    private int directionX;
    private int directionY;

    private int score;
    private int level;
    private int bestScore;

    // «Призрак-указатель», к которому стремится голова змейки (управление мышью)
    private int ghostX;
    private int ghostY;
    private int targetX;
    private int targetY;
    private boolean targetActive;

    private long speedBoostUntil;

    public GameEngine(GamePanel panel) {
        this.panel = panel;
        resetState();
    }

    // ------------------------------------------------------------------
    // Управление потоком
    // ------------------------------------------------------------------

    /** Запускает игровой цикл в отдельном потоке. */
    public void start() {
        if (running) {
            return;
        }
        running = true;
        gameThread = new Thread(this, "NeonSnake-Engine");
        gameThread.start();
    }

    /** Корректно останавливает игровой поток. */
    public void stop() {
        running = false;
        if (gameThread != null) {
            gameThread.interrupt();
        }
    }

    @Override
    public void run() {
        while (running) {
            if (!paused && !gameOver) {
                update();
            }
            if (panel != null) {
                panel.repaint();
            }
            try {
                // Задержка игрового цикла регулирует скорость игры.
                Thread.sleep(currentDelay());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /** Текущая задержка с учётом уровня и активного ускорения от бонуса. */
    private int currentDelay() {
        int delay = DELAY - (level - 1) * SPEED_STEP;
        if (isSpeedBoostActive()) {
            delay -= SPEED_STEP * 2;
        }
        return Math.max(MIN_DELAY, delay);
    }

    // ------------------------------------------------------------------
    // Игровой цикл
    // ------------------------------------------------------------------

    /** Один шаг игровой логики. Выполняется под монитором. */
    private void update() {
        synchronized (lock) {
            expireBonus();
            updateGhost();
            applyGhostSteering();
            moveSnake();
        }
    }

    private void moveSnake() {
        GameObject head = snake.peekFirst();
        if (head == null) {
            return;
        }

        int newX = head.getX() + directionX;
        int newY = head.getY() + directionY;

        // Столкновение со стеной — конец игры.
        if (newX < 0 || newX >= COLS || newY < 0 || newY >= ROWS) {
            endGame();
            return;
        }

        boolean eatFood = food != null && newX == food.getX() && newY == food.getY();
        boolean eatBonus = bonus != null && newX == bonus.getX() && newY == bonus.getY();

        // Столкновение с собственным телом. Хвост в этом такте сдвинется,
        // поэтому его клетка свободна — если только змейка не растёт (не ест еду).
        int index = 0;
        int size = snake.size();
        for (GameObject segment : snake) {
            boolean isTail = (index == size - 1);
            boolean tailWillMove = isTail && !eatFood;
            if (!tailWillMove && segment.getX() == newX && segment.getY() == newY) {
                endGame();
                return;
            }
            index++;
        }

        // Бывшая голова становится телом, спереди добавляется новая голова.
        head.setType(GameObject.Type.SNAKE_BODY);
        snake.addFirst(new GameObject(newX, newY, GameObject.Type.SNAKE_HEAD));

        if (eatFood) {
            score += SCORE_FOOD;
            generateFood();      // еды всегда должна быть на поле
            trySpawnBonus();     // бонус появляется случайно
        } else {
            snake.removeLast();  // обычное движение: хвост уходит
        }

        if (eatBonus) {
            score += SCORE_BONUS;
            bonus = null;
            speedBoostUntil = System.currentTimeMillis() + SPEED_BOOST_MS;
        }

        updateLevel();
        if (score > bestScore) {
            bestScore = score;
        }
    }

    // ------------------------------------------------------------------
    // Управление мышью: призрак-указатель
    // ------------------------------------------------------------------

    private void updateGhost() {
        if (!targetActive) {
            return;
        }
        // Двигаем призрак по одной клетке к цели (сначала по X, затем по Y).
        if (ghostX < targetX) {
            ghostX++;
        } else if (ghostX > targetX) {
            ghostX--;
        } else if (ghostY < targetY) {
            ghostY++;
        } else if (ghostY > targetY) {
            ghostY--;
        }
        if (ghostX == targetX && ghostY == targetY) {
            targetActive = false; // цель достигнута
        }
    }

    /** Голова змейки плавно следует за призраком-указателем. */
    private void applyGhostSteering() {
        if (!targetActive) {
            return;
        }
        GameObject head = snake.peekFirst();
        if (head == null) {
            return;
        }
        int headX = head.getX();
        int headY = head.getY();
        if (headX == ghostX && headY == ghostY) {
            return;
        }
        int dx = Integer.signum(ghostX - headX);
        int dy = Integer.signum(ghostY - headY);

        // Сначала сокращаем большее расстояние, при запрете — вторую ось.
        if (Math.abs(ghostX - headX) >= Math.abs(ghostY - headY)) {
            if (dx != 0 && trySetDirection(dx, 0)) {
                return;
            }
            if (dy != 0) {
                trySetDirection(0, dy);
            }
        } else {
            if (dy != 0 && trySetDirection(0, dy)) {
                return;
            }
            if (dx != 0) {
                trySetDirection(dx, 0);
            }
        }
    }

    // ------------------------------------------------------------------
    // Генерация объектов
    // ------------------------------------------------------------------

    /**
     * Ищет случайную свободную клетку и кладёт туда еду.
     * Если свободных клеток не осталось (змейка заняла всё поле) —
     * оставляем еду в прежней позиции, чтобы не зациклиться.
     */
    private void generateFood() {
        int maxAttempts = COLS * ROWS;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int x = random.nextInt(COLS);
            int y = random.nextInt(ROWS);
            if (!isOccupied(x, y)) {
                food = new GameObject(x, y, GameObject.Type.FOOD);
                return;
            }
        }
    }

    /** С некоторой вероятностью ставит бонус на свободную клетку. */
    private void trySpawnBonus() {
        if (bonus != null) {
            return;
        }
        if (random.nextInt(100) >= BONUS_SPAWN_CHANCE) {
            return;
        }
        int maxAttempts = COLS * ROWS;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int x = random.nextInt(COLS);
            int y = random.nextInt(ROWS);
            if (!isOccupied(x, y)) {
                bonus = new GameObject(x, y, GameObject.Type.BONUS);
                bonusSpawnTime = System.currentTimeMillis();
                return;
            }
        }
    }

    /** Снимает бонус с поля, если истекло время его жизни. */
    private void expireBonus() {
        if (bonus != null && System.currentTimeMillis() - bonusSpawnTime > BONUS_DURATION_MS) {
            bonus = null;
        }
    }

    /** Проверяет, занята ли клетка змейкой, едой или бонусом. */
    private boolean isOccupied(int x, int y) {
        for (GameObject segment : snake) {
            if (segment.getX() == x && segment.getY() == y) {
                return true;
            }
        }
        if (food != null && food.getX() == x && food.getY() == y) {
            return true;
        }
        if (bonus != null && bonus.getX() == x && bonus.getY() == y) {
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Вспомогательные операции
    // ------------------------------------------------------------------

    /**
     * Меняет направление движения.
     * Запрещает разворот на 180 градусов (змейка не может войти в себя).
     */
    public boolean trySetDirection(int dx, int dy) {
        synchronized (lock) {
            if (dx == 0 && dy == 0) {
                return false;
            }
            if (dx == -directionX && dy == -directionY) {
                return false; // разворот запрещён
            }
            directionX = dx;
            directionY = dy;
            return true;
        }
    }

    /** Обновляет уровень по текущему счёту. */
    private void updateLevel() {
        level = 1 + score / LEVEL_UP_SCORE;
    }

    private void endGame() {
        gameOver = true;
        if (score > bestScore) {
            bestScore = score;
        }
    }

    /** Полностью сбрасывает состояние игры. */
    public void resetGame() {
        synchronized (lock) {
            resetState();
        }
        if (panel != null) {
            panel.repaint();
        }
    }

    /** Приватная инициализация состояния (без побочных эффектов). */
    private void resetState() {
        snake.clear();
        for (int i = 0; i < START_LENGTH; i++) {
            GameObject.Type type = (i == 0) ? GameObject.Type.SNAKE_HEAD : GameObject.Type.SNAKE_BODY;
            snake.addLast(new GameObject(START_X - i, START_Y, type));
        }

        directionX = 1;
        directionY = 0;
        score = 0;
        level = 1;
        gameOver = false;
        paused = false;
        bonus = null;
        speedBoostUntil = 0;

        GameObject head = snake.peekFirst();
        ghostX = head.getX();
        ghostY = head.getY();
        targetX = ghostX;
        targetY = ghostY;
        targetActive = false;

        generateFood();
    }

    // ------------------------------------------------------------------
    // Публичное API для ввода и отрисовки
    // ------------------------------------------------------------------

    /** Смена направления стрелками (управление клавиатурой). */
    public void changeDirection(int dx, int dy) {
        synchronized (lock) {
            if (gameOver) {
                return;
            }
            trySetDirection(dx, dy);
            targetActive = false; // клавиатура вытесняет управление мышью
        }
    }

    /** Клик мышью по полю задаёт цель для призрака-указателя. */
    public void setMouseTarget(int x, int y) {
        synchronized (lock) {
            if (gameOver || snake.isEmpty()) {
                return;
            }
            GameObject head = snake.peekFirst();
            targetX = x;
            targetY = y;
            ghostX = head.getX();
            ghostY = head.getY();
            targetActive = true;
        }
    }

    /** Пауза / продолжение игры. */
    public void togglePause() {
        synchronized (lock) {
            if (gameOver) {
                return;
            }
            paused = !paused;
        }
        if (panel != null) {
            panel.repaint();
        }
    }

    public boolean isSpeedBoostActive() {
        return System.currentTimeMillis() < speedBoostUntil;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public int getScore() {
        return score;
    }

    public int getLevel() {
        return level;
    }

    public int getBestScore() {
        return bestScore;
    }

    public int getDirectionX() {
        return directionX;
    }

    public int getDirectionY() {
        return directionY;
    }

    public int getGhostX() {
        return ghostX;
    }

    public int getGhostY() {
        return ghostY;
    }

    public boolean isTargetActive() {
        return targetActive;
    }

    public GameObject getFood() {
        synchronized (lock) {
            return food;
        }
    }

    public GameObject getBonus() {
        synchronized (lock) {
            return bonus;
        }
    }

    /**
     * Возвращает снимок сегментов змейки. Копия делается под монитором,
     * поэтому поток отрисовки не конфликтует с игровым потоком.
     */
    public List<GameObject> getSnake() {
        synchronized (lock) {
            return new ArrayList<>(snake);
        }
    }
}
