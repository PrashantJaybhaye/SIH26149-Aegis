package com.forensix.blockchain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.forensix.util.FormatUtil;
import com.forensix.util.HashUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable Cryptographic Audit Ledger for FORENSIX.
 * <p>
 * Implements a SHA-256 hash-chained block ledger for every sanitization, file shredding,
 * and evidence carving operation. Ensures tamper-evident forensic chain of custody
 * and court admissibility.
 */
public class AuditBlockchain {

    private static final String PRIMARY_LEDGER_PATH = "reports/forensix_audit_blockchain.json";
    private static final String LEGACY_LEDGER_PATH = "reports/aegis_audit_blockchain.json";
    private static final String ZERO_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private static AuditBlockchain instance;

    private List<Block> chain;
    private final ObjectMapper mapper;
    private File activeLedgerFile;

    /**
     * An individual block within the forensic audit ledger.
     */
    public static class Block {
        private int index;
        private String timestamp;
        private String action;          // e.g. "DATA_SANITIZATION", "EVIDENCE_CARVING", "FILE_SHREDDING"
        private String targetDevice;    // e.g. "SanDisk 3.2Gen1 (\\.\PhysicalDrive1)"
        private String standardOrMethod;// e.g. "NIST SP 800-88 Rev 1 (Clear)", "Raw Sector Carving"
        private String evidenceHash;    // SHA-256 hash of extracted evidence or post-wipe verification
        private String details;         // Descriptive metadata (e.g. "14 files recovered (32.00 MB)")
        private String previousHash;
        private String hash;
        private long nonce;

        public Block() {
            // Default constructor required for Jackson JSON deserialization
        }

        public Block(int index, String timestamp, String action, String targetDevice,
                     String standardOrMethod, String evidenceHash, String details, String previousHash) {
            this.index = index;
            this.timestamp = timestamp;
            this.action = action;
            this.targetDevice = targetDevice;
            this.standardOrMethod = standardOrMethod;
            this.evidenceHash = evidenceHash;
            this.details = details;
            this.previousHash = previousHash;
            this.nonce = 0;
            this.hash = calculateHash();
        }

        /**
         * Computes the SHA-256 hash over block contents.
         */
        public String calculateHash() {
            String payload = index + timestamp + action + targetDevice + standardOrMethod +
                    evidenceHash + details + previousHash + nonce;
            return HashUtil.calculateSHA256(payload);
        }

        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }

        public String getTargetDevice() { return targetDevice; }
        public void setTargetDevice(String targetDevice) { this.targetDevice = targetDevice; }

        public String getStandardOrMethod() { return standardOrMethod; }
        public void setStandardOrMethod(String standardOrMethod) { this.standardOrMethod = standardOrMethod; }

        public String getEvidenceHash() { return evidenceHash; }
        public void setEvidenceHash(String evidenceHash) { this.evidenceHash = evidenceHash; }

        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }

        public String getPreviousHash() { return previousHash; }
        public void setPreviousHash(String previousHash) { this.previousHash = previousHash; }

        public String getHash() { return hash; }
        public void setHash(String hash) { this.hash = hash; }

        public long getNonce() { return nonce; }
        public void setNonce(long nonce) { this.nonce = nonce; }

        @Override
        public String toString() {
            return String.format("Block #%d [%s] %s -> %s (Hash: %s)",
                    index, timestamp, action, targetDevice, FormatUtil.truncateHash(hash, 16));
        }
    }

    private AuditBlockchain() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.chain = new ArrayList<>();
        loadChainFromDisk();
    }

    public static synchronized AuditBlockchain getInstance() {
        if (instance == null) {
            instance = new AuditBlockchain();
        }
        return instance;
    }

    /**
     * Loads the blockchain from disk, inspecting both primary and legacy file paths.
     */
    private void loadChainFromDisk() {
        File primaryFile = new File(PRIMARY_LEDGER_PATH);
        File legacyFile = new File(LEGACY_LEDGER_PATH);

        if (primaryFile.exists() && primaryFile.length() > 0) {
            activeLedgerFile = primaryFile;
        } else if (legacyFile.exists() && legacyFile.length() > 0) {
            activeLedgerFile = legacyFile;
        } else {
            activeLedgerFile = primaryFile;
        }

        if (activeLedgerFile.exists() && activeLedgerFile.length() > 0) {
            try {
                this.chain = mapper.readValue(activeLedgerFile, new TypeReference<List<Block>>() {});
                if (this.chain == null || this.chain.isEmpty()) {
                    createGenesisBlock();
                }
            } catch (Exception e) {
                System.err.println("AuditBlockchain: Failed reading existing ledger (" + e.getMessage() + "), initializing fresh chain.");
                createGenesisBlock();
            }
        } else {
            createGenesisBlock();
        }
    }

    /**
     * Initializes the root Genesis block anchoring the forensic chain.
     */
    private void createGenesisBlock() {
        chain = new ArrayList<>();
        String time = FormatUtil.currentTimestamp();
        Block genesis = new Block(
                0,
                time,
                "GENESIS_INITIALIZATION",
                "FORENSIX Platform Root",
                "SHA-256 Chained Merkle Ledger",
                ZERO_HASH,
                "FORENSIX Forensic Audit Blockchain Initialized",
                ZERO_HASH
        );
        chain.add(genesis);
        saveChainToDisk();
    }

    /**
     * Appends a new immutable audit block to the ledger and persists it to disk.
     */
    public synchronized Block recordAuditBlock(String action, String targetDevice, String standardOrMethod,
                                               String evidenceHash, String details) {
        Block lastBlock = getLatestBlock();
        int newIndex = lastBlock.getIndex() + 1;
        String time = FormatUtil.currentTimestamp();
        String prevHash = lastBlock.getHash();

        Block newBlock = new Block(newIndex, time, action, targetDevice, standardOrMethod, evidenceHash, details, prevHash);
        chain.add(newBlock);
        saveChainToDisk();
        return newBlock;
    }

    /**
     * Retrieves the latest block on the chain.
     */
    public synchronized Block getLatestBlock() {
        if (chain.isEmpty()) {
            createGenesisBlock();
        }
        return chain.get(chain.size() - 1);
    }

    /**
     * Returns an unmodifiable snapshot of all blocks in the ledger.
     */
    public synchronized List<Block> getBlocks() {
        return Collections.unmodifiableList(new ArrayList<>(chain));
    }

    /**
     * Verifies the cryptographic integrity of the entire blockchain.
     * Validates both internal SHA-256 recalculation and previous-hash link pointers.
     *
     * @return true if all blocks are valid and unmodified; false if tampering is detected
     */
    public synchronized boolean isChainValid() {
        for (int i = 1; i < chain.size(); i++) {
            Block current = chain.get(i);
            Block previous = chain.get(i - 1);

            // 1. Verify block hash integrity
            if (!current.getHash().equals(current.calculateHash())) {
                return false;
            }

            // 2. Verify chain linkage pointer
            if (!current.getPreviousHash().equals(previous.getHash())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Persists the block chain to disk as formatted JSON.
     */
    private synchronized void saveChainToDisk() {
        try {
            if (activeLedgerFile == null) {
                activeLedgerFile = new File(PRIMARY_LEDGER_PATH);
            }
            File parentDir = activeLedgerFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            mapper.writeValue(activeLedgerFile, chain);
        } catch (Exception e) {
            System.err.println("AuditBlockchain: Failed to save ledger to disk: " + e.getMessage());
        }
    }
}
