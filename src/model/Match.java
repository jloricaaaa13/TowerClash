package model;

/**
 * Statistics of one battle, and the score / star / coin formulas.
 *
 * SCORE (campaign) =
 *     damage dealt
 *   + 30 per enemy unit defeated
 *   + (win only) 20 per % of tower HP left       (max 2000)
 *   + (win only) 10 per second faster than 5:00  (max 3000)
 *   then x1.0 on Stage 1 up to x1.4 on Stage 5
 *
 * SCORE (endless) = damage dealt + 30 per unit defeated + 10 per second survived
 */
public class Match {

    private static final int KILL_POINTS = 30;
    private static final int PAR_TIME_SECONDS = 300;
    public static final int STAR_COINS = 20;

    private final Stage stage;
    private final Department playerDepartment;
    private final Department enemyDepartment;
    private double elapsed;
    private double damageDealt;
    private int unitsDefeated;
    private int unitsLost;
    private Side winner;
    private double towerHpLeftRatio;

    public Match(Stage stage, Department playerDepartment, Department enemyDepartment) {
        this.stage = stage;
        this.playerDepartment = playerDepartment;
        this.enemyDepartment = enemyDepartment;
    }

    public void addTime(double dt) {
        elapsed += dt;
    }

    public void addDamage(double damage) {
        damageDealt += damage;
    }

    public void addUnitDefeated() {
        unitsDefeated++;
    }

    public void addUnitLost() {
        unitsLost++;
    }

    public void finish(Side winner, double playerTowerHpRatio) {
        this.winner = winner;
        this.towerHpLeftRatio = playerTowerHpRatio;
    }

    public boolean isPlayerWin() {
        return winner == Side.PLAYER;
    }

    public int getScore() {
        double score = damageDealt + unitsDefeated * KILL_POINTS;
        if (stage.endless()) {
            return (int) Math.round(score + getSeconds() * 10);
        }
        if (isPlayerWin()) {
            score += Math.round(towerHpLeftRatio * 100) * 20;
            score += Math.max(0, PAR_TIME_SECONDS - getSeconds()) * 10;
        }
        return (int) Math.round(score * (1 + 0.1 * (stage.number() - 1)));
    }

    /** 0-3 stars (endless mode has no stars). */
    public int getStars() {
        if (!isPlayerWin() || stage.endless()) {
            return 0;
        }
        int stars = 1;
        if (towerHpLeftRatio >= Stage.STAR_HP) {
            stars++;
        }
        if (getSeconds() <= Stage.STAR_SECONDS) {
            stars++;
        }
        return stars;
    }

    /** Campaign win: stage reward + score/200. Loss: 10 + score/400. Endless: stage reward + score/150. */
    public int getCoinsEarned() {
        if (stage.endless()) {
            return stage.rewardCoins() + getScore() / 150;
        }
        return isPlayerWin() ? stage.rewardCoins() + getScore() / 200 : 10 + getScore() / 400;
    }

    public int getSeconds() {
        return (int) elapsed;
    }

    public double getElapsed() {
        return elapsed;
    }

    public double getDamageDealt() {
        return damageDealt;
    }

    public int getUnitsDefeated() {
        return unitsDefeated;
    }

    public int getUnitsLost() {
        return unitsLost;
    }

    public double getTowerHpLeftRatio() {
        return towerHpLeftRatio;
    }

    public Stage getStage() {
        return stage;
    }

    public Department getPlayerDepartment() {
        return playerDepartment;
    }

    public Department getEnemyDepartment() {
        return enemyDepartment;
    }

    public Side getWinner() {
        return winner;
    }
}
