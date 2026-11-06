package model.units;

import game.Battle;
import model.Projectile;
import model.Role;
import model.Side;
import model.Unit;
import ui.pixel.PixelSprite;

/**
 * Hospitality - ranged support. Throws hot coffee (low damage).
 * Ability COFFEE BREAK: every 4 seconds heals itself and nearby teammates.
 */
public class Barista extends Unit {

    private static final double HEAL_EVERY = 4;
    private static final double HEAL_RANGE = 40;
    private static final double HEAL_AMOUNT = 16;

    private double healTimer = 1.5; // first heal comes a bit sooner

    public Barista(Side side) {
        //      name      role         side  hp  dmg atkInterval range speed
        super("Barista", Role.RANGED, side, 90, 8, 1.3, 45, 17);
    }

    @Override
    protected void updateAbility(Battle battle, double dt) {
        healTimer -= dt;
        if (healTimer > 0) {
            return;
        }
        healTimer = HEAL_EVERY;
        for (Unit ally : battle.unitsNear(side, x, HEAL_RANGE)) {
            battle.healTarget(ally, HEAL_AMOUNT);
        }
    }

    @Override
    protected Projectile.Kind projectileKind() {
        return Projectile.Kind.COFFEE;
    }

    @Override
    public String getAbilityName() {
        return "COFFEE BREAK";
    }

    @Override
    public String getAbilityText() {
        return "HEALS NEARBY TEAMMATES";
    }

    // ---- Looks (pixel art: each letter is one pixel, '.' = see-through) ----

    private static final String COLORS = "r=B91C1C R=7F1D1D a=6B3E1E A=8B5A2B c=FFFFFF C=6B3E1E v=E5E7EB";

    private static final PixelSprite STAND = new PixelSprite(5, COLORS,
            "...........v....",
            "..........v.v...",
            "..RRRRRR...v....",
            ".RRRRRRRR.cccc..",
            "..hSSSSSS.CCCCc.",
            "..hSSSSeS.cccc.c",
            "..hSSSSSS.cccc.c",
            "...SSSmSs.cccc..",
            "....SSSS.SScc...",
            "..rrrrrr.S......",
            ".rrrrrrrrr......",
            ".raaaaaar.......",
            ".raaAaaar.......",
            ".raaaaaar.......",
            ".SaaaaaaS.......",
            "..aaaaaa........",
            "..kkkkkk........",
            "..kkk.kk........",
            "..kkk..kk.......",
            "..kkk..kk.......",
            ".KKKK.KKKK......");
    private static final PixelSprite WALK = STAND.walkFrame(17);

    @Override
    protected PixelSprite getSprite(int frame, int tick) {
        return frame == 0 ? STAND : WALK;
    }
}
