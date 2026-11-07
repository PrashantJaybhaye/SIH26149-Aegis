package com.forensix.blockchain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Immutable Cryptographic Audit Ledger for Forensix (NTRO SIH26149).
 * Maintains a SHA-256 hash-chained block ledger for every sanitization and carving operation,
 * ensuring tamper-evident forensic chain of custody and court admissibility.
 */
public class AuditBlockchain {

    private static final String LEDGER_FILE_PATH = "reports/forensix_audit_blockchain.json";
    private static AuditBlockchain instance;

    private List<Block> chain;
    private final ObjectMapper mapper;

    public static class Block {
        private int index;
        private String timestamp;
        private String action;          // e.g. "DATA_SANITIZATION", "EVIDENCE_CARVING", "FILE_SHREDDING"
        private String targetDevice;    // e.g. "SanDisk 3.2Gen1 (\\\\.\\PhysicalDrive1)"
        private String standardOrMethod;// e.g. "NIST SP 800-88 Rev 1 (Clear)", "Raw Sector Carving"
        private String evidenceHash;    // SHA-256 of carved files or post-wipe verification hash
        private String details;         // e.g. "14 files recovered (32 MB)"
        private String previousHash;
        private String hash;
        private long nonce;

        public Block() {}

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

        public String calculateHash() {
            String dataToHash = index + timestamp + action + targetDevice + standardOrMethod +
                    evidenceHash + details + previousHash + nonce;
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hashBytes = digest.digest(dataToHash.getBytes(StandardCharsets.UTF_8));
                StringBuilder hex = new StringBuilder();
                for (byte b : hashBytes) {
                    hex.append(String.format("%02x", b));
                }
                return hex.toString();
            } catch (Exception e) {
                return "HASH_ERROR_" + System.currentTimeMillis();
            }
        }

        // Getters and Setters for Jackson serialization
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

    private void loadChainFromDisk() {
        File file = new File(LEDGER_FILE_PATH);
        if (file.exists() && file.length() > 0) {
            try {
                this.chain = mapper.readValue(file, new TypeReference<List<Block>>() {});
            } catch (Exception e) {
                System.err.println("AuditBlockchain: Error reading ledger file, creating new chain: " + e.getMessage());
                createGenesisBlock();
            }
        } else {
            createGenesisBlock();
        }
    }

    private void createGenesisBlock() {
        chain = new ArrayList<>();
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        Block genesis = new Block(0, time, "GENESIS_INITIALIZATION", "Forensix Platform Root",
                "SHA-256 Chained Merkle Ledger", "0000000000000000000000000000000000000000000000000000000000000000",
                "Forensix NTRO SIH26149 Genesis Block Initialized", "0000000000000000000000000000000000000000000000000000000000000000");
        chain.add(genesis);
        saveChainToDisk();
    }

    public synchronized Block recordAuditBlock(String action, String targetDevice, String standardOrMethod,
                                               String evidenceHash, String details) {
        Block lastBlock = getLatestBlock();
        int newIndex = lastBlock.getIndex() + 1;
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String prevHash = lastBlock.getHash();

        Block newBlock = new Block(newIndex, time, action, targetDevice, standardOrMethod, evidenceHash, details, prevHash);
        chain.add(newBlock);
        saveChainToDisk();
        return newBlock;
    }

    public synchronized Block getLatestBlock() {
        if (chain.isEmpty()) {
            createGenesisBlock();
        }
        return chain.get(chain.size() - 1);
    }

    public synchronized List<Block> getBlocks() {
        return new ArrayList<>(chain);
    }

    public synchronized boolean isChainValid() {
        for (int i = 1; i < chain.size(); i++) {
            Block current = chain.get(i);
            Block previous = chain.get(i - 1);

            // Verify current block's recalculated hash matches stored hash
            if (!current.getHash().equals(current.calculateHash())) {
                return false;
            }

            // Verify previous hash pointer matches previous block's hash
            if (!current.getPreviousHash().equals(previous.getHash())) {
                return false;
            }
        }
        return true;
    }

    private synchronized void saveChainToDisk() {
        try {
            File file = new File(LEDGER_FILE_PATH);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            mapper.writeValue(file, chain);
        } catch (Exception e) {
            System.err.println("AuditBlockchain: Failed to save ledger to disk: " + e.getMessage());
        }
    }
}

