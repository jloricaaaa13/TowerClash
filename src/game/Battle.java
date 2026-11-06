package game;

import audio.Sfx;
import game.effects.DamageNumber;
import game.effects.Effect;
import game.effects.FloatText;
import game.effects.Puff;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import model.Combatant;
import model.Department;
import model.Match;
import model.Modifier;
import model.Projectile;
import model.Role;
import model.Side;
import model.Stage;
import model.Tower;
import model.Unit;
import model.UnitType;

/**
 * One match: two towers, the units on the field, projectiles, energy, and the rules.
 *
 * This class has NO Swing or sound code, so the same battle can be:
 *   - shown on screen by BattleScene (the player plays it), or
 *   - simulated very fast with two AIs (used to test the balance).
 * It only REPORTS sounds through a listener; BattleScene decides to play them.
 *
 * update(dt) moves the whole battle forward by dt seconds.
 */
public class Battle {

    public static final int GROUND_Y = 122;
    public static final double MAX_ENERGY = 100;
    public static final double ENERGY_PER_SECOND = 7;
    public static final double START_ENERGY = 40;
    public static final double OVERTIME_ENERGY_BONUS = 2;
    public static final double OVERTIME_TOWER_DAMAGE = 1.5;    // towers take +50% damage in overtime...
    public static final double OVERTIME_DAMAGE_PER_MINUTE = 0.5; // ...and +50% more every minute after
    public static final double FROZEN_DAMAGE_BONUS = 1.5;
    public static final int KILL_BOUNTY = 4;
    public static final double ENDLESS_WAVE_SECONDS = 30;
    private static final double BANNER_SECONDS = 1.8;

    private static final Color NUMBER_NORMAL = new Color(0xFFF7E6);
    private static final Color NUMBER_STRONG = new Color(0xFFD86E);
    private static final Color NUMBER_WEAK = new Color(0xA8A29E);
    private static final Color NUMBER_HEAL = new Color(0x6BE36B);

    private final Tower[] towers = new Tower[2];
    private final double[] energy = new double[2];
    private final double[] energyRate = new double[2];
    private final double[][] spawnCooldown = new double[2][Tower.LINEUP_SIZE];
    private final double[] spawnDisabled = new double[2];
    private final List<Unit> units = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<Projectile> newProjectiles = new ArrayList<>();
    private final List<Effect> effects = new ArrayList<>();
    private final List<Effect> newEffects = new ArrayList<>();
    private final Match match;
    private final Modifier modifier;
    private final Random random;
    private Consumer<Sfx> soundListener = sfx -> { };
    private Side localSide = Side.PLAYER; // the side controlled on THIS computer (for sounds and shaking)
    private boolean showNumbers = true;
    private Side winner;
    private String bannerText;
    private Color bannerColor;
    private double bannerTime;
    private double shake;
    private int endlessWave;

    public Battle(Tower playerTower, Tower enemyTower, double enemyEnergyRate, Stage stage, long seed) {
        towers[Side.PLAYER.ordinal()] = playerTower;
        towers[Side.ENEMY.ordinal()] = enemyTower;
        energy[0] = START_ENERGY;
        energy[1] = START_ENERGY;
        energyRate[Side.PLAYER.ordinal()] = 1.0;
        energyRate[Side.ENEMY.ordinal()] = enemyEnergyRate;
        match = new Match(stage, playerTower.getDepartment(), enemyTower.getDepartment());
        modifier = stage.modifier();
        random = new Random(seed);

        for (Tower t : towers) {
            t.scaleRange(modifier.towerRange());
            t.setHpRatio(modifier.towerStartHp());
        }
    }

    /**
     * Creates a normal campaign battle: the stage decides how strong the enemy is.
     * Each side brings its own lineup (the 3 units it can spawn).
     */
    public static Battle forStage(Department player, int playerTowerLevel, List<UnitType> playerLineup,
                                  Department enemy, List<UnitType> enemyLineup, Stage stage, long seed) {
        Tower playerTower = player.createTower(Side.PLAYER, playerTowerLevel, 1.0, playerLineup);
        Tower enemyTower = enemy.createTower(Side.ENEMY, stage.aiTowerLevel(), stage.aiTowerHp(), enemyLineup);
        return new Battle(playerTower, enemyTower, stage.aiEnergyRate(), stage, seed);
    }

    private static int index(Side side) {
        return side.ordinal();
    }

    // ------------------------------------------------------------------
    //  Game loop step
    // ------------------------------------------------------------------

    public void update(double dt) {
        updateEffects(dt);
        if (winner != null) {
            return;
        }
        boolean wasOvertime = isOvertime();
        match.addTime(dt);
        if (!wasOvertime && isOvertime()) {
            showBanner("OVERTIME! TOWERS WEAKEN", new Color(0xFFD86E));
        }
        if (match.getStage().endless()) {
            updateEndlessWaves();
        }
        double overtimeBonus = isOvertime() ? OVERTIME_ENERGY_BONUS : 1;

        for (Side side : Side.values()) {
            int i = index(side);
            energy[i] = Math.min(MAX_ENERGY, energy[i] + ENERGY_PER_SECOND * energyRate[i] * overtimeBonus * dt);
            for (int slot = 0; slot < Tower.LINEUP_SIZE; slot++) {
                spawnCooldown[i][slot] = Math.max(0, spawnCooldown[i][slot] - dt);
            }
            spawnDisabled[i] = Math.max(0, spawnDisabled[i] - dt);
            towers[i].getUltimate().update(dt);
        }

        // Status effects (burn damage, slow / freeze / flash timers)
        for (Tower t : towers) {
            tickStatus(t, dt);
        }
        for (int i = 0; i < units.size(); i++) {
            tickStatus(units.get(i), dt);
        }

        // Everyone acts
        for (Tower t : towers) {
            t.update(this, dt);
        }
        for (int i = 0; i < units.size(); i++) {
            units.get(i).update(this, dt);
        }
        for (int i = 0; i < projectiles.size(); i++) {
            projectiles.get(i).update(this, dt);
        }

        projectiles.removeIf(Projectile::isDone);
        projectiles.addAll(newProjectiles);
        newProjectiles.clear();
        units.removeIf(u -> !u.isAlive());
        checkWinner();
    }

    /** Endless mode: every 30 seconds the enemy gets 10% more energy. */
    private void updateEndlessWaves() {
        int wave = (int) (match.getElapsed() / ENDLESS_WAVE_SECONDS);
        if (wave > endlessWave) {
            endlessWave = wave;
            energyRate[index(Side.ENEMY)] += 0.1;
            showBanner("WAVE " + (wave + 1) + "!", new Color(0xFF9A8A));
        }
    }

    private void tickStatus(Combatant c, double dt) {
        double burn = c.tickEffects(dt);
        if (burn > 0 && c.getBurnSource() != null) {
            applyDamage(c, burn, c.getBurnSource(), null);
        }
    }

    private void updateEffects(double dt) {
        for (Effect e : effects) {
            e.update(dt);
        }
        effects.removeIf(Effect::isDone);
        effects.addAll(newEffects);
        newEffects.clear();
        if (bannerTime > 0) {
            bannerTime -= dt;
        }
        shake = Math.max(0, shake - dt * 12);
    }

    private void checkWinner() {
        Tower player = getTower(Side.PLAYER);
        Tower enemy = getTower(Side.ENEMY);
        if (!player.isAlive()) {
            winner = Side.ENEMY;
        } else if (!enemy.isAlive()) {
            winner = Side.PLAYER;
        }
        if (winner != null) {
            match.finish(winner, player.hpRatio());
            Tower loser = getTower(winner.opponent());
            for (int i = 0; i < 6; i++) {
                addEffect(new Puff(loser.centerX() - 10 + random.nextInt(20),
                        GROUND_Y - 10 - random.nextInt(40), new Color(0x6B6B6B)));
            }
            shake(7);
            playSound(Sfx.TOWER_HIT);
        }
    }

    // ------------------------------------------------------------------
    //  Damage (the only place where HP goes down)
    // ------------------------------------------------------------------

    /** Normal hit (towers, ultimates). */
    public void damage(Combatant target, double amount, Side source) {
        hit(target, amount, source, 1.0);
    }

    /**
     * A hit that flashes the target, shows a damage number, and makes a sound.
     * @param counterMultiplier only used to color the number (1.5 = gold, 0.7 = grey)
     */
    public void hit(Combatant target, double amount, Side source, double counterMultiplier) {
        if (!target.isAlive() || amount <= 0 || winner != null) {
            return;
        }
        Color color = counterMultiplier > 1 ? NUMBER_STRONG : counterMultiplier < 1 ? NUMBER_WEAK : NUMBER_NORMAL;
        target.flash();
        applyDamage(target, amount, source, color);
        if (target instanceof Tower tower) {
            playSound(Sfx.TOWER_HIT);
            if (tower.getSide() == localSide && amount >= 10) {
                shake(1.5); // your own tower shaking = danger
            }
        } else {
            playSound(Sfx.HIT);
        }
    }

    private void applyDamage(Combatant target, double amount, Side source, Color numberColor) {
        if (!target.isAlive() || amount <= 0 || winner != null) {
            return;
        }
        if (target.isFrozen()) {
            amount *= FROZEN_DAMAGE_BONUS;
        }
        if (target instanceof Tower) {
            amount *= getTowerDamageMultiplier();
        }
        double dealt = target.receiveDamage(amount);
        getTower(source).getUltimate().onDamageDealt(dealt);
        if (source == Side.PLAYER) {
            match.addDamage(dealt);
        }
        if (numberColor != null && showNumbers && amount >= 4) {
            addEffect(new DamageNumber(hitX(target) + random.nextInt(5) - 2, hitY(target) - 8,
                    (int) Math.round(amount), numberColor));
        }
        if (!target.isAlive() && target instanceof Unit unit) {
            energy[index(source)] = Math.min(MAX_ENERGY, energy[index(source)] + KILL_BOUNTY);
            if (source == Side.PLAYER) {
                match.addUnitDefeated();
            } else if (unit.getSide() == Side.PLAYER) {
                match.addUnitLost();
            }
            addEffect(new Puff(unit.getX(), hitY(unit), new Color(0xD6D6D6)));
            playSound(Sfx.DEATH);
            unit.onDeath(this); // e.g. the Robot explodes
        }
    }

    // ------------------------------------------------------------------
    //  Player / AI actions
    // ------------------------------------------------------------------

    public boolean canSpawn(Side side, int slot) {
        int i = index(side);
        Tower tower = towers[i];
        return winner == null && tower.isAlive() && spawnDisabled[i] <= 0
                && spawnCooldown[i][slot] <= 0 && energy[i] >= tower.getUnitTypes().get(slot).cost();
    }

    public boolean spawn(Side side, int slot) {
        if (!canSpawn(side, slot)) {
            return false;
        }
        int i = index(side);
        Tower tower = towers[i];
        UnitType type = tower.getUnitTypes().get(slot);
        Unit unit = type.create(side);
        double spawnX = tower.frontX() + side.direction() * 2;
        unit.placeAt(spawnX, freestLane(side, spawnX));
        unit.applyModifiers(modifier.unitSpeed(), side == Side.ENEMY ? modifier.enemyUnitHp() : 1.0);
        units.add(unit);
        unit.onSpawn(this); // e.g. the Coach blows the whistle
        energy[i] -= type.cost();
        spawnCooldown[i][slot] = type.spawnCooldown();
        if (side == localSide) {
            playSound(Sfx.SPAWN);
        }
        return true;
    }

    /** The lane with the most free space near the tower. */
    private int freestLane(Side side, double spawnX) {
        int best = 0;
        double bestGap = -1;
        for (int lane = 0; lane < Unit.LANES; lane++) {
            double gap = Double.MAX_VALUE;
            for (Unit u : units) {
                if (u.getSide() == side && u.getLane() == lane) {
                    gap = Math.min(gap, Math.abs(u.getX() - spawnX));
                }
            }
            if (gap > bestGap) {
                bestGap = gap;
                best = lane;
            }
        }
        return best;
    }

    public boolean canUpgrade(Side side) {
        Tower tower = getTower(side);
        int cost = tower.getUpgradeCost();
        return winner == null && tower.isAlive() && cost > 0 && energy[index(side)] >= cost;
    }

    public boolean upgradeTower(Side side) {
        if (!canUpgrade(side)) {
            return false;
        }
        Tower tower = getTower(side);
        energy[index(side)] -= tower.getUpgradeCost();
        tower.upgrade();
        addEffect(new FloatText(tower.centerX(), GROUND_Y - Tower.HEIGHT - 16,
                "LEVEL " + tower.getLevel() + "!", new Color(0xFFD86E)));
        if (side == localSide) {
            playSound(Sfx.UPGRADE);
        }
        return true;
    }

    public boolean useUltimate(Side side) {
        Tower tower = getTower(side);
        return winner == null && tower.isAlive() && tower.getUltimate().activate(this, tower);
    }

    /** Gives energy to a side (Waiter tips, Hacker). */
    public void addEnergy(Side side, double amount) {
        int i = index(side);
        energy[i] = Math.max(0, Math.min(MAX_ENERGY, energy[i] + amount));
    }

    /** Takes up to 'amount' energy away from a side and returns how much was really taken. */
    public double drainEnergy(Side side, double amount) {
        int i = index(side);
        double taken = Math.min(energy[i], amount);
        energy[i] -= taken;
        return taken;
    }

    /** Heals a unit or tower and shows a green number. */
    public void healTarget(Combatant target, double amount) {
        if (!target.isAlive() || winner != null || target.getHp() >= target.getMaxHp()) {
            return;
        }
        double before = target.getHp();
        target.heal(amount);
        int healed = (int) Math.round(target.getHp() - before);
        if (showNumbers && healed >= 2) {
            addEffect(new DamageNumber(hitX(target), hitY(target) - 8, "+" + healed, NUMBER_HEAL));
        }
    }

    public void disableSpawning(Side side, double seconds) {
        spawnDisabled[index(side)] = Math.max(spawnDisabled[index(side)], seconds);
    }

    // ------------------------------------------------------------------
    //  Targeting and movement checks along the lane
    // ------------------------------------------------------------------

    /** The closest enemy in front of the unit within its range, or the enemy tower if it is in range. */
    public Combatant findTargetFor(Unit unit) {
        Side enemy = unit.getSide().opponent();
        int dir = unit.getSide().direction();
        Unit best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Unit other : units) {
            if (!unit.targetsUnits() || other.getSide() != enemy || !other.isAlive()) {
                continue;
            }
            double d = (other.getX() - unit.getX()) * dir;
            if (d >= -4 && d <= unit.getRange() && d < bestDistance) {
                best = other;
                bestDistance = d;
            }
        }
        if (best != null) {
            return best;
        }
        Tower tower = getTower(enemy);
        if (tower.isAlive() && (tower.frontX() - unit.getX()) * dir <= unit.getRange()) {
            return tower;
        }
        return null;
    }

    /** True if a teammate in the same lane is standing right in front (units form a line). */
    public boolean isBlockedByTeammate(Unit unit) {
        int dir = unit.getSide().direction();
        for (Unit other : units) {
            if (other == unit || other.getSide() != unit.getSide() || other.getLane() != unit.getLane()
                    || !other.isAlive()) {
                continue;
            }
            double d = (other.getX() - unit.getX()) * dir;
            boolean inFront = d > 0 || (d == 0 && other.getId() < unit.getId());
            if (inFront && d < Unit.LANE_SPACING) {
                return true;
            }
        }
        return false;
    }

    /** Living units of a side within 'range' pixels of x (used by area abilities). */
    public List<Unit> unitsNear(Side side, double x, double range) {
        List<Unit> result = new ArrayList<>();
        for (Unit u : units) {
            if (u.getSide() == side && u.isAlive() && Math.abs(u.getX() - x) <= range) {
                result.add(u);
            }
        }
        return result;
    }

    /** The enemy unit closest to the tower, if it is within range. */
    public Unit nearestEnemyUnit(Tower tower, double range) {
        int dir = tower.getSide().direction();
        Unit best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Unit u : units) {
            if (u.getSide() == tower.getSide() || !u.isAlive()) {
                continue;
            }
            double d = (u.getX() - tower.frontX()) * dir;
            if (d >= -Tower.WIDTH && d <= range && d < bestDistance) {
                best = u;
                bestDistance = d;
            }
        }
        return best;
    }

    /** How many units of each role a side has on the field (used by the AI to pick counters). */
    public int[] countRoles(Side side) {
        int[] counts = new int[Role.values().length];
        for (Unit u : units) {
            if (u.getSide() == side && u.isAlive()) {
                counts[u.getRole().ordinal()]++;
            }
        }
        return counts;
    }

    public double hitX(Combatant c) {
        return c instanceof Tower t ? t.centerX() : c.getX();
    }

    public double hitY(Combatant c) {
        return GROUND_Y - c.getHitHeight();
    }

    // ------------------------------------------------------------------
    //  Adding things / visuals / sound
    // ------------------------------------------------------------------

    public void addProjectile(Projectile projectile) {
        newProjectiles.add(projectile);
    }

    public void addEffect(Effect effect) {
        newEffects.add(effect);
    }

    public void showBanner(String text, Color color) {
        bannerText = text;
        bannerColor = color;
        bannerTime = BANNER_SECONDS;
    }

    /** Shakes the screen (bigger number = stronger). */
    public void shake(double amount) {
        shake = Math.max(shake, amount);
    }

    public void playSound(Sfx sfx) {
        soundListener.accept(sfx);
    }

    /** LAN 1v1: the player who joined controls Side.ENEMY on their computer. */
    public void setLocalSide(Side side) {
        this.localSide = side;
    }

    public void setSoundListener(Consumer<Sfx> listener) {
        this.soundListener = listener == null ? sfx -> { } : listener;
    }

    /** Turned off in the balance simulator (no one is watching). */
    public void setShowNumbers(boolean showNumbers) {
        this.showNumbers = showNumbers;
    }

    // ------------------------------------------------------------------
    //  Getters
    // ------------------------------------------------------------------

    public Tower getTower(Side side) {
        return towers[index(side)];
    }

    public List<Unit> unitsOf(Side side) {
        List<Unit> result = new ArrayList<>();
        for (Unit u : units) {
            if (u.getSide() == side && u.isAlive()) {
                result.add(u);
            }
        }
        return result;
    }

    public List<Unit> getUnits() {
        return units;
    }

    public List<Projectile> getProjectiles() {
        return projectiles;
    }

    public List<Effect> getEffects() {
        return effects;
    }

    public double getEnergy(Side side) {
        return energy[index(side)];
    }

    /** 0 = ready, 1 = just spawned. */
    public double getSpawnCooldownRatio(Side side, int slot) {
        double total = getTower(side).getUnitTypes().get(slot).spawnCooldown();
        return total <= 0 ? 0 : spawnCooldown[index(side)][slot] / total;
    }

    public double getSpawnDisabledTime(Side side) {
        return spawnDisabled[index(side)];
    }

    public boolean isOver() {
        return winner != null;
    }

    public boolean isOvertime() {
        return !match.getStage().endless() && match.getElapsed() >= modifier.overtimeStart();
    }

    /** 1.0 normally; in overtime 1.5x, then +0.5x every minute, so every match ends. */
    public double getTowerDamageMultiplier() {
        if (!isOvertime()) {
            return 1.0;
        }
        double minutesInOvertime = (match.getElapsed() - modifier.overtimeStart()) / 60.0;
        return OVERTIME_TOWER_DAMAGE + OVERTIME_DAMAGE_PER_MINUTE * Math.floor(minutesInOvertime);
    }

    public Side getWinner() {
        return winner;
    }

    public Match getMatch() {
        return match;
    }

    public Modifier getModifier() {
        return modifier;
    }

    public Random getRandom() {
        return random;
    }

    public String getBannerText() {
        return bannerTime > 0 ? bannerText : null;
    }

    public Color getBannerColor() {
        return bannerColor;
    }

    public double getShake() {
        return shake;
    }

    public int getEndlessWave() {
        return endlessWave + 1;
    }
}
