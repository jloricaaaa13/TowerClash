package db;

/**
 * A player's security question (used by FORGOT PASSWORD).
 * The answer is stored like a password: only SHA-256(salt + answer), never the answer itself.
 *
 * @param question    the question text, or null if the player has not set one yet
 * @param attempts    wrong answers in a row
 * @param lockedUntil System.currentTimeMillis() until which resetting is blocked (0 = not locked)
 */
public record SecurityInfo(String question, String answerHash, String salt, int attempts, long lockedUntil) {

    public boolean hasQuestion() {
        return question != null && !question.isBlank();
    }
}
