package model.units;

import game.Battle;
import model.Combatant;
import model.Role;
import model.Side;
import model.Unit;
import ui.pixel.PixelSprite;

/**
 * Education - the tank. Very high HP, slow.
 * Ability DETENTION: every 4th hit FREEZES the enemy unit for 0.8 seconds.
 */
public class Principal extends Unit {

    private static final int EVERY = 4;
    private static final double STUN_SECONDS = 0.8;

    public Principal(Side side) {
        super("Principal", Role.TANK, side, 330, 15, 1.3, 12, 11);
    }

    @Override
    protected void onHit(Battle battle, Combatant target) {
        if (target instanceof Unit enemy && getAttacksMade() % EVERY == 0) {
            enemy.freeze(STUN_SECONDS);
        }
    }

    @Override
    public String getAbilityName() {
        return "DETENTION";
    }

    @Override
    public String getAbilityText() {
        return "EVERY 4TH HIT FREEZES THE ENEMY";
    }

    // ---- Looks (pixel art: each letter is one pixel, '.' = see-through) ----

    private static final String COLORS = "n=1E3A8A N=172554 t=60A5FA G=D4D4D8 g=A1A1AA y=FACC15 Y=CA8A04 B=7C4A2D";

    private static final PixelSprite STAND = new PixelSprite(6, COLORS,
            "..GGGGGGG............",
            ".GGGGGGGGG...........",
            ".GgSSSSSSS...........",
            ".gSSSSSSeS...........",
            ".gSSSSSSSS...........",
            "..SSSSggggS..........",
            "...SSSSSSS...........",
            "..nnnWWWnnn......y...",
            ".nnnnWtWnnnn....yyy..",
            ".nnnnWtWnnnn...yyyyy.",
            ".nnnnntnnnnn..YyyyyyY",
            ".nnnnntnnnnnSSBBYyyyY",
            ".nnnnnnnnnnSS..YYYYY.",
            ".nnnnnnnnnn......Y...",
            ".nnnnnnnnnn..........",
            ".Snnnnnnnnn..........",
            "..NNNNNNNNN..........",
            "..NNNN.NNNN..........",
            "..NNNN.NNNN..........",
            "..NNN...NNN..........",
            "..NNN...NNN..........",
            "..NNN...NNN..........",
            ".kkkk...kkkk.........",
            ".kkkk...kkkk.........");
    private static final PixelSprite WALK = STAND.walkFrame(17);

    @Override
    protected PixelSprite getSprite(int frame, int tick) {
        return frame == 0 ? STAND : WALK;
    }
}
