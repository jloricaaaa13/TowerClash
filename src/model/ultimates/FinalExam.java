package model.ultimates;

import audio.Sfx;
import game.Battle;
import game.effects.ScreenFlash;
import java.awt.Color;
import model.Side;
import model.Tower;
import model.UltimateSkill;
import model.Unit;

/**
 * Education Ultimate: FREEZES all enemy units AND the enemy tower for 5 seconds.
 * Frozen enemies cannot move or attack, and take +50% damage (handled in Battle.damage).
 */
public class FinalExam extends UltimateSkill {

    private static final double FREEZE_SECONDS = 5;

    public FinalExam() {
        super("Final Exam", "FREEZES ALL ENEMIES + TOWER FOR 5 SECONDS", 28);
    }

    @Override
    protected void perform(Battle battle, Tower owner) {
        double power = owner.getUltimatePower();
        Side me = owner.getSide();
        Side enemy = me.opponent();

        for (Unit u : battle.unitsOf(enemy)) {
            u.freeze(FREEZE_SECONDS);
            battle.damage(u, 30 * power, me);
        }
        Tower enemyTower = battle.getTower(enemy);
        enemyTower.freeze(FREEZE_SECONDS);
        battle.damage(enemyTower, 60 * power, me);

        battle.addEffect(new ScreenFlash(new Color(190, 235, 255)));
        battle.playSound(Sfx.FREEZE);
        battle.showBanner("FINAL EXAM!", new Color(0x7DD3FC));
    }
}
