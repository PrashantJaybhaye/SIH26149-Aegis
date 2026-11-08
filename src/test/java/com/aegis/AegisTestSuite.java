package com.aegis;

import com.aegis.blockchain.AuditBlockchain;
import com.aegis.carver.FileCarver;
import com.aegis.carver.FileSignature;
import com.aegis.model.CarvedFile;
import com.aegis.model.SanitizationStandard;
import com.aegis.model.StorageDevice;
import com.aegis.util.EntropyCalculator;
import com.aegis.util.FormatUtil;
import com.aegis.util.HashUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

/**
 * Self-contained automated test suite for AEGIS.
 * <p>
 * Validates cryptographic hashing, Shannon entropy, human-readable formatting,
 * storage device models, file signatures, magic-byte raw carving, and blockchain audit integrity
 * without relying on external test runner plugins.
 */
public class AegisTestSuite {

    private static int testsRun = 0;
    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("=========================================================================");
        System.out.println("   AEGIS: Automated Comprehensive Verification & Test Suite           ");
        System.out.println("=========================================================================");

        testHashUtil();
        testEntropyCalculator();
        testFormatUtil();
        testModels();
        testAuditBlockchain();
        testFileSignatureCatalog();
        testFileCarverEngine();

        System.out.println("\n-------------------------------------------------------------------------");
        System.out.printf("   TEST SUITE SUMMARY: Run: %d | Passed: %d | Failed: %d%n", testsRun, testsPassed, testsFailed);
        System.out.println("-------------------------------------------------------------------------");

        if (testsFailed > 0) {
            System.err.println("Verification FAILED with " + testsFailed + " failure(s).");
            System.exit(1);
        } else {
            System.out.println("[SUCCESS] All AEGIS test suites passed verification successfully!");
        }
    }

    private static void check(String testName, boolean condition) {
        testsRun++;
        if (condition) {
            testsPassed++;
            System.out.printf("  [PASS] %s%n", testName);
        } else {
            testsFailed++;
            System.err.printf("  [FAIL] %s%n", testName);
        }
    }

    private static void testHashUtil() {
        System.out.println("\n--- 1. Cryptographic Hashing (HashUtil) ---");
        String hash = HashUtil.calculateSHA256("AEGIS_TEST_VECTOR");
        check("SHA-256 computation returns 64 hex characters", hash != null && hash.length() == 64);
        check("Null string input returns UNKNOWN", "UNKNOWN".equals(HashUtil.calculateSHA256((String) null)));
        check("Null byte array returns UNKNOWN", "UNKNOWN".equals(HashUtil.calculateSHA256((byte[]) null)));

        byte[] data = "Hello AEGIS".getBytes();
        String fullHash = HashUtil.calculateSHA256(data);
        String sliceHash = HashUtil.calculateSHA256(data, data.length);
        check("Byte array and slice hashing match", fullHash.equals(sliceHash));

        byte[] hexBytes = new byte[]{(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF};
        check("bytesToHex produces correct hex string", "deadbeef".equals(HashUtil.bytesToHex(hexBytes)));
    }

    private static void testEntropyCalculator() {
        System.out.println("\n--- 2. Shannon Entropy Calculator (EntropyCalculator) ---");
        byte[] zeroBuffer = new byte[4096];
        Arrays.fill(zeroBuffer, (byte) 0x00);
        double zeroEntropy = EntropyCalculator.calculateShannonEntropy(zeroBuffer, zeroBuffer.length);
        check("Zero fill entropy is 0.0 bits/byte", zeroEntropy == 0.0);

        byte[] oneBuffer = new byte[4096];
        Arrays.fill(oneBuffer, (byte) 0xFF);
        double oneEntropy = EntropyCalculator.calculateShannonEntropy(oneBuffer, oneBuffer.length);
        check("One fill entropy is 0.0 bits/byte", oneEntropy == 0.0);

        byte[] randomBuffer = new byte[65536];
        new SecureRandom().nextBytes(randomBuffer);
        double randEntropy = EntropyCalculator.calculateShannonEntropy(randomBuffer, randomBuffer.length);
        check("Random byte buffer entropy is > 7.8 bits/byte", randEntropy > 7.8);

        byte[] sparseBuffer = new byte[65536];
        byte[] payload = new byte[16384];
        new SecureRandom().nextBytes(payload);
        System.arraycopy(payload, 0, sparseBuffer, 16384, payload.length);
        double peakEntropy = EntropyCalculator.calculatePeakBlockEntropy(sparseBuffer, sparseBuffer.length, 16384);
        check("Peak block entropy detects embedded high-entropy window", peakEntropy > 7.8);
    }

    private static void testFormatUtil() {
        System.out.println("\n--- 3. Centralized Formatting Utility (FormatUtil) ---");
        check("0 bytes formatted correctly", "0 Bytes".equals(FormatUtil.formatBytes(0)));
        check("KB formatted correctly", "4.00 KB".equals(FormatUtil.formatBytes(4096)));
        check("MB formatted correctly", "14.50 MB".equals(FormatUtil.formatBytes((long) (14.5 * 1024 * 1024))));
        check("GB formatted correctly", "500.00 GB".equals(FormatUtil.formatBytes(500L * 1024 * 1024 * 1024)));
        check("TB formatted correctly", "2.00 TB".equals(FormatUtil.formatBytes(2L * 1024 * 1024 * 1024 * 1024)));

        check("Count formatting with thousands separator", "1,234,567".equals(FormatUtil.formatCount(1234567)));
        check("Truncate hash with ellipsis", "abcdef123456...".equals(FormatUtil.truncateHash("abcdef1234567890abcdef", 12)));
        check("Standard timestamp generated", FormatUtil.currentTimestamp().matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$"));
    }

    private static void testModels() {
        System.out.println("\n--- 4. Domain Models (StorageDevice, CarvedFile, SanitizationStandard) ---");
        StorageDevice dev = new StorageDevice("\\\\.\\PhysicalDrive1", "SanDisk Ultra", "SN-12345", 32L * 1024 * 1024 * 1024, 512, "USB", false);
        check("Physical drive detected correctly", dev.isPhysical());
        check("Sector count calculated correctly", dev.getTotalSectors() == (32L * 1024 * 1024 * 1024 / 512));
        check("Formatted size string matches 32.00 GB", "32.00 GB".equals(dev.getFormattedSize()));

        CarvedFile carved = new CarvedFile("CARVE_0001", "JPEG Image", ".jpg", 100L, 51200L, 204800L, 0.98, "a1b2c3d4e5f67890", true);
        check("Carved file ID matches", "CARVE_0001".equals(carved.getId()));
        check("Carved file formatted size matches 200.00 KB", "200.00 KB".equals(carved.getFormattedSize()));
        check("Carved file intact flag is true", carved.isIntact());

        check("NIST 800-88 Clear pass count is 1", SanitizationStandard.NIST_800_88_CLEAR.getTotalPasses() == 1);
        check("DoD 5220.22-M pass count is 3", SanitizationStandard.DOD_5220_22_M.getTotalPasses() == 3);
        check("Gutmann Lite pass count is 7", SanitizationStandard.GUTMANN_LITE.getTotalPasses() == 7);
    }

    private static void testAuditBlockchain() {
        System.out.println("\n--- 5. Cryptographic Audit Blockchain (AuditBlockchain) ---");
        AuditBlockchain blockchain = AuditBlockchain.getInstance();
        check("Blockchain instance loaded", blockchain != null);
        check("Genesis block present", !blockchain.getBlocks().isEmpty());

        AuditBlockchain.Block newBlock = blockchain.recordAuditBlock(
                "VERIFICATION_TEST", "\\\\.\\PhysicalDrive99",
                "Self-Test Verification", "test_hash_val", "Automated self-test verification block"
        );
        check("Audit block created and indexed", newBlock != null && newBlock.getIndex() > 0);
        check("Cryptographic chain linkage valid", blockchain.isChainValid());
    }

    private static void testFileSignatureCatalog() {
        System.out.println("\n--- 6. Forensic Signature Catalog (FileSignature) ---");
        List<FileSignature> sigs = FileSignature.getStandardForensicSignatures();
        check("Signatures catalog is populated", sigs != null && sigs.size() >= 10);
        check("Contains JPEG signature", sigs.stream().anyMatch(s -> s.getExtension().equalsIgnoreCase(".jpg")));
        check("Contains PNG signature", sigs.stream().anyMatch(s -> s.getExtension().equalsIgnoreCase(".png")));
        check("Contains PDF signature", sigs.stream().anyMatch(s -> s.getExtension().equalsIgnoreCase(".pdf")));
        check("Contains MP4 signature", sigs.stream().anyMatch(s -> s.getExtension().equalsIgnoreCase(".mp4")));
        check("Contains ZIP signature", sigs.stream().anyMatch(s -> s.getExtension().equalsIgnoreCase(".zip")));
    }

    private static void testFileCarverEngine() {
        System.out.println("\n--- 7. File Carver Engine (FileCarver) ---");
        try {
            File tempRaw = File.createTempFile("AEGIS_carver_test", ".raw");
            tempRaw.deleteOnExit();

            // 128 KB raw disk image with embedded JPEG evidence
            byte[] disk = new byte[128 * 1024];
            int jpegOffset = 10 * 512;
            byte[] jpegHeader = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
            byte[] jpegFooter = new byte[]{(byte) 0xFF, (byte) 0xD9};
            System.arraycopy(jpegHeader, 0, disk, jpegOffset, jpegHeader.length);
            System.arraycopy("EVIDENCE_TEST_STRING".getBytes(), 0, disk, jpegOffset + 20, 20);
            System.arraycopy(jpegFooter, 0, disk, jpegOffset + 400, jpegFooter.length);

            try (FileOutputStream fos = new FileOutputStream(tempRaw)) {
                fos.write(disk);
            }

            File outDir = new File(tempRaw.getParentFile(), "carver_test_out_" + System.currentTimeMillis());
            outDir.mkdirs();

            List<CarvedFile> results = FileCarver.carveDevice(tempRaw.getAbsolutePath(), outDir);
            check("Carver extracted embedded file", results != null && !results.isEmpty());
            if (results != null && !results.isEmpty()) {
                CarvedFile cf = results.get(0);
                check("Carved file is JPEG", ".jpg".equalsIgnoreCase(cf.getExtension()));
                check("Carved file starting LBA matches sector 10", cf.getStartLba() == 10L);
                check("Carved file has valid SHA-256", cf.getSha256Hash() != null && !cf.getSha256Hash().equals("UNKNOWN"));
            }

            // Cleanup test directory
            if (outDir.exists()) {
                File[] files = outDir.listFiles();
                if (files != null) for (File f : files) f.delete();
                outDir.delete();
            }

        } catch (Exception e) {
            check("Carver test encountered exception: " + e.getMessage(), false);
        }
    }
}

