package db;

/** One row of the leaderboard. */
public record LeaderboardEntry(int rank, String username, int value) {
}
