package com.forensix.carver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a recognized forensic file signature used by the carving engine.
 * Maps file magic headers, optional footers, category taxonomy, and maximum plausible file sizes.
 */
public class FileSignature {

    private final String name;
    private final String extension;
    private final String category; // PICTURES, VIDEOS, AUDIO, DOCUMENTS, ARCHIVES, OTHER
    private final byte[] headerMagic;
    private final byte[] footerMagic;
    private final long maxSizeBytes;

    public FileSignature(String name, String extension, String category,
                         byte[] headerMagic, byte[] footerMagic, long maxSizeBytes) {
        this.name = name;
        this.extension = extension;
        this.category = category;
        this.headerMagic = headerMagic != null ? headerMagic.clone() : new byte[0];
        this.footerMagic = footerMagic != null ? footerMagic.clone() : null;
        this.maxSizeBytes = maxSizeBytes;
    }

    public String getName() { return name; }
    public String getExtension() { return extension; }
    public String getCategory() { return category; }
    public byte[] getHeaderMagic() { return headerMagic != null ? headerMagic.clone() : new byte[0]; }
    public byte[] getFooterMagic() { return footerMagic != null ? footerMagic.clone() : null; }
    public long getMaxSizeBytes() { return maxSizeBytes; }

    /**
     * Returns the standard catalog of forensic file signatures.
     */
    public static List<FileSignature> getStandardForensicSignatures() {
        List<FileSignature> signatures = new ArrayList<>();

        // 1. PICTURES: JPEG (SOI: FF D8 FF | EOI: FF D9)
        signatures.add(new FileSignature(
                "JPEG Image", ".jpg", "PICTURES",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
                new byte[]{(byte) 0xFF, (byte) 0xD9},
                20L * 1024 * 1024
        ));

        // 2. PICTURES: PNG (Header: 89 50 4E 47 0D 0A 1A 0A | Footer: IEND)
        signatures.add(new FileSignature(
                "PNG Image", ".png", "PICTURES",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
                new byte[]{0x49, 0x45, 0x4E, 0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82},
                30L * 1024 * 1024
        ));

        // 3. VIDEOS: MP4 / ISO Base Media (Magic 'ftyp' standard box)
        signatures.add(new FileSignature(
                "MP4 Video", ".mp4", "VIDEOS",
                new byte[]{0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70},
                null,
                150L * 1024 * 1024
        ));

        // 4. VIDEOS: AVI / RIFF (RIFF header)
        signatures.add(new FileSignature(
                "AVI Video", ".avi", "VIDEOS",
                new byte[]{0x52, 0x49, 0x46, 0x46},
                null,
                100L * 1024 * 1024
        ));

        // 5. AUDIO: MP3 (ID3v2 header: 49 44 33)
        signatures.add(new FileSignature(
                "MP3 Audio", ".mp3", "AUDIO",
                new byte[]{0x49, 0x44, 0x33},
                null,
                25L * 1024 * 1024
        ));

        // 6. AUDIO: WAV / RIFF
        signatures.add(new FileSignature(
                "WAV Audio", ".wav", "AUDIO",
                new byte[]{0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 0x57, 0x41, 0x56, 0x45},
                null,
                40L * 1024 * 1024
        ));

        // 7. DOCUMENTS: PDF (%PDF- .... %%EOF)
        signatures.add(new FileSignature(
                "PDF Document", ".pdf", "DOCUMENTS",
                new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D}, // %PDF-
                new byte[]{0x25, 0x25, 0x45, 0x4F, 0x46}, // %%EOF
                50L * 1024 * 1024
        ));

        // 8. ARCHIVES: ZIP / Office OpenXML (PK\x03\x04 .... PK\x05\x06)
        signatures.add(new FileSignature(
                "ZIP / Office Document", ".zip", "ARCHIVES",
                new byte[]{0x50, 0x4B, 0x03, 0x04},
                new byte[]{0x50, 0x4B, 0x05, 0x06},
                100L * 1024 * 1024
        ));

        // 9. ARCHIVES: 7-Zip Archive (7z\xBC\xAF'\x1C)
        signatures.add(new FileSignature(
                "7-Zip Archive", ".7z", "ARCHIVES",
                new byte[]{0x37, 0x7A, (byte) 0xBC, (byte) 0xAF, 0x27, 0x1C},
                null,
                100L * 1024 * 1024
        ));

        // 10. OTHER / FORENSIC: SQLite Database (SQLite format 3\000)
        signatures.add(new FileSignature(
                "SQLite Forensic DB", ".db", "OTHER",
                new byte[]{0x53, 0x51, 0x4C, 0x69, 0x74, 0x65, 0x20, 0x66, 0x6F, 0x72, 0x6D, 0x61, 0x74, 0x20, 0x33, 0x00},
                null,
                80L * 1024 * 1024
        ));

        return Collections.unmodifiableList(signatures);
    }
}
