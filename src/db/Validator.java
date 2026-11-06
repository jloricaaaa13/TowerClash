package db;

import java.util.regex.Pattern;

/**
 * Rules for usernames, emails and passwords.
 * Each check returns an error message to show the player, or null if the input is OK.
 */
public final class Validator {

    public static final int USERNAME_MIN = 3;
    public static final int USERNAME_MAX = 16;
    public static final int PASSWORD_MIN = 6;
    public static final int PASSWORD_MAX = 32;
    public static final int EMAIL_MAX = 60;

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]+$");
    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");

    private Validator() {
    }

    public static String checkUsername(String username) {
        if (username == null || username.isBlank()) {
            return "PLEASE ENTER A USERNAME";
        }
        if (username.length() < USERNAME_MIN || username.length() > USERNAME_MAX) {
            return "USERNAME: " + USERNAME_MIN + " TO " + USERNAME_MAX + " CHARACTERS";
        }
        if (!USERNAME.matcher(username).matches()) {
            return "USERNAME: LETTERS, NUMBERS AND _ ONLY";
        }
        return null;
    }

    public static String checkEmail(String email) {
        if (email == null || email.isBlank()) {
            return "PLEASE ENTER YOUR EMAIL";
        }
        if (email.length() > EMAIL_MAX || !EMAIL.matcher(email).matches()) {
            return "EMAIL IS NOT VALID (EXAMPLE: JUAN@SCHOOL.EDU)";
        }
        return null;
    }

    public static String checkPassword(String password) {
        if (password == null || password.isEmpty()) {
            return "PLEASE ENTER A PASSWORD";
        }
        if (password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
            return "PASSWORD: " + PASSWORD_MIN + " TO " + PASSWORD_MAX + " CHARACTERS";
        }
        if (password.contains(" ")) {
            return "PASSWORD MUST NOT HAVE SPACES";
        }
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*[0-9].*")) {
            return "PASSWORD NEEDS LETTERS AND NUMBERS";
        }
        return null;
    }
}
