package com.forensix.util;

public class EntropyCalculator {

    /**
     * Calculates Shannon Entropy of a byte buffer in bits per byte (0.0 to 8.0).
     * 0.0 = Uniform single byte (e.g. all 0x00 zeroes or 0xFF)
     * 8.0 = Completely random data (e.g. encrypted or pseudo-random wipe pass)
     */
    public static double calculateShannonEntropy(byte[] data, int length) {
        if (data == null || length == 0) {
            return 0.0;
        }

        int[] frequency = new int[256];
        for (int i = 0; i < length; i++) {
            frequency[data[i] & 0xFF]++;
        }

        double entropy = 0.0;
        double len = length;

        for (int count : frequency) {
            if (count > 0) {
                double probability = count / len;
                entropy -= probability * (Math.log(probability) / Math.log(2.0));
            }
        }

        return entropy;
    }

    /**
     * Calculates peak block entropy across sub-blocks (e.g. 16 KB windows).
     * Prevents small file payloads, filesystem tables, and headers from being diluted by vast surrounding zero-fill blocks.
     */
    public static double calculatePeakBlockEntropy(byte[] data, int length, int blockSize) {
        if (data == null || length <= 0) return 0.0;
        if (blockSize <= 0 || blockSize >= length) return calculateShannonEntropy(data, length);

        double maxEntropy = 0.0;
        int[] freq = new int[256];

        for (int offset = 0; offset < length; offset += blockSize) {
            int curLen = Math.min(blockSize, length - offset);
            java.util.Arrays.fill(freq, 0);

            for (int i = 0; i < curLen; i++) {
                freq[data[offset + i] & 0xFF]++;
            }

            double blockEntropy = 0.0;
            double len = curLen;
            for (int count : freq) {
                if (count > 0) {
                    double p = count / len;
                    blockEntropy -= p * (Math.log(p) / Math.log(2.0));
                }
            }

            if (blockEntropy > maxEntropy) {
                maxEntropy = blockEntropy;
            }
        }
        return maxEntropy;
    }
}
