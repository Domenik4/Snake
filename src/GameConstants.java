import java.awt.Color;
import java.awt.event.KeyEvent;

/**
 * Интерфейс со всеми константами игры «Neon Snake».
 * Реализуется игровыми классами, чтобы использовать константы без префикса
 * и полностью исключить «магические числа» из кода.
 */
public interface GameConstants {

    // ------------------------------------------------------------------
    // Название и размеры окна / сетки
    // ------------------------------------------------------------------
    String TITLE = "NEON SNAKE";

    int CELL_SIZE = 28;                     // размер одной клетки в пикселях
    int COLS = 22;                          // количество столбцов игрового поля
    int ROWS = 18;                          // количество строк игрового поля
    int HUD_HEIGHT = 64;                    // высота верхней информационной панели

    // WIDTH и HEIGHT вычисляются из размеров сетки — магических чисел нет.
    int WIDTH = COLS * CELL_SIZE;
    int HEIGHT = ROWS * CELL_SIZE + HUD_HEIGHT;

    // ------------------------------------------------------------------
    // Стартовые параметры змейки
    // ------------------------------------------------------------------
    int START_LENGTH = 4;
    int START_X = COLS / 2;
    int START_Y = ROWS / 2;

    // ------------------------------------------------------------------
    // Скорости и тайминги
    // ------------------------------------------------------------------
    int DELAY = 115;                        // базовая задержка игрового цикла, мс
    int MIN_DELAY = 55;                     // минимальная задержка (максимальная скорость)
    int SPEED_STEP = 9;                     // на сколько мс ускоряется каждый уровень
    int LEVEL_UP_SCORE = 50;                // очков на повышение уровня
    int SPEED_BOOST_MS = 3500;              // длительность ускорения от бонуса, мс
    int BONUS_DURATION_MS = 5000;           // время жизни бонуса на поле, мс
    int BONUS_SPAWN_CHANCE = 12;            // шанс появления бонуса, %

    // ------------------------------------------------------------------
    // Начисление очков
    // ------------------------------------------------------------------
    int SCORE_FOOD = 10;
    int SCORE_BONUS = 50;

    // ------------------------------------------------------------------
    // Цвета (тёмная неоновая тема)
    // ------------------------------------------------------------------
    Color COLOR_BG = new Color(8, 10, 18);
    Color COLOR_BG_DARK = new Color(2, 3, 8);
    Color COLOR_GRID = new Color(26, 38, 48);
    Color COLOR_PANEL = new Color(14, 20, 30);
    Color COLOR_SNAKE = new Color(57, 255, 20);
    Color COLOR_SNAKE_HEAD = new Color(170, 255, 140);
    Color COLOR_SNAKE_DARK = new Color(20, 120, 30);
    Color COLOR_FOOD = new Color(255, 45, 85);
    Color COLOR_BONUS = new Color(255, 205, 20);
    Color COLOR_GHOST = new Color(0, 230, 255);
    Color COLOR_ACCENT = new Color(0, 230, 255);
    Color COLOR_TEXT = new Color(220, 240, 255);
    Color COLOR_TEXT_DIM = new Color(140, 165, 190);

    // ------------------------------------------------------------------
    // Клавиши управления
    // ------------------------------------------------------------------
    int KEY_UP = KeyEvent.VK_UP;
    int KEY_DOWN = KeyEvent.VK_DOWN;
    int KEY_LEFT = KeyEvent.VK_LEFT;
    int KEY_RIGHT = KeyEvent.VK_RIGHT;
    int KEY_NEW_GAME = KeyEvent.VK_SPACE;   // новая игра / рестарт
    int KEY_PAUSE = KeyEvent.VK_P;          // пауза / продолжить
    int KEY_EXIT = KeyEvent.VK_ESCAPE;      // выход

    // ------------------------------------------------------------------
    // Шрифты и геометрия оформления
    // ------------------------------------------------------------------
    int FONT_SIZE_LARGE = 30;
    int FONT_SIZE_MEDIUM = 18;
    int FONT_SIZE_SMALL = 12;
    int PADDING = 14;                       // внутренние отступы
    int CORNER = 9;                         // радиус скругления сегментов змейки
}
