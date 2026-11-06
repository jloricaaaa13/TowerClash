package db;

import java.util.List;
import java.util.Locale;

/**
 * FORGOT PASSWORD with a SECURITY QUESTION (no email or internet needed).
 *
 *   When registering, the player picks a question and types an answer.
 *   The answer is saved as a hash (like a password).
 *
 *   To reset: type the username or email -> the question is shown -> type the answer and a
 *   new password. After 5 wrong answers, resetting is blocked for 15 minutes so nobody can
 *   keep guessing.
 *
 * Answers are not case-sensitive and extra spaces are ignored ("San  Pablo " = "san pablo").
 */
public final class PasswordReset {

    public static final List<String> QUESTIONS = List.of(
            "NAME OF YOUR FIRST PET?",
            "MIDDLE NAME OF YOUR MOTHER?",
            "CITY WHERE YOU WERE BORN?",
            "YOUR ELEMENTARY SCHOOL?",
            "YOUR CHILDHOOD NICKNAME?",
            "YOUR FAVORITE FOOD?",
            "NAME OF YOUR BEST FRIEND?");

    public static final int ANSWER_MIN = 2;
    public static final int ANSWER_MAX = 40;
    public static final int MAX_ATTEMPTS = 5;
    public static final int LOCK_MINUTES = 15;

    /** The account and its question, after step 1 of FORGOT PASSWORD. */
    public record Start(Account account, String question) {
    }

    private PasswordReset() {
    }

    /** null if the answer is OK, otherwise the problem to show. */
    public static String checkAnswer(String answer) {
        String a = normalize(answer);
        if (a.length() < ANSWER_MIN) {
            return "PLEASE TYPE AN ANSWER (" + ANSWER_MIN + "+ CHARACTERS)";
        }
        if (a.length() > ANSWER_MAX) {
            return "ANSWER: AT MOST " + ANSWER_MAX + " CHARACTERS";
        }
        return null;
    }

    /** Saves (or changes) the question and the hashed answer. */
    public static void setQuestion(AccountStore store, Account account, String question, String answer)
            throws AccountException {
        String salt = PasswordUtil.generateSalt();
        store.setSecurityQuestion(account, question, PasswordUtil.hashPassword(normalize(answer), salt), salt);
    }

    /** Step 1: finds the account and returns its question. */
    public static Start start(AccountStore store, String usernameOrEmail) throws AccountException {
        String key = usernameOrEmail == null ? "" : usernameOrEmail.trim();
        if (key.isEmpty()) {
            throw new AccountException("ENTER YOUR USERNAME OR EMAIL");
        }
        Account account = store.findAccount(key);
        if (account == null) {
            throw new AccountException("NO ACCOUNT WITH THAT USERNAME OR EMAIL");
        }
        SecurityInfo info = store.getSecurityInfo(account);
        if (!info.hasQuestion()) {
            throw new AccountException("THIS ACCOUNT HAS NO SECURITY QUESTION YET");
        }
        checkLock(info);
        return new Start(account, info.question());
    }

    /** Step 2: checks the answer and saves the new password (already checked by the screen). */
    public static void resetPassword(AccountStore store, Account account, String answer, String newPassword)
            throws AccountException {
        SecurityInfo info = store.getSecurityInfo(account);
        checkLock(info);
        if (!PasswordUtil.verifyPassword(normalize(answer), info.salt(), info.answerHash())) {
            int attempts = info.attempts() + 1;
            if (attempts >= MAX_ATTEMPTS) {
                store.setResetAttempts(account, 0, System.currentTimeMillis() + LOCK_MINUTES * 60_000L);
                throw new AccountException("TOO MANY WRONG ANSWERS. TRY AGAIN IN " + LOCK_MINUTES + " MINUTES");
            }
            store.setResetAttempts(account, attempts, 0);
            int left = MAX_ATTEMPTS - attempts;
            throw new AccountException("WRONG ANSWER (" + left + (left == 1 ? " TRY" : " TRIES") + " LEFT)");
        }
        store.setPassword(account, newPassword);
        store.setResetAttempts(account, 0, 0);
    }

    private static void checkLock(SecurityInfo info) throws AccountException {
        long wait = info.lockedUntil() - System.currentTimeMillis();
        if (wait > 0) {
            long minutes = (wait + 59_999) / 60_000;
            throw new AccountException("TOO MANY WRONG ANSWERS. TRY AGAIN IN " + minutes
                    + (minutes == 1 ? " MINUTE" : " MINUTES"));
        }
    }

    /** "  San   Pablo " -> "san pablo" */
    public static String normalize(String answer) {
        return answer == null ? "" : answer.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
