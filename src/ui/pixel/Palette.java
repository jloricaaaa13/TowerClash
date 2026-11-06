package ui.pixel;

import java.awt.Color;

/** All colors used by the pixel-art screens. */
public final class Palette {

    // Sky (top to bottom)
    public static final Color[] SKY = {
        rgb(0x3CAEE3), rgb(0x4FB8E6), rgb(0x66C3E7), rgb(0x84D0E6), rgb(0xA6DEE3)
    };
    public static final Color CLOUD = rgb(0xFFFFFF);
    public static final Color CLOUD_SHADE = rgb(0xD4EDF5);
    public static final Color HAZE = rgb(0xCBEAF2);
    public static final Color HILL_FAR = rgb(0xA3D9C0);
    public static final Color HILL_NEAR = rgb(0x8BCDA9);

    // Trees
    public static final Color TREE = rgb(0x5CB87A);
    public static final Color TREE_SHADE = rgb(0x46A064);
    public static final Color TREE_FAR = rgb(0x93D1AC);
    public static final Color TREE_FAR_SHADE = rgb(0x80C49B);
    public static final Color TRUNK = rgb(0x7A4E2D);

    // Ground
    public static final Color GRASS_LIGHT = rgb(0x9BE564);
    public static final Color GRASS = rgb(0x72C84C);
    public static final Color GRASS_DARK = rgb(0x4E9E3A);
    public static final Color DIRT = rgb(0x9C6B5E);
    public static final Color DIRT_LIGHT = rgb(0xB98C7C);
    public static final Color DIRT_DARK = rgb(0x7B5049);
    public static final Color BEDROCK = rgb(0x6B4463);
    public static final Color BEDROCK_DARK = rgb(0x573752);
    public static final Color ROCK = rgb(0x8A8F99);
    public static final Color ROCK_LIGHT = rgb(0xB9BEC7);

    // Wood (buttons and boards)
    public static final Color WOOD = rgb(0xCB8D50);
    public static final Color WOOD_HOVER = rgb(0xDEA266);
    public static final Color WOOD_LIGHT = rgb(0xE9B777);
    public static final Color WOOD_DARK = rgb(0x93602F);
    public static final Color WOOD_GRAIN = rgb(0xAE7542);
    public static final Color WOOD_OUTLINE = rgb(0x3B2414);
    public static final Color WOOD_DISABLED = rgb(0x9E9083);

    // Text
    public static final Color TEXT_GOLD = rgb(0xFFD86E);
    public static final Color TEXT_WHITE = rgb(0xFFF7E6);
    public static final Color TEXT_DARK = rgb(0x2E1B0F);
    public static final Color TEXT_DISABLED = rgb(0xC9BFB3);
    public static final Color TEXT_ON = rgb(0xA8F07A);
    public static final Color TEXT_OFF = rgb(0xFF9A8A);

    // HUD
    public static final Color ENERGY = rgb(0x38BDF8);
    public static final Color GOLD = rgb(0xFACC15);
    public static final Color GOLD_DARK = rgb(0xCA8A04);
    public static final Color BAR_BACK = rgb(0x3A2424);
    public static final Color HP_GOOD = rgb(0x4ADE80);
    public static final Color HP_MID = rgb(0xFACC15);
    public static final Color HP_LOW = rgb(0xEF4444);
    public static final Color PANEL = rgb(0x2A1A10);

    // Misc
    public static final Color WINDOW_LIGHT = rgb(0xFFE38A);
    public static final Color SIGN_ARROW = rgb(0xC0392B);
    public static final Color SHADOW = new Color(0, 0, 0, 70);
    public static final Color DIM = new Color(10, 6, 20, 140);
    public static final Color LETTERBOX = rgb(0x17111D);

    private Palette() {
    }

    public static Color rgb(int value) {
        return new Color(value);
    }

    public static Color lighter(Color c, float amount) {
        return mix(c, Color.WHITE, amount);
    }

    public static Color darker(Color c, float amount) {
        return mix(c, Color.BLACK, amount);
    }

    /** Mixes two colors: amount 0 = all a, amount 1 = all b. */
    public static Color mix(Color a, Color b, float amount) {
        float keep = 1 - amount;
        return new Color(
                Math.round(a.getRed() * keep + b.getRed() * amount),
                Math.round(a.getGreen() * keep + b.getGreen() * amount),
                Math.round(a.getBlue() * keep + b.getBlue() * amount));
    }
}
