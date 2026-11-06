package model;

import java.util.function.Function;

/**
 * The "shop entry" for a unit: its name, energy cost, and how long before
 * it can be spawned again. The factory creates the real Unit object.
 */
public record UnitType(String name, int cost, double spawnCooldown, Function<Side, Unit> factory) {

    public Unit create(Side side) {
        return factory.apply(side);
    }
}
