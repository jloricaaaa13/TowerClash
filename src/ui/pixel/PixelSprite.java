package ui.pixel;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * A pixel-art picture written as TEXT: every character is one pixel.
 *
 *   "..hhhhh..",      '.' = see-through
 *   ".hSSSSeS.",      other letters = colors from the palette
 *
 * This makes detailed sprites easy to read and edit right in the code.
 * Units are drawn facing RIGHT; for the enemy the picture is mirrored automatically.
 * A dark outline is added around every sprite so it stands out from the background.
 *
 * The finished pictures are made once and saved (cached) as images, so drawing is fast.
 */
public final class PixelSprite {

    private static final Color OUTLINE = new Color(0x1E140C);

    /** Colors every sprite can use without listing them. */
    private static final Map<Character, Integer> BASE_COLORS = new HashMap<>();

    static {
        base('S', 0xF2C28E); // skin
        base('s', 0xC98E5A); // skin shadow
        base('e', 0x1E140C); // eye
        base('m', 0xB5544A); // mouth
        base('W', 0xF7F7F7); // white
        base('w', 0xD4D4D8); // light grey
        base('k', 0x1F1F23); // black
        base('K', 0x4B4B55); // dark grey
        base('g', 0x9CA3AF); // grey
        base('G', 0xD1D5DB); // silver
        base('y', 0xFACC15); // gold / yellow
        base('Y', 0xCA8A04); // dark gold
        base('b', 0x7C4A2D); // brown
        base('B', 0xA86B3C); // light brown
        base('h', 0x3B2314); // brown hair
        base('H', 0x1E140C); // black hair
    }

    private static void base(char c, int rgb) {
        BASE_COLORS.put(c, rgb);
    }

    private final int[][] pixels; // [row][column], ARGB color, 0 = see-through
    private final int width;
    private final int height;
    private final int footCol;    // the column between the feet (the unit's x position)
    private final BufferedImage[] cache = new BufferedImage[4]; // right / left, normal / white flash

    /**
     * @param footCol column (counted from 0 on the left) where the unit stands
     * @param palette extra colors, e.g. "r=DC2626 R=991B1B" (letter=hex color)
     * @param rows    the picture, top row first (short rows are filled with '.')
     */
    public PixelSprite(int footCol, String palette, String... rows) {
        Map<Character, Integer> colors = new HashMap<>(BASE_COLORS);
        for (String entry : palette.trim().split("\\s+")) {
            if (!entry.isEmpty()) {
                colors.put(entry.charAt(0), Integer.parseInt(entry.substring(2), 16));
            }
        }
        int w = 0;
        for (String row : rows) {
            w = Math.max(w, row.length());
        }
        this.width = w;
        this.height = rows.length;
        this.footCol = footCol;
        this.pixels = new int[height][width];
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < rows[r].length(); c++) {
                char ch = rows[r].charAt(c);
                if (ch == '.') {
                    continue;
                }
                Integer rgb = colors.get(ch);
                if (rgb == null) {
                    throw new IllegalArgumentException("Unknown color letter '" + ch + "' in sprite row " + r);
                }
                pixels[r][c] = 0xFF000000 | rgb;
            }
        }
    }

    private PixelSprite(int footCol, int[][] pixels) {
        this.footCol = footCol;
        this.pixels = pixels;
        this.height = pixels.length;
        this.width = pixels[0].length;
    }

    /**
     * The second walking frame: from row 'legsFrom' down, the back leg moves forward
     * and the front leg moves back by one pixel (the legs swing together).
     */
    public PixelSprite walkFrame(int legsFrom) {
        int[][] copy = new int[height][];
        for (int r = 0; r < height; r++) {
            copy[r] = pixels[r].clone();
        }
        for (int r = legsFrom; r < height; r++) {
            int[] row = new int[width];
            for (int c = 0; c < width; c++) {
                if (pixels[r][c] == 0) {
                    continue;
                }
                int to = c < footCol ? c + 1 : c > footCol ? c - 1 : c;
                if (row[to] == 0 || to == c) {
                    row[to] = pixels[r][c];
                }
            }
            copy[r] = row;
        }
        return new PixelSprite(footCol, copy);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** The column between the feet. */
    public int footCol() {
        return footCol;
    }

    /**
     * Draws the sprite standing on the ground.
     * @param facing 1 = facing right (player), -1 = facing left (enemy)
     * @param scale  1 = normal size, 2 = twice as big
     * @param flash  true = all white (the hit flash)
     */
    public void draw(Graphics2D g, int footX, int groundY, int facing, int scale, boolean flash) {
        boolean left = facing < 0;
        BufferedImage img = image(left, flash);
        int s = Math.max(1, scale);
        int x = left ? footX + (footCol - width) * s : footX - (footCol + 1) * s;
        int y = groundY - (height + 1) * s;
        g.drawImage(img, x, y, img.getWidth() * s, img.getHeight() * s, null);
    }

    /** Builds (once) the picture with its outline, mirrored and/or white if needed. */
    private BufferedImage image(boolean mirrored, boolean flash) {
        int index = (mirrored ? 1 : 0) + (flash ? 2 : 0);
        if (cache[index] != null) {
            return cache[index];
        }
        BufferedImage img = new BufferedImage(width + 2, height + 2, BufferedImage.TYPE_INT_ARGB);
        int outline = 0xFF000000 | OUTLINE.getRGB();
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                if (pixels[r][c] == 0) {
                    continue;
                }
                for (int dy = 0; dy <= 2; dy++) {
                    for (int dx = 0; dx <= 2; dx++) {
                        img.setRGB(column(c + dx, mirrored), r + dy, outline);
                    }
                }
            }
        }
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                if (pixels[r][c] != 0) {
                    img.setRGB(column(c + 1, mirrored), r + 1, flash ? 0xFFFFFFFF : pixels[r][c]);
                }
            }
        }
        cache[index] = img;
        return img;
    }

    /** Column inside the image (with the 1-pixel outline border), mirrored if needed. */
    private int column(int imageCol, boolean mirrored) {
        return mirrored ? width + 1 - imageCol : imageCol;
    }
}
