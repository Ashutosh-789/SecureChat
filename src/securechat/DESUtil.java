package securechat;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * DES Encryption/Decryption Utility
 * Uses DES/CBC/PKCS5Padding for message encryption.
 */
public class DESUtil {

    private static final String ALGORITHM = "DES";
    private static final String TRANSFORMATION = "DES/CBC/PKCS5Padding";

    // Fixed IV for demo (in production, generate fresh IV per message)
    private static final byte[] IV_BYTES = {0x12, 0x34, 0x56, 0x78, (byte)0x9A, (byte)0xBC, (byte)0xDE, (byte)0xF0};

    /**
     * Generates a new random 56-bit DES secret key.
     */
    public static SecretKey generateDESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
        keyGen.init(56); // DES uses 56-bit effective key length
        return keyGen.generateKey();
    }

    /**
     * Encrypts a plaintext string using DES/CBC.
     * Returns Base64-encoded ciphertext.
     */
    public static String encrypt(String plaintext, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        IvParameterSpec ivSpec = new IvParameterSpec(IV_BYTES);
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        byte[] encrypted = cipher.doFinal(plaintext.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * Decrypts a Base64-encoded DES/CBC ciphertext.
     * Returns the original plaintext string.
     */
    public static String decrypt(String ciphertext, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        IvParameterSpec ivSpec = new IvParameterSpec(IV_BYTES);
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        byte[] decoded = Base64.getDecoder().decode(ciphertext);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted, "UTF-8");
    }

    /**
     * Converts a SecretKey to its hex string representation.
     */
    public static String keyToHex(SecretKey key) {
        byte[] encoded = key.getEncoded();
        StringBuilder sb = new StringBuilder();
        for (byte b : encoded) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    /**
     * Recreates a SecretKey from raw byte array.
     */
    public static SecretKey keyFromBytes(byte[] keyBytes) {
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    /**
     * Returns IV as hex string for display.
     */
    public static String ivToHex() {
        StringBuilder sb = new StringBuilder();
        for (byte b : IV_BYTES) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}
