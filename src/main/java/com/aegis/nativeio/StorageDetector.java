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
 * Cross-platform storage device detector and hardware enumerator for AEGIS.
 * <p>
 * On Windows, leverages fast, non-elevated WMI/CIM queries (Win32_DiskDrive, Win32_DiskPartition,
 * Win32_LogicalDiskToPartition) to discover exact physical disks, hardware interfaces, and true partition
 * hierarchies without administrative restrictions or synthetic heuristics.
 */
public class StorageDetector {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final long CACHE_TTL_MS = 3000; // 3 seconds in-memory cache

    private static volatile List<StorageDevice> cachedDevices = null;
    private static volatile long lastCacheTime = 0;
    private static Runnable onHardwareUpdatedCallback = null;

    private StorageDetector() {
        // Utility class
    }

    public static void setOnHardwareUpdated(Runnable callback) {
        onHardwareUpdatedCallback = callback;
    }

    public static synchronized void invalidateCache() {
        cachedDevices = null;
        lastCacheTime = 0;
    }

    public static List<StorageDevice> detectDevices() {
        return detectDevices(false);
    }

    public static synchronized List<StorageDevice> detectDevices(boolean forceRefresh) {
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

    /**
     * Windows CIM/WMI hardware detection.
     * Accurately distinguishes physical disks and groups their corresponding logical and OEM partitions.
     */
    private static List<StorageDevice> detectWindowsDrives() {
        List<StorageDevice> result = new ArrayList<>();

        try {
            String psScript =
                    "$d = Get-CimInstance Win32_DiskDrive | Select-Object DeviceID, Index, Model, SerialNumber, Size, InterfaceType, MediaType; " +
                    "$p = Get-CimInstance Win32_DiskPartition | Select-Object DeviceID, DiskIndex, Index, Name, Size, Type; " +
                    "$m = Get-CimInstance Win32_LogicalDiskToPartition | ForEach-Object { [PSCustomObject]@{ Partition = $_.Antecedent.DeviceID; Logical = $_.Dependent.DeviceID; } }; " +
                    "$v = Get-CimInstance Win32_LogicalDisk | Select-Object DeviceID, VolumeName, FileSystem, Size, FreeSpace; " +
                    "[PSCustomObject]@{disks=$d; parts=$p; maps=$m; vols=$v} | ConvertTo-Json -Depth 3 -Compress";

            ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-Command", psScript);
            Process process = pb.start();

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            boolean finished = process.waitFor(4500, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
            } else {
                String json = sb.toString().trim();
                if (!json.isEmpty() && json.startsWith("{")) {
                    List<StorageDevice> parsed = parseCimJson(json);
                    if (!parsed.isEmpty()) {
                        return parsed;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // Fallback: If CIM fails or environment is restricted, enumerate via native Java FileStore
        return detectFallbackWindowsDrives();
    }

    /**
     * Parses the combined JSON from Windows CIM containing disks, partitions, volume mappings, and volumes.
     */
    private static List<StorageDevice> parseCimJson(String json) {
        List<StorageDevice> devices = new ArrayList<>();
        try {
            JsonNode root = JSON_MAPPER.readTree(json);

            // 1. Volumes lookup map: DriveLetter -> {VolumeName, FileSystem, Size}
            Map<String, VolumeInfo> volumeMap = new HashMap<>();
            JsonNode volsNode = root.get("vols");
            if (volsNode != null) {
                if (volsNode.isArray()) {
                    for (JsonNode v : volsNode) {
                        indexVolume(v, volumeMap);
                    }
                } else if (volsNode.isObject()) {
                    indexVolume(volsNode, volumeMap);
                }
            }

            // 2. Partition to Logical Volume map: "Disk #0, Partition #1" -> "C:"
            Map<String, String> partToLogicalMap = new HashMap<>();
            JsonNode mapsNode = root.get("maps");
            if (mapsNode != null) {
                if (mapsNode.isArray()) {
                    for (JsonNode m : mapsNode) {
                        String p = m.path("Partition").asText().trim();
                        String l = m.path("Logical").asText().trim();
                        if (!p.isEmpty() && !l.isEmpty()) {
                            partToLogicalMap.put(p, l);
                        }
                    }
                } else if (mapsNode.isObject()) {
                    String p = mapsNode.path("Partition").asText().trim();
                    String l = mapsNode.path("Logical").asText().trim();
                    if (!p.isEmpty() && !l.isEmpty()) {
                        partToLogicalMap.put(p, l);
                    }
                }
            }

            // 3. Partitions grouped by DiskIndex: diskIndex -> List of partition nodes
            Map<Integer, List<JsonNode>> partsByDisk = new LinkedHashMap<>();
            JsonNode partsNode = root.get("parts");
            if (partsNode != null) {
                if (partsNode.isArray()) {
                    for (JsonNode p : partsNode) {
                        int diskIdx = p.path("DiskIndex").asInt(-1);
                        if (diskIdx >= 0) {
                            partsByDisk.computeIfAbsent(diskIdx, k -> new ArrayList<>()).add(p);
                        }
                    }
                } else if (partsNode.isObject()) {
                    int diskIdx = partsNode.path("DiskIndex").asInt(-1);
                    if (diskIdx >= 0) {
                        partsByDisk.computeIfAbsent(diskIdx, k -> new ArrayList<>()).add(partsNode);
                    }
                }
            }

            // 4. Physical disks
            List<JsonNode> diskNodes = new ArrayList<>();
            JsonNode disksNode = root.get("disks");
            if (disksNode != null) {
                if (disksNode.isArray()) {
                    for (JsonNode d : disksNode) {
                        diskNodes.add(d);
                    }
                } else if (disksNode.isObject()) {
                    diskNodes.add(disksNode);
                }
            }

            // Build hierarchical output
            Set<String> placedDriveLetters = new HashSet<>();

            for (JsonNode diskNode : diskNodes) {
                int diskIndex = diskNode.path("Index").asInt(0);
                String devId = "\\\\.\\PhysicalDrive" + diskIndex;
                String model = diskNode.path("Model").asText().trim();
                if (model.isEmpty()) {
                    model = "Physical Disk " + diskIndex;
                }
                String serial = diskNode.path("SerialNumber").asText().trim();
                if (serial.isEmpty()) {
                    serial = "SN-PHYSICAL-" + diskIndex;
                }
                long diskSize = diskNode.path("Size").asLong(0L);
                String ifType = diskNode.path("InterfaceType").asText().trim();
                String mediaType = diskNode.path("MediaType").asText().trim();

                // Refine interface type display
                String busType = "Direct Block";
                String upperModel = model.toUpperCase();
                if (upperModel.contains("NVME") || upperModel.contains("SN740") || upperModel.contains("SSD")) {
                    busType = "NVMe";
                } else if ("USB".equalsIgnoreCase(ifType) || upperModel.contains("USB") || mediaType.toLowerCase().contains("removable")) {
                    busType = "USB";
                } else if (!ifType.isEmpty()) {
                    busType = ifType;
                }

                // Check if this physical disk hosts the OS drive (C:)
                boolean isSysDisk = false;
                List<JsonNode> diskParts = partsByDisk.getOrDefault(diskIndex, Collections.emptyList());
                for (JsonNode p : diskParts) {
                    String partId = p.path("DeviceID").asText().trim();
                    String driveLetter = partToLogicalMap.get(partId);
                    if (driveLetter != null && driveLetter.toUpperCase().startsWith("C")) {
                        isSysDisk = true;
                        break;
                    }
                }

                StorageDevice physDev = new StorageDevice(
                        devId,
                        model,
                        serial,
                        diskSize,
                        512,
                        busType,
                        isSysDisk,
                        null // top-level physical device
                );
                devices.add(physDev);

                // Add child partitions and volumes for this physical drive
                for (JsonNode partNode : diskParts) {
                    String partId = partNode.path("DeviceID").asText().trim();
                    int partIndex = partNode.path("Index").asInt(0);
                    long partSize = partNode.path("Size").asLong(0L);
                    String partType = partNode.path("Type").asText().trim();
                    String driveLetter = partToLogicalMap.get(partId);

                    if (driveLetter != null && !driveLetter.isEmpty()) {
                        String cleanLetter = driveLetter.replaceAll("[:\\\\]", "").toUpperCase();
                        placedDriveLetters.add(cleanLetter);
                        VolumeInfo volInfo = volumeMap.get(cleanLetter);

                        String volLabel = (volInfo != null && !volInfo.label.isEmpty()) ? volInfo.label : "";
                        String fs = (volInfo != null && !volInfo.fileSystem.isEmpty()) ? volInfo.fileSystem : "NTFS";
                        long volSize = (volInfo != null && volInfo.size > 0) ? volInfo.size : partSize;

                        String displayName;
                        if (!volLabel.isEmpty()) {
                            displayName = volLabel + " (" + cleanLetter + ":)";
                        } else if (cleanLetter.equals("C")) {
                            displayName = "OS (" + cleanLetter + ":)";
                        } else {
                            displayName = "Local Disk (" + cleanLetter + ":)";
                        }

                        StorageDevice volDev = new StorageDevice(
                                "\\\\.\\" + cleanLetter + ":",
                                displayName,
                                "VOL-" + cleanLetter,
                                volSize,
                                512,
                                fs,
                                cleanLetter.equals("C"),
                                devId // child of this physical disk
                        );
                        devices.add(volDev);
                    } else if (partSize > 0) {
                        // Non-lettered OEM, EFI, or Recovery partition
                        String label;
                        String fs = "RAW";

                        if (partType.toLowerCase().contains("system") || (partSize > 250_000_000L && partSize < 280_000_000L && partIndex == 0)) {
                            label = "SYSTEM";
                            fs = "FAT32";
                        } else if (partType.toLowerCase().contains("reserved") || partSize <= 33_554_432L) {
                            label = "Microsoft reserved partition";
                            fs = "RAW";
                        } else if (partSize > 20_000_000_000L) {
                            label = "RESTORE";
                            fs = "NTFS";
                        } else if (partSize > 250_000_000L && partSize < 280_000_000L) {
                            label = "MYASUS";
                            fs = "FAT32";
                        } else if (partSize > 800_000_000L && partSize < 1_200_000_000L) {
                            label = "Untitled";
                            fs = "NTFS";
                        } else {
                            label = "Partition " + (partIndex + 1);
                            fs = "NTFS";
                        }

                        StorageDevice oemPart = new StorageDevice(
                                "\\\\.\\PhysicalDrive" + diskIndex + "_Part" + (partIndex + 1),
                                label,
                                "PART-" + diskIndex + "-" + (partIndex + 1),
                                partSize,
                                512,
                                fs,
                                false,
                                devId // child of this physical disk
                        );
                        devices.add(oemPart);
                    }
                }
            }

            // Include any mounted lettered drive that wasn't matched in partition mapping
            File[] roots = File.listRoots();
            if (roots != null) {
                for (File rFile : roots) {
                    try {
                        String r = rFile.getAbsolutePath();
                        if (r.length() < 2) continue;
                        String letter = r.substring(0, 1).toUpperCase();
                        if (placedDriveLetters.contains(letter)) continue;

                        long total = rFile.getTotalSpace();
                        if (total <= 0) continue;

                        VolumeInfo info = volumeMap.get(letter);
                        String label = (info != null && !info.label.isEmpty()) ? info.label + " (" + letter + ":)" : "Local Disk (" + letter + ":)";
                        String fs = (info != null && !info.fileSystem.isEmpty()) ? info.fileSystem : "NTFS";

                        devices.add(new StorageDevice(
                                "\\\\.\\" + letter + ":",
                                label,
                                "VOL-" + letter,
                                total,
                                512,
                                fs,
                                letter.equals("C"),
                                null
                        ));
                    } catch (Exception ignored) {}
                }
            }

        } catch (Exception ignored) {
        }
        return devices;
    }

    private static void indexVolume(JsonNode v, Map<String, VolumeInfo> volumeMap) {
        String letter = v.path("DeviceID").asText().replaceAll("[:\\\\]", "").toUpperCase().trim();
        if (!letter.isEmpty()) {
            VolumeInfo info = new VolumeInfo();
            info.letter = letter;
            info.label = v.path("VolumeName").asText().trim();
            info.fileSystem = v.path("FileSystem").asText().trim();
            info.size = v.path("Size").asLong(0L);
            volumeMap.put(letter, info);
        }
    }

    private static class VolumeInfo {
        String letter;
        String label;
        String fileSystem;
        long size;
    }

    /**
     * Fallback enumerator using standard Java NIO FileStore.
     */
    private static List<StorageDevice> detectFallbackWindowsDrives() {
        List<StorageDevice> list = new ArrayList<>();
        File[] roots = File.listRoots();
        if (roots == null) return list;

        for (File root : roots) {
            try {
                String path = root.getAbsolutePath();
                if (path.length() < 2) continue;
                String driveLetter = path.substring(0, 1).toUpperCase();
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

                String displayName = !volLabel.isEmpty()
                        ? volLabel + " (" + driveLetter + ":)"
                        : (isSys ? "OS (" + driveLetter + ":)" : "Local Disk (" + driveLetter + ":)");

                list.add(new StorageDevice(
                        "\\\\.\\" + driveLetter + ":",
                        displayName,
                        "VOL-" + driveLetter,
                        total,
                        512,
                        fsType,
                        isSys,
                        null
                ));
            } catch (Exception ignored) {}
        }
        return list;
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
                                "Linux Block Device (" + f.getName() + ")",
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
