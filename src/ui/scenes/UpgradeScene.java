package ui.scenes;

import java.awt.Graphics2D;
import model.Department;
import model.PlayerProgress;
import model.Side;
import model.Tower;
import ui.GameWindow;
import ui.pixel.Landscape;
import ui.pixel.Palette;
import ui.pixel.PixelArt;
import ui.pixel.PixelButton;
import ui.pixel.PixelFont;

/**
 * Permanent upgrades bought with coins (Level 1 to 5 for each department tower).
 * Each level: +15% HP, +15% damage, +25% Ultimate power. Saved right away.
 */
public class UpgradeScene extends PixelScene {

    private static final int CARD_W = 92;
    private static final int CARD_H = 94;
    private static final int CARD_Y = 36;

    private final Landscape landscape = new Landscape(172, new int[]{6, 312}, new int[]{});
    private final Department[] departments = Department.values();
    private final PixelButton[] buyButtons = new PixelButton[departments.length];
    private final int startX;

    public UpgradeScene(GameWindow window) {
        super(window);
        int gap = 10;
        startX = (W - (CARD_W * departments.length + gap * (departments.length - 1))) / 2;
        for (int i = 0; i < departments.length; i++) {
            Department d = departments[i];
            int x = startX + i * (CARD_W + gap);
            buyButtons[i] = addWidget(new PixelButton("", x, CARD_Y + CARD_H + 6, CARD_W, 14, () -> buy(d)));
        }
        addWidget(new PixelButton("BACK", (W - 76) / 2, 156, 76, 15, window::showMainMenu));
    }

    @Override
    public void onShow() {
        refresh();
        super.onShow();
    }

    private void buy(Department d) {
        PlayerProgress progress = window.getProgress();
        if (progress.upgradeTower(d)) {
            window.saveProgress();
            showToast(d.getTowerShortName().toUpperCase() + " IS NOW LEVEL " + progress.getTowerLevel(d) + "!");
        } else if (progress.getTowerUpgradeCost(d) > 0) {
            showToast("NOT ENOUGH COINS");
        }
        refresh();
    }

    private void refresh() {
        PlayerProgress progress = window.getProgress();
        for (int i = 0; i < departments.length; i++) {
            int cost = progress.getTowerUpgradeCost(departments[i]);
            if (cost < 0) {
                buyButtons[i].setText("MAXED");
                buyButtons[i].setEnabled(false);
            } else {
                buyButtons[i].setText("UPGRADE " + cost);
                buyButtons[i].setEnabled(true);
                buyButtons[i].setTextColor(progress.getCoins() >= cost ? Palette.TEXT_GOLD : Palette.TEXT_OFF);
            }
        }
    }

    @Override
    protected void paintScene(Graphics2D g) {
        landscape.draw(g, tick);
        PixelFont.drawCentered(g, "UPGRADES", W / 2, 6, 2, Palette.TEXT_WHITE, Palette.TEXT_DARK);

        PlayerProgress progress = window.getProgress();
        String coins = String.valueOf(progress.getCoins());
        int cw = PixelFont.width(coins, 1) + 10;
        PixelArt.coin(g, W / 2 - cw / 2, 24);
        PixelFont.drawOutlined(g, coins, W / 2 - cw / 2 + 10, 24, 1, Palette.TEXT_GOLD, Palette.TEXT_DARK);

        for (int i = 0; i < departments.length; i++) {
            drawCard(g, departments[i], startX + i * (CARD_W + 10), progress);
        }
    }

    private void drawCard(Graphics2D g, Department d, int x, PlayerProgress progress) {
        int level = progress.getTowerLevel(d);
        int cx = x + CARD_W / 2;
        PixelArt.board(g, x, CARD_Y, CARD_W, CARD_H);
        PixelArt.tower(g, cx - 8, CARD_Y + 38, 16, 26, d.getColor(), tick);
        PixelFont.drawCentered(g, d.getShortName(), cx, CARD_Y + 43, 1,
                Palette.lighter(d.getColor(), 0.15f), Palette.TEXT_DARK);

        // Level stars
        int starsLeft = cx - PlayerProgress.MAX_TOWER_LEVEL * 10 / 2 + 2;
        for (int s = 0; s < PlayerProgress.MAX_TOWER_LEVEL; s++) {
            int sx = starsLeft + s * 10;
            g.setColor(Palette.TEXT_DARK);
            g.fillRect(sx - 1, CARD_Y + 53, 8, 7);
            g.setColor(s < level ? Palette.GOLD : Palette.WOOD_DARK);
            g.fillRect(sx, CARD_Y + 54, 6, 5);
        }

        // Stats now -> after the next upgrade
        Tower now = d.createTower(Side.PLAYER, level, 1.0);
        Tower next = level < PlayerProgress.MAX_TOWER_LEVEL ? d.createTower(Side.PLAYER, level + 1, 1.0) : null;
        statLine(g, cx, CARD_Y + 64, "HP", (int) now.getMaxHp(), next == null ? -1 : (int) next.getMaxHp());
        statLine(g, cx, CARD_Y + 73, "DMG", (int) Math.round(now.getDamage()),
                next == null ? -1 : (int) Math.round(next.getDamage()));
        statLine(g, cx, CARD_Y + 82, "ULT", (int) Math.round(now.getUltimatePower() * 100),
                next == null ? -1 : (int) Math.round(next.getUltimatePower() * 100));
    }

    private void statLine(Graphics2D g, int cx, int y, String label, int now, int next) {
        String suffix = label.equals("ULT") ? "%" : "";
        String text = label + " " + now + suffix + (next >= 0 ? ">" + next + suffix : "");
        PixelFont.drawCentered(g, text, cx, y, 1, Palette.TEXT_DARK, null);
    }
}
