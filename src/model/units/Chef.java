package model.units;

import game.Battle;
import model.Combatant;
import model.Projectile;
import model.Role;
import model.Side;
import model.Unit;
import ui.pixel.PixelSprite;

/**
 * Hospitality - ranged. Throws sizzling hot pans.
 * Ability KITCHEN FIRE: hits BURN the target (damage over time).
 */
public class Chef extends Unit {

    private static final double BURN_DPS = 4;
    private static final double BURN_SECONDS = 3;

    public Chef(Side side) {
        super("Chef", Role.RANGED, side, 100, 13, 1.3, 45, 17);
    }

    @Override
    protected void onHit(Battle battle, Combatant target) {
        target.applyBurn(BURN_DPS, BURN_SECONDS, side);
    }

    @Override
    protected Projectile.Kind projectileKind() {
        return Projectile.Kind.PLATE;
    }

    @Override
    public String getAbilityName() {
        return "KITCHEN FIRE";
    }

    @Override
    public String getAbilityText() {
        return "HOT PANS BURN THE TARGET";
    }

    // ---- Looks (pixel art: each letter is one pixel, '.' = see-through) ----

    private static final String COLORS = "R=DC2626 o=FB923C f=FDE047 q=3F3F46 Q=71717A";

    private static final PixelSprite STAND = new PixelSprite(5, COLORS,
            "...WWWWW..............",
            "..WWWWWWW.............",
            ".WWWWWWWWW............",
            ".WWWWWWWWW............",
            "..WwWwWwW.............",
            "...wwwww..............",
            "..hSSSSSS.............",
            "..hSSSSeS.............",
            "..hSSSSSS.............",
            "...SSSmSs.........f...",
            "....SSSS........fo.f..",
            "..WWRRRWW......of.o...",
            ".WWWWRWWWW..qqqqqqqq..",
            ".WWkWWWkWWSSbbbQqqqqqq",
            ".WWWWWWWWW...qqqqqqq..",
            ".WWkWWWWWW............",
            ".SWWWWWWWW............",
            "..WWWWWWW.............",
            "..KKKKKKK.............",
            "..KKKK.KK.............",
            "..KKK..KKK............",
            "..KKK..KKK............",
            ".kkkk..kkkk...........");
    private static final PixelSprite WALK = STAND.walkFrame(19);

    @Override
    protected PixelSprite getSprite(int frame, int tick) {
        return frame == 0 ? STAND : WALK;
    }
}
