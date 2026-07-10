package securechat;

import java.io.Serializable;

/**
 * Represents a message exchanged between clients via the server.
 * Carries the DES-encrypted message body and the RSA-wrapped DES key.
 * Implements Serializable for transmission over ObjectStreams.
 */
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_HANDSHAKE = "HANDSHAKE"; // RSA public key exchange
    public static final String TYPE_KEY        = "KEY";       // RSA-encrypted DES key
    public static final String TYPE_MESSAGE    = "MESSAGE";   // DES-encrypted chat message
    public static final String TYPE_SYSTEM     = "SYSTEM";    // Server info message

    private String type;
    private String sender;
    private String receiver;

    // For TYPE_MESSAGE: DES-encrypted ciphertext (Base64)
    private String encryptedBody;

    // For TYPE_KEY: RSA-encrypted DES key (Base64)
    private String encryptedDESKey;

    // For TYPE_HANDSHAKE: sender's RSA public key (Base64)
    private String rsaPublicKey;

    // For TYPE_SYSTEM: plain text info
    private String systemText;

    // Metadata for display in dialogs
    private String senderKeyHex;      // DES key in hex (shown in enc dialog)
    private String ciphertextPreview; // Short ciphertext preview

    public Message() {}

    // ── Getters & Setters ──

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }

    public String getEncryptedBody() { return encryptedBody; }
    public void setEncryptedBody(String encryptedBody) { this.encryptedBody = encryptedBody; }

    public String getEncryptedDESKey() { return encryptedDESKey; }
    public void setEncryptedDESKey(String encryptedDESKey) { this.encryptedDESKey = encryptedDESKey; }

    public String getRsaPublicKey() { return rsaPublicKey; }
    public void setRsaPublicKey(String rsaPublicKey) { this.rsaPublicKey = rsaPublicKey; }

    public String getSystemText() { return systemText; }
    public void setSystemText(String systemText) { this.systemText = systemText; }

    public String getSenderKeyHex() { return senderKeyHex; }
    public void setSenderKeyHex(String senderKeyHex) { this.senderKeyHex = senderKeyHex; }

    public String getCiphertextPreview() { return ciphertextPreview; }
    public void setCiphertextPreview(String ciphertextPreview) { this.ciphertextPreview = ciphertextPreview; }

    @Override
    public String toString() {
        return "[Message type=" + type + " from=" + sender + " to=" + receiver + "]";
    }
}
