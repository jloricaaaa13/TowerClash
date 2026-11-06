package game.effects;

import java.awt.Color;
import java.awt.Graphics2D;
import ui.pixel.PixelArt;

/** The whole screen flashes a color and fades out (with snowflakes for Final Exam). */
public class ScreenFlash extends Effect {

    private final Color color;

    public ScreenFlash(Color color) {
        super(0.8);
        this.color = color;
    }

    @Override
    public void draw(Graphics2D g, int tick) {
        int alpha = (int) (170 * (1 - progress()));
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        g.fillRect(0, 0, PixelArt.WIDTH, PixelArt.HEIGHT);
        g.setColor(new Color(255, 255, 255, Math.min(255, alpha + 60)));
        for (int i = 0; i < 40; i++) {
            int x = PixelArt.hash(i, 1) % PixelArt.WIDTH;
            int y = (PixelArt.hash(i, 2) % 120 + (int) (age * 40)) % 120;
            g.fillRect(x, y, 1, 1);
        }
    }
}
