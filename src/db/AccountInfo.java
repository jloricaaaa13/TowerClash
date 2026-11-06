package db;

import java.time.LocalDateTime;

/**
 * Account details shown on the Profile screen.
 *
 * @param createdAt when the account was made (null if unknown, e.g. very old local accounts)
 * @param matchCount how many matches are in the player's match history
 */
public record AccountInfo(int id, String username, String email, LocalDateTime createdAt, int matchCount) {
}
