package ui.pixel;

import java.awt.Graphics2D;

/**
 * Anything on a pixel screen that the player can click or select with the keyboard.
 *
 * Abstraction + inheritance: PixelButton and TowerCard both extend this class.
 * The screen treats them all the same way (polymorphism) and just calls draw() and click().
 */
public abstract class PixelWidget {

    protected int x;
    protected int y;
    protected int w;
    protected int h;
    private boolean enabled = true;
    private boolean visible = true;
    private Runnable onClick;

    protected PixelWidget(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    /**
     * Draws the widget on the 320x180 canvas.
     * @param focused true when the mouse is over it or it is selected with the arrow keys
     * @param pressed true while the mouse button is held down on it
     * @param tick    animation frame counter
     */
    public abstract void draw(Graphics2D g, boolean focused, boolean pressed, int tick);

    public boolean contains(int px, int py) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    public void click() {
        if (enabled && visible && onClick != null) {
            onClick.run();
        }
    }

    public void setOnClick(Runnable onClick) {
        this.onClick = onClick;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return w;
    }

    public int getHeight() {
        return h;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
