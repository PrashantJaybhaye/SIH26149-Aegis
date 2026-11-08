package com.aegis.model;

import com.aegis.util.FormatUtil;
import java.util.Objects;

/**
 * Represents a physical or logical storage device detected on the system.
 * Contains hardware metadata, capacity, sector size, interface type, and OS protection flags.
 */
public class StorageDevice {

    private final String devicePath;        // e.g., "\\.\PhysicalDrive1", "\\.\C:", "/dev/sdb", "evidence.raw"
    private final String model;             // e.g., "SanDisk Ultra USB 3.0"
    private final String serialNumber;      // Hardware serial number
    private final long totalBytes;          // Storage capacity in bytes
    private final int sectorSize;           // Logical sector size in bytes (typically 512 or 4096)
    private final String interfaceType;     // NVMe, SATA, USB, or filesystem type
    private final boolean isSystemDrive;    // Safety flag to prevent accidental destruction of OS drive
    private final String parentDevicePath;  // Parent physical disk path if this is a logical volume

    public StorageDevice(String devicePath, String model, String serialNumber,
                         long totalBytes, int sectorSize, String interfaceType, boolean isSystemDrive) {
        this(devicePath, model, serialNumber, totalBytes, sectorSize, interfaceType, isSystemDrive, null);
    }

    public StorageDevice(String devicePath, String model, String serialNumber,
                         long totalBytes, int sectorSize, String interfaceType,
                         boolean isSystemDrive, String parentDevicePath) {
        this.devicePath = devicePath;
        this.model = model;
        this.serialNumber = serialNumber;
        this.sectorSize = sectorSize > 0 ? sectorSize : 512;
        this.totalBytes = Math.max(0, totalBytes);
        this.interfaceType = interfaceType != null ? interfaceType : "Unknown";
        this.isSystemDrive = isSystemDrive;
        this.parentDevicePath = parentDevicePath;
    }

    public String getDevicePath() { return devicePath; }
    public String getModel() { return model; }
    public String getSerialNumber() { return serialNumber; }
    public long getTotalBytes() { return totalBytes; }
    public int getSectorSize() { return sectorSize; }
    public String getInterfaceType() { return interfaceType; }
    public boolean isSystemDrive() { return isSystemDrive; }
    public String getParentDevicePath() { return parentDevicePath; }

    /**
     * Checks if this device represents a top-level physical drive rather than a partition/volume.
     */
    public boolean isPhysical() {
        return devicePath != null && devicePath.startsWith("\\\\.\\Physical")
                && parentDevicePath == null && !devicePath.contains("#");
    }

    /**
     * Checks if this device represents a partition or mounted volume.
     */
    public boolean isLogicalVolume() {
        return !isPhysical();
    }

    /**
     * Calculates total sector count based on device capacity and sector size.
     */
    public long getTotalSectors() {
        return sectorSize > 0 ? (totalBytes / sectorSize) : 0;
    }

    /**
     * Formats device capacity into human-readable representation using FormatUtil.
     */
    public String getFormattedSize() {
        return FormatUtil.formatBytes(totalBytes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StorageDevice that = (StorageDevice) o;
        return Objects.equals(devicePath, that.devicePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(devicePath);
    }

    @Override
    public String toString() {
        String cleanModel = (model != null && !model.trim().isEmpty()) ? model : devicePath;
        return cleanModel + " (" + getFormattedSize() + " • " + interfaceType + (isSystemDrive ? " - OS PROTECTED" : "") + ")";
    }
}
