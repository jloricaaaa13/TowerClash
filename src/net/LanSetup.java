package net;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Finds an opponent on the LAN: one player HOSTS (waits for a connection) and the other JOINS
 * (types the host's IP address). Everything runs in a background thread; the Lobby screen
 * checks getState() every frame.
 *
 * The "handshake" (first messages):
 *   both:  HELLO <version> <name> <department> <towerLevel> <lineup>
 *   host:  START <seed>        (the same random seed makes both computers simulate the same battle)
 */
public class LanSetup {

    public static final int PORT = 5555;
    public static final String VERSION = "TOWERCLASH-LAN-1";
    private static final int CONNECT_TIMEOUT = 4000;
    private static final int HANDSHAKE_TIMEOUT = 10000;

    public enum State { IDLE, HOSTING, CONNECTING, READY, FAILED }

    private volatile State state = State.IDLE;
    private volatile String problem = "";
    private volatile LanMatch match;
    private ServerSocket server;
    private Thread worker;

    /** Wait for another player to join this computer. */
    public void host(LanPlayer me) {
        cancel();
        state = State.HOSTING;
        worker = new Thread(() -> {
            try (ServerSocket ss = new ServerSocket(PORT)) {
                server = ss;
                Socket socket = ss.accept();
                LanConnection conn = new LanConnection(socket);
                conn.send("HELLO " + VERSION + " " + me.encode());
                LanPlayer other = readHello(conn);
                long seed = new Random().nextLong();
                conn.send("START " + seed);
                match = new LanMatch(conn, true, me, other, seed);
                if (state == State.HOSTING) {
                    state = State.READY;
                } else {
                    conn.close(); // cancelled meanwhile
                }
            } catch (IOException | InterruptedException e) {
                fail(state == State.IDLE ? "" : "COULD NOT HOST: " + shortReason(e));
            } catch (LanException e) {
                fail(e.getMessage());
            }
        }, "lan-host");
        worker.setDaemon(true);
        worker.start();
    }

    /** Connect to a player who is hosting. */
    public void join(String hostIp, LanPlayer me) {
        cancel();
        state = State.CONNECTING;
        worker = new Thread(() -> {
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(hostIp.trim(), PORT), CONNECT_TIMEOUT);
                LanConnection conn = new LanConnection(socket);
                conn.send("HELLO " + VERSION + " " + me.encode());
                LanPlayer other = readHello(conn);
                String start = conn.waitFor(HANDSHAKE_TIMEOUT);
                if (start == null || !start.startsWith("START ")) {
                    throw new LanException("THE HOST DID NOT START THE MATCH");
                }
                long seed = Long.parseLong(start.substring(6).trim());
                match = new LanMatch(conn, false, me, other, seed);
                if (state == State.CONNECTING) {
                    state = State.READY;
                } else {
                    conn.close(); // cancelled meanwhile
                }
            } catch (IOException | InterruptedException e) {
                if (state == State.CONNECTING) {
                    fail("NO GAME FOUND AT " + hostIp.trim() + " (IS THE OTHER PLAYER HOSTING?)");
                }
            } catch (LanException | NumberFormatException e) {
                fail(e.getMessage());
            }
        }, "lan-join");
        worker.setDaemon(true);
        worker.start();
    }

    private static LanPlayer readHello(LanConnection conn) throws InterruptedException, LanException {
        String hello = conn.waitFor(HANDSHAKE_TIMEOUT);
        if (hello == null || !hello.startsWith("HELLO ")) {
            throw new LanException("THE OTHER PLAYER DID NOT ANSWER");
        }
        String[] parts = hello.split(" ", 3);
        if (parts.length < 3 || !VERSION.equals(parts[1])) {
            conn.close();
            throw new LanException("THE OTHER PLAYER HAS A DIFFERENT GAME VERSION");
        }
        LanPlayer other = LanPlayer.decode(parts[2]);
        if (other == null) {
            conn.close();
            throw new LanException("COULD NOT READ THE OTHER PLAYER'S TEAM");
        }
        return other;
    }

    private void fail(String message) {
        problem = message;
        state = message.isEmpty() ? State.IDLE : State.FAILED;
    }

    /** Stops hosting / connecting. */
    public void cancel() {
        State old = state;
        state = State.IDLE;
        if (old == State.HOSTING && server != null) {
            try {
                server.close(); // makes accept() stop
            } catch (IOException e) {
                // ignore
            }
        }
        if (worker != null) {
            worker.interrupt();
        }
        match = null;
    }

    public State getState() {
        return state;
    }

    public String getProblem() {
        return problem;
    }

    /** The ready match (only when getState() == READY). Taking it resets the setup. */
    public LanMatch takeMatch() {
        LanMatch m = match;
        match = null;
        state = State.IDLE;
        return m;
    }

    /** This computer's LAN addresses (what the other player types to join). */
    public static List<String> localAddresses() {
        List<String> list = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address && !a.isLoopbackAddress() && !a.isLinkLocalAddress()) {
                        list.add(a.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            // no network
        }
        return list;
    }

    private static String shortReason(Exception e) {
        String m = String.valueOf(e.getMessage()).toUpperCase();
        return m.contains("ADDRESS ALREADY IN USE") || m.contains("BIND") ? "PORT " + PORT + " IS BUSY" : "NETWORK ERROR";
    }

    /** Handshake problem with a message for the player. */
    static class LanException extends Exception {
        LanException(String message) {
            super(message);
        }
    }
}
