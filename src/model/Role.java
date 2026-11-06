package model;

import java.awt.Color;

/**
 * Every unit has a role. Roles counter each other like rock-paper-scissors:
 *
 *      FAST  beats  RANGED   (runs in and hits the weak shooters)
 *      RANGED beats TANK     (shoots the slow tank from far away)
 *      TANK  beats  FAST     (too tough for the small, quick hits)
 *
 * A unit deals 1.5x damage to the role it beats, and only 0.7x to the role that beats it.
 */
public enum Role {

    FAST("FAST", new Color(0x4ADE80)),
    RANGED("RANGED", new Color(0x60A5FA)),
    TANK("TANK", new Color(0xF87171));

    public static final double STRONG = 1.5;
    public static final double WEAK = 0.7;

    private final String label;
    private final Color color;

    Role(String label, Color color) {
        this.label = label;
        this.color = color;
    }

    /** The role this one is strong against. */
    public Role beats() {
        return switch (this) {
            case FAST -> RANGED;
            case RANGED -> TANK;
            case TANK -> FAST;
        };
    }

    /** The role that is strong against this one. */
    public Role counteredBy() {
        return switch (this) {
            case FAST -> TANK;
            case RANGED -> FAST;
            case TANK -> RANGED;
        };
    }

    /** Damage multiplier when a unit of this role hits a unit of the target role. */
    public double multiplierAgainst(Role target) {
        if (target == null) {
            return 1.0; // towers have no role
        }
        if (beats() == target) {
            return STRONG;
        }
        if (counteredBy() == target) {
            return WEAK;
        }
        return 1.0;
    }

    public String getLabel() {
        return label;
    }

    public Color getColor() {
        return color;
    }
}
