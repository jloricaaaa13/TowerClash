package model.units;

import model.Role;
import model.Side;
import model.Unit;
import ui.pixel.PixelSprite;

/**
 * Hospitality - the tank. Lots of HP, slow, carries a big suitcase.
 * Ability LUGGAGE SHIELD: the big suitcase blocks 20% of all damage it takes.
 */
public class Bellboy extends Unit {

    private static final double BLOCK = 0.20;

    public Bellboy(Side side) {
        super("Bellboy", Role.TANK, side, 320, 14, 1.2, 12, 12);
    }

    /** Overrides how damage is received: only 80% gets through the suitcase. */
    @Override
    public double receiveDamage(double amount) {
        return super.receiveDamage(amount * (1 - BLOCK));
    }

    @Override
    public String getAbilityName() {
        return "LUGGAGE SHIELD";
    }

    @Override
    public String getAbilityText() {
        return "SUITCASE BLOCKS 20% OF DAMAGE";
    }

    // ---- Looks (pixel art: each letter is one pixel, '.' = see-through) ----

    private static final String COLORS = "r=DC2626 R=991B1B u=8B5A2B U=5C3A1A L=D9A066";

    private static final PixelSprite STAND = new PixelSprite(6, COLORS,
            "...RRRRRR..........",
            "..RyyyyyyR.........",
            "..RRRRRRRR.........",
            "..hSSSSSSS.........",
            "..hSSSSSeS.........",
            "..hSSSSSSS.........",
            "...SSSSmSs.........",
            "....SSSSS..........",
            "..rrrRRrrrr........",
            ".rrrryRyrrrr.LLL...",
            ".rrrrrRrrrrr.L.L...",
            ".rrrryRyrrrSuuuuuuu",
            ".rrrrrRrrrrSuUuuuUu",
            ".rrrryRyrrr.uuuuuuu",
            ".rrrrrRrrrr.uLLLLLu",
            ".Srrrrrrrrr.uuuuuuu",
            "..yyyyyyyyy.uUuuuUu",
            "..RRRRRRRRR.uuuuuuu",
            "..RRRR.RRRR........",
            "..RRRR.RRRR........",
            "..RRR...RRR........",
            "..RRR...RRR........",
            ".kkkk...kkkk.......",
            ".kkkk...kkkk.......");
    private static final PixelSprite WALK = STAND.walkFrame(18);

    @Override
    protected PixelSprite getSprite(int frame, int tick) {
        return frame == 0 ? STAND : WALK;
    }
}
