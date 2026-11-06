package ui;

import audio.SoundManager;
import db.Account;
import db.AccountException;
import db.AccountStore;
import db.AccountStores;
import db.LegacySave;
import db.PasswordReset;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JPanel;
import model.Department;
import model.GameSettings;
import net.LanMatch;
import model.Match;
import model.PlayerProgress;
import model.Stage;
import ui.pixel.Palette;
import ui.pixel.PixelArt;
import ui.scenes.BattleScene;
import ui.scenes.ChangePasswordScene;
import ui.scenes.MatchHistoryScene;
import ui.scenes.ProfileScene;
import ui.scenes.CreditsScene;
import ui.scenes.HowToPlayScene;
import ui.scenes.LanLobbyScene;
import ui.scenes.LineupScene;
import ui.scenes.DashboardScene;
import ui.scenes.DatabaseScene;
import ui.scenes.ForgotPasswordScene;
import ui.scenes.LoginScene;
import ui.scenes.OptionsScene;
import ui.scenes.PixelScene;
import ui.scenes.RegisterScene;
import ui.scenes.SecurityQuestionScene;
import ui.scenes.StageSelectScene;
import ui.scenes.TowerSelectScene;
import ui.scenes.UpgradeScene;

/**
 * The one and only game window. Every screen is a PixelScene inside a CardLayout,
 * so switching screens is instant and there is never more than one window open.
 *
 * It also remembers WHO is playing (the logged-in Account) and their progress.
 * Progress is saved through an AccountStore: the MySQL database, or files on this
 * computer when MySQL is not running.
 *
 * Screen flow:  LOGIN (or REGISTER) -> DASHBOARD -> PLAY -> Tower -> Lineup -> Stage -> Battle
 */
public final class GameWindow extends JFrame {

    public static final String LOGIN = "LOGIN";
    public static final String REGISTER = "REGISTER";
    public static final String DASHBOARD = "DASHBOARD";
    public static final String DATABASE = "DATABASE";
    public static final String LAN_LOBBY = "LAN_LOBBY";
    public static final String TOWER_SELECT = "TOWER_SELECT";
    public static final String LINEUP = "LINEUP";
    public static final String STAGE_SELECT = "STAGE_SELECT";
    public static final String BATTLE = "BATTLE";
    public static final String UPGRADES = "UPGRADES";
    public static final String HOW_TO_PLAY = "HOW_TO_PLAY";
    public static final String OPTIONS = "OPTIONS";
    public static final String CREDITS = "CREDITS";
    public static final String PROFILE = "PROFILE";
    public static final String MATCH_HISTORY = "MATCH_HISTORY";
    public static final String CHANGE_PASSWORD = "CHANGE_PASSWORD";
    public static final String FORGOT_PASSWORD = "FORGOT_PASSWORD";
    public static final String SECURITY_QUESTION = "SECURITY_QUESTION";

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final Map<String, PixelScene> scenes = new HashMap<>();
    private final GameSettings settings = new GameSettings();
    private final SoundManager sound = new SoundManager(settings);
    private final BattleScene battleScene;
    private AccountStore accounts;
    private Account account;          // who is logged in (null = nobody)
    private PlayerProgress progress;  // that player's progress
    private String lastUsername = ""; // filled in on the Login screen after logging out
    private boolean securityFirstTime;  // true = the Security Question screen was opened right after login
    private boolean lineupForLan;     // true = the Lineup screen was opened from the LAN lobby
    private String historyReturn = DASHBOARD; // where BACK goes from Match History
    private PixelScene currentScene;
    private Department selectedDepartment;
    private Rectangle windowedBounds;

    public GameWindow() {
        super("Department Tower Clash");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        root.setBackground(Palette.LETTERBOX);
        accounts = AccountStores.connect(); // MySQL if it is running, otherwise files on this computer

        // Pick the biggest zoom (x2 to x4) that fits the screen, e.g. 1280x720 on a big monitor
        Rectangle usable = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int zoom = Math.max(2, Math.min(4, Math.min(
                (usable.width - 40) / PixelArt.WIDTH, (usable.height - 60) / PixelArt.HEIGHT)));
        root.setPreferredSize(new Dimension(PixelArt.WIDTH * zoom, PixelArt.HEIGHT * zoom));

        battleScene = new BattleScene(this);
        addScene(LOGIN, new LoginScene(this));
        addScene(REGISTER, new RegisterScene(this));
        addScene(DASHBOARD, new DashboardScene(this));
        addScene(DATABASE, new DatabaseScene(this));
        addScene(LAN_LOBBY, new LanLobbyScene(this));
        addScene(TOWER_SELECT, new TowerSelectScene(this));
        addScene(LINEUP, new LineupScene(this));
        addScene(STAGE_SELECT, new StageSelectScene(this));
        addScene(BATTLE, battleScene);
        addScene(UPGRADES, new UpgradeScene(this));
        addScene(HOW_TO_PLAY, new HowToPlayScene(this));
        addScene(OPTIONS, new OptionsScene(this));
        addScene(CREDITS, new CreditsScene(this));
        addScene(PROFILE, new ProfileScene(this));
        addScene(MATCH_HISTORY, new MatchHistoryScene(this));
        addScene(CHANGE_PASSWORD, new ChangePasswordScene(this));
        addScene(FORGOT_PASSWORD, new ForgotPasswordScene(this));
        addScene(SECURITY_QUESTION, new SecurityQuestionScene(this));

        setContentPane(root);
        pack();
        Insets in = getInsets();
        setMinimumSize(new Dimension(PixelArt.WIDTH * 2 + in.left + in.right,
                PixelArt.HEIGHT * 2 + in.top + in.bottom));
        setLocationRelativeTo(null);
        if (zoom < 3) {
            setExtendedState(JFrame.MAXIMIZED_BOTH); // small screen: use all of it so the text is bigger
        }

        // Pause the battle when the player alt-tabs or minimizes the game
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                if (currentScene != null) {
                    currentScene.onWindowFocusLost();
                }
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowIconified(WindowEvent e) {
                if (currentScene != null) {
                    currentScene.onWindowFocusLost();
                }
            }
        });

        showScene(LOGIN);
    }

    private void addScene(String name, PixelScene scene) {
        scenes.put(name, scene);
        root.add(scene, name);
    }

    public void showScene(String name) {
        if (currentScene != null) {
            currentScene.onHide();
        }
        currentScene = scenes.get(name);
        cards.show(root, name);
        sound.playMusic(BATTLE.equals(name) ? SoundManager.Music.BATTLE : SoundManager.Music.MENU);
        currentScene.onShow();
    }

    /** The home screen: the Dashboard (or the Login screen if nobody is logged in). */
    public void showMainMenu() {
        showScene(account == null ? LOGIN : DASHBOARD);
    }

    public void showLogin() {
        showScene(LOGIN);
    }

    public void showRegister() {
        showScene(REGISTER);
    }

    public void showDatabaseSettings() {
        showScene(DATABASE);
    }

    public void showTowerSelect() {
        lineupForLan = false;
        showScene(TOWER_SELECT);
    }

    /** LAN 1v1: host a game or join another computer's game. */
    public void showLanLobby() {
        lineupForLan = false;
        showScene(LAN_LOBBY);
    }

    /** Opens the Lineup screen from the LAN lobby (it returns to the lobby afterwards). */
    public void showLineupForLan(Department department) {
        selectedDepartment = department;
        lineupForLan = true;
        showScene(LINEUP);
    }

    public boolean isLineupForLan() {
        return lineupForLan;
    }

    /** Both players are connected: start the LAN 1v1 battle. */
    public void startLanBattle(LanMatch match) {
        battleScene.startLan(match);
        showScene(BATTLE);
    }

    /** Pick the 3 units to bring into battle (after choosing a department). */
    public void showLineup() {
        showScene(LINEUP);
    }

    public void showStageSelect() {
        showScene(STAGE_SELECT);
    }

    public void showUpgrades() {
        showScene(UPGRADES);
    }

    public void showHowToPlay() {
        showScene(HOW_TO_PLAY);
    }

    public void showOptions() {
        showScene(OPTIONS);
    }

    public void showCredits() {
        showScene(CREDITS);
    }

    /** The player's profile: account details, progress summary, change password. */
    public void showProfile() {
        showScene(PROFILE);
    }

    /** Every finished match, newest first. BACK returns to the screen that opened it. */
    public void showMatchHistory() {
        historyReturn = currentScene == scenes.get(PROFILE) ? PROFILE : DASHBOARD;
        showScene(MATCH_HISTORY);
    }

    /** The BACK button of the Match History screen. */
    public void showHistoryReturn() {
        showScene(account == null ? LOGIN : historyReturn);
    }

    public void showChangePassword() {
        showScene(CHANGE_PASSWORD);
    }

    /** Login > FORGOT PASSWORD?: answer the security question to make a new password. */
    public void showForgotPassword() {
        showScene(FORGOT_PASSWORD);
    }

    /** Profile > SECURITY QUESTION: change the question (asks for the password). */
    public void showSecurityQuestion() {
        securityFirstTime = false;
        showScene(SECURITY_QUESTION);
    }

    /** true when the Security Question screen was opened right after login (no question yet). */
    public boolean isSecurityFirstTime() {
        return securityFirstTime;
    }

    /** Starts a match on the given stage with the department chosen on the Tower Select screen.
     * @param stage */
    public void startBattle(Stage stage) {
        battleScene.start(selectedDepartment, stage);
        showScene(BATTLE);
    }

    // ---- Accounts and progress ----

    /**
     * Logs in and loads that player's progress.
     * @throws AccountException with a message for the player (wrong password, database error...)
     */
    public void login(String username, String password) throws AccountException {
        Account found = accounts.login(username, password);
        progress = accounts.loadProgress(found);
        account = found;
        selectedDepartment = null;
        if (!accounts.getSecurityInfo(found).hasQuestion()) {
            securityFirstTime = true; // older account: ask for a security question first (LATER = skip)
            showScene(SECURITY_QUESTION);
        } else {
            showScene(DASHBOARD);
        }
    }

    /**
     * Creates a new account with its security question and logs in. The very first account on this
     * computer also gets the progress from the old single save file (from before accounts existed).
     */
    public void register(String username, String email, String password, String question, String answer)
            throws AccountException {
        Account created = accounts.register(username, email, password);
        PasswordReset.setQuestion(accounts, created, question, answer);
        PlayerProgress old = LegacySave.take();
        if (old != null) {
            accounts.saveProgress(created, old);
        }
        login(username, password);
    }

    /**
     * Changes the logged-in player's password.
     * @throws AccountException e.g. "CURRENT PASSWORD IS WRONG"
     */
    public void changePassword(String currentPassword, String newPassword) throws AccountException {
        if (account == null) {
            throw new AccountException("NOBODY IS LOGGED IN");
        }
        accounts.changePassword(account, currentPassword, newPassword);
    }

    public void logout() {
        lastUsername = account == null ? "" : account.username();
        account = null;
        progress = null;
        selectedDepartment = null;
        showScene(LOGIN);
    }

    /** Tries to connect to MySQL again (the RETRY button on the Login screen). */
    public void reconnect() {
        accounts = AccountStores.connect();
    }

    /** The name of the player who logged out last (so they do not have to type it again). */
    public String getLastUsername() {
        return lastUsername;
    }

    /** Fills in the username on the Login screen (e.g. after a password reset). */
    public void setLastUsername(String username) {
        lastUsername = username == null ? "" : username;
    }

    public AccountStore getAccounts() {
        return accounts;
    }

    public Account getAccount() {
        return account;
    }

    public PlayerProgress getProgress() {
        return progress;
    }

    /** Saves the logged-in player's progress. Returns false if saving failed. */
    public boolean saveProgress() {
        if (account == null) {
            return false;
        }
        try {
            accounts.saveProgress(account, progress);
            return true;
        } catch (AccountException e) {
            System.err.println(e.getMessage() + ": " + e.getCause());
            return false;
        }
    }

    /** Saves a finished LAN 1v1 match in the match history. */
    public void recordLanMatch(Department mine, Department theirs, boolean won, int seconds) {
        if (account == null) {
            return;
        }
        try {
            accounts.recordLanMatch(account, mine, theirs, won, seconds);
        } catch (AccountException e) {
            System.err.println(e.getMessage() + ": " + e.getCause());
        }
    }

    /** Adds a finished match to the player's match history. */
    public void recordMatch(Match match) {
        if (account == null) {
            return;
        }
        try {
            accounts.recordMatch(account, match);
        } catch (AccountException e) {
            System.err.println(e.getMessage() + ": " + e.getCause());
        }
    }

    // ---- Settings and sound ----

    public GameSettings getSettings() {
        return settings;
    }

    public SoundManager getSound() {
        return sound;
    }

    public Department getSelectedDepartment() {
        return selectedDepartment;
    }

    public void setSelectedDepartment(Department selectedDepartment) {
        this.selectedDepartment = selectedDepartment;
    }

    /** Switches between a normal window and borderless fullscreen. */
    public void setFullscreen(boolean fullscreen) {
        if (fullscreen == isUndecorated()) {
            return;
        }
        if (fullscreen) {
            windowedBounds = getBounds();
        }
        dispose(); // a window must be hidden before its border can be removed or added
        setUndecorated(fullscreen);
        if (fullscreen) {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        } else {
            setExtendedState(JFrame.NORMAL);
            if (windowedBounds != null) {
                setBounds(windowedBounds);
            }
        }
        setVisible(true);
        currentScene.requestFocusInWindow();
    }
}
