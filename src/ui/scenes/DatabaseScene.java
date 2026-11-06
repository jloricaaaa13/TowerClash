package ui.scenes;

import db.AccountStores;
import db.DatabaseConfig;
import java.awt.Color;
import java.awt.Graphics2D;
import ui.GameWindow;
import ui.pixel.Landscape;
import ui.pixel.Palette;
import ui.pixel.PixelArt;
import ui.pixel.PixelButton;
import ui.pixel.PixelFont;
import ui.pixel.PixelTextField;

/**
 * DATABASE SETTINGS (from the Login screen): which MySQL to save accounts in.
 *
 *   THIS COMPUTER  - the XAMPP MySQL on this computer (server = localhost)
 *   LAN SERVER     - type the IP address of the computer running XAMPP, so every player
 *                    in the same Wi-Fi/LAN shares the same accounts and leaderboard.
 *                    (How to prepare that computer: see LAN_SETUP.txt in the project folder.)
 */
public class DatabaseScene extends PixelScene {

    private static final int BOX_X = 30;
    private static final int BOX_Y = 24;
    private static final int BOX_W = 260;
    private static final int BOX_H = 130;
    private static final int LABEL_X = BOX_X + 14;
    private static final int FIELD_X = BOX_X + 96;
    private static final int FIELD_W = BOX_W - 110;
    private static final int ROW = 18;

    private final Landscape landscape = new Landscape(162, new int[]{8, 310}, new int[]{});
    private final PixelTextField hostField;
    private final PixelTextField portField;
    private final PixelTextField userField;
    private final PixelTextField passwordField;
    private String message = "";
    private Color messageColor = Palette.TEXT_DARK;

    public DatabaseScene(GameWindow window) {
        super(window);
        int y = BOX_Y + 22;
        hostField = addWidget(new PixelTextField(FIELD_X, y, FIELD_W, 40, false).withHint("LOCALHOST OR 192.168.X.X"));
        portField = addWidget(new PixelTextField(FIELD_X, y + ROW, FIELD_W, 5, false).withHint("3306"));
        userField = addWidget(new PixelTextField(FIELD_X, y + ROW * 2, FIELD_W, 32, false).withHint("ROOT"));
        passwordField = addWidget(new PixelTextField(FIELD_X, y + ROW * 3, FIELD_W, 40, true).withHint("(EMPTY FOR XAMPP)"));
        hostField.setOnSubmit(() -> setTypingField(portField));
        portField.setOnSubmit(() -> setTypingField(userField));
        userField.setOnSubmit(() -> setTypingField(passwordField));
        passwordField.setOnSubmit(this::connect);

        int by = BOX_Y + BOX_H - 21;
        addWidget(new PixelButton("CONNECT", BOX_X + 14, by, 70, 15, this::connect)).setTextColor(Palette.TEXT_ON);
        addWidget(new PixelButton("THIS COMPUTER", BOX_X + 88, by, 92, 15, this::useThisComputer));
        addWidget(new PixelButton("BACK", BOX_X + BOX_W - 74, by, 60, 15, window::showLogin));
    }

    @Override
    public void onShow() {
        DatabaseConfig config = DatabaseConfig.load();
        hostField.setText(config.getHost());
        portField.setText(String.valueOf(config.getPort()));
        userField.setText(config.getUser());
        passwordField.setText(config.getPassword());
        message = "";
        super.onShow();
        setTypingField(hostField);
    }

    /** Saves the settings and tries to connect right away. */
    private void connect() {
        DatabaseConfig config = new DatabaseConfig();
        config.setHost(hostField.getText());
        try {
            config.setPort(Integer.parseInt(portField.getText().trim()));
        } catch (NumberFormatException e) {
            config.setPort(DatabaseConfig.DEFAULT_PORT);
        }
        config.setUser(userField.getText());
        config.setPassword(passwordField.getText());
        config.save();

        window.reconnect();
        if (window.getAccounts().isDatabase()) {
            message = "CONNECTED! SAVING TO: " + window.getAccounts().getName();
            messageColor = new Color(0x15803D);
        } else {
            message = AccountStores.getLastProblem() + " - USING THIS COMPUTER";
            messageColor = Palette.HP_LOW;
        }
    }

    /** Back to the normal setup: the XAMPP MySQL on this computer. */
    private void useThisComputer() {
        hostField.setText(DatabaseConfig.DEFAULT_HOST);
        portField.setText(String.valueOf(DatabaseConfig.DEFAULT_PORT));
        userField.setText(DatabaseConfig.DEFAULT_USER);
        passwordField.setText("");
        connect();
    }

    @Override
    protected void onEscape() {
        window.showLogin();
    }

    @Override
    protected void paintScene(Graphics2D g) {
        landscape.draw(g, tick);
        PixelFont.drawCentered(g, "DATABASE SETTINGS", W / 2, 6, 2, Palette.TEXT_WHITE, Palette.TEXT_DARK);
        PixelArt.board(g, BOX_X, BOX_Y, BOX_W, BOX_H);
        PixelFont.drawCentered(g, "WHERE ARE ACCOUNTS AND THE LEADERBOARD SAVED?", W / 2, BOX_Y + 7, 1,
                Palette.TEXT_GOLD, Palette.TEXT_DARK);
        String[] labels = {"SERVER IP", "PORT", "MYSQL USER", "PASSWORD"};
        for (int i = 0; i < labels.length; i++) {
            PixelFont.draw(g, labels[i], LABEL_X, BOX_Y + 25 + ROW * i, 1, Palette.TEXT_DARK);
        }
        if (!message.isEmpty()) {
            PixelFont.drawCentered(g, message, W / 2, BOX_Y + 96, 1, messageColor, null);
        } else {
            PixelFont.drawCentered(g, "LAN: TYPE THE IP OF THE COMPUTER RUNNING XAMPP", W / 2, BOX_Y + 96, 1,
                    Palette.WOOD_OUTLINE, null);
        }
        boolean db = window.getAccounts().isDatabase();
        PixelFont.drawCentered(g, "NOW SAVING TO: " + window.getAccounts().getName(), W / 2, 160, 1,
                db ? Palette.TEXT_ON : Palette.TEXT_OFF, Palette.TEXT_DARK);
    }
}
