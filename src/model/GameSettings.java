package model;

/**
 * Player options from the OPTIONS screen.
 * Kept in memory for now; later it can be saved to a file or the database.
 */
public class GameSettings {

    private boolean musicOn = true;
    private boolean soundOn = true;
    private boolean fullscreen = false;
    private boolean pixelPerfect = false; // false = stretch to fill the window
    private boolean smoothText = true;    // true = smooth, easy-to-read letters

    public boolean isMusicOn() {
        return musicOn;
    }

    public void setMusicOn(boolean musicOn) {
        this.musicOn = musicOn;
    }

    public boolean isSoundOn() {
        return soundOn;
    }

    public void setSoundOn(boolean soundOn) {
        this.soundOn = soundOn;
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
    }

    /** true = only whole-number zoom (sharpest pixels, may leave black borders). */
    public boolean isPixelPerfect() {
        return pixelPerfect;
    }

    public void setPixelPerfect(boolean pixelPerfect) {
        this.pixelPerfect = pixelPerfect;
    }

    /** true = smooth high-resolution letters, false = classic pixel letters. */
    public boolean isSmoothText() {
        return smoothText;
    }

    public void setSmoothText(boolean smoothText) {
        this.smoothText = smoothText;
    }
}
