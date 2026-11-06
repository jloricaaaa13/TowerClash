package ui.pixel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

/**
 * The game's text.
 *
 * Two looks (switch in OPTIONS > SMOOTH TEXT):
 *   SMOOTH (default) - clean bold letters drawn at full screen resolution: easiest to read.
 *   PIXEL            - our own 5x7 pixel font (each letter is 7 rows of 5 bits).
 *
 * Both looks use the SAME size boxes (6 game pixels per letter, 7 tall), so every screen
 * layout stays exactly the same whichever look is chosen. Text is always shown in capitals.
 */
public final class PixelFont {

    public static final int GLYPH_W = 5;
    public static final int GLYPH_H = 7;
    private static final int SPACING = 1;

    private static final Map<Character, int[]> GLYPHS = new HashMap<>();
    private static final Map<Character, Shape> SMOOTH_GLYPHS = new HashMap<>();
    private static final Map<Character, Double> SMOOTH_WIDTHS = new HashMap<>();
    private static final double SMOOTH_GAP = 1.0; // space between smooth letters (game pixels)
    private static final Font SMOOTH_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 100);
    private static boolean smooth = true;

    static {
        glyph('A', "01110 10001 10001 11111 10001 10001 10001");
        glyph('B', "11110 10001 10001 11110 10001 10001 11110");
        glyph('C', "01110 10001 10000 10000 10000 10001 01110");
        glyph('D', "11110 10001 10001 10001 10001 10001 11110");
        glyph('E', "11111 10000 10000 11110 10000 10000 11111");
        glyph('F', "11111 10000 10000 11110 10000 10000 10000");
        glyph('G', "01110 10001 10000 10111 10001 10001 01111");
        glyph('H', "10001 10001 10001 11111 10001 10001 10001");
        glyph('I', "01110 00100 00100 00100 00100 00100 01110");
        glyph('J', "00111 00010 00010 00010 00010 10010 01100");
        glyph('K', "10001 10010 10100 11000 10100 10010 10001");
        glyph('L', "10000 10000 10000 10000 10000 10000 11111");
        glyph('M', "10001 11011 10101 10101 10001 10001 10001");
        glyph('N', "10001 10001 11001 10101 10011 10001 10001");
        glyph('O', "01110 10001 10001 10001 10001 10001 01110");
        glyph('P', "11110 10001 10001 11110 10000 10000 10000");
        glyph('Q', "01110 10001 10001 10001 10101 10010 01101");
        glyph('R', "11110 10001 10001 11110 10100 10010 10001");
        glyph('S', "01111 10000 10000 01110 00001 00001 11110");
        glyph('T', "11111 00100 00100 00100 00100 00100 00100");
        glyph('U', "10001 10001 10001 10001 10001 10001 01110");
        glyph('V', "10001 10001 10001 10001 10001 01010 00100");
        glyph('W', "10001 10001 10001 10101 10101 10101 01010");
        glyph('X', "10001 10001 01010 00100 01010 10001 10001");
        glyph('Y', "10001 10001 10001 01010 00100 00100 00100");
        glyph('Z', "11111 00001 00010 00100 01000 10000 11111");
        glyph('0', "01110 10001 10011 10101 11001 10001 01110");
        glyph('1', "00100 01100 00100 00100 00100 00100 01110");
        glyph('2', "01110 10001 00001 00010 00100 01000 11111");
        glyph('3', "11111 00010 00100 00010 00001 10001 01110");
        glyph('4', "00010 00110 01010 10010 11111 00010 00010");
        glyph('5', "11111 10000 11110 00001 00001 10001 01110");
        glyph('6', "00110 01000 10000 11110 10001 10001 01110");
        glyph('7', "11111 00001 00010 00100 01000 01000 01000");
        glyph('8', "01110 10001 10001 01110 10001 10001 01110");
        glyph('9', "01110 10001 10001 01111 00001 00010 01100");
        glyph(' ', "00000 00000 00000 00000 00000 00000 00000");
        glyph('.', "00000 00000 00000 00000 00000 01100 01100");
        glyph(',', "00000 00000 00000 00000 01100 00100 01000");
        glyph('!', "00100 00100 00100 00100 00100 00000 00100");
        glyph('?', "01110 10001 00001 00010 00100 00000 00100");
        glyph('-', "00000 00000 00000 11111 00000 00000 00000");
        glyph('+', "00000 00100 00100 11111 00100 00100 00000");
        glyph(':', "00000 01100 01100 00000 01100 01100 00000");
        glyph('\'', "00100 00100 01000 00000 00000 00000 00000");
        glyph('/', "00001 00010 00010 00100 01000 01000 10000");
        glyph('(', "00010 00100 01000 01000 01000 00100 00010");
        glyph(')', "01000 00100 00010 00010 00010 00100 01000");
        glyph('>', "01000 00100 00010 00001 00010 00100 01000");
        glyph('<', "00010 00100 01000 10000 01000 00100 00010");
        glyph('&', "01100 10010 10100 01000 10101 10010 01101");
        glyph('%', "11000 11001 00010 00100 01000 10011 00011");
        glyph('|', "00100 00100 00100 00100 00100 00100 00100");
        glyph('*', "00100 00100 11111 01110 01110 11011 10001"); // star
        glyph('@', "01110 10001 10111 10101 10111 10000 01110");
        glyph('_', "00000 00000 00000 00000 00000 00000 11111");
        glyph('#', "01010 01010 11111 01010 11111 01010 01010");
        glyph('=', "00000 00000 11111 00000 11111 00000 00000");
    }

    private PixelFont() {
    }

    private static void glyph(char c, String rows) {
        String[] parts = rows.split(" ");
        int[] bits = new int[GLYPH_H];
        for (int i = 0; i < GLYPH_H; i++) {
            bits[i] = Integer.parseInt(parts[i], 2);
        }
        GLYPHS.put(c, bits);
    }

    /** Width of the text in pixels. */
    public static int width(String text, int scale) {
        if (text.isEmpty()) {
            return 0;
        }
        if (smooth) {
            double total = 0;
            String upper = text.toUpperCase();
            for (int i = 0; i < upper.length(); i++) {
                total += advance(upper.charAt(i));
            }
            return (int) Math.ceil((total - SMOOTH_GAP) * scale);
        }
        return (text.length() * (GLYPH_W + SPACING) - SPACING) * scale;
    }

    /**
     * How far to move right after a smooth letter. Smooth letters are proportional
     * (an I is thinner than an M), but never wider than a pixel letter, so text always fits.
     */
    private static double advance(char c) {
        if (c == ' ') {
            return 3.4;
        }
        if (c == '*' || (c >= '0' && c <= '9')) {
            return GLYPH_W + SMOOTH_GAP; // stars and digits: same width (numbers do not wiggle)
        }
        smoothGlyph(c);
        return SMOOTH_WIDTHS.get(c) + SMOOTH_GAP;
    }

    /** true = smooth high-resolution letters, false = classic 5x7 pixel letters. */
    public static void setSmooth(boolean value) {
        smooth = value;
    }

    public static void draw(Graphics2D g, String text, int x, int y, int scale, Color color) {
        if (smooth) {
            drawSmooth(g, text, x, y, scale, color, null);
            return;
        }
        g.setColor(color);
        String upper = text.toUpperCase();
        for (int i = 0; i < upper.length(); i++) {
            drawPixelGlyph(g, upper.charAt(i), x + i * (GLYPH_W + SPACING) * scale, y, scale);
        }
    }

    /** Text with a border around every letter (easy to read on any background). */
    public static void drawOutlined(Graphics2D g, String text, int x, int y, int scale, Color fill, Color outline) {
        if (smooth) {
            drawSmooth(g, text, x, y, scale, fill, outline);
            return;
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) {
                    draw(g, text, x + dx, y + dy, scale, outline);
                }
            }
        }
        draw(g, text, x, y, scale, fill);
    }

    private static void drawPixelGlyph(Graphics2D g, char c, int gx, int y, int scale) {
        int[] bits = GLYPHS.getOrDefault(c, GLYPHS.get('?'));
        for (int row = 0; row < GLYPH_H; row++) {
            for (int col = 0; col < GLYPH_W; col++) {
                if (((bits[row] >> (GLYPH_W - 1 - col)) & 1) == 1) {
                    g.fillRect(gx + col * scale, y + row * scale, scale, scale);
                }
            }
        }
    }

    /**
     * Smooth letters: each letter is a real font shape, sized to fill the same 5x7 box
     * the pixel letter would use. Because the canvas is high resolution, the curves stay sharp.
     */
    private static void drawSmooth(Graphics2D g, String text, int x, int y, int scale, Color fill, Color outline) {
        Object oldAa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        AffineTransform old = g.getTransform();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        String upper = text.toUpperCase();
        BasicStroke border = new BasicStroke(2.2f / scale, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        // Pass 1: dark borders for all letters (so a border never covers a neighbor letter)
        for (int pass = outline == null ? 1 : 0; pass < 2; pass++) {
            double cx = x;
            for (int i = 0; i < upper.length(); i++) {
                char c = upper.charAt(i);
                double letterX = cx;
                cx += advance(c) * scale;
                if (c == ' ') {
                    continue;
                }
                g.setTransform(old);
                g.translate(letterX, y);
                g.scale(scale, scale);
                if (c == '*') { // the star icon keeps its pixel shape
                    if (pass == 0) {
                        g.setColor(outline);
                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dy = -1; dy <= 1; dy++) {
                                drawPixelGlyph(g, c, dx, dy, 1);
                            }
                        }
                    } else {
                        g.setColor(fill);
                        drawPixelGlyph(g, c, 0, 0, 1);
                    }
                    continue;
                }
                Shape shape = smoothGlyph(c);
                if (pass == 0) {
                    g.setColor(outline);
                    g.setStroke(border);
                    g.draw(shape);
                } else {
                    g.setColor(fill);
                    g.fill(shape);
                }
            }
        }
        g.setTransform(old);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAa);
    }

    /** The font shape of one letter, fitted inside a 5 x 7 box (made once, then reused). */
    private static Shape smoothGlyph(char c) {
        Shape cached = SMOOTH_GLYPHS.get(c);
        if (cached != null) {
            return cached;
        }
        FontRenderContext frc = new FontRenderContext(null, true, true);
        Rectangle2D capBox = SMOOTH_FONT.createGlyphVector(frc, "H").getOutline().getBounds2D();
        double k = GLYPH_H / capBox.getHeight();                  // capital letters are 7 high
        Shape raw = SMOOTH_FONT.createGlyphVector(frc, String.valueOf(c)).getOutline();
        Rectangle2D box = raw.getBounds2D();
        double sx = k;
        double maxWidth = GLYPH_W + 0.4;
        if (box.getWidth() * k > maxWidth) {
            sx = maxWidth / box.getWidth();                        // squeeze wide letters (M, W)
        }
        double width = box.getWidth() * sx;
        boolean digit = c >= '0' && c <= '9';
        double left = digit ? (GLYPH_W - width) / 2 : 0;            // digits centered in their box
        AffineTransform fit = new AffineTransform();
        fit.translate(left - box.getX() * sx, -capBox.getY() * k); // baseline at the bottom of the box
        fit.scale(sx, k);
        Shape shape = fit.createTransformedShape(raw);
        SMOOTH_GLYPHS.put(c, shape);
        SMOOTH_WIDTHS.put(c, Math.max(1.2, width));
        return shape;
    }

    /** Centers the text on centerX. Pass null as outline for plain text. */
    public static void drawCentered(Graphics2D g, String text, int centerX, int y, int scale,
                                    Color fill, Color outline) {
        int x = centerX - width(text, scale) / 2;
        if (outline == null) {
            draw(g, text, x, y, scale, fill);
        } else {
            drawOutlined(g, text, x, y, scale, fill, outline);
        }
    }
}
