package audio;

import java.util.Random;

/**
 * A tiny synthesizer: it makes every sound and song from math (sine-free "chiptune" waves),
 * so the game needs no audio files.
 *
 * A sound is an array of samples between -1 and 1, played 22050 times per second.
 */
final class Synth {

    static final float RATE = 22050f;

    enum Wave { SQUARE, TRIANGLE, SAW, NOISE }

    private Synth() {
    }

    // ------------------------------------------------------------------
    //  Building blocks
    // ------------------------------------------------------------------

    /** A tone that slides from startHz to endHz and fades out. */
    static float[] tone(Wave wave, double startHz, double endHz, double seconds, double volume) {
        int n = (int) (RATE * seconds);
        float[] out = new float[n];
        Random random = new Random(7);
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / n;
            phase += (startHz + (endHz - startHz) * t) / RATE;
            double attack = Math.min(1, i / (RATE * 0.004));
            out[i] = (float) (wave(wave, phase, random) * volume * attack * (1 - t));
        }
        return out;
    }

    /** A melody: one MIDI note number per step (0 = silence). */
    static float[] notes(Wave wave, int[] midi, double stepSeconds, double volume) {
        int stepLen = (int) (RATE * stepSeconds);
        float[] out = new float[stepLen * midi.length];
        Random random = new Random(11);
        for (int s = 0; s < midi.length; s++) {
            if (midi[s] <= 0) {
                continue;
            }
            double hz = midiToHz(midi[s]);
            double phase = 0;
            for (int i = 0; i < stepLen; i++) {
                double t = (double) i / stepLen;
                phase += hz / RATE;
                double attack = Math.min(1, i / (RATE * 0.004));
                double release = t > 0.85 ? (1 - t) / 0.15 : 1;
                double envelope = attack * release * (1 - 0.45 * t);
                out[s * stepLen + i] = (float) (wave(wave, phase, random) * volume * envelope);
            }
        }
        return out;
    }

    /** Short noise ticks (hi-hat) on every step where pattern is 1. */
    static float[] hats(int[] pattern, double stepSeconds, double volume) {
        int stepLen = (int) (RATE * stepSeconds);
        int hitLen = (int) (RATE * 0.03);
        float[] out = new float[stepLen * pattern.length];
        Random random = new Random(3);
        for (int s = 0; s < pattern.length; s++) {
            if (pattern[s] == 0) {
                continue;
            }
            for (int i = 0; i < hitLen; i++) {
                out[s * stepLen + i] = (float) ((random.nextDouble() * 2 - 1) * volume * (1 - (double) i / hitLen));
            }
        }
        return out;
    }

    private static double wave(Wave wave, double phase, Random random) {
        double p = phase - Math.floor(phase);
        return switch (wave) {
            case SQUARE -> p < 0.5 ? 1 : -1;
            case TRIANGLE -> 4 * Math.abs(p - 0.5) - 1;
            case SAW -> 2 * p - 1;
            case NOISE -> random.nextDouble() * 2 - 1;
        };
    }

    static double midiToHz(int midi) {
        return 440 * Math.pow(2, (midi - 69) / 12.0);
    }

    /** Plays several sounds at the same time (adds them together). */
    static float[] mix(float[]... parts) {
        int n = 0;
        for (float[] p : parts) {
            n = Math.max(n, p.length);
        }
        float[] out = new float[n];
        for (float[] p : parts) {
            for (int i = 0; i < p.length; i++) {
                out[i] += p[i];
            }
        }
        return out;
    }

    /** Plays sounds one after another. */
    static float[] concat(float[]... parts) {
        int n = 0;
        for (float[] p : parts) {
            n += p.length;
        }
        float[] out = new float[n];
        int pos = 0;
        for (float[] p : parts) {
            System.arraycopy(p, 0, out, pos, p.length);
            pos += p.length;
        }
        return out;
    }

    static float[] repeat(float[] part, int times) {
        float[][] parts = new float[times][];
        for (int i = 0; i < times; i++) {
            parts[i] = part;
        }
        return concat(parts);
    }

    /** Converts samples to 16-bit sound data that Java can play. */
    static byte[] toPcm16(float[] samples) {
        byte[] out = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            int v = (int) (Math.max(-1, Math.min(1, samples[i])) * 32767);
            out[i * 2] = (byte) v;
            out[i * 2 + 1] = (byte) (v >> 8);
        }
        return out;
    }

    // ------------------------------------------------------------------
    //  The actual sounds
    // ------------------------------------------------------------------

    static float[] effect(Sfx sfx) {
        return switch (sfx) {
            case CLICK -> tone(Wave.SQUARE, 880, 660, 0.05, 0.16);
            case SPAWN -> tone(Wave.SQUARE, 330, 700, 0.09, 0.16);
            case HIT -> mix(tone(Wave.NOISE, 0, 0, 0.05, 0.12), tone(Wave.SQUARE, 220, 110, 0.05, 0.08));
            case TOWER_HIT -> mix(tone(Wave.NOISE, 0, 0, 0.12, 0.18), tone(Wave.TRIANGLE, 110, 50, 0.15, 0.3));
            case DEATH -> tone(Wave.SQUARE, 520, 90, 0.2, 0.14);
            case UPGRADE -> notes(Wave.SQUARE, new int[]{72, 76, 79, 84}, 0.07, 0.18);
            case FIRE -> mix(tone(Wave.NOISE, 0, 0, 1.0, 0.22), tone(Wave.SAW, 220, 60, 1.0, 0.12));
            case FREEZE -> concat(tone(Wave.TRIANGLE, 1400, 900, 0.15, 0.25),
                    tone(Wave.TRIANGLE, 1800, 1200, 0.15, 0.2), tone(Wave.TRIANGLE, 2200, 1500, 0.3, 0.15));
            case LASER -> mix(tone(Wave.SAW, 900, 150, 0.8, 0.2), tone(Wave.SQUARE, 1800, 300, 0.8, 0.07));
            case VICTORY -> notes(Wave.SQUARE, new int[]{72, 76, 79, 84, 0, 79, 84, 84}, 0.12, 0.2);
            case DEFEAT -> notes(Wave.TRIANGLE, new int[]{67, 64, 60, 55, 55}, 0.22, 0.3);
            case ERROR -> tone(Wave.SQUARE, 160, 140, 0.12, 0.12);
            case STAR -> tone(Wave.SQUARE, 1047, 1568, 0.12, 0.16);
        };
    }

    /** Calm menu song in C major (8 seconds, loops). */
    static float[] menuMusic() {
        int[] melody = {
            72, 0, 76, 79, 76, 0, 74, 72,   69, 0, 72, 76, 74, 72, 71, 69,
            65, 69, 72, 0, 77, 76, 74, 72,  71, 74, 79, 0, 77, 74, 71, 74};
        int[] bass = {
            48, 0, 55, 0, 48, 0, 55, 0,     45, 0, 52, 0, 45, 0, 52, 0,
            41, 0, 48, 0, 41, 0, 48, 0,     43, 0, 50, 0, 43, 0, 50, 0};
        double step = 0.25;
        return mix(notes(Wave.TRIANGLE, melody, step, 0.11), notes(Wave.SQUARE, bass, step, 0.05));
    }

    /** Faster battle song in A minor (12.8 seconds, loops). */
    static float[] battleMusic() {
        int[] melody = {
            69, 72, 76, 72, 81, 79, 76, 72,  65, 69, 72, 69, 77, 76, 72, 69,
            67, 71, 74, 71, 79, 77, 74, 71,  64, 68, 71, 76, 74, 71, 68, 71};
        int[] bass = {
            45, 45, 57, 45, 45, 57, 45, 57,  41, 41, 53, 41, 41, 53, 41, 53,
            43, 43, 55, 43, 43, 55, 43, 55,  40, 40, 52, 40, 40, 52, 40, 52};
        int[] hat = {0, 1, 0, 1, 0, 1, 0, 1};
        double step = 0.2;
        float[] loop = mix(notes(Wave.SQUARE, melody, step, 0.06),
                notes(Wave.TRIANGLE, bass, step, 0.13),
                repeat(hats(hat, step, 0.05), 4));
        return repeat(loop, 2);
    }
}
