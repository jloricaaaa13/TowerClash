package ui.pixel;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Draws small pixel sprites relative to a character's feet.
 * Units are designed facing RIGHT; for the enemy (facing left) the pen mirrors
 * everything automatically, so each unit only needs to be drawn once.
 *
 * rect(dx, dy, w, h): dx = pixels in front of the feet (negative = behind),
 *                     dy = how high above the ground the TOP of the rectangle is.
 *
 * scale: 1 = normal size, 2 = every sprite pixel becomes a 2x2 block (used on the battlefield
 * so you can clearly see what each unit is holding).
 */
public final class SpritePen {

    private static final Color OUTLINE = new Color(0x1E140C);

    private Graphics2D g;
    private int footX;
    private int groundY;
    private int facing;
    private int scale = 1;
    private boolean outline;
    private Color tint;

    public SpritePen begin(Graphics2D g, int footX, int groundY, int facing) {
        return begin(g, footX, groundY, facing, 1);
    }

    public SpritePen begin(Graphics2D g, int footX, int groundY, int facing, int scale) {
        this.g = g;
        this.footX = footX;
        this.groundY = groundY;
        this.facing = facing;
        this.scale = Math.max(1, scale);
        return this;
    }

    /** In outline mode every rectangle is drawn 1 pixel bigger in a dark color (a border). */
    public void setOutline(boolean outline) {
        this.outline = outline;
    }

    /** Draws every part in this one color (used for the white hit flash). null = normal colors. */
    public void setTint(Color tint) {
        this.tint = tint;
    }

    public void rect(int dx, int dy, int w, int h, Color color) {
        int s = scale;
        int sx = facing >= 0 ? footX + dx * s : footX - (dx + w - 1) * s;
        int sy = groundY - dy * s;
        if (outline) {
            g.setColor(OUTLINE);
            g.fillRect(sx - 1, sy - 1, w * s + 2, h * s + 2);
        } else {
            g.setColor(tint != null ? tint : color);
            g.fillRect(sx, sy, w * s, h * s);
        }
    }
}
