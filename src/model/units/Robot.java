package model.units;

import game.Battle;
import game.effects.Puff;
import java.awt.Color;
import model.Role;
import model.Side;
import model.Unit;
import ui.pixel.PixelSprite;

/**
 * Computer Science - the tank. A slow metal robot.
 * Ability SELF-DESTRUCT: explodes when destroyed, hurting nearby enemy units.
 */
public class Robot extends Unit {

    private static final double BLAST_RANGE = 24;
    private static final double BLAST_DAMAGE = 30;

    public Robot(Side side) {
        super("Robot", Role.TANK, side, 310, 17, 1.4, 12, 10);
    }

    @Override
    public void onDeath(Battle battle) {
        for (Unit enemy : battle.unitsNear(side.opponent(), x, BLAST_RANGE)) {
            battle.damage(enemy, BLAST_DAMAGE, side);
        }
        for (int i = 0; i < 3; i++) {
            battle.addEffect(new Puff(x - 8 + i * 8, Battle.GROUND_Y - 6 - i % 2 * 4, new Color(0xFF8A3D)));
        }
        battle.shake(2);
    }

    @Override
    public String getAbilityName() {
        return "SELF-DESTRUCT";
    }

    @Override
    public String getAbilityText() {
        return "EXPLODES WHEN DESTROYED (30 DMG)";
    }

    // ---- Looks (pixel art: each letter is one pixel, '.' = see-through) ----

    private static final String COLORS = "m=CBD5E1 M=94A3B8 d=64748B y=EAB308 r=EF4444 q=0F172A c=38BDF8 t=374151 T=6B7280";

    private static final PixelSprite STAND = new PixelSprite(7, COLORS,
            "......y.........",
            "......d.........",
            "......d.........",
            "...MMMMMMM......",
            "..MmmmmmmmM.....",
            "..MqqqqqqqM.....",
            "..MqcqqqcqM.....",
            "..MqqqqqqqM.....",
            "..MmmmmmmmM.....",
            "...MMdMMMM......",
            ".MMMMMMMMMMM....",
            "MmmmmmmmmmmmM...",
            "MmmmmmmmmmmmMdd.",
            "MmyyyyyyyyymMdMM",
            "MmmmmmmmmmmmMdMm",
            "MmmmrmmmmmmmMdMM",
            "MmmmmmmmmmmmMdd.",
            "MmmmmmmmmmmmM...",
            ".MMMMMMMMMMM....",
            "..ddddddddd.....",
            ".ttttttttttt....",
            "tTtTtTtTtTtTt...",
            "tttttttttttt....",
            ".ttttttttttt....");
    private static final PixelSprite WALK = new PixelSprite(7, COLORS, // same, treads moved
            "......y.........",
            "......d.........",
            "......d.........",
            "...MMMMMMM......",
            "..MmmmmmmmM.....",
            "..MqqqqqqqM.....",
            "..MqcqqqcqM.....",
            "..MqqqqqqqM.....",
            "..MmmmmmmmM.....",
            "...MMdMMMM......",
            ".MMMMMMMMMMM....",
            "MmmmmmmmmmmmM...",
            "MmmmmmmmmmmmMdd.",
            "MmyyyyyyyyymMdMM",
            "MmmmmmmmmmmmMdMm",
            "MmmmrmmmmmmmMdMM",
            "MmmmmmmmmmmmMdd.",
            "MmmmmmmmmmmmM...",
            ".MMMMMMMMMMM....",
            "..ddddddddd.....",
            ".ttttttttttt....",
            "TtTtTtTtTtTtT...",
            "tttttttttttt....",
            ".ttttttttttt....");

    @Override
    protected PixelSprite getSprite(int frame, int tick) {
        return frame == 0 ? STAND : WALK;
    }
}
