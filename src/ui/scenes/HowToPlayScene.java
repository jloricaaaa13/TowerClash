package ui.scenes;

import java.awt.Color;
import java.awt.Graphics2D;
import model.Department;
import model.Role;
import model.Side;
import model.Unit;
import model.units.Robot;
import model.units.Teacher;
import model.units.Waiter;
import ui.GameWindow;
import ui.pixel.Landscape;
import ui.pixel.Palette;
import ui.pixel.PixelArt;
import ui.pixel.PixelButton;
import ui.pixel.PixelFont;

/** A 4-page guide: goal and energy, units and counters, ultimates and effects, controls. */
public class HowToPlayScene extends PixelScene {

    private static final int PAGES = 4;
    private static final int BX = 14;
    private static final int BY = 26;
    private static final int BW = W - 28;
    private static final int BH = 114;

    private final Landscape landscape = new Landscape(168, new int[]{6, 314}, new int[]{});
    private final PixelButton prevButton;
    private final PixelButton nextButton;
    private final Unit fastSample = new Waiter(Side.PLAYER);
    private final Unit rangedSample = new Teacher(Side.PLAYER);
    private final Unit tankSample = new Robot(Side.PLAYER);
    private int page;

    public HowToPlayScene(GameWindow window) {
        super(window);
        prevButton = addWidget(new PixelButton("< PREV", 16, 148, 64, 15, () -> turn(-1)));
        addWidget(new PixelButton("MENU", (W - 64) / 2, 148, 64, 15, window::showMainMenu));
        nextButton = addWidget(new PixelButton("NEXT >", W - 80, 148, 64, 15, () -> turn(1)));
    }

    @Override
    public void onShow() {
        page = 0;
        refresh();
        super.onShow();
    }

    private void turn(int direction) {
        page = Math.max(0, Math.min(PAGES - 1, page + direction));
        refresh();
    }

    private void refresh() {
        prevButton.setEnabled(page > 0);
        nextButton.setEnabled(page < PAGES - 1);
    }

    @Override
    protected void paintScene(Graphics2D g) {
        landscape.draw(g, tick);
        PixelFont.drawCentered(g, "HOW TO PLAY", W / 2, 6, 2, Palette.TEXT_WHITE, Palette.TEXT_DARK);
        PixelArt.board(g, BX, BY, BW, BH);
        PixelFont.drawCentered(g, "PAGE " + (page + 1) + " / " + PAGES, W / 2, BY + BH - 11, 1, Palette.WOOD_OUTLINE, null);
        switch (page) {
            case 0 -> pageGoal(g);
            case 1 -> pageCounters(g);
            case 2 -> pageUltimates(g);
            default -> pageControls(g);
        }
    }

    private void title(Graphics2D g, String text) {
        PixelFont.drawCentered(g, text, W / 2, BY + 6, 1, Palette.TEXT_GOLD, Palette.TEXT_DARK);
    }

    private void line(Graphics2D g, String text, int row) {
        PixelFont.drawCentered(g, text, W / 2, BY + 20 + row * 10, 1, Palette.TEXT_DARK, null);
    }

    private void pageGoal(Graphics2D g) {
        title(g, "THE GOAL");
        line(g, "DESTROY THE ENEMY TOWER BEFORE", 0);
        line(g, "IT DESTROYS YOURS.", 1);
        line(g, "ENERGY FILLS UP BY ITSELF. SPEND IT", 3);
        line(g, "ON UNITS AND TOWER UPGRADES.", 4);
        line(g, "DEFEATING AN ENEMY UNIT GIVES +4 ENERGY.", 5);
        line(g, "OVERTIME: 2X ENERGY, TOWERS TAKE +50%.", 6);
        line(g, "BEFORE A BATTLE, PICK 3 OF YOUR 5 UNITS.", 7);
    }

    private void pageCounters(Graphics2D g) {
        title(g, "UNITS AND COUNTERS");
        line(g, "EVERY UNIT HAS A ROLE AND ITS OWN ABILITY:", 0);
        int cy = BY + 68;
        drawRole(g, fastSample, Role.FAST, W / 2 - 80, cy);
        drawRole(g, rangedSample, Role.RANGED, W / 2, cy);
        drawRole(g, tankSample, Role.TANK, W / 2 + 80, cy);
        PixelFont.drawCentered(g, "FAST  >  RANGED  >  TANK  >  FAST", W / 2, BY + 67, 1,
                Palette.TEXT_GOLD, Palette.TEXT_DARK);
        line(g, "A UNIT DEALS 1.5X DAMAGE TO THE ROLE IT", 6);
        line(g, "BEATS, AND ONLY 0.7X TO THE ONE THAT BEATS IT.", 7);
    }

    private void drawRole(Graphics2D g, Unit sample, Role role, int cx, int groundY) {
        sample.drawSprite(g, cx, groundY - 8, 1, (tick / 10) % 2, tick);
        PixelFont.drawCentered(g, role.getLabel(), cx, groundY - 40, 1, role.getColor(), Palette.TEXT_DARK);
    }

    private void pageUltimates(Graphics2D g) {
        title(g, "ULTIMATES AND EFFECTS");
        line(g, "THE ULTIMATE METER FILLS OVER TIME AND", 0);
        line(g, "WHEN YOU DEAL DAMAGE. WHEN FULL, PRESS R!", 1);
        effect(g, "KITCHEN INFERNO", "BURN ALL ENEMIES, HEAL", Department.HOSPITALITY.getColor(), 3);
        effect(g, "FINAL EXAM", "FREEZE EVERYTHING 5 SEC", Department.EDUCATION.getColor(), 4);
        effect(g, "SYSTEM OVERLOAD", "LASER + BLOCK SPAWNS", Department.COMPUTER_SCIENCE.getColor(), 5);
        effect(g, "BURN / SLOW", "LOSE HP / MOVE SLOWER", new Color(0xFF7A1A), 6);
        effect(g, "FROZEN", "CANNOT ACT, +50% DAMAGE", new Color(0x7DD3FC), 7);
    }

    private void effect(Graphics2D g, String name, String text, Color color, int row) {
        int y = BY + 20 + row * 10;
        PixelFont.drawOutlined(g, name, BX + 22, y, 1, Palette.lighter(color, 0.15f), Palette.TEXT_DARK);
        PixelFont.draw(g, text, BX + 132, y, 1, Palette.TEXT_DARK);
    }

    private void pageControls(Graphics2D g) {
        title(g, "CONTROLS");
        control(g, "1  2  3", "SPAWN A UNIT", 0);
        control(g, "U", "UPGRADE YOUR TOWER", 1);
        control(g, "R", "USE YOUR ULTIMATE", 2);
        control(g, "F", "SPEED X2", 3);
        control(g, "P / ESC", "PAUSE", 4);
        line(g, "HOVER A BUTTON TO SEE WHAT IT DOES.", 6);
        line(g, "STARS: WIN / 50% TOWER HP / UNDER 2:00", 7);
    }

    private void control(Graphics2D g, String key, String text, int row) {
        int y = BY + 20 + row * 10;
        PixelFont.drawOutlined(g, key, BX + 60, y, 1, Palette.TEXT_GOLD, Palette.TEXT_DARK);
        PixelFont.draw(g, text, BX + 130, y, 1, Palette.TEXT_DARK);
    }
}
