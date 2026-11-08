package com.forensix.blockchain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuditBlockchainTest {

    @Test
    @DisplayName("Blockchain ledger should initialize genesis block and append chained audit records")
    void testBlockchainAppendAndIntegrity() {
        AuditBlockchain ledger = AuditBlockchain.getInstance();
        assertNotNull(ledger);

        List<AuditBlockchain.Block> initialBlocks = ledger.getBlocks();
        assertFalse(initialBlocks.isEmpty(), "Chain must contain at least the genesis block");

        AuditBlockchain.Block genesis = initialBlocks.get(0);
        assertEquals(0, genesis.getIndex());
        assertNotNull(genesis.getHash());

        // Append an audit record
        AuditBlockchain.Block newBlock = ledger.recordAuditBlock(
                "DATA_SANITIZATION",
                "\\\\.\\PhysicalDrive1",
                "NIST SP 800-88 Rev 1 (Clear)",
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "Verified zero entropy state on 32GB volume"
        );

        assertNotNull(newBlock);
        assertTrue(newBlock.getIndex() > 0);
        assertNotNull(newBlock.getHash());
        assertNotNull(newBlock.getPreviousHash());

        // Verify cryptographic chain validity
        assertTrue(ledger.isChainValid(), "Cryptographic audit chain must be fully intact and valid");
    }

    @Test
    @DisplayName("Individual block hash recalculation should match stored hash")
    void testBlockCalculateHash() {
        AuditBlockchain.Block block = new AuditBlockchain.Block(
                1, "2026-11-08 12:00:00", "EVIDENCE_CARVING", "\\\\.\\PhysicalDrive2",
                "Raw Sector Carving", "hash123", "4 files recovered", "prevHash123"
        );

        assertEquals(block.getHash(), block.calculateHash());
    }
}

