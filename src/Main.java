import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Точка входа приложения «Neon Snake».
 * Создаёт окно JFrame, связывает панель и игровой движок (через ссылки)
 * и запускает игровой поток.
 */
public class Main implements GameConstants {

    public static void main(String[] args) {
        // GUI создаём в потоке диспетчеризации событий Swing.
        SwingUtilities.invokeLater(Main::createAndShow);
    }

    private static void createAndShow() {
        GamePanel panel = new GamePanel();
        GameEngine engine = new GameEngine(panel);
        panel.setEngine(engine);

        JFrame frame = new JFrame(TITLE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        panel.requestFocusInWindow();

        engine.start();
    }
}
