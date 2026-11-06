package ui.scenes;

import db.AccountException;
import db.Validator;
import java.awt.Graphics2D;
import ui.GameWindow;
import ui.pixel.Landscape;
import ui.pixel.Palette;
import ui.pixel.PixelArt;
import ui.pixel.PixelButton;
import ui.pixel.PixelFont;
import ui.pixel.PixelTextField;

/**
 * Change your password: type the CURRENT password, then the NEW one twice.
 * The new password follows the same rules as when registering (6-32 characters, letters AND numbers).
 * The database only ever stores the hash (with a brand-new salt), never the password itself.
 */
public class ChangePasswordScene extends PixelScene {

    private static final int BOX_X = 36;
    private static final int BOX_Y = 24;
    private static final int BOX_W = 248;
    private static final int BOX_H = 118;
    private static final int LABEL_X = BOX_X + 14;
    private static final int FIELD_X = BOX_X + 96;
    private static final int FIELD_W = BOX_W - 110;
    private static final int ROW = 19;

    private final Landscape landscape = new Landscape(162, new int[]{8, 310}, new int[]{});
    private final PixelTextField currentField;
    private final PixelTextField newField;
    private final PixelTextField confirmField;
    private String message = "";

    public ChangePasswordScene(GameWindow window) {
        super(window);
        int y = BOX_Y + 24;
        currentField = addWidget(new PixelTextField(FIELD_X, y, FIELD_W, Validator.PASSWORD_MAX, true)
                .withHint("YOUR PASSWORD NOW"));
        newField = addWidget(new PixelTextField(FIELD_X, y + ROW, FIELD_W, Validator.PASSWORD_MAX, true)
                .withHint("6+ LETTERS AND NUMBERS"));
        confirmField = addWidget(new PixelTextField(FIELD_X, y + ROW * 2, FIELD_W, Validator.PASSWORD_MAX, true)
                .withHint("TYPE IT AGAIN"));
        currentField.setOnSubmit(() -> setTypingField(newField));
        newField.setOnSubmit(() -> setTypingField(confirmField));
        confirmField.setOnSubmit(this::save);

        addWidget(new PixelButton("SAVE PASSWORD", BOX_X + 14, BOX_Y + BOX_H - 21, 100, 15, this::save))
                .setTextColor(Palette.TEXT_ON);
        addWidget(new PixelButton("BACK", BOX_X + BOX_W - 78, BOX_Y + BOX_H - 21, 64, 15, window::showProfile));
    }

    @Override
    public void onShow() {
        currentField.clear();
        newField.clear();
        confirmField.clear();
        message = "";
        super.onShow();
        setTypingField(currentField);
    }

    private void save() {
        String current = currentField.getText();
        String next = newField.getText();

        String problem = current.isEmpty() ? "PLEASE ENTER YOUR CURRENT PASSWORD" : null;
        PixelTextField where = currentField;
        if (problem == null) {
            problem = Validator.checkPassword(next);
            where = newField;
        }
        if (problem == null && next.equals(current)) {
            problem = "THE NEW PASSWORD IS THE SAME AS THE OLD ONE";
            where = newField;
        }
        if (problem == null && !next.equals(confirmField.getText())) {
            problem = "THE TWO NEW PASSWORDS ARE NOT THE SAME";
            where = confirmField;
        }
        if (problem != null) {
            message = problem;
            setTypingField(where);
            return;
        }
        try {
            window.changePassword(current, next);
        } catch (AccountException e) {
            message = e.getMessage();
            currentField.clear();
            setTypingField(currentField);
            return;
        }
        currentField.clear();
        newField.clear();
        confirmField.clear();
        setTypingField(null);
        showDialog("PASSWORD CHANGED", new String[]{"USE YOUR NEW PASSWORD", "THE NEXT TIME YOU LOG IN."},
                0, false, new DialogButton("OK", window::showProfile));
    }

    @Override
    protected void onEscape() {
        window.showProfile();
    }

    @Override
    protected void paintScene(Graphics2D g) {
        landscape.draw(g, tick);
        PixelFont.drawCentered(g, "CHANGE PASSWORD", W / 2, 6, 2, Palette.TEXT_WHITE, Palette.TEXT_DARK);

        PixelArt.board(g, BOX_X, BOX_Y, BOX_W, BOX_H);
        String name = window.getAccount() == null ? "" : window.getAccount().username();
        PixelFont.drawCentered(g, "ACCOUNT: " + name, W / 2, BOX_Y + 8, 1, Palette.TEXT_GOLD, Palette.TEXT_DARK);
        String[] labels = {"CURRENT", "NEW PASSWORD", "CONFIRM NEW"};
        for (int i = 0; i < labels.length; i++) {
            PixelFont.draw(g, labels[i], LABEL_X, BOX_Y + 27 + ROW * i, 1, Palette.TEXT_DARK);
        }
        if (!message.isEmpty()) {
            PixelFont.drawCentered(g, message, W / 2, BOX_Y + 85, 1, Palette.HP_LOW, null);
        } else {
            PixelFont.drawCentered(g, "ONLY THE HASH IS SAVED, NEVER THE PASSWORD", W / 2, BOX_Y + 85, 1,
                    Palette.WOOD_OUTLINE, null);
        }
    }
}
