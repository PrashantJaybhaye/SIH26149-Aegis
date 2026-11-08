package com.aegis.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class HashUtilTest {

    @Test
    @DisplayName("Should return valid SHA-256 for standard string input")
    void testCalculateSHA256String() {
        String input = "AEGIS_TEST_VECTOR";
        String hash = HashUtil.calculateSHA256(input);
        assertNotNull(hash);
        assertEquals(64, hash.length(), "SHA-256 hex string must be exactly 64 characters");
        assertNotEquals("UNKNOWN", hash);
    }

    @Test
    @DisplayName("Should handle null and empty string inputs gracefully")
    void testCalculateSHA256NullOrEmpty() {
        assertEquals("UNKNOWN", HashUtil.calculateSHA256((String) null));
        assertEquals("UNKNOWN", HashUtil.calculateSHA256((byte[]) null));
        assertEquals("UNKNOWN", HashUtil.calculateSHA256((File) null));
    }

    @Test
    @DisplayName("Should compute accurate SHA-256 for byte arrays and match slice calculations")
    void testCalculateSHA256Bytes() {
        byte[] data = "Hello AEGIS".getBytes();
        String fullHash = HashUtil.calculateSHA256(data);
        String sliceHash = HashUtil.calculateSHA256(data, data.length);
        assertEquals(fullHash, sliceHash);
    }

    @Test
    @DisplayName("Should compute correct SHA-256 hash for a disk file")
    void testCalculateSHA256File() throws IOException {
        File tempFile = File.createTempFile("AEGIS_test", ".tmp");
        tempFile.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write("TamperEvidentForensicData".getBytes());
        }

        String fileHash = HashUtil.calculateSHA256(tempFile);
        String memoryHash = HashUtil.calculateSHA256("TamperEvidentForensicData".getBytes());
        assertEquals(memoryHash, fileHash);
    }

    @Test
    @DisplayName("Should format byte arrays to lowercase hexadecimal strings")
    void testBytesToHex() {
        byte[] bytes = new byte[]{(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF};
        assertEquals("deadbeef", HashUtil.bytesToHex(bytes));
        assertEquals("", HashUtil.bytesToHex(null));
        assertEquals("", HashUtil.bytesToHex(new byte[0]));
    }
}

