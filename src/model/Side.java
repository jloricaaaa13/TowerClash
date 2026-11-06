package model;

/** Which team something belongs to. The player is on the left and walks right. */
public enum Side {

    PLAYER(1),
    ENEMY(-1);

    private final int direction;

    Side(int direction) {
        this.direction = direction;
    }

    /** +1 = moves to the right, -1 = moves to the left. */
    public int direction() {
        return direction;
    }

    public Side opponent() {
        return this == PLAYER ? ENEMY : PLAYER;
    }
}
