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
import ui.pixel.QuestionPicker;

/**
 * Make a new account, in two steps:
 *   1. Username, email, password (typed twice) -> NEXT
 *   2. A security question + answer (used by FORGOT PASSWORD) -> CREATE ACCOUNT
 * Every field is checked before saving, and the reason is shown if something is wrong.
 */
public class RegisterScene extends PixelScene {

    private static final int BOX_X = 36;
    private static final int BOX_Y = 24;
    private static final int BOX_W = 248;
    private static final int BOX_H = 128;
    private static final int LABEL_X = BOX_X + 14;
    private static final int FIELD_X = BOX_X + 84;
    private static final int FIELD_W = BOX_W - 98;
    private static final int ROW = 19;

    private final Landscape landscape = new Landscape(162, new int[]{8, 310}, new int[]{});
    private final PixelTextField usernameField;
    private final PixelTextField emailField;
    private final PixelTextField passwordField;
    private final PixelTextField confirmField;
    private final PixelButton nextButton;
    private final QuestionPicker picker;
    private final PixelTextField answerField;
    private final PixelButton createButton;
    private boolean questionStep;
    private String message = "";

    public RegisterScene(GameWindow window) {
        super(window);
        int y = BOX_Y + 24;
        usernameField = addWidget(new PixelTextField(FIELD_X, y, FIELD_W, Validator.USERNAME_MAX, false)
                .withHint("3-16 LETTERS/NUMBERS"));
        emailField = addWidget(new PixelTextField(FIELD_X, y + ROW, FIELD_W, Validator.EMAIL_MAX, false)
                .withHint("JUAN@SCHOOL.EDU"));
        passwordField = addWidget(new PixelTextField(FIELD_X, y + ROW * 2, FIELD_W, Validator.PASSWORD_MAX, true)
                .withHint("6+ LETTERS AND NUMBERS"));
        confirmField = addWidget(new PixelTextField(FIELD_X, y + ROW * 3, FIELD_W, Validator.PASSWORD_MAX, true)
                .withHint("TYPE IT AGAIN"));
        usernameField.setOnSubmit(() -> setTypingField(emailField));
        emailField.setOnSubmit(() -> setTypingField(passwordField));
        passwordField.setOnSubmit(() -> setTypingField(confirmField));
        confirmField.setOnSubmit(this::next);

        picker = addWidget(new QuestionPicker(BOX_X + 14, BOX_Y + 24, BOX_W - 28, PasswordReset.QUESTIONS));
        answerField = addWidget(new PixelTextField(FIELD_X, BOX_Y + 46, FIELD_W, PasswordReset.ANSWER_MAX, false)
                .withSpaces().withHint("NOT CASE-SENSITIVE"));
        answerField.setOnSubmit(this::create);

        nextButton = addWidget(new PixelButton("NEXT", BOX_X + 14, BOX_Y + BOX_H - 21, 70, 15, this::next));
        nextButton.setTextColor(Palette.TEXT_ON);
        createButton = addWidget(new PixelButton("CREATE ACCOUNT", BOX_X + 14, BOX_Y + BOX_H - 21, 100, 15, this::create));
        createButton.setTextColor(Palette.TEXT_ON);
        addWidget(new PixelButton("BACK", BOX_X + BOX_W - 78, BOX_Y + BOX_H - 21, 64, 15, this::back));
    }

    private void showStep(boolean question) {
        questionStep = question;
        usernameField.setVisible(!question);
        emailField.setVisible(!question);
        passwordField.setVisible(!question);
        confirmField.setVisible(!question);
        nextButton.setVisible(!question);
        picker.setVisible(question);
        answerField.setVisible(question);
        createButton.setVisible(question);
        message = "";
        setTypingField(question ? answerField : usernameField);
    }

    private void back() {
        if (questionStep) {
            showStep(false);
        } else {
            window.showLogin();
        }
    }

    @Override
    public void onShow() {
        usernameField.clear();
        emailField.clear();
        passwordField.clear();
        confirmField.clear();
        answerField.clear();
        picker.setQuestion(PasswordReset.QUESTIONS.get(0));
        super.onShow();
        showStep(false);
    }

    /** Step 1 -> 2: checks the account details (and that the username/email are free). */
    private void next() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        // Check every box first; stop at the first problem and put the cursor there
        String problem = Validator.checkUsername(username);
        PixelTextField where = usernameField;
        if (problem == null) {
            problem = Validator.checkEmail(email);
            where = emailField;
        }
        if (problem == null) {
            problem = Validator.checkPassword(password);
            where = passwordField;
        }
        if (problem == null && !password.equals(confirmField.getText())) {
            problem = "THE TWO PASSWORDS ARE NOT THE SAME";
            where = confirmField;
        }
        try {
            if (problem == null && window.getAccounts().findAccount(username) != null) {
                problem = "USERNAME IS ALREADY TAKEN";
                where = usernameField;
            }
            if (problem == null && window.getAccounts().findAccount(email) != null) {
                problem = "EMAIL IS ALREADY USED BY ANOTHER ACCOUNT";
                where = emailField;
            }
        } catch (AccountException e) {
            problem = e.getMessage();
        }
        if (problem != null) {
            message = problem;
            setTypingField(where);
            return;
        }
        showStep(true);
    }

    /** Step 2: saves the account with its security question, then logs in. */
    private void create() {
        String problem = PasswordReset.checkAnswer(answerField.getText());
        if (problem != null) {
            message = problem;
            setTypingField(answerField);
            return;
        }
        try {
            window.register(usernameField.getText().trim(), emailField.getText().trim(), passwordField.getText(),
                    picker.getQuestion(), answerField.getText()); // logs in and opens the Dashboard
        } catch (AccountException e) {
            showStep(false);
            message = e.getMessage();
            setTypingField(e.getMessage().contains("EMAIL") ? emailField : usernameField);
        }
    }

    @Override
    protected void onEscape() {
        back();
    }

    @Override
    protected void paintScene(Graphics2D g) {
        landscape.draw(g, tick);
        PixelFont.drawCentered(g, "NEW PLAYER", W / 2, 6, 2, Palette.TEXT_WHITE, Palette.TEXT_DARK);

        PixelArt.board(g, BOX_X, BOX_Y, BOX_W, BOX_H);
        if (!questionStep) {
            PixelFont.drawCentered(g, "CREATE YOUR ACCOUNT (STEP 1 OF 2)", W / 2, BOX_Y + 8, 1, Palette.TEXT_GOLD,
                    Palette.TEXT_DARK);
            String[] labels = {"USERNAME", "EMAIL", "PASSWORD", "CONFIRM"};
            for (int i = 0; i < labels.length; i++) {
                PixelFont.draw(g, labels[i], LABEL_X, BOX_Y + 27 + ROW * i, 1, Palette.TEXT_DARK);
            }
        } else {
            PixelFont.drawCentered(g, "SECURITY QUESTION (STEP 2 OF 2)", W / 2, BOX_Y + 8, 1, Palette.TEXT_GOLD,
                    Palette.TEXT_DARK);
            PixelFont.draw(g, "YOUR ANSWER", LABEL_X, BOX_Y + 49, 1, Palette.TEXT_DARK);
            PixelFont.drawCentered(g, "CLICK THE QUESTION TO PICK ANOTHER ONE.", W / 2, BOX_Y + 66, 1,
                    Palette.WOOD_OUTLINE, null);
            PixelFont.drawCentered(g, "NEEDED IF YOU FORGET YOUR PASSWORD.", W / 2, BOX_Y + 76, 1,
                    Palette.WOOD_OUTLINE, null);
        }
        if (!message.isEmpty()) {
            drawWrapped(g, message, W / 2, questionStep ? BOX_Y + 92 : BOX_Y + 98, BOX_W - 20, Palette.HP_LOW);
        } else if (!questionStep) {
            PixelFont.drawCentered(g, "YOUR PROGRESS WILL BE SAVED TO THIS ACCOUNT", W / 2, BOX_Y + 98, 1,
                    Palette.WOOD_OUTLINE, null);
        }
        boolean db = window.getAccounts().isDatabase();
        PixelFont.drawCentered(g, "SAVING TO: " + window.getAccounts().getName(), W / 2, 158, 1,
                db ? Palette.TEXT_ON : Palette.TEXT_OFF, Palette.TEXT_DARK);
    }
}
