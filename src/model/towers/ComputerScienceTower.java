package model.towers;

import game.Battle;
import game.effects.Zap;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import model.Combatant;
import model.Department;
import model.Projectile;
import model.Side;
import model.Tower;
import model.Unit;
import model.UnitType;
import model.ultimates.SystemOverload;
import model.units.BugFixer;
import model.units.Drone;
import model.units.Hacker;
import model.units.Programmer;
import model.units.Robot;

/**
 * YELLOW - Server Tower.
 * Very fast attacks. Each bolt CHAINS to 1 more enemy nearby (2 more at tower level 3)
 * for half damage.
 */
public class ComputerScienceTower extends Tower {

    private static final double CHAIN_RANGE = 35;
    private static final double CHAIN_DAMAGE = 0.4;

    /** All 5 Computer Science units. The first 3 are the starting lineup. */
    public static final List<UnitType> ROSTER = List.of(
            new UnitType("Bug Fixer", 20, 2.0, BugFixer::new),
            new UnitType("Programmer", 50, 4.5, Programmer::new),
            new UnitType("Robot", 80, 8.0, Robot::new),
            new UnitType("Hacker", 55, 5.0, Hacker::new),
            new UnitType("Drone", 35, 5.0, Drone::new));

    public ComputerScienceTower(Side side, int permanentLevel, double hpMultiplier, List<UnitType> lineup) {
        //                                                   hp   dmg interval range regen/s
        super(Department.COMPUTER_SCIENCE, side, permanentLevel, hpMultiplier, 550, 6, 0.5, 95, 0,
                new SystemOverload(), lineup);
    }

    @Override
    protected void attack(Battle battle, Unit target) {
        battle.addProjectile(new Projectile(Projectile.Kind.BOLT, side,
                centerX(), Battle.GROUND_Y - launchHeight(), target, 220, damage, 0,
                hit -> chain(battle, hit)));
    }

    /** Jumps from the enemy that was hit to the nearest other enemies. */
    private void chain(Battle battle, Combatant first) {
        int jumps = getLevel() >= 3 ? 2 : 1;
        List<Combatant> alreadyHit = new ArrayList<>();
        alreadyHit.add(first);
        Combatant from = first;
        for (int i = 0; i < jumps; i++) {
            Unit next = null;
            double best = CHAIN_RANGE;
            for (Unit u : battle.unitsOf(side.opponent())) {
                double d = Math.abs(u.getX() - from.getX());
                if (u.isAlive() && !alreadyHit.contains(u) && d <= best) {
                    best = d;
                    next = u;
                }
            }
            if (next == null) {
                return;
            }
            battle.addEffect(new Zap(battle.hitX(from), battle.hitY(from), battle.hitX(next), battle.hitY(next)));
            battle.damage(next, damage * CHAIN_DAMAGE, side);
            alreadyHit.add(next);
            from = next;
        }
    }

    @Override
    protected void drawEmblem(Graphics2D g, int left, int top, int tick) {
        // Blinking server lights down the middle
        for (int i = 0; i < 4; i++) {
            boolean on = ((tick / 6) + i) % 3 != 0;
            g.setColor(on ? new Color(0x4ADE80) : new Color(0x14532D));
            g.fillRect(left + 11, top + 5 + i * 8, 2, 1);
        }
    }
}
