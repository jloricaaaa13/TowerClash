package ui.scenes;

import java.awt.Color;
import java.awt.Graphics2D;
import model.Stage;
import ui.pixel.Palette;
import ui.pixel.PixelArt;
import ui.pixel.PixelFont;
import ui.pixel.PixelWidget;

/** A wooden card for one stage. Locked stages show a padlock. */
public class StageCard extends PixelWidget {

    private static final Color[] DIFFICULTY_COLORS = {
        new Color(0x22C55E), new Color(0x84CC16), new Color(0xF59E0B), new Color(0xF97316), new Color(0xEF4444)
    };

    private final Stage stage;
    private boolean locked;
    private boolean selected;
    private int stars;

    public StageCard(Stage stage, int x, int y, int w, int h) {
        super(x, y, w, h);
        this.stage = stage;
    }

    public Stage getStage() {
        return stage;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
        setEnabled(!locked);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public void draw(Graphics2D g, boolean focused, boolean pressed, int tick) {
        int lift = locked || pressed ? 0 : (focused || selected) ? -2 : 0;
        int top = y + lift;
        int cx = x + w / 2;
        PixelArt.board(g, x, top, w, h);

        if (selected) {
            g.setColor(Palette.TEXT_GOLD);
            g.fillRect(x - 2, top - 2, w + 4, 2);
            g.fillRect(x - 2, top + h, w + 4, 2);
            g.fillRect(x - 2, top, 2, h);
            g.fillRect(x + w, top, 2, h);
        }

        PixelFont.drawCentered(g, "STAGE", cx, top + 5, 1, Palette.TEXT_DARK, null);
        PixelFont.drawCentered(g, String.valueOf(stage.number()), cx, top + 15, 2,
                locked ? Palette.TEXT_DISABLED : Palette.TEXT_GOLD, Palette.TEXT_DARK);

        String[] words = stage.name().toUpperCase().split(" ", 2);
        PixelFont.drawCentered(g, words[0], cx, top + 35, 1, Palette.TEXT_DARK, null);
        if (words.length > 1) {
            PixelFont.drawCentered(g, words[1], cx, top + 44, 1, Palette.TEXT_DARK, null);
        }

        if (locked) {
            g.setColor(new Color(40, 25, 15, 120));
            g.fillRect(x + 1, top + 1, w - 2, h - 2);
            drawPadlock(g, cx - 5, top + 58);
            PixelFont.drawCentered(g, "LOCKED", cx, top + 76, 1, Palette.TEXT_WHITE, Palette.TEXT_DARK);
            return;
        }

        Color pip = DIFFICULTY_COLORS[Math.max(0, Math.min(4, stage.difficulty() - 1))];
        int pipsX = cx - 12;
        for (int i = 0; i < 5; i++) {
            g.setColor(Palette.TEXT_DARK);
            g.fillRect(pipsX + i * 5 - 1, top + 55, 5, 5);
            g.setColor(i < stage.difficulty() ? pip : Palette.WOOD_DARK);
            g.fillRect(pipsX + i * 5, top + 56, 3, 3);
        }
        String reward = "+" + stage.rewardCoins();
        int rw = PixelFont.width(reward, 1) + 9;
        PixelArt.coin(g, cx - rw / 2, top + 64);
        PixelFont.drawOutlined(g, reward, cx - rw / 2 + 9, top + 64, 1, Palette.TEXT_GOLD, Palette.TEXT_DARK);

        // Best stars on this stage
        for (int i = 0; i < 3; i++) {
            PixelFont.drawOutlined(g, "*", cx - 14 + i * 10, top + 77, 1,
                    i < stars ? Palette.GOLD : Palette.WOOD_DARK, Palette.TEXT_DARK);
        }
    }

    public void setStars(int stars) {
        this.stars = stars;
    }

    private void drawPadlock(Graphics2D g, int px, int py) {
        g.setColor(Palette.TEXT_DARK);
        g.fillRect(px + 2, py, 6, 2);
        g.fillRect(px + 2, py, 2, 6);
        g.fillRect(px + 6, py, 2, 6);
        g.fillRect(px, py + 5, 10, 8);
        g.setColor(Palette.GOLD);
        g.fillRect(px + 1, py + 6, 8, 6);
        g.setColor(Palette.TEXT_DARK);
        g.fillRect(px + 4, py + 8, 2, 2);
    }
}
