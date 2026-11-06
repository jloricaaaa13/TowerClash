package model.ultimates;

import audio.Sfx;
import game.Battle;
import game.effects.LaserBeam;
import java.awt.Color;
import model.Side;
import model.Tower;
import model.UltimateSkill;
import model.Unit;

/**
 * Computer Science Ultimate: a giant laser beam straight at the enemy tower.
 * Massive tower damage, hurts every enemy unit in the lane, and the enemy
 * cannot spawn units for 5 seconds.
 */
public class SystemOverload extends UltimateSkill {

    private static final double DISABLE_SECONDS = 5;

    public SystemOverload() {
        super("System Overload", "LASER THE ENEMY TOWER, BLOCK ITS SPAWNS 5S", 32);
    }

    @Override
    protected void perform(Battle battle, Tower owner) {
        double power = owner.getUltimatePower();
        Side me = owner.getSide();
        Side enemy = me.opponent();
        Tower enemyTower = battle.getTower(enemy);

        for (Unit u : battle.unitsOf(enemy)) {
            battle.damage(u, 40 * power, me);
        }
        battle.damage(enemyTower, 120 * power, me);
        battle.disableSpawning(enemy, DISABLE_SECONDS);

        int beamY = Battle.GROUND_Y - owner.launchHeight();
        battle.addEffect(new LaserBeam(owner.centerX(), enemyTower.centerX(), beamY));
        battle.playSound(Sfx.LASER);
        battle.showBanner("SYSTEM OVERLOAD!", new Color(0xFACC15));
    }
}
