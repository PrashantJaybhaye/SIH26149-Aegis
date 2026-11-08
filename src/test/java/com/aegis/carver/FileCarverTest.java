package com.aegis.carver;

import com.aegis.model.CarvedFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileCarverTest {

    @Test
    @DisplayName("FileSignature catalog should contain all essential forensic formats")
    void testStandardSignaturesCatalog() {
        List<FileSignature> signatures = FileSignature.getStandardForensicSignatures();
        assertNotNull(signatures);
        assertFalse(signatures.isEmpty());

        boolean hasJpeg = signatures.stream().anyMatch(s -> s.getExtension().equalsIgnoreCase(".jpg"));
        boolean hasPng = signatures.stream().anyMatch(s -> s.getExtension().equalsIgnoreCase(".png"));
        boolean hasPdf = signatures.stream().anyMatch(s -> s.getExtension().equalsIgnoreCase(".pdf"));
        boolean hasZip = signatures.stream().anyMatch(s -> s.getExtension().equalsIgnoreCase(".zip"));
        boolean hasMp4 = signatures.stream().anyMatch(s -> s.getExtension().equalsIgnoreCase(".mp4"));

        assertTrue(hasJpeg, "Must include JPEG signature");
        assertTrue(hasPng, "Must include PNG signature");
        assertTrue(hasPdf, "Must include PDF signature");
        assertTrue(hasZip, "Must include ZIP signature");
        assertTrue(hasMp4, "Must include MP4 signature");
    }

    @Test
    @DisplayName("FileCarver should accurately extract embedded evidence from synthetic raw image")
    void testCarveDeviceSyntheticDisk(@TempDir Path tempDir) throws IOException {
        // Create 256 KB synthetic raw disk image
        int diskSize = 256 * 1024;
        byte[] diskBytes = new byte[diskSize];

        // 1. Embed JPEG at sector 10 (offset 5120)
        int jpegOffset = 10 * 512;
        byte[] jpegHeader = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
        byte[] jpegFooter = new byte[]{(byte) 0xFF, (byte) 0xD9};
        System.arraycopy(jpegHeader, 0, diskBytes, jpegOffset, jpegHeader.length);
        System.arraycopy("SAMPLE_EVIDENCE_PAYLOAD".getBytes(), 0, diskBytes, jpegOffset + 10, 23);
        System.arraycopy(jpegFooter, 0, diskBytes, jpegOffset + 200, jpegFooter.length);

        // 2. Embed PDF at sector 50 (offset 25600)
        int pdfOffset = 50 * 512;
        byte[] pdfHeader = "%PDF-1.7\n1 0 obj\n<< /Title (Confidential Evidence) >>\nendobj\n".getBytes();
        byte[] pdfFooter = "%%EOF\n".getBytes();
        System.arraycopy(pdfHeader, 0, diskBytes, pdfOffset, pdfHeader.length);
        System.arraycopy(pdfFooter, 0, diskBytes, pdfOffset + 500, pdfFooter.length);

        File rawDiskFile = tempDir.resolve("test_evidence.raw").toFile();
        try (FileOutputStream fos = new FileOutputStream(rawDiskFile)) {
            fos.write(diskBytes);
        }

        File outputDir = tempDir.resolve("carved_out").toFile();
        List<CarvedFile> carved = FileCarver.carveDevice(rawDiskFile.getAbsolutePath(), outputDir);

        assertNotNull(carved);
        assertTrue(carved.size() >= 2, "Carver must recover both JPEG and PDF evidence items (found: " + carved.size() + ")");

        CarvedFile jpegCarved = carved.stream().filter(f -> f.getExtension().equalsIgnoreCase(".jpg")).findFirst().orElse(null);
        assertNotNull(jpegCarved, "Recovered JPEG file must exist");
        assertEquals(10L, jpegCarved.getStartLba());
        assertTrue(jpegCarved.isIntact());
        assertNotNull(jpegCarved.getSha256Hash());
        assertNotEquals("UNKNOWN", jpegCarved.getSha256Hash());

        CarvedFile pdfCarved = carved.stream().filter(f -> f.getExtension().equalsIgnoreCase(".pdf")).findFirst().orElse(null);
        assertNotNull(pdfCarved, "Recovered PDF file must exist");
        assertEquals(50L, pdfCarved.getStartLba());
        assertTrue(pdfCarved.isIntact());
    }

    @Test
    @DisplayName("Should normalize Windows drive letters to block device handles")
    void testNormalizeDevicePath() {
        assertEquals("\\\\.\\PhysicalDrive0", FileCarver.normalizeDevicePath("\\\\.\\PhysicalDrive0"));
        assertNull(FileCarver.normalizeDevicePath(null));
        assertEquals("", FileCarver.normalizeDevicePath(""));
    }
}

