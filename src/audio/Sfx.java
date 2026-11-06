package audio;

/**
 * Every sound effect in the game.
 * poolSize = how many copies can play at the same time.
 * minGapMillis = the same sound is skipped if it played less than this long ago
 * (so 20 hits in one moment do not become noise).
 */
public enum Sfx {

    CLICK(1, 30),
    SPAWN(2, 40),
    HIT(3, 60),
    TOWER_HIT(2, 90),
    DEATH(3, 50),
    UPGRADE(1, 0),
    FIRE(1, 0),
    FREEZE(1, 0),
    LASER(1, 0),
    VICTORY(1, 0),
    DEFEAT(1, 0),
    ERROR(1, 120),
    STAR(2, 0);

    private final int poolSize;
    private final int minGapMillis;

    Sfx(int poolSize, int minGapMillis) {
        this.poolSize = poolSize;
        this.minGapMillis = minGapMillis;
    }

    public int poolSize() {
        return poolSize;
    }

    public int minGapMillis() {
        return minGapMillis;
    }
}
