package ui.scenes;

import java.awt.Color;
import java.awt.Graphics2D;
import model.Side;
import model.Unit;
import model.UnitType;
import ui.pixel.Palette;
import ui.pixel.PixelArt;
import ui.pixel.PixelFont;
import ui.pixel.PixelWidget;

/**
 * A wooden card for one unit on the Lineup screen:
 * name, picture, role, energy cost, and (when picked) its slot number 1-3.
 */
public class UnitCard extends PixelWidget {

    private final UnitType type;
    private final Unit preview; // a sample unit, only used for drawing and stats
    private int slot = -1;      // -1 = not in the lineup, otherwise 0, 1 or 2

    public UnitCard(UnitType type, int x, int y, int w, int h) {
        super(x, y, w, h);
        this.type = type;
        this.preview = type.create(Side.PLAYER);
    }

    public UnitType getType() {
        return type;
    }

    public Unit getPreview() {
        return preview;
    }

    /** -1 = not picked, 0-2 = which slot (key 1, 2 or 3) the unit will use in battle. */
    public void setSlot(int slot) {
        this.slot = slot;
    }

    @Override
    public void draw(Graphics2D g, boolean focused, boolean pressed, int tick) {
        boolean picked = slot >= 0;
        int lift = pressed ? 0 : (focused || picked) ? -2 : 0;
        int cy = y + lift;
        int cx = x + w / 2;

        PixelArt.board(g, x, cy, w, h);
        if (!picked) {
            g.setColor(new Color(0, 0, 0, 45)); // unpicked cards look a bit darker
            g.fillRect(x + 1, cy + 1, w - 2, h - 2);
        }

        // Gold frame and slot number when the unit is in the lineup
        if (picked) {
            g.setColor(Palette.TEXT_GOLD);
            g.fillRect(x - 2, cy - 2, w + 4, 2);
            g.fillRect(x - 2, cy + h, w + 4, 2);
            g.fillRect(x - 2, cy, 2, h);
            g.fillRect(x + w, cy, 2, h);
            g.setColor(Palette.WOOD_OUTLINE);
            g.fillRect(x - 4, cy + h - 8, 11, 11);
            g.setColor(Palette.GOLD);
            g.fillRect(x - 3, cy + h - 7, 9, 9);
            PixelFont.draw(g, String.valueOf(slot + 1), x - 1, cy + h - 6, 1, Palette.TEXT_DARK);
        }

        PixelFont.drawCentered(g, type.name(), cx, cy + 5, 1, Palette.TEXT_DARK, null);

        // Unit picture on a little patch of grass
        int groundY = cy + 52;
        g.setColor(Palette.GRASS);
        g.fillRect(x + 8, groundY, w - 16, 2);
        g.setColor(Palette.GRASS_DARK);
        g.fillRect(x + 8, groundY + 2, w - 16, 1);
        preview.drawSprite(g, cx - 3, groundY, 1, (tick / 10) % 2, tick);

        Color roleColor = preview.getRole().getColor();
        PixelFont.drawCentered(g, preview.getRole().getLabel(), cx, cy + 56, 1, roleColor, Palette.TEXT_DARK);

        String cost = String.valueOf(type.cost());
        int costW = 7 + PixelFont.width(cost, 1);
        PixelArt.bolt(g, cx - costW / 2, cy + 66);
        PixelFont.drawOutlined(g, cost, cx - costW / 2 + 7, cy + 66, 1, Palette.ENERGY, Palette.TEXT_DARK);
    }
}
