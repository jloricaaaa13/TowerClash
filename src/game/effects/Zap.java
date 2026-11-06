package game.effects;

import java.awt.Color;
import java.awt.Graphics2D;
import ui.pixel.PixelArt;

/** A jagged electric line between two enemies (Computer Science chain attack). */
public class Zap extends Effect {

    private final double x1;
    private final double y1;
    private final double x2;
    private final double y2;

    public Zap(double x1, double y1, double x2, double y2) {
        super(0.18);
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    @Override
    public void draw(Graphics2D g, int tick) {
        int steps = (int) Math.max(1, Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1)));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            int px = (int) Math.round(x1 + (x2 - x1) * t);
            int jitter = (i == 0 || i == steps) ? 0 : PixelArt.hash(i, tick) % 3 - 1;
            int py = (int) Math.round(y1 + (y2 - y1) * t) + jitter;
            g.setColor(new Color(0xFFF59D));
            g.fillRect(px, py, 1, 1);
        }
    }
}
