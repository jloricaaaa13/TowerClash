package model.units;

import game.Battle;
import model.Projectile;
import model.Role;
import model.Side;
import model.Unit;
import ui.pixel.PixelSprite;

/**
 * Education - ranged support. Carries a stack of books and throws them.
 * Ability QUIET PLEASE: enemy units near the Librarian are slowed by 30%.
 */
public class Librarian extends Unit {

    private static final double AURA_RANGE = 50;
    private static final double SLOW = 0.3;

    public Librarian(Side side) {
        //      name        role         side  hp  dmg atkInterval range speed
        super("Librarian", Role.RANGED, side, 110, 15, 1.4, 55, 16);
    }

    @Override
    protected void updateAbility(Battle battle, double dt) {
        for (Unit enemy : battle.unitsNear(side.opponent(), x, AURA_RANGE)) {
            enemy.applySlow(SLOW, 0.3); // refreshed every step while they stay close
        }
    }

    @Override
    protected Projectile.Kind projectileKind() {
        return Projectile.Kind.BOOK;
    }

    @Override
    public String getAbilityName() {
        return "QUIET PLEASE";
    }

    @Override
    public String getAbilityText() {
        return "SLOWS ALL ENEMIES NEARBY";
    }

    // ---- Looks (pixel art: each letter is one pixel, '.' = see-through) ----

    private static final String COLORS = "c=60A5FA C=3B82F6 n=1E3A8A N=172554 r=DC2626 g=16A34A y=EAB308 p=A855F7 h=6B3E1E";

    private static final PixelSprite STAND = new PixelSprite(5, COLORS,
            ".hh..............",
            "hhhhhhhh.........",
            "hhhhhhhhh........",
            ".hhhSSSSS........",
            "..hSkkkkk........",
            "..hSSSkSk........",
            "...SSSSmS........",
            "....SSSS.........",
            "...ccWWcc..rrrrrr",
            "..cccWWcccSgggggg",
            "..cCcWWcCc.yyyyyy",
            "..cccWWcccSpppppp",
            "..cCcccccC.rrrrrr",
            "..ccccccccc......",
            "..nnnnnnnn.......",
            "..nnnnnnnnn......",
            "..nnnnnnnnn......",
            "..NNNNNNNNN......",
            "....SS.SS........",
            "....SS.SS........",
            "...kkk.kkk.......");
    private static final PixelSprite WALK = STAND.walkFrame(18);

    @Override
    protected PixelSprite getSprite(int frame, int tick) {
        return frame == 0 ? STAND : WALK;
    }
}
