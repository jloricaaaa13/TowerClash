package net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A TCP connection to the other player's computer.
 * Messages are simple lines of text. A background thread reads every line that arrives
 * and puts it in a queue, so the game never freezes while waiting for the network.
 */
public class LanConnection {

    private final Socket socket;
    private final PrintWriter out;
    private final BlockingQueue<String> incoming = new LinkedBlockingQueue<>();
    private volatile boolean closed;

    public LanConnection(Socket socket) throws IOException {
        this.socket = socket;
        socket.setTcpNoDelay(true); // send small messages right away (less lag)
        this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        Thread reader = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    incoming.add(line);
                }
            } catch (IOException e) {
                // connection lost
            }
            closed = true;
        }, "lan-reader");
        reader.setDaemon(true);
        reader.start();
    }

    public synchronized void send(String line) {
        if (!closed) {
            out.println(line);
            if (out.checkError()) {
                closed = true;
            }
        }
    }

    /** The next message, or null if none has arrived yet. */
    public String poll() {
        return incoming.poll();
    }

    /** Waits up to 'millis' for the next message (null = nothing came). */
    public String waitFor(long millis) throws InterruptedException {
        return incoming.poll(millis, TimeUnit.MILLISECONDS);
    }

    public boolean isClosed() {
        return closed && incoming.isEmpty();
    }

    public String getRemoteAddress() {
        return socket.getInetAddress().getHostAddress();
    }

    public void close() {
        closed = true;
        try {
            socket.close();
        } catch (IOException e) {
            // already closed
        }
    }
}
