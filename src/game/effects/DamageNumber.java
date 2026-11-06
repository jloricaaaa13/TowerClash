package game.effects;

import java.awt.Color;
import java.awt.Graphics2D;
import ui.pixel.Palette;
import ui.pixel.PixelFont;

/**
 * A number that pops up when something is hit.
 * Gold = counter hit (1.5x), grey = weak hit (0.7x), white = normal.
 */
public class DamageNumber extends Effect {

    private final double x;
    private final double y;
    private final String text;
    private final Color color;

    public DamageNumber(double x, double y, int amount, Color color) {
        this(x, y, String.valueOf(amount), color);
    }

    /** Any short text, e.g. "+12" for healing. */
    public DamageNumber(double x, double y, String text, Color color) {
        super(0.55);
        this.x = x;
        this.y = y;
        this.text = text;
        this.color = color;
    }

    @Override
    public void draw(Graphics2D g, int tick) {
        int rise = (int) (progress() * 7);
        PixelFont.drawCentered(g, text, (int) x, (int) y - rise, 1, color, Palette.TEXT_DARK);
    }
}
