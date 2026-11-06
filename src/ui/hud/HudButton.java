package ui.hud;

import java.awt.Graphics2D;
import ui.pixel.Palette;
import ui.pixel.PixelArt;
import ui.pixel.PixelFont;
import ui.pixel.PixelWidget;

/**
 * A tall wooden button on the battle HUD (abstract).
 * It is always clickable (so the game can say WHY an action failed),
 * but it looks grey when the action is not available right now.
 */
public abstract class HudButton extends PixelWidget {

    protected HudButton(int x, int y, int w, int h, Runnable onClick) {
        super(x, y, w, h);
        setOnClick(onClick);
    }

    /** Can the action be done right now? (enough energy, not on cooldown...) */
    protected abstract boolean isAvailable();

    /** Draw the text/icons on top of the plank. */
    protected abstract void drawContent(Graphics2D g, int top, boolean available, int tick);

    @Override
    public void draw(Graphics2D g, boolean focused, boolean pressed, int tick) {
        boolean available = isAvailable();
        int lift = pressed ? 1 : (focused && available) ? -1 : 0;
        int top = y + lift;
        g.setColor(Palette.SHADOW);
        g.fillRect(x + 1, y + h, w - 2, 1);
        PixelArt.plank(g, x, top, w, h,
                !available ? Palette.WOOD_DISABLED : focused ? Palette.WOOD_HOVER : Palette.WOOD);
        drawContent(g, top, available, tick);
    }

    protected void drawLabel(Graphics2D g, String text, int textY, boolean available) {
        PixelFont.drawCentered(g, text, x + w / 2, textY, 1,
                available ? Palette.TEXT_GOLD : Palette.TEXT_DISABLED, Palette.TEXT_DARK);
    }
}
