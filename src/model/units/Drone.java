package model.units;

import model.Role;
import model.Side;
import model.Unit;
import ui.pixel.PixelSprite;

/**
 * Computer Science - a small flying drone.
 * Ability AIR STRIKE: flies over enemy units and ONLY attacks the enemy tower.
 * Enemy units and towers can still shoot it down, and it has low HP.
 */
public class Drone extends Unit {

    public Drone(Side side) {
        //      name    role       side  hp  dmg atkInterval range speed
        super("Drone", Role.FAST, side, 65, 9, 0.9, 10, 36);
    }

    @Override
    public boolean targetsUnits() {
        return false;
    }

    @Override
    public String getAbilityName() {
        return "AIR STRIKE";
    }

    @Override
    public String getAbilityText() {
        return "FLIES PAST UNITS, HITS ONLY TOWERS";
    }

    // ---- Looks (pixel art: each letter is one pixel, '.' = see-through) ----

    private static final String COLORS = "y=EAB308 Y=CA8A04 x=9CA3AF X=6B7280 b=E5E7EB q=0F172A r=EF4444 c=38BDF8";

    private static final PixelSprite BLADES_WIDE = new PixelSprite(8, COLORS,
            "bbbbbbb...bbbbbbb",
            "...x.........x...",
            "...x.........x...",
            "...XXXyyyyyXXX...",
            "....yyyyyyyyy....",
            "...yyYyyyyYyyy...",
            "...yyyyyyyyyyy...",
            "....YYYYYYYYY....",
            "......XqqcX......",
            "......XqrqX......",
            ".......XXX.......");
    private static final PixelSprite BLADES_THIN = new PixelSprite(8, COLORS, // propellers mid-spin
            "..bbb.......bbb..",
            "...x.........x...",
            "...x.........x...",
            "...XXXyyyyyXXX...",
            "....yyyyyyyyy....",
            "...yyYyyyyYyyy...",
            "...yyyyyyyyyyy...",
            "....YYYYYYYYY....",
            "......XqqcX......",
            "......XqrqX......",
            ".......XXX.......");

    @Override
    protected PixelSprite getSprite(int frame, int tick) {
        return (tick / 2) % 2 == 0 ? BLADES_WIDE : BLADES_THIN; // spinning propellers
    }

    /** Flies above the ground and bobs up and down. */
    @Override
    protected int hoverHeight(int tick) {
        return 12 + (tick / 8) % 2;
    }
}
