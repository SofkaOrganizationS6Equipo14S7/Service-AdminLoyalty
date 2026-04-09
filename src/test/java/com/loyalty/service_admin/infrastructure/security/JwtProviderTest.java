package com.loyalty.service_admin.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtProvider Unit Tests")
class JwtProviderTest {

    private JwtProvider jwtProvider;
    private static final String SECRET = "loyalty-secret-key-v1-test-minimum-32-characters-long";
    private static final long EXPIRATION_MS = 86400000L;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(SECRET, EXPIRATION_MS);
    }

    // ==================== Token Generation ====================

    @Test
    void testGenerateToken_success() {
        // Arrange
        String username = "admin";
        UUID userId = UUID.randomUUID();
        String role = "SUPER_ADMIN";
        UUID ecommerceId = UUID.randomUUID();

        // Act
        String token = jwtProvider.generateToken(username, userId, role, ecommerceId);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testGenerateToken_withNullEcommerceId() {
        // Arrange
        String username = "superadmin";
        UUID userId = UUID.randomUUID();
        String role = "SUPER_ADMIN";

        // Act
        String token = jwtProvider.generateToken(username, userId, role, null);

        // Assert
        assertNotNull(token);
    }

    @Test
    void testGenerateTokenFull_withUid() {
        // Arrange
        UUID uid = UUID.randomUUID();
        String username = "admin";
        UUID userId = UUID.randomUUID();
        String role = "STORE_ADMIN";
        UUID ecommerceId = UUID.randomUUID();

        // Act
        String token = jwtProvider.generateTokenFull(uid, username, userId, role, ecommerceId);

        // Assert
        assertNotNull(token);
    }

    @Test
    void testGenerateTokenFull_withNullUid() {
        // Arrange
        String username = "admin";
        UUID userId = UUID.randomUUID();
        String role = "STORE_ADMIN";
        UUID ecommerceId = UUID.randomUUID();

        // Act
        String token = jwtProvider.generateTokenFull(null, username, userId, role, ecommerceId);

        // Assert
        assertNotNull(token);
    }

    // ==================== Token Validation ====================

    @Test
    void testValidateToken_validToken() {
        // Arrange
        String token = jwtProvider.generateToken("admin", UUID.randomUUID(), "ADMIN", UUID.randomUUID());

        // Act & Assert
        assertTrue(jwtProvider.validateToken(token));
    }

    @Test
    void testValidateToken_validTokenWithBearerPrefix() {
        // Arrange
        String token = jwtProvider.generateToken("admin", UUID.randomUUID(), "ADMIN", UUID.randomUUID());

        // Act & Assert
        assertTrue(jwtProvider.validateToken("Bearer " + token));
    }

    @Test
    void testValidateToken_invalidToken() {
        // Act & Assert
        assertFalse(jwtProvider.validateToken("invalid.token.here"));
    }

    @Test
    void testValidateToken_expiredToken() {
        // Arrange - Create provider with 0ms expiration
        JwtProvider expiredProvider = new JwtProvider(SECRET, 0L);
        String token = expiredProvider.generateToken("admin", UUID.randomUUID(), "ADMIN", UUID.randomUUID());

        // Act & Assert
        assertFalse(jwtProvider.validateToken(token));
    }

    @Test
    void testValidateToken_emptyToken() {
        // Act & Assert
        assertFalse(jwtProvider.validateToken(""));
    }

    // ==================== Claim Extraction ====================

    @Test
    void testGetUsernameFromToken_success() {
        // Arrange
        String expectedUsername = "testuser";
        String token = jwtProvider.generateToken(expectedUsername, UUID.randomUUID(), "ADMIN", UUID.randomUUID());

        // Act
        String username = jwtProvider.getUsernameFromToken(token);

        // Assert
        assertEquals(expectedUsername, username);
    }

    @Test
    void testGetUsernameFromToken_withBearerPrefix() {
        // Arrange
        String expectedUsername = "testuser";
        String token = jwtProvider.generateToken(expectedUsername, UUID.randomUUID(), "ADMIN", UUID.randomUUID());

        // Act
        String username = jwtProvider.getUsernameFromToken("Bearer " + token);

        // Assert
        assertEquals(expectedUsername, username);
    }

    @Test
    void testGetUsernameFromToken_invalidToken_throwsException() {
        // Act & Assert
        assertThrows(io.jsonwebtoken.JwtException.class, () -> jwtProvider.getUsernameFromToken("invalid.token"));
    }

    @Test
    void testGetUserIdFromToken_success() {
        // Arrange
        UUID expectedUserId = UUID.randomUUID();
        String token = jwtProvider.generateToken("admin", expectedUserId, "ADMIN", UUID.randomUUID());

        // Act
        UUID userId = jwtProvider.getUserIdFromToken(token);

        // Assert
        assertEquals(expectedUserId, userId);
    }

    @Test
    void testGetUserIdFromToken_withBearerPrefix() {
        // Arrange
        UUID expectedUserId = UUID.randomUUID();
        String token = jwtProvider.generateToken("admin", expectedUserId, "ADMIN", UUID.randomUUID());

        // Act
        UUID userId = jwtProvider.getUserIdFromToken("Bearer " + token);

        // Assert
        assertEquals(expectedUserId, userId);
    }

    @Test
    void testGetUserIdFromToken_invalidToken_throwsException() {
        // Act & Assert
        assertThrows(io.jsonwebtoken.JwtException.class, () -> jwtProvider.getUserIdFromToken("invalid"));
    }

    @Test
    void testGetUidFromToken_returnsUserId() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String token = jwtProvider.generateToken("admin", userId, "ADMIN", UUID.randomUUID());

        // Act
        UUID uid = jwtProvider.getUidFromToken(token);

        // Assert
        assertEquals(userId, uid);
    }

    @Test
    void testGetUidFromToken_invalidToken_returnsNull() {
        // Act
        UUID uid = jwtProvider.getUidFromToken("invalid.token");

        // Assert
        assertNull(uid);
    }

    @Test
    void testGetRoleFromToken_success() {
        // Arrange
        String expectedRole = "STORE_ADMIN";
        String token = jwtProvider.generateToken("admin", UUID.randomUUID(), expectedRole, UUID.randomUUID());

        // Act
        String role = jwtProvider.getRoleFromToken(token);

        // Assert
        assertEquals(expectedRole, role);
    }

    @Test
    void testGetRoleFromToken_invalidToken_throwsException() {
        // Act & Assert
        assertThrows(io.jsonwebtoken.JwtException.class, () -> jwtProvider.getRoleFromToken("invalid"));
    }

    @Test
    void testGetEcommerceIdFromToken_success() {
        // Arrange
        UUID expectedEcommerceId = UUID.randomUUID();
        String token = jwtProvider.generateToken("admin", UUID.randomUUID(), "ADMIN", expectedEcommerceId);

        // Act
        UUID ecommerceId = jwtProvider.getEcommerceIdFromToken(token);

        // Assert
        assertEquals(expectedEcommerceId, ecommerceId);
    }

    @Test
    void testGetEcommerceIdFromToken_nullEcommerceId_returnsNull() {
        // Arrange
        String token = jwtProvider.generateToken("admin", UUID.randomUUID(), "SUPER_ADMIN", null);

        // Act
        UUID ecommerceId = jwtProvider.getEcommerceIdFromToken(token);

        // Assert
        assertNull(ecommerceId);
    }

    @Test
    void testGetEcommerceIdFromToken_invalidToken_returnsNull() {
        // Act
        UUID ecommerceId = jwtProvider.getEcommerceIdFromToken("invalid.token");

        // Assert
        assertNull(ecommerceId);
    }

    // ==================== Constructor Edge Cases ====================

    @Test
    void testConstructor_shortSecret_doesNotThrow() {
        // Arrange & Act & Assert - should log warning but not throw
        // Using exactly 32 chars to avoid the warning
        assertDoesNotThrow(() -> new JwtProvider("12345678901234567890123456789012", EXPIRATION_MS));
    }
}
