# AEGIS: Automated Erasure and Guarded Information Suite

**Smart India Hackathon 2026 (SIH26149)**
* **Organization:** National Technical Research Organisation (NTRO)
* **Category:** Software
* **Theme:** Blockchain & Cybersecurity

---

**To remove the directory from Git's index without deleting your local files:**

```bash

git rm -r --cached reports

```

## 📌 Executive Summary

**AEGIS** (**Automated Erasure and Guarded Information Suite**) is a unified digital forensics and cybersecurity platform that integrates **Secure Hardware & Logical Data Sanitization** with **Advanced Forensic File Carving & Recovery**.

Existing tools force cybersecurity teams and law enforcement agencies to use separate, fragmented software for wiping (e.g., DBAN, Blancco) and recovery (e.g., Autopsy, Scalpel, PhotoRec). **AEGIS** unifies both capabilities into a single environment built specifically for the **National Technical Research Organisation (NTRO)** under **SIH 2026 (SIH26149)**.

---

## 🎯 Target Audience & Use Cases

* **Law Enforcement & Forensics Units:** Extracting deleted evidence from seized drives, USBs, and memory cards without relying on filesystem metadata.
* **Cybersecurity & IT Defense Teams:** Securely destroying sensitive data on decommissioned hardware to prevent unauthorized recovery.
* **Government & Military Agencies (NTRO):** Guaranteeing media sanitization compliance with court-verifiable audit reports.

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
com.forensix/
├── Main.java                    ← Unified application entry point (GUI / CLI fallback)
├── ui/
│   ├── ForensixApp.java         ← JavaFX Desktop Application & Disk Drill 4-Step Flow
│   ├── DiskDrillComponents.java ← Vector Hardware Disk, Category Cards & Success Badges
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
    └── HashUtil.java            ← SHA-256 hash utility
```

---

## ⚙️ Module-by-Module Technical Breakdown

### Module 1: Secure Drive Eraser (`DriveEraser.java`)
* **Block-Level Wiping:** Overwrites entire physical drives or disk images sequentially across LBAs.
* **Standards Supported:** NIST SP 800-88 Rev 1 (Clear & Purge), DoD 5220.22-M (3-pass), Gutmann Lite (7-pass), Zero-fill, Random-fill.
* **Real-Time Shannon Entropy:** Measures buffer entropy ($0.0$ bits = pure zero fill, $8.0$ bits = random) live during wiping and executes a verification pass to confirm zero residual readable data.
* **System Drive Protection Lock:** Hardcoded protection guards prevent accidental selection/wiping of OS system drives.

### Module 2: Secure File & Slack Space Eraser (`FileEraser.java`)
* **Selective File Deletion:** Multi-pass overwrite of targeted files.
* **Slack Space Scrubbing:** Zeroes out residual leftover bytes in cluster boundaries (e.g., 500-byte file in a 4096-byte cluster leaves 3596 bytes of slack space).
* **Metadata & Timestamp Destruction:** Resets file modification/creation times to Epoch 1970 and scrambles file names prior to deletion to destroy MFT/Inode entry traces.

### Module 3: Advanced File Carving (`FileCarver.java` & `FileSignature.java`)
* **Filesystem-Less Recovery:** Extracts deleted files directly from raw disk sectors by scanning for magic bytes (`JPEG`, `PNG`, `PDF`, `ZIP/DOCX`).
* **Overlapping Sector Buffers:** 4096-byte buffer overlap prevents missing file signatures split across chunk boundaries.
* **Confidence Scoring & Hashing:** Evaluates structural validity (0–100% score) and computes a SHA-256 hash for forensic chain-of-custody.

### Module 4: Cryptographic Audit & Reporting (`ReportGenerator.java`)
* **Court-Ready PDF Certificates:** Exports PDF certificates detailing device model, serial number, sanitization standard used, pass counts, SHA-256 hashes, and entropy verification results.

---

## 🚀 Setup & Execution Guide

### 📋 Prerequisites
* **Java 17 or Java 21 JDK** installed (e.g. Eclipse Adoptium Temurin or Oracle JDK).
* **Apache Maven 3.8+** (or IntelliJ IDEA, which bundles Maven automatically).
* **Administrator Privileges:** Recommended on Windows (right-click -> "Run as administrator") to allow raw physical sector/block IOCTL disk access (`\\.\PhysicalDriveX`).

---

### ⚡ Method 1: One-Click Quick Run (Windows)

The simplest way to run the platform is using the included launcher scripts:

* **Desktop Application (JavaFX GUI):**  
  Double-click **`run.bat`** or **`run-gui.bat`**  
  *(Opens the modern dark-themed cybersecurity GUI with real-time graphs and tables).*
* **Terminal Dashboard (CLI Mode):**  
  Double-click **`run-cli.bat`**  
  *(Opens the interactive numbered terminal menu).*

> *Both scripts feature automatic UAC self-elevation (`-Verb RunAs`): when launched, Windows prompts for Administrator permission to unlock raw block IOCTL device access and auto-detects Maven.*

---

### 💻 Method 2: Running via IntelliJ IDEA (Recommended for Development)

1. Open **IntelliJ IDEA**.
2. Select **File > Open...** and choose the `d:\Project\Forensix` folder.
3. Wait for IntelliJ to sync the Maven project dependencies (`pom.xml`).
4. Navigate to:  
   `src/main/java/com/forensix/Main.java`
5. Click the green **Run (▶)** arrow next to `public static void main(String[] args)` or press `Shift + F10`.
6. Use the built-in terminal/Run console to interact with the menu.

---

### 🖥️ Method 3: Running via Terminal / Command Prompt / PowerShell

Open Command Prompt or PowerShell in the project directory:

```powershell
# 1. Clean and compile the source code
mvn clean compile

# 2. Launch the interactive AEGIS platform
mvn exec:java -Dexec.mainClass="com.forensix.Main"
```

> **Note if `mvn` is not recognized:**  
> If Maven is not added to your Windows system PATH, you can run directly using IntelliJ's bundled Maven:
> ```powershell
> & "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" compile exec:java
> ```

---

### 🧪 Automated Demonstration & Verification (Option 5)

To quickly test all 4 core modules without risking real drives:
1. Launch the application and type **`5`** (End-to-End System Demonstration).
2. The benchmark engine will:
   - Generate a 10 MB mock raw disk image (`test_evidence.raw`) containing embedded hidden JPEG and PDF files.
   - Run the **File Carver** to extract the files into `carved_evidence/` and compute SHA-256 hashes.
   - Run the **Secure Drive Eraser** (NIST SP 800-88 Clear) and calculate live Shannon Entropy ($0.0000$ bits/byte).
   - Generate a tamper-evident court-admissible audit certificate PDF in `reports/`.

---

## 🎮 Main Menu Options Explained

| Option | Name | Description |
| :---: | :--- | :--- |
| **1** | **Detect Storage Drives** | Queries the Win32/POSIX subsystem for physical disks, size, serials, and flags OS system drives. |
| **2** | **Secure Drive Eraser** | Block-level sanitization using NIST 800-88, DoD 5220.22-M, or Zero-fill with live Shannon entropy calculation. |
| **3** | **Selective File & Slack Eraser** | Multi-pass file shredding, slack space scrubbing, timestamp epoch zeroing, and MFT name scrambling. |
| **4** | **Advanced File Carving** | Scans raw sectors/images for magic bytes (JPEG, PNG, PDF, ZIP), validates file structure, and exports carved evidence. |
| **5** | **End-to-End Demonstration** | Automated test suite verifying carving, wiping, entropy validation, and PDF generation on synthetic media. |
| **6** | **Exit** | Safely exits the platform. |

---

## 📄 Compliance & Governance
Designed for National Technical Research Organisation (NTRO) - SIH 2026.
Compliant with:
* **NIST SP 800-88 Rev 1** (Guidelines for Media Sanitization)
* **DoD 5220.22-M** (National Industrial Security Program Operating Manual)
* **ISO/IEC 27040** (Storage Security)

