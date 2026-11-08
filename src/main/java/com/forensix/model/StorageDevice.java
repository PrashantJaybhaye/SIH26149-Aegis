package com.forensix.model;

public class StorageDevice {
    private final String devicePath;      // e.g., "\\.\PhysicalDrive1" or "/dev/sdb" or "evidence.raw"
    private final String model;           // e.g., "SanDisk Ultra USB 3.0"
    private final String serialNumber;    // e.g., "AA01020304"
    private final long totalBytes;        // Capacity in bytes
    private final int sectorSize;         // 512 or 4096 bytes
    private final String interfaceType;   // USB, NVMe, SATA
    private final boolean isSystemDrive;  // Safety lock flag
    private final String parentDevicePath; // Parent physical drive path if logical volume (e.g. "\\.\PhysicalDrive0")

    public StorageDevice(String devicePath, String model, String serialNumber, long totalBytes, int sectorSize, String interfaceType, boolean isSystemDrive) {
        this(devicePath, model, serialNumber, totalBytes, sectorSize, interfaceType, isSystemDrive, null);
    }

    public StorageDevice(String devicePath, String model, String serialNumber, long totalBytes, int sectorSize, String interfaceType, boolean isSystemDrive, String parentDevicePath) {
        this.devicePath = devicePath;
        this.model = model;
        this.serialNumber = serialNumber;
        this.sectorSize = sectorSize > 0 ? sectorSize : 512;
        this.totalBytes = totalBytes;
        this.interfaceType = interfaceType;
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

    public boolean isPhysical() {
        return devicePath != null && devicePath.startsWith("\\\\.\\Physical") && parentDevicePath == null && !devicePath.contains("#");
    }

    public boolean isLogicalVolume() {
        return !isPhysical();
    }

    public long getTotalSectors() {
        return totalBytes / sectorSize;
    }

    public String getFormattedSize() {
        double gb = totalBytes / (1024.0 * 1024.0 * 1024.0);
        if (gb >= 1.0) {
            return String.format("%.2f GB", gb);
        }
        double mb = totalBytes / (1024.0 * 1024.0);
        return String.format("%.2f MB", mb);
    }

    @Override
    public String toString() {
        String cleanModel = model != null && !model.isEmpty() ? model : devicePath;
        return cleanModel + " (" + getFormattedSize() + " • " + interfaceType + (isSystemDrive ? " - OS PROTECTED" : "") + ")";
    }
}
