package ui.pixel;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Drawing helpers for the pixel-art world.
 * Everything is drawn on a small 320 x 180 canvas using only fillRect,
 * then scaled up, which gives the chunky "retro game" look.
 */
public final class PixelArt {

    /** Size of the virtual game screen, in game pixels. */
    public static final int WIDTH = 320;
    public static final int HEIGHT = 180;

    private PixelArt() {
    }

    // ------------------------------------------------------------------
    //  Sky
    // ------------------------------------------------------------------

    /** Sky made of color bands, with dithered edges like classic pixel art. */
    public static void sky(Graphics2D g, int horizonY, Color[] bands) {
        int bandH = (horizonY + bands.length - 1) / bands.length;
        for (int i = 0; i < bands.length; i++) {
            int top = i * bandH;
            int bottom = (i == bands.length - 1) ? HEIGHT : top + bandH;
            g.setColor(bands[i]);
            g.fillRect(0, top, WIDTH, bottom - top);
        }
        for (int i = 1; i < bands.length; i++) {
            int y = i * bandH;
            g.setColor(bands[i]);
            for (int x = 0; x < WIDTH; x += 4) {
                g.fillRect(x, y - 2, 1, 1);
            }
            for (int x = 0; x < WIDTH; x += 2) {
                g.fillRect(x + 1, y - 1, 1, 1);
            }
        }
    }

    public static void cloud(Graphics2D g, int x, int y, Color body, Color shade) {
        g.setColor(body);
        g.fillRect(x, y + 5, 34, 5);
        g.fillRect(x + 4, y + 2, 12, 4);
        g.fillRect(x + 12, y, 12, 6);
        g.fillRect(x + 22, y + 3, 9, 3);
        g.setColor(shade);
        g.fillRect(x + 2, y + 9, 30, 1);
    }

    /** Thin, long, faint cloud streak near the horizon. */
    public static void haze(Graphics2D g, int x, int y, int length, Color color) {
        g.setColor(color);
        g.fillRect(x, y, length, 1);
        g.fillRect(x + 5, y - 1, length - 12, 1);
    }

    /** Rolling hills: one thin column per x, height from a sine wave. */
    public static void hills(Graphics2D g, int baseY, int amplitude, double frequency, double phase, Color color) {
        g.setColor(color);
        for (int x = 0; x < WIDTH; x++) {
            double wave = Math.sin(x * frequency + phase) * 0.6 + Math.sin(x * frequency * 2.3 + phase * 1.7) * 0.4;
            int top = baseY - (int) Math.round(amplitude * (0.5 + 0.5 * wave));
            g.fillRect(x, top, 1, HEIGHT - top);
        }
    }

    // ------------------------------------------------------------------
    //  Ground and nature
    // ------------------------------------------------------------------

    /** A pine tree with three layers of leaves; the right half is shaded. */
    public static void pine(Graphics2D g, int centerX, int groundY, int height, Color body, Color shade) {
        int trunkH = Math.max(2, height / 7);
        g.setColor(Palette.TRUNK);
        g.fillRect(centerX - 1, groundY - trunkH, 2, trunkH);

        int crownTop = groundY - height;
        int crownH = height - trunkH;
        int tierH = crownH * 45 / 100;
        for (int t = 0; t < 3; t++) {
            int tierTop = crownTop + t * (crownH - tierH) / 2;
            int maxHalf = Math.max(2, height * (15 + t * 7) / 100);
            for (int r = 0; r < tierH; r++) {
                int half = 1 + r * maxHalf / tierH;
                g.setColor(body);
                g.fillRect(centerX - half, tierTop + r, half, 1);
                g.setColor(shade);
                g.fillRect(centerX, tierTop + r, half, 1);
            }
        }
    }

    /** Grass on top, brown dirt with pebbles, and purple rock at the bottom. */
    public static void ground(Graphics2D g, int groundY) {
        int bedrockY = groundY + (HEIGHT - groundY) * 62 / 100;

        g.setColor(Palette.DIRT);
        g.fillRect(0, groundY, WIDTH, HEIGHT - groundY);
        g.setColor(Palette.BEDROCK);
        g.fillRect(0, bedrockY, WIDTH, HEIGHT - bedrockY);
        for (int x = 0; x < WIDTH; x++) {
            int bump = hash(x, 7) % 3;
            g.fillRect(x, bedrockY - bump, 1, bump);
        }

        for (int y = groundY + 3; y < bedrockY - 2; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int h = hash(x, y) % 40;
                if (h == 0) {
                    g.setColor(Palette.DIRT_LIGHT);
                    g.fillRect(x, y, 2, 1);
                } else if (h == 1) {
                    g.setColor(Palette.DIRT_DARK);
                    g.fillRect(x, y, 2, 2);
                }
            }
        }
        g.setColor(Palette.BEDROCK_DARK);
        for (int y = bedrockY + 1; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (hash(x, y) % 30 == 0) {
                    g.fillRect(x, y, 2, 1);
                }
            }
        }

        g.setColor(Palette.GRASS);
        g.fillRect(0, groundY, WIDTH, 3);
        g.setColor(Palette.GRASS_LIGHT);
        g.fillRect(0, groundY, WIDTH, 1);
        for (int x = 0; x < WIDTH; x++) {
            if (hash(x, 1) % 4 == 0) {
                g.fillRect(x, groundY - 1, 1, 1); // grass tufts
            }
        }
        g.setColor(Palette.GRASS_DARK);
        for (int x = 0; x < WIDTH; x++) {
            g.fillRect(x, groundY + 3, 1, hash(x, 2) % 3); // jagged edge into the dirt
        }
    }

    /** Small wooden sign with a red arrow, standing on a rock. */
    public static void signpost(Graphics2D g, int x, int groundY) {
        g.setColor(Palette.ROCK);
        g.fillRect(x - 3, groundY - 3, 9, 3);
        g.fillRect(x - 2, groundY - 4, 6, 1);
        g.setColor(Palette.ROCK_LIGHT);
        g.fillRect(x - 2, groundY - 4, 3, 1);

        g.setColor(Palette.WOOD_OUTLINE);
        g.fillRect(x, groundY - 14, 3, 11);
        g.setColor(Palette.WOOD_DARK);
        g.fillRect(x + 1, groundY - 14, 1, 11);

        g.setColor(Palette.WOOD_OUTLINE);
        g.fillRect(x - 5, groundY - 20, 14, 8);
        g.setColor(Palette.WOOD);
        g.fillRect(x - 4, groundY - 19, 12, 6);
        g.setColor(Palette.SIGN_ARROW);
        g.fillRect(x - 2, groundY - 17, 6, 2);
        g.fillRect(x + 4, groundY - 18, 1, 4);
        g.fillRect(x + 5, groundY - 17, 1, 2);
    }

    // ------------------------------------------------------------------
    //  Towers
    // ------------------------------------------------------------------

    /**
     * A department tower in its color, with battlements, windows, a door,
     * and a flag that waves (tick is the animation frame counter).
     */
    public static void tower(Graphics2D g, int x, int groundY, int w, int h, Color color, int tick) {
        Color light = Palette.lighter(color, 0.3f);
        Color dark = Palette.darker(color, 0.3f);
        Color outline = Palette.darker(color, 0.7f);
        int top = groundY - h;

        // Flag pole and waving flag
        int poleX = x + w / 2;
        g.setColor(Palette.WOOD_OUTLINE);
        g.fillRect(poleX, top - 14, 1, 10);
        boolean wave = (tick / 8) % 2 == 0;
        int flagY = wave ? top - 14 : top - 13;
        g.setColor(outline);
        g.fillRect(poleX + 1, flagY, 7, 5);
        g.setColor(color);
        g.fillRect(poleX + 1, flagY + 1, 6, 3);
        g.setColor(light);
        g.fillRect(poleX + 1, flagY + 1, 6, 1);

        // Battlements
        int block = Math.max(2, w / 5);
        for (int bx = x; bx < x + w; bx += block * 2) {
            int bw = Math.min(block, x + w - bx);
            g.setColor(outline);
            g.fillRect(bx - 1, top - 5, bw + 2, 5);
            g.setColor(color);
            g.fillRect(bx, top - 4, bw, 4);
        }

        // Body with shading
        g.setColor(outline);
        g.fillRect(x - 1, top - 1, w + 2, h + 1);
        g.setColor(color);
        g.fillRect(x, top, w, h);
        g.setColor(dark);
        g.fillRect(x + w * 2 / 3, top, w - w * 2 / 3, h);
        g.setColor(light);
        g.fillRect(x, top, 1, h);

        // Bricks
        g.setColor(dark);
        for (int by = top + 4; by < groundY - 2; by += 5) {
            int shift = ((by - top) / 5) % 2 == 0 ? 0 : 3;
            for (int bx = x + 2 + shift; bx < x + w - 2; bx += 6) {
                g.fillRect(bx, by, 2, 1);
            }
        }

        // Windows
        for (int wy = top + 6; wy < groundY - 14; wy += 9) {
            window(g, x + w / 4 - 1, wy, outline);
            window(g, x + w * 3 / 4 - 2, wy, outline);
        }

        // Door
        int doorW = Math.max(4, w / 3);
        int doorH = Math.max(6, h / 5);
        int doorX = x + (w - doorW) / 2;
        g.setColor(outline);
        g.fillRect(doorX, groundY - doorH, doorW, doorH);
        g.fillRect(doorX + 1, groundY - doorH - 1, doorW - 2, 1);
    }

    private static void window(Graphics2D g, int x, int y, Color frame) {
        g.setColor(frame);
        g.fillRect(x - 1, y - 1, 4, 5);
        g.setColor(Palette.WINDOW_LIGHT);
        g.fillRect(x, y, 2, 3);
    }

    // ------------------------------------------------------------------
    //  Wooden board (used for dialogs, cards, options, credits)
    // ------------------------------------------------------------------

    public static void board(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(Palette.SHADOW);
        g.fillRect(x + 2, y + 3, w, h);

        g.setColor(Palette.WOOD_OUTLINE);
        g.fillRect(x + 1, y, w - 2, h);
        g.fillRect(x, y + 1, w, h - 2);

        int plankH = 11;
        int row = 0;
        for (int py = y + 1; py < y + h - 1; py += plankH, row++) {
            int ph = Math.min(plankH, y + h - 1 - py);
            g.setColor(Palette.WOOD);
            g.fillRect(x + 1, py, w - 2, ph);
            g.setColor(Palette.WOOD_LIGHT);
            g.fillRect(x + 2, py, w - 4, 1);
            if (ph > 2) {
                g.setColor(Palette.WOOD_DARK);
                g.fillRect(x + 1, py + ph - 1, w - 2, 1);
            }
            if (ph > 6) {
                g.setColor(Palette.WOOD_GRAIN);
                for (int i = 0; i < w / 24 + 1; i++) {
                    int gx = x + 4 + hash(row, i) % Math.max(1, w - 16);
                    int gy = py + 3 + hash(i, row) % (ph - 5);
                    g.fillRect(gx, gy, 7, 1);
                }
            }
        }

        // Nails in the corners
        g.setColor(Palette.WOOD_OUTLINE);
        g.fillRect(x + 3, y + 3, 1, 1);
        g.fillRect(x + w - 4, y + 3, 1, 1);
        g.fillRect(x + 3, y + h - 4, 1, 1);
        g.fillRect(x + w - 4, y + h - 4, 1, 1);
    }

    // ------------------------------------------------------------------
    //  HUD pieces
    // ------------------------------------------------------------------

    /** A wooden plank (used as the background of HUD buttons). */
    public static void plank(Graphics2D g, int x, int y, int w, int h, Color wood) {
        g.setColor(Palette.WOOD_OUTLINE);
        g.fillRect(x + 1, y, w - 2, h);
        g.fillRect(x, y + 1, w, h - 2);
        g.setColor(wood);
        g.fillRect(x + 1, y + 1, w - 2, h - 2);
        g.setColor(Palette.lighter(wood, 0.25f));
        g.fillRect(x + 2, y + 1, w - 4, 1);
        g.setColor(Palette.darker(wood, 0.25f));
        g.fillRect(x + 1, y + h - 3, w - 2, 2);
    }

    /** Small lightning bolt = energy (3 x 5 pixels, with a shadow). */
    public static void bolt(Graphics2D g, int x, int y) {
        g.setColor(Palette.TEXT_DARK);
        boltShape(g, x + 1, y + 1);
        g.setColor(Palette.ENERGY);
        boltShape(g, x, y);
    }

    private static void boltShape(Graphics2D g, int x, int y) {
        g.fillRect(x + 1, y, 2, 1);
        g.fillRect(x + 1, y + 1, 1, 1);
        g.fillRect(x, y + 2, 3, 1);
        g.fillRect(x + 1, y + 3, 1, 1);
        g.fillRect(x, y + 4, 1, 1);
    }

    /** Small gold coin (7 x 7 pixels). */
    public static void coin(Graphics2D g, int x, int y) {
        g.setColor(Palette.TEXT_DARK);
        g.fillRect(x + 1, y, 5, 7);
        g.fillRect(x, y + 1, 7, 5);
        g.setColor(Palette.GOLD);
        g.fillRect(x + 1, y + 1, 5, 5);
        g.setColor(Palette.GOLD_DARK);
        g.fillRect(x + 3, y + 2, 1, 3);
    }

    /** A progress bar with a dark border. ratio is 0 to 1. */
    public static void bar(Graphics2D g, int x, int y, int w, int h, double ratio, Color fill) {
        g.setColor(Palette.TEXT_DARK);
        g.fillRect(x - 1, y - 1, w + 2, h + 2);
        g.setColor(Palette.BAR_BACK);
        g.fillRect(x, y, w, h);
        int filled = (int) Math.round(w * Math.max(0, Math.min(1, ratio)));
        if (filled > 0) {
            g.setColor(fill);
            g.fillRect(x, y, filled, h);
            g.setColor(Palette.lighter(fill, 0.35f));
            g.fillRect(x, y, filled, 1);
        }
    }

    /** Pseudo-random but always the same for the same (x, y), so textures do not flicker. */
    public static int hash(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return h & 0x7fffffff;
    }
}
