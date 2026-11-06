package model;

/**
 * What the player got from a match (shown on the results screen).
 * @param stars      stars earned in this match (0-3)
 * @param newStars   stars that are better than the previous best (each gives bonus coins)
 * @param starCoins  bonus coins from new stars
 * @param unlocked   true if a new stage was unlocked
 * @param newRecord  true if this was a new endless-mode record
 */
public record MatchReward(int stars, int newStars, int starCoins, boolean unlocked, boolean newRecord) {
}
