package model.units;

import game.Battle;
import model.Combatant;
import model.Role;
import model.Side;
import model.Unit;
import ui.pixel.PixelSprite;

/**
 * Hospitality - fast and cheap, but low HP. Carries a tray.
 * Ability TIP JAR: every hit earns you +2 energy.
 */
public class Waiter extends Unit {

    private static final double TIP = 2;

    public Waiter(Side side) {
        //      name     role       side  hp   dmg  atkInterval range speed
        super("Waiter", Role.FAST, side, 80, 11, 0.7, 10, 30);
    }

    @Override
    protected void onHit(Battle battle, Combatant target) {
        battle.addEnergy(side, TIP);
    }

    @Override
    public String getAbilityName() {
        return "TIP JAR";
    }

    @Override
    public String getAbilityText() {
        return "EVERY HIT GIVES YOU +2 ENERGY";
    }

    // ---- Looks (pixel art: each letter is one pixel, '.' = see-through) ----

    private static final String COLORS = "r=DC2626 R=991B1B d=E5E7EB D=9CA3AF t=6B7280";

    private static final PixelSprite STAND = new PixelSprite(5, COLORS,
            "............D.....",
            "..........dddd....",
            ".........dWddDd...",
            "........dWddddDd..",
            "........ddddddDD..",
            ".......ttttttttttt",
            "...hhhhh...SS.....",
            "..hhhhhhh..SS.....",
            "..hhSSSSS..WW.....",
            "..hSSSSeS..WW.....",
            "..hSSSSSS.WW......",
            "...SSSmSs.WW......",
            "....SSSS.WW.......",
            "...WWkkWWWW.......",
            "..rRWWWRrW........",
            "..rrRWkRrr........",
            ".SrrrRWRrr........",
            ".SrrrRkRrr........",
            "..rrrRWRrr........",
            "..kkkkkkkk........",
            "..kkkk.kkk........",
            "..kkk..kkk........",
            "..kkk..kkk........",
            "..kkk..kkk........",
            ".KKKK..KKKK.......");
    private static final PixelSprite WALK = STAND.walkFrame(20);

    @Override
    protected PixelSprite getSprite(int frame, int tick) {
        return frame == 0 ? STAND : WALK;
    }
}
