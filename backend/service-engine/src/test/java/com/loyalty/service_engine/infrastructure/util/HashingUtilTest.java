package com.loyalty.service_engine.infrastructure.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HashingUtilTest {

    @Test
    void sha256ShouldGenerateDeterministic64LengthHash() {
        String input = "test-api-key";

        String hash1 = HashingUtil.sha256(input);
        String hash2 = HashingUtil.sha256(input);

        assertNotNull(hash1);
        assertEquals(64, hash1.length());
        assertEquals(hash1, hash2, "Same input must produce same hash");
        assertTrue(hash1.matches("^[a-f0-9]{64}$"), "Hash should be lowercase hexadecimal");
    }

    @Test
    void sha256ShouldGenerateDifferentHashesForDifferentInputs() {
        String hash1 = HashingUtil.sha256("key-1");
        String hash2 = HashingUtil.sha256("key-2");

        assertNotEquals(hash1, hash2);
    }
}

