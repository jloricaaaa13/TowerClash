package model.units;

import game.Battle;
import model.Combatant;
import model.Projectile;
import model.Role;
import model.Side;
import model.Unit;
import ui.pixel.PixelSprite;

/**
 * Computer Science - ranged. A hacker in a dark hoodie who sends viruses.
 * Ability DATA THEFT: every hit steals 3 energy from the enemy and gives it to you.
 */
public class Hacker extends Unit {

    private static final double STEAL = 3;

    public Hacker(Side side) {
        //      name     role         side  hp  dmg atkInterval range speed
        super("Hacker", Role.RANGED, side, 80, 10, 1.5, 55, 16);
    }

    @Override
    protected void onHit(Battle battle, Combatant target) {
        double stolen = battle.drainEnergy(side.opponent(), STEAL);
        battle.addEnergy(side, stolen);
    }

    @Override
    protected Projectile.Kind projectileKind() {
        return Projectile.Kind.VIRUS;
    }

    @Override
    public String getAbilityName() {
        return "DATA THEFT";
    }

    @Override
    public String getAbilityText() {
        return "HITS STEAL 3 ENEMY ENERGY";
    }

    // ---- Looks (pixel art: each letter is one pixel, '.' = see-through) ----

    private static final String COLORS = "n=1F2937 N=111827 y=EAB308 l=4ADE80 r=EF4444 R=7F1D1D x=9CA3AF X=4B5563 q=0F172A";

    private static final PixelSprite STAND = new PixelSprite(5, COLORS,
            "...nnnnn.............",
            "..nnnnnnn............",
            ".nnnnnnnnn...........",
            ".nnNSSSSSn...........",
            ".nnNSllSln...........",
            ".nnNSSSSSn...........",
            ".nnNNSSSS............",
            ".nnnnnnnn....XXXXXXX.",
            ".nnnnnnnnn...XqqqqqX.",
            ".nnnnnnnnn...XqrRrqX.",
            ".nnyyyyyynn..XqRrRqX.",
            ".nnnnnnnnSS..XqrRrqX.",
            ".nnnnnnnnn.SSXqqqqqX.",
            ".nnnnnnnnn..xxxxxxxxx",
            ".Snnnnnnn..xxxxxxxx..",
            "..NNNNNNN............",
            "..NNN.NNN............",
            "..NNN.NNN............",
            "..NNN..NN............",
            "..NNN..NN............",
            ".kkkk.kkkk...........");
    private static final PixelSprite WALK = STAND.walkFrame(16);

    @Override
    protected PixelSprite getSprite(int frame, int tick) {
        return frame == 0 ? STAND : WALK;
    }
}
