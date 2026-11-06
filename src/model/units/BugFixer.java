package model.units;

import game.Battle;
import model.Combatant;
import model.Role;
import model.Side;
import model.Unit;
import ui.pixel.PixelSprite;

/**
 * Computer Science - fast unit with very quick wrench hits.
 * Ability PATCH: repairs itself for half of the damage it deals.
 */
public class BugFixer extends Unit {

    public BugFixer(Side side) {
        super("Bug Fixer", Role.FAST, side, 60, 6, 0.45, 10, 34);
    }

    @Override
    protected void onHit(Battle battle, Combatant target) {
        heal(damage * 0.5);
    }

    @Override
    public String getAbilityName() {
        return "PATCH";
    }

    @Override
    public String getAbilityText() {
        return "HEALS ITSELF WHEN IT HITS";
    }

    // ---- Looks (pixel art: each letter is one pixel, '.' = see-through) ----

    private static final String COLORS = "y=EAB308 Y=CA8A04 x=9CA3AF X=6B7280 n=374151 N=1F2937 l=4ADE80";

    private static final PixelSprite STAND = new PixelSprite(5, COLORS,
            "...YYYYY...........",
            "..YyyyyyY..........",
            "..YYYYYYYY.........",
            "..hSSSSSS..........",
            "..hSSSSeS..........",
            "..hSSSSSS..........",
            "...SSSmSs.....xx.xx",
            "....SSSS......xxxxx",
            "..nyyyyyyn.....xxx.",
            ".nnyyyyyynn.....xX.",
            ".nnyyllyynnSS...xX.",
            ".nnyyyyyynn.SS..xX.",
            ".nnyyyyyynn...SSxX.",
            ".Snnnnnnnn......xX.",
            "..nnnnnnnn.........",
            "..NNNN.NNN.........",
            "..NNN..NNN.........",
            "..NNN..NNN.........",
            "..NNN..NNN.........",
            ".kkkk..kkkk........");
    private static final PixelSprite WALK = STAND.walkFrame(15);

    @Override
    protected PixelSprite getSprite(int frame, int tick) {
        return frame == 0 ? STAND : WALK;
    }
}
