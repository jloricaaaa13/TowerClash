package ui.scenes;

import audio.Sfx;
import java.awt.Graphics2D;
import model.GameSettings;
import ui.GameWindow;
import ui.pixel.Landscape;
import ui.pixel.Palette;
import ui.pixel.PixelArt;
import ui.pixel.PixelButton;
import ui.pixel.PixelFont;

/** Music, sound effects, fullscreen, pixel-perfect scaling and smooth-text switches. */
public class OptionsScene extends PixelScene {

    private static final int BOARD_X = 64;
    private static final int BOARD_Y = 30;
    private static final int BOARD_W = 192;
    private static final int BOARD_H = 112;
    private static final int ROW = 18;

    private final Landscape landscape = new Landscape(162, new int[]{14, 300}, new int[]{28, 288});
    private final PixelButton musicButton;
    private final PixelButton soundButton;
    private final PixelButton fullscreenButton;
    private final PixelButton pixelButton;
    private final PixelButton textButton;

    public OptionsScene(GameWindow window) {
        super(window);
        GameSettings settings = window.getSettings();
        int toggleX = BOARD_X + BOARD_W - 52;

        musicButton = addWidget(new PixelButton("", toggleX, BOARD_Y + 10, 38, 14, () -> {
            settings.setMusicOn(!settings.isMusicOn());
            window.getSound().refreshMusic();
            refresh();
        }));
        soundButton = addWidget(new PixelButton("", toggleX, BOARD_Y + 10 + ROW, 38, 14, () -> {
            settings.setSoundOn(!settings.isSoundOn());
            window.getSound().play(Sfx.STAR); // so you can hear it is on
            refresh();
        }));
        fullscreenButton = addWidget(new PixelButton("", toggleX, BOARD_Y + 10 + ROW * 2, 38, 14, () -> {
            settings.setFullscreen(!settings.isFullscreen());
            refresh();
            window.setFullscreen(settings.isFullscreen());
        }));
        pixelButton = addWidget(new PixelButton("", toggleX, BOARD_Y + 10 + ROW * 3, 38, 14, () -> {
            settings.setPixelPerfect(!settings.isPixelPerfect());
            refresh();
        }));
        textButton = addWidget(new PixelButton("", toggleX, BOARD_Y + 10 + ROW * 4, 38, 14, () -> {
            settings.setSmoothText(!settings.isSmoothText());
            refresh();
        }));
        addWidget(new PixelButton("BACK", W / 2 - 80, 146, 76, 15, window::showMainMenu));
        addWidget(new PixelButton("CREDITS", W / 2 + 4, 146, 76, 15, window::showCredits));
        refresh();
    }

    private void refresh() {
        GameSettings s = window.getSettings();
        setToggle(musicButton, s.isMusicOn());
        setToggle(soundButton, s.isSoundOn());
        setToggle(fullscreenButton, s.isFullscreen());
        setToggle(pixelButton, s.isPixelPerfect());
        setToggle(textButton, s.isSmoothText());
    }

    private void setToggle(PixelButton button, boolean on) {
        button.setText(on ? "ON" : "OFF");
        button.setTextColor(on ? Palette.TEXT_ON : Palette.TEXT_OFF);
    }

    @Override
    public void onShow() {
        refresh(); // fullscreen may have changed from outside
        super.onShow();
    }

    @Override
    protected void paintScene(Graphics2D g) {
        landscape.draw(g, tick);
        PixelFont.drawCentered(g, "OPTIONS", W / 2, 10, 2, Palette.TEXT_WHITE, Palette.TEXT_DARK);

        PixelArt.board(g, BOARD_X, BOARD_Y, BOARD_W, BOARD_H);
        int labelX = BOARD_X + 16;
        PixelFont.draw(g, "MUSIC", labelX, BOARD_Y + 14, 1, Palette.TEXT_DARK);
        PixelFont.draw(g, "SOUND FX", labelX, BOARD_Y + 14 + ROW, 1, Palette.TEXT_DARK);
        PixelFont.draw(g, "FULLSCREEN", labelX, BOARD_Y + 14 + ROW * 2, 1, Palette.TEXT_DARK);
        PixelFont.draw(g, "PIXEL PERFECT", labelX, BOARD_Y + 14 + ROW * 3, 1, Palette.TEXT_DARK);
        PixelFont.draw(g, "SMOOTH TEXT", labelX, BOARD_Y + 14 + ROW * 4, 1, Palette.TEXT_DARK);
        PixelFont.drawCentered(g, "SMOOTH TEXT OFF = PIXEL LETTERS", W / 2, BOARD_Y + 101, 1,
                Palette.WOOD_OUTLINE, null);
    }
}
