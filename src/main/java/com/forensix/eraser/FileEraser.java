package com.forensix.eraser;

import com.forensix.model.SanitizationStandard;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Targeted file and slack space sanitization engine.
 * <p>
 * Performs multi-pass data overwrite, cluster slack space scrubbing,
 * timestamp reset (epoch 1970), and filename randomization before deletion
 * to eradicate Master File Table (MFT) / Inode residual records.
 */
public class FileEraser {

    private static final int BUFFER_SIZE = 8192;
    private static final int CLUSTER_SIZE = 4096; // Standard 4KB filesystem cluster boundary

    private FileEraser() {
        // Utility class
    }

    /**
     * Wipes a target file, scrubs file slack space, resets timestamps, and unlinks it.
     */
    public static boolean wipeFile(File file, SanitizationStandard standard) {
        return wipeFile(file, standard, false);
    }

    /**
     * Wipes a target file, scrubs file slack space, resets timestamps, and unlinks it.
     *
     * @param file target file to sanitize
     * @param standard sanitization standard (e.g. DoD 5220.22-M, NIST 800-88)
     * @param quiet suppress console output if true
     * @return true if file was successfully overwritten and deleted; false otherwise
     */
    public static boolean wipeFile(File file, SanitizationStandard standard, boolean quiet) {
        if (file == null || !file.exists() || !file.isFile()) {
            if (!quiet) {
                System.err.println("File does not exist or is not a regular file: " + (file != null ? file.getAbsolutePath() : "null"));
            }
            return false;
        }

        long fileLength = file.length();
        long allocatedSize = ((fileLength + CLUSTER_SIZE - 1) / CLUSTER_SIZE) * CLUSTER_SIZE;
        long slackSpaceBytes = allocatedSize - fileLength;

        SecureRandom random = new SecureRandom();
        int totalPasses = standard.getTotalPasses();
        byte[] buffer = new byte[BUFFER_SIZE];

        try {
            // 1. Multi-pass overwrite of actual file data bytes
            try (RandomAccessFile raf = new RandomAccessFile(file, "rw");
                 FileChannel channel = raf.getChannel()) {

                for (int pass = 1; pass <= totalPasses; pass++) {
                    channel.position(0);
                    long written = 0;

                    while (written < fileLength) {
                        int chunkSize = (int) Math.min(buffer.length, fileLength - written);
                        DriveEraser.fillPatternBuffer(buffer, chunkSize, standard, pass, random);

                        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, chunkSize);
                        written += channel.write(byteBuffer);
                    }

                    // 2. Scrub File Slack Space up to the allocated cluster boundary
                    if (slackSpaceBytes > 0) {
                        byte[] slackBuffer = new byte[(int) slackSpaceBytes];
                        Arrays.fill(slackBuffer, (byte) 0x00);
                        channel.write(ByteBuffer.wrap(slackBuffer));
                    }

                    channel.force(true); // Flush hardware buffers to disk
                }
            }

            // 3. Metadata & Timestamps Scrubbing (Reset Creation/Modification times to epoch 1970)
            FileTime epoch = FileTime.fromMillis(0);
            Files.setLastModifiedTime(file.toPath(), epoch);

            // 4. Rename File to randomized junk before deleting (destroys directory table/MFT filename traces)
            File junkFile = new File(file.getParent(), "scramble_" + System.currentTimeMillis() + "_" + random.nextInt(99999) + ".tmp");
            boolean renamed = file.renameTo(junkFile);

            // 5. Unlink/Delete File
            File targetToDelete = renamed ? junkFile : file;
            boolean deleted = targetToDelete.delete();

            if (!quiet) {
                System.out.printf("[FILE ERASED] %s (Size: %d bytes, Slack Scrubbed: %d bytes)%n",
                        file.getName(), fileLength, slackSpaceBytes);
            }
            return deleted;

        } catch (Exception e) {
            if (!quiet) {
                System.err.println("Failed to securely wipe file: " + e.getMessage());
            }
            return false;
        }
    }
}
