package net;

import game.Battle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.Side;
import model.Stage;
import model.Tower;
import model.Unit;

/**
 * A LAN 1v1 match between two computers, kept in sync with "LOCKSTEP":
 *
 *   - Both computers run the SAME Battle simulation (same towers, same random seed).
 *   - The game moves in fixed steps ("ticks") of 1/60 of a second.
 *   - A player's actions (spawn, upgrade, ultimate) are NOT done right away: they are sent to
 *     the other computer and BOTH computers do them at the same future tick (DELAY ticks later).
 *   - Every tick, each computer sends its actions for that future tick (often "none").
 *     A computer only moves to the next tick when it has the other player's actions for it,
 *     so both screens always show exactly the same battle.
 *
 * Messages:  F <tick> <actions>   actions = S0,S1,S2 (spawn slot), U (upgrade), R (ultimate) or -
 *            H <tick> <hash>      a fingerprint of the battle, to detect "out of sync"
 *            Q                    the other player surrendered
 *
 * The host plays the LEFT tower (Side.PLAYER), the player who joined plays the RIGHT tower (Side.ENEMY).
 */
public class LanMatch {

    public static final double TICK = 1 / 60.0;
    private static final int DELAY = 6;            // actions happen 6 ticks (0.1 s) after the click
    private static final int CHECK_EVERY = 60;     // compare fingerprints once a second
    private static final long TIMEOUT_MS = 10000;  // no message for 10 s = connection lost

    private final LanConnection connection;
    private final boolean host;
    private final LanPlayer me;
    private final LanPlayer opponent;
    private final long seed;

    private final Map<Integer, String> remoteFrames = new HashMap<>();
    private final Map<Integer, String> localFrames = new HashMap<>();
    private final Map<Integer, String> remoteHashes = new HashMap<>();
    private final Map<Integer, String> localHashes = new HashMap<>();
    private final List<String> pending = new ArrayList<>();
    private int tick;
    private long lastMessageTime = System.currentTimeMillis();
    private boolean opponentQuit;
    private boolean disconnected;
    private boolean outOfSync;

    LanMatch(LanConnection connection, boolean host, LanPlayer me, LanPlayer opponent, long seed) {
        this.connection = connection;
        this.host = host;
        this.me = me;
        this.opponent = opponent;
        this.seed = seed;
        for (int t = 0; t < DELAY; t++) { // the first ticks have no actions on both sides
            remoteFrames.put(t, "-");
            localFrames.put(t, "-");
        }
    }

    /** Builds the battle. Both computers call this and get exactly the same starting battle. */
    public Battle createBattle() {
        LanPlayer left = host ? me : opponent;
        LanPlayer right = host ? opponent : me;
        Tower leftTower = left.department().createTower(Side.PLAYER, left.towerLevel(), 1.0, left.lineup());
        Tower rightTower = right.department().createTower(Side.ENEMY, right.towerLevel(), 1.0, right.lineup());
        Battle battle = new Battle(leftTower, rightTower, 1.0, Stage.LAN, seed);
        battle.setLocalSide(getMySide());
        return battle;
    }

    /** Which tower this computer controls. */
    public Side getMySide() {
        return host ? Side.PLAYER : Side.ENEMY;
    }

    public String nameOf(Side side) {
        return side == getMySide() ? me.name() : opponent.name();
    }

    /** Remembers an action (it is sent with the next tick). Example: "S0" = spawn slot 1. */
    public void queue(String action) {
        pending.add(action);
    }

    /**
     * Moves the battle forward by ONE tick if the other player's actions for it have arrived.
     * @return false = still waiting for the other computer (the battle does not move)
     */
    public boolean step(Battle battle) {
        readMessages();
        if (isFinished()) {
            return false;
        }
        String theirs = remoteFrames.get(tick);
        if (theirs == null) {
            if (System.currentTimeMillis() - lastMessageTime > TIMEOUT_MS) {
                disconnected = true;
            }
            return false;
        }
        // Send my actions for a future tick (DELAY ticks from now)
        int future = tick + DELAY;
        if (!localFrames.containsKey(future)) {
            String mine = pending.isEmpty() ? "-" : String.join(",", pending);
            pending.clear();
            localFrames.put(future, mine);
            connection.send("F " + future + " " + mine);
        }
        // Do both players' actions for THIS tick - always the left player first, so both computers agree
        String leftActions = host ? localFrames.get(tick) : theirs;
        String rightActions = host ? theirs : localFrames.get(tick);
        apply(battle, Side.PLAYER, leftActions);
        apply(battle, Side.ENEMY, rightActions);
        battle.update(TICK);

        if (tick % CHECK_EVERY == 0) {
            String hash = fingerprint(battle);
            localHashes.put(tick, hash);
            connection.send("H " + tick + " " + hash);
            compareHashes(tick);
        }
        remoteFrames.remove(tick);
        localFrames.remove(tick);
        tick++;
        return true;
    }

    private static void apply(Battle battle, Side side, String actions) {
        if (actions == null || actions.equals("-")) {
            return;
        }
        for (String a : actions.split(",")) {
            if (a.startsWith("S") && a.length() == 2) {
                battle.spawn(side, a.charAt(1) - '0');
            } else if (a.equals("U")) {
                battle.upgradeTower(side);
            } else if (a.equals("R")) {
                battle.useUltimate(side);
            }
        }
    }

    private void readMessages() {
        String line;
        while ((line = connection.poll()) != null) {
            lastMessageTime = System.currentTimeMillis();
            String[] p = line.split(" ", 3);
            try {
                switch (p[0]) {
                    case "F" -> remoteFrames.put(Integer.parseInt(p[1]), p.length > 2 ? p[2] : "-");
                    case "H" -> {
                        int t = Integer.parseInt(p[1]);
                        remoteHashes.put(t, p[2]);
                        compareHashes(t);
                    }
                    case "Q" -> opponentQuit = true;
                    default -> {
                        // unknown message: ignore
                    }
                }
            } catch (RuntimeException e) {
                // broken message: ignore
            }
        }
        if (connection.isClosed() && !opponentQuit) {
            disconnected = true;
        }
    }

    private void compareHashes(int t) {
        String a = localHashes.get(t);
        String b = remoteHashes.get(t);
        if (a != null && b != null) {
            if (!a.equals(b)) {
                outOfSync = true;
            }
            localHashes.remove(t);
            remoteHashes.remove(t);
        }
    }

    /** A short "fingerprint" of the battle. If the two computers ever disagree, they are out of sync. */
    private static String fingerprint(Battle battle) {
        long h = 17;
        for (Side side : Side.values()) {
            h = h * 31 + Double.doubleToLongBits(battle.getTower(side).getHp());
            h = h * 31 + Double.doubleToLongBits(battle.getEnergy(side));
        }
        for (Unit u : battle.getUnits()) {
            h = h * 31 + Double.doubleToLongBits(u.getX());
            h = h * 31 + Double.doubleToLongBits(u.getHp());
        }
        return Long.toHexString(h);
    }

    /** I give up: the other player wins. */
    public void surrender() {
        connection.send("Q");
    }

    public void close() {
        connection.close();
    }

    public boolean isFinished() {
        return opponentQuit || disconnected || outOfSync;
    }

    public boolean isOpponentQuit() {
        return opponentQuit;
    }

    public boolean isDisconnected() {
        return disconnected;
    }

    public boolean isOutOfSync() {
        return outOfSync;
    }

    /** Seconds since we last heard from the other computer (to show "WAITING..."). */
    public double secondsSinceLastMessage() {
        return (System.currentTimeMillis() - lastMessageTime) / 1000.0;
    }

    public LanPlayer getMe() {
        return me;
    }

    public LanPlayer getOpponent() {
        return opponent;
    }

    public boolean isHost() {
        return host;
    }
}
