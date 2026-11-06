package model.units;

import game.Battle;
import game.effects.FloatText;
import java.awt.Color;
import model.Role;
import model.Side;
import model.Unit;
import ui.pixel.PixelSprite;

/**
 * Education - the cheapest and fastest unit. Comes in big groups.
 * Ability GRADUATION: if it survives for 15 seconds it graduates:
 * full HP, +25% max HP and +30% damage (and it gets a graduation cap).
 */
public class Student extends Unit {

    private static final double GRADUATE_AFTER = 15;

    private boolean graduated;

    public Student(Side side) {
        super("Student", Role.FAST, side, 60, 10, 0.7, 10, 32);
    }

    @Override
    protected void updateAbility(Battle battle, double dt) {
        if (!graduated && getAliveTime() >= GRADUATE_AFTER) {
            graduated = true;
            powerUp(1.3, 1.25);
            battle.addEffect(new FloatText(x, Battle.GROUND_Y - bodyHeight() - 8, "GRADUATED!", new Color(0xFFD86E)));
        }
    }

    public boolean isGraduated() {
        return graduated;
    }

    @Override
    public String getAbilityName() {
        return "GRADUATION";
    }

    @Override
    public String getAbilityText() {
        return "SURVIVE 15 SEC: GETS STRONGER";
    }

    // ---- Looks (pixel art: each letter is one pixel, '.' = see-through) ----

    private static final String COLORS = "n=1D4ED8 N=1E3A8A o=F2B33D O=C2850C p=FACC15 P=F472B6 x=3F3F46 i=60A5FA B=F5D0A9";

    private static final PixelSprite STAND = new PixelSprite(5, COLORS,
            "...hhhhh................",
            "..hhhhhhh...............",
            "..hhSSSSS...............",
            "..hSSSSeS...............",
            "..hSSSSSS...............",
            "...SSSmSs...............",
            "....SSSS................",
            "OooWWnWWW...............",
            "OoWWWnWWWW.PPpppppppBB..",
            "OoWWWnWWWSSPPpppppppBBxx",
            "OooWWnWWW..PPYYYYYYYBB..",
            "OoWWWiWWW...............",
            ".oWWWWWWW...............",
            "..NNNNNNN...............",
            "..NNN.NNN...............",
            "..NNN.NNN...............",
            "..NNN.NNN...............",
            ".kkkk.kkkk..............");
    private static final PixelSprite WALK = STAND.walkFrame(14);
    private static final PixelSprite GRAD_STAND = new PixelSprite(5, COLORS,
            "..kkkkkkkk..............",
            "....kkkk.y..............",
            "........y...............",
            "...hhhhh................",
            "..hhhhhhh...............",
            "..hhSSSSS...............",
            "..hSSSSeS...............",
            "..hSSSSSS...............",
            "...SSSmSs...............",
            "....SSSS................",
            "OooWWnWWW...............",
            "OoWWWnWWWW.PPpppppppBB..",
            "OoWWWnWWWSSPPpppppppBBxx",
            "OooWWnWWW..PPYYYYYYYBB..",
            "OoWWWiWWW...............",
            ".oWWWWWWW...............",
            "..NNNNNNN...............",
            "..NNN.NNN...............",
            "..NNN.NNN...............",
            "..NNN.NNN...............",
            ".kkkk.kkkk..............");
    private static final PixelSprite GRAD_WALK = GRAD_STAND.walkFrame(17);

    @Override
    protected PixelSprite getSprite(int frame, int tick) {
        if (graduated) {
            return frame == 0 ? GRAD_STAND : GRAD_WALK; // with a graduation cap
        }
        return frame == 0 ? STAND : WALK;
    }
}
