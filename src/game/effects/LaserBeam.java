package game.effects;

import java.awt.Color;
import java.awt.Graphics2D;

/** The giant beam of System Overload, from one tower to the other. */
public class LaserBeam extends Effect {

    private final int fromX;
    private final int toX;
    private final int y;

    public LaserBeam(double fromX, double toX, int y) {
        super(1.0);
        this.fromX = (int) Math.min(fromX, toX);
        this.toX = (int) Math.max(fromX, toX);
        this.y = y;
    }

    @Override
    public void draw(Graphics2D g, int tick) {
        double p = progress();
        int thickness = p < 0.15 ? 2 : (int) Math.round(7 * (1 - p)) + 1 + (tick % 2);
        g.setColor(new Color(250, 204, 21, 170));
        g.fillRect(fromX, y - thickness, toX - fromX, thickness * 2 + 1);
        g.setColor(new Color(255, 250, 220));
        g.fillRect(fromX, y - thickness / 3, toX - fromX, thickness / 3 * 2 + 1);
        // sparks where it hits
        g.setColor(Color.WHITE);
        for (int i = 0; i < 5; i++) {
            int sx = toX - 8 + ((tick * 7 + i * 13) % 16);
            int sy = y - 8 + ((tick * 5 + i * 11) % 16);
            g.fillRect(sx, sy, 1, 1);
        }
    }
}
