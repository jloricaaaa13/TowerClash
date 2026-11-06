package model.units;

import game.Battle;
import model.Combatant;
import model.Projectile;
import model.Role;
import model.Side;
import model.Unit;
import ui.pixel.PixelSprite;

/**
 * Education - ranged attacker.
 * Ability CHALK TOSS: hits SLOW the target by 25%.
 */
public class Teacher extends Unit {

    public Teacher(Side side) {
        super("Teacher", Role.RANGED, side, 95, 20, 1.4, 60, 16);
    }

    @Override
    public String getAbilityName() {
        return "CHALK TOSS";
    }

    @Override
    public String getAbilityText() {
        return "CHALK SLOWS THE TARGET";
    }

    @Override
    protected void onHit(Battle battle, Combatant target) {
        target.applySlow(0.25, 1.5);
    }

    @Override
    protected Projectile.Kind projectileKind() {
        return Projectile.Kind.CHALK;
    }

    // ---- Looks (pixel art: each letter is one pixel, '.' = see-through) ----

    private static final String COLORS = "n=2563EB N=1E40AF b=B91C1C B=7F1D1D x=52525B c=FFFFFF";

    private static final PixelSprite STAND = new PixelSprite(5, COLORS,
            "...hhhhh........",
            "..hhhhhhh.......",
            "..hhSSSSSS......",
            "..hSSkkkkk......",
            "..hSSSSkSk......",
            "...SSSSmS.......",
            "....SSSS......cc",
            "...nnWWnn....SS.",
            "..nnnnWnnn..SS..",
            "..nnnnWnnnSS....",
            "..nnnnWnnnBBBB..",
            "..nnnnWnnnbcbb..",
            "..nnnnWnnnbcbb..",
            "..nnnnWnnnbbbb..",
            "..nnnnnnnn......",
            "..nnnnnnnn......",
            "..NNNNNNNN......",
            "...xxx.xx.......",
            "...xxx.xx.......",
            "...xx..xx.......",
            "...xx..xx.......",
            "..kkk..kkk......");
    private static final PixelSprite WALK = STAND.walkFrame(17);

    @Override
    protected PixelSprite getSprite(int frame, int tick) {
        return frame == 0 ? STAND : WALK;
    }
}
