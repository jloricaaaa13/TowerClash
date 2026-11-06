package model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import model.towers.ComputerScienceTower;
import model.towers.EducationTower;
import model.towers.HospitalityTower;

/**
 * The three school departments. Each one becomes a tower in the game.
 * To add a new department later: add a line here, create its Tower subclass,
 * and add it to createTower() and getRoster().
 */
public enum Department {

    HOSPITALITY("Hospitality Management", "Hospitality", "Grand Hotel", new Color(0xDC2626), "Burn + Heal"),
    EDUCATION("Education", "Education", "Academy", new Color(0x2563EB), "Slow + Range"),
    COMPUTER_SCIENCE("Computer Science", "Comp Sci", "Server", new Color(0xEAB308), "Fast + Chain");

    private final String fullName;
    private final String shortName;
    private final String towerShortName;
    private final Color color;
    private final String style;

    Department(String fullName, String shortName, String towerShortName, Color color, String style) {
        this.fullName = fullName;
        this.shortName = shortName;
        this.towerShortName = towerShortName;
        this.color = color;
        this.style = style;
    }

    /** Creates this department's tower with the default lineup (first 3 units of the roster). */
    public Tower createTower(Side side, int permanentLevel, double hpMultiplier) {
        return createTower(side, permanentLevel, hpMultiplier, getDefaultLineup());
    }

    /**
     * Creates this department's tower with a chosen lineup of 3 units.
     * Polymorphism: the caller gets a plain "Tower" but it is really a
     * HospitalityTower, EducationTower, or ComputerScienceTower.
     */
    public Tower createTower(Side side, int permanentLevel, double hpMultiplier, List<UnitType> lineup) {
        return switch (this) {
            case HOSPITALITY -> new HospitalityTower(side, permanentLevel, hpMultiplier, lineup);
            case EDUCATION -> new EducationTower(side, permanentLevel, hpMultiplier, lineup);
            case COMPUTER_SCIENCE -> new ComputerScienceTower(side, permanentLevel, hpMultiplier, lineup);
        };
    }

    /** All 5 units of this department (the player picks 3 of them on the Lineup screen). */
    public List<UnitType> getRoster() {
        return switch (this) {
            case HOSPITALITY -> HospitalityTower.ROSTER;
            case EDUCATION -> EducationTower.ROSTER;
            case COMPUTER_SCIENCE -> ComputerScienceTower.ROSTER;
        };
    }

    /** The starting lineup: the first 3 units of the roster. */
    public List<UnitType> getDefaultLineup() {
        return getRoster().subList(0, Tower.LINEUP_SIZE);
    }

    /** Finds a unit of this department by its name (used when loading a saved lineup), or null. */
    public UnitType findUnit(String name) {
        for (UnitType type : getRoster()) {
            if (type.name().equalsIgnoreCase(name.trim())) {
                return type;
            }
        }
        return null;
    }

    /**
     * A lineup for the computer opponent: one FAST, one RANGED and one TANK unit
     * (randomly chosen when the department has more than one of a role),
     * so the AI can always answer your units with a counter.
     */
    public List<UnitType> randomEnemyLineup(Random random) {
        List<UnitType> lineup = new ArrayList<>();
        for (Role role : Role.values()) {
            List<UnitType> choices = new ArrayList<>();
            for (UnitType type : getRoster()) {
                if (type.create(Side.ENEMY).getRole() == role) {
                    choices.add(type);
                }
            }
            if (!choices.isEmpty()) {
                lineup.add(choices.get(random.nextInt(choices.size())));
            }
        }
        // Safety net: fill any empty slot with unused units
        for (UnitType type : getRoster()) {
            if (lineup.size() >= Tower.LINEUP_SIZE) {
                break;
            }
            if (!lineup.contains(type)) {
                lineup.add(type);
            }
        }
        return lineup;
    }

    public String getFullName() {
        return fullName;
    }

    public String getShortName() {
        return shortName;
    }

    /** e.g. "Grand Hotel" */
    public String getTowerShortName() {
        return towerShortName;
    }

    /** e.g. "Grand Hotel Tower" */
    public String getTowerName() {
        return towerShortName + " Tower";
    }

    public Color getColor() {
        return color;
    }

    /** Short description of how the tower plays, e.g. "Burn + Heal". */
    public String getStyle() {
        return style;
    }
}
