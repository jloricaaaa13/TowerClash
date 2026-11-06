package db;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * WHERE the MySQL database is. Saved in a small file on each computer:
 *   C:\Users\<you>\.department-tower-clash\database.properties
 *
 *   host = localhost        -> the MySQL on THIS computer (normal XAMPP)
 *   host = 192.168.1.10     -> the MySQL on another computer in the same Wi-Fi/LAN,
 *                              so every player shares the same accounts and leaderboard
 *
 * Changed in the game: Login screen > DATABASE SETTINGS.
 */
public class DatabaseConfig {

    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 3306;    // XAMPP default
    public static final String DEFAULT_USER = "root"; // XAMPP default (no password)

    private static final Path FILE = Paths.get(System.getProperty("user.home"), ".department-tower-clash",
            "database.properties");

    private String host = DEFAULT_HOST;
    private int port = DEFAULT_PORT;
    private String user = DEFAULT_USER;
    private String password = "";

    /** Reads the saved settings (or the XAMPP defaults if there are none). */
    public static DatabaseConfig load() {
        DatabaseConfig config = new DatabaseConfig();
        if (!Files.exists(FILE)) {
            return config;
        }
        Properties props = new Properties();
        try (Reader r = Files.newBufferedReader(FILE)) {
            props.load(r);
            config.host = props.getProperty("host", DEFAULT_HOST).trim();
            config.port = Integer.parseInt(props.getProperty("port", String.valueOf(DEFAULT_PORT)).trim());
            config.user = props.getProperty("user", DEFAULT_USER).trim();
            config.password = props.getProperty("password", "");
        } catch (IOException | NumberFormatException e) {
            return new DatabaseConfig();
        }
        return config;
    }

    public void save() {
        Properties props = new Properties();
        props.setProperty("host", host);
        props.setProperty("port", String.valueOf(port));
        props.setProperty("user", user);
        props.setProperty("password", password);
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer w = Files.newBufferedWriter(FILE)) {
                props.store(w, "Department Tower Clash - where the MySQL database is");
            }
        } catch (IOException e) {
            System.err.println("Could not save database settings: " + e.getMessage());
        }
    }

    /** true = the database is on another computer (shared LAN leaderboard). */
    public boolean isRemote() {
        return !host.equalsIgnoreCase("localhost") && !host.equals("127.0.0.1");
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host == null || host.isBlank() ? DEFAULT_HOST : host.trim();
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port <= 0 || port > 65535 ? DEFAULT_PORT : port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user == null || user.isBlank() ? DEFAULT_USER : user.trim();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password == null ? "" : password;
    }
}
