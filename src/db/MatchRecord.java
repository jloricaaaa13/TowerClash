package db;

import java.time.LocalDateTime;
import model.Department;

/**
 * One row of the match history (one finished match), as read back from the database.
 *
 * @param stageNumber 1-5 = story stages, 6 = Endless, 7 = LAN 1v1
 * @param towerHpLeft percent 0-100
 */
public record MatchRecord(
        LocalDateTime playedAt,
        int stageNumber,
        Department playerTower,
        Department enemyTower,
        boolean win,
        int score,
        int seconds,
        int damageDealt,
        int unitsDefeated,
        int towerHpLeft,
        int coinsEarned) {

    public static final int ENDLESS = 6;
    public static final int LAN = 7;

    public boolean isEndless() {
        return stageNumber == ENDLESS;
    }

    public boolean isLan() {
        return stageNumber == LAN;
    }

    /** "STAGE 3", "ENDLESS" or "LAN 1V1". */
    public String getModeText() {
        return isLan() ? "LAN 1V1" : isEndless() ? "ENDLESS" : "STAGE " + stageNumber;
    }

    /** "WIN", "LOSS", or "SURVIVED" for Endless (it always ends when your tower falls). */
    public String getResultText() {
        return isEndless() ? "SURVIVED" : win ? "WIN" : "LOSS";
    }
}
