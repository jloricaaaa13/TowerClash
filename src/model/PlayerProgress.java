package model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Everything the game remembers between matches:
 * coins, unlocked stages, stars, permanent tower levels, lineups, and records.
 */
public class PlayerProgress {

    public static final int MAX_TOWER_LEVEL = 5;
    private static final int[] TOWER_UPGRADE_COST = {0, 150, 300, 500, 800}; // coins to reach level 2..5

    private int coins;
    private int unlockedStage = 1;
    private int matchesPlayed;
    private int wins;
    private int losses;
    private int highScore;
    private int bestWinSeconds;     // 0 = no win yet
    private int bestEndlessSeconds;
    private int bestEndlessScore;
    private int lanWins;
    private int lanLosses;
    private final int[] stars = new int[Stage.ALL.size() + 1]; // index = stage number
    private final Map<Department, Integer> towerLevels = new EnumMap<>(Department.class);
    private final Map<Department, List<UnitType>> lineups = new EnumMap<>(Department.class);

    public PlayerProgress() {
        for (Department d : Department.values()) {
            towerLevels.put(d, 1);
            lineups.put(d, d.getDefaultLineup());
        }
    }

    /** Updates records and coins after a match, and says what was earned. */
    public MatchReward recordMatch(Match match) {
        matchesPlayed++;
        coins += match.getCoinsEarned();
        highScore = Math.max(highScore, match.getScore());
        Stage stage = match.getStage();

        if (stage.endless()) {
            boolean record = match.getSeconds() > bestEndlessSeconds;
            bestEndlessSeconds = Math.max(bestEndlessSeconds, match.getSeconds());
            bestEndlessScore = Math.max(bestEndlessScore, match.getScore());
            return new MatchReward(0, 0, 0, false, record);
        }

        if (!match.isPlayerWin()) {
            losses++;
            return new MatchReward(0, 0, 0, false, false);
        }

        wins++;
        if (bestWinSeconds == 0 || match.getSeconds() < bestWinSeconds) {
            bestWinSeconds = match.getSeconds();
        }
        int earned = match.getStars();
        int newStars = Math.max(0, earned - stars[stage.number()]);
        stars[stage.number()] = Math.max(stars[stage.number()], earned);
        int starCoins = newStars * Match.STAR_COINS;
        coins += starCoins;

        boolean unlocked = false;
        int next = stage.number() + 1;
        if (next <= Stage.ALL.size() && next > unlockedStage) {
            unlockedStage = next;
            unlocked = true;
        }
        return new MatchReward(earned, newStars, starCoins, unlocked, false);
    }

    /** A finished LAN 1v1 match (does not change campaign wins, coins or stars). */
    public void recordLanMatch(boolean won) {
        if (won) {
            lanWins++;
        } else {
            lanLosses++;
        }
    }

    public int getLanWins() {
        return lanWins;
    }

    public void setLanWins(int lanWins) {
        this.lanWins = lanWins;
    }

    public int getLanLosses() {
        return lanLosses;
    }

    public void setLanLosses(int lanLosses) {
        this.lanLosses = lanLosses;
    }

    public boolean isEndlessUnlocked() {
        return unlockedStage > Stage.ENDLESS_UNLOCK_AFTER;
    }

    // ---- Permanent tower upgrades ----

    public int getTowerLevel(Department d) {
        return towerLevels.get(d);
    }

    public void setTowerLevel(Department d, int level) {
        towerLevels.put(d, Math.max(1, Math.min(MAX_TOWER_LEVEL, level)));
    }

    /** Coins for the next level, or -1 if already maxed. */
    public int getTowerUpgradeCost(Department d) {
        int level = getTowerLevel(d);
        return level < MAX_TOWER_LEVEL ? TOWER_UPGRADE_COST[level] : -1;
    }

    public boolean upgradeTower(Department d) {
        int cost = getTowerUpgradeCost(d);
        if (cost < 0 || coins < cost) {
            return false;
        }
        coins -= cost;
        towerLevels.put(d, getTowerLevel(d) + 1);
        return true;
    }

    // ---- Lineups (the 3 units you bring into battle, one lineup per department) ----

    public List<UnitType> getLineup(Department d) {
        return lineups.get(d);
    }

    /**
     * Saves a lineup if it is valid: exactly 3 different units from that department.
     * @return false if the lineup was not valid (nothing is changed)
     */
    public boolean setLineup(Department d, List<UnitType> lineup) {
        if (lineup == null || lineup.size() != Tower.LINEUP_SIZE) {
            return false;
        }
        List<UnitType> checked = new ArrayList<>();
        for (UnitType type : lineup) {
            if (type == null || !d.getRoster().contains(type) || checked.contains(type)) {
                return false;
            }
            checked.add(type);
        }
        lineups.put(d, List.copyOf(checked));
        return true;
    }

    // ---- Stars ----

    public int getStars(int stageNumber) {
        return stageNumber >= 1 && stageNumber < stars.length ? stars[stageNumber] : 0;
    }

    public void setStars(int stageNumber, int value) {
        if (stageNumber >= 1 && stageNumber < stars.length) {
            stars[stageNumber] = Math.max(0, Math.min(3, value));
        }
    }

    public int getTotalStars() {
        int total = 0;
        for (int s : stars) {
            total += s;
        }
        return total;
    }

    // ---- Getters / setters (setters are used when loading a save file) ----

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public int getUnlockedStage() {
        return unlockedStage;
    }

    public void setUnlockedStage(int unlockedStage) {
        this.unlockedStage = unlockedStage;
    }

    public int getMatchesPlayed() {
        return matchesPlayed;
    }

    public void setMatchesPlayed(int matchesPlayed) {
        this.matchesPlayed = matchesPlayed;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public int getHighScore() {
        return highScore;
    }

    public void setHighScore(int highScore) {
        this.highScore = highScore;
    }

    public int getBestWinSeconds() {
        return bestWinSeconds;
    }

    public void setBestWinSeconds(int bestWinSeconds) {
        this.bestWinSeconds = bestWinSeconds;
    }

    public int getBestEndlessSeconds() {
        return bestEndlessSeconds;
    }

    public void setBestEndlessSeconds(int bestEndlessSeconds) {
        this.bestEndlessSeconds = bestEndlessSeconds;
    }

    public int getBestEndlessScore() {
        return bestEndlessScore;
    }

    public void setBestEndlessScore(int bestEndlessScore) {
        this.bestEndlessScore = bestEndlessScore;
    }
}
