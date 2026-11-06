package model.units;

import game.Battle;
import game.effects.Zap;
import model.Combatant;
import model.Projectile;
import model.Role;
import model.Side;
import model.Unit;
import ui.pixel.PixelSprite;

/**
 * Computer Science - ranged attacker that shoots lines of code from a laptop.
 * Ability OVERCLOCK: every 4th shot also zaps the nearest other enemy.
 */
public class Programmer extends Unit {

    private static final int EVERY = 4;
    private static final double ZAP_RANGE = 35;
    private static final double ZAP_DAMAGE = 0.6;

    public Programmer(Side side) {
        super("Programmer", Role.RANGED, side, 85, 12, 0.8, 55, 17);
    }

    @Override
    protected void onHit(Battle battle, Combatant target) {
        if (getAttacksMade() % EVERY != 0) {
            return;
        }
        Unit next = null;
        double best = ZAP_RANGE;
        for (Unit u : battle.unitsOf(side.opponent())) {
            double d = Math.abs(u.getX() - target.getX());
            if (u != target && d <= best) {
                best = d;
                next = u;
            }
        }
        if (next != null) {
            battle.addEffect(new Zap(battle.hitX(target), battle.hitY(target), battle.hitX(next), battle.hitY(next)));
            battle.damage(next, damage * ZAP_DAMAGE, side);
        }
    }

    @Override
    public String getAbilityName() {
        return "OVERCLOCK";
    }

    @Override
    public String getAbilityText() {
        return "EVERY 4TH SHOT ZAPS ONE MORE ENEMY";
    }

    @Override
    protected Projectile.Kind projectileKind() {
        return Projectile.Kind.CODE;
    }

    // ---- Looks (pixel art: each letter is one pixel, '.' = see-through) ----

    private static final String COLORS = "y=EAB308 Y=CA8A04 x=9CA3AF X=6B7280 q=0F172A l=4ADE80 L=16A34A n=1F2937 H=111827";

    private static final PixelSprite STAND = new PixelSprite(5, COLORS,
            "...hhhhh.............",
            "..hhhhhhh............",
            ".HhhSSSSS............",
            ".HhSSSSeS............",
            ".HhSSSSSS............",
            "..HSSSmSs............",
            "..YySSSS.....XXXXXXX.",
            ".YyyyyyyY....XqqqqqX.",
            ".YyyyyyyyY...XqlLlqX.",
            ".YyyyyyyyY...XqLllqX.",
            ".YyyyyyyySS..XqllLqX.",
            ".YyyyyyyyY.SSXqqqqqX.",
            ".YyyyyyyyY..xxxxxxxxx",
            ".Syyyyyyy..xxxxxxxx..",
            "..nnnnnnn............",
            "..nnn.nnn............",
            "..nnn.nnn............",
            "..nnn..nn............",
            "..nnn..nn............",
            ".kkkk.kkkk...........");
    private static final PixelSprite WALK = STAND.walkFrame(15);

    @Override
    protected PixelSprite getSprite(int frame, int tick) {
        return frame == 0 ? STAND : WALK;
    }
}
