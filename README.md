# AEGIS: Active Erasure & Guarded Information Suite

[![Java](https://img.shields.io/badge/Java-17%20%7C%2021-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-blue?style=for-the-badge&logo=java&logoColor=white)](https://openjfx.io/)
[![Maven](https://img.shields.io/badge/Maven-3.8%2B-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![NIST](https://img.shields.io/badge/Compliance-NIST%20SP%20800--88%20Rev%201-0052CC?style=for-the-badge)](https://csrc.nist.gov/publications/detail/sp/800-88/rev-1/final)
[![DoD](https://img.shields.io/badge/Compliance-DoD%205220.22--M-green?style=for-the-badge)](https://www.esd.whs.mil/)
[![Tests](https://img.shields.io/badge/Tests-23%20Passing-brightgreen?style=for-the-badge)]()

**Smart India Hackathon 2026 (SIH26149)**
* **Organization:** National Technical Research Organisation (NTRO)
* **Category:** Software
* **Theme:** Blockchain & Cybersecurity

---

## 📌 Executive Summary

**AEGIS** (Active Erasure & Guarded Information Suite) is an enterprise-grade digital forensics and data sanitization platform that bridges the gap between **Certified High-Assurance Data Destruction** and **Filesystem-Agnostic Forensic Recovery**.

Historically, cyber defense teams, forensics laboratories, and intelligence agencies have been forced to deploy disconnected, single-purpose utilities—using one tool for data wiping (e.g., DBAN, Blancco) and completely separate toolsets for forensic file recovery (e.g., Autopsy, PhotoRec, Scalpel). **AEGIS** unifies both critical disciplines into a cohesive, high-performance forensic workstation designed specifically for the **National Technical Research Organisation (NTRO)** under **Smart India Hackathon 2026**.

AEGIS provides hardware-level drive detection, multi-standard disk sanitization, cluster slack space wiping, deep raw-sector magic-byte file carving, live Shannon entropy analytics, tamper-evident cryptographic PDF audit generation, and an immutable SHA-256 blockchain audit trail.

---

## 🎯 Target Audience & Strategic Use Cases

| Domain / User | Operational Use Case |
| :--- | :--- |
| **National Security & Intelligence (NTRO)** | High-assurance sanitization of retired storage media, preventing forensic reconstruction of top-secret assets while maintaining a tamper-evident blockchain ledger. |
| **Law Enforcement & Digital Forensics Units** | Extraction of deleted or hidden evidentiary artifacts (images, video, documents, databases) from seized or formatted drives without relying on compromised filesystem tables (MFT/FAT). |
| **Enterprise IT & Compliance Officers** | Certified media decommissioning meeting NIST SP 800-88 Rev 1 and DoD 5220.22-M standards, complete with court-admissible PDF audit certificates. |
| **Incident Response Teams** | Selective forensic shredding of malware payloads, compromised temporary files, and unallocated cluster slack space without disrupting host operations. |

---

## ⚡ Core Innovations & Features

### 1. 🖥️ Modern Cyber-Forensic Desktop GUI (JavaFX 21)
* **4-Step Forensic Workflow:** Designed around a clear operational lifecycle:
  1. **Storage Device Discovery:** Hierarchical physical disk and partition inspection with bus protocol identification (NVMe, SATA, USB) and boot drive safety locks.
  2. **Deep Sector Carving with Live Waveform:** Real-time Shannon entropy chart ($0.0$ to $8.0$ bits/byte), throughput monitor (MB/s), ETA timer, and live categorized counters.
  3. **Evidence Manifest & Inspection:** Interactive results grid with SHA-256 checksums, sector LBA locations, file category filters, and bulk/selective recovery export.
  4. **Forensic Audit & Blockchain Explorer:** One-click cryptographic PDF certificate generation and ledger verification.
* **Apple-Inspired Dark/Light Design System:** Fluid responsiveness, vector storage visualizers, custom scrollbars, and borderless window management with full multi-monitor support.

### 2. 🛡️ Certified Storage Sanitization (Block & Logical)
* **Hardware-Level Block Wiping:** Direct sector access via OS kernel IOCTL (`\\.\PhysicalDriveX` on Windows, `/dev/sdX` on Linux).
* **Multi-Standard Compliance:** NIST SP 800-88 Rev 1 (Clear & Purge), DoD 5220.22-M (3-Pass), Gutmann Lite (7-Pass), Fast Zero-Fill, and Pseudo-Random fills.
* **Real-Time Entropy Verification:** Verifies entropy drop to $0.0000$ bits/byte (zero-fill) or saturation to $7.99+$ bits/byte (random-fill) to mathematically prove complete data erasure.
* **System Drive Safety Guard:** Automatic hardware lock preventing accidental overwriting of host OS boot volumes (e.g., `C:`).

### 3. 🔍 Advanced Raw Sector Magic-Byte File Carver
* **Filesystem-Agnostic Extraction:** Recovers evidence directly from raw unallocated sectors without needing partition tables or Master File Table (MFT) records.
* **Chunk Overlap Architecture:** Uses a 1 MB primary read buffer combined with a 4,096-byte sliding overlap window to ensure signatures spanning buffer boundaries are never truncated or missed.
* **False Positive Filtering:** Enforces strict structural integrity checks (e.g., valid SOS/EOI markers for JPEG, trailer verification for PDF, catalog structures for SQLite DB).
* **Cryptographic Evidence Sealing:** Computes individual SHA-256 digests for every extracted file to maintain strict chain-of-custody integrity.

### 4. 🗄️ Selective File & Cluster Slack Space Shredder
* **Targeted File Obliteration:** Securely overwrites targeted files across multiple passes and truncates filesystem entries before deletion.
* **Slack Space Wiping:** Identifies RAM slack and drive slack (unallocated remnant bytes between end-of-file and cluster boundary) and zeroes them out, defeating forensic slack-space extraction techniques.

### 5. 📜 Cryptographic Audit Certificates & Blockchain Ledger
* **Tamper-Evident PDF Certificates:** Auto-generated via Apache PDFBox 3.0, featuring drive serial numbers, model metadata, wiping algorithm, pre/post-wipe Shannon entropy, cryptographic hash, and officer signature blocks.
* **Local SHA-256 Blockchain Ledger:** Every operation (wipe, carve, shred) is recorded as a cryptographically linked block in `reports/AEGIS_audit_blockchain.json` with hash validation on startup.

---

## 🛠️ Technology Stack

| Component | Technology | Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Runtime Platform** | Java JDK | 17 / 21 LTS | Core platform execution, multi-threading & NIO |
| **Desktop UI** | OpenJFX (JavaFX) | 21.0.5 | Reactive cyber-forensic interface & live entropy waveform |
| **Build & Orchestration** | Apache Maven | 3.8+ | Dependency management, compilation & test execution |
| **Native Kernel Access** | Java Native Access (JNA) | 5.14.0 | Low-level Win32 API & POSIX system calls for direct block IOCTL |
| **PDF Reporting Engine** | Apache PDFBox | 3.0.2 | Cryptographic forensic certificate generation |
| **Audit Serialization** | FasterXML Jackson | 2.17.0 | Structured device metadata and blockchain ledger serialization |
| **Cryptography** | Java Security Provider | Standard JDK | SHA-256 evidence hashing & `SecureRandom` PRNG patterns |
| **Unit & Integration Testing** | JUnit 5 | 5.10.2 | Automated unit and integration test verification |

---

## 🏗️ Architecture & Project Directory Structure

```
d:\Project\Aegis\
├── pom.xml                                ← Maven project definition and dependencies
├── run.bat                                ← Unified one-click launcher (GUI / CLI with UAC self-elevation)
├── README.md                              ← Architectural & operational documentation
├── reports/                               ← Generated PDF forensic certificates & blockchain ledger
│   └── AEGIS_audit_blockchain.json       ← Local SHA-256 chained audit ledger
├── src/
│   ├── main/
│   │   ├── java/com/aegis/
│   │   │   ├── Main.java                  ← Universal entry point (GUI / CLI switch)
│   │   │   ├── audit/
│   │   │   │   └── ReportGenerator.java   ← Module 4: PDF sanitization & carving certificate engine
│   │   │   ├── blockchain/
│   │   │   │   └── AuditBlockchain.java   ← Module 5: SHA-256 chained audit ledger & verification
│   │   │   ├── carver/
│   │   │   │   ├── FileCarver.java        ← Module 3: Raw sector carving engine with boundary overlap
│   │   │   │   └── FileSignature.java     ← Forensic signature catalog & magic byte specifications
│   │   │   ├── eraser/
│   │   │   │   ├── DriveEraser.java       ← Module 1: Block-level physical drive sanitizer
│   │   │   │   └── FileEraser.java        ← Module 2: Selective file & cluster slack space eraser
│   │   │   ├── model/
│   │   │   │   ├── CarvedFile.java        ← Evidence data model (LBA, hash, category, integrity)
│   │   │   │   ├── SanitizationStandard.java ← Supported wipe protocols (NIST, DoD, Gutmann)
│   │   │   │   └── StorageDevice.java     ← Physical/logical drive hierarchy & geometry model
│   │   │   ├── nativeio/
│   │   │   │   └── StorageDetector.java   ← Kernel drive enumeration (Win32 IOCTL & Linux sysfs)
│   │   │   ├── ui/
│   │   │   │   ├── AegisApp.java          ← Modern JavaFX 4-step forensic application
│   │   │   │   ├── AegisDialogs.java      ← Standardized modal alerts, confirmations & prompts
│   │   │   │   ├── AegisUiComponents.java ← Vector drive visualizers, cards & entropy waveform
│   │   │   │   ├── GuiLauncher.java       ← JavaFX runtime bootstrapper
│   │   │   │   └── ResizeHelper.java      ← Frameless draggable & resizable window handler
│   │   │   └── util/
│   │   │       ├── EntropyCalculator.java ← Shannon entropy (bits/byte) & peak block analyzer
│   │   │       ├── FormatUtil.java        ← Standardized byte, timestamp, and hash formatters
│   │   │       └── HashUtil.java          ← SHA-256 digest engine and hex conversion
│   │   └── resources/
│   │       ├── app-icon.svg               ← High-resolution vector application branding
│   │       └── style.css                  ← Cyber-forensic design system & typography stylesheet
│   └── test/
│       └── java/com/aegis/
│           ├── AegisTestSuite.java        ← Self-contained verification suite (zero-dependency test runner)
│           ├── blockchain/
│           │   └── AuditBlockchainTest.java ← Cryptographic ledger chain integrity tests
│           ├── carver/
│           │   └── FileCarverTest.java    ← Magic-byte extraction & overlap window tests
│           ├── model/
│           │   └── ModelTest.java         ← Storage device hierarchy & metadata mapping tests
│           └── util/
│               ├── EntropyCalculatorTest.java ← Shannon entropy computation tests
│               ├── FormatUtilTest.java    ← Byte and number formatting verification
│               └── HashUtilTest.java      ← SHA-256 digest validation tests
```

---

## 📋 Forensic File Signatures Catalog

The raw carving engine in `FileCarver.java` matches byte patterns across unallocated blocks using the signatures defined in `FileSignature.java`:

| Category | File Type | Extension | Header Magic (Hex) | Footer Magic (Hex) | Max Allocation |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **PICTURES** | JPEG Image | `.jpg` | `FF D8 FF` | `FF D9` | 20 MB |
| **PICTURES** | PNG Image | `.png` | `89 50 4E 47 0D 0A 1A 0A` | `49 45 4E 44 AE 42 60 82` | 30 MB |
| **VIDEOS** | MP4 Video | `.mp4` | `00 00 00 18 66 74 79 70` (`ftyp`) | N/A (Box Structure) | 150 MB |
| **VIDEOS** | AVI / RIFF | `.avi` | `52 49 46 46` (`RIFF`) | N/A (Chunk Size Header) | 100 MB |
| **AUDIO** | MP3 Audio | `.mp3` | `49 44 33` (`ID3`) | N/A (Frame Synch) | 25 MB |
| **AUDIO** | WAV Audio | `.wav` | `52 49 46 46 ... 57 41 56 45` | N/A (Data Chunk) | 40 MB |
| **DOCUMENTS**| PDF Document | `.pdf` | `25 50 44 46 2D` (`%PDF-`) | `25 25 45 4F 46` (`%%EOF`) | 50 MB |
| **ARCHIVES** | ZIP / OpenXML | `.zip` | `50 4B 03 04` (`PK..`) | `50 4B 05 06` | 100 MB |
| **ARCHIVES** | 7-Zip Archive | `.7z` | `37 7A BC AF 27 1C` (`7z..`) | N/A (End Header) | 100 MB |
| **OTHER** | SQLite Database | `.db` | `53 51 4C 69 74 65 20 66 ...` | N/A (Page Architecture) | 80 MB |

---

## 🛡️ Sanitization Standards Supported

AEGIS implements five industry and defense sanitization standards in `SanitizationStandard.java`:

| Standard | Passes | Pattern Sequence | Security Level & Compliance |
| :--- | :---: | :--- | :--- |
| **NIST SP 800-88 Rev 1 (Clear)** | 1 | Overwrite with `0x00` (logical zero fill) | Standard media sanitization against keyboard recovery |
| **NIST SP 800-88 Rev 1 (Purge)** | 3 | Pseudo-Random $\rightarrow$ Complement $\rightarrow$ Zero | Protects against lab/hardware recovery attacks |
| **DoD 5220.22-M** | 3 | Pass 1: `0x00` $\rightarrow$ Pass 2: `0xFF` $\rightarrow$ Pass 3: Pseudo-Random | U.S. National Industrial Security Operating Manual |
| **Gutmann Lite** | 7 | 7-Pass pseudo-random magnetic polarization inversion | Ultra-high assurance defense decommissioning |
| **Single Pass Zero / Random** | 1 | High-speed single pass `0x00` or cryptographic PRNG | Quick wipe for re-purposing storage media |

---

## 🚀 Installation & Execution Guide

### 📋 Prerequisites
1. **Java JDK 17 or JDK 21** installed (e.g., Eclipse Adoptium Temurin or Oracle JDK).
2. **Apache Maven 3.8+** installed (or IntelliJ IDEA, which includes Maven bundled).
3. **Administrator / Root Privileges:** Required on Windows (Right-Click $\rightarrow$ **Run as Administrator**) to allow physical block IOCTL drive access (`\\.\PhysicalDriveX`).

---

### ⚡ Method 1: Unified Launcher (`run.bat`) — Recommended for Windows

The included `run.bat` provides automatic UAC Administrator elevation, auto-detects installed JDK runtimes (JDK 17/21), locates Maven (including IntelliJ IDEA's bundled Maven), and launches AEGIS:

```powershell
# Open terminal or double-click run.bat in Administrator mode:
.\run.bat           # Prompts and defaults to Modern Desktop GUI
.\run.bat --gui     # Directly launches JavaFX Desktop GUI
.\run.bat --cli     # Directly launches Forensic Interactive CLI
```

---

### 💻 Method 2: Running via IntelliJ IDEA

1. Launch **IntelliJ IDEA**.
2. Select **File > Open...** and choose the `d:\Project\Aegis` directory.
3. Allow IntelliJ to import and sync Maven dependencies (`pom.xml`).
4. To launch the **Desktop GUI**:
   - Open `src/main/java/com/aegis/ui/GuiLauncher.java` and click **Run (▶)**.
5. To launch the **Forensic Terminal CLI**:
   - Open `src/main/java/com/aegis/Main.java`, set program arguments to `--cli`, and click **Run (▶)**.

---

### 🖥️ Method 3: Running via Command Line (Maven)

```powershell
# 1. Clean and compile the codebase
mvn clean compile

# 2. Launch AEGIS JavaFX Desktop GUI
mvn exec:java -Dexec.mainClass="com.aegis.ui.GuiLauncher"

# 3. Or launch AEGIS Forensic CLI Terminal
mvn exec:java -Dexec.mainClass="com.aegis.Main" -Dexec.args="--cli"
```

> **Note:** If `mvn` is not in your global system `PATH`, you can use IntelliJ's bundled Maven executable:  
> `& "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" compile exec:java`

---

## 🧪 Testing & Verification

### 1. Automated Test Suite (JUnit 5)

AEGIS features automated unit and integration tests covering all critical components:

```powershell
mvn test
```

**Verification Results:**
```
[INFO] Running com.aegis.blockchain.AuditBlockchainTest  -> 2 tests passed
[INFO] Running com.aegis.carver.FileCarverTest          -> 3 tests passed
[INFO] Running com.aegis.model.ModelTest                -> 4 tests passed
[INFO] Running com.aegis.util.EntropyCalculatorTest     -> 5 tests passed
[INFO] Running com.aegis.util.FormatUtilTest            -> 4 tests passed
[INFO] Running com.aegis.util.HashUtilTest              -> 5 tests passed
[INFO] -------------------------------------------------------
[INFO] Tests run: 23, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 2. Zero-Dependency Test Suite Runner

You can also run the self-contained verification suite directly from Java:

```powershell
mvn exec:java -Dexec.mainClass="com.aegis.AegisTestSuite"
```

### 3. End-to-End System Benchmark & Demonstration (Option 5)

To safely evaluate all capabilities without connecting physical test drives:
1. Launch CLI mode: `.\run.bat --cli`
2. Select menu option **`5`** (**Run End-to-End System Demonstration**).
3. The benchmark engine will:
   - Generate a 10 MB synthetic disk image (`test_evidence.raw`) with embedded forensic artifacts (JPEG and PDF).
   - Execute the **File Carver**, extracting files into `carved_evidence/` and computing SHA-256 hashes.
   - Execute the **Secure Drive Eraser** with NIST SP 800-88 Clear, verifying live entropy drop to $0.0000$ bits/byte.
   - Generate a cryptographic PDF audit certificate in `reports/`.
   - Record the operation to the immutable blockchain ledger in `reports/AEGIS_audit_blockchain.json`.

---

## ⚖️ Standards Compliance & Legal Admissibility

AEGIS adheres to international forensic standards to guarantee evidentiary admissibility:

* **NIST SP 800-88 Rev 1:** Guidelines for Media Sanitization (Clear & Purge methods).
* **DoD 5220.22-M:** National Industrial Security Program Operating Manual (NISPOM).
* **ISO/IEC 27040:2015:** Information Technology — Security Techniques — Storage Security.
* **Federal Rules of Evidence (FRE) Rule 901 / 902:** Tamper-evident hashing (SHA-256) and cryptographically verifiable chain-of-custody ledgers.

---

## 👥 Authors & Acknowledgments

* **Project:** AEGIS (Active Erasure & Guarded Information Suite)
* **Team:** Smart India Hackathon 2026 Team
* **Submitted to:** National Technical Research Organisation (NTRO)
* **Problem Statement ID:** SIH26149

---
*Developed with modern Java 17/21 LTS, OpenJFX, and JNA Native I/O.*
