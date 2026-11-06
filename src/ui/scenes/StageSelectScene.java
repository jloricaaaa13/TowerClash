package ui.scenes;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import model.Department;
import model.PlayerProgress;
import model.Stage;
import ui.GameWindow;
import ui.pixel.Landscape;
import ui.pixel.Palette;
import ui.pixel.PixelButton;
import ui.pixel.PixelFont;

/** Pick a stage. Winning a stage unlocks the next one. Endless mode opens after Stage 3. */
public class StageSelectScene extends PixelScene {

    private final Landscape landscape = new Landscape(162, new int[]{6, 312}, new int[]{});
    private final List<StageCard> cards = new ArrayList<>();
    private final PixelButton fightButton;
    private final PixelButton endlessButton;
    private Stage selected;

    public StageSelectScene(GameWindow window) {
        super(window);
        int cardW = 56;
        int gap = 6;
        int startX = (W - (cardW * Stage.ALL.size() + gap * (Stage.ALL.size() - 1))) / 2;
        for (int i = 0; i < Stage.ALL.size(); i++) {
            Stage stage = Stage.ALL.get(i);
            StageCard card = new StageCard(stage, startX + i * (cardW + gap), 36, cardW, 90);
            card.setOnClick(() -> select(stage));
            cards.add(addWidget(card));
        }
        addWidget(new PixelButton("BACK", 16, 151, 64, 15, window::showLineup));
        endlessButton = addWidget(new PixelButton("ENDLESS", (W - 72) / 2, 151, 72, 15, this::endless));
        fightButton = addWidget(new PixelButton("FIGHT!", W - 80, 151, 64, 15, this::fight));
    }

    @Override
    public void onShow() {
        PlayerProgress progress = window.getProgress();
        int unlocked = progress.getUnlockedStage();
        for (StageCard card : cards) {
            card.setLocked(card.getStage().number() > unlocked);
            card.setStars(progress.getStars(card.getStage().number()));
        }
        endlessButton.setTextColor(progress.isEndlessUnlocked() ? Palette.TEXT_GOLD : Palette.TEXT_DISABLED);
        if (selected == null || selected.number() > unlocked) {
            selected = Stage.get(unlocked); // newest unlocked stage
        }
        select(selected);
        super.onShow();
    }

    private void select(Stage stage) {
        selected = stage;
        for (StageCard card : cards) {
            card.setSelected(card.getStage() == stage);
        }
    }

    private void fight() {
        if (selected != null) {
            window.startBattle(selected);
        }
    }

    private void endless() {
        if (window.getProgress().isEndlessUnlocked()) {
            window.startBattle(Stage.ENDLESS);
        } else {
            showToast("BEAT STAGE " + Stage.ENDLESS_UNLOCK_AFTER + " TO UNLOCK ENDLESS MODE");
        }
    }

    @Override
    protected void onEscape() {
        window.showLineup();
    }

    @Override
    protected void paintScene(Graphics2D g) {
        landscape.draw(g, tick);
        PixelFont.drawCentered(g, "CHOOSE A STAGE", W / 2, 6, 2, Palette.TEXT_WHITE, Palette.TEXT_DARK);

        Department dept = window.getSelectedDepartment();
        PlayerProgress progress = window.getProgress();
        if (dept != null) {
            String info = "YOUR TOWER: " + dept.getTowerName() + "  (LV " + progress.getTowerLevel(dept) + ")";
            PixelFont.drawCentered(g, info, W / 2, 24, 1, Palette.TEXT_GOLD, Palette.TEXT_DARK);
        }
        if (selected != null) {
            PixelFont.drawCentered(g, selected.modifier().getTitle() + ": "
                    + selected.modifier().getDescription(), W / 2, 129, 1, Palette.TEXT_WHITE, Palette.TEXT_DARK);
        }
        PixelFont.drawCentered(g, "STARS: WIN / KEEP 50% TOWER HP / WIN UNDER 2:00", W / 2, 139, 1,
                Palette.TEXT_GOLD, Palette.TEXT_DARK);
    }
}
