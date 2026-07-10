package securechat;

import javax.swing.*;
import javax.swing.border.*;
import javax.crypto.SecretKey;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * SecureChat Client — WhatsApp-style GUI
 * 
 * HOW TO RUN IN ECLIPSE:
 *   1. Start Server.java first
 *   2. Run Client.java → a "Login" dialog appears → enter your name (e.g. Alice)
 *   3. Run Client.java again in a second window → enter the other name (e.g. Bob)
 *   4. Click any message bubble to see encryption/decryption details
 *
 * To run a second instance in Eclipse:
 *   Run → Run Configurations → Client → "Allow multiple instances" checkbox
 *   (or just run it twice — Eclipse allows this by default for Java apps)
 */
public class Client extends JFrame {

    // ── Network ──
    private static final String HOST = "localhost";
    private static final int PORT = 5000;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    // ── Crypto ──
    private KeyPair myRSAKeyPair;
    private PublicKey peerPublicKey;
    private SecretKey desSessionKey;
    private String myDesKeyHex;
    private String myRsaPubHex;
    private String myRsaPrivHex;
    private String encryptedDesKeyForPeer; // RSA-wrapped DES key sent to peer

    // ── Identity ──
    private String myName;
    private String peerName;

    // ── UI Colors (WhatsApp dark theme) ──
    private static final Color BG_CHAT    = new Color(10, 15, 25);
    private static final Color BG_HEADER  = new Color(18, 27, 38);
    private static final Color BG_INPUT   = new Color(18, 27, 38);
    private static final Color BG_SENT    = new Color(0, 92, 75);
    private static final Color BG_RECV    = new Color(30, 41, 59);
    private static final Color BG_SYSTEM  = new Color(20, 30, 45);
    private static final Color COLOR_GREEN = new Color(37, 211, 102);
    private static final Color COLOR_BLUE  = new Color(59, 130, 246);
    private static final Color TEXT_WHITE  = new Color(226, 232, 240);
    private static final Color TEXT_MUTED  = new Color(148, 163, 184);
    private static final Color TEXT_SENT   = new Color(233, 251, 229);
    private static final Color BORDER_CLR  = new Color(42, 53, 72);

    // ── UI Components ──
    private JPanel  messagesPanel;
    private JScrollPane scrollPane;
    private JTextField  inputField;
    private JButton     sendButton;
    private JLabel      statusLabel;
    private JLabel      peerNameLabel;
    private JButton     viewKeysBtn;

    // Message records for dialog lookup
    private final java.util.List<MessageRecord> messageHistory = new ArrayList<>();

    private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Client client = new Client();
            client.showLoginDialog();
        });
    }

    // ─────────────────────────────────────────────
    // Login dialog — user enters their name
    // ─────────────────────────────────────────────
    private void showLoginDialog() {
        JDialog login = new JDialog((Frame) null, "SecureChat — Login", true);
        login.setSize(380, 280);
        login.setLocationRelativeTo(null);
        login.setResizable(false);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(13, 17, 23));
        panel.setBorder(new EmptyBorder(30, 40, 30, 40));

        JLabel iconLbl = new JLabel("🔐 SecureChat");
        iconLbl.setFont(new Font("SansSerif", Font.BOLD, 22));
        iconLbl.setForeground(COLOR_GREEN);
        iconLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLbl = new JLabel("DES + RSA End-to-End Encrypted");
        subLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subLbl.setForeground(TEXT_MUTED);
        subLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel nameLbl = new JLabel("Your name:");
        nameLbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        nameLbl.setForeground(TEXT_WHITE);
        nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        nameLbl.setBorder(new EmptyBorder(20, 0, 6, 0));

        JTextField nameField = new JTextField();
        nameField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        nameField.setBackground(new Color(22, 33, 46));
        nameField.setForeground(TEXT_WHITE);
        nameField.setCaretColor(TEXT_WHITE);
        nameField.setBorder(new CompoundBorder(
            new LineBorder(BORDER_CLR, 1, true),
            new EmptyBorder(8, 12, 8, 12)
        ));
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JButton startBtn = new JButton("Connect & Generate Keys");
        startBtn.setBackground(COLOR_GREEN);
        startBtn.setForeground(Color.BLACK);
        startBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        startBtn.setBorderPainted(false);
        startBtn.setFocusPainted(false);
        startBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        startBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        startBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        startBtn.setBorder(new EmptyBorder(10, 0, 10, 0));

        ActionListener connectAction = e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(login, "Please enter your name.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            myName = name;
            login.dispose();
            initUI();
            connectToServer();
        };

        startBtn.addActionListener(connectAction);
        nameField.addActionListener(connectAction);

        panel.add(iconLbl);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        panel.add(subLbl);
        panel.add(nameLbl);
        panel.add(nameField);
        panel.add(Box.createRigidArea(new Dimension(0, 14)));
        panel.add(startBtn);

        login.setContentPane(panel);
        login.setVisible(true);
    }

    // ─────────────────────────────────────────────
    // Build the main chat UI
    // ─────────────────────────────────────────────
    private void initUI() {
        setTitle("SecureChat — " + myName);
        setSize(480, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setBackground(BG_CHAT);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_CHAT);

        // ── Header ──
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(BG_HEADER);
        header.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER_CLR),
            new EmptyBorder(10, 16, 10, 16)
        ));

        // Avatar circle
        JLabel avatar = new JLabel(myName.substring(0, 1).toUpperCase());
        avatar.setFont(new Font("SansSerif", Font.BOLD, 16));
        avatar.setForeground(COLOR_GREEN);
        avatar.setBackground(new Color(37, 211, 102, 30));
        avatar.setOpaque(true);
        avatar.setPreferredSize(new Dimension(42, 42));
        avatar.setHorizontalAlignment(SwingConstants.CENTER);
        avatar.setBorder(BorderFactory.createLineBorder(COLOR_GREEN, 1, true));

        JPanel headerInfo = new JPanel(new GridLayout(2, 1, 0, 2));
        headerInfo.setOpaque(false);

        peerNameLabel = new JLabel("Waiting for peer...");
        peerNameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        peerNameLabel.setForeground(TEXT_WHITE);

        statusLabel = new JLabel("Connecting...");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        statusLabel.setForeground(TEXT_MUTED);

        headerInfo.add(peerNameLabel);
        headerInfo.add(statusLabel);

        viewKeysBtn = new JButton("🔑 Keys");
        viewKeysBtn.setBackground(new Color(42, 53, 72));
        viewKeysBtn.setForeground(new Color(167, 243, 208));
        viewKeysBtn.setBorderPainted(false);
        viewKeysBtn.setFocusPainted(false);
        viewKeysBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        viewKeysBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        viewKeysBtn.setEnabled(false);
        viewKeysBtn.addActionListener(e -> showKeyViewer());

        header.add(avatar, BorderLayout.WEST);
        header.add(headerInfo, BorderLayout.CENTER);
        header.add(viewKeysBtn, BorderLayout.EAST);

        // ── Messages Area ──
        messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(BG_CHAT);
        messagesPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        scrollPane = new JScrollPane(messagesPanel);
        scrollPane.setBackground(BG_CHAT);
        scrollPane.getViewport().setBackground(BG_CHAT);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // ── Input Area ──
        JPanel inputArea = new JPanel(new BorderLayout(10, 0));
        inputArea.setBackground(BG_INPUT);
        inputArea.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, BORDER_CLR),
            new EmptyBorder(10, 14, 10, 14)
        ));

        inputField = new JTextField();
        inputField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        inputField.setBackground(new Color(15, 23, 36));
        inputField.setForeground(TEXT_WHITE);
        inputField.setCaretColor(TEXT_WHITE);
        inputField.setBorder(new CompoundBorder(
            new LineBorder(BORDER_CLR, 1, true),
            new EmptyBorder(10, 14, 10, 14)
        ));
        inputField.setEnabled(false);
        inputField.addActionListener(e -> sendMessage());

        sendButton = new JButton("Send");
        sendButton.setBackground(COLOR_GREEN);
        sendButton.setForeground(Color.BLACK);
        sendButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        sendButton.setBorderPainted(false);
        sendButton.setFocusPainted(false);
        sendButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendButton.setPreferredSize(new Dimension(80, 44));
        sendButton.setEnabled(false);
        sendButton.addActionListener(e -> sendMessage());

        inputArea.add(inputField, BorderLayout.CENTER);
        inputArea.add(sendButton, BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);
        root.add(scrollPane, BorderLayout.CENTER);
        root.add(inputArea, BorderLayout.SOUTH);

        setContentPane(root);
        setVisible(true);
    }

    // ─────────────────────────────────────────────
    // Connect to server and do RSA handshake
    // ─────────────────────────────────────────────
    private void connectToServer() {
        new Thread(() -> {
            try {
                updateStatus("Generating RSA key pair...");
                myRSAKeyPair = RSAUtil.generateKeyPair();
                myRsaPubHex  = RSAUtil.publicKeyToHex(myRSAKeyPair.getPublic());
                myRsaPrivHex = RSAUtil.privateKeyToHex(myRSAKeyPair.getPrivate());

                updateStatus("Connecting to server...");
                socket = new Socket(HOST, PORT);
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in  = new ObjectInputStream(socket.getInputStream());

                // Send HANDSHAKE with our name + RSA public key
                Message handshake = new Message();
                handshake.setType(Message.TYPE_HANDSHAKE);
                handshake.setSender(myName);
                handshake.setRsaPublicKey(RSAUtil.publicKeyToBase64(myRSAKeyPair.getPublic()));
                out.writeObject(handshake);
                out.flush();

                updateStatus("Waiting for peer to connect...");

                // Start listening
                startReceiving();

            } catch (ConnectException ce) {
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this,
                        "Cannot connect to server.\nMake sure Server.java is running first!",
                        "Connection Error", JOptionPane.ERROR_MESSAGE));
                updateStatus("Connection failed.");
            } catch (Exception e) {
                updateStatus("Error: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    // ─────────────────────────────────────────────
    // Background thread: listen for incoming messages
    // ─────────────────────────────────────────────
    private void startReceiving() {
        new Thread(() -> {
            try {
                while (true) {
                    Message msg = (Message) in.readObject();
                    handleIncoming(msg);
                }
            } catch (EOFException | SocketException e) {
                updateStatus("Disconnected from server.");
            } catch (Exception e) {
                updateStatus("Receive error: " + e.getMessage());
            }
        }).start();
    }

    private void handleIncoming(Message msg) throws Exception {
        switch (msg.getType()) {

            case Message.TYPE_HANDSHAKE:
                // Peer sent us their RSA public key
                peerName = msg.getSender();
                // Reconstruct peer's public key from Base64
                byte[] pubBytes = java.util.Base64.getDecoder().decode(msg.getRsaPublicKey());
                peerPublicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new java.security.spec.X509EncodedKeySpec(pubBytes));

                // Generate DES session key and send it to peer (RSA-encrypted)
                if (desSessionKey == null) {
                    desSessionKey = DESUtil.generateDESKey();
                    myDesKeyHex   = DESUtil.keyToHex(desSessionKey);
                    encryptedDesKeyForPeer = RSAUtil.encryptDESKey(desSessionKey, peerPublicKey);

                    Message keyMsg = new Message();
                    keyMsg.setType(Message.TYPE_KEY);
                    keyMsg.setSender(myName);
                    keyMsg.setReceiver(peerName);
                    keyMsg.setEncryptedDESKey(encryptedDesKeyForPeer);
                    out.writeObject(keyMsg);
                    out.flush();
                }

                SwingUtilities.invokeLater(() -> {
                    peerNameLabel.setText(peerName);
                    statusLabel.setText("end-to-end encrypted");
                    statusLabel.setForeground(COLOR_GREEN);
                    viewKeysBtn.setEnabled(true);
                    sendButton.setEnabled(true);
                    inputField.setEnabled(true);
                    inputField.requestFocus();
                    addSystemBubble("🔐 E2EE established with " + peerName + ". RSA handshake complete.");
                });
                break;

            case Message.TYPE_KEY:
                // Peer sent us their RSA-encrypted DES key
                if (desSessionKey == null) {
                    desSessionKey = RSAUtil.decryptDESKey(
                        msg.getEncryptedDESKey(), myRSAKeyPair.getPrivate());
                    myDesKeyHex   = DESUtil.keyToHex(desSessionKey);
                }
                break;

            case Message.TYPE_MESSAGE:
                // Decrypt and display
                String plaintext = DESUtil.decrypt(msg.getEncryptedBody(), desSessionKey);
                String cipher    = msg.getEncryptedBody();
                String encDES    = msg.getEncryptedDESKey() != null ? msg.getEncryptedDESKey() : "";

                SwingUtilities.invokeLater(() ->
                    addMessageBubble(msg.getSender(), plaintext, cipher, encDES, false));
                break;

            case Message.TYPE_SYSTEM:
                SwingUtilities.invokeLater(() -> addSystemBubble(msg.getSystemText()));
                break;
        }
    }

    // ─────────────────────────────────────────────
    // Send a message
    // ─────────────────────────────────────────────
    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty() || desSessionKey == null || peerPublicKey == null) return;
        inputField.setText("");

        try {
            String ciphertext = DESUtil.encrypt(text, desSessionKey);
            String encDESKey  = RSAUtil.encryptDESKey(desSessionKey, peerPublicKey);

            Message msg = new Message();
            msg.setType(Message.TYPE_MESSAGE);
            msg.setSender(myName);
            msg.setReceiver(peerName);
            msg.setEncryptedBody(ciphertext);
            msg.setEncryptedDESKey(encDESKey);

            out.writeObject(msg);
            out.flush();

            addMessageBubble(myName, text, ciphertext, encDESKey, true);

        } catch (Exception e) {
            addSystemBubble("⚠ Encryption error: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // UI: Add a chat bubble
    // ─────────────────────────────────────────────
    private void addMessageBubble(String sender, String text, String cipher,
                                  String encDESKey, boolean isSent) {
        String time = SDF.format(new Date());
        int idx = messageHistory.size();
        messageHistory.add(new MessageRecord(sender, peerName != null ? peerName : "?",
            text, cipher, encDESKey, isSent, time));

        JPanel row = new JPanel(new FlowLayout(isSent ? FlowLayout.RIGHT : FlowLayout.LEFT, 4, 2));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel bubble = new JPanel();
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBackground(isSent ? BG_SENT : BG_RECV);
        bubble.setBorder(new EmptyBorder(8, 12, 6, 12));

        // Message text
        JLabel textLbl = new JLabel("<html><body style='width:220px'>" + escapeHtml(text) + "</body></html>");
        textLbl.setFont(new Font("SansSerif", Font.PLAIN, 14));
        textLbl.setForeground(isSent ? TEXT_SENT : TEXT_WHITE);

        // Cipher preview + time row
        String preview = cipher.substring(0, Math.min(18, cipher.length())) + "...";
        JPanel metaRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        metaRow.setOpaque(false);

        JButton cipherBtn = new JButton("🔒 " + preview);
        cipherBtn.setFont(new Font("Monospaced", Font.PLAIN, 9));
        cipherBtn.setForeground(isSent ? new Color(74, 222, 128) : new Color(129, 140, 248));
        cipherBtn.setBackground(isSent ? new Color(0, 26, 10) : new Color(26, 26, 42));
        cipherBtn.setBorderPainted(false);
        cipherBtn.setFocusPainted(false);
        cipherBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cipherBtn.setMargin(new Insets(2, 5, 2, 5));
        cipherBtn.setToolTipText("Click to see encryption/decryption details");

        final int capturedIdx = idx;
        cipherBtn.addActionListener(e -> showEncryptionDialog(capturedIdx));

        JLabel timeLbl = new JLabel(time);
        timeLbl.setFont(new Font("SansSerif", Font.PLAIN, 10));
        timeLbl.setForeground(TEXT_MUTED);

        metaRow.add(cipherBtn);
        metaRow.add(timeLbl);

        bubble.add(textLbl);
        bubble.add(Box.createRigidArea(new Dimension(0, 4)));
        bubble.add(metaRow);

        // Round corners via border
        bubble.setBorder(new CompoundBorder(
            new RoundedBorder(isSent ? new Color(0, 92, 75) : new Color(42, 53, 72), 12),
            new EmptyBorder(8, 12, 6, 12)
        ));

        row.add(bubble);
        messagesPanel.add(row);
        messagesPanel.add(Box.createRigidArea(new Dimension(0, 3)));
        messagesPanel.revalidate();

        SwingUtilities.invokeLater(() -> {
            JScrollBar sb = scrollPane.getVerticalScrollBar();
            sb.setValue(sb.getMaximum());
        });
    }

    private void addSystemBubble(String text) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel("<html><center>" + text + "</center></html>");
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lbl.setForeground(new Color(100, 116, 139));
        lbl.setBorder(new CompoundBorder(
            new LineBorder(new Color(42, 53, 72), 1, true),
            new EmptyBorder(4, 12, 4, 12)
        ));
        lbl.setBackground(BG_SYSTEM);
        lbl.setOpaque(true);

        row.add(lbl);
        messagesPanel.add(row);
        messagesPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        messagesPanel.revalidate();

        SwingUtilities.invokeLater(() -> {
            JScrollBar sb = scrollPane.getVerticalScrollBar();
            sb.setValue(sb.getMaximum());
        });
    }

    // ─────────────────────────────────────────────
    // Show Encryption / Decryption dialog
    // ─────────────────────────────────────────────
    private void showEncryptionDialog(int idx) {
        if (idx < 0 || idx >= messageHistory.size()) return;
        MessageRecord rec = messageHistory.get(idx);

        EncryptionDialog dlg = new EncryptionDialog(
            this,
            rec.isSent,
            rec.senderName,
            rec.receiverName,
            rec.plaintext,
            rec.ciphertext,
            myDesKeyHex != null ? myDesKeyHex : "(key not yet set)",
            DESUtil.ivToHex(),
            rec.encDESKey,
            myRsaPubHex != null ? myRsaPubHex : "",
            myRsaPrivHex != null ? myRsaPrivHex : ""
        );
        dlg.setVisible(true);
    }

    // ─────────────────────────────────────────────
    // Key viewer dialog
    // ─────────────────────────────────────────────
    private void showKeyViewer() {
        JDialog dlg = new JDialog(this, "🔑 My Cryptographic Keys — " + myName, true);
        dlg.setSize(560, 500);
        dlg.setLocationRelativeTo(this);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(13, 17, 23));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        panel.add(keyRow("RSA Public Key (shared with peer)", myRsaPubHex,
            new Color(74, 222, 128)));
        panel.add(Box.createRigidArea(new Dimension(0, 12)));
        panel.add(keyRow("RSA Private Key (NEVER shared — stays on device)", myRsaPrivHex,
            new Color(248, 113, 113)));
        panel.add(Box.createRigidArea(new Dimension(0, 12)));
        panel.add(keyRow("DES Session Key (transmitted via RSA)",
            myDesKeyHex != null ? myDesKeyHex : "(not yet established)",
            new Color(251, 191, 36)));
        panel.add(Box.createRigidArea(new Dimension(0, 12)));
        panel.add(keyRow("DES IV (Initialization Vector)", DESUtil.ivToHex(),
            new Color(96, 165, 250)));

        JScrollPane sp = new JScrollPane(panel);
        sp.setBorder(null);
        sp.getViewport().setBackground(new Color(13, 17, 23));

        JButton closeBtn = new JButton("Close");
        closeBtn.setBackground(new Color(37, 211, 102));
        closeBtn.setForeground(Color.BLACK);
        closeBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.addActionListener(e -> dlg.dispose());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(new Color(18, 27, 38));
        footer.add(closeBtn);

        dlg.add(sp, BorderLayout.CENTER);
        dlg.add(footer, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private JPanel keyRow(String label, String value, Color valueColor) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        lbl.setForeground(TEXT_MUTED);

        JTextArea val = new JTextArea(value);
        val.setFont(new Font("Monospaced", Font.PLAIN, 11));
        val.setForeground(valueColor);
        val.setBackground(new Color(15, 23, 36));
        val.setBorder(new CompoundBorder(
            new LineBorder(new Color(42, 53, 72), 1, true),
            new EmptyBorder(8, 10, 8, 10)
        ));
        val.setEditable(false);
        val.setLineWrap(true);
        val.setWrapStyleWord(true);

        p.add(lbl, BorderLayout.NORTH);
        p.add(val, BorderLayout.CENTER);
        return p;
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────
    private void updateStatus(String text) {
        SwingUtilities.invokeLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText(text);
                statusLabel.setForeground(TEXT_MUTED);
            } else {
                System.out.println("[STATUS] " + text);
            }
        });
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ─────────────────────────────────────────────
    // Inner classes
    // ─────────────────────────────────────────────
    static class MessageRecord {
        String senderName, receiverName, plaintext, ciphertext, encDESKey, time;
        boolean isSent;

        MessageRecord(String sender, String receiver, String plain, String cipher,
                      String encKey, boolean sent, String time) {
            this.senderName   = sender;
            this.receiverName = receiver;
            this.plaintext    = plain;
            this.ciphertext   = cipher;
            this.encDESKey    = encKey;
            this.isSent       = sent;
            this.time         = time;
        }
    }

    /** Custom rounded border for chat bubbles */
    static class RoundedBorder implements Border {
        private final Color color;
        private final int radius;

        RoundedBorder(Color color, int radius) {
            this.color  = color;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        }

        @Override
        public Insets getBorderInsets(Component c) { return new Insets(0, 0, 0, 0); }

        @Override
        public boolean isBorderOpaque() { return false; }
    }
}
