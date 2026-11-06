package db;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Hashes and checks passwords with SHA-256 and a random salt.
 * The real password is NEVER saved anywhere.
 *
 *   1. When an account is created, a random 16-byte "salt" is made for that user.
 *   2. hash = SHA-256(salt + password), saved as 64 hex characters.
 *   3. On login, the typed password is hashed again with the same salt.
 *      If both hashes are the same, the password is correct.
 *
 * The salt makes two players with the same password get different hashes.
 */
public final class PasswordUtil {

    private static final int SALT_LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    /** A new random salt in Base64 (24 characters). */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /** SHA-256(salt + password) as 64 hex characters. */
    public static String hashPassword(String password, String saltBase64) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Base64.getDecoder().decode(saltBase64));
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e); // never happens in normal Java
        }
    }

    /** Checks a typed password against the saved hash and salt. */
    public static boolean verifyPassword(String password, String saltBase64, String storedHash) {
        String typed = hashPassword(password, saltBase64);
        // MessageDigest.isEqual takes the same time for every input (safer than String.equals)
        return MessageDigest.isEqual(typed.getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8));
    }
}
