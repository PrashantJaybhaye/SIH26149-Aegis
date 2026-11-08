package com.aegis.util;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Centralized formatting utility for byte sizes, counts, hashes, and timestamps.
 * Eliminates duplicate formatting logic across models, carvers, and UI components.
 */
public final class FormatUtil {

    private static final long ONE_KB = 1024L;
    private static final long ONE_MB = 1024L * 1024L;
    private static final long ONE_GB = 1024L * 1024L * 1024L;
    private static final long ONE_TB = 1024L * 1024L * 1024L * 1024L;

    public static final DateTimeFormatter STANDARD_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private FormatUtil() {
        // Utility class: prevent instantiation
    }

    /**
     * Formats raw bytes into a human-readable string (B, KB, MB, GB, TB).
     *
     * @param bytes count of bytes
     * @return readable formatted string (e.g., "512.00 GB", "14.20 MB", "4.50 KB", "512 Bytes")
     */
    public static String formatBytes(long bytes) {
        if (bytes < 0) {
            return "0 Bytes";
        }
        if (bytes >= ONE_TB) {
            return String.format(Locale.US, "%.2f TB", (double) bytes / ONE_TB);
        }
        if (bytes >= ONE_GB) {
            return String.format(Locale.US, "%.2f GB", (double) bytes / ONE_GB);
        }
        if (bytes >= ONE_MB) {
            return String.format(Locale.US, "%.2f MB", (double) bytes / ONE_MB);
        }
        if (bytes >= ONE_KB) {
            return String.format(Locale.US, "%.2f KB", (double) bytes / ONE_KB);
        }
        return bytes + " Bytes";
    }

    /**
     * Formats integer numbers with standard comma groupings (e.g. 12,345).
     *
     * @param count numeric count
     * @return comma-separated formatted string
     */
    public static String formatCount(long count) {
        return NumberFormat.getIntegerInstance(Locale.US).format(count);
    }

    /**
     * Shortens a hash string for display with ellipsis.
     *
     * @param hash full hash string
     * @param maxLength maximum visible length before ellipsis
     * @return truncated hash or original if shorter
     */
    public static String truncateHash(String hash, int maxLength) {
        if (hash == null || hash.isEmpty()) {
            return "N/A";
        }
        if (hash.length() <= maxLength) {
            return hash;
        }
        return hash.substring(0, Math.max(0, maxLength)) + "...";
    }

    /**
     * Returns the current timestamp in standard format.
     */
    public static String currentTimestamp() {
        return LocalDateTime.now().format(STANDARD_DATE_TIME);
    }
}

