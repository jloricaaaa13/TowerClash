package model;

import game.Battle;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;
import ui.pixel.PixelArt;

/**
 * A department tower (abstract).
 *
 * Every tower shoots the nearest enemy unit in range, can be upgraded 2 times
 * during a match, owns an Ultimate Skill, and brings a LINEUP of 3 units
 * (picked from the department's 5 units on the Lineup screen).
 * What is DIFFERENT for each department is filled in by the subclasses:
 *   - attack()       : what it throws and what the hit does (burn / slow / chain)
 *   - drawEmblem()   : small decoration so each tower looks different
 *
 * Permanent upgrades (Upgrades screen, level 1-3) make the tower stronger for every match:
 *   +15% HP and damage per level, +25% Ultimate power per level.
 */
public abstract class Tower extends Combatant {

    public static final int WIDTH = 24;
    public static final int HEIGHT = 54;
    public static final int MARGIN = 8;          // gap between the tower and the screen edge
    public static final int MAX_LEVEL = 3;
    public static final int LINEUP_SIZE = 3;     // units you can bring into one match
    public static final String UPGRADE_TEXT = "+20% HP, +25% DAMAGE, +12 RANGE";
    private static final int[] UPGRADE_COST = {0, 60, 100}; // energy to reach level 2 and 3

    private final Department department;
    private final UltimateSkill ultimate;
    private final List<UnitType> unitTypes;
    private final int permanentLevel;
    private final double regenPerSecond;
    protected double damage;
    protected double range;
    protected final double attackInterval;
    private double attackCooldown;
    private int level = 1;

    protected Tower(Department department, Side side, int permanentLevel, double hpMultiplier,
                    double baseHp, double baseDamage, double attackInterval, double range,
                    double regenPerSecond, UltimateSkill ultimate, List<UnitType> unitTypes) {
        super(side, side == Side.PLAYER ? MARGIN : PixelArt.WIDTH - MARGIN - WIDTH,
                baseHp * permanentBonus(permanentLevel) * hpMultiplier);
        this.department = department;
        this.permanentLevel = permanentLevel;
        this.damage = baseDamage * permanentBonus(permanentLevel);
        this.attackInterval = attackInterval;
        this.range = range;
        this.regenPerSecond = regenPerSecond;
        this.ultimate = ultimate;
        if (unitTypes.size() != LINEUP_SIZE) {
            throw new IllegalArgumentException("A lineup must have exactly " + LINEUP_SIZE + " units");
        }
        this.unitTypes = List.copyOf(unitTypes);
    }

    private static double permanentBonus(int permanentLevel) {
        return 1 + 0.15 * (permanentLevel - 1);
    }

    /** Fire at the target (each department does this differently). */
    protected abstract void attack(Battle battle, Unit target);

    /** Small decoration drawn on the tower. */
    protected abstract void drawEmblem(Graphics2D g, int left, int top, int tick);

    /** Called every game step. */
    public void update(Battle battle, double dt) {
        if (!isAlive() || isFrozen()) {
            return;
        }
        heal(regenPerSecond * dt);
        attackCooldown -= dt;
        if (attackCooldown <= 0) {
            Unit target = battle.nearestEnemyUnit(this, range);
            if (target != null) {
                attack(battle, target);
                attackCooldown = attackInterval;
            }
        }
    }

    // ---- In-match upgrades ----

    /** Energy needed for the next level, or -1 if already at max level. */
    public int getUpgradeCost() {
        return level < MAX_LEVEL ? UPGRADE_COST[level] : -1;
    }

    /** +20% max HP (the new HP is added), +25% damage, +12 range. */
    public void upgrade() {
        if (level >= MAX_LEVEL) {
            return;
        }
        level++;
        double extraHp = maxHp * 0.20;
        maxHp += extraHp;
        heal(extraHp);
        damage *= 1.25;
        range += 12;
    }

    /** Used by the Power Outage modifier. */
    public void scaleRange(double multiplier) {
        range *= multiplier;
    }

    // ---- Positions ----

    /** x of the side of the tower that faces the enemy. */
    public double frontX() {
        return side == Side.PLAYER ? x + WIDTH : x;
    }

    public double centerX() {
        return x + WIDTH / 2.0;
    }

    @Override
    public int getHitHeight() {
        return HEIGHT / 2;
    }

    /** Where projectiles leave the tower (near the top). */
    public int launchHeight() {
        return HEIGHT - 8;
    }

    // ---- Drawing ----

    public void draw(Graphics2D g, int groundY, int tick) {
        int left = (int) x;
        int top = groundY - HEIGHT;
        if (!isAlive()) {
            drawRubble(g, left, groundY);
            return;
        }
        PixelArt.tower(g, left, groundY, WIDTH, HEIGHT, department.getColor(), tick);
        drawEmblem(g, left, top, tick);
        drawCracks(g, left, top);

        if (isFlashing()) {
            g.setColor(new Color(255, 255, 255, 110));
            g.fillRect(left, top - 4, WIDTH, HEIGHT + 4);
        }

        if (isFrozen()) {
            g.setColor(new Color(180, 230, 255, 140));
            g.fillRect(left - 2, top - 6, WIDTH + 4, HEIGHT + 6);
            g.setColor(new Color(235, 250, 255));
            g.fillRect(left - 2, top - 6, WIDTH + 4, 1);
        }
        if (isBurning()) {
            for (int i = 0; i < 4; i++) {
                int fx = left + 2 + i * 6 + (tick / 3 + i) % 2;
                int fh = 2 + (tick / 2 + i * 3) % 3;
                g.setColor(new Color(0xFF7A1A));
                g.fillRect(fx, top - 5 - fh, 2, fh);
                g.setColor(new Color(0xFFD23F));
                g.fillRect(fx, top - 5 - fh / 2, 1, fh / 2 + 1);
            }
        }
    }

    /** Cracks appear below 66% HP, and more below 33%. */
    private void drawCracks(Graphics2D g, int left, int top) {
        double ratio = hpRatio();
        if (ratio >= 0.66) {
            return;
        }
        g.setColor(department.getColor().darker().darker().darker());
        // first crack: zig-zag from the top-left
        int[][] crack1 = {{3, 4}, {4, 5}, {4, 6}, {5, 7}, {6, 8}, {6, 9}, {5, 10}, {6, 11}, {7, 12}};
        for (int[] p : crack1) {
            g.fillRect(left + p[0], top + p[1], 1, 1);
        }
        if (ratio < 0.33) {
            int[][] crack2 = {{19, 20}, {18, 21}, {18, 22}, {17, 23}, {16, 24}, {17, 25}, {16, 26},
                {15, 27}, {15, 28}, {14, 29}, {20, 34}, {19, 35}, {20, 36}, {21, 37}};
            for (int[] p : crack2) {
                g.fillRect(left + p[0], top + p[1], 1, 1);
            }
            g.fillRect(left + 2, top - 5, 3, 2); // broken battlement
        }
    }

    private void drawRubble(Graphics2D g, int left, int groundY) {
        Color c = department.getColor().darker().darker();
        g.setColor(c);
        g.fillRect(left, groundY - 8, WIDTH, 8);
        g.fillRect(left + 3, groundY - 13, 8, 5);
        g.fillRect(left + 13, groundY - 11, 7, 3);
        g.setColor(c.darker());
        g.fillRect(left + 5, groundY - 5, 3, 2);
        g.fillRect(left + 15, groundY - 6, 4, 2);
    }

    // ---- Getters ----

    public Department getDepartment() {
        return department;
    }

    public UltimateSkill getUltimate() {
        return ultimate;
    }

    /** The 3 units in this tower's lineup (slot 0, 1, 2 = keys 1, 2, 3). */
    public List<UnitType> getUnitTypes() {
        return unitTypes;
    }

    public int getLevel() {
        return level;
    }

    public int getPermanentLevel() {
        return permanentLevel;
    }

    public double getDamage() {
        return damage;
    }

    public double getRange() {
        return range;
    }

    public double getAttackInterval() {
        return attackInterval;
    }

    /** 1.0 at permanent level 1, 1.25 at level 2, 1.5 at level 3. */
    public double getUltimatePower() {
        return 1 + 0.25 * (permanentLevel - 1);
    }
}
