package ui.scenes;

import db.AccountException;
import java.awt.Color;
import java.awt.Graphics2D;
import model.Department;
import ui.GameWindow;
import ui.pixel.Landscape;
import ui.pixel.Palette;
import ui.pixel.PixelArt;
import ui.pixel.PixelButton;
import ui.pixel.PixelFont;
import ui.pixel.PixelTextField;

/**
 * The first screen: log in with a username and password.
 * New players press REGISTER to make an account. Every account has its own saved progress.
 * The line at the bottom shows WHERE accounts are saved (MySQL database or this computer);
 * DATABASE opens the settings (for example, a shared MySQL server on the LAN).
 * FORGOT PASSWORD? sends a reset code to the account's email.
 */
public class LoginScene extends PixelScene {

    private static final int GROUND_Y = 150;
    private static final int BOX_X = 72;
    private static final int BOX_Y = 40;
    private static final int BOX_W = 176;
    private static final int BOX_H = 100;

    private final Landscape landscape = new Landscape(GROUND_Y,
            new int[]{12, 30, 286, 304}, new int[]{21, 295});
    private final PixelTextField usernameField;
    private final PixelTextField passwordField;
    private String message = "";
    private Color messageColor = Palette.TEXT_OFF;

    public LoginScene(GameWindow window) {
        super(window);
        int fieldX = BOX_X + 12;
        int fieldW = BOX_W - 24;
        usernameField = addWidget(new PixelTextField(fieldX, BOX_Y + 27, fieldW, 16, false)
                .withHint("YOUR USERNAME"));
        passwordField = addWidget(new PixelTextField(fieldX, BOX_Y + 53, fieldW, 32, true)
                .withHint("YOUR PASSWORD"));
        usernameField.setOnSubmit(() -> setTypingField(passwordField)); // ENTER = go to the password box
        passwordField.setOnSubmit(this::login);                          // ENTER = log in

        addWidget(new PixelButton("LOG IN", fieldX, BOX_Y + 80, 72, 15, this::login));
        addWidget(new PixelButton("REGISTER", fieldX + fieldW - 72, BOX_Y + 80, 72, 15, window::showRegister));
        addWidget(new PixelButton("EXIT", 8, 162, 50, 14, this::confirmExit));
        addWidget(new PixelButton("DATABASE", W - 76, 162, 68, 14, window::showDatabaseSettings));
        addWidget(new PixelButton("FORGOT PASSWORD?", (W - 104) / 2, 162, 104, 14, window::showForgotPassword));
    }

    @Override
    public void onShow() {
        usernameField.setText(window.getLastUsername());
        passwordField.clear();
        message = "";
        super.onShow();
        setTypingField(usernameField.getText().isEmpty() ? usernameField : passwordField);
    }

    private void login() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        if (username.isEmpty() || password.isEmpty()) {
            showMessage("ENTER YOUR USERNAME AND PASSWORD", Palette.TEXT_OFF);
            return;
        }
        try {
            window.login(username, password); // goes to the Dashboard
        } catch (AccountException e) {
            showMessage(e.getMessage(), Palette.TEXT_OFF);
            passwordField.clear();
            setTypingField(passwordField);
        }
    }

    private void showMessage(String text, Color color) {
        message = text;
        messageColor = color;
    }

    private void confirmExit() {
        showDialog("QUIT GAME?", new String[]{"ARE YOU SURE YOU WANT TO QUIT?"}, "YES", () -> System.exit(0), "NO");
    }

    @Override
    protected void onEscape() {
        confirmExit();
    }

    @Override
    protected void paintScene(Graphics2D g) {
        landscape.draw(g, tick);
        PixelArt.tower(g, 26, GROUND_Y, 16, 34, Department.HOSPITALITY.getColor(), tick);
        PixelArt.tower(g, 46, GROUND_Y, 18, 44, Department.EDUCATION.getColor(), tick + 5);
        PixelArt.tower(g, 268, GROUND_Y, 18, 38, Department.COMPUTER_SCIENCE.getColor(), tick + 3);

        // Title
        PixelFont.drawCentered(g, "DEPARTMENT", W / 2, 5, 1, Palette.TEXT_GOLD, Palette.TEXT_DARK);
        String title = "TOWER CLASH";
        int tx = (W - PixelFont.width(title, 2)) / 2;
        PixelFont.draw(g, title, tx + 2, 17, 2, new Color(0, 0, 0, 90)); // drop shadow
        PixelFont.drawOutlined(g, title, tx, 15, 2, Palette.TEXT_WHITE, Palette.TEXT_DARK);

        // Login box
        PixelArt.board(g, BOX_X, BOX_Y, BOX_W, BOX_H);
        PixelFont.drawCentered(g, "LOG IN TO PLAY", W / 2, BOX_Y + 6, 1, Palette.TEXT_GOLD, Palette.TEXT_DARK);
        PixelFont.draw(g, "USERNAME", BOX_X + 12, BOX_Y + 18, 1, Palette.TEXT_DARK);
        PixelFont.draw(g, "PASSWORD", BOX_X + 12, BOX_Y + 44, 1, Palette.TEXT_DARK);
        if (!message.isEmpty()) {
            PixelFont.drawCentered(g, message, W / 2, BOX_Y + 70, 1, messageColor, Palette.TEXT_DARK);
        }

        // Where accounts are saved
        boolean db = window.getAccounts().isDatabase();
        String where = "SAVING TO: " + window.getAccounts().getName();
        PixelFont.drawCentered(g, where, W / 2, 149, 1, db ? Palette.TEXT_ON : Palette.TEXT_OFF, Palette.TEXT_DARK);
    }
}
