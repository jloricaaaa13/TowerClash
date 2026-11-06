package model.towers;

import game.Battle;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;
import model.Department;
import model.Projectile;
import model.Side;
import model.Tower;
import model.Unit;
import model.UnitType;
import model.ultimates.KitchenInferno;
import model.units.Barista;
import model.units.Bellboy;
import model.units.Chef;
import model.units.HotelManager;
import model.units.Waiter;

/**
 * RED - Grand Hotel Tower.
 * Throws hot plates in an arc that BURN the target. Slowly heals itself (sustain).
 */
public class HospitalityTower extends Tower {

    private static final double BURN_DPS = 4;
    private static final double BURN_SECONDS = 3;

    /** All 5 Hospitality units. The first 3 are the starting lineup. */
    public static final List<UnitType> ROSTER = List.of(
            new UnitType("Waiter", 20, 2.0, Waiter::new),
            new UnitType("Chef", 45, 4.0, Chef::new),
            new UnitType("Bellboy", 70, 7.0, Bellboy::new),
            new UnitType("Barista", 50, 5.0, Barista::new),
            new UnitType("Manager", 75, 8.0, HotelManager::new));

    public HospitalityTower(Side side, int permanentLevel, double hpMultiplier, List<UnitType> lineup) {
        //                                              hp    dmg  interval range regen/s
        super(Department.HOSPITALITY, side, permanentLevel, hpMultiplier, 540, 9, 1.0, 90, 1.5,
                new KitchenInferno(), lineup);
    }

    @Override
    protected void attack(Battle battle, Unit target) {
        battle.addProjectile(new Projectile(Projectile.Kind.PLATE, side,
                centerX(), Battle.GROUND_Y - launchHeight(), target, 120, damage, 14,
                hit -> hit.applyBurn(BURN_DPS, BURN_SECONDS, side)));
    }

    @Override
    protected void drawEmblem(Graphics2D g, int left, int top, int tick) {
        // A golden hotel service bell above the door
        g.setColor(new Color(0xFACC15));
        g.fillRect(left + 11, top + 34, 2, 1);
        g.fillRect(left + 9, top + 35, 6, 2);
        g.setColor(new Color(0xCA8A04));
        g.fillRect(left + 8, top + 37, 8, 1);
    }
}
