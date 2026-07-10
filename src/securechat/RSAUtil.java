package securechat;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.*;
import java.util.Base64;

/**
 * RSA Encryption/Decryption Utility
 * Uses RSA/ECB/PKCS1Padding for DES key exchange.
 */
public class RSAUtil {

    private static final String ALGORITHM = "RSA";
    private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";
    private static final int KEY_SIZE = 2048;

    /**
     * Generates a 2048-bit RSA key pair (public + private).
     */
    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(ALGORITHM);
        keyPairGen.initialize(KEY_SIZE, new SecureRandom());
        return keyPairGen.generateKeyPair();
    }

    /**
     * Encrypts a DES SecretKey using RSA public key.
     * Returns Base64-encoded encrypted key bytes.
     */
    public static String encryptDESKey(SecretKey desKey, PublicKey rsaPublicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey);
        byte[] encryptedKey = cipher.doFinal(desKey.getEncoded());
        return Base64.getEncoder().encodeToString(encryptedKey);
    }

    /**
     * Decrypts a Base64-encoded RSA-encrypted DES key using RSA private key.
     * Returns the recovered DES SecretKey.
     */
    public static SecretKey decryptDESKey(String encryptedKeyB64, PrivateKey rsaPrivateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);
        byte[] decoded = Base64.getDecoder().decode(encryptedKeyB64);
        byte[] decryptedKey = cipher.doFinal(decoded);
        return DESUtil.keyFromBytes(decryptedKey);
    }

    /**
     * Converts a PublicKey to a short hex preview string for display.
     */
    public static String publicKeyToHex(PublicKey key) {
        byte[] encoded = key.getEncoded();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(encoded.length, 32); i++) {
            sb.append(String.format("%02X", encoded[i]));
            if (i < 31) sb.append(":");
        }
        sb.append("...");
        return sb.toString();
    }

    /**
     * Converts a PrivateKey to a short hex preview string for display.
     */
    public static String privateKeyToHex(PrivateKey key) {
        byte[] encoded = key.getEncoded();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(encoded.length, 32); i++) {
            sb.append(String.format("%02X", encoded[i]));
            if (i < 31) sb.append(":");
        }
        sb.append("...");
        return sb.toString();
    }

    /**
     * Returns full Base64 representation of a PublicKey.
     */
    public static String publicKeyToBase64(PublicKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
}
