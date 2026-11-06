package ui.scenes;

import db.Account;
import db.AccountException;
import db.PasswordReset;
import db.SecurityInfo;
import java.awt.Color;
import java.awt.Graphics2D;
import ui.GameWindow;
import ui.pixel.Landscape;
import ui.pixel.Palette;
import ui.pixel.PixelArt;
import ui.pixel.PixelButton;
import ui.pixel.PixelFont;
import ui.pixel.PixelTextField;
import ui.pixel.QuestionPicker;

/**
 * SECURITY QUESTION: the question + answer used by FORGOT PASSWORD.
 *   - Shown right after login for accounts that do not have one yet (LATER = skip for now).
 *   - Opened from the Profile to change it (then the current password is asked first).
 */
public class SecurityQuestionScene extends PixelScene {

    private static final int BOX_X = 36;
    private static final int BOX_Y = 24;
    private static final int BOX_W = 248;
    private static final int BOX_H = 128;
    private static final int LABEL_X = BOX_X + 14;
    private static final int FIELD_X = BOX_X + 100;
    private static final int FIELD_W = BOX_W - 114;
    private static final Color GREEN = new Color(0x15803D);

    private final Landscape landscape = new Landscape(162, new int[]{8, 310}, new int[]{});
    private final QuestionPicker picker;
    private final PixelTextField answerField;
    private final PixelTextField passwordField;
    private final PixelButton backButton;
    private boolean firstTime;
    private String message = "";
    private Color messageColor = Palette.WOOD_OUTLINE;

    public SecurityQuestionScene(GameWindow window) {
        super(window);
        picker = addWidget(new QuestionPicker(BOX_X + 14, BOX_Y + 24, BOX_W - 28, PasswordReset.QUESTIONS));
        answerField = addWidget(new PixelTextField(FIELD_X, BOX_Y + 46, FIELD_W, PasswordReset.ANSWER_MAX, false)
                .withSpaces().withHint("NOT CASE-SENSITIVE"));
        passwordField = addWidget(new PixelTextField(FIELD_X, BOX_Y + 65, FIELD_W, 32, true)
                .withHint("YOUR PASSWORD NOW"));
        answerField.setOnSubmit(() -> {
            if (passwordField.isVisible()) {
                setTypingField(passwordField);
            } else {
                save();
            }
        });
        passwordField.setOnSubmit(this::save);
        int by = BOX_Y + BOX_H - 21;
        addWidget(new PixelButton("SAVE", BOX_X + 14, by, 70, 15, this::save)).setTextColor(Palette.TEXT_ON);
        backButton = addWidget(new PixelButton("BACK", BOX_X + BOX_W - 78, by, 64, 15, this::leave));
    }

    @Override
    public void onShow() {
        firstTime = window.isSecurityFirstTime();
        passwordField.setVisible(!firstTime);
        backButton.setText(firstTime ? "LATER" : "BACK");
        answerField.clear();
        passwordField.clear();
        message = "";
        try {
            SecurityInfo info = window.getAccounts().getSecurityInfo(window.getAccount());
            picker.setQuestion(info.hasQuestion() ? info.question() : PasswordReset.QUESTIONS.get(0));
        } catch (AccountException e) {
            picker.setQuestion(PasswordReset.QUESTIONS.get(0));
        }
        super.onShow();
        setTypingField(answerField);
    }

    private void save() {
        String problem = PasswordReset.checkAnswer(answerField.getText());
        if (problem != null) {
            show(problem, Palette.HP_LOW);
            setTypingField(answerField);
            return;
        }
        Account me = window.getAccount();
        try {
            if (!firstTime) {
                window.getAccounts().login(me.username(), passwordField.getText()); // checks the password
            }
        } catch (AccountException e) {
            show("CURRENT PASSWORD IS WRONG", Palette.HP_LOW);
            passwordField.clear();
            setTypingField(passwordField);
            return;
        }
        try {
            PasswordReset.setQuestion(window.getAccounts(), me, picker.getQuestion(), answerField.getText());
        } catch (AccountException e) {
            show(e.getMessage(), Palette.HP_LOW);
            return;
        }
        setTypingField(null);
        showDialog("SAVED!", new String[]{"IF YOU FORGET YOUR PASSWORD,", "ANSWER THIS QUESTION TO RESET IT."},
                0, false, new DialogButton("OK", this::leave));
    }

    private void leave() {
        if (firstTime) {
            window.showMainMenu();
        } else {
            window.showProfile();
        }
    }

    @Override
    protected void onEscape() {
        leave();
    }

    private void show(String text, Color color) {
        message = text;
        messageColor = color;
    }

    @Override
    protected void paintScene(Graphics2D g) {
        landscape.draw(g, tick);
        PixelFont.drawCentered(g, "SECURITY QUESTION", W / 2, 6, 2, Palette.TEXT_WHITE, Palette.TEXT_DARK);
        PixelArt.board(g, BOX_X, BOX_Y, BOX_W, BOX_H);
        PixelFont.drawCentered(g, firstTime ? "IN CASE YOU FORGET YOUR PASSWORD"
                : "CLICK THE QUESTION TO CHANGE IT", W / 2, BOX_Y + 8, 1, Palette.TEXT_GOLD, Palette.TEXT_DARK);
        PixelFont.draw(g, "YOUR ANSWER", LABEL_X, BOX_Y + 49, 1, Palette.TEXT_DARK);
        if (!firstTime) {
            PixelFont.draw(g, "PASSWORD", LABEL_X, BOX_Y + 68, 1, Palette.TEXT_DARK);
        }
        if (!message.isEmpty()) {
            drawWrapped(g, message, W / 2, BOX_Y + 90, BOX_W - 20, messageColor);
        } else {
            PixelFont.drawCentered(g, firstTime ? "YOU CAN ALSO SET IT LATER IN YOUR PROFILE"
                    : "THE ANSWER IS SAVED AS A HASH", W / 2, BOX_Y + 90, 1, GREEN, null);
        }
    }
}
