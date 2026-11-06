package ui.scenes;

import db.Account;
import db.AccountException;
import db.AccountInfo;
import java.awt.Color;
import java.awt.Graphics2D;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import model.Department;
import model.PlayerProgress;
import model.Stage;
import ui.GameWindow;
import ui.pixel.Landscape;
import ui.pixel.Palette;
import ui.pixel.PixelArt;
import ui.pixel.PixelButton;
import ui.pixel.PixelFont;

/**
 * The player's PROFILE:
 *   Left:   account details (username, email, player ID, date joined, where it is saved)
 *   Right:  a summary of their progress and their tower levels
 *   Bottom: HISTORY, CHANGE PASSWORD, SECURITY QUESTION, BACK
 */
public class ProfileScene extends PixelScene {

    private static final int PANEL_Y = 24;
    private static final int PANEL_H = 126;
    private static final int LEFT_X = 4;
    private static final int LEFT_W = 160;
    private static final int RIGHT_X = 167;
    private static final int RIGHT_W = 149;
    private static final Color GREEN = new Color(0x15803D);
    private static final DateTimeFormatter JOINED =
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    private final Landscape landscape = new Landscape(170, new int[]{}, new int[]{});
    private AccountInfo info;
    private String error;

    public ProfileScene(GameWindow window) {
        super(window);
        int by = 156;
        addWidget(new PixelButton("HISTORY", 4, by, 58, 17, window::showMatchHistory)).setTextColor(Palette.TEXT_ON);
        addWidget(new PixelButton("CHANGE PASSWORD", 66, by, 100, 17, window::showChangePassword));
        addWidget(new PixelButton("SECURITY QUESTION", 170, by, 108, 17, window::showSecurityQuestion));
        addWidget(new PixelButton("BACK", 282, by, 34, 17, window::showMainMenu));
    }

    @Override
    public void onShow() {
        Account me = window.getAccount();
        try {
            info = me == null ? null : window.getAccounts().getAccountInfo(me);
            error = null;
        } catch (AccountException e) {
            info = null;
            error = e.getMessage();
        }
        super.onShow();
    }

    @Override
    protected void paintScene(Graphics2D g) {
        landscape.draw(g, tick);
        PixelFont.drawCentered(g, "MY PROFILE", W / 2, 5, 2, Palette.TEXT_WHITE, Palette.TEXT_DARK);
        Account me = window.getAccount();
        PlayerProgress p = window.getProgress();
        if (me == null || p == null) {
            return;
        }
        drawAccount(g, me);
        drawSummary(g, p);
    }

    private void drawAccount(Graphics2D g, Account me) {
        PixelArt.board(g, LEFT_X, PANEL_Y, LEFT_W, PANEL_H);
        int cx = LEFT_X + LEFT_W / 2;
        PixelFont.drawCentered(g, "ACCOUNT", cx, PANEL_Y + 4, 1, Palette.TEXT_GOLD, Palette.TEXT_DARK);

        // A simple badge with the first letter of the username
        int bx = cx - 12;
        int by = PANEL_Y + 15;
        g.setColor(Palette.WOOD_OUTLINE);
        g.fillRect(bx, by, 24, 24);
        g.setColor(Palette.GOLD_DARK);
        g.fillRect(bx + 1, by + 1, 22, 22);
        g.setColor(Palette.GOLD);
        g.fillRect(bx + 2, by + 2, 20, 18);
        String letter = me.username().substring(0, 1).toUpperCase();
        PixelFont.drawCentered(g, letter, cx, by + 5, 2, Palette.TEXT_DARK, null);
        PixelFont.drawCentered(g, me.username(), cx, by + 28, 1, Palette.TEXT_DARK, null);

        if (error != null) {
            PixelFont.drawCentered(g, error, cx, PANEL_Y + 80, 1, Palette.HP_LOW, null);
            return;
        }
        String email = info == null ? me.email() : info.email();
        String joined = info == null || info.createdAt() == null ? "-"
                : info.createdAt().format(JOINED).toUpperCase();
        String[][] rows = {
            {"EMAIL", ""},
            {"", email},
            {"PLAYER ID", "#" + me.id()},
            {"JOINED", joined},
            {"MATCHES SAVED", info == null ? "-" : String.valueOf(info.matchCount())},
        };
        int y = PANEL_Y + 56;
        for (int i = 0; i < rows.length; i++) {
            int rowY = y + i * 10;
            if (!rows[i][0].isEmpty()) {
                PixelFont.draw(g, rows[i][0], LEFT_X + 8, rowY, 1, Palette.TEXT_DARK);
            }
            String value = fit(rows[i][1], LEFT_W - 16);
            PixelFont.draw(g, value, LEFT_X + LEFT_W - 8 - PixelFont.width(value, 1), rowY, 1, Palette.WOOD_OUTLINE);
        }
        boolean db = window.getAccounts().isDatabase();
        PixelFont.drawCentered(g, db ? "SAVED IN MYSQL DATABASE" : "SAVED ON THIS COMPUTER", cx,
                PANEL_Y + PANEL_H - 11, 1, db ? GREEN : Palette.SIGN_ARROW, null);
    }

    private void drawSummary(Graphics2D g, PlayerProgress p) {
        PixelArt.board(g, RIGHT_X, PANEL_Y, RIGHT_W, PANEL_H);
        int cx = RIGHT_X + RIGHT_W / 2;
        PixelFont.drawCentered(g, "PROGRESS", cx, PANEL_Y + 4, 1, Palette.TEXT_GOLD, Palette.TEXT_DARK);

        int played = p.getMatchesPlayed();
        int winRate = played == 0 ? 0 : Math.round(p.getWins() * 100f / played);
        String[][] rows = {
            {"WINS / LOSSES", p.getWins() + " / " + p.getLosses()},
            {"WIN RATE", winRate + "%"},
            {"HIGH SCORE", String.valueOf(p.getHighScore())},
            {"STAGES OPEN", Math.min(p.getUnlockedStage(), Stage.ALL.size()) + " / " + Stage.ALL.size()},
            {"STARS", p.getTotalStars() + " / " + Stage.ALL.size() * 3},
            {"COINS", String.valueOf(p.getCoins())},
        };
        int y = PANEL_Y + 16;
        for (int i = 0; i < rows.length; i++) {
            int rowY = y + i * 10;
            if (i % 2 == 0) {
                g.setColor(new Color(0, 0, 0, 22));
                g.fillRect(RIGHT_X + 5, rowY - 1, RIGHT_W - 10, 9);
            }
            PixelFont.draw(g, rows[i][0], RIGHT_X + 8, rowY, 1, Palette.TEXT_DARK);
            String value = rows[i][1];
            PixelFont.draw(g, value, RIGHT_X + RIGHT_W - 8 - PixelFont.width(value, 1), rowY, 1, Palette.WOOD_OUTLINE);
        }

        // Tower levels, one colored bar per department
        int ty = PANEL_Y + 80;
        PixelFont.drawCentered(g, "TOWER LEVELS", cx, ty, 1, Palette.TEXT_GOLD, Palette.TEXT_DARK);
        Department[] all = Department.values();
        for (int i = 0; i < all.length; i++) {
            Department d = all[i];
            int rowY = ty + 11 + i * 10;
            PixelFont.draw(g, code(d), RIGHT_X + 8, rowY, 1, Palette.TEXT_DARK);
            int level = p.getTowerLevel(d);
            for (int k = 0; k < PlayerProgress.MAX_TOWER_LEVEL; k++) {
                int px = RIGHT_X + 42 + k * 13;
                g.setColor(Palette.WOOD_OUTLINE);
                g.fillRect(px, rowY, 11, 7);
                g.setColor(k < level ? d.getColor() : Palette.WOOD_DARK);
                g.fillRect(px + 1, rowY + 1, 9, 5);
            }
            String lv = "LV " + level;
            PixelFont.draw(g, lv, RIGHT_X + RIGHT_W - 8 - PixelFont.width(lv, 1), rowY, 1, Palette.WOOD_OUTLINE);
        }
    }

    /** "HM", "EDUC", "CS" */
    static String code(Department d) {
        return switch (d) {
            case HOSPITALITY -> "HM";
            case EDUCATION -> "EDUC";
            case COMPUTER_SCIENCE -> "CS";
        };
    }

    /** Cuts long text (like a long email) so it fits, ending with "..". */
    static String fit(String text, int maxWidth) {
        if (PixelFont.width(text, 1) <= maxWidth) {
            return text;
        }
        String t = text;
        while (t.length() > 1 && PixelFont.width(t + "..", 1) > maxWidth) {
            t = t.substring(0, t.length() - 1);
        }
        return t + "..";
    }
}
