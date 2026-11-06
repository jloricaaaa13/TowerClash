package model;

/** A special rule for a stage, so every stage plays a little differently. */
public enum Modifier {

    NORMAL("NORMAL DAY", "NO SPECIAL RULES."),
    RUSH_HOUR("RUSH HOUR", "ALL UNITS MOVE 25% FASTER."),
    POWER_OUTAGE("POWER OUTAGE", "BOTH TOWERS HAVE 20% LESS RANGE."),
    EXAM_WEEK("EXAM WEEK", "ENEMY UNITS HAVE 20% MORE HP."),
    FINALS("FINALS", "TOWERS START AT 70% HP, EARLY OVERTIME.");

    private final String title;
    private final String description;

    Modifier(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public double unitSpeed() {
        return this == RUSH_HOUR ? 1.25 : 1.0;
    }

    public double towerRange() {
        return this == POWER_OUTAGE ? 0.8 : 1.0;
    }

    public double enemyUnitHp() {
        return this == EXAM_WEEK ? 1.2 : 1.0;
    }

    public double towerStartHp() {
        return this == FINALS ? 0.7 : 1.0;
    }

    /** Seconds until overtime (double energy). */
    public double overtimeStart() {
        return this == FINALS ? 45 : 90;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
