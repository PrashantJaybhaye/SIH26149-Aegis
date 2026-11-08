package com.forensix.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * High-performance cryptographic hashing utility for forensic evidence verification.
 * Provides SHA-256 computation for byte buffers, strings, and files.
 */
public final class HashUtil {

    private static final String SHA_256 = "SHA-256";
    private static final int BUFFER_SIZE = 65536; // 64 KB read buffer

    private HashUtil() {
        // Prevent instantiation of utility class
    }

    /**
     * Calculates SHA-256 hash for a UTF-8 string.
     */
    public static String calculateSHA256(String text) {
        if (text == null) {
            return "UNKNOWN";
        }
        return calculateSHA256(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Calculates SHA-256 hash for an entire byte array.
     */
    public static String calculateSHA256(byte[] data) {
        if (data == null) {
            return "UNKNOWN";
        }
        return calculateSHA256(data, data.length);
    }

    /**
     * Calculates SHA-256 hash for a slice of a byte array up to the specified length.
     */
    public static String calculateSHA256(byte[] data, int length) {
        if (data == null || length <= 0) {
            return "UNKNOWN";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            digest.update(data, 0, Math.min(length, data.length));
            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            return "UNKNOWN";
        }
    }

    /**
     * Calculates SHA-256 hash for a file stream.
     */
    public static String calculateSHA256(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return "UNKNOWN";
        }
        try (InputStream fis = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) > 0) {
                digest.update(buffer, 0, bytesRead);
            }
            return bytesToHex(digest.digest());
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    /**
     * Converts a raw byte array into a lowercase hexadecimal string.
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        StringBuilder hexString = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }
}
