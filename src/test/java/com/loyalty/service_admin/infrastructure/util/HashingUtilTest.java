package com.loyalty.service_admin.infrastructure.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HashingUtil Unit Tests")
class HashingUtilTest {

    @Test
    void testSha256_consistentOutput() {
        String input = "test-api-key-value";
        String hash1 = HashingUtil.sha256(input);
        String hash2 = HashingUtil.sha256(input);

        assertEquals(hash1, hash2);
    }

    @Test
    void testSha256_returns64CharHexString() {
        String hash = HashingUtil.sha256("any-input");

        assertNotNull(hash);
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]{64}"));
    }

    @Test
    void testSha256_differentInputs_differentHashes() {
        String hash1 = HashingUtil.sha256("input-a");
        String hash2 = HashingUtil.sha256("input-b");

        assertNotEquals(hash1, hash2);
    }

    @Test
    void testSha256_emptyString() {
        String hash = HashingUtil.sha256("");

        assertNotNull(hash);
        assertEquals(64, hash.length());
        // SHA-256 of "" is a known value: e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hash);
    }

    @Test
    void testSha256_specialCharacters() {
        String hash = HashingUtil.sha256("áéíóú ñ 日本語 🎉");

        assertNotNull(hash);
        assertEquals(64, hash.length());
    }
}
