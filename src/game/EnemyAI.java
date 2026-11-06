package game;

import java.util.Random;
import model.Role;
import model.Side;
import model.Stage;
import model.Tower;
import model.Unit;

/**
 * The computer opponent. Harder stages make it SMARTER, not just stronger:
 *
 *  1. ULTIMATE - waits until you have enough units on the field to be worth hitting
 *     (or uses it right away if its tower is in danger).
 *  2. UPGRADE  - buys a tower upgrade when hurt or when it has spare energy.
 *  3. DEFEND   - if your units get close to its tower, it spawns the unit that counters them.
 *  4. COUNTER  - looks at the roles you have on the field and sends the role that beats them
 *     (how often depends on the stage's aiSkill).
 *  5. WAVES    - on harder stages it saves energy, then sends a tank followed by support units.
 */
public class EnemyAI {

    private static final double DANGER_DISTANCE = 80;
    private static final Role[] WAVE_ORDER = {Role.TANK, Role.RANGED, Role.FAST, Role.RANGED, Role.FAST};

    private final Battle battle;
    private final Side side;
    private final Stage stage;
    private final Random random;
    private final int[] slotOfRole = new int[Role.values().length];
    private double thinkTimer = 1.0;
    private double ultimateReadyFor;
    private boolean sendingWave;
    private int waveStep;
    private int plannedSlot = -1;

    public EnemyAI(Battle battle, Side side, Stage stage, long seed) {
        this.battle = battle;
        this.side = side;
        this.stage = stage;
        this.random = new Random(seed);
        // Remember which spawn slot holds each role.
        // If the lineup has no unit of a role, use the cheapest unit instead.
        var types = battle.getTower(side).getUnitTypes();
        int cheapest = 0;
        for (int slot = 0; slot < types.size(); slot++) {
            if (types.get(slot).cost() < types.get(cheapest).cost()) {
                cheapest = slot;
            }
        }
        java.util.Arrays.fill(slotOfRole, cheapest);
        for (int slot = types.size() - 1; slot >= 0; slot--) {
            slotOfRole[types.get(slot).create(side).getRole().ordinal()] = slot;
        }
    }

    public void update(double dt) {
        if (battle.isOver()) {
            return;
        }
        handleUltimate(dt);

        thinkTimer -= dt;
        if (thinkTimer > 0) {
            return;
        }
        thinkTimer = stage.aiThinkTime() * (0.7 + random.nextDouble() * 0.6);

        Tower own = battle.getTower(side);
        if (own.getLevel() < stage.aiMaxUpgrades() && battle.canUpgrade(side)
                && (own.hpRatio() < 0.6 || battle.getEnergy(side) > 85)) {
            battle.upgradeTower(side);
            return;
        }

        int threats = enemiesNearTower();
        if (threats > 0) {
            sendingWave = false;
            spawnRole(smart() ? counterToOpponent() : Role.FAST);
            return;
        }

        if (stage.aiWaveEnergy() > 0) {
            playWaves();
        } else {
            playSimple();
        }
    }

    // ---- Ultimate ----

    private void handleUltimate(double dt) {
        Tower own = battle.getTower(side);
        if (!own.getUltimate().isReady()) {
            ultimateReadyFor = 0;
            return;
        }
        ultimateReadyFor += dt;
        double reaction = (1 - stage.aiSkill()) * 5; // clumsy AIs react slowly
        if (ultimateReadyFor < reaction) {
            return;
        }
        int targets = battle.unitsOf(side.opponent()).size();
        boolean worthIt = targets >= stage.aiUltimateTargets();
        boolean emergency = own.hpRatio() < 0.35;
        boolean waitedTooLong = ultimateReadyFor > 15;
        if (worthIt || emergency || waitedTooLong) {
            battle.useUltimate(side);
            ultimateReadyFor = 0;
        }
    }

    // ---- Spawning ----

    /** Easy stages: pick units one at a time, sometimes countering. */
    private void playSimple() {
        if (plannedSlot < 0) {
            Role role = smart() ? counterToOpponent() : randomRole();
            plannedSlot = slotOfRole[role.ordinal()];
        }
        if (battle.spawn(side, plannedSlot)) {
            plannedSlot = -1;
        }
    }

    /** Harder stages: save energy, then send a wave (tank in front, support behind). */
    private void playWaves() {
        if (!sendingWave) {
            if (battle.getEnergy(side) >= stage.aiWaveEnergy()) {
                sendingWave = true;
                waveStep = 0;
            } else {
                return; // saving up
            }
        }
        Role role = WAVE_ORDER[waveStep % WAVE_ORDER.length];
        if (waveStep > 0 && smart() && battle.unitsOf(side.opponent()).size() > 0) {
            role = counterToOpponent();
        }
        if (spawnRole(role)) {
            waveStep++;
        }
        if (battle.getEnergy(side) < 15 || waveStep >= WAVE_ORDER.length) {
            sendingWave = false;
        }
    }

    private boolean spawnRole(Role role) {
        return battle.spawn(side, slotOfRole[role.ordinal()]);
    }

    /** The role that beats the most common role the opponent has on the field. */
    private Role counterToOpponent() {
        int[] counts = battle.countRoles(side.opponent());
        Role most = null;
        int best = 0;
        for (Role r : Role.values()) {
            if (counts[r.ordinal()] > best) {
                best = counts[r.ordinal()];
                most = r;
            }
        }
        return most == null ? randomRole() : most.counteredBy();
    }

    private Role randomRole() {
        double r = random.nextDouble();
        return r < 0.40 ? Role.FAST : r < 0.75 ? Role.RANGED : Role.TANK;
    }

    private boolean smart() {
        return random.nextDouble() < stage.aiSkill();
    }

    private int enemiesNearTower() {
        Tower own = battle.getTower(side);
        int count = 0;
        for (Unit u : battle.unitsOf(side.opponent())) {
            if (Math.abs(u.getX() - own.frontX()) < DANGER_DISTANCE) {
                count++;
            }
        }
        return count;
    }
}
