package com.forensix.model;

import com.forensix.util.FormatUtil;
import java.util.Objects;

/**
 * Domain model representing a recovered file extracted through forensic signature carving.
 * Records the sector LBA, byte offset, size, structural integrity score, and cryptographic hash.
 */
public class CarvedFile {

    private final String id;               // e.g. "CARVE_0001"
    private final String fileType;         // e.g., "JPEG Image", "PDF Document", "MP3 Audio"
    private final String extension;        // e.g., ".jpg", ".pdf", ".mp3"
    private final long startLba;           // Logical Block Address where signature header was found
    private final long startByteOffset;    // Absolute byte offset on disk or raw image
    private final long lengthBytes;        // Extracted file length in bytes
    private final double confidenceScore;  // Confidence rating (0.0 to 1.0)
    private final String sha256Hash;       // SHA-256 hash of the extracted file payload
    private final boolean isIntact;        // True if both valid header and footer signatures were validated
    private String outputFilePath;        // Absolute path where carved file is written on disk

    public CarvedFile(String id, String fileType, String extension, long startLba, long startByteOffset,
                      long lengthBytes, double confidenceScore, String sha256Hash, boolean isIntact) {
        this.id = id;
        this.fileType = fileType;
        this.extension = extension;
        this.startLba = startLba;
        this.startByteOffset = startByteOffset;
        this.lengthBytes = lengthBytes;
        this.confidenceScore = confidenceScore;
        this.sha256Hash = sha256Hash;
        this.isIntact = isIntact;
    }

    public String getId() { return id; }
    public String getFileType() { return fileType; }
    public String getExtension() { return extension; }
    public long getStartLba() { return startLba; }
    public long getStartByteOffset() { return startByteOffset; }
    public long getLengthBytes() { return lengthBytes; }
    public double getConfidenceScore() { return confidenceScore; }
    public String getSha256Hash() { return sha256Hash; }
    public boolean isIntact() { return isIntact; }

    public String getOutputFilePath() { return outputFilePath; }
    public void setOutputFilePath(String outputFilePath) { this.outputFilePath = outputFilePath; }

    /**
     * Formats the extracted file size into human-readable format using FormatUtil.
     */
    public String getFormattedSize() {
        return FormatUtil.formatBytes(lengthBytes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CarvedFile that = (CarvedFile) o;
        return Objects.equals(id, that.id) && Objects.equals(sha256Hash, that.sha256Hash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sha256Hash);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s) @ LBA %d | Size: %s | Score: %.1f%% | Hash: %s",
                id, fileType, extension, startLba, getFormattedSize(),
                confidenceScore * 100, FormatUtil.truncateHash(sha256Hash, 12));
    }
}
