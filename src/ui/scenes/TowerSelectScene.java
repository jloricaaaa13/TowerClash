package ui.scenes;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import model.Department;
import ui.GameWindow;
import ui.pixel.Landscape;
import ui.pixel.Palette;
import ui.pixel.PixelButton;
import ui.pixel.PixelFont;

/** PLAY -> choose which department tower you will use. */
public class TowerSelectScene extends PixelScene {

    private final Landscape landscape = new Landscape(162, new int[]{8, 310}, new int[]{});
    private final List<TowerCard> cards = new ArrayList<>();
    private final PixelButton startButton;
    private Department selected;

    public TowerSelectScene(GameWindow window) {
        super(window);

        Department[] departments = Department.values();
        int cardW = 88;
        int gap = 12;
        int startX = (W - (cardW * departments.length + gap * (departments.length - 1))) / 2;
        for (int i = 0; i < departments.length; i++) {
            Department department = departments[i];
            TowerCard card = new TowerCard(department, startX + i * (cardW + gap), 38, cardW, 100);
            card.setOnClick(() -> select(department));
            cards.add(addWidget(card));
        }

        addWidget(new PixelButton("BACK", 16, 144, 64, 15, window::showMainMenu));
        startButton = addWidget(new PixelButton("NEXT", W - 80, 144, 64, 15, this::start));
        startButton.setEnabled(false); // enabled after picking a tower
    }

    private void select(Department department) {
        selected = department;
        for (TowerCard card : cards) {
            card.setSelected(card.getDepartment() == department);
        }
        startButton.setEnabled(true);
    }

    private void start() {
        window.setSelectedDepartment(selected);
        window.showLineup();
    }

    @Override
    protected void paintScene(Graphics2D g) {
        landscape.draw(g, tick);
        PixelFont.drawCentered(g, "CHOOSE YOUR DEPARTMENT", W / 2, 8, 2, Palette.TEXT_WHITE, Palette.TEXT_DARK);
        PixelFont.drawCentered(g, "THE ENEMY WILL USE ONE OF THE OTHER TWO", W / 2, 27, 1,
                Palette.TEXT_GOLD, Palette.TEXT_DARK);

        String status = selected == null ? "PICK A TOWER" : "SELECTED: " + selected.getShortName();
        PixelFont.drawCentered(g, status, W / 2, 148, 1, Palette.TEXT_WHITE, Palette.TEXT_DARK);
    }
}
