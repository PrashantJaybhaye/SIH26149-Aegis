package com.aegis.nativeio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aegis.model.StorageDevice;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Cross-platform storage device detector and hardware enumerator.
 * <p>
 * Combines high-speed Java NIO {@link FileStore} inspection with OS kernel queries
 * (PowerShell/WMI on Windows, /dev block nodes on Linux) to discover physical hard drives,
 * NVMe SSDs, removable USB flash storage, and logical volume partitions.
 */
public class StorageDetector {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final long CACHE_TTL_MS = 2500; // 2.5 seconds cache TTL

    // In-memory cache for fast UI updates
    private static volatile List<StorageDevice> cachedDevices = null;
    private static volatile long lastCacheTime = 0;

    // Hardware physical drive cache across plug/unplug events
    private static final List<StorageDevice> cachedPhysicalDisks = new ArrayList<>();
    private static final Map<String, String> cachedVolumeToParentMap = new HashMap<>();
    private static volatile boolean isBackgroundEnriching = false;
    private static Runnable onHardwareUpdatedCallback = null;

    private StorageDetector() {
        // Static detector
    }

    public static void setOnHardwareUpdated(Runnable callback) {
        onHardwareUpdatedCallback = callback;
    }

    public static void invalidateCache() {
        cachedDevices = null;
        lastCacheTime = 0;
    }

    /**
     * Enumerates connected storage devices (both physical disks and partitions).
     */
    public static List<StorageDevice> detectDevices() {
        return detectDevices(false);
    }

    /**
     * Enumerates connected storage devices, optionally bypassing cache.
     */
    public static List<StorageDevice> detectDevices(boolean forceRefresh) {
        long now = System.currentTimeMillis();
        if (!forceRefresh && cachedDevices != null && (now - lastCacheTime < CACHE_TTL_MS)) {
            return new ArrayList<>(cachedDevices);
        }

        List<StorageDevice> devices = new ArrayList<>();
        String os = System.getProperty("os.name", "").toLowerCase();

        if (os.contains("win")) {
            devices.addAll(detectWindowsDrives());
        } else {
            devices.addAll(detectLinuxDrives());
        }

        cachedDevices = new ArrayList<>(devices);
        lastCacheTime = now;
        return devices;
    }

    private static void triggerBackgroundEnrichment() {
        if (isBackgroundEnriching) {
            return;
        }
        isBackgroundEnriching = true;
        Thread enrichmentThread = new Thread(() -> {
            try {
                List<StorageDevice> freshDisks = new ArrayList<>();
                Map<String, String> freshVolMap = new HashMap<>();
                runPowerShellDiskQuery(freshDisks, freshVolMap, 5500);

                if (!freshDisks.isEmpty()) {
                    synchronized (cachedPhysicalDisks) {
                        cachedPhysicalDisks.clear();
                        cachedPhysicalDisks.addAll(freshDisks);
                        cachedVolumeToParentMap.clear();
                        cachedVolumeToParentMap.putAll(freshVolMap);
                    }
                    invalidateCache();
                    if (onHardwareUpdatedCallback != null) {
                        onHardwareUpdatedCallback.run();
                    }
                }
            } catch (Exception ignored) {
            } finally {
                isBackgroundEnriching = false;
            }
        }, "AEGIS-StorageEnricher");
        enrichmentThread.setDaemon(true);
        enrichmentThread.start();
    }

    private static void runPowerShellDiskQuery(List<StorageDevice> physicalDisks,
                                               Map<String, String> volumeToParentMap,
                                               int timeoutMs) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "powershell", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-Command",
                    "$d = Get-Disk -ErrorAction SilentlyContinue | Select-Object Number, FriendlyName, SerialNumber, Size, SectorSize, BusType, IsSystem; " +
                    "$p = Get-Partition -ErrorAction SilentlyContinue | Where-Object DriveLetter | Select-Object DiskNumber, DriveLetter; " +
                    "@{disks=$d; parts=$p} | ConvertTo-Json -Compress"
            );
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
            } else {
                String json = output.toString().trim();
                if (!json.isEmpty() && !json.contains("Error") && !json.contains("Exception")) {
                    parsePowerShellCombinedOutput(json, physicalDisks, volumeToParentMap);
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Windows storage detector implementation.
     */
    private static List<StorageDevice> detectWindowsDrives() {
        List<StorageDevice> physicalDisks = new ArrayList<>();
        Map<String, String> volumeToParentMap = new HashMap<>();

        // Fast path: reuse cached physical disks for rapid response
        boolean hasCachedPhysicalDisks;
        synchronized (cachedPhysicalDisks) {
            hasCachedPhysicalDisks = !cachedPhysicalDisks.isEmpty();
            if (hasCachedPhysicalDisks) {
                physicalDisks.addAll(cachedPhysicalDisks);
                volumeToParentMap.putAll(cachedVolumeToParentMap);
            }
        }

        if (!hasCachedPhysicalDisks) {
            runPowerShellDiskQuery(physicalDisks, volumeToParentMap, 2200);
            if (!physicalDisks.isEmpty()) {
                synchronized (cachedPhysicalDisks) {
                    cachedPhysicalDisks.clear();
                    cachedPhysicalDisks.addAll(physicalDisks);
                    cachedVolumeToParentMap.clear();
                    cachedVolumeToParentMap.putAll(volumeToParentMap);
                }
            } else {
                triggerBackgroundEnrichment();
            }
        }

        // Fast Native Java FileStore Enumeration (~1ms)
        Map<String, StorageDevice> detectedVolumes = new LinkedHashMap<>();
        File[] roots = File.listRoots();
        if (roots != null) {
            for (File root : roots) {
                try {
                    String rootPath = root.getAbsolutePath();
                    if (rootPath.length() < 2) continue;
                    String driveLetter = rootPath.substring(0, 1).toUpperCase();
                    long total = root.getTotalSpace();
                    if (total <= 0) continue;

                    boolean isSys = driveLetter.equals("C");
                    String fsType = "NTFS";
                    String volLabel = "";

                    try {
                        FileStore store = Files.getFileStore(root.toPath());
                        String name = store.name();
                        if (name != null && !name.trim().isEmpty()) {
                            volLabel = name.trim();
                        }
                        String type = store.type();
                        if (type != null && !type.trim().isEmpty()) {
                            fsType = type.trim().toUpperCase();
                        }
                    } catch (Exception ignored) {}

                    String displayName;
                    if (!volLabel.isEmpty()) {
                        displayName = volLabel + " (" + driveLetter + ":)";
                    } else if (isSys) {
                        displayName = "OS (" + driveLetter + ":)";
                    } else {
                        displayName = "Local Disk (" + driveLetter + ":)";
                    }

                    String rawDevicePath = "\\\\.\\" + driveLetter + ":";
                    String parentPath = volumeToParentMap.get(driveLetter);

                    StorageDevice volDev = new StorageDevice(
                            rawDevicePath,
                            displayName,
                            "VOL-" + driveLetter,
                            total,
                            512,
                            fsType,
                            isSys,
                            parentPath
                    );

                    detectedVolumes.put(driveLetter, volDev);
                } catch (Exception ignored) {}
            }
        }

        // Association heuristic if PowerShell partition mapping was incomplete
        for (Map.Entry<String, StorageDevice> entry : detectedVolumes.entrySet()) {
            String letter = entry.getKey();
            StorageDevice vol = entry.getValue();

            if (vol.getParentDevicePath() == null && !physicalDisks.isEmpty()) {
                String resolvedParent = null;

                if (vol.isSystemDrive() || "C".equalsIgnoreCase(letter)) {
                    for (StorageDevice pd : physicalDisks) {
                        if (pd.isSystemDrive()) {
                            resolvedParent = pd.getDevicePath();
                            break;
                        }
                    }
                    if (resolvedParent == null) {
                        resolvedParent = physicalDisks.get(0).getDevicePath();
                    }
                } else if ("FAT32".equalsIgnoreCase(vol.getInterfaceType()) || "EXFAT".equalsIgnoreCase(vol.getInterfaceType())) {
                    for (StorageDevice pd : physicalDisks) {
                        if ("USB".equalsIgnoreCase(pd.getInterfaceType())) {
                            resolvedParent = pd.getDevicePath();
                            break;
                        }
                    }
                } else {
                    for (StorageDevice pd : physicalDisks) {
                        if (Math.abs(pd.getTotalBytes() - vol.getTotalBytes()) < 2L * 1024 * 1024 * 1024) {
                            resolvedParent = pd.getDevicePath();
                            break;
                        }
                    }
                    if (resolvedParent == null && !physicalDisks.isEmpty()) {
                        resolvedParent = physicalDisks.get(0).getDevicePath();
                    }
                }

                if (resolvedParent != null) {
                    detectedVolumes.put(letter, new StorageDevice(
                            vol.getDevicePath(),
                            vol.getModel(),
                            vol.getSerialNumber(),
                            vol.getTotalBytes(),
                            vol.getSectorSize(),
                            vol.getInterfaceType(),
                            vol.isSystemDrive(),
                            resolvedParent
                    ));
                }
            }
        }

        // If physicalDisks is empty (e.g. non-elevated permissions), synthesize clean parents
        if (physicalDisks.isEmpty() && !detectedVolumes.isEmpty()) {
            synthesizeDrivesFromVolumes(detectedVolumes, physicalDisks);
        }

        // Build Hierarchical Topology Ordering: Physical Hardware Drive -> Followed by Child Partitions
        List<StorageDevice> orderedList = new ArrayList<>();
        Set<String> placedVolumes = new HashSet<>();

        if (!physicalDisks.isEmpty()) {
            for (StorageDevice phys : physicalDisks) {
                long physBytes = phys.getTotalBytes();
                if (physBytes <= 0) {
                    long childSum = 0;
                    for (StorageDevice vol : detectedVolumes.values()) {
                        if (vol.getParentDevicePath() != null && vol.getParentDevicePath().equalsIgnoreCase(phys.getDevicePath())) {
                            childSum += vol.getTotalBytes();
                        }
                    }
                    if (childSum > 0) {
                        phys = new StorageDevice(
                                phys.getDevicePath(),
                                phys.getModel(),
                                phys.getSerialNumber(),
                                childSum,
                                phys.getSectorSize(),
                                phys.getInterfaceType(),
                                phys.isSystemDrive(),
                                null
                        );
                    }
                }

                List<StorageDevice> children = new ArrayList<>();
                long totalChildrenBytes = 0;
                for (Map.Entry<String, StorageDevice> entry : detectedVolumes.entrySet()) {
                    StorageDevice vol = entry.getValue();
                    if (vol.getParentDevicePath() != null && vol.getParentDevicePath().equalsIgnoreCase(phys.getDevicePath())) {
                        children.add(vol);
                        placedVolumes.add(entry.getKey());
                        totalChildrenBytes += vol.getTotalBytes();
                    }
                }

                boolean isUsb = "USB".equalsIgnoreCase(phys.getInterfaceType()) ||
                        (phys.getModel() != null && phys.getModel().toUpperCase().contains("USB"));
                if (isUsb && children.isEmpty()) {
                    continue; // Skip unplugged USB hardware device
                }

                orderedList.add(phys);
                orderedList.addAll(children);

                // Add unallocated space entry if significant space exists between physical capacity and partitions
                long unallocBytes = physBytes - totalChildrenBytes;
                if (unallocBytes >= 100L * 1024L * 1024L) {
                    String cleanParentId = phys.getDevicePath().replaceAll("[^a-zA-Z0-9]", "_");
                    orderedList.add(new StorageDevice(
                            "\\\\.\\" + cleanParentId + "_UNALLOC",
                            "Unallocated Space",
                            "RAW-UNALLOC",
                            unallocBytes,
                            phys.getSectorSize(),
                            "RAW",
                            false,
                            phys.getDevicePath()
                    ));
                }
            }
        }

        // Add any remaining unplaced volumes
        for (Map.Entry<String, StorageDevice> entry : detectedVolumes.entrySet()) {
            if (!placedVolumes.contains(entry.getKey())) {
                orderedList.add(entry.getValue());
            }
        }

        return orderedList;
    }

    private static void synthesizeDrivesFromVolumes(Map<String, StorageDevice> volumes, List<StorageDevice> physicalList) {
        long fixedSize = 0;
        long usbSize = 0;
        boolean hasUsb = false;

        for (StorageDevice v : volumes.values()) {
            if ("FAT32".equalsIgnoreCase(v.getInterfaceType()) || "EXFAT".equalsIgnoreCase(v.getInterfaceType())) {
                usbSize += v.getTotalBytes();
                hasUsb = true;
            } else {
                fixedSize += v.getTotalBytes();
            }
        }

        if (fixedSize > 0) {
            String phys0Path = "\\\\.\\PhysicalDrive0";
            StorageDevice phys0 = new StorageDevice(
                    phys0Path,
                    "Internal System Storage (Disk 0)",
                    "PHYSICAL-DISK-0",
                    fixedSize,
                    512,
                    "Fixed Disk",
                    true,
                    null
            );
            physicalList.add(phys0);

            for (Map.Entry<String, StorageDevice> e : volumes.entrySet()) {
                StorageDevice v = e.getValue();
                if (!("FAT32".equalsIgnoreCase(v.getInterfaceType()) || "EXFAT".equalsIgnoreCase(v.getInterfaceType()))) {
                    volumes.put(e.getKey(), new StorageDevice(
                            v.getDevicePath(), v.getModel(), v.getSerialNumber(), v.getTotalBytes(), v.getSectorSize(),
                            v.getInterfaceType(), v.isSystemDrive(), phys0Path
                    ));
                }
            }
        }

        if (hasUsb) {
            String phys1Path = "\\\\.\\PhysicalDrive1";
            StorageDevice phys1 = new StorageDevice(
                    phys1Path,
                    "Removable USB Storage (Disk 1)",
                    "REMOVABLE-DISK-1",
                    usbSize,
                    512,
                    "USB",
                    false,
                    null
            );
            physicalList.add(phys1);

            for (Map.Entry<String, StorageDevice> e : volumes.entrySet()) {
                StorageDevice v = e.getValue();
                if ("FAT32".equalsIgnoreCase(v.getInterfaceType()) || "EXFAT".equalsIgnoreCase(v.getInterfaceType())) {
                    volumes.put(e.getKey(), new StorageDevice(
                            v.getDevicePath(), v.getModel(), v.getSerialNumber(), v.getTotalBytes(), v.getSectorSize(),
                            v.getInterfaceType(), v.isSystemDrive(), phys1Path
                    ));
                }
            }
        }
    }

    /**
     * Parses the JSON output from PowerShell containing disks and partition mappings using Jackson.
     */
    private static void parsePowerShellCombinedOutput(String json,
                                                     List<StorageDevice> physicalDisks,
                                                     Map<String, String> volumeToParentMap) {
        try {
            JsonNode rootNode = JSON_MAPPER.readTree(json);

            // 1. Parse partitions: parts
            JsonNode partsNode = rootNode.get("parts");
            if (partsNode != null) {
                if (partsNode.isArray()) {
                    for (JsonNode part : partsNode) {
                        mapPartitionNode(part, volumeToParentMap);
                    }
                } else if (partsNode.isObject()) {
                    mapPartitionNode(partsNode, volumeToParentMap);
                }
            }

            // 2. Parse physical disks: disks
            JsonNode disksNode = rootNode.get("disks");
            if (disksNode != null) {
                if (disksNode.isArray()) {
                    for (JsonNode disk : disksNode) {
                        parseDiskNode(disk, physicalDisks);
                    }
                } else if (disksNode.isObject()) {
                    parseDiskNode(disksNode, physicalDisks);
                }
            }
        } catch (Exception ignored) {}
    }

    private static void mapPartitionNode(JsonNode partNode, Map<String, String> volumeToParentMap) {
        int diskNum = partNode.path("DiskNumber").asInt(-1);
        String driveLetter = partNode.path("DriveLetter").asText("").trim();
        if (diskNum >= 0 && !driveLetter.isEmpty()) {
            volumeToParentMap.put(driveLetter.toUpperCase(), "\\\\.\\PhysicalDrive" + diskNum);
        }
    }

    private static void parseDiskNode(JsonNode diskNode, List<StorageDevice> physicalDisks) {
        int number = diskNode.path("Number").asInt(0);
        String name = diskNode.path("FriendlyName").asText("Physical Disk " + number);
        String serial = diskNode.path("SerialNumber").asText("SN-PHYSICAL-" + number);
        long size = diskNode.path("Size").asLong(0L);
        int sectorSize = diskNode.path("SectorSize").asInt(512);
        String bus = diskNode.path("BusType").asText("SATA/USB");
        boolean isSystem = diskNode.path("IsSystem").asBoolean(false);

        String devicePath = "\\\\.\\PhysicalDrive" + number;
        StorageDevice device = new StorageDevice(devicePath, name, serial, size, sectorSize, bus, isSystem, null);
        physicalDisks.add(device);
    }

    /**
     * Linux /dev block device enumeration.
     */
    private static List<StorageDevice> detectLinuxDrives() {
        List<StorageDevice> list = new ArrayList<>();
        try {
            File devDir = new File("/dev");
            if (devDir.exists() && devDir.isDirectory()) {
                File[] files = devDir.listFiles((dir, name) -> name.matches("^(sd[a-z]|nvme[0-9]+n[0-9]+)$"));
                if (files != null) {
                    for (File f : files) {
                        list.add(new StorageDevice(
                                f.getAbsolutePath(),
                                "Linux Block Storage (" + f.getName() + ")",
                                "DEV-" + f.getName(),
                                f.length(),
                                512,
                                "Direct Block IO",
                                f.getName().equals("sda")
                        ));
                    }
                }
            }
        } catch (Exception ignored) {}
        return list;
    }
}
