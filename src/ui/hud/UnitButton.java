package ui.hud;

import game.Battle;
import java.awt.Color;
import java.awt.Graphics2D;
import model.Side;
import model.Unit;
import model.UnitType;
import ui.pixel.Palette;
import ui.pixel.PixelArt;
import ui.pixel.PixelFont;

/** Spawn button: unit name, picture, energy cost, hotkey, and a cooldown shadow. */
public class UnitButton extends HudButton {

    private final int slot;
    private Battle battle;
    private Side side = Side.PLAYER;
    private UnitType type;
    private Unit preview;

    public UnitButton(int slot, int x, int y, int w, int h, Runnable onClick) {
        super(x, y, w, h, onClick);
        this.slot = slot;
    }

    /** @param side the tower this button controls (Side.ENEMY for the player who joined a LAN match) */
    public void setBattle(Battle battle, Side side) {
        this.battle = battle;
        this.side = side;
        this.type = battle.getTower(side).getUnitTypes().get(slot);
        this.preview = type.create(Side.PLAYER); // only used to draw the icon
    }

    /** A sample unit (used for the icon and the tooltip). */
    public Unit getPreview() {
        return preview;
    }

    public UnitType getType() {
        return type;
    }

    @Override
    protected boolean isAvailable() {
        return battle != null && battle.canSpawn(side, slot);
    }

    @Override
    protected void drawContent(Graphics2D g, int top, boolean available, int tick) {
        if (type == null) {
            return;
        }
        drawLabel(g, type.name(), top + 3, available);
        preview.drawPortrait(g, x + 2, top + 11, 29, h - 16, tick); // head and what it is holding

        PixelArt.bolt(g, x + 31, top + 17);
        PixelFont.drawOutlined(g, String.valueOf(type.cost()), x + 37, top + 17, 1,
                available ? Palette.ENERGY : Palette.TEXT_DISABLED, Palette.TEXT_DARK);
        PixelFont.draw(g, String.valueOf(slot + 1), x + w - 9, top + h - 12, 1, Palette.WOOD_OUTLINE);

        // Role stripe: green = FAST, blue = RANGED, red = TANK
        g.setColor(available ? preview.getRole().getColor() : Palette.darker(preview.getRole().getColor(), 0.35f));
        g.fillRect(x + 2, top + h - 4, w - 4, 2);

        double cooldown = battle.getSpawnCooldownRatio(side, slot);
        if (cooldown > 0) {
            g.setColor(new Color(0, 0, 0, 110));
            g.fillRect(x + 1, top + 1, w - 2, (int) Math.ceil((h - 2) * cooldown));
        }
        if (battle.getSpawnDisabledTime(side) > 0) {
            g.setColor(new Color(120, 0, 0, 130));
            g.fillRect(x + 1, top + 1, w - 2, h - 2);
            PixelFont.drawCentered(g, "OFFLINE", x + w / 2, top + 15, 1, Palette.TEXT_OFF, Palette.TEXT_DARK);
        }
    }
}
