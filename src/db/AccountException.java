package db;

/** Something went wrong with an account (wrong password, name taken, database error...). The message is shown to the player. */
public class AccountException extends Exception {

    public AccountException(String message) {
        super(message);
    }

    public AccountException(String message, Throwable cause) {
        super(message, cause);
    }
}
