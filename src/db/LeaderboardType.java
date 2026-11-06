package db;

/** What the leaderboard is sorted by (the tabs on the Dashboard). */
public enum LeaderboardType {

    HIGH_SCORE("SCORE", "HIGHEST SCORE IN ONE MATCH", "high_score"),
    WINS("WINS", "MOST MATCHES WON", "wins"),
    STARS("STARS", "MOST CAMPAIGN STARS", "total_stars"),
    ENDLESS("ENDLESS", "LONGEST ENDLESS SURVIVAL", "best_endless_seconds");

    private final String tab;
    private final String description;
    private final String column; // column in the player_progress table

    LeaderboardType(String tab, String description, String column) {
        this.tab = tab;
        this.description = description;
        this.column = column;
    }

    public String getTab() {
        return tab;
    }

    public String getDescription() {
        return description;
    }

    public String getColumn() {
        return column;
    }

    /** How a value looks on the board, e.g. 154 seconds -> "2:34" for Endless. */
    public String format(int value) {
        return this == ENDLESS ? value / 60 + ":" + String.format("%02d", value % 60) : String.valueOf(value);
    }
}
