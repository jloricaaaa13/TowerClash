package model;

import game.Battle;
import java.awt.Color;
import java.awt.Graphics2D;
import ui.pixel.PixelSprite;
import ui.pixel.SpritePen;

/**
 * A soldier that walks toward the enemy tower (abstract).
 *
 * The walking, targeting, and attacking logic is the same for every unit, so it lives here.
 * Each subclass only decides:
 *   - its role (FAST / RANGED / TANK) and stats (passed to the constructor)
 *   - how it looks: getSprite() returns a PixelSprite (pixel art written as text)
 *     and drawExtras() can add small animated details (steam, blinking lights)
 *   - its ABILITY, using these "hooks" (methods the Battle calls at the right moment):
 *       onSpawn()        right after it enters the field     (Coach's whistle)
 *       updateAbility()  every game step while it is alive    (Barista heals, Librarian slows)
 *       onHit()          after each of its attacks lands      (Chef burns, Hacker steals energy)
 *       onDeath()        when it is destroyed                 (Robot explodes)
 *       targetsUnits()   false = ignores units, only hits towers (Drone)
 *     and getAbilityName() / getAbilityText() to explain it on the Lineup screen and tooltips.
 *   - optional projectile look for ranged units: projectileKind()
 *
 * Polymorphism: the Battle just calls unit.onHit(...), unit.onDeath(...) and so on;
 * each subclass decides what really happens.
 *
 * Units walk in one of 3 lanes. A unit waits if a teammate in the same lane is right in front
 * of it, so armies form lines instead of one big blob.
 */
public abstract class Unit extends Combatant {

    public static final int LANES = 3;
    public static final int LANE_SPACING = 13; // pixels between units in the same lane
    public static final int SPRITE_SCALE = 1; // 1 = sprites are drawn at their real (detailed) size
    public static final double SIEGE_BONUS = 1.5; // units deal extra damage to towers
    private static final SpritePen PEN = new SpritePen();
    private static Side friendlySide = Side.PLAYER; // units of this side get green HP bars
    private static int nextId;

    private final int id = nextId++;
    private final String name;
    private final Role role;
    protected double damage;
    protected final double attackInterval;
    protected final double range;
    protected double speed;
    private double attackCooldown;
    private double attackAnimation;
    private double walked;
    private double aliveTime;
    private int attacksMade;
    private int lane;
    private boolean moving;

    protected Unit(String name, Role role, Side side, double hp, double damage, double attackInterval,
                   double range, double speed) {
        super(side, 0, hp);
        this.name = name;
        this.role = role;
        this.damage = damage;
        this.attackInterval = attackInterval;
        this.range = range;
        this.speed = speed;
    }

    /**
     * How the unit looks (facing right; it is mirrored automatically for the enemy).
     * @param frame 0 or 1 (walking animation)
     */
    protected abstract PixelSprite getSprite(int frame, int tick);

    /** Optional small animated details drawn on top of the sprite. Default: nothing. */
    protected void drawExtras(SpritePen pen, int frame, int tick) {
    }

    /** How high above the ground the unit floats (Drone). Default: 0 = walks on the ground. */
    protected int hoverHeight(int tick) {
        return 0;
    }

    /** Sprite height in pixels (at scale 1). */
    public int getHeight() {
        return getSprite(0, 0).height();
    }

    /** Height on the battlefield (sprite height x SPRITE_SCALE): for the HP bar and where projectiles hit. */
    public int bodyHeight() {
        return getHeight() * SPRITE_SCALE;
    }

    // ---- Ability hooks (subclasses override the ones they need) ----

    /** Called once, right after the unit is spawned. Default: nothing. */
    public void onSpawn(Battle battle) {
    }

    /** Called every game step while the unit is alive and not frozen. Default: nothing. */
    protected void updateAbility(Battle battle, double dt) {
    }

    /** Special effect after a hit. Default: nothing. */
    protected void onHit(Battle battle, Combatant target) {
    }

    /** Called once when this unit is destroyed. Default: nothing. */
    public void onDeath(Battle battle) {
    }

    /** false = walks past enemy units and only attacks the tower (Drone). */
    public boolean targetsUnits() {
        return true;
    }

    /** Name of the special ability, e.g. "KITCHEN FIRE". */
    public abstract String getAbilityName();

    /** Short explanation of the ability, e.g. "HOT PANS BURN THE TARGET". */
    public abstract String getAbilityText();

    /** What ranged units throw. */
    protected Projectile.Kind projectileKind() {
        return Projectile.Kind.CHALK;
    }

    public void placeAt(double x, int lane) {
        this.x = x;
        this.lane = lane;
    }

    /** Applies stage modifiers when the unit is spawned. */
    public void applyModifiers(double speedMultiplier, double hpMultiplier) {
        speed *= speedMultiplier;
        scaleHp(hpMultiplier);
    }

    public boolean isRanged() {
        return range > 20;
    }

    /** 1.5 if this unit counters the target, 0.7 if the target counters it, otherwise 1. */
    public double counterMultiplier(Combatant target) {
        Role targetRole = target instanceof Unit u ? u.role : null;
        return role.multiplierAgainst(targetRole);
    }

    /** Damage against a target, including the role counter bonus and the siege bonus vs towers. */
    public double damageAgainst(Combatant target) {
        double siege = target instanceof Tower ? SIEGE_BONUS : 1.0;
        return damage * counterMultiplier(target) * siege;
    }

    /** Called every game step: attack if something is in range, otherwise walk forward. */
    public void update(Battle battle, double dt) {
        moving = false;
        if (!isAlive() || isFrozen()) {
            return;
        }
        aliveTime += dt;
        attackCooldown -= dt * attackSpeedFactor();
        attackAnimation -= dt;
        updateAbility(battle, dt);

        Combatant target = battle.findTargetFor(this);
        if (target != null) {
            if (attackCooldown <= 0) {
                attacksMade++;
                attack(battle, target);
                attackCooldown = attackInterval;
                attackAnimation = 0.15;
            }
        } else if (!battle.isBlockedByTeammate(this)) {
            double step = speed * speedFactor() * dt;
            x += side.direction() * step;
            walked += step;
            moving = true;
        }
    }

    protected void attack(Battle battle, Combatant target) {
        if (isRanged()) {
            battle.addProjectile(new Projectile(projectileKind(), side,
                    x + side.direction() * 8, Battle.GROUND_Y + laneY() - bodyHeight() + 8,
                    target, 110, damageAgainst(target), 6, hit -> onHit(battle, hit))
                    .withCounterMultiplier(counterMultiplier(target)));
        } else {
            battle.hit(target, damageAgainst(target), side, counterMultiplier(target));
            onHit(battle, target);
        }
    }

    /** Raises damage and max HP (Student graduation). */
    protected void powerUp(double damageMultiplier, double hpMultiplier) {
        damage *= damageMultiplier;
        maxHp *= hpMultiplier;
        hp = maxHp;
    }

    /** Seconds since the unit was spawned. */
    public double getAliveTime() {
        return aliveTime;
    }

    /** How many attacks it has started (used for "every 4th hit" abilities). */
    protected int getAttacksMade() {
        return attacksMade;
    }

    // ---- Drawing ----

    public void draw(Graphics2D g, int groundY, int tick) {
        int frame = (int) (walked / 4) % 2;
        int lunge = attackAnimation > 0 ? side.direction() * 2 : 0;
        int footX = (int) Math.round(x) + lunge;
        int gy = groundY + laneY();
        drawSprite(g, footX, gy, side.direction(), frame, tick, SPRITE_SCALE, isFlashing());

        int top = gy - hoverHeight(tick) * SPRITE_SCALE; // where the feet (or the bottom) are
        int h = bodyHeight();
        int half = getSprite(0, 0).width() * SPRITE_SCALE / 2 + 1; // half the width of the effect boxes
        if (isFrozen()) {
            g.setColor(new Color(180, 230, 255, 150));
            g.fillRect(footX - half, top - h - 1, half * 2, h + 1);
            g.setColor(new Color(235, 250, 255));
            g.fillRect(footX - half, top - h - 1, half * 2, 1);
        }
        if (isBurning()) {
            g.setColor(new Color(0xFF7A1A));
            g.fillRect(footX - 4 + (tick / 3) % 3 * 2, top - h - 4, 2, 3);
            g.setColor(new Color(0xFFD23F));
            g.fillRect(footX + 2 - (tick / 4) % 2 * 2, top - h - 3, 2, 3);
        }
        if (isHasted() && !isFrozen()) {
            // little speed lines behind the unit
            g.setColor(new Color(0xFFE066));
            int back = footX - side.direction() * (half + 1);
            g.fillRect(back - 2, top - h / 2 - 3, 5, 1);
            g.fillRect(back - side.direction() * 3 - 2, top - h / 2 + 2, 5, 1);
        }
        if (isSlowed() && !isFrozen()) {
            g.setColor(new Color(0x7DD3FC));
            g.fillRect(footX - 8, top, 2, 1);
            g.fillRect(footX + 7, top, 2, 1);
        }
        if (hp < maxHp) {
            int barY = top - h - 6;
            g.setColor(new Color(0x2A1A1A));
            g.fillRect(footX - 8, barY, 16, 4);
            g.setColor(side == friendlySide ? new Color(0x6BE36B) : new Color(0xFF5A4E));
            g.fillRect(footX - 7, barY + 1, (int) Math.ceil(14 * hpRatio()), 2);
        }
    }

    /** Draws only the body (also used for the icons on the spawn buttons and the Lineup cards). */
    public void drawSprite(Graphics2D g, int footX, int groundY, int facing, int frame, int tick) {
        drawSprite(g, footX, groundY, facing, frame, tick, 1, false);
    }

    /** Draws the body at any size (1 = normal, 2 = twice as big). */
    public void drawSprite(Graphics2D g, int footX, int groundY, int facing, int frame, int tick, int scale) {
        drawSprite(g, footX, groundY, facing, frame, tick, scale, false);
    }

    private void drawSprite(Graphics2D g, int footX, int groundY, int facing, int frame, int tick,
                            int scale, boolean flash) {
        int y = groundY - hoverHeight(tick) * scale;
        getSprite(frame, tick).draw(g, footX, y, facing, scale, flash);
        PEN.begin(g, footX, y, facing, scale);
        PEN.setTint(flash ? Color.WHITE : null);
        drawExtras(PEN, frame, tick);
        PEN.setTint(null);
    }

    /**
     * Draws the unit inside a small box, starting from the top of its head
     * (the icon on the spawn buttons). Anything outside the box is cut off.
     */
    public void drawPortrait(Graphics2D g, int boxX, int boxY, int boxW, int boxH, int tick) {
        PixelSprite sprite = getSprite(0, tick);
        Graphics2D clip = (Graphics2D) g.create();
        clip.clipRect(boxX, boxY, boxW, boxH);
        int footX = boxX + sprite.footCol() + 2;
        int groundY = boxY + sprite.height() + 2 + hoverHeight(tick);
        drawSprite(clip, footX, groundY, 1, 0, tick);
        clip.dispose();
    }

    /** Which side's units are "ours" on this screen (green HP bars). Changes only in LAN 1v1. */
    public static void setFriendlySide(Side side) {
        friendlySide = side;
    }

    /** Lanes further back are drawn higher up, so the three lanes look like depth. */
    public int laneY() {
        return lane * 3;
    }

    @Override
    public int getHitHeight() {
        return hoverHeight(0) * SPRITE_SCALE + bodyHeight() / 2 - laneY();
    }

    // ---- Getters ----

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Role getRole() {
        return role;
    }

    public int getLane() {
        return lane;
    }

    public boolean isMoving() {
        return moving;
    }

    public double getRange() {
        return range;
    }

    public double getDamage() {
        return damage;
    }

    public double getSpeed() {
        return speed;
    }

    public double getAttackInterval() {
        return attackInterval;
    }
}
