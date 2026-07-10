package securechat;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/**
 * EncryptionDialog — pops up when a user clicks a message bubble.
 * Shows step-by-step encryption or decryption details in a
 * WhatsApp-inspired dark dialog.
 */
public class EncryptionDialog extends JDialog {

    private static final Color BG_DARK   = new Color(13, 17, 23);
    private static final Color BG_PANEL  = new Color(22, 33, 46);
    private static final Color BG_CARD   = new Color(15, 23, 36);
    private static final Color COLOR_GREEN  = new Color(37, 211, 102);
    private static final Color COLOR_YELLOW = new Color(251, 191, 36);
    private static final Color COLOR_PURPLE = new Color(192, 132, 252);
    private static final Color COLOR_RED    = new Color(248, 113, 113);
    private static final Color COLOR_BLUE   = new Color(96, 165, 250);
    private static final Color BORDER_COLOR = new Color(42, 53, 72);
    private static final Color TEXT_PRIMARY = new Color(226, 232, 240);
    private static final Color TEXT_MUTED   = new Color(148, 163, 184);
    private static final Font  MONO_FONT    = new Font("Monospaced", Font.PLAIN, 11);
    private static final Font  TITLE_FONT   = new Font("SansSerif", Font.BOLD, 13);
    private static final Font  BODY_FONT    = new Font("SansSerif", Font.PLAIN, 12);

    /**
     * @param parent      parent frame
     * @param isSent      true = show encryption flow, false = show decryption flow
     * @param senderName  name of message sender
     * @param receiverName name of message receiver
     * @param plaintext   original message text
     * @param ciphertext  DES-encrypted Base64 string
     * @param desKeyHex   DES key in hex
     * @param ivHex       DES IV in hex
     * @param encDesKey   RSA-encrypted DES key (Base64 preview)
     * @param rsaPubHex   RSA public key hex preview
     * @param rsaPrivHex  RSA private key hex preview (shown only on receiver side)
     */
    public EncryptionDialog(JFrame parent, boolean isSent,
                            String senderName, String receiverName,
                            String plaintext, String ciphertext,
                            String desKeyHex, String ivHex,
                            String encDesKey, String rsaPubHex, String rsaPrivHex) {

        super(parent, (isSent ? "🔒 Encryption Details — " : "🔓 Decryption Details — ")
                + (isSent ? senderName + " sent" : receiverName + " received"), true);

        setSize(620, 680);
        setLocationRelativeTo(parent);
        setResizable(true);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_DARK);

        // ── Header ──
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(22, 33, 46));
        header.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER_COLOR),
            new EmptyBorder(14, 20, 14, 20)
        ));

        JLabel titleLbl = new JLabel(isSent
            ? "🔒  Hybrid Encryption: DES + RSA"
            : "🔓  Hybrid Decryption: RSA + DES");
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 15));
        titleLbl.setForeground(isSent ? COLOR_YELLOW : COLOR_GREEN);

        JLabel subLbl = new JLabel(isSent
            ? senderName + "  →  " + receiverName
            : senderName + "  →  " + receiverName + "  (decrypted by " + receiverName + ")");
        subLbl.setFont(BODY_FONT);
        subLbl.setForeground(TEXT_MUTED);

        JPanel headerText = new JPanel(new GridLayout(2, 1, 0, 3));
        headerText.setOpaque(false);
        headerText.add(titleLbl);
        headerText.add(subLbl);
        header.add(headerText, BorderLayout.CENTER);

        // ── Scrollable Content ──
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG_DARK);
        content.setBorder(new EmptyBorder(16, 16, 16, 16));

        if (isSent) {
            // SENDER VIEW: Show encryption steps
            content.add(buildStepCard("01", "Original Plaintext", COLOR_GREEN,
                row("Message", plaintext, COLOR_GREEN)));

            content.add(spacer(8));
            content.add(buildArrow("DES Encrypt ↓"));
            content.add(spacer(8));

            content.add(buildStepCard("02", "DES Encryption", COLOR_YELLOW,
                row("Algorithm", "DES / CBC / PKCS5Padding", TEXT_MUTED),
                row("DES Key (hex)", desKeyHex, COLOR_PURPLE),
                row("IV (hex)", ivHex, COLOR_BLUE),
                row("Ciphertext (Base64)", ciphertext, COLOR_RED)));

            content.add(spacer(8));
            content.add(buildArrow("RSA Key Wrap ↓"));
            content.add(spacer(8));

            content.add(buildStepCard("03", "RSA Key Wrapping", COLOR_PURPLE,
                row("Purpose", "Encrypt DES key with " + receiverName + "'s RSA public key", TEXT_MUTED),
                row("RSA Algorithm", "RSA / ECB / PKCS1Padding (2048-bit)", TEXT_MUTED),
                row(receiverName + "'s Public Key (preview)", rsaPubHex, COLOR_PURPLE),
                row("RSA-Encrypted DES Key", encDesKey.substring(0, Math.min(60, encDesKey.length())) + "...", COLOR_RED)));

            content.add(spacer(8));
            content.add(buildArrow("Transmitted over network ↓"));
            content.add(spacer(8));

            content.add(buildStepCard("04", "What the Server Sees", COLOR_BLUE,
                row("Server receives", "Only ciphertext — cannot decrypt!", TEXT_MUTED),
                row("Ciphertext (Base64)", ciphertext.substring(0, Math.min(40, ciphertext.length())) + "...", COLOR_RED),
                row("Encrypted DES Key", encDesKey.substring(0, Math.min(40, encDesKey.length())) + "...", COLOR_RED),
                row("Server can read?", "❌  NO — True End-to-End Encryption", new Color(248, 113, 113))));

        } else {
            // RECEIVER VIEW: Show decryption steps
            content.add(buildStepCard("01", "Received Ciphertext", COLOR_RED,
                row("Algorithm", "DES / CBC / PKCS5Padding", TEXT_MUTED),
                row("Ciphertext (Base64)", ciphertext, COLOR_RED)));

            content.add(spacer(8));
            content.add(buildArrow("RSA Key Unwrap ↓"));
            content.add(spacer(8));

            content.add(buildStepCard("02", "RSA Key Unwrapping", COLOR_PURPLE,
                row("Purpose", "Decrypt DES key using " + receiverName + "'s RSA private key", TEXT_MUTED),
                row(receiverName + "'s Private Key (preview)", rsaPrivHex, COLOR_RED),
                row("Recovered DES Key (hex)", desKeyHex, COLOR_GREEN)));

            content.add(spacer(8));
            content.add(buildArrow("DES Decrypt ↓"));
            content.add(spacer(8));

            content.add(buildStepCard("03", "DES Decryption", COLOR_YELLOW,
                row("DES Key (hex)", desKeyHex, COLOR_PURPLE),
                row("IV (hex)", ivHex, COLOR_BLUE),
                row("Algorithm", "DES / CBC / PKCS5Padding", TEXT_MUTED),
                row("Decrypted Plaintext", plaintext, COLOR_GREEN)));

            content.add(spacer(8));

            // Success card
            JPanel successCard = buildCard(new Color(0, 26, 10), new Color(21, 83, 45));
            successCard.setLayout(new BoxLayout(successCard, BoxLayout.Y_AXIS));
            JLabel okLbl = new JLabel("✅  Message successfully decrypted!");
            okLbl.setFont(TITLE_FONT);
            okLbl.setForeground(COLOR_GREEN);
            okLbl.setBorder(new EmptyBorder(0, 0, 8, 0));
            JLabel msgLbl = new JLabel("<html><i>\"" + plaintext + "\"</i></html>");
            msgLbl.setFont(new Font("SansSerif", Font.ITALIC, 13));
            msgLbl.setForeground(COLOR_GREEN);
            JLabel noteLbl = new JLabel("<html>Only " + receiverName + " could decrypt this —<br>their private RSA key never left their device.</html>");
            noteLbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
            noteLbl.setForeground(new Color(74, 222, 128, 180));
            noteLbl.setBorder(new EmptyBorder(6, 0, 0, 0));
            successCard.add(okLbl);
            successCard.add(msgLbl);
            successCard.add(noteLbl);
            content.add(successCard);
        }

        // Summary footer
        content.add(spacer(12));
        content.add(buildSummaryCard());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBackground(BG_DARK);
        scroll.getViewport().setBackground(BG_DARK);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        // Close button
        JButton closeBtn = new JButton("Close");
        closeBtn.setBackground(new Color(37, 211, 102));
        closeBtn.setForeground(Color.BLACK);
        closeBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setPreferredSize(new Dimension(120, 36));
        closeBtn.addActionListener(e -> dispose());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(BG_PANEL);
        footer.setBorder(new MatteBorder(1, 0, 0, 0, BORDER_COLOR));
        footer.add(closeBtn);

        root.add(header, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }

    // ── Builder Helpers ──

    private JPanel buildStepCard(String num, String title, Color accentColor, JComponent... rows) {
        JPanel card = buildCard(BG_CARD, BORDER_COLOR);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        // Step header
        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        head.setOpaque(false);

        JLabel numBadge = new JLabel(num);
        numBadge.setFont(new Font("Monospaced", Font.BOLD, 11));
        numBadge.setForeground(accentColor);
        numBadge.setBorder(new CompoundBorder(
            new LineBorder(accentColor, 1, true),
            new EmptyBorder(1, 6, 1, 6)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(TITLE_FONT);
        titleLbl.setForeground(accentColor);

        head.add(numBadge);
        head.add(titleLbl);
        head.setBorder(new EmptyBorder(0, 0, 8, 0));
        card.add(head);

        // Separator
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER_COLOR);
        sep.setBackground(BORDER_COLOR);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        card.add(sep);
        card.add(spacer(8));

        for (JComponent row : rows) {
            card.add(row);
            card.add(spacer(6));
        }

        return card;
    }

    private JPanel buildCard(Color bg, Color border) {
        JPanel card = new JPanel();
        card.setBackground(bg);
        card.setBorder(new CompoundBorder(
            new LineBorder(border, 1, true),
            new EmptyBorder(12, 14, 12, 14)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    private JPanel row(String label, String value, Color valueColor) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(label + ":");
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lbl.setForeground(TEXT_MUTED);
        lbl.setPreferredSize(new Dimension(180, 16));

        // Value — use JTextArea for long values to allow wrapping
        JTextArea val = new JTextArea(value);
        val.setFont(MONO_FONT);
        val.setForeground(valueColor);
        val.setBackground(new Color(7, 13, 26));
        val.setBorder(new EmptyBorder(3, 8, 3, 8));
        val.setLineWrap(true);
        val.setWrapStyleWord(true);
        val.setEditable(false);
        val.setOpaque(true);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.CENTER);
        return row;
    }

    private Component spacer(int h) {
        return Box.createRigidArea(new Dimension(0, h));
    }

    private JPanel buildArrow(String label) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.ITALIC, 12));
        lbl.setForeground(new Color(100, 116, 139));
        p.add(lbl);
        return p;
    }

    private JPanel buildSummaryCard() {
        JPanel card = buildCard(new Color(13, 23, 36), new Color(42, 53, 72));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Hybrid Encryption Summary");
        title.setFont(TITLE_FONT);
        title.setForeground(TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(0, 0, 8, 0));
        card.add(title);

        String[] lines = {
            "🔑  RSA-2048  —  Asymmetric. Used only for DES key exchange.",
            "🔒  DES-56    —  Symmetric. Used for fast message encryption.",
            "📡  Server    —  Sees only ciphertext. True end-to-end encryption.",
            "🛡️  Security  —  Private keys never leave the client device."
        };
        for (String line : lines) {
            JLabel lbl = new JLabel(line);
            lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
            lbl.setForeground(TEXT_MUTED);
            lbl.setBorder(new EmptyBorder(2, 0, 2, 0));
            card.add(lbl);
        }
        return card;
    }
}
