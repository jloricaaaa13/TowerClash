package ui.scenes;

import db.AccountException;
import db.PasswordReset;
import db.Validator;
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
 * FORGOT PASSWORD (from the Login screen), in two steps:
 *   1. Type your username or email -> NEXT. Your security question is shown.
 *   2. Type the answer and a new password (twice) -> RESET PASSWORD.
 * No email or internet needed. After 5 wrong answers, resetting is blocked for 15 minutes.
 */
public class ForgotPasswordScene extends PixelScene {

    private static final int BOX_X = 30;
    private static final int BOX_Y = 24;
    private static final int BOX_W = 260;
    private static final int BOX_H = 130;
    private static final int LABEL_X = BOX_X + 14;
    private static final int FIELD_X = BOX_X + 110;
    private static final int FIELD_W = BOX_W - 124;
    private static final int ROW = 19;

    private final Landscape landscape = new Landscape(162, new int[]{8, 310}, new int[]{});

    // Step 1
    private final PixelTextField accountField;
    private final PixelButton nextButton;
    // Step 2
    private final PixelTextField answerField;
    private final PixelTextField newField;
    private final PixelTextField confirmField;
    private final PixelButton resetButton;

    private PasswordReset.Start found; // null = step 1
    private String message = "";
    private Color messageColor = Palette.HP_LOW;

    public ForgotPasswordScene(GameWindow window) {
        super(window);
        accountField = addWidget(new PixelTextField(FIELD_X, BOX_Y + 30, FIELD_W, 60, false)
                .withHint("JUAN_DC OR JUAN@GMAIL.COM"));
        accountField.setOnSubmit(this::next);
        int by = BOX_Y + BOX_H - 21;
        nextButton = addWidget(new PixelButton("NEXT", BOX_X + 14, by, 70, 15, this::next));
        nextButton.setTextColor(Palette.TEXT_ON);

        int y = BOX_Y + 33;
        answerField = addWidget(new PixelTextField(FIELD_X, y, FIELD_W, PasswordReset.ANSWER_MAX, false)
                .withSpaces().withHint("NOT CASE-SENSITIVE"));
        newField = addWidget(new PixelTextField(FIELD_X, y + ROW, FIELD_W, Validator.PASSWORD_MAX, true)
                .withHint("6+ LETTERS AND NUMBERS"));
        confirmField = addWidget(new PixelTextField(FIELD_X, y + ROW * 2, FIELD_W, Validator.PASSWORD_MAX, true)
                .withHint("TYPE IT AGAIN"));
        answerField.setOnSubmit(() -> setTypingField(newField));
        newField.setOnSubmit(() -> setTypingField(confirmField));
        confirmField.setOnSubmit(this::reset);
        resetButton = addWidget(new PixelButton("RESET PASSWORD", BOX_X + 14, by, 100, 15, this::reset));
        resetButton.setTextColor(Palette.TEXT_ON);

        addWidget(new PixelButton("BACK", BOX_X + BOX_W - 74, by, 60, 15, this::back));
    }

    @Override
    public void onShow() {
        showStep(null);
        accountField.setText(window.getLastUsername());
        message = "";
        super.onShow();
        setTypingField(accountField);
    }

    private void showStep(PasswordReset.Start start) {
        found = start;
        boolean two = start != null;
        accountField.setVisible(!two);
        nextButton.setVisible(!two);
        answerField.setVisible(two);
        newField.setVisible(two);
        confirmField.setVisible(two);
        resetButton.setVisible(two);
        answerField.clear();
        newField.clear();
        confirmField.clear();
    }

    /** Step 1: find the account and show its question. */
    private void next() {
        try {
            showStep(PasswordReset.start(window.getAccounts(), accountField.getText()));
            message = "";
            setTypingField(answerField);
        } catch (AccountException e) {
            show(e.getMessage());
            setTypingField(accountField);
        }
    }

    /** Step 2: check the answer and save the new password. */
    private void reset() {
        String password = newField.getText();
        String problem = PasswordReset.checkAnswer(answerField.getText()) == null ? null : "TYPE YOUR ANSWER";
        PixelTextField where = answerField;
        if (problem == null) {
            problem = Validator.checkPassword(password);
            where = newField;
        }
        if (problem == null && !password.equals(confirmField.getText())) {
            problem = "THE TWO NEW PASSWORDS ARE NOT THE SAME";
            where = confirmField;
        }
        if (problem != null) {
            show(problem);
            setTypingField(where);
            return;
        }
        try {
            PasswordReset.resetPassword(window.getAccounts(), found.account(), answerField.getText(), password);
        } catch (AccountException e) {
            show(e.getMessage());
            answerField.clear();
            setTypingField(answerField);
            return;
        }
        setTypingField(null);
        String name = found.account().username();
        showDialog("PASSWORD CHANGED", new String[]{"LOG IN WITH YOUR NEW PASSWORD."}, 0, false,
                new DialogButton("LOG IN", () -> {
                    window.setLastUsername(name);
                    window.showLogin();
                }));
    }

    private void back() {
        if (found != null) {
            showStep(null);
            message = "";
            setTypingField(accountField);
        } else {
            window.showLogin();
        }
    }

    @Override
    protected void onEscape() {
        back();
    }

    private void show(String text) {
        message = text;
        messageColor = Palette.HP_LOW;
    }

    @Override
    protected void paintScene(Graphics2D g) {
        landscape.draw(g, tick);
        PixelFont.drawCentered(g, "FORGOT PASSWORD", W / 2, 6, 2, Palette.TEXT_WHITE, Palette.TEXT_DARK);
        PixelArt.board(g, BOX_X, BOX_Y, BOX_W, BOX_H);
        if (found == null) {
            PixelFont.drawCentered(g, "ANSWER YOUR SECURITY QUESTION TO RESET IT", W / 2, BOX_Y + 8, 1,
                    Palette.TEXT_GOLD, Palette.TEXT_DARK);
            PixelFont.draw(g, "USERNAME OR EMAIL", LABEL_X, BOX_Y + 33, 1, Palette.TEXT_DARK);
        } else {
            PixelFont.drawCentered(g, "ACCOUNT: " + found.account().username(), W / 2, BOX_Y + 6, 1,
                    Palette.TEXT_GOLD, Palette.TEXT_DARK);
            PixelFont.drawCentered(g, found.question(), W / 2, BOX_Y + 19, 1, Palette.TEXT_DARK, null);
            String[] labels = {"YOUR ANSWER", "NEW PASSWORD", "CONFIRM NEW"};
            for (int i = 0; i < labels.length; i++) {
                PixelFont.draw(g, labels[i], LABEL_X, BOX_Y + 36 + ROW * i, 1, Palette.TEXT_DARK);
            }
        }
        if (!message.isEmpty()) {
            drawWrapped(g, message, W / 2, found == null ? BOX_Y + 66 : BOX_Y + 96, BOX_W - 20, messageColor);
        }
    }
}
