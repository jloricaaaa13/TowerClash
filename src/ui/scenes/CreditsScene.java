package ui.scenes;

import java.awt.Color;
import java.awt.Graphics2D;
import model.Department;
import ui.GameWindow;
import ui.pixel.Landscape;
import ui.pixel.Palette;
import ui.pixel.PixelArt;
import ui.pixel.PixelButton;
import ui.pixel.PixelFont;

/** Who made the game. Edit the LINES below to put your names. */
public class CreditsScene extends PixelScene {

    /** One line of text; outline null = plain dark text. */
    private record Line(String text, Color fill, Color outline) {
    }

    private static final Line[] LINES = {
        new Line("DEPARTMENT TOWER CLASH", Palette.TEXT_DARK, null),
        new Line("A TOWER VS TOWER BATTLE GAME", Palette.TEXT_DARK, null),
        new Line("", null, null),
        new Line("MADE BY:  Group 8", Palette.TEXT_DARK, null),   // <-- change this
        new Line("", null, null),
        new Line(Department.HOSPITALITY.getFullName(), Department.HOSPITALITY.getColor(), Palette.TEXT_DARK),
        new Line(Department.EDUCATION.getFullName(), Department.EDUCATION.getColor(), Palette.TEXT_DARK),
        new Line(Department.COMPUTER_SCIENCE.getFullName(), Department.COMPUTER_SCIENCE.getColor(), Palette.TEXT_DARK),
        new Line("", null, null),
        new Line("MADE WITH JAVA SWING", Palette.TEXT_DARK, null)
    };

    private final Landscape landscape = new Landscape(162, new int[]{12, 306}, new int[]{26, 292});

    public CreditsScene(GameWindow window) {
        super(window);
        addWidget(new PixelButton("BACK", (W - 76) / 2, 144, 76, 15, window::showMainMenu));
    }

    @Override
    protected void paintScene(Graphics2D g) {
        landscape.draw(g, tick);
        PixelFont.drawCentered(g, "CREDITS", W / 2, 10, 2, Palette.TEXT_WHITE, Palette.TEXT_DARK);

        PixelArt.board(g, 40, 30, 240, 108);
        for (int i = 0; i < LINES.length; i++) {
            Line line = LINES[i];
            if (!line.text().isEmpty()) {
                PixelFont.drawCentered(g, line.text(), W / 2, 37 + i * 10, 1, line.fill(), line.outline());
            }
        }
    }
}
