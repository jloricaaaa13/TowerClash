package db;

import java.util.List;
import model.Department;
import model.Match;
import model.PlayerProgress;

/**
 * Where accounts and progress are saved.
 *
 * There are two ways (both follow this same interface - polymorphism):
 *   MySqlAccountStore - the XAMPP MySQL database (the main way)
 *   LocalAccountStore - files on this computer, used automatically when MySQL is not running
 *
 * The rest of the game only talks to "AccountStore", so it does not care which one is used.
 */
public interface AccountStore {

    /** Shown on the Login screen and Dashboard, e.g. "MYSQL DATABASE". */
    String getName();

    /** true = MySQL database, false = files on this computer. */
    boolean isDatabase();

    Account register(String username, String email, String password) throws AccountException;

    Account login(String username, String password) throws AccountException;

    PlayerProgress loadProgress(Account account) throws AccountException;

    void saveProgress(Account account, PlayerProgress progress) throws AccountException;

    /** Remembers one finished match (match history). */
    void recordMatch(Account account, Match match) throws AccountException;

    /** Remembers one finished LAN 1v1 match against another player. */
    void recordLanMatch(Account account, Department mine, Department theirs, boolean won, int seconds)
            throws AccountException;

    /** The best players, best first. */
    List<LeaderboardEntry> getLeaderboard(LeaderboardType type, int limit) throws AccountException;

    /** The player's place on the leaderboard (1 = first), or 0 if they have no score yet. */
    int getRank(Account account, LeaderboardType type) throws AccountException;

    // ---- Profile and match history ----

    /** Details for the Profile screen (join date, number of matches...). */
    AccountInfo getAccountInfo(Account account) throws AccountException;

    /**
     * Changes the password. The current password must be right first.
     * @throws AccountException "CURRENT PASSWORD IS WRONG" or a database error
     */
    void changePassword(Account account, String currentPassword, String newPassword) throws AccountException;

    /** How many matches are in the player's history. */
    int countMatches(Account account) throws AccountException;

    /**
     * The player's matches, NEWEST first.
     * @param offset how many of the newest matches to skip (for pages: page * pageSize)
     * @param limit  how many to return
     */
    List<MatchRecord> getMatchHistory(Account account, int offset, int limit) throws AccountException;

    // ---- Forgot password (security question) ----

    /** The account with this username OR email, or null if there is none. */
    Account findAccount(String usernameOrEmail) throws AccountException;

    /** Sets a new password without asking for the old one (only after a correct security answer). */
    void setPassword(Account account, String newPassword) throws AccountException;

    /** The security question, hashed answer and wrong-answer counter (question = null if none). */
    SecurityInfo getSecurityInfo(Account account) throws AccountException;

    /** Saves the security question and the HASH of the answer. */
    void setSecurityQuestion(Account account, String question, String answerHash, String salt) throws AccountException;

    /** Saves the wrong-answer counter and the lock time (0 = not locked). */
    void setResetAttempts(Account account, int attempts, long lockedUntil) throws AccountException;
}
