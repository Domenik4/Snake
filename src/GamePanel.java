import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JPanel;

/**
 * Панель отрисовки игры «Neon Snake» в тёмной неоновой теме.
 * Хранит ссылку на GameEngine (без статики) и обрабатывает ввод
 * с клавиатуры (KeyListener) и мыши (MouseListener).
 */
@SuppressWarnings("this-escape")
public class GamePanel extends JPanel implements GameConstants, KeyListener, MouseListener {

    private static final long serialVersionUID = 1L;

    private transient GameEngine engine;

    private final Font fontLarge = new Font("Monospaced", Font.BOLD, FONT_SIZE_LARGE);
    private final Font fontMedium = new Font("Monospaced", Font.BOLD, FONT_SIZE_MEDIUM);
    private final Font fontSmall = new Font("Monospaced", Font.PLAIN, FONT_SIZE_SMALL);

    public GamePanel() {
        setPreferredSize(new Dimension(GameConstants.WIDTH, GameConstants.HEIGHT));
        setBackground(COLOR_BG);
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);
    }

    public void setEngine(GameEngine engine) {
        this.engine = engine;
    }

    // ------------------------------------------------------------------
    // Отрисовка
    // ------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawBackground(g2);
        drawGrid(g2);

        if (engine == null) {
            return;
        }

        drawFood(g2);
        drawBonus(g2);
        drawSnake(g2);
        drawGhost(g2);
        drawHud(g2);
        drawOverlays(g2);
    }

    private void drawBackground(Graphics2D g2) {
        g2.setPaint(new GradientPaint(0, 0, COLOR_BG, 0, GameConstants.HEIGHT, COLOR_BG_DARK));
        g2.fillRect(0, 0, getWidth(), getHeight());
    }

    private void drawGrid(Graphics2D g2) {
        g2.setColor(COLOR_GRID);
        int fieldBottom = HUD_HEIGHT + ROWS * CELL_SIZE;
        for (int c = 0; c <= COLS; c++) {
            int x = c * CELL_SIZE;
            g2.drawLine(x, HUD_HEIGHT, x, fieldBottom);
        }
        for (int r = 0; r <= ROWS; r++) {
            int y = HUD_HEIGHT + r * CELL_SIZE;
            g2.drawLine(0, y, GameConstants.WIDTH, y);
        }
        // Неоновая рамка вокруг игрового поля.
        g2.setColor(withAlpha(COLOR_SNAKE, 70));
        g2.drawRect(1, HUD_HEIGHT + 1, GameConstants.WIDTH - 3, ROWS * CELL_SIZE - 3);
    }

    private void drawSnake(Graphics2D g2) {
        int index = 0;
        for (GameObject segment : engine.getSnake()) {
            int px = segment.getX() * CELL_SIZE;
            int py = HUD_HEIGHT + segment.getY() * CELL_SIZE;
            boolean isHead = (index == 0);
            int inset = isHead ? 1 : 2;

            // Свечение вокруг сегмента.
            g2.setColor(withAlpha(COLOR_SNAKE, isHead ? 90 : 40));
            g2.fillRoundRect(px - 2, py - 2, CELL_SIZE + 4, CELL_SIZE + 4,
                    CORNER + 4, CORNER + 4);

            // Тело сегмента с градиентом.
            Color top = isHead ? COLOR_SNAKE_HEAD : COLOR_SNAKE;
            g2.setPaint(new GradientPaint(px, py, top,
                    px + CELL_SIZE, py + CELL_SIZE, COLOR_SNAKE_DARK));
            g2.fillRoundRect(px + inset, py + inset,
                    CELL_SIZE - inset * 2, CELL_SIZE - inset * 2, CORNER, CORNER);

            if (isHead) {
                drawEyes(g2, px, py);
            }
            index++;
        }
    }

    /** Глаза головы, смещённые в сторону движения. */
    private void drawEyes(Graphics2D g2, int px, int py) {
        int eye = CELL_SIZE / 6;
        int cx = px + CELL_SIZE / 2;
        int cy = py + CELL_SIZE / 2;
        int dx = engine.getDirectionX();
        int dy = engine.getDirectionY();

        int ox = dx * CELL_SIZE / 5;
        int oy = dy * CELL_SIZE / 5;
        int sx = -dy * CELL_SIZE / 6; // перпендикуляр к направлению
        int sy = dx * CELL_SIZE / 6;

        g2.setColor(COLOR_BG);
        g2.fillOval(cx + ox + sx - eye, cy + oy + sy - eye, eye * 2, eye * 2);
        g2.fillOval(cx + ox - sx - eye, cy + oy - sy - eye, eye * 2, eye * 2);
    }

    private void drawFood(Graphics2D g2) {
        GameObject food = engine.getFood();
        if (food == null) {
            return;
        }
        double pulse = (Math.sin(System.currentTimeMillis() / 140.0) + 1) / 2.0; // 0..1
        int full = CELL_SIZE;
        int cx = food.getX() * CELL_SIZE + full / 2;
        int cy = HUD_HEIGHT + food.getY() * CELL_SIZE + full / 2;

        // Пульсирующее свечение.
        int glow = (int) (full * (0.9 + 0.35 * pulse));
        g2.setColor(withAlpha(COLOR_FOOD, 55));
        g2.fillOval(cx - glow / 2, cy - glow / 2, glow, glow);
        g2.setColor(withAlpha(COLOR_FOOD, 120));
        g2.fillOval(cx - full / 2, cy - full / 2, full, full);

        // Ядро еды.
        int core = (int) (full * (0.42 + 0.14 * pulse));
        g2.setColor(COLOR_FOOD);
        g2.fillOval(cx - core / 2, cy - core / 2, core, core);
        g2.setColor(withAlpha(Color.WHITE, 200));
        g2.fillOval(cx - core / 6, cy - core / 6, Math.max(2, core / 4), Math.max(2, core / 4));
    }

    /** Золотой бонус — «Золотое яблоко»: золотой ромб со свечением. */
    private void drawBonus(Graphics2D g2) {
        GameObject bonus = engine.getBonus();
        if (bonus == null) {
            return;
        }
        double pulse = (Math.sin(System.currentTimeMillis() / 120.0) + 1) / 2.0;
        int full = CELL_SIZE;
        int cx = bonus.getX() * CELL_SIZE + full / 2;
        int cy = HUD_HEIGHT + bonus.getY() * CELL_SIZE + full / 2;
        int size = (int) (full * (0.55 + 0.15 * pulse));

        g2.setColor(withAlpha(COLOR_BONUS, 70));
        g2.fillOval(cx - size, cy - size, size * 2, size * 2);

        int half = size / 2;
        int[] xs = {cx, cx + half, cx, cx - half};
        int[] ys = {cy - half, cy, cy + half, cy};
        g2.setColor(COLOR_BONUS);
        g2.fillPolygon(xs, ys, xs.length);
        g2.setColor(withAlpha(Color.WHITE, 220));
        g2.drawPolygon(xs, ys, xs.length);
    }

    /** Призрак-указатель (управление мышью) — голубой прицел. */
    private void drawGhost(Graphics2D g2) {
        if (!engine.isTargetActive()) {
            return;
        }
        int px = engine.getGhostX() * CELL_SIZE;
        int py = HUD_HEIGHT + engine.getGhostY() * CELL_SIZE;
        g2.setColor(withAlpha(COLOR_GHOST, 200));
        g2.drawRect(px + 2, py + 2, CELL_SIZE - 4, CELL_SIZE - 4);
        g2.drawLine(px + CELL_SIZE / 2, py + 2, px + CELL_SIZE / 2, py + CELL_SIZE - 2);
        g2.drawLine(px + 2, py + CELL_SIZE / 2, px + CELL_SIZE - 2, py + CELL_SIZE / 2);
    }

    private void drawHud(Graphics2D g2) {
        g2.setColor(withAlpha(COLOR_PANEL, 220));
        g2.fillRect(0, 0, GameConstants.WIDTH, HUD_HEIGHT);
        g2.setColor(withAlpha(COLOR_ACCENT, 130));
        g2.drawLine(0, HUD_HEIGHT - 1, GameConstants.WIDTH, HUD_HEIGHT - 1);

        g2.setFont(fontMedium);
        g2.setColor(COLOR_TEXT);
        String scoreText = "СЧЁТ: " + engine.getScore();
        g2.drawString(scoreText, PADDING, 30);

        String levelText = "УРОВЕНЬ " + engine.getLevel();
        int levelWidth = g2.getFontMetrics().stringWidth(levelText);
        g2.setColor(COLOR_ACCENT);
        g2.drawString(levelText, (GameConstants.WIDTH - levelWidth) / 2, 30);

        g2.setColor(COLOR_TEXT_DIM);
        String bestText = "ЛУЧШИЙ: " + engine.getBestScore();
        int bestWidth = g2.getFontMetrics().stringWidth(bestText);
        g2.drawString(bestText, GameConstants.WIDTH - bestWidth - PADDING, 30);

        if (engine.isSpeedBoostActive()) {
            g2.setColor(COLOR_BONUS);
            g2.setFont(fontSmall);
            g2.drawString(">>> УСКОРЕНИЕ <<<", PADDING, 50);
        }

        g2.setFont(fontSmall);
        g2.setColor(COLOR_TEXT_DIM);
        String hint = "Стрелки: движение  |  SPACE: новая игра  |  P: пауза  |  ESC: выход  |  Мышь: цель";
        int hintWidth = g2.getFontMetrics().stringWidth(hint);
        g2.drawString(hint, (GameConstants.WIDTH - hintWidth) / 2, 52);
    }

    private void drawOverlays(Graphics2D g2) {
        int fieldTop = HUD_HEIGHT;
        int fieldHeight = ROWS * CELL_SIZE;

        if (engine.isPaused() && !engine.isGameOver()) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, fieldTop, GameConstants.WIDTH, fieldHeight);
            drawCentered(g2, "ПАУЗА", fieldTop + fieldHeight / 2 - 10, fontLarge, COLOR_ACCENT);
            drawCentered(g2, "P — продолжить", fieldTop + fieldHeight / 2 + 24, fontMedium, COLOR_TEXT);
        }

        if (engine.isGameOver()) {
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(0, fieldTop, GameConstants.WIDTH, fieldHeight);
            drawCentered(g2, "ИГРА ОКОНЧЕНА", fieldTop + fieldHeight / 2 - 30, fontLarge, COLOR_FOOD);
            drawCentered(g2, "Ваш счёт: " + engine.getScore(),
                    fieldTop + fieldHeight / 2 + 4, fontMedium, COLOR_TEXT);
            drawCentered(g2, "SPACE — новая игра",
                    fieldTop + fieldHeight / 2 + 34, fontSmall, COLOR_TEXT_DIM);
        }
    }

    private void drawCentered(Graphics2D g2, String text, int y, Font font, Color color) {
        g2.setFont(font);
        g2.setColor(color);
        int width = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, (GameConstants.WIDTH - width) / 2, y);
    }

    private static Color withAlpha(Color c, int alpha) {
        int a = Math.max(0, Math.min(255, alpha));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    // ------------------------------------------------------------------
    // KeyListener
    // ------------------------------------------------------------------

    @Override
    public void keyPressed(KeyEvent e) {
        if (engine == null) {
            return;
        }
        switch (e.getKeyCode()) {
            case KEY_UP:
                engine.changeDirection(0, -1);
                break;
            case KEY_DOWN:
                engine.changeDirection(0, 1);
                break;
            case KEY_LEFT:
                engine.changeDirection(-1, 0);
                break;
            case KEY_RIGHT:
                engine.changeDirection(1, 0);
                break;
            case KEY_NEW_GAME:
                engine.resetGame();
                break;
            case KEY_PAUSE:
                engine.togglePause();
                break;
            case KEY_EXIT:
                engine.stop();
                System.exit(0);
                break;
            default:
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // не используется
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // не используется
    }

    // ------------------------------------------------------------------
    // MouseListener — управление призраком-указателем
    // ------------------------------------------------------------------

    @Override
    public void mousePressed(MouseEvent e) {
        if (engine == null) {
            return;
        }
        requestFocusInWindow();
        int col = e.getX() / CELL_SIZE;
        int row = (e.getY() - HUD_HEIGHT) / CELL_SIZE;
        if (col >= 0 && col < COLS && row >= 0 && row < ROWS) {
            engine.setMouseTarget(col, row);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // обработка в mousePressed
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // не используется
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // не используется
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // не используется
    }
}
