/**
 * Модель игрового объекта: сегмент змейки, еда или бонус.
 * Хранит координаты в клетках сетки и свой тип.
 * Все поля приватные, доступ — через геттеры/сеттеры.
 */
public class GameObject implements GameConstants {

    /** Тип игрового объекта. */
    public enum Type {
        SNAKE_HEAD,
        SNAKE_BODY,
        FOOD,
        BONUS
    }

    private int x;
    private int y;
    private Type type;

    public GameObject(int x, int y, Type type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
}
