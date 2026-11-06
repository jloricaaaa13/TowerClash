package ui.hud;

import game.Battle;
import java.awt.Graphics2D;
import model.Side;
import model.UltimateSkill;
import ui.pixel.Palette;
import ui.pixel.PixelArt;

/** ULTIMATE button: meter bar, cooldown, and a blinking gold border when it is ready. */
public class UltimateButton extends HudButton {

    private Battle battle;
    private Side side = Side.PLAYER;

    public UltimateButton(int x, int y, int w, int h, Runnable onClick) {
        super(x, y, w, h, onClick);
    }

    /** @param side the tower this button controls (Side.ENEMY for the player who joined a LAN match) */
    public void setBattle(Battle battle, Side side) {
        this.battle = battle;
        this.side = side;
    }

    @Override
    protected boolean isAvailable() {
        return battle != null && !battle.isOver() && battle.getTower(side).getUltimate().isReady();
    }

    @Override
    protected void drawContent(Graphics2D g, int top, boolean available, int tick) {
        if (battle == null) {
            return;
        }
        UltimateSkill skill = battle.getTower(side).getUltimate();
        boolean blink = (tick / 8) % 2 == 0;

        if (available && blink) {
            g.setColor(Palette.GOLD);
            g.fillRect(x - 1, top - 2, w + 2, 1);
            g.fillRect(x - 1, top + h + 1, w + 2, 1);
            g.fillRect(x - 2, top - 1, 1, h + 2);
            g.fillRect(x + w + 1, top - 1, 1, h + 2);
        }

        drawLabel(g, "ULTIMATE", top + 3, available);
        PixelArt.bar(g, x + 5, top + 14, w - 10, 4, skill.getMeter() / UltimateSkill.METER_MAX,
                skill.getCooldownLeft() > 0 ? Palette.WOOD_DISABLED : Palette.GOLD);

        String status;
        if (skill.getCooldownLeft() > 0) {
            status = "WAIT " + (int) Math.ceil(skill.getCooldownLeft());
        } else if (available) {
            status = "READY!";
        } else {
            status = (int) skill.getMeter() + "%";
        }
        drawLabel(g, status, top + 24, available && blink || !available && skill.getCooldownLeft() <= 0);
    }
}
