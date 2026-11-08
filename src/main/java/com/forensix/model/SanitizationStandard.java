package com.forensix.model;

/**
 * Data sanitization standards supported by FORENSIX.
 * Aligned with government and defense guidelines including NIST SP 800-88 Rev 1 and DoD 5220.22-M.
 */
public enum SanitizationStandard {

    NIST_800_88_CLEAR(
            "NIST SP 800-88 Rev 1 (Clear)",
            1,
            "Single-pass logical zero overwrite for non-classified media sanitization."
    ),
    NIST_800_88_PURGE(
            "NIST SP 800-88 Rev 1 (Purge)",
            3,
            "Multi-pass sanitization executing hardware purge and block overwriting."
    ),
    DOD_5220_22_M(
            "DoD 5220.22-M (3-Pass)",
            3,
            "DoD standard: Pass 1 writes 0x00, Pass 2 writes 0xFF, Pass 3 writes pseudo-random bytes followed by verification."
    ),
    GUTMANN_LITE(
            "Gutmann (Lite 7-Pass)",
            7,
            "7-pass pseudo-random magnetic polarization inversion overwrite."
    ),
    ZERO_FILL(
            "Single Pass Zero (Fast)",
            1,
            "High-speed single pass zero-fill (0x00)."
    ),
    RANDOM_FILL(
            "Single Pass Pseudo-Random",
            1,
            "High-speed single pass cryptographically secure random byte fill."
    );

    private final String displayName;
    private final int totalPasses;
    private final String description;

    SanitizationStandard(String displayName, int totalPasses, String description) {
        this.displayName = displayName;
        this.totalPasses = totalPasses;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public int getTotalPasses() { return totalPasses; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return displayName + " (" + totalPasses + (totalPasses == 1 ? " pass)" : " passes)");
    }
}
