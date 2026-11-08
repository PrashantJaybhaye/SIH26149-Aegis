# AEGIS: Automated Erasure & Guarded Information Suite

**Smart India Hackathon 2026 (SIH26149)**
* **Organization:** National Technical Research Organisation (NTRO)
* **Category:** Software
* **Theme:** Blockchain & Cybersecurity

---

## 📌 Executive Summary

**AEGIS** is a unified digital forensics and cybersecurity platform that integrates **Secure Hardware & Logical Data Sanitization** with **Advanced Forensic File Carving & Recovery**.

Existing tools force cybersecurity teams and law enforcement agencies to use separate, fragmented software for wiping (e.g., DBAN, Blancco) and recovery (e.g., Autopsy, Scalpel, PhotoRec). **AEGIS** unifies both capabilities into a single environment built specifically for the **National Technical Research Organisation (NTRO)** under **SIH 2026 (SIH26149)**.

---

## 🎯 Target Audience & Use Cases

* **Law Enforcement & Forensics Units:** Extracting deleted evidence from seized drives, USBs, and memory cards without relying on filesystem metadata.
* **Cybersecurity & IT Defense Teams:** Securely destroying sensitive data on decommissioned hardware to prevent unauthorized recovery.
* **Government & Military Agencies (NTRO):** Guaranteeing media sanitization compliance with court-verifiable audit reports and immutable blockchain ledgers.

---

## 🛠️ Technology Stack

| Component | Technology / Library | Description |
| :--- | :--- | :--- |
| **Language** | Java 17 / 21 | Modern JDK platform for cross-platform execution |
| **GUI Framework** | OpenJFX (JavaFX 21) | Cyber-forensic desktop dashboard with real-time entropy charting |
| **Build System** | Apache Maven | Project dependency management & build orchestration |
| **Native OS Access** | JNA (Java Native Access) | Win32 API & POSIX system calls for low-level block IOCTLs |
| **PDF Reporting** | Apache PDFBox 3.0.2 | Cryptographic PDF certificate generator for legal audits |
| **JSON Handling** | Jackson 2.17.0 | Structured device metadata & audit log serialization |
| **Cryptography** | Java `MessageDigest` + `SecureRandom` | SHA-256 evidence hashing & cryptographically secure wiping patterns |

---

## 🏗️ Solution Architecture & Module Structure

```
com.aegis/
├── Main.java                    ← Unified application entry point (GUI / CLI fallback)
├── ui/
│   ├── AegisApp.java            ← JavaFX Desktop Application & 4-Step Forensic Workflow
│   ├── AegisUiComponents.java   ← Vector Hardware Disks, Category Cards & Real-Time Waveform
│   ├── AegisDialogs.java        ← Standardized modal confirmations, safety notices & prompts
│   ├── ResizeHelper.java        ← Frameless window edge resizing & drag handlers
│   └── GuiLauncher.java         ← Standard classpath bootstrap launcher
├── blockchain/
│   └── AuditBlockchain.java     ← Module 5: SHA-256 Chained Cryptographic Audit Ledger
├── audit/
│   └── ReportGenerator.java     ← Module 4: PDF certificate generator (Sanitization & Recovery)
├── carver/
│   ├── FileCarver.java          ← Module 3: Raw sector file carving engine with live entropy
│   └── FileSignature.java       ← Forensic signatures (JPEG, PNG, MP4, AVI, MP3, WAV, PDF, ZIP, 7Z, DB)
├── eraser/
│   ├── DriveEraser.java         ← Module 1: Block-level drive sanitization
│   └── FileEraser.java          ← Module 2: Selective file + slack space wiping
├── model/
│   ├── CarvedFile.java          ← Data model for carved evidence
│   ├── SanitizationStandard.java ← Enum: NIST 800-88, DoD 5220.22-M, Gutmann, Zero-fill
│   └── StorageDevice.java       ← Data model for physical/logical drives
├── nativeio/
│   └── StorageDetector.java     ← Cross-platform drive detection (Win32/Linux)
└── util/
    ├── EntropyCalculator.java   ← Shannon entropy (bits/byte) calculator
    ├── FormatUtil.java          ← Unified human-readable size, hash, and count formatting
    └── HashUtil.java            ← Centralized SHA-256 hash utility
```

---

## ⚙️ Module-by-Module Technical Breakdown

### Module 1: Secure Drive Eraser (`DriveEraser.java`)
* **Block-Level Wiping:** Overwrites entire physical drives or disk images sequentially across LBAs.
* **Standards Supported:** NIST SP 800-88 Rev 1 (Clear & Purge), DoD 5220.22-M (3-pass), Gutmann Lite (7-pass), Zero-fill, Random-fill.
* **Real-Time Shannon Entropy:** Measures buffer entropy ($0.0$ bits = pure zero fill, $8.0$ bits = random) live during wiping and executes a verification pass to confirm zero residual readable data.
* **OS Drive Protection Safety Lock:** Hardware lock prevents users from accidentally selecting and overwriting the boot drive (e.g., `C:`).

### Module 2: Selective File & Slack Space Eraser (`FileEraser.java`)
* **Targeted File Destruction:** Overwrites specific sensitive files using multi-pass patterns before unlinking them from the file system.
* **Cluster Slack Space Wiping:** Scans unallocated bytes left at the tail end of allocated disk clusters (RAM slack & drive slack) and zeroes them out, preventing residual payload recovery.

### Module 3: Advanced Raw Sector File Carver (`FileCarver.java`)
* **Filesystem-Agnostic Extraction:** Recovers deleted files from raw unallocated disk sectors using magic byte header/trailer analysis without relying on MFT, FAT, or ext4 metadata tables.
* **Chunk Overlap Architecture:** Uses a 1 MB read buffer with a 4,096-byte overlap window to ensure signatures crossing buffer boundaries are never missed.
* **Signature Catalog (`FileSignature.java`):** Supports JPEG, PNG, MP4, AVI, MP3, WAV, PDF, ZIP, 7Z, and SQLite DB files.
* **False Positive Rejection:** Validates structural integrity (e.g., PDF trailer markers, JPEG EXIF/SOS headers) and calculates cryptographic SHA-256 hashes for chain of custody.

### Module 4: Forensic Audit PDF Generator (`ReportGenerator.java`)
* **Tamper-Evident Certificates:** Generates cryptographic PDF certificates documenting media serial numbers, wiping standards, pass counts, starting/ending timestamps, and pre/post-wipe Shannon entropy.
* **Court-Admissible Chain of Custody:** Includes digital verification hashes and investigator attestation sections.

### Module 5: Immutable Cryptographic Blockchain Ledger (`AuditBlockchain.java`)
* **Proof of Sanitization:** Every wipe or recovery operation is committed as an immutable block in a local SHA-256 chained Merkle ledger (`reports/aegis_audit_blockchain.json`).
* **Cryptographic Tamper Detection:** Verifies block hashes and previous-block links on startup to detect any post-incident tampering.

---

## 🚀 Setup & Execution Guide

### 📋 Prerequisites
* **Java 17 or Java 21 JDK** installed (e.g. Eclipse Adoptium Temurin or Oracle JDK).
* **Apache Maven 3.8+** (or IntelliJ IDEA, which bundles Maven automatically).
* **Administrator Privileges:** Recommended on Windows (right-click -> "Run as administrator") to allow raw physical sector/block IOCTL disk access (`\\.\PhysicalDriveX`).

---

### ⚡ Method 1: Single Unified Launcher (`run.bat`)

The simplest way to run AEGIS is using the included unified launcher:

```powershell
.\run.bat           # Prompts for GUI or CLI mode (defaults to GUI after timeout)
.\run.bat --gui     # Directly launches Desktop JavaFX GUI
.\run.bat --cli     # Directly launches Forensic Interactive CLI
```

> *The script features automatic UAC self-elevation (`-Verb RunAs`): when launched, Windows prompts for Administrator permission to unlock raw block IOCTL device access and auto-detects Maven and Java.*

---

### 💻 Method 2: Running via IntelliJ IDEA (Recommended for Development)

1. Open **IntelliJ IDEA**.
2. Select **File > Open...** and choose the `d:\Project\Forensix` folder.
3. Wait for IntelliJ to sync the Maven project dependencies (`pom.xml`).
4. Navigate to:  
   `src/main/java/com/aegis/Main.java`
5. Click the green **Run (▶)** arrow next to `public static void main(String[] args)` or press `Shift + F10`.
6. Use the built-in terminal/Run console to interact with the menu.

---

### 🖥️ Method 3: Running via Terminal / Command Prompt / PowerShell

Open Command Prompt or PowerShell in the project directory:

```powershell
# 1. Clean and compile the source code
mvn clean compile

# 2. Launch AEGIS GUI
mvn exec:java -Dexec.mainClass="com.aegis.ui.GuiLauncher"

# 3. Or launch AEGIS CLI
mvn exec:java -Dexec.mainClass="com.aegis.Main" -Dexec.args="--cli"
```

---

### 🧪 Automated Demonstration & Verification (Option 5)

To quickly test all core modules without risking real drives:
1. Launch the application and select **`5`** (End-to-End System Demonstration).
2. The benchmark engine will:
   - Generate a 10 MB mock raw disk image (`test_evidence.raw`) containing embedded hidden JPEG and PDF files.
   - Run the **File Carver** to extract the files into `carved_evidence/` and compute SHA-256 hashes.
   - Run the **Secure Drive Eraser** (NIST SP 800-88 Clear) and calculate live Shannon Entropy ($0.0000$ bits/byte).
   - Generate a tamper-evident court-admissible audit certificate PDF in `reports/`.

---

## 📄 Compliance & Governance
Designed for National Technical Research Organisation (NTRO) - SIH 2026.
Compliant with:
* **NIST SP 800-88 Rev 1** (Guidelines for Media Sanitization)
* **DoD 5220.22-M** (National Industrial Security Program Operating Manual)
* **ISO/IEC 27040** (Storage Security)
