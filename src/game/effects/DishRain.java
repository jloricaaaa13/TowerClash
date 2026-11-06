package game.effects;

import game.Battle;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Random;

/** Flaming plates falling from the sky all over the battlefield (Kitchen Inferno). */
public class DishRain extends Effect {

    private static final int COUNT = 20;
    private static final double FALL_SPEED = 200;

    private final int[] xs = new int[COUNT];
    private final double[] delays = new double[COUNT];

    public DishRain(Random random) {
        super(1.8);
        for (int i = 0; i < COUNT; i++) {
            xs[i] = 30 + random.nextInt(260);
            delays[i] = random.nextDouble() * 0.9;
        }
    }

    @Override
    public void draw(Graphics2D g, int tick) {
        for (int i = 0; i < COUNT; i++) {
            double t = age - delays[i];
            if (t < 0) {
                continue;
            }
            int y = (int) (-10 + t * FALL_SPEED);
            int x = xs[i];
            if (y < Battle.GROUND_Y - 2) {
                g.setColor(new Color(0xFF7A1A));
                g.fillRect(x, y - 4, 1, 3);
                g.setColor(new Color(0xFFD23F));
                g.fillRect(x + 1, y - 3, 1, 2);
                g.setColor(new Color(0xF4F4F4));
                g.fillRect(x - 2, y, 5, 1);
            } else if (y < Battle.GROUND_Y + 25) {
                int r = (y - Battle.GROUND_Y) / 6 + 1;
                g.setColor(new Color(0xFF7A1A));
                g.fillRect(x - r, Battle.GROUND_Y - 2, 1, 1);
                g.fillRect(x + r, Battle.GROUND_Y - 2, 1, 1);
                g.setColor(new Color(0xFFD23F));
                g.fillRect(x, Battle.GROUND_Y - 2 - r, 1, 1);
            }
        }
    }
}
