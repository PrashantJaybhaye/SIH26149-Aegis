package com.forensix.nativeio;

import com.forensix.model.StorageDevice;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class StorageDetector {

    /**
     * Enumerates physical storage devices connected to the host system.
     */
    public static List<StorageDevice> detectDevices() {
        List<StorageDevice> devices = new ArrayList<>();
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            devices.addAll(detectWindowsDrives());
        } else {
            devices.addAll(detectLinuxDrives());
        }

        return devices;
    }

    private static List<StorageDevice> detectWindowsDrives() {
        List<StorageDevice> list = new ArrayList<>();
        try {
            // Run PowerShell Get-Disk command to query physical disks reliably (requires Admin or CIM access)
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell", "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "Get-Disk | Select-Object Number, FriendlyName, SerialNumber, Size, SectorSize, BusType, IsSystem | ConvertTo-Json"
            );
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder jsonOutput = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonOutput.append(line);
            }
            process.waitFor();

            String json = jsonOutput.toString().trim();
            if (!json.isEmpty() && !json.contains("Error") && !json.contains("Exception")) {
                parsePowerShellDiskOutput(json, list);
            }
        } catch (Exception e) {
            System.err.println("PowerShell disk enumeration notice: " + e.getMessage());
        }

        // Always query logical drive roots to ensure all mounted volumes (USBs, external disks) are accessible
        File[] roots = File.listRoots();
        javax.swing.filechooser.FileSystemView fsv = javax.swing.filechooser.FileSystemView.getFileSystemView();
        for (File root : roots) {
            String rootPath = root.getAbsolutePath(); // e.g. "E:\" or "C:\"
            boolean isSys = rootPath.equalsIgnoreCase("C:\\");
            long total = root.getTotalSpace();
            if (total > 0) {
                String systemName = fsv.getSystemDisplayName(root);
                String driveLetter = rootPath.length() >= 2 ? rootPath.substring(0, 2) : rootPath;
                if (systemName == null || systemName.isEmpty()) {
                    systemName = "USB / Logical Volume (" + driveLetter + ")";
                }

                // Format raw Win32 volume path without trailing backslash: "\\.\E:"
                String rawDevicePath = "\\\\.\\" + driveLetter;

                // Check if this physical disk was already detected to avoid duplicates
                boolean exists = false;
                for (StorageDevice sd : list) {
                    if (sd.getDevicePath().equalsIgnoreCase(rawDevicePath)) {
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    String fsType = "NTFS";
                    try {
                        java.nio.file.FileStore store = java.nio.file.Files.getFileStore(root.toPath());
                        String type = store.type();
                        if (type != null && !type.trim().isEmpty()) {
                            fsType = type.trim().toUpperCase();
                        }
                    } catch (Exception ignored) {}

                    list.add(new StorageDevice(
                            rawDevicePath,
                            systemName,
                            "VOL-" + rootPath.charAt(0),
                            total,
                            512,
                            fsType,
                            isSys
                    ));
                }
            }
        }

        return list;
    }

    private static void parsePowerShellDiskOutput(String json, List<StorageDevice> list) {
        try {
            if (json.startsWith("[")) {
                String[] items = json.split("\\},\\s*\\{");
                for (String item : items) {
                    StorageDevice dev = parseDiskJsonItem(item);
                    if (dev != null) list.add(dev);
                }
            } else if (json.startsWith("{")) {
                StorageDevice dev = parseDiskJsonItem(json);
                if (dev != null) list.add(dev);
            }
        } catch (Exception e) {
            System.err.println("Error parsing PowerShell disk JSON: " + e.getMessage());
        }
    }

    private static StorageDevice parseDiskJsonItem(String json) {
        try {
            int number = extractJsonInt(json, "Number", 0);
            String name = extractJsonString(json, "FriendlyName", "Physical Disk");
            String serial = extractJsonString(json, "SerialNumber", "SN-PHYSICAL-" + number);
            long size = extractJsonLong(json, "Size", 0L);
            int sectorSize = extractJsonInt(json, "SectorSize", 512);
            String bus = extractJsonString(json, "BusType", "SATA/USB");
            boolean isSystem = extractJsonBool(json, "IsSystem");

            String devicePath = "\\\\.\\PhysicalDrive" + number;
            return new StorageDevice(devicePath, name, serial, size, sectorSize, bus, isSystem);
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractJsonString(String json, String key, String defaultVal) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return defaultVal;
        int start = json.indexOf(":", idx) + 1;
        int quoteStart = json.indexOf("\"", start);
        if (quoteStart == -1) return defaultVal;
        int quoteEnd = json.indexOf("\"", quoteStart + 1);
        if (quoteEnd == -1) return defaultVal;
        return json.substring(quoteStart + 1, quoteEnd).trim();
    }

    private static long extractJsonLong(String json, String key, long defaultVal) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return defaultVal;
        int start = json.indexOf(":", idx) + 1;
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (Character.isDigit(c)) sb.append(c);
            else if (!sb.isEmpty() && !Character.isWhitespace(c)) break;
        }
        return sb.isEmpty() ? defaultVal : Long.parseLong(sb.toString());
    }

    private static int extractJsonInt(String json, String key, int defaultVal) {
        return (int) extractJsonLong(json, key, defaultVal);
    }

    private static boolean extractJsonBool(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return false;
        int start = json.indexOf(":", idx) + 1;
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        if (end == -1) return false;
        String val = json.substring(start, end).trim();
        return Boolean.parseBoolean(val);
    }

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
        } catch (Exception e) {
            System.err.println("Error detecting Linux drives: " + e.getMessage());
        }
        return list;
    }
}
