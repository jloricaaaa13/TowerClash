package model.ultimates;

import audio.Sfx;
import game.Battle;
import game.effects.DishRain;
import java.awt.Color;
import model.Side;
import model.Tower;
import model.UltimateSkill;
import model.Unit;

/**
 * Hospitality Ultimate: a rain of flaming dishes over the whole battlefield.
 * Burns every enemy unit, damages and burns the enemy tower, and heals your tower by 20%.
 */
public class KitchenInferno extends UltimateSkill {

    private static final double HEAL_RATIO = 0.12;

    public KitchenInferno() {
        super("Kitchen Inferno", "BURNS ALL ENEMIES, HEALS YOUR TOWER 12%", 30);
    }

    @Override
    protected void perform(Battle battle, Tower owner) {
        double power = owner.getUltimatePower();
        Side me = owner.getSide();
        Side enemy = me.opponent();

        for (Unit u : battle.unitsOf(enemy)) {
            battle.damage(u, 15 * power, me);
            u.applyBurn(6 * power, 4, me);
        }
        Tower enemyTower = battle.getTower(enemy);
        battle.damage(enemyTower, 40 * power, me);
        enemyTower.applyBurn(6 * power, 4, me);
        owner.heal(owner.getMaxHp() * HEAL_RATIO);

        battle.addEffect(new DishRain(battle.getRandom()));
        battle.playSound(Sfx.FIRE);
        battle.showBanner("KITCHEN INFERNO!", new Color(0xFF7A1A));
    }
}
