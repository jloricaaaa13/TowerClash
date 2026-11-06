package ui.pixel;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import model.StageTheme;

/**
 * The background world: sky, moving clouds, hills, trees, and ground.
 * The parts that never move are drawn ONCE into images (cached),
 * so each frame only has to copy them and draw the clouds, rain, and lightning.
 * A StageTheme changes the colors (day, sunset, night, rain, storm).
 */
public class Landscape {

    private static final int W = PixelArt.WIDTH;
    private static final int H = PixelArt.HEIGHT;

    // {start x, y, speed divisor (bigger = slower)}
    private static final int[][] CLOUDS = {{20, 16, 3}, {150, 30, 4}, {250, 10, 5}};
    // {start x, y, length, speed divisor}
    private static final int[][] HAZES = {{0, 72, 60, 6}, {120, 84, 90, 8}, {220, 66, 70, 7}, {60, 98, 50, 9}};

    private final StageTheme theme;
    private final int groundY;
    private final BufferedImage skyLayer;
    private final BufferedImage frontLayer;

    public Landscape(int groundY, int[] trees, int[] farTrees) {
        this(groundY, trees, farTrees, StageTheme.DAY);
    }

    /**
     * @param groundY  y where the grass starts
     * @param trees    x positions of the big green pine trees
     * @param farTrees x positions of the small pale trees behind them
     */
    public Landscape(int groundY, int[] trees, int[] farTrees, StageTheme theme) {
        this.theme = theme;
        this.groundY = groundY;
        skyLayer = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = skyLayer.createGraphics();
        PixelArt.sky(g, groundY, theme.getSky());
        if (theme.hasStars()) {
            for (int i = 0; i < 60; i++) {
                int x = PixelArt.hash(i, 91) % W;
                int y = PixelArt.hash(i, 92) % (groundY - 30);
                g.setColor(i % 5 == 0 ? new Color(0xFFF6C8) : new Color(0xC9D4FF));
                g.fillRect(x, y, 1, 1);
            }
            g.setColor(new Color(0xF4F1DE)); // moon
            g.fillRect(262, 14, 10, 10);
            g.fillRect(261, 15, 12, 8);
            g.setColor(theme.getSky()[0]);
            g.fillRect(266, 13, 8, 8);
        }
        g.dispose();

        frontLayer = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        g = frontLayer.createGraphics();
        PixelArt.hills(g, groundY - 10, 16, 0.035, 0.0, Palette.HILL_FAR);
        PixelArt.hills(g, groundY - 2, 9, 0.05, 1.7, Palette.HILL_NEAR);
        for (int x : farTrees) {
            PixelArt.pine(g, x, groundY, 20, Palette.TREE_FAR, Palette.TREE_FAR_SHADE);
        }
        for (int x : trees) {
            PixelArt.pine(g, x, groundY, 28 + PixelArt.hash(x, 3) % 8, Palette.TREE, Palette.TREE_SHADE);
        }
        PixelArt.ground(g, groundY);
        if (theme.getTint() != null) {
            // Tint only the pixels that were drawn (not the transparent sky area)
            g.setComposite(AlphaComposite.SrcAtop);
            g.setColor(theme.getTint());
            g.fillRect(0, 0, W, H);
        }
        g.dispose();
    }

    /** Sky, clouds, hills, trees, ground. */
    public void draw(Graphics2D g, int tick) {
        g.drawImage(skyLayer, 0, 0, null);
        for (int[] c : CLOUDS) {
            PixelArt.cloud(g, wrap(c[0] + tick / c[2], 40), c[1], theme.getCloud(), theme.getCloudShade());
        }
        for (int[] h : HAZES) {
            PixelArt.haze(g, wrap(h[0] + tick / h[3], h[2]), h[1], h[2], theme.getHaze());
        }
        g.drawImage(frontLayer, 0, 0, null);
    }

    /** Rain and lightning, drawn on top of everything in the world. */
    public void drawWeather(Graphics2D g, int tick) {
        if (theme.hasRain()) {
            g.setColor(new Color(190, 205, 230, 150));
            for (int i = 0; i < 70; i++) {
                int x = Math.floorMod(PixelArt.hash(i, 5) % W - tick * 2, W);
                int y = Math.floorMod(PixelArt.hash(i, 6) % H + tick * 6, groundY + 4);
                g.fillRect(x, y, 1, 3);
            }
        }
        if (theme.hasLightning()) {
            int cycle = tick % 210;
            if (cycle < 3 || (cycle > 6 && cycle < 8)) {
                g.setColor(new Color(255, 255, 255, 120));
                g.fillRect(0, 0, W, H);
            }
        }
    }

    public StageTheme getTheme() {
        return theme;
    }

    /** Makes things that drift off the right edge come back from the left. */
    private static int wrap(int x, int objectWidth) {
        int span = W + objectWidth;
        return Math.floorMod(x, span) - objectWidth;
    }
}
