package app;

import javax.swing.SwingUtilities;
import ui.GameWindow;

/** Program entry point. Set this as the Main Class in NetBeans. */
public class Main {

    public static void main(String[] args) {
        // Swing screens must be created on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> new GameWindow().setVisible(true));
    }
}
