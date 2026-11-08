package com.aegis.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModelTest {

    @Test
    @DisplayName("StorageDevice model should calculate sectors and distinguish physical vs logical drives")
    void testStorageDeviceModel() {
        StorageDevice physical = new StorageDevice(
                "\\\\.\\PhysicalDrive1", "SanDisk Ultra", "SN-12345",
                32L * 1024 * 1024 * 1024, 512, "USB", false, null
        );

        assertTrue(physical.isPhysical());
        assertFalse(physical.isLogicalVolume());
        assertEquals(32L * 1024 * 1024 * 1024 / 512, physical.getTotalSectors());
        assertEquals("32.00 GB", physical.getFormattedSize());
        assertFalse(physical.isSystemDrive());

        StorageDevice volume = new StorageDevice(
                "\\\\.\\E:", "USB Volume (E:)", "VOL-E",
                16L * 1024 * 1024 * 1024, 512, "FAT32", false, "\\\\.\\PhysicalDrive1"
        );
        assertFalse(volume.isPhysical());
        assertTrue(volume.isLogicalVolume());
        assertEquals("\\\\.\\PhysicalDrive1", volume.getParentDevicePath());
    }

    @Test
    @DisplayName("CarvedFile model should accurately preserve forensic metadata and calculate formatted size")
    void testCarvedFileModel() {
        CarvedFile file = new CarvedFile(
                "CARVE_0001", "JPEG Image", ".jpg", 100L, 51200L,
                204800L, 0.98, "a1b2c3d4e5f678901234567890abcdef", true
        );

        assertEquals("CARVE_0001", file.getId());
        assertEquals("JPEG Image", file.getFileType());
        assertEquals(".jpg", file.getExtension());
        assertEquals(100L, file.getStartLba());
        assertEquals(51200L, file.getStartByteOffset());
        assertEquals(204800L, file.getLengthBytes());
        assertTrue(file.isIntact());
        assertEquals("200.00 KB", file.getFormattedSize());
        assertNotNull(file.toString());
    }

    @Test
    @DisplayName("SanitizationStandard enum should define correct pass counts and descriptions")
    void testSanitizationStandardEnum() {
        assertEquals(1, SanitizationStandard.NIST_800_88_CLEAR.getTotalPasses());
        assertEquals(3, SanitizationStandard.NIST_800_88_PURGE.getTotalPasses());
        assertEquals(3, SanitizationStandard.DOD_5220_22_M.getTotalPasses());
        assertEquals(7, SanitizationStandard.GUTMANN_LITE.getTotalPasses());
        assertEquals(1, SanitizationStandard.ZERO_FILL.getTotalPasses());
        assertEquals(1, SanitizationStandard.RANDOM_FILL.getTotalPasses());

        assertNotNull(SanitizationStandard.DOD_5220_22_M.getDescription());
        assertNotNull(SanitizationStandard.DOD_5220_22_M.getDisplayName());
    }
}

