package model;

import java.awt.Color;

/** How a stage looks: sky colors, clouds, a color tint over the world, stars, rain, lightning. */
public enum StageTheme {

    DAY(new int[]{0x3CAEE3, 0x4FB8E6, 0x66C3E7, 0x84D0E6, 0xA6DEE3},
            0xFFFFFF, 0xD4EDF5, 0xCBEAF2, null, false, false, false),
    SUNSET(new int[]{0x3B2A6B, 0x7A3E7E, 0xC2557A, 0xF08A5D, 0xF9C46B},
            0xFFD1A1, 0xE89A7A, 0xF8B38A, new Color(255, 120, 60, 45), false, false, false),
    NIGHT(new int[]{0x0B1026, 0x131A3A, 0x1C2550, 0x27336A, 0x34437F},
            0x3B4675, 0x2C355C, 0x2E3A66, new Color(20, 30, 90, 115), true, false, false),
    RAIN(new int[]{0x5B6770, 0x69757E, 0x78848C, 0x87939A, 0x96A2A8},
            0x9AA3AA, 0x7C868E, 0xA9B3B9, new Color(40, 50, 70, 70), false, true, false),
    STORM(new int[]{0x1A1426, 0x261A33, 0x35213F, 0x45294B, 0x553258},
            0x3A2E48, 0x2A2136, 0x4A3A5A, new Color(45, 10, 60, 105), false, true, true);

    private final Color[] sky;
    private final Color cloud;
    private final Color cloudShade;
    private final Color haze;
    private final Color tint;
    private final boolean stars;
    private final boolean rain;
    private final boolean lightning;

    StageTheme(int[] sky, int cloud, int cloudShade, int haze, Color tint,
               boolean stars, boolean rain, boolean lightning) {
        this.sky = new Color[sky.length];
        for (int i = 0; i < sky.length; i++) {
            this.sky[i] = new Color(sky[i]);
        }
        this.cloud = new Color(cloud);
        this.cloudShade = new Color(cloudShade);
        this.haze = new Color(haze);
        this.tint = tint;
        this.stars = stars;
        this.rain = rain;
        this.lightning = lightning;
    }

    public Color[] getSky() {
        return sky.clone();
    }

    public Color getCloud() {
        return cloud;
    }

    public Color getCloudShade() {
        return cloudShade;
    }

    public Color getHaze() {
        return haze;
    }

    /** Transparent color painted over trees/ground/towers (null = none). */
    public Color getTint() {
        return tint;
    }

    public boolean hasStars() {
        return stars;
    }

    public boolean hasRain() {
        return rain;
    }

    public boolean hasLightning() {
        return lightning;
    }
}
