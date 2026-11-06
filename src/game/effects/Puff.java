package game.effects;

import java.awt.Color;
import java.awt.Graphics2D;

/** A little cloud of smoke when a unit is defeated. */
public class Puff extends Effect {

    private final double x;
    private final double y;
    private final Color color;

    public Puff(double x, double y, Color color) {
        super(0.45);
        this.x = x;
        this.y = y;
        this.color = color;
    }

    @Override
    public void draw(Graphics2D g, int tick) {
        double p = progress();
        int spread = (int) (2 + p * 6);
        int size = Math.max(1, (int) (4 - p * 3));
        g.setColor(color);
        int cx = (int) x;
        int cy = (int) y;
        g.fillRect(cx - spread, cy - size / 2, size, size);
        g.fillRect(cx + spread - size, cy - size / 2, size, size);
        g.fillRect(cx - size / 2, cy - spread, size, size);
        g.fillRect(cx - size / 2, cy + spread - size, size, size);
        g.fillRect(cx - size / 2, cy - size / 2, size, size);
    }
}
