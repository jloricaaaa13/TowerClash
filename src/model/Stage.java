package model;

import java.util.List;

/**
 * One campaign stage. Harder stages make the enemy SMARTER (counters your units,
 * saves energy for big waves, times its Ultimate) and only a little stronger.
 * Each stage also has its own look and a special rule (Modifier).
 * To add a stage, add one line to ALL.
 */
public record Stage(
        int number,
        String name,
        String description,
        int difficulty,          // 1 to 5 (shown as dots)
        Modifier modifier,
        StageTheme theme,
        double aiEnergyRate,     // 1.0 = same energy as the player
        double aiTowerHp,        // multiplier for the enemy tower's HP
        int aiTowerLevel,        // enemy's permanent tower level (1-3)
        int aiMaxUpgrades,       // highest in-match tower level the AI will buy (1 = never)
        double aiThinkTime,      // seconds between AI decisions
        double aiSkill,          // 0 = random choices, 1 = always counters your units
        int aiWaveEnergy,        // saves up this much energy before attacking (0 = never saves)
        int aiUltimateTargets,   // waits until you have this many units before using its Ultimate
        int rewardCoins,
        boolean endless) {

    /** Stars: 1 = win, 2 = your tower kept 50% HP, 3 = won in 2:00 or less. */
    public static final double STAR_HP = 0.5;
    public static final int STAR_SECONDS = 120;

    public static final List<Stage> ALL = List.of(
            new Stage(1, "Freshman Rush", "A new enemy still learning the ropes.", 1,
                    Modifier.NORMAL, StageTheme.DAY, 0.85, 0.90, 1, 1, 1.8, 0.10, 0, 1, 60, false),
            new Stage(2, "Sophomore Siege", "Everyone is in a hurry today.", 2,
                    Modifier.RUSH_HOUR, StageTheme.SUNSET, 0.95, 1.00, 1, 2, 1.2, 0.55, 50, 2, 80, false),
            new Stage(3, "Junior Clash", "The lights are out. Towers see less.", 3,
                    Modifier.POWER_OUTAGE, StageTheme.NIGHT, 0.95, 1.00, 2, 3, 1.1, 0.60, 70, 3, 100, false),
            new Stage(4, "Senior Showdown", "Exam week: the enemy is tougher.", 4,
                    Modifier.EXAM_WEEK, StageTheme.RAIN, 0.95, 1.05, 2, 3, 0.8, 0.80, 85, 3, 130, false),
            new Stage(5, "Dean's Gauntlet", "Finals. Short and brutal.", 5,
                    Modifier.FINALS, StageTheme.STORM, 1.00, 1.10, 3, 3, 0.6, 0.95, 95, 4, 180, false));

    /** Survive as long as you can: the enemy tower is huge and the enemy gets stronger every 30 seconds. */
    public static final Stage ENDLESS = new Stage(6, "Endless Siege", "Survive as long as you can!", 5,
            Modifier.NORMAL, StageTheme.NIGHT, 0.90, 4.00, 3, 3, 0.8, 0.85, 70, 3, 40, true);

    /** LAN 1v1: a player on another computer instead of the AI (the AI values are not used). */
    public static final Stage LAN = new Stage(7, "LAN 1v1", "Player against player on the same network.", 5,
            Modifier.NORMAL, StageTheme.DAY, 1.00, 1.00, 1, 3, 1.0, 1.0, 0, 3, 0, false);

    /** Endless mode opens after beating this stage. */
    public static final int ENDLESS_UNLOCK_AFTER = 3;

    public static Stage get(int number) {
        return ALL.get(Math.max(1, Math.min(ALL.size(), number)) - 1);
    }

    public boolean isLast() {
        return !endless && number == ALL.size();
    }
}
