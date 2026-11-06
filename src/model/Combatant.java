package model;

/**
 * Anything that has HP and can be hit: towers and units.
 * It also keeps track of status effects:
 *   BURN   - loses HP every second
 *   SLOW   - moves slower
 *   FROZEN - cannot move or attack, and takes extra damage
 *   HASTE  - moves and attacks faster (a buff from teammates)
 *
 * Encapsulation: HP can only change through receiveDamage() and heal(),
 * so it can never go below 0 or above max HP.
 */
public abstract class Combatant {

    protected final Side side;
    protected double x;
    protected double hp;
    protected double maxHp;

    private double burnDps;
    private double burnTime;
    private Side burnSource;
    private double slowAmount;
    private double slowTime;
    private double freezeTime;
    private double hasteAmount;
    private double hasteTime;
    private double flashTime; // white flash right after being hit

    protected Combatant(Side side, double x, double maxHp) {
        this.side = side;
        this.x = x;
        this.maxHp = maxHp;
        this.hp = maxHp;
    }

    /** Height (above the ground) where projectiles should hit this target. */
    public abstract int getHitHeight();

    // ---- HP ----

    public boolean isAlive() {
        return hp > 0;
    }

    /** Removes HP and returns how much was really lost (never more than the HP left). */
    public double receiveDamage(double amount) {
        double lost = Math.min(hp, amount);
        hp = Math.max(0, hp - amount);
        return lost;
    }

    /** Makes the hit flash visible for a moment (burn ticks do not flash). */
    public void flash() {
        flashTime = 0.1;
    }

    public boolean isFlashing() {
        return flashTime > 0;
    }

    /** Multiplies max HP (and current HP), e.g. for the Exam Week modifier. */
    public void scaleHp(double multiplier) {
        maxHp *= multiplier;
        hp *= multiplier;
    }

    /** Sets HP to a share of max HP, e.g. 0.7 for the Finals modifier. */
    public void setHpRatio(double ratio) {
        hp = maxHp * Math.max(0, Math.min(1, ratio));
    }

    public void heal(double amount) {
        if (isAlive()) {
            hp = Math.min(maxHp, hp + amount);
        }
    }

    public double getHp() {
        return hp;
    }

    public double getMaxHp() {
        return maxHp;
    }

    public double hpRatio() {
        return maxHp <= 0 ? 0 : hp / maxHp;
    }

    // ---- Status effects ----

    public void applyBurn(double damagePerSecond, double seconds, Side source) {
        burnDps = Math.max(burnDps, damagePerSecond);
        burnTime = Math.max(burnTime, seconds);
        burnSource = source;
    }

    /** @param amount 0.4 = 40% slower */
    public void applySlow(double amount, double seconds) {
        slowAmount = Math.max(slowAmount, amount);
        slowTime = Math.max(slowTime, seconds);
    }

    /** @param amount 0.3 = moves and attacks 30% faster (Coach, Hotel Manager) */
    public void applyHaste(double amount, double seconds) {
        hasteAmount = Math.max(hasteAmount, amount);
        hasteTime = Math.max(hasteTime, seconds);
    }

    public boolean isHasted() {
        return hasteTime > 0;
    }

    /** 1 = normal attack speed, 1.3 = 30% faster. */
    public double attackSpeedFactor() {
        return isHasted() ? 1 + hasteAmount : 1;
    }

    public void freeze(double seconds) {
        freezeTime = Math.max(freezeTime, seconds);
    }

    public boolean isBurning() {
        return burnTime > 0;
    }

    public boolean isSlowed() {
        return slowTime > 0;
    }

    public boolean isFrozen() {
        return freezeTime > 0;
    }

    public Side getBurnSource() {
        return burnSource;
    }

    /** 1 = normal speed, 0.6 = slowed by 40%, 0 = frozen. */
    public double speedFactor() {
        if (isFrozen()) {
            return 0;
        }
        double factor = isSlowed() ? 1 - slowAmount : 1;
        return isHasted() ? factor * (1 + hasteAmount) : factor;
    }

    /**
     * Counts down all effect timers. Returns the burn damage for this step
     * (the Battle applies it, so it is counted in the score).
     */
    public double tickEffects(double dt) {
        double burnDamage = 0;
        if (burnTime > 0) {
            burnDamage = burnDps * Math.min(dt, burnTime);
            burnTime -= dt;
            if (burnTime <= 0) {
                burnDps = 0;
            }
        }
        if (slowTime > 0) {
            slowTime -= dt;
            if (slowTime <= 0) {
                slowAmount = 0;
            }
        }
        if (freezeTime > 0) {
            freezeTime -= dt;
        }
        if (hasteTime > 0) {
            hasteTime -= dt;
            if (hasteTime <= 0) {
                hasteAmount = 0;
            }
        }
        if (flashTime > 0) {
            flashTime -= dt;
        }
        return burnDamage;
    }

    // ---- Position ----

    public Side getSide() {
        return side;
    }

    public double getX() {
        return x;
    }
}
