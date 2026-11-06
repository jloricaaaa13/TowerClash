package ui.pixel;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * A box the player can type in (username, email, password).
 *
 * Click it (or move to it with TAB / arrow keys) and type. BACKSPACE deletes,
 * ENTER runs the "submit" action (for example: go to the next box, or log in).
 * Password boxes show dots instead of the letters.
 */
public class PixelTextField extends PixelWidget {

    private static final Color BOX = new Color(0xFFF7E6);
    private static final Color BOX_ACTIVE = Color.WHITE;
    private static final Color TEXT = new Color(0x2E1B0F);
    private static final Color HINT = new Color(0xA8A29E);

    private final StringBuilder text = new StringBuilder();
    private final int maxLength;
    private final boolean password;
    private boolean allowSpaces;
    private String hint = "";
    private boolean active;
    private Runnable onSubmit;

    public PixelTextField(int x, int y, int w, int maxLength, boolean password) {
        super(x, y, w, 13);
        this.maxLength = maxLength;
        this.password = password;
        this.allowSpaces = false;
    }

    /** Grey text shown while the box is empty, e.g. "3-16 LETTERS OR NUMBERS". */
    public PixelTextField withHint(String hint) {
        this.hint = hint;
        return this;
    }

    /** Lets the player type spaces too (e.g. a security answer like "san pablo"). */
    public PixelTextField withSpaces() {
        this.allowSpaces = true;
        return this;
    }

    public void setOnSubmit(Runnable onSubmit) {
        this.onSubmit = onSubmit;
    }

    /** Called when ENTER is pressed while typing in this box. */
    public void submit() {
        if (onSubmit != null) {
            onSubmit.run();
        }
    }

    /** Adds a typed character (ignored if it is not allowed or the box is full). */
    public void type(char c) {
        boolean ok = c >= 33 && c <= 126 || (c == ' ' && allowSpaces);
        if (ok && text.length() < maxLength) {
            text.append(c);
        }
    }

    public void backspace() {
        if (text.length() > 0) {
            text.setLength(text.length() - 1);
        }
    }

    public String getText() {
        return text.toString();
    }

    public void setText(String value) {
        text.setLength(0);
        text.append(value == null ? "" : value);
    }

    public void clear() {
        text.setLength(0);
    }

    /** true = this is the box the keyboard is typing into (draws a blinking cursor). */
    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    /** Clicking a box only selects it (it never "submits"). */
    @Override
    public void click() {
    }

    @Override
    public void draw(Graphics2D g, boolean focused, boolean pressed, int tick) {
        // Box with a dark border (gold border while typing in it)
        g.setColor(active ? Palette.GOLD : Palette.WOOD_OUTLINE);
        g.fillRect(x - 1, y - 1, w + 2, h + 2);
        g.setColor(active ? BOX_ACTIVE : BOX);
        g.fillRect(x, y, w, h);
        g.setColor(new Color(0, 0, 0, 30));
        g.fillRect(x, y, w, 1); // inner shadow at the top

        int tx = x + 4;
        int ty = y + 3;
        int cursorX;
        if (text.length() == 0) {
            if (!active) {
                PixelFont.draw(g, hint, tx, ty, 1, HINT);
            }
            cursorX = tx;
        } else if (password) {
            int max = (w - 10) / 5;
            int shown = Math.min(text.length(), max);
            g.setColor(TEXT);
            for (int i = 0; i < shown; i++) {
                g.fillRect(tx + i * 5, ty + 2, 3, 3); // a dot for every letter
            }
            cursorX = tx + shown * 5;
        } else {
            String shown = text.toString();
            while (PixelFont.width(shown, 1) > w - 10) {
                shown = shown.substring(1); // long text: show the end
            }
            PixelFont.draw(g, shown, tx, ty, 1, TEXT);
            cursorX = tx + PixelFont.width(shown, 1) + 1;
        }
        if (active && (tick / 15) % 2 == 0) {
            g.setColor(TEXT);
            g.fillRect(cursorX, ty - 1, 1, 9); // blinking cursor
        }
    }
}
