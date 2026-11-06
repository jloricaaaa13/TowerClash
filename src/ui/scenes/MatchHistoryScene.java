package ui.scenes;

import db.Account;
import db.AccountException;
import db.MatchRecord;
import java.awt.Color;
import java.awt.Graphics2D;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import ui.GameWindow;
import ui.pixel.Landscape;
import ui.pixel.Palette;
import ui.pixel.PixelArt;
import ui.pixel.PixelButton;
import ui.pixel.PixelFont;
import ui.pixel.PixelWidget;

/**
 * MATCH HISTORY: every match the player finished, newest first, 8 per page.
 * Point at (or select) a row to see more details about that match at the bottom.
 * The rows come from the match_history table (or the .history file when offline).
 */
public class MatchHistoryScene extends PixelScene {

    private static final int PAGE_SIZE = 8;
    private static final int BOARD_X = 4;
    private static final int BOARD_Y = 22;
    private static final int BOARD_W = 312;
    private static final int BOARD_H = 129;
    private static final int HEADER_Y = 37;
    private static final int ROWS_Y = 47;
    private static final int ROW_H = 10;

    // Column positions
    private static final int DATE_X = 12;
    private static final int MODE_X = 78;
    private static final int TEAMS_X = 126;
    private static final int RESULT_X = 196;
    private static final int SCORE_RIGHT = 270;
    private static final int TIME_RIGHT = 306;

    private static final Color WIN_COLOR = new Color(0x15803D);
    private static final Color LOSS_COLOR = new Color(0xB91C1C);
    private static final Color ENDLESS_COLOR = new Color(0x6D28D9);
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("MM/dd HH:mm");

    private final Landscape landscape = new Landscape(170, new int[]{}, new int[]{});
    private final List<Row> rowWidgets = new ArrayList<>();
    private final PixelButton prevButton;
    private final PixelButton nextButton;
    private List<MatchRecord> matches = new ArrayList<>();
    private int page;
    private int total;
    private String error;

    public MatchHistoryScene(GameWindow window) {
        super(window);
        int by = 156;
        prevButton = addWidget(new PixelButton("< PREV", 4, by, 60, 17, () -> loadPage(page - 1)));
        nextButton = addWidget(new PixelButton("NEXT >", 68, by, 60, 17, () -> loadPage(page + 1)));
        addWidget(new PixelButton("BACK", 252, by, 64, 17, window::showHistoryReturn));
        for (int i = 0; i < PAGE_SIZE; i++) {
            rowWidgets.add(addWidget(new Row(i)));
        }
    }

    @Override
    public void onShow() {
        loadPage(0);
        super.onShow();
    }

    private int pageCount() {
        return Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    /** Reads one page of matches from the database. */
    private void loadPage(int newPage) {
        Account me = window.getAccount();
        if (me == null) {
            return;
        }
        try {
            total = window.getAccounts().countMatches(me);
            page = Math.max(0, Math.min(pageCount() - 1, newPage));
            matches = window.getAccounts().getMatchHistory(me, page * PAGE_SIZE, PAGE_SIZE);
            error = null;
        } catch (AccountException e) {
            matches = new ArrayList<>();
            total = 0;
            page = 0;
            error = e.getMessage();
        }
        for (int i = 0; i < rowWidgets.size(); i++) {
            rowWidgets.get(i).setVisible(i < matches.size());
        }
        prevButton.setEnabled(page > 0);
        nextButton.setEnabled(page < pageCount() - 1);
    }

    @Override
    protected void onEscape() {
        window.showHistoryReturn();
    }

    // ------------------------------------------------------------------
    //  Drawing
    // ------------------------------------------------------------------

    @Override
    protected void paintScene(Graphics2D g) {
        landscape.draw(g, tick);
        PixelFont.drawCentered(g, "MATCH HISTORY", W / 2, 4, 2, Palette.TEXT_WHITE, Palette.TEXT_DARK);
        PixelArt.board(g, BOARD_X, BOARD_Y, BOARD_W, BOARD_H);

        String count = "TOTAL: " + total + (total == 1 ? " MATCH" : " MATCHES");
        PixelFont.draw(g, count, BOARD_X + 8, BOARD_Y + 4, 1, Palette.TEXT_GOLD);
        String pageText = "PAGE " + (page + 1) + " / " + pageCount();
        PixelFont.draw(g, pageText, BOARD_X + BOARD_W - 8 - PixelFont.width(pageText, 1), BOARD_Y + 4, 1,
                Palette.TEXT_GOLD);

        // Column titles
        PixelFont.draw(g, "DATE", DATE_X, HEADER_Y, 1, Palette.TEXT_DARK);
        PixelFont.draw(g, "MODE", MODE_X, HEADER_Y, 1, Palette.TEXT_DARK);
        PixelFont.draw(g, "YOU VS", TEAMS_X, HEADER_Y, 1, Palette.TEXT_DARK);
        PixelFont.draw(g, "RESULT", RESULT_X, HEADER_Y, 1, Palette.TEXT_DARK);
        PixelFont.draw(g, "SCORE", SCORE_RIGHT - PixelFont.width("SCORE", 1), HEADER_Y, 1, Palette.TEXT_DARK);
        PixelFont.draw(g, "TIME", TIME_RIGHT - PixelFont.width("TIME", 1), HEADER_Y, 1, Palette.TEXT_DARK);
        g.setColor(Palette.WOOD_OUTLINE);
        g.fillRect(BOARD_X + 6, HEADER_Y + 8, BOARD_W - 12, 1);

        int cx = BOARD_X + BOARD_W / 2;
        if (error != null) {
            PixelFont.drawCentered(g, error, cx, ROWS_Y + 30, 1, Palette.HP_LOW, null);
            return;
        }
        if (matches.isEmpty()) {
            PixelFont.drawCentered(g, "NO MATCHES YET.", cx, ROWS_Y + 26, 1, Palette.TEXT_DARK, null);
            PixelFont.drawCentered(g, "FINISH A MATCH AND IT WILL SHOW UP HERE!", cx, ROWS_Y + 36, 1,
                    Palette.TEXT_DARK, null);
            return;
        }
        drawDetails(g);
    }

    /** The details line under the table, for the row under the mouse (or the newest match). */
    private void drawDetails(Graphics2D g) {
        MatchRecord m = matches.get(0);
        for (Row r : rowWidgets) {
            if (r.isVisible() && r.highlighted) {
                m = matches.get(r.index);
            }
        }
        int y = BOARD_Y + BOARD_H - 12;
        g.setColor(Palette.WOOD_OUTLINE);
        g.fillRect(BOARD_X + 6, y - 4, BOARD_W - 12, 1);
        String text;
        if (m.isLan()) {
            text = "LAN 1V1 AGAINST ANOTHER PLAYER  -  " + (m.win() ? "YOU WON!" : "YOU LOST");
        } else {
            text = "DAMAGE " + m.damageDealt() + "  DEFEATED " + m.unitsDefeated()
                    + (m.isEndless() ? "" : "  TOWER HP " + m.towerHpLeft() + "%")
                    + "  COINS +" + m.coinsEarned();
        }
        PixelFont.drawCentered(g, text, BOARD_X + BOARD_W / 2, y, 1, Palette.TEXT_DARK, null);
    }

    private static String time(int seconds) {
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }

    /** One row of the table. It is a widget so it can be pointed at with the mouse or arrow keys. */
    private final class Row extends PixelWidget {

        private final int index;
        private boolean highlighted;

        Row(int index) {
            super(BOARD_X + 5, ROWS_Y + index * ROW_H - 1, BOARD_W - 10, ROW_H);
            this.index = index;
        }

        @Override
        public void draw(Graphics2D g, boolean focused, boolean pressed, int tick) {
            if (index >= matches.size()) {
                return;
            }
            highlighted = focused;
            MatchRecord m = matches.get(index);
            if (focused) {
                g.setColor(new Color(255, 216, 110, 150));
                g.fillRect(x, y, w, h - 1);
            } else if (index % 2 == 0) {
                g.setColor(new Color(0, 0, 0, 22));
                g.fillRect(x, y, w, h - 1);
            }
            int ty = y + 1;
            String when = m.playedAt() == null ? "-" : m.playedAt().format(WHEN);
            PixelFont.draw(g, when, DATE_X, ty, 1, Palette.WOOD_OUTLINE);
            PixelFont.draw(g, m.getModeText(), MODE_X, ty, 1, Palette.TEXT_DARK);

            // "CS VS HM", each in its department color
            String mine = ProfileScene.code(m.playerTower());
            PixelFont.draw(g, mine, TEAMS_X, ty, 1, darker(m.playerTower().getColor()));
            int vx = TEAMS_X + PixelFont.width(mine + " ", 1);
            PixelFont.draw(g, "VS", vx, ty, 1, Palette.WOOD_OUTLINE);
            PixelFont.draw(g, ProfileScene.code(m.enemyTower()), vx + PixelFont.width("VS ", 1), ty, 1,
                    darker(m.enemyTower().getColor()));

            Color resultColor = m.isEndless() ? ENDLESS_COLOR : m.win() ? WIN_COLOR : LOSS_COLOR;
            PixelFont.draw(g, m.getResultText(), RESULT_X, ty, 1, resultColor);
            String score = m.isLan() ? "-" : String.valueOf(m.score());
            PixelFont.draw(g, score, SCORE_RIGHT - PixelFont.width(score, 1), ty, 1, Palette.TEXT_DARK);
            String t = time(m.seconds());
            PixelFont.draw(g, t, TIME_RIGHT - PixelFont.width(t, 1), ty, 1, Palette.TEXT_DARK);
        }

        /** Department colors are bright; a darker version is easier to read on the wood. */
        private Color darker(Color c) {
            return Palette.darker(c, 0.4f);
        }
    }
}
