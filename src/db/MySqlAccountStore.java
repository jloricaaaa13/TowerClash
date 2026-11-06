package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import model.Department;
import model.Match;
import model.PlayerProgress;

/**
 * Saves accounts and progress in the MySQL database "tower_battle_db".
 *
 * Setup: just start MySQL in the XAMPP Control Panel. The first time the game runs,
 * it creates the database and the tables by itself (the same tables as sql/tower_battle_db.sql).
 * The database can be on THIS computer or on another computer in the LAN (see DatabaseConfig),
 * so a whole class can share one leaderboard.
 *
 * Every query uses PreparedStatement (the ? marks), which protects against SQL injection.
 */
public class MySqlAccountStore implements AccountStore {

    private static final String DATABASE = "tower_battle_db";
    private static final String OPTIONS = "?useSSL=false&allowPublicKeyRetrieval=true"
            + "&serverTimezone=Asia/Manila&connectTimeout=3000";

    private final DatabaseConfig config;
    private final String databaseUrl;

    private MySqlAccountStore(DatabaseConfig config) {
        this.config = config;
        this.databaseUrl = "jdbc:mysql://" + config.getHost() + ":" + config.getPort() + "/" + DATABASE + OPTIONS;
    }

    /**
     * Connects to MySQL and makes sure the database and tables exist.
     * @throws AccountException if MySQL is not running or the driver JAR is missing
     */
    public static MySqlAccountStore connect(DatabaseConfig config) throws AccountException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new AccountException("MYSQL DRIVER (JAR) NOT FOUND IN LIBRARIES", e);
        }
        String serverUrl = "jdbc:mysql://" + config.getHost() + ":" + config.getPort() + "/" + OPTIONS;
        try (Connection con = DriverManager.getConnection(serverUrl, config.getUser(), config.getPassword());
             Statement st = con.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DATABASE
                    + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        } catch (SQLException e) {
            throw new AccountException(explain(e, config), e);
        }
        MySqlAccountStore store = new MySqlAccountStore(config);
        store.createTables();
        return store;
    }

    /** A short reason the player can understand. */
    private static String explain(SQLException e, DatabaseConfig config) {
        String text = String.valueOf(e.getMessage()).toLowerCase();
        if (text.contains("access denied")) {
            return "WRONG DATABASE USER OR PASSWORD";
        }
        if (text.contains("is not allowed to connect")) {
            return "SERVER DOES NOT ALLOW THIS COMPUTER (SEE LAN_SETUP)";
        }
        return config.isRemote() ? "CANNOT REACH MYSQL AT " + config.getHost().toUpperCase()
                : "MYSQL IS NOT RUNNING (START IT IN XAMPP)";
    }

    private Connection open() throws SQLException {
        return DriverManager.getConnection(databaseUrl, config.getUser(), config.getPassword());
    }

    /** Creates the tables if they are not there yet (safe to run every time). */
    private void createTables() throws AccountException {
        String[] sql = {
            "CREATE TABLE IF NOT EXISTS users ("
                + " user_id INT NOT NULL AUTO_INCREMENT,"
                + " username VARCHAR(20) NOT NULL,"
                + " email VARCHAR(100) NOT NULL,"
                + " password_hash CHAR(64) NOT NULL COMMENT 'SHA-256 hash (hex)',"
                + " salt VARCHAR(44) NOT NULL COMMENT 'random salt (Base64)',"
                + " created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + " last_login DATETIME NULL,"
                + " security_question VARCHAR(100) NULL,"
                + " security_answer_hash CHAR(64) NULL COMMENT 'SHA-256 of the answer (never the answer itself)',"
                + " security_salt VARCHAR(44) NULL,"
                + " reset_attempts INT NOT NULL DEFAULT 0 COMMENT 'wrong answers in a row',"
                + " reset_locked_until BIGINT NOT NULL DEFAULT 0 COMMENT 'milliseconds; 0 = not locked',"
                + " PRIMARY KEY (user_id),"
                + " UNIQUE KEY uq_users_username (username),"
                + " UNIQUE KEY uq_users_email (email)) ENGINE = InnoDB",
            "CREATE TABLE IF NOT EXISTS towers ("
                + " tower_code VARCHAR(20) NOT NULL, tower_name VARCHAR(50) NOT NULL,"
                + " department VARCHAR(60) NOT NULL, short_name VARCHAR(20) NOT NULL,"
                + " color_hex CHAR(7) NOT NULL, PRIMARY KEY (tower_code)) ENGINE = InnoDB",
            "INSERT IGNORE INTO towers VALUES"
                + " ('HOSPITALITY','Grand Hotel Tower','Hospitality Management','Hospitality','#DC2626'),"
                + " ('EDUCATION','Academy Tower','Education','Education','#2563EB'),"
                + " ('COMPSCI','Server Tower','Computer Science','Comp Sci','#EAB308')",
            "CREATE TABLE IF NOT EXISTS stages ("
                + " stage_number INT NOT NULL, stage_name VARCHAR(50) NOT NULL,"
                + " description VARCHAR(150) NOT NULL, difficulty TINYINT NOT NULL,"
                + " ai_multiplier DECIMAL(4,2) NOT NULL DEFAULT 1.00, reward_coins INT NOT NULL DEFAULT 50,"
                + " PRIMARY KEY (stage_number)) ENGINE = InnoDB",
            "INSERT IGNORE INTO stages VALUES"
                + " (1,'Freshman Rush','A new enemy still learning the ropes.',1,1.00,60),"
                + " (2,'Sophomore Siege','Everyone is in a hurry today.',2,1.20,80),"
                + " (3,'Junior Clash','The lights are out. Towers see less.',3,1.45,100),"
                + " (4,'Senior Showdown','Exam week: the enemy is tougher.',4,1.75,130),"
                + " (5,'Dean''s Gauntlet','Finals. Short and brutal.',5,2.10,180),"
                + " (6,'Endless Siege','Survive as long as you can!',5,1.00,40),"
                + " (7,'LAN 1v1','Player against player on the same network.',5,1.00,0)",
            "CREATE TABLE IF NOT EXISTS player_progress ("
                + " user_id INT NOT NULL,"
                + " coins INT NOT NULL DEFAULT 0,"
                + " unlocked_stage INT NOT NULL DEFAULT 1,"
                + " matches_played INT NOT NULL DEFAULT 0,"
                + " wins INT NOT NULL DEFAULT 0,"
                + " losses INT NOT NULL DEFAULT 0,"
                + " high_score INT NOT NULL DEFAULT 0,"
                + " best_win_seconds INT NOT NULL DEFAULT 0,"
                + " total_stars INT NOT NULL DEFAULT 0,"
                + " best_endless_seconds INT NOT NULL DEFAULT 0,"
                + " best_endless_score INT NOT NULL DEFAULT 0,"
                + " save_data TEXT NULL COMMENT 'everything else: tower levels, lineups, stars per stage',"
                + " updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + " PRIMARY KEY (user_id),"
                + " CONSTRAINT fk_progress_user FOREIGN KEY (user_id) REFERENCES users (user_id)"
                + " ON DELETE CASCADE) ENGINE = InnoDB",
            "CREATE TABLE IF NOT EXISTS match_history ("
                + " match_id INT NOT NULL AUTO_INCREMENT,"
                + " user_id INT NOT NULL,"
                + " stage_number INT NOT NULL,"
                + " player_tower VARCHAR(20) NOT NULL,"
                + " enemy_tower VARCHAR(20) NOT NULL,"
                + " result ENUM('WIN','LOSS') NOT NULL,"
                + " score INT NOT NULL DEFAULT 0,"
                + " duration_seconds INT NOT NULL DEFAULT 0,"
                + " damage_dealt INT NOT NULL DEFAULT 0,"
                + " units_defeated INT NOT NULL DEFAULT 0,"
                + " tower_hp_left INT NOT NULL DEFAULT 0 COMMENT 'percent 0-100',"
                + " coins_earned INT NOT NULL DEFAULT 0,"
                + " played_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + " PRIMARY KEY (match_id),"
                + " KEY idx_match_user (user_id, played_at),"
                + " CONSTRAINT fk_match_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,"
                + " CONSTRAINT fk_match_ptower FOREIGN KEY (player_tower) REFERENCES towers (tower_code),"
                + " CONSTRAINT fk_match_etower FOREIGN KEY (enemy_tower) REFERENCES towers (tower_code),"
                + " CONSTRAINT fk_match_stage FOREIGN KEY (stage_number) REFERENCES stages (stage_number)"
                + ") ENGINE = InnoDB",
            "DROP TABLE IF EXISTS password_resets" // from the email version, no longer used
        };
        try (Connection con = open(); Statement st = con.createStatement()) {
            for (String s : sql) {
                st.executeUpdate(s);
            }
            // Databases made by an older version of the game: add the security question columns
            addColumnIfMissing(con, "security_question", "VARCHAR(100) NULL");
            addColumnIfMissing(con, "security_answer_hash", "CHAR(64) NULL");
            addColumnIfMissing(con, "security_salt", "VARCHAR(44) NULL");
            addColumnIfMissing(con, "reset_attempts", "INT NOT NULL DEFAULT 0");
            addColumnIfMissing(con, "reset_locked_until", "BIGINT NOT NULL DEFAULT 0");
        } catch (SQLException e) {
            throw new AccountException("COULD NOT CREATE THE DATABASE TABLES", e);
        }
    }

    /** Adds a column to the users table if it is not there yet (works on MySQL and MariaDB). */
    private static void addColumnIfMissing(Connection con, String column, String definition) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM information_schema.COLUMNS"
                + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = ?")) {
            ps.setString(1, column);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0) {
                    return;
                }
            }
        }
        try (Statement st = con.createStatement()) {
            st.executeUpdate("ALTER TABLE users ADD COLUMN " + column + " " + definition); // names are our own constants
        }
    }

    @Override
    public String getName() {
        return config.isRemote() ? "MYSQL AT " + config.getHost().toUpperCase() : "MYSQL DATABASE";
    }

    @Override
    public boolean isDatabase() {
        return true;
    }

    // ------------------------------------------------------------------
    //  Accounts
    // ------------------------------------------------------------------

    @Override
    public Account register(String username, String email, String password) throws AccountException {
        try (Connection con = open()) {
            if (exists(con, "SELECT 1 FROM users WHERE username = ?", username)) {
                throw new AccountException("USERNAME IS ALREADY TAKEN");
            }
            if (exists(con, "SELECT 1 FROM users WHERE email = ?", email)) {
                throw new AccountException("EMAIL IS ALREADY USED BY ANOTHER ACCOUNT");
            }
            String salt = PasswordUtil.generateSalt();
            String hash = PasswordUtil.hashPassword(password, salt);
            int id;
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO users (username, email, password_hash, salt) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, username);
                ps.setString(2, email);
                ps.setString(3, hash);
                ps.setString(4, salt);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    id = keys.getInt(1);
                }
            }
            Account account = new Account(id, username, email);
            saveProgress(account, new PlayerProgress());
            return account;
        } catch (SQLException e) {
            throw new AccountException("DATABASE ERROR WHILE CREATING THE ACCOUNT", e);
        }
    }

    @Override
    public Account login(String username, String password) throws AccountException {
        try (Connection con = open();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT user_id, username, email, password_hash, salt FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !PasswordUtil.verifyPassword(password, rs.getString("salt"),
                        rs.getString("password_hash"))) {
                    throw new AccountException("WRONG USERNAME OR PASSWORD");
                }
                Account account = new Account(rs.getInt("user_id"), rs.getString("username"), rs.getString("email"));
                try (PreparedStatement up = con.prepareStatement(
                        "UPDATE users SET last_login = NOW() WHERE user_id = ?")) {
                    up.setInt(1, account.id());
                    up.executeUpdate();
                }
                return account;
            }
        } catch (SQLException e) {
            throw new AccountException("DATABASE ERROR WHILE LOGGING IN", e);
        }
    }

    private static boolean exists(Connection con, String sql, String value) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ------------------------------------------------------------------
    //  Progress
    // ------------------------------------------------------------------

    @Override
    public PlayerProgress loadProgress(Account account) throws AccountException {
        try (Connection con = open();
             PreparedStatement ps = con.prepareStatement("SELECT save_data FROM player_progress WHERE user_id = ?")) {
            ps.setInt(1, account.id());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? ProgressCodec.fromText(rs.getString(1)) : new PlayerProgress();
            }
        } catch (SQLException e) {
            throw new AccountException("COULD NOT LOAD YOUR PROGRESS", e);
        }
    }

    @Override
    public void saveProgress(Account account, PlayerProgress p) throws AccountException {
        String sql = "INSERT INTO player_progress (user_id, coins, unlocked_stage, matches_played, wins, losses,"
                + " high_score, best_win_seconds, total_stars, best_endless_seconds, best_endless_score, save_data)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE coins = VALUES(coins), unlocked_stage = VALUES(unlocked_stage),"
                + " matches_played = VALUES(matches_played), wins = VALUES(wins), losses = VALUES(losses),"
                + " high_score = VALUES(high_score), best_win_seconds = VALUES(best_win_seconds),"
                + " total_stars = VALUES(total_stars), best_endless_seconds = VALUES(best_endless_seconds),"
                + " best_endless_score = VALUES(best_endless_score), save_data = VALUES(save_data)";
        try (Connection con = open(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, account.id());
            ps.setInt(2, p.getCoins());
            ps.setInt(3, p.getUnlockedStage());
            ps.setInt(4, p.getMatchesPlayed());
            ps.setInt(5, p.getWins());
            ps.setInt(6, p.getLosses());
            ps.setInt(7, p.getHighScore());
            ps.setInt(8, p.getBestWinSeconds());
            ps.setInt(9, p.getTotalStars());
            ps.setInt(10, p.getBestEndlessSeconds());
            ps.setInt(11, p.getBestEndlessScore());
            ps.setString(12, ProgressCodec.toText(p));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccountException("COULD NOT SAVE YOUR PROGRESS", e);
        }
    }

    @Override
    public void recordMatch(Account account, Match m) throws AccountException {
        String sql = "INSERT INTO match_history (user_id, stage_number, player_tower, enemy_tower, result, score,"
                + " duration_seconds, damage_dealt, units_defeated, tower_hp_left, coins_earned)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = open(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, account.id());
            ps.setInt(2, m.getStage().number());
            ps.setString(3, towerCode(m.getPlayerDepartment()));
            ps.setString(4, towerCode(m.getEnemyDepartment()));
            ps.setString(5, m.isPlayerWin() ? "WIN" : "LOSS");
            ps.setInt(6, m.getScore());
            ps.setInt(7, m.getSeconds());
            ps.setInt(8, (int) m.getDamageDealt());
            ps.setInt(9, m.getUnitsDefeated());
            ps.setInt(10, (int) Math.round(m.getTowerHpLeftRatio() * 100));
            ps.setInt(11, m.getCoinsEarned());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccountException("COULD NOT SAVE THE MATCH HISTORY", e);
        }
    }

    @Override
    public void recordLanMatch(Account account, Department mine, Department theirs, boolean won, int seconds)
            throws AccountException {
        String sql = "INSERT INTO match_history (user_id, stage_number, player_tower, enemy_tower, result,"
                + " duration_seconds) VALUES (?, 7, ?, ?, ?, ?)";
        try (Connection con = open(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, account.id());
            ps.setString(2, towerCode(mine));
            ps.setString(3, towerCode(theirs));
            ps.setString(4, won ? "WIN" : "LOSS");
            ps.setInt(5, seconds);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccountException("COULD NOT SAVE THE MATCH HISTORY", e);
        }
    }

    /** The code used in the towers table. */
    private static String towerCode(Department d) {
        return d == Department.COMPUTER_SCIENCE ? "COMPSCI" : d.name();
    }

    // ------------------------------------------------------------------
    //  Leaderboard
    // ------------------------------------------------------------------

    @Override
    public List<LeaderboardEntry> getLeaderboard(LeaderboardType type, int limit) throws AccountException {
        String col = "p." + type.getColumn(); // comes from our own enum, never from the player
        String sql = "SELECT u.username, " + col + " AS score_value FROM player_progress p"
                + " JOIN users u ON u.user_id = p.user_id"
                + " WHERE " + col + " > 0 ORDER BY score_value DESC, p.updated_at ASC LIMIT ?";
        List<LeaderboardEntry> rows = new ArrayList<>();
        try (Connection con = open(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                int rank = 0;
                int previous = -1;
                int position = 0;
                while (rs.next()) {
                    position++;
                    int value = rs.getInt("score_value");
                    if (value != previous) {
                        rank = position; // same score = same rank
                        previous = value;
                    }
                    rows.add(new LeaderboardEntry(rank, rs.getString("username"), value));
                }
            }
        } catch (SQLException e) {
            throw new AccountException("COULD NOT LOAD THE LEADERBOARD", e);
        }
        return rows;
    }

    @Override
    public int getRank(Account account, LeaderboardType type) throws AccountException {
        String col = type.getColumn();
        try (Connection con = open();
             PreparedStatement mine = con.prepareStatement("SELECT " + col + " FROM player_progress WHERE user_id = ?")) {
            mine.setInt(1, account.id());
            int value;
            try (ResultSet rs = mine.executeQuery()) {
                if (!rs.next() || rs.getInt(1) <= 0) {
                    return 0;
                }
                value = rs.getInt(1);
            }
            try (PreparedStatement better = con.prepareStatement(
                    "SELECT COUNT(*) FROM player_progress WHERE " + col + " > ?")) {
                better.setInt(1, value);
                try (ResultSet rs = better.executeQuery()) {
                    rs.next();
                    return rs.getInt(1) + 1;
                }
            }
        } catch (SQLException e) {
            throw new AccountException("COULD NOT LOAD YOUR RANK", e);
        }
    }

    // ------------------------------------------------------------------
    //  Profile and match history
    // ------------------------------------------------------------------

    @Override
    public AccountInfo getAccountInfo(Account account) throws AccountException {
        try (Connection con = open();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT username, email, created_at FROM users WHERE user_id = ?")) {
            ps.setInt(1, account.id());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new AccountException("ACCOUNT NOT FOUND");
                }
                Timestamp created = rs.getTimestamp("created_at");
                return new AccountInfo(account.id(), rs.getString("username"), rs.getString("email"),
                        created == null ? null : created.toLocalDateTime(), countMatches(account));
            }
        } catch (SQLException e) {
            throw new AccountException("COULD NOT LOAD YOUR PROFILE", e);
        }
    }

    @Override
    public void changePassword(Account account, String currentPassword, String newPassword) throws AccountException {
        try (Connection con = open()) {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT password_hash, salt FROM users WHERE user_id = ?")) {
                ps.setInt(1, account.id());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || !PasswordUtil.verifyPassword(currentPassword, rs.getString("salt"),
                            rs.getString("password_hash"))) {
                        throw new AccountException("CURRENT PASSWORD IS WRONG");
                    }
                }
            }
            String salt = PasswordUtil.generateSalt(); // a new salt every time the password changes
            try (PreparedStatement up = con.prepareStatement(
                    "UPDATE users SET password_hash = ?, salt = ? WHERE user_id = ?")) {
                up.setString(1, PasswordUtil.hashPassword(newPassword, salt));
                up.setString(2, salt);
                up.setInt(3, account.id());
                up.executeUpdate();
            }
        } catch (SQLException e) {
            throw new AccountException("DATABASE ERROR WHILE CHANGING THE PASSWORD", e);
        }
    }

    @Override
    public int countMatches(Account account) throws AccountException {
        try (Connection con = open();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM match_history WHERE user_id = ?")) {
            ps.setInt(1, account.id());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new AccountException("COULD NOT LOAD THE MATCH HISTORY", e);
        }
    }

    @Override
    public List<MatchRecord> getMatchHistory(Account account, int offset, int limit) throws AccountException {
        String sql = "SELECT played_at, stage_number, player_tower, enemy_tower, result, score, duration_seconds,"
                + " damage_dealt, units_defeated, tower_hp_left, coins_earned FROM match_history"
                + " WHERE user_id = ? ORDER BY played_at DESC, match_id DESC LIMIT ? OFFSET ?";
        List<MatchRecord> list = new ArrayList<>();
        try (Connection con = open(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, account.id());
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp when = rs.getTimestamp("played_at");
                    list.add(new MatchRecord(
                            when == null ? null : when.toLocalDateTime(),
                            rs.getInt("stage_number"),
                            department(rs.getString("player_tower")),
                            department(rs.getString("enemy_tower")),
                            "WIN".equals(rs.getString("result")),
                            rs.getInt("score"),
                            rs.getInt("duration_seconds"),
                            rs.getInt("damage_dealt"),
                            rs.getInt("units_defeated"),
                            rs.getInt("tower_hp_left"),
                            rs.getInt("coins_earned")));
                }
            }
        } catch (SQLException e) {
            throw new AccountException("COULD NOT LOAD THE MATCH HISTORY", e);
        }
        return list;
    }

    /** The opposite of towerCode(): "COMPSCI" -> COMPUTER_SCIENCE. */
    private static Department department(String code) {
        if ("COMPSCI".equals(code)) {
            return Department.COMPUTER_SCIENCE;
        }
        try {
            return Department.valueOf(code);
        } catch (RuntimeException e) {
            return Department.HOSPITALITY;
        }
    }

    // ------------------------------------------------------------------
    //  Forgot password (security question)
    // ------------------------------------------------------------------

    @Override
    public Account findAccount(String usernameOrEmail) throws AccountException {
        try (Connection con = open();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT user_id, username, email FROM users WHERE username = ? OR email = ? LIMIT 1")) {
            ps.setString(1, usernameOrEmail);
            ps.setString(2, usernameOrEmail);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Account(rs.getInt(1), rs.getString(2), rs.getString(3)) : null;
            }
        } catch (SQLException e) {
            throw new AccountException("DATABASE ERROR WHILE FINDING THE ACCOUNT", e);
        }
    }

    @Override
    public void setPassword(Account account, String newPassword) throws AccountException {
        String salt = PasswordUtil.generateSalt();
        try (Connection con = open();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE users SET password_hash = ?, salt = ? WHERE user_id = ?")) {
            ps.setString(1, PasswordUtil.hashPassword(newPassword, salt));
            ps.setString(2, salt);
            ps.setInt(3, account.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccountException("DATABASE ERROR WHILE SAVING THE PASSWORD", e);
        }
    }

    @Override
    public SecurityInfo getSecurityInfo(Account account) throws AccountException {
        try (Connection con = open();
             PreparedStatement ps = con.prepareStatement("SELECT security_question, security_answer_hash,"
                     + " security_salt, reset_attempts, reset_locked_until FROM users WHERE user_id = ?")) {
            ps.setInt(1, account.id());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new AccountException("ACCOUNT NOT FOUND");
                }
                return new SecurityInfo(rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getLong(5));
            }
        } catch (SQLException e) {
            throw new AccountException("DATABASE ERROR WHILE READING THE SECURITY QUESTION", e);
        }
    }

    @Override
    public void setSecurityQuestion(Account account, String question, String answerHash, String salt)
            throws AccountException {
        try (Connection con = open();
             PreparedStatement ps = con.prepareStatement("UPDATE users SET security_question = ?,"
                     + " security_answer_hash = ?, security_salt = ?, reset_attempts = 0, reset_locked_until = 0"
                     + " WHERE user_id = ?")) {
            ps.setString(1, question);
            ps.setString(2, answerHash);
            ps.setString(3, salt);
            ps.setInt(4, account.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccountException("DATABASE ERROR WHILE SAVING THE SECURITY QUESTION", e);
        }
    }

    @Override
    public void setResetAttempts(Account account, int attempts, long lockedUntil) throws AccountException {
        try (Connection con = open();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE users SET reset_attempts = ?, reset_locked_until = ? WHERE user_id = ?")) {
            ps.setInt(1, attempts);
            ps.setLong(2, lockedUntil);
            ps.setInt(3, account.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccountException("DATABASE ERROR WHILE CHECKING THE ANSWER", e);
        }
    }
}
