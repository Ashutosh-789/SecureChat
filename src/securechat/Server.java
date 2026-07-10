package securechat;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/**
 * SecureChat Server — Fixed
 *
 * Fixes applied:
 *  1. Queues each client's HANDSHAKE and only exchanges them once BOTH are connected.
 *  2. Sends a PEER_NAME system message so each client knows who to talk to.
 *  3. Falls back to "send to whoever is not the sender" when receiver field is blank.
 *  4. Keeps accepting connections after 2 join (handles reconnects cleanly).
 *
 * HOW TO RUN IN ECLIPSE:
 *   Right-click Server.java → Run As → Java Application
 *   Start this BEFORE the two Client windows.
 */
public class Server {

    private static final int PORT = 5000;
    private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss");

    // name → live handler
    private static final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    // Stored until the peer is online to receive it
    private static final Map<String, Message> pendingHandshakes = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        log("==============================================");
        log("   SecureChat Server  —  DES + RSA E2EE");
        log("==============================================");
        log("Listening on port " + PORT);
        log("Waiting for 2 clients...");
        log("Server sees ONLY ciphertext — true E2EE.");
        log("----------------------------------------------");

        try (ServerSocket ss = new ServerSocket(PORT)) {
            ss.setReuseAddress(true);
            while (true) {                          // keep accepting (supports reconnects)
                Socket cs = ss.accept();
                log("Connection from " + cs.getInetAddress().getHostAddress());
                Thread t = new Thread(new ClientHandler(cs));
                t.setDaemon(true);
                t.start();
            }
        } catch (Exception e) {
            log("Fatal: " + e.getMessage());
        }
    }

    // Called once a client's name is known
    public static synchronized void registerClient(String name, ClientHandler handler, Message handshake) {
        clients.put(name, handler);
        pendingHandshakes.put(name, handshake);
        log("[REG] " + name + "  (connected: " + clients.size() + ")");

        if (clients.size() >= 2) {
            // Exchange public keys between every pair of clients
            List<String> names = new ArrayList<>(clients.keySet());
            for (int i = 0; i < names.size(); i++) {
                for (int j = 0; j < names.size(); j++) {
                    if (i == j) continue;
                    String me    = names.get(i);
                    String peer  = names.get(j);
                    ClientHandler myHandler   = clients.get(me);
                    Message       peerHs      = pendingHandshakes.get(peer);
                    if (myHandler != null && peerHs != null) {
                        // 1. Tell this client their peer's name
                        Message peerInfo = new Message();
                        peerInfo.setType(Message.TYPE_SYSTEM);
                        peerInfo.setSystemText("PEER_NAME:" + peer);
                        myHandler.sendMessage(peerInfo);
                        // 2. Deliver peer's HANDSHAKE (RSA public key)
                        myHandler.sendMessage(peerHs);
                        log("[KEY-EXCHANGE] " + peer + "'s public key  →  " + me);
                    }
                }
            }
            broadcast("Both clients connected! E2EE chat is live.");
        } else {
            Message wait = new Message();
            wait.setType(Message.TYPE_SYSTEM);
            wait.setSystemText("Connected to server. Waiting for the other client...");
            handler.sendMessage(wait);
        }
    }

    // Routes to named receiver, or falls back to "the other client"
    public static void routeMessage(Message msg) {
        String receiver = msg.getReceiver();
        ClientHandler ch = (receiver != null && !receiver.isEmpty()) ? clients.get(receiver) : null;

        if (ch != null) {
            ch.sendMessage(msg);
            logRoute(msg, receiver);
        } else {
            // Send to whoever is NOT the sender
            for (Map.Entry<String, ClientHandler> e : clients.entrySet()) {
                if (!e.getKey().equals(msg.getSender())) {
                    e.getValue().sendMessage(msg);
                    logRoute(msg, e.getKey() + " (fallback)");
                    return;
                }
            }
            log("[WARN] No peer found for " + msg.getSender());
        }
    }

    private static void logRoute(Message msg, String dest) {
        String body = msg.getEncryptedBody();
        log("[ROUTE] " + msg.getSender() + " → " + dest
            + "  type=" + msg.getType()
            + (body != null ? "  cipher=" + body.substring(0, Math.min(20, body.length())) + "..." : ""));
    }

    public static void broadcast(String text) {
        Message m = new Message();
        m.setType(Message.TYPE_SYSTEM);
        m.setSystemText(text);
        clients.values().forEach(c -> c.sendMessage(m));
        log("[BROADCAST] " + text);
    }

    public static synchronized void removeClient(String name) {
        clients.remove(name);
        pendingHandshakes.remove(name);
        log("[LEFT] " + name);
        if (!clients.isEmpty()) broadcast(name + " disconnected.");
    }

    static void log(String s) {
        System.out.println("[" + SDF.format(new Date()) + "] " + s);
    }

    // ── One thread per client ──────────────────────────────────────
    static class ClientHandler implements Runnable {
        private final Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream  in;
        private String clientName;

        ClientHandler(Socket s) { socket = s; }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in  = new ObjectInputStream(socket.getInputStream());

                // First message must be HANDSHAKE
                Message hs = (Message) in.readObject();
                if (!Message.TYPE_HANDSHAKE.equals(hs.getType())) {
                    log("Bad first message: " + hs.getType());
                    socket.close();
                    return;
                }
                clientName = hs.getSender();
                registerClient(clientName, this, hs);

                // Read loop
                while (!socket.isClosed()) {
                    Message msg = (Message) in.readObject();
                    routeMessage(msg);
                }
            } catch (EOFException | SocketException ignored) {
                // clean disconnect
            } catch (Exception e) {
                log("Error [" + clientName + "]: " + e.getMessage());
            } finally {
                if (clientName != null) removeClient(clientName);
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        public synchronized void sendMessage(Message msg) {
            try {
                out.writeObject(msg);
                out.flush();
                out.reset();
            } catch (IOException e) {
                log("Send failed → " + clientName + ": " + e.getMessage());
            }
        }
    }
}