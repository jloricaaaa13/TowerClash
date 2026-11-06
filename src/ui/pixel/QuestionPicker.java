package ui.pixel;

import java.awt.Graphics2D;
import java.util.List;

/**
 * A box that shows one security question. Click it (or press ENTER on it) to show the next one.
 * Looks like a text box with arrows on both sides:   <  NAME OF YOUR FIRST PET?  >
 */
public class QuestionPicker extends PixelWidget {

    private final List<String> questions;
    private int index;

    public QuestionPicker(int x, int y, int w, List<String> questions) {
        super(x, y, w, 13);
        this.questions = questions;
        setOnClick(() -> index = (index + 1) % questions.size());
    }

    public String getQuestion() {
        return questions.get(index);
    }

    public void setQuestion(String question) {
        int i = questions.indexOf(question);
        index = i < 0 ? 0 : i;
    }

    @Override
    public void draw(Graphics2D g, boolean focused, boolean pressed, int tick) {
        g.setColor(focused ? Palette.GOLD : Palette.WOOD_OUTLINE);
        g.fillRect(x - 1, y - 1, w + 2, h + 2);
        g.setColor(focused ? Palette.WOOD_LIGHT : Palette.WOOD_HOVER);
        g.fillRect(x, y, w, h);
        PixelFont.draw(g, "<", x + 4, y + 3, 1, Palette.TEXT_DARK);
        PixelFont.draw(g, ">", x + w - 4 - PixelFont.width(">", 1), y + 3, 1, Palette.TEXT_DARK);
        PixelFont.drawCentered(g, getQuestion(), x + w / 2, y + 3, 1, Palette.TEXT_DARK, null);
    }
}
