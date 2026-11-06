package ui.hud;

import game.Battle;
import java.awt.Graphics2D;
import model.Side;
import model.Tower;
import ui.pixel.Palette;
import ui.pixel.PixelArt;
import ui.pixel.PixelFont;

/** In-match tower upgrade button: shows the next level and its energy cost. */
public class UpgradeButton extends HudButton {

    private Battle battle;
    private Side side = Side.PLAYER;

    public UpgradeButton(int x, int y, int w, int h, Runnable onClick) {
        super(x, y, w, h, onClick);
    }

    /** @param side the tower this button controls (Side.ENEMY for the player who joined a LAN match) */
    public void setBattle(Battle battle, Side side) {
        this.battle = battle;
        this.side = side;
    }

    @Override
    protected boolean isAvailable() {
        return battle != null && battle.canUpgrade(side);
    }

    @Override
    protected void drawContent(Graphics2D g, int top, boolean available, int tick) {
        if (battle == null) {
            return;
        }
        Tower tower = battle.getTower(side);
        drawLabel(g, "TOWER", top + 3, available);
        int cost = tower.getUpgradeCost();
        if (cost < 0) {
            drawLabel(g, "MAX", top + 15, false);
            return;
        }
        drawLabel(g, "LV" + tower.getLevel() + ">" + (tower.getLevel() + 1), top + 13, available);
        int textW = PixelFont.width(String.valueOf(cost), 1);
        int startX = x + (w - textW - 6) / 2;
        PixelArt.bolt(g, startX, top + 24);
        PixelFont.drawOutlined(g, String.valueOf(cost), startX + 6, top + 24, 1,
                available ? Palette.ENERGY : Palette.TEXT_DISABLED, Palette.TEXT_DARK);
    }
}
