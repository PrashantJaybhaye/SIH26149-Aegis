package com.aegis.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class EntropyCalculatorTest {

    @Test
    @DisplayName("Shannon Entropy of uniform 0x00 zero-fill should be exactly 0.0 bits/byte")
    void testZeroEntropy() {
        byte[] zeroBuffer = new byte[4096];
        Arrays.fill(zeroBuffer, (byte) 0x00);
        double entropy = EntropyCalculator.calculateShannonEntropy(zeroBuffer, zeroBuffer.length);
        assertEquals(0.0, entropy, 0.0001);
    }

    @Test
    @DisplayName("Shannon Entropy of uniform 0xFF one-fill should be exactly 0.0 bits/byte")
    void testOneEntropy() {
        byte[] oneBuffer = new byte[4096];
        Arrays.fill(oneBuffer, (byte) 0xFF);
        double entropy = EntropyCalculator.calculateShannonEntropy(oneBuffer, oneBuffer.length);
        assertEquals(0.0, entropy, 0.0001);
    }

    @Test
    @DisplayName("Shannon Entropy of cryptographically random data should be close to 8.0 bits/byte")
    void testRandomEntropy() {
        byte[] randomBuffer = new byte[65536];
        new SecureRandom().nextBytes(randomBuffer);
        double entropy = EntropyCalculator.calculateShannonEntropy(randomBuffer, randomBuffer.length);
        assertTrue(entropy > 7.8, "Random byte buffer entropy should be close to 8.0 bits/byte (was " + entropy + ")");
    }

    @Test
    @DisplayName("Peak block entropy should detect high-entropy payloads inside sparse zero blocks")
    void testPeakBlockEntropy() {
        byte[] sparseBuffer = new byte[65536]; // mostly zeroes
        byte[] highEntropyPayload = new byte[16384];
        new SecureRandom().nextBytes(highEntropyPayload);
        System.arraycopy(highEntropyPayload, 0, sparseBuffer, 16384, highEntropyPayload.length);

        double peakEntropy = EntropyCalculator.calculatePeakBlockEntropy(sparseBuffer, sparseBuffer.length, 16384);
        assertTrue(peakEntropy > 7.8, "Peak block entropy must detect embedded high entropy window");
    }

    @Test
    @DisplayName("Should handle edge cases such as null or empty buffers gracefully")
    void testEdgeCases() {
        assertEquals(0.0, EntropyCalculator.calculateShannonEntropy(null, 10));
        assertEquals(0.0, EntropyCalculator.calculateShannonEntropy(new byte[10], 0));
        assertEquals(0.0, EntropyCalculator.calculatePeakBlockEntropy(null, 100, 16));
        assertEquals(0.0, EntropyCalculator.calculatePeakBlockEntropy(new byte[10], 0, 16));
    }
}

