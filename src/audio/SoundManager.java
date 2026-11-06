package audio;

import java.util.EnumMap;
import java.util.Map;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import model.GameSettings;

/**
 * Plays sound effects and background music (made by Synth when the game starts).
 * If the computer has no audio device, sound is simply turned off and the game keeps working.
 */
public final class SoundManager {

    public enum Music { MENU, BATTLE }

    private final GameSettings settings;
    private final Map<Sfx, Clip[]> effects = new EnumMap<>(Sfx.class);
    private final Map<Sfx, Integer> nextCopy = new EnumMap<>(Sfx.class);
    private final Map<Sfx, Long> lastPlayed = new EnumMap<>(Sfx.class);
    private final Map<Music, Clip> songs = new EnumMap<>(Music.class);
    private Music currentSong;

    public SoundManager(GameSettings settings) {
        this.settings = settings;
        AudioFormat format = new AudioFormat(Synth.RATE, 16, 1, true, false);
        try {
            for (Sfx sfx : Sfx.values()) {
                byte[] data = Synth.toPcm16(Synth.effect(sfx));
                Clip[] copies = new Clip[sfx.poolSize()];
                for (int i = 0; i < copies.length; i++) {
                    copies[i] = openClip(format, data);
                }
                effects.put(sfx, copies);
            }
            songs.put(Music.MENU, openClip(format, Synth.toPcm16(Synth.menuMusic())));
            songs.put(Music.BATTLE, openClip(format, Synth.toPcm16(Synth.battleMusic())));
        } catch (Exception | LinkageError e) {
            System.err.println("Sound is off (no audio device?): " + e.getMessage());
        }
    }

    private static Clip openClip(AudioFormat format, byte[] data) {
        try {
            Clip clip = AudioSystem.getClip();
            clip.open(format, data, 0, data.length);
            return clip;
        } catch (Exception | LinkageError e) {
            return null; // this one sound will be silent
        }
    }

    public void play(Sfx sfx) {
        if (!settings.isSoundOn()) {
            return;
        }
        Clip[] copies = effects.get(sfx);
        if (copies == null) {
            return;
        }
        long now = System.nanoTime();
        Long last = lastPlayed.get(sfx);
        if (last != null && now - last < sfx.minGapMillis() * 1_000_000L) {
            return;
        }
        lastPlayed.put(sfx, now);
        int index = nextCopy.merge(sfx, 1, Integer::sum) % copies.length;
        Clip clip = copies[index];
        if (clip != null) {
            clip.stop();
            clip.setFramePosition(0);
            clip.start();
        }
    }

    /** Switches the background song (starts it from the beginning if it changed). */
    public void playMusic(Music song) {
        if (song != currentSong) {
            Clip clip = songs.get(song);
            if (clip != null) {
                clip.setFramePosition(0);
            }
        }
        currentSong = song;
        refreshMusic();
    }

    /** Call after the Music option changes. */
    public void refreshMusic() {
        for (Map.Entry<Music, Clip> entry : songs.entrySet()) {
            Clip clip = entry.getValue();
            if (clip == null) {
                continue;
            }
            boolean shouldPlay = settings.isMusicOn() && entry.getKey() == currentSong;
            if (shouldPlay && !clip.isRunning()) {
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            } else if (!shouldPlay && clip.isRunning()) {
                clip.stop();
            }
        }
    }
}
