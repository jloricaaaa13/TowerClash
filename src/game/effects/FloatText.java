package game.effects;

import java.awt.Color;
import java.awt.Graphics2D;
import ui.pixel.Palette;
import ui.pixel.PixelFont;

/** Text that floats up and disappears, e.g. "LEVEL 2!". */
public class FloatText extends Effect {

    private final double x;
    private final double y;
    private final String text;
    private final Color color;

    public FloatText(double x, double y, String text, Color color) {
        super(1.2);
        this.x = x;
        this.y = y;
        this.text = text;
        this.color = color;
    }

    @Override
    public void draw(Graphics2D g, int tick) {
        int dy = (int) (progress() * 10);
        PixelFont.drawCentered(g, text, (int) x, (int) y - dy, 1, color, Palette.TEXT_DARK);
    }
}
