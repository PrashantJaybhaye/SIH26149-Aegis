package com.aegis.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FormatUtilTest {

    @Test
    @DisplayName("Should correctly format byte quantities across all forensic unit scales")
    void testFormatBytes() {
        assertEquals("0 Bytes", FormatUtil.formatBytes(0));
        assertEquals("0 Bytes", FormatUtil.formatBytes(-50));
        assertEquals("512 Bytes", FormatUtil.formatBytes(512));
        assertEquals("4.00 KB", FormatUtil.formatBytes(4096));
        assertEquals("14.50 MB", FormatUtil.formatBytes((long) (14.5 * 1024 * 1024)));
        assertEquals("500.00 GB", FormatUtil.formatBytes(500L * 1024 * 1024 * 1024));
        assertEquals("2.00 TB", FormatUtil.formatBytes(2L * 1024 * 1024 * 1024 * 1024));
    }

    @Test
    @DisplayName("Should format integers with comma grouping")
    void testFormatCount() {
        assertEquals("0", FormatUtil.formatCount(0));
        assertEquals("999", FormatUtil.formatCount(999));
        assertEquals("1,000", FormatUtil.formatCount(1000));
        assertEquals("12,345,678", FormatUtil.formatCount(12345678));
    }

    @Test
    @DisplayName("Should truncate long hashes for compact UI rendering")
    void testTruncateHash() {
        String hash = "d9604011c185eff406214682a6ecd3113f3cbb5a6cf4127fed121597a2c6f059";
        assertEquals("d9604011c185eff4...", FormatUtil.truncateHash(hash, 16));
        assertEquals("N/A", FormatUtil.truncateHash(null, 16));
        assertEquals("abc", FormatUtil.truncateHash("abc", 10));
    }

    @Test
    @DisplayName("Should generate valid non-empty current timestamps")
    void testCurrentTimestamp() {
        String ts = FormatUtil.currentTimestamp();
        assertNotNull(ts);
        assertTrue(ts.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$"),
                "Timestamp should match yyyy-MM-dd HH:mm:ss format (was: " + ts + ")");
    }
}

