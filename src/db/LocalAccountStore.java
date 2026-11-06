package db;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import model.Department;
import model.Match;
import model.PlayerProgress;

/**
 * The backup way to save accounts: small files on THIS computer, used automatically
 * when MySQL is not running. Each player gets:
 *   <name>.account   username, email, password hash and salt (never the real password)
 *   <name>.save      their progress
 *   <name>.history   one line per finished match
 * Folder: C:\Users\<you>\.department-tower-clash\accounts
 */
public class LocalAccountStore implements AccountStore {

    private final Path folder;

    public LocalAccountStore() {
        this(Paths.get(System.getProperty("user.home"), ".department-tower-clash", "accounts"));
    }

    public LocalAccountStore(Path folder) {
        this.folder = folder;
    }

    @Override
    public String getName() {
        return "THIS COMPUTER (OFFLINE)";
    }

    @Override
    public boolean isDatabase() {
        return false;
    }

    // ------------------------------------------------------------------
    //  Accounts
    // ------------------------------------------------------------------

    @Override
    public synchronized Account register(String username, String email, String password) throws AccountException {
        if (Files.exists(accountFile(username))) {
            throw new AccountException("USERNAME IS ALREADY TAKEN");
        }
        int nextId = 1;
        for (Properties other : allAccounts()) {
            if (email.equalsIgnoreCase(other.getProperty("email", ""))) {
                throw new AccountException("EMAIL IS ALREADY USED BY ANOTHER ACCOUNT");
            }
            nextId = Math.max(nextId, parse(other.getProperty("id")) + 1);
        }
        String salt = PasswordUtil.generateSalt();
        Properties props = new Properties();
        props.setProperty("id", String.valueOf(nextId));
        props.setProperty("username", username);
        props.setProperty("email", email);
        props.setProperty("salt", salt);
        props.setProperty("passwordHash", PasswordUtil.hashPassword(password, salt));
        props.setProperty("createdAt", LocalDateTime.now().toString());
        write(accountFile(username), props);
        Account account = new Account(nextId, username, email);
        saveProgress(account, new PlayerProgress());
        return account;
    }

    @Override
    public Account login(String username, String password) throws AccountException {
        Path file = accountFile(username);
        if (!Files.exists(file)) {
            throw new AccountException("WRONG USERNAME OR PASSWORD");
        }
        Properties props = read(file);
        if (!PasswordUtil.verifyPassword(password, props.getProperty("salt", ""), props.getProperty("passwordHash", ""))) {
            throw new AccountException("WRONG USERNAME OR PASSWORD");
        }
        return new Account(parse(props.getProperty("id")), props.getProperty("username"), props.getProperty("email"));
    }

    // ------------------------------------------------------------------
    //  Progress
    // ------------------------------------------------------------------

    @Override
    public PlayerProgress loadProgress(Account account) throws AccountException {
        Path file = saveFile(account.username());
        return Files.exists(file) ? ProgressCodec.fromProperties(read(file)) : new PlayerProgress();
    }

    @Override
    public void saveProgress(Account account, PlayerProgress progress) throws AccountException {
        try {
            Files.createDirectories(folder);
            Files.writeString(saveFile(account.username()), ProgressCodec.toText(progress));
        } catch (IOException e) {
            throw new AccountException("COULD NOT SAVE YOUR PROGRESS", e);
        }
    }

    @Override
    public void recordMatch(Account account, Match m) throws AccountException {
        String line = LocalDateTime.now().withNano(0) + ",STAGE " + m.getStage().number() + ","
                + m.getPlayerDepartment() + "," + m.getEnemyDepartment() + ","
                + (m.isPlayerWin() ? "WIN" : "LOSS") + "," + m.getScore() + "," + m.getSeconds() + "s,"
                + (int) m.getDamageDealt() + "," + m.getUnitsDefeated() + ","
                + Math.round(m.getTowerHpLeftRatio() * 100) + "," + m.getCoinsEarned()
                + System.lineSeparator();
        try {
            Files.createDirectories(folder);
            Files.writeString(folder.resolve(key(account.username()) + ".history"), line,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new AccountException("COULD NOT SAVE THE MATCH HISTORY", e);
        }
    }

    @Override
    public void recordLanMatch(Account account, Department mine, Department theirs, boolean won, int seconds)
            throws AccountException {
        String line = LocalDateTime.now().withNano(0) + ",LAN 1V1," + mine + "," + theirs + ","
                + (won ? "WIN" : "LOSS") + ",0," + seconds + "s" + System.lineSeparator();
        try {
            Files.createDirectories(folder);
            Files.writeString(folder.resolve(key(account.username()) + ".history"), line,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new AccountException("COULD NOT SAVE THE MATCH HISTORY", e);
        }
    }

    // ------------------------------------------------------------------
    //  Leaderboard (reads every player's save file on this computer)
    // ------------------------------------------------------------------

    @Override
    public List<LeaderboardEntry> getLeaderboard(LeaderboardType type, int limit) throws AccountException {
        List<LeaderboardEntry> all = new ArrayList<>();
        for (Properties account : allAccounts()) {
            String name = account.getProperty("username");
            int value = valueOf(name, type);
            if (value > 0) {
                all.add(new LeaderboardEntry(0, name, value));
            }
        }
        all.sort((a, b) -> Integer.compare(b.value(), a.value()));
        List<LeaderboardEntry> rows = new ArrayList<>();
        int rank = 0;
        int previous = -1;
        for (int i = 0; i < all.size() && i < limit; i++) {
            LeaderboardEntry e = all.get(i);
            if (e.value() != previous) {
                rank = i + 1; // same score = same rank
                previous = e.value();
            }
            rows.add(new LeaderboardEntry(rank, e.username(), e.value()));
        }
        return rows;
    }

    @Override
    public int getRank(Account account, LeaderboardType type) throws AccountException {
        int mine = valueOf(account.username(), type);
        if (mine <= 0) {
            return 0;
        }
        int better = 0;
        for (Properties other : allAccounts()) {
            if (valueOf(other.getProperty("username"), type) > mine) {
                better++;
            }
        }
        return better + 1;
    }

    private int valueOf(String username, LeaderboardType type) throws AccountException {
        Path file = saveFile(username);
        if (!Files.exists(file)) {
            return 0;
        }
        PlayerProgress p = ProgressCodec.fromProperties(read(file));
        return switch (type) {
            case HIGH_SCORE -> p.getHighScore();
            case WINS -> p.getWins();
            case STARS -> p.getTotalStars();
            case ENDLESS -> p.getBestEndlessSeconds();
        };
    }

    // ------------------------------------------------------------------
    //  Profile and match history
    // ------------------------------------------------------------------

    @Override
    public AccountInfo getAccountInfo(Account account) throws AccountException {
        Properties props = read(accountFile(account.username()));
        LocalDateTime created = null;
        try {
            created = LocalDateTime.parse(props.getProperty("createdAt", ""));
        } catch (RuntimeException e) {
            // older account files may not have a date
        }
        return new AccountInfo(account.id(), props.getProperty("username", account.username()),
                props.getProperty("email", account.email()), created, countMatches(account));
    }

    @Override
    public synchronized void changePassword(Account account, String currentPassword, String newPassword)
            throws AccountException {
        Path file = accountFile(account.username());
        Properties props = read(file);
        if (!PasswordUtil.verifyPassword(currentPassword, props.getProperty("salt", ""),
                props.getProperty("passwordHash", ""))) {
            throw new AccountException("CURRENT PASSWORD IS WRONG");
        }
        String salt = PasswordUtil.generateSalt();
        props.setProperty("salt", salt);
        props.setProperty("passwordHash", PasswordUtil.hashPassword(newPassword, salt));
        write(file, props);
    }

    @Override
    public int countMatches(Account account) throws AccountException {
        return readHistory(account).size();
    }

    @Override
    public List<MatchRecord> getMatchHistory(Account account, int offset, int limit) throws AccountException {
        List<MatchRecord> all = readHistory(account);
        Collections.reverse(all); // the file is oldest first; show newest first
        int from = Math.min(Math.max(0, offset), all.size());
        int to = Math.min(all.size(), from + Math.max(0, limit));
        return new ArrayList<>(all.subList(from, to));
    }

    /**
     * Reads the .history file. Each line:
     *   date,STAGE 3,HOSPITALITY,EDUCATION,WIN,1200,95s[,damage,units,hp%,coins]
     * (older lines do not have the last four numbers; they count as 0)
     */
    private List<MatchRecord> readHistory(Account account) throws AccountException {
        Path file = folder.resolve(key(account.username()) + ".history");
        List<MatchRecord> list = new ArrayList<>();
        if (!Files.exists(file)) {
            return list;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (IOException e) {
            throw new AccountException("COULD NOT LOAD THE MATCH HISTORY", e);
        }
        for (String line : lines) {
            String[] f = line.split(",");
            if (f.length < 7) {
                continue;
            }
            try {
                int stage = f[1].startsWith("LAN") ? MatchRecord.LAN : Integer.parseInt(f[1].replace("STAGE", "").trim());
                list.add(new MatchRecord(
                        LocalDateTime.parse(f[0].trim()),
                        stage,
                        Department.valueOf(f[2].trim()),
                        Department.valueOf(f[3].trim()),
                        "WIN".equals(f[4].trim()),
                        parse(f[5]),
                        parse(f[6].replace("s", "")),
                        f.length > 7 ? parse(f[7]) : 0,
                        f.length > 8 ? parse(f[8]) : 0,
                        f.length > 9 ? parse(f[9]) : 0,
                        f.length > 10 ? parse(f[10]) : 0));
            } catch (RuntimeException e) {
                // skip a broken line instead of losing the whole history
            }
        }
        return list;
    }

    // ------------------------------------------------------------------
    //  Forgot password (security question)
    // ------------------------------------------------------------------

    @Override
    public Account findAccount(String usernameOrEmail) throws AccountException {
        for (Properties p : allAccounts()) {
            if (usernameOrEmail.equalsIgnoreCase(p.getProperty("username", ""))
                    || usernameOrEmail.equalsIgnoreCase(p.getProperty("email", ""))) {
                return new Account(parse(p.getProperty("id")), p.getProperty("username"), p.getProperty("email"));
            }
        }
        return null;
    }

    @Override
    public synchronized void setPassword(Account account, String newPassword) throws AccountException {
        Path file = accountFile(account.username());
        Properties props = read(file);
        String salt = PasswordUtil.generateSalt();
        props.setProperty("salt", salt);
        props.setProperty("passwordHash", PasswordUtil.hashPassword(newPassword, salt));
        write(file, props);
    }

    @Override
    public SecurityInfo getSecurityInfo(Account account) throws AccountException {
        Properties p = read(accountFile(account.username()));
        long locked;
        try {
            locked = Long.parseLong(p.getProperty("resetLockedUntil", "0").trim());
        } catch (NumberFormatException e) {
            locked = 0;
        }
        return new SecurityInfo(p.getProperty("securityQuestion"), p.getProperty("securityAnswerHash", ""),
                p.getProperty("securitySalt", ""), parse(p.getProperty("resetAttempts", "0")), locked);
    }

    @Override
    public synchronized void setSecurityQuestion(Account account, String question, String answerHash, String salt)
            throws AccountException {
        Path file = accountFile(account.username());
        Properties p = read(file);
        p.setProperty("securityQuestion", question);
        p.setProperty("securityAnswerHash", answerHash);
        p.setProperty("securitySalt", salt);
        p.setProperty("resetAttempts", "0");
        p.setProperty("resetLockedUntil", "0");
        write(file, p);
    }

    @Override
    public synchronized void setResetAttempts(Account account, int attempts, long lockedUntil) throws AccountException {
        Path file = accountFile(account.username());
        Properties p = read(file);
        p.setProperty("resetAttempts", String.valueOf(attempts));
        p.setProperty("resetLockedUntil", String.valueOf(lockedUntil));
        write(file, p);
    }

    // ------------------------------------------------------------------
    //  Files
    // ------------------------------------------------------------------

    /** Usernames are not case-sensitive: "Juan" and "juan" are the same player. */
    private static String key(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private Path accountFile(String username) {
        return folder.resolve(key(username) + ".account");
    }

    private Path saveFile(String username) {
        return folder.resolve(key(username) + ".save");
    }

    private List<Properties> allAccounts() throws AccountException {
        List<Properties> list = new ArrayList<>();
        if (!Files.isDirectory(folder)) {
            return list;
        }
        try (DirectoryStream<Path> files = Files.newDirectoryStream(folder, "*.account")) {
            for (Path f : files) {
                list.add(read(f));
            }
        } catch (IOException e) {
            throw new AccountException("COULD NOT READ THE ACCOUNTS FOLDER", e);
        }
        return list;
    }

    private static Properties read(Path file) throws AccountException {
        Properties props = new Properties();
        try (Reader r = Files.newBufferedReader(file)) {
            props.load(r);
        } catch (IOException e) {
            throw new AccountException("COULD NOT READ " + file.getFileName(), e);
        }
        return props;
    }

    private void write(Path file, Properties props) throws AccountException {
        try {
            Files.createDirectories(folder);
            try (Writer w = Files.newBufferedWriter(file)) {
                props.store(w, "Department Tower Clash account (the password is NOT stored, only its hash)");
            }
        } catch (IOException e) {
            throw new AccountException("COULD NOT SAVE THE ACCOUNT", e);
        }
    }

    private static int parse(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (RuntimeException e) {
            return 0;
        }
    }
}
