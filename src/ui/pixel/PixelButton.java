package ui.pixel;

import java.awt.Color;
import java.awt.Graphics2D;

/** A wooden plank button with gold pixel text. It lifts up when selected and sinks when pressed. */
public class PixelButton extends PixelWidget {

    private String text;
    private Color textColor = Palette.TEXT_GOLD;

    public PixelButton(String text, int x, int y, int w, int h, Runnable onClick) {
        super(x, y, w, h);
        this.text = text;
        setOnClick(onClick);
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setTextColor(Color textColor) {
        this.textColor = textColor;
    }

    @Override
    public void draw(Graphics2D g, boolean focused, boolean pressed, int tick) {
        boolean active = isEnabled();
        int lift = !active ? 0 : pressed ? 1 : focused ? -1 : 0;
        int by = y + lift;

        // Shadow on the ground
        g.setColor(Palette.SHADOW);
        g.fillRect(x + 1, y + h, w - 2, 1);

        // Outline with cut corners
        g.setColor(Palette.WOOD_OUTLINE);
        g.fillRect(x + 1, by, w - 2, h);
        g.fillRect(x, by + 1, w, h - 2);

        // Wood
        Color wood = !active ? Palette.WOOD_DISABLED : focused ? Palette.WOOD_HOVER : Palette.WOOD;
        g.setColor(wood);
        g.fillRect(x + 1, by + 1, w - 2, h - 2);
        g.setColor(Palette.lighter(wood, 0.25f));
        g.fillRect(x + 2, by + 1, w - 4, 1);
        g.setColor(Palette.darker(wood, 0.25f));
        g.fillRect(x + 1, by + h - 3, w - 2, 2);

        // Wood grain and nails
        int seed = Math.abs(text.hashCode());
        g.setColor(Palette.darker(wood, 0.12f));
        g.fillRect(x + 5 + seed % 8, by + 3, 8, 1);
        g.fillRect(x + w - 16 - seed % 6, by + h - 5, 7, 1);
        g.setColor(Palette.WOOD_OUTLINE);
        g.fillRect(x + 3, by + h / 2 - 1, 1, 1);
        g.fillRect(x + w - 4, by + h / 2 - 1, 1, 1);

        // Text
        int tx = x + (w - PixelFont.width(text, 1)) / 2;
        int ty = by + (h - PixelFont.GLYPH_H) / 2 - 1;
        Color fill = active ? textColor : Palette.TEXT_DISABLED;
        PixelFont.drawOutlined(g, text, tx, ty, 1, fill, Palette.TEXT_DARK);

        // Bouncing arrow next to the selected button
        if (focused && active) {
            int bounce = (tick / 6) % 2;
            PixelFont.drawOutlined(g, ">", x - 9 - bounce, y + (h - PixelFont.GLYPH_H) / 2, 1,
                    Palette.TEXT_WHITE, Palette.TEXT_DARK);
        }
    }
}
