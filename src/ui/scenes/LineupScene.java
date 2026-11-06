package ui.scenes;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import model.Department;
import model.Tower;
import model.Unit;
import model.UnitType;
import ui.GameWindow;
import ui.pixel.Landscape;
import ui.pixel.Palette;
import ui.pixel.PixelArt;
import ui.pixel.PixelButton;
import ui.pixel.PixelFont;
import ui.pixel.PixelWidget;

/**
 * Tower Select -> LINEUP -> Stage Select.
 *
 * Each department has 5 units, but you can only bring 3 into a battle.
 * Click a card to add it to your lineup (it gets slot 1, 2 or 3 = the keys you press in battle),
 * click it again to remove it. The lineup is saved, so it is remembered next time.
 */
public class LineupScene extends PixelScene {

    private static final int CARD_W = 61;
    private static final int CARD_H = 76;
    private static final int CARD_GAP = 2;
    private static final int CARD_Y = 36;
    private static final int INFO_Y = 117;
    private static final int INFO_H = 40;

    private final Landscape landscape = new Landscape(162, new int[]{6, 312}, new int[]{});
    private final List<UnitCard> cards = new ArrayList<>();
    private final List<UnitType> lineup = new ArrayList<>();
    private final PixelButton nextButton;
    private Department department;
    private UnitCard infoCard; // the card explained in the info box

    public LineupScene(GameWindow window) {
        super(window);
        addWidget(new PixelButton("BACK", 16, 161, 64, 15, this::back));
        addWidget(new PixelButton("RESET", (W - 64) / 2, 161, 64, 15, this::reset));
        nextButton = addWidget(new PixelButton("NEXT", W - 80, 161, 64, 15, this::next));
    }

    /** Builds the 5 cards for the department picked on the Tower Select screen. */
    @Override
    public void onShow() {
        Department chosen = window.getSelectedDepartment();
        if (chosen != department) {
            department = chosen;
            buildCards();
        }
        lineup.clear();
        lineup.addAll(window.getProgress().getLineup(department));
        refresh();
        super.onShow();
    }

    private void buildCards() {
        // Widgets cannot be removed, so old cards are hidden and new ones are added
        for (UnitCard card : cards) {
            card.setVisible(false);
            card.setEnabled(false);
        }
        cards.clear();
        List<UnitType> roster = department.getRoster();
        int startX = (W - (CARD_W * roster.size() + CARD_GAP * (roster.size() - 1))) / 2;
        for (int i = 0; i < roster.size(); i++) {
            UnitCard card = new UnitCard(roster.get(i), startX + i * (CARD_W + CARD_GAP), CARD_Y, CARD_W, CARD_H);
            card.setOnClick(() -> toggle(card));
            cards.add(addWidget(card));
        }
        infoCard = cards.get(0);
    }

    /** Adds the unit to the lineup, or removes it if it is already there. */
    private void toggle(UnitCard card) {
        infoCard = card;
        UnitType type = card.getType();
        if (lineup.contains(type)) {
            lineup.remove(type);
        } else if (lineup.size() < Tower.LINEUP_SIZE) {
            lineup.add(type);
        } else {
            showToast("LINEUP IS FULL - CLICK A PICKED UNIT TO REMOVE IT");
        }
        refresh();
    }

    private void reset() {
        lineup.clear();
        lineup.addAll(department.getDefaultLineup());
        refresh();
    }

    private void refresh() {
        for (UnitCard card : cards) {
            card.setSlot(lineup.indexOf(card.getType()));
        }
        nextButton.setEnabled(lineup.size() == Tower.LINEUP_SIZE);
    }

    private void next() {
        if (lineup.size() != Tower.LINEUP_SIZE) {
            showToast("PICK " + Tower.LINEUP_SIZE + " UNITS FIRST");
            return;
        }
        window.getProgress().setLineup(department, lineup);
        window.saveProgress();
        if (window.isLineupForLan()) {
            window.showLanLobby(); // changing the lineup from the LAN lobby: go back there
        } else {
            window.showStageSelect();
        }
    }

    @Override
    protected void onEscape() {
        back();
    }

    private void back() {
        if (window.isLineupForLan()) {
            window.showLanLobby();
            return;
        }
        window.showTowerSelect();
    }

    @Override
    protected void update() {
        PixelWidget target = getTooltipTarget();
        if (target instanceof UnitCard card) {
            infoCard = card; // explain whatever the mouse / arrow keys are on
        }
    }

    @Override
    protected void paintScene(Graphics2D g) {
        landscape.draw(g, tick);
        PixelFont.drawCentered(g, "CHOOSE YOUR LINEUP", W / 2, 6, 2, Palette.TEXT_WHITE, Palette.TEXT_DARK);
        if (department == null) {
            return;
        }
        String sub = department.getShortName().toUpperCase() + "  -  PICK " + Tower.LINEUP_SIZE
                + " OF " + cards.size() + " UNITS  (" + lineup.size() + "/" + Tower.LINEUP_SIZE + ")";
        PixelFont.drawCentered(g, sub, W / 2, 22, 1, Palette.lighter(department.getColor(), 0.2f), Palette.TEXT_DARK);
        drawInfo(g);
    }

    /** The box under the cards that explains one unit. */
    private void drawInfo(Graphics2D g) {
        int x = 4;
        int w = W - 8;
        PixelArt.board(g, x, INFO_Y, w, INFO_H);
        if (infoCard == null) {
            return;
        }
        Unit u = infoCard.getPreview();
        Color roleColor = u.getRole().getColor();
        String title = u.getName().toUpperCase() + " - " + u.getRole().getLabel()
                + "  (STRONG VS " + u.getRole().beats().getLabel() + ")";
        PixelFont.drawOutlined(g, title, x + 7, INFO_Y + 4, 1, roleColor, Palette.TEXT_DARK);
        PixelFont.drawOutlined(g, "ABILITY: " + u.getAbilityName(), x + 7, INFO_Y + 13, 1,
                Palette.TEXT_GOLD, Palette.TEXT_DARK);
        PixelFont.draw(g, u.getAbilityText(), x + 7, INFO_Y + 22, 1, Palette.TEXT_DARK);
        String stats = "HP " + (int) u.getMaxHp() + "   DMG " + (int) u.getDamage() + "   "
                + (u.isRanged() ? "RANGE " + (int) u.getRange() : "MELEE") + "   SPEED " + (int) u.getSpeed()
                + "   COST " + infoCard.getType().cost();
        PixelFont.draw(g, stats, x + 7, INFO_Y + 31, 1, Palette.WOOD_OUTLINE);
    }
}
