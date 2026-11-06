package model.units;

import game.Battle;
import model.Role;
import model.Side;
import model.Unit;
import ui.pixel.PixelSprite;

/**
 * Hospitality - tank and leader. Slow, tough, in a dark red suit with a clipboard.
 * Ability FIVE-STAR SERVICE: teammates near the Manager move and attack 25% faster.
 */
public class HotelManager extends Unit {

    private static final double AURA_RANGE = 50;
    private static final double HASTE = 0.25;

    public HotelManager(Side side) {
        //      name      role       side  hp  dmg atkInterval range speed
        super("Manager", Role.TANK, side, 290, 13, 1.2, 12, 12);
    }

    @Override
    protected void updateAbility(Battle battle, double dt) {
        for (Unit ally : battle.unitsNear(side, x, AURA_RANGE)) {
            if (ally != this) {
                ally.applyHaste(HASTE, 0.3); // refreshed every step while they stay close
            }
        }
    }

    @Override
    public String getAbilityName() {
        return "FIVE-STAR SERVICE";
    }

    @Override
    public String getAbilityText() {
        return "NEARBY TEAMMATES ARE 25% FASTER";
    }

    // ---- Looks (pixel art: each letter is one pixel, '.' = see-through) ----

    private static final String COLORS = "r=7F1D1D R=5C1414 t=FACC15 p=FFF7E6 P=A16207 x=94A3B8";

    private static final PixelSprite STAND = new PixelSprite(6, COLORS,
            "..HHHHHHH........",
            ".HHHHHHHHH.......",
            ".HHSSSSSSS.......",
            ".HSSSSSSeS.......",
            ".HSSSSSSSS.......",
            "..SSSSSmSs.......",
            "...SSSSSS........",
            "..rrrWWWrrr......",
            ".rrrrWtWrrrr.....",
            ".rrrrWtWrrrr.....",
            ".rrrrrtrrrrrPPPPP",
            ".rrrrrtrrrrSPpppP",
            ".rrrrrrrrrrSPpxpP",
            ".rrryrrryrr.PpppP",
            ".rrrrrrrrrr.PpxpP",
            ".Srrrrrrrrr.PpppP",
            "..RRRRRRRRR.PPPPP",
            "..RRRR.RRRR......",
            "..RRRR.RRRR......",
            "..RRR...RRR......",
            "..RRR...RRR......",
            "..RRR...RRR......",
            ".kkkk...kkkk.....",
            ".kkkk...kkkk.....");
    private static final PixelSprite WALK = STAND.walkFrame(17);

    @Override
    protected PixelSprite getSprite(int frame, int tick) {
        return frame == 0 ? STAND : WALK;
    }
}
