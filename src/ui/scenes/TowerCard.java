package ui.scenes;

import java.awt.Graphics2D;
import model.Department;
import ui.pixel.Palette;
import ui.pixel.PixelArt;
import ui.pixel.PixelFont;
import ui.pixel.PixelWidget;

/** A wooden card showing one department tower on the Tower Select screen. */
public class TowerCard extends PixelWidget {

    private final Department department;
    private boolean selected;

    public TowerCard(Department department, int x, int y, int w, int h) {
        super(x, y, w, h);
        this.department = department;
    }

    public Department getDepartment() {
        return department;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public void draw(Graphics2D g, boolean focused, boolean pressed, int tick) {
        int lift = pressed ? 0 : (focused || selected) ? -2 : 0;
        int cy = y + lift;
        int cx = x + w / 2;

        PixelArt.board(g, x, cy, w, h);

        // Gold frame around the chosen card
        if (selected) {
            g.setColor(Palette.TEXT_GOLD);
            g.fillRect(x - 2, cy - 2, w + 4, 2);
            g.fillRect(x - 2, cy + h, w + 4, 2);
            g.fillRect(x - 2, cy, 2, h);
            g.fillRect(x + w, cy, 2, h);
        }

        // Tower on a small patch of grass
        g.setColor(Palette.GRASS);
        g.fillRect(x + 14, cy + 58, w - 28, 2);
        g.setColor(Palette.GRASS_DARK);
        g.fillRect(x + 14, cy + 60, w - 28, 1);
        PixelArt.tower(g, cx - 11, cy + 58, 22, 38, department.getColor(), tick);

        // Text
        PixelFont.drawCentered(g, department.getShortName(), cx, cy + 66, 1,
                Palette.lighter(department.getColor(), 0.15f), Palette.TEXT_DARK);
        PixelFont.drawCentered(g, department.getTowerShortName(), cx, cy + 78, 1, Palette.TEXT_DARK, null);
        PixelFont.drawCentered(g, department.getStyle(), cx, cy + 88, 1, Palette.WOOD_OUTLINE, null);
    }
}
