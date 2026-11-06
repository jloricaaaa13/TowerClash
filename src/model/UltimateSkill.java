package model;

import game.Battle;

/**
 * A tower's special move (abstract). The meter goes from 0 to 100:
 *   - fills a little every second
 *   - fills more when this side deals damage
 * When it is full the skill can be used. Then the meter resets to 0 and
 * does not fill again until the cooldown is over.
 *
 * Each subclass (KitchenInferno, FinalExam, SystemOverload) only writes perform().
 */
public abstract class UltimateSkill {

    public static final double METER_MAX = 100;
    private static final double FILL_PER_SECOND = 1.5;
    private static final double FILL_PER_DAMAGE = 0.08;

    private final String name;
    private final String description;
    private final double cooldown;
    private double meter;
    private double cooldownLeft;

    protected UltimateSkill(String name, String description, double cooldown) {
        this.name = name;
        this.description = description;
        this.cooldown = cooldown;
    }

    /** What the skill actually does. */
    protected abstract void perform(Battle battle, Tower owner);

    public void update(double dt) {
        if (cooldownLeft > 0) {
            cooldownLeft = Math.max(0, cooldownLeft - dt);
        } else {
            meter = Math.min(METER_MAX, meter + FILL_PER_SECOND * dt);
        }
    }

    public void onDamageDealt(double damage) {
        if (cooldownLeft <= 0) {
            meter = Math.min(METER_MAX, meter + damage * FILL_PER_DAMAGE);
        }
    }

    public boolean isReady() {
        return cooldownLeft <= 0 && meter >= METER_MAX;
    }

    /** Uses the skill if it is ready. */
    public boolean activate(Battle battle, Tower owner) {
        if (!isReady()) {
            return false;
        }
        meter = 0;
        cooldownLeft = cooldown;
        perform(battle, owner);
        battle.shake(4);
        return true;
    }

    public String getName() {
        return name;
    }

    /** One line for the tooltip. */
    public String getDescription() {
        return description;
    }

    public double getMeter() {
        return meter;
    }

    public double getCooldown() {
        return cooldown;
    }

    public double getCooldownLeft() {
        return cooldownLeft;
    }
}
