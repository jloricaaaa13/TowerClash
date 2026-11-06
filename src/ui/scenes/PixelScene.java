package ui.scenes;

import audio.Sfx;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyEvent;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import ui.GameWindow;
import ui.pixel.Palette;
import ui.pixel.PixelArt;
import ui.pixel.PixelButton;
import ui.pixel.PixelFont;
import ui.pixel.PixelTextField;
import ui.pixel.PixelWidget;

/**
 * Parent class of every screen (Main Menu, Tower Select, Options, Credits...).
 *
 * How it works:
 *  1. Each frame, the screen is drawn using GAME coordinates (320 x 180) on a high-resolution
 *     image (the "canvas", e.g. 1280 x 720). Pixel art becomes crisp square blocks, and text
 *     is drawn with smooth letters at full resolution so it is easy to read.
 *  2. The canvas is copied to the window in REAL screen pixels (so Windows display scaling
 *     like 125% / 150% does not blur it). Pixel Perfect ON = whole-number zoom only.
 *  3. A Swing Timer repaints about 30 times per second for animations.
 *  4. Mouse and keyboard input are converted to canvas coordinates and sent to the widgets.
 *
 * Abstraction: subclasses only have to say WHAT to draw (paintScene) and which buttons exist.
 */
public abstract class PixelScene extends JPanel {

    protected static final int W = PixelArt.WIDTH;
    protected static final int H = PixelArt.HEIGHT;
    private static final int TOAST_FRAMES = 60; // about 2 seconds

    protected final GameWindow window;
    protected int tick; // animation frame counter

    private static final int MIN_DETAIL = 2;
    private static final int MAX_DETAIL = 6;
    private BufferedImage canvas;        // the game screen at high resolution (320x180 times "detail")
    private int detail;                  // screen pixels per game pixel inside the canvas
    private double screenScale = 1;      // Windows display scaling: 1.25 = 125%
    private double drawScale = 1;        // screen pixels per game pixel (last frame)
    private double drawX;                // where the game image starts on screen (screen pixels)
    private double drawY;
    private final List<PixelWidget> widgets = new ArrayList<>();
    private final List<PixelWidget> dialogWidgets = new ArrayList<>();
    private final Timer timer;
    private PixelWidget focused;
    private PixelWidget pressed;
    private PixelWidget hovered;     // widget under the mouse right now (null = none)
    private boolean keyboardMode;    // true after arrow keys, false after the mouse moves
    private PixelTextField typingField; // the text box the keyboard types into (null = none)

    private String toastText;
    private int toastUntil;

    /** One button in a pop-up dialog. */
    public record DialogButton(String label, Runnable action) {
    }

    private boolean dialogOpen;
    private boolean dialogClosable;
    private String dialogTitle;
    private String[] dialogLines;
    private int dialogX;
    private int dialogY;
    private int dialogW;
    private int dialogH;

    protected PixelScene(GameWindow window) {
        this.window = window;
        setBackground(Palette.LETTERBOX);
        timer = new Timer(33, e -> {
            tick++;
            update();
            repaint();
        });
        installMouse();
        installKeys();
        installTyping();
    }

    // ------------------------------------------------------------------
    //  What subclasses fill in
    // ------------------------------------------------------------------

    /** Draw the background and anything that is not a widget. */
    protected abstract void paintScene(Graphics2D g);

    /** Called every frame before drawing (for animations). */
    protected void update() {
    }

    /** What the Esc key does. Default: go back to the main menu. */
    protected void onEscape() {
        window.showMainMenu();
    }

    protected <T extends PixelWidget> T addWidget(T widget) {
        widgets.add(widget);
        return widget;
    }

    /** Milliseconds between frames (33 = about 30 FPS, 16 = about 60 FPS). */
    protected void setFrameDelay(int milliseconds) {
        timer.setDelay(milliseconds);
    }

    /**
     * Draws a centered message; if it is wider than maxWidth it is split into two lines
     * at a space (the second line goes 10 pixels lower).
     */
    protected static void drawWrapped(Graphics2D g, String text, int centerX, int y, int maxWidth, java.awt.Color color) {
        if (PixelFont.width(text, 1) <= maxWidth) {
            PixelFont.drawCentered(g, text, centerX, y, 1, color, null);
            return;
        }
        int split = -1;
        for (int i = text.indexOf(' '); i > 0; i = text.indexOf(' ', i + 1)) {
            if (PixelFont.width(text.substring(0, i), 1) <= maxWidth) {
                split = i;
            }
        }
        if (split < 0) {
            PixelFont.drawCentered(g, text, centerX, y, 1, color, null);
            return;
        }
        PixelFont.drawCentered(g, text.substring(0, split), centerX, y - 5, 1, color, null);
        PixelFont.drawCentered(g, text.substring(split + 1), centerX, y + 5, 1, color, null);
    }

    protected boolean isDialogOpen() {
        return dialogOpen;
    }

    /** The widget the player is pointing at (mouse hover, or arrow-key selection). Used for tooltips. */
    protected PixelWidget getTooltipTarget() {
        if (dialogOpen) {
            return null;
        }
        return keyboardMode ? focused : hovered;
    }

    /**
     * Called before any click / Enter / Space is handled.
     * Return true to "eat" the input (e.g. to skip an intro screen). Default: false.
     */
    protected boolean interceptInput() {
        return false;
    }

    /** Called when the player switches to another window (alt-tab) or minimizes the game. */
    public void onWindowFocusLost() {
    }

    /** Called by GameWindow when this screen appears. */
    public void onShow() {
        focused = firstSelectable(widgets);
        setTypingField(focused instanceof PixelTextField field ? field : null);
        timer.start();
        requestFocusInWindow();
    }

    /** Called by GameWindow when this screen is hidden. */
    public void onHide() {
        timer.stop();
        dialogOpen = false;
        dialogWidgets.clear();
        toastText = null;
        pressed = null;
        hovered = null;
        setTypingField(null);
    }

    /** Puts the keyboard into a text box (or takes it out when null). */
    protected void setTypingField(PixelTextField field) {
        if (typingField != null) {
            typingField.setActive(false);
        }
        typingField = field;
        if (field != null) {
            field.setActive(true);
            focused = field;
        }
    }

    // ------------------------------------------------------------------
    //  Messages and dialogs (drawn in pixel style instead of JOptionPane)
    // ------------------------------------------------------------------

    /** A short message at the bottom of the screen for about 2 seconds. */
    protected void showToast(String text) {
        toastText = text;
        toastUntil = tick + TOAST_FRAMES;
    }

    /**
     * A wooden pop-up with a title, some lines of text, and one or two buttons.
     * @param cancelText null = only the OK button
     */
    protected void showDialog(String title, String[] lines, String okText, Runnable onOk, String cancelText) {
        if (cancelText == null) {
            showDialog(title, lines, 0, true, new DialogButton(okText, onOk));
        } else {
            // focus the cancel button: pressing Enter by accident does nothing harmful
            showDialog(title, lines, 1, true, new DialogButton(okText, onOk), new DialogButton(cancelText, null));
        }
    }

    /**
     * A wooden pop-up with any number of buttons. Every button closes the dialog, then runs its action.
     * @param focusIndex which button is selected first
     * @param closable   true = the Esc key closes it
     */
    protected void showDialog(String title, String[] lines, int focusIndex, boolean closable, DialogButton... buttons) {
        dialogTitle = title;
        dialogLines = lines;
        dialogClosable = closable;

        int buttonW = 56;
        int gap = 6;
        int buttonsWidth = buttons.length * buttonW + (buttons.length - 1) * gap;
        int widest = Math.max(PixelFont.width(title, 2), buttonsWidth);
        for (String line : lines) {
            widest = Math.max(widest, PixelFont.width(line, 1));
        }
        dialogW = Math.max(180, widest + 28);
        dialogH = 28 + lines.length * 10 + 24;
        dialogX = (W - dialogW) / 2;
        dialogY = (H - dialogH) / 2;

        dialogWidgets.clear();
        int buttonY = dialogY + dialogH - 20;
        int bx = (W - buttonsWidth) / 2;
        for (DialogButton b : buttons) {
            dialogWidgets.add(new PixelButton(b.label(), bx, buttonY, buttonW, 14, () -> {
                closeDialog();
                if (b.action() != null) {
                    b.action().run();
                }
            }));
            bx += buttonW + gap;
        }
        focused = dialogWidgets.get(Math.max(0, Math.min(focusIndex, dialogWidgets.size() - 1)));
        dialogOpen = true;
    }

    protected void closeDialog() {
        dialogOpen = false;
        dialogWidgets.clear();
        focused = firstSelectable(widgets);
    }

    // ------------------------------------------------------------------
    //  Drawing
    // ------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // fills the black bars around the game screen
        Graphics2D screen = (Graphics2D) g.create();
        prepareCanvas(screen);

        // Everything is drawn in GAME pixels (320 x 180); the scale() makes it sharp at high resolution
        Graphics2D c = canvas.createGraphics();
        c.scale(detail, detail);
        c.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        PixelFont.setSmooth(window.getSettings().isSmoothText());
        paintScene(c);
        for (PixelWidget w : widgets) {
            if (w.isVisible()) {
                w.draw(c, !dialogOpen && w == focused, w == pressed, tick);
            }
        }
        if (dialogOpen) {
            paintDialog(c);
        }
        if (toastText != null && tick < toastUntil) {
            paintToast(c);
        }
        c.dispose();

        // Copy the canvas to the window
        int outW = (int) Math.round(W * drawScale);
        int outH = (int) Math.round(H * drawScale);
        boolean exact = outW == canvas.getWidth();
        screen.setRenderingHint(RenderingHints.KEY_INTERPOLATION, exact
                ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        screen.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        screen.drawImage(canvas, (int) drawX, (int) drawY, outW, outH, null);
        screen.dispose();
    }

    /**
     * Works out how big the game is on screen, in REAL screen pixels.
     * Java normally multiplies everything by the Windows display scaling (e.g. 1.25),
     * which makes some game pixels bigger than others (uneven, blurry look), so we undo that
     * scaling and do the math ourselves.
     *
     * The canvas is then made almost as big as the window (320x180 times "detail", e.g. x4 =
     * 1280x720). Pixel art still looks the same (each game pixel = a 4x4 block), but TEXT is
     * drawn with real smooth letters at that high resolution, so it is much easier to read.
     */
    private void prepareCanvas(Graphics2D screen) {
        AffineTransform t = screen.getTransform();
        screenScale = t.getScaleX();
        screen.setTransform(new AffineTransform(1, 0, 0, 1, t.getTranslateX(), t.getTranslateY()));

        double screenW = getWidth() * screenScale;
        double screenH = getHeight() * screenScale;
        double fit = Math.min(screenW / W, screenH / H);
        boolean pixelPerfect = window.getSettings().isPixelPerfect();
        drawScale = pixelPerfect ? Math.max(1, Math.floor(fit)) : Math.max(0.5, fit);
        drawX = Math.floor((screenW - W * drawScale) / 2);
        drawY = Math.floor((screenH - H * drawScale) / 2);

        int wanted = pixelPerfect ? (int) drawScale : (int) Math.ceil(drawScale - 0.05);
        wanted = Math.max(MIN_DETAIL, Math.min(MAX_DETAIL, wanted));
        if (canvas == null || detail != wanted) {
            detail = wanted;
            canvas = new BufferedImage(W * detail, H * detail, BufferedImage.TYPE_INT_RGB);
        }
    }

    private void paintDialog(Graphics2D g) {
        g.setColor(Palette.DIM);
        g.fillRect(0, 0, W, H);
        PixelArt.board(g, dialogX, dialogY, dialogW, dialogH);
        PixelFont.drawCentered(g, dialogTitle, W / 2, dialogY + 7, 2, Palette.TEXT_GOLD, Palette.TEXT_DARK);
        for (int i = 0; i < dialogLines.length; i++) {
            PixelFont.drawCentered(g, dialogLines[i], W / 2, dialogY + 27 + i * 10, 1, Palette.TEXT_DARK, null);
        }
        for (PixelWidget w : dialogWidgets) {
            w.draw(g, w == focused, w == pressed, tick);
        }
    }

    private void paintToast(Graphics2D g) {
        int tw = PixelFont.width(toastText, 1) + 18;
        int tx = (W - tw) / 2;
        int ty = H - 26;
        PixelArt.board(g, tx, ty, tw, 15);
        PixelFont.drawOutlined(g, toastText, tx + 9, ty + 4, 1, Palette.TEXT_GOLD, Palette.TEXT_DARK);
    }

    // ------------------------------------------------------------------
    //  Input
    // ------------------------------------------------------------------

    private List<PixelWidget> activeWidgets() {
        return dialogOpen ? dialogWidgets : widgets;
    }

    private static PixelWidget firstSelectable(List<PixelWidget> list) {
        for (PixelWidget w : list) {
            if (w.isVisible() && w.isEnabled()) {
                return w;
            }
        }
        return null;
    }

    private PixelWidget widgetAt(MouseEvent e) {
        // mouse position (window units) -> screen pixels -> game pixels
        int vx = (int) Math.floor((e.getX() * screenScale - drawX) / drawScale);
        int vy = (int) Math.floor((e.getY() * screenScale - drawY) / drawScale);
        List<PixelWidget> list = activeWidgets();
        for (int i = list.size() - 1; i >= 0; i--) {
            PixelWidget w = list.get(i);
            if (w.isVisible() && w.isEnabled() && w.contains(vx, vy)) {
                return w;
            }
        }
        return null;
    }

    private void installMouse() {
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                PixelWidget w = widgetAt(e);
                hovered = w;
                keyboardMode = false;
                if (w != null) {
                    focused = w;
                }
                setCursor(Cursor.getPredefinedCursor(w != null ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = null;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                mouseMoved(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                pressed = widgetAt(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                PixelWidget w = widgetAt(e);
                PixelWidget wasPressed = pressed;
                pressed = null;
                if (interceptInput()) {
                    return;
                }
                if (w != null && w == wasPressed) {
                    activate(w);
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    private void installKeys() {
        bindKey("UP", () -> moveFocus(-1));
        bindKey("LEFT", () -> moveFocus(-1));
        bindKey("DOWN", () -> moveFocus(1));
        bindKey("RIGHT", () -> moveFocus(1));
        bindKey("ENTER", this::clickFocused);
        bindKey("SPACE", this::clickFocused);
        bindKey("ESCAPE", () -> {
            if (!dialogOpen) {
                onEscape();
            } else if (dialogClosable) {
                closeDialog();
            }
        });
    }

    /** Connects a keyboard key (e.g. "1", "U", "ESCAPE") to an action on this screen. */
    protected void bindKey(String key, Runnable action) {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), key);
        getActionMap().put(key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isShowing()) { // only the screen that is currently visible reacts
                    action.run();
                }
            }
        });
    }

    private void moveFocus(int direction) {
        List<PixelWidget> selectable = new ArrayList<>();
        for (PixelWidget w : activeWidgets()) {
            if (w.isVisible() && w.isEnabled()) {
                selectable.add(w);
            }
        }
        if (selectable.isEmpty()) {
            return;
        }
        int index = selectable.indexOf(focused);
        index = index < 0 ? 0 : Math.floorMod(index + direction, selectable.size());
        focused = selectable.get(index);
        keyboardMode = true;
        setTypingField(focused instanceof PixelTextField field ? field : null);
    }

    private void clickFocused() {
        if (interceptInput()) {
            return;
        }
        if (typingField != null && !dialogOpen) {
            typingField.submit(); // ENTER while typing = next box / log in
            return;
        }
        if (focused != null && activeWidgets().contains(focused)) {
            activate(focused);
        }
    }

    /** Clicks a widget and plays the click sound. */
    private void activate(PixelWidget widget) {
        if (widget.isEnabled()) {
            window.getSound().play(Sfx.CLICK);
        }
        if (widget instanceof PixelTextField field) {
            setTypingField(field);
        }
        widget.click();
    }

    /**
     * Typing in text boxes. A KeyEventDispatcher sees every key BEFORE the normal key bindings,
     * so while a box is selected, letters go into the box (and SPACE does not press a button).
     * TAB / SHIFT+TAB move between boxes and buttons. CTRL+V pastes.
     */
    private void installTyping() {
        setFocusTraversalKeysEnabled(false); // let TAB reach the game instead of Swing
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (!isShowing() || dialogOpen) {
                return false;
            }
            if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_TAB) {
                moveFocus(e.isShiftDown() ? -1 : 1);
                return true;
            }
            if (typingField == null) {
                return false;
            }
            if (e.getID() == KeyEvent.KEY_TYPED) {
                char c = e.getKeyChar();
                if (c >= 32 && c != 127 && !e.isControlDown()) {
                    typingField.type(c);
                }
                return true;
            }
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_BACK_SPACE -> typingField.backspace();
                    case KeyEvent.VK_V -> {
                        if (e.isControlDown()) {
                            paste();
                        }
                    }
                    case KeyEvent.VK_SPACE, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT -> {
                        // eaten: these would otherwise press buttons or move between them
                    }
                    default -> {
                        return false; // ENTER, ESC, UP, DOWN keep working as usual
                    }
                }
                return true;
            }
            return false;
        });
    }

    private void paste() {
        try {
            String text = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            for (char c : text.toCharArray()) {
                typingField.type(c);
            }
        } catch (Exception e) {
            // nothing (or no text) to paste
        }
    }
}
