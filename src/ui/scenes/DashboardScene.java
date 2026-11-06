package ui.scenes;

import db.Account;
import db.AccountException;
import db.LeaderboardEntry;
import db.LeaderboardType;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import model.PlayerProgress;
import model.Stage;
import ui.GameWindow;
import ui.pixel.Landscape;
import ui.pixel.Palette;
import ui.pixel.PixelArt;
import ui.pixel.PixelButton;
import ui.pixel.PixelFont;

/**
 * The home screen after logging in.
 *   Left:   the player's own stats (matches, wins, best score, stars...)
 *   Right:  the LEADERBOARD (top 10), with tabs: SCORE / WINS / STARS / ENDLESS.
 *           Your own row is highlighted, and your rank is shown even if you are not in the top 10.
 *           Under the stats: PROFILE and HISTORY (match history).
 *   Bottom: PLAY, LAN 1V1, UPGRADES, HELP, OPTIONS (credits are inside), LOG OUT.
 */
public class DashboardScene extends PixelScene {

    private static final int TOP_ROWS = 10;
    private static final int PANEL_Y = 24;
    private static final int PANEL_H = 126;
    private static final int STATS_X = 4;
    private static final int STATS_W = 139;
    private static final int BOARD_X = 146;
    private static final int BOARD_W = 170;
    private static final int ROWS_Y = 53;
    private static final int ROW_H = 8;
    private static final Color GREEN = new Color(0x15803D);

    private final Landscape landscape = new Landscape(170, new int[]{}, new int[]{});
    private final List<PixelButton> tabs = new ArrayList<>();
    private LeaderboardType type = LeaderboardType.HIGH_SCORE;
    private List<LeaderboardEntry> rows = new ArrayList<>();
    private int myRank;
    private String boardError;

    public DashboardScene(GameWindow window) {
        super(window);
        int bx = 4;
        int by = 156;
        addWidget(new PixelButton("PLAY", bx, by, 54, 17, window::showTowerSelect)).setTextColor(Palette.TEXT_ON);
        addWidget(new PixelButton("LAN 1V1", bx + 56, by, 54, 17, window::showLanLobby)).setTextColor(Palette.TEXT_ON);
        addWidget(new PixelButton("UPGRADES", bx + 112, by, 56, 17, window::showUpgrades));
        addWidget(new PixelButton("HELP", bx + 170, by, 40, 17, window::showHowToPlay));
        addWidget(new PixelButton("OPTIONS", bx + 212, by, 50, 17, window::showOptions));
        addWidget(new PixelButton("LOG OUT", bx + 264, by, 48, 17, this::confirmLogout));

        int halfW = (STATS_W - 13) / 2;
        int py = PANEL_Y + PANEL_H - 17;
        addWidget(new PixelButton("PROFILE", STATS_X + 5, py, halfW, 13, window::showProfile));
        addWidget(new PixelButton("HISTORY", STATS_X + 8 + halfW, py, halfW, 13, window::showMatchHistory));

        LeaderboardType[] types = LeaderboardType.values();
        int tabW = 41;
        int tabX = BOARD_X + (BOARD_W - (tabW * types.length + (types.length - 1))) / 2;
        for (int i = 0; i < types.length; i++) {
            LeaderboardType t = types[i];
            tabs.add(addWidget(new PixelButton(t.getTab(), tabX + i * (tabW + 1), PANEL_Y + 13, tabW, 12,
                    () -> showBoard(t))));
        }
    }

    @Override
    public void onShow() {
        showBoard(type);
        super.onShow(); // PLAY is selected first
    }

    /** Loads the leaderboard for one tab from the database (or the local files). */
    private void showBoard(LeaderboardType newType) {
        type = newType;
        for (int i = 0; i < tabs.size(); i++) {
            tabs.get(i).setTextColor(LeaderboardType.values()[i] == type ? Palette.TEXT_WHITE : Palette.TEXT_GOLD);
        }
        Account me = window.getAccount();
        try {
            rows = window.getAccounts().getLeaderboard(type, TOP_ROWS);
            myRank = me == null ? 0 : window.getAccounts().getRank(me, type);
            boardError = null;
        } catch (AccountException e) {
            rows = new ArrayList<>();
            myRank = 0;
            boardError = e.getMessage();
        }
    }

    private void confirmLogout() {
        showDialog("LOG OUT?", new String[]{"YOUR PROGRESS IS ALREADY SAVED."}, "LOG OUT", window::logout, "CANCEL");
    }

    @Override
    protected void onEscape() {
        showDialog("LEAVING?", new String[]{"LOG OUT, OR QUIT THE GAME?"}, 2, true,
                new DialogButton("LOG OUT", window::logout),
                new DialogButton("QUIT", () -> System.exit(0)),
                new DialogButton("CANCEL", null));
    }

    // ------------------------------------------------------------------
    //  Drawing
    // ------------------------------------------------------------------

    @Override
    protected void paintScene(Graphics2D g) {
        landscape.draw(g, tick);
        Account me = window.getAccount();
        PlayerProgress p = window.getProgress();
        if (me == null || p == null) {
            return;
        }
        PixelFont.drawCentered(g, "WELCOME, " + me.username() + "!", W / 2, 5, 2, Palette.TEXT_WHITE, Palette.TEXT_DARK);
        drawStats(g, p);
        drawLeaderboard(g, me);
    }

    private void drawStats(Graphics2D g, PlayerProgress p) {
        PixelArt.board(g, STATS_X, PANEL_Y, STATS_W, PANEL_H);
        PixelFont.drawCentered(g, "YOUR STATS", STATS_X + STATS_W / 2, PANEL_Y + 4, 1, Palette.TEXT_GOLD, Palette.TEXT_DARK);

        int played = p.getMatchesPlayed();
        int winRate = played == 0 ? 0 : Math.round(p.getWins() * 100f / played);
        String[][] stats = {
            {"MATCHES PLAYED", String.valueOf(played)},
            {"WINS / LOSSES", p.getWins() + " / " + p.getLosses()},
            {"WIN RATE", winRate + "%"},
            {"HIGH SCORE", String.valueOf(p.getHighScore())},
            {"FASTEST WIN", p.getBestWinSeconds() == 0 ? "-" : time(p.getBestWinSeconds())},
            {"STARS", p.getTotalStars() + " / " + Stage.ALL.size() * 3},
            {"LAN 1V1 W / L", p.getLanWins() + " / " + p.getLanLosses()},
            {"ENDLESS BEST", p.getBestEndlessSeconds() == 0 ? "-" : time(p.getBestEndlessSeconds())},
            {"COINS", String.valueOf(p.getCoins())},
        };
        int y = PANEL_Y + 16;
        for (int i = 0; i < stats.length; i++) {
            int rowY = y + i * 10;
            if (i % 2 == 0) {
                g.setColor(new Color(0, 0, 0, 22));
                g.fillRect(STATS_X + 5, rowY - 1, STATS_W - 10, 9);
            }
            PixelFont.draw(g, stats[i][0], STATS_X + 8, rowY, 1, Palette.TEXT_DARK);
            String value = stats[i][1];
            PixelFont.draw(g, value, STATS_X + STATS_W - 8 - PixelFont.width(value, 1), rowY, 1, Palette.WOOD_OUTLINE);
        }
    }

    private void drawLeaderboard(Graphics2D g, Account me) {
        PixelArt.board(g, BOARD_X, PANEL_Y, BOARD_W, PANEL_H);
        PixelFont.drawCentered(g, "LEADERBOARD - TOP " + TOP_ROWS, BOARD_X + BOARD_W / 2, PANEL_Y + 4, 1,
                Palette.TEXT_GOLD, Palette.TEXT_DARK);

        int rankX = BOARD_X + 8;
        int nameX = BOARD_X + 26;
        int valueRight = BOARD_X + BOARD_W - 8;

        if (boardError != null) {
            PixelFont.drawCentered(g, boardError, BOARD_X + BOARD_W / 2, ROWS_Y + 30, 1, Palette.HP_LOW, null);
            return;
        }
        if (rows.isEmpty()) {
            PixelFont.drawCentered(g, "NO SCORES YET.", BOARD_X + BOARD_W / 2, ROWS_Y + 26, 1, Palette.TEXT_DARK, null);
            PixelFont.drawCentered(g, "PLAY A MATCH TO BE THE FIRST!", BOARD_X + BOARD_W / 2, ROWS_Y + 36, 1,
                    Palette.TEXT_DARK, null);
            return;
        }
        boolean meShown = false;
        for (int i = 0; i < rows.size(); i++) {
            LeaderboardEntry e = rows.get(i);
            int y = ROWS_Y + i * ROW_H;
            boolean mine = e.username().equalsIgnoreCase(me.username());
            meShown |= mine;
            if (mine) {
                g.setColor(new Color(255, 216, 110, 150)); // highlight your own row
                g.fillRect(BOARD_X + 4, y - 1, BOARD_W - 8, ROW_H);
            }
            Color color = mine ? Palette.TEXT_DARK : Palette.WOOD_OUTLINE;
            Color rankColor = e.rank() == 1 ? new Color(0xB45309) : e.rank() == 2 ? new Color(0x52525B)
                    : e.rank() == 3 ? new Color(0x9A3412) : color; // gold, silver, bronze
            PixelFont.draw(g, String.valueOf(e.rank()), rankX, y, 1, rankColor);
            PixelFont.draw(g, e.username(), nameX, y, 1, color);
            String value = type.format(e.value());
            PixelFont.draw(g, value, valueRight - PixelFont.width(value, 1), y, 1, color);
        }

        // Your place, even when you are not in the top 10
        int lineY = PANEL_Y + PANEL_H - 11;
        String mineText = myRank == 0 ? "NOT RANKED HERE YET - KEEP PLAYING!"
                : meShown ? "YOU ARE #" + myRank + " - GREAT JOB!" : "YOUR RANK: #" + myRank;
        PixelFont.drawCentered(g, mineText, BOARD_X + BOARD_W / 2, lineY, 1, GREEN, null);
    }

    private static String time(int seconds) {
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }
}
