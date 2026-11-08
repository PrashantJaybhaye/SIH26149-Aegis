package com.aegis.util;

import java.util.Arrays;

/**
 * Shannon Entropy Calculator for forensic storage analysis.
 * <p>
 * Measures information density in bits per byte (0.0 to 8.0):
 * <ul>
 *   <li>0.0 bits/byte: Uniform byte fill (e.g., all 0x00 zero-fill or 0xFF).</li>
 *   <li>~1.0 to 3.5 bits/byte: Plain text and structured data.</li>
 *   <li>~6.0 to 7.5 bits/byte: Compressed media (JPEG, MP3, MP4).</li>
 *   <li>~7.9 to 8.0 bits/byte: High-entropy encrypted blocks or cryptographically pseudo-random wipe patterns.</li>
 * </ul>
 */
public final class EntropyCalculator {

    private static final double LOG_2 = Math.log(2.0);

    private EntropyCalculator() {
        // Utility class
    }

    /**
     * Calculates the Shannon Entropy of a byte buffer starting from index 0.
     *
     * @param data byte array
     * @param length number of bytes to evaluate
     * @return entropy score from 0.0 to 8.0 bits/byte
     */
    public static double calculateShannonEntropy(byte[] data, int length) {
        return calculateShannonEntropy(data, 0, length);
    }

    /**
     * Calculates the Shannon Entropy of a slice of a byte buffer.
     *
     * @param data byte array
     * @param offset starting index
     * @param length number of bytes to evaluate
     * @return entropy score from 0.0 to 8.0 bits/byte
     */
    public static double calculateShannonEntropy(byte[] data, int offset, int length) {
        if (data == null || length <= 0 || offset < 0 || offset >= data.length) {
            return 0.0;
        }

        int actualLength = Math.min(length, data.length - offset);
        int[] frequency = new int[256];

        for (int i = 0; i < actualLength; i++) {
            frequency[data[offset + i] & 0xFF]++;
        }

        double entropy = 0.0;
        double totalBytes = actualLength;

        for (int count : frequency) {
            if (count > 0) {
                double probability = count / totalBytes;
                entropy -= probability * (Math.log(probability) / LOG_2);
            }
        }

        return entropy;
    }

    /**
     * Calculates peak block entropy across sub-blocks (e.g. 16 KB windows).
     * <p>
     * Prevents small file payloads, filesystem tables, and headers from being diluted
     * by vast surrounding zero-fill blocks.
     *
     * @param data byte buffer
     * @param length number of bytes in data to evaluate
     * @param blockSize window size in bytes
     * @return highest entropy found among all sub-windows
     */
    public static double calculatePeakBlockEntropy(byte[] data, int length, int blockSize) {
        if (data == null || length <= 0) {
            return 0.0;
        }
        if (blockSize <= 0 || blockSize >= length) {
            return calculateShannonEntropy(data, 0, length);
        }

        double maxEntropy = 0.0;
        for (int offset = 0; offset < length; offset += blockSize) {
            int currentChunkLength = Math.min(blockSize, length - offset);
            double blockEntropy = calculateShannonEntropy(data, offset, currentChunkLength);
            if (blockEntropy > maxEntropy) {
                maxEntropy = blockEntropy;
            }
        }
        return maxEntropy;
    }
}
