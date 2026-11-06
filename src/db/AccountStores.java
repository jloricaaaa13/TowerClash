package db;

/**
 * Picks where accounts are saved when the game starts:
 * the MySQL database (on this computer or on the LAN server, see DatabaseConfig)
 * if it can be reached, otherwise files on this computer.
 */
public final class AccountStores {

    private static String lastProblem = "";

    private AccountStores() {
    }

    public static AccountStore connect() {
        try {
            AccountStore store = MySqlAccountStore.connect(DatabaseConfig.load());
            lastProblem = "";
            return store;
        } catch (AccountException e) {
            lastProblem = e.getMessage();
            System.err.println("MySQL not available, using local accounts: " + e.getMessage()
                    + (e.getCause() != null ? " (" + e.getCause().getMessage() + ")" : ""));
            return new LocalAccountStore();
        }
    }

    /** Why MySQL could not be used (empty if it is being used). */
    public static String getLastProblem() {
        return lastProblem;
    }
}
