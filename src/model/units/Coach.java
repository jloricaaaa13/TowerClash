package model.units;

import game.Battle;
import game.effects.FloatText;
import java.awt.Color;
import model.Role;
import model.Side;
import model.Unit;
import ui.pixel.PixelSprite;

/**
 * Education - fast fighter, the P.E. coach.
 * Ability PEP TALK: when spawned, blows the whistle and ALL your units
 * move and attack 30% faster for 5 seconds.
 */
public class Coach extends Unit {

    private static final double HASTE = 0.3;
    private static final double HASTE_SECONDS = 5;

    public Coach(Side side) {
        //      name    role       side  hp  dmg atkInterval range speed
        super("Coach", Role.FAST, side, 90, 12, 0.8, 10, 30);
    }

    @Override
    public void onSpawn(Battle battle) {
        for (Unit ally : battle.unitsOf(side)) {
            ally.applyHaste(HASTE, HASTE_SECONDS);
        }
        battle.addEffect(new FloatText(x, Battle.GROUND_Y - bodyHeight() - 8, "TWEET!", new Color(0xFFE066)));
    }

    @Override
    public String getAbilityName() {
        return "PEP TALK";
    }

    @Override
    public String getAbilityText() {
        return "ON SPAWN: ALL UNITS 30% FASTER";
    }

    // ---- Looks (pixel art: each letter is one pixel, '.' = see-through) ----

    private static final String COLORS = "n=2563EB N=1E3A8A o=F97316 O=C2410C x=D1D5DB l=EF4444";

    private static final PixelSprite STAND = new PixelSprite(5, COLORS,
            "..nnnnnn...........",
            ".nnnnnnnn..........",
            ".nnnnnnnnnn........",
            "..hSSSSSS..........",
            "..hSSSSeS..........",
            "..hSSSSSS..........",
            "...SSSmSs..........",
            "....SlSS...........",
            "..nnlWWlnn.........",
            ".nnnnlWlnnn...OOO..",
            ".nnnWWxWWnn..ooooo.",
            ".nnnWnnnWnSSoOoooOo",
            ".nnnWWWWWn..ooooo..",
            ".nnnnWnnnn...OOO...",
            ".Snnnnnnnn.........",
            "..NNNNNNN..........",
            "..NNNN.NN..........",
            "..SSS..SS..........",
            "..SSS..SS..........",
            "..WWW..WW..........",
            ".kkkk..kkkk........");
    private static final PixelSprite WALK = STAND.walkFrame(16);

    @Override
    protected PixelSprite getSprite(int frame, int tick) {
        return frame == 0 ? STAND : WALK;
    }
}
