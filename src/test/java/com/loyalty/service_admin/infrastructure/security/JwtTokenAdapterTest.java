package com.loyalty.service_admin.infrastructure.security;

import com.loyalty.service_admin.infrastructure.exception.UnauthorizedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenAdapter Unit Tests")
class JwtTokenAdapterTest {

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private JwtTokenAdapter jwtTokenAdapter;

    @Test
    void testGenerateToken_delegatesToProvider() {
        // Arrange
        String username = "admin";
        UUID userId = UUID.randomUUID();
        String role = "ADMIN";
        UUID ecommerceId = UUID.randomUUID();
        String expectedToken = "jwt-token";
        when(jwtProvider.generateToken(username, userId, role, ecommerceId)).thenReturn(expectedToken);

        // Act
        String token = jwtTokenAdapter.generateToken(username, userId, role, ecommerceId);

        // Assert
        assertEquals(expectedToken, token);
        verify(jwtProvider).generateToken(username, userId, role, ecommerceId);
    }

    @Test
    void testValidateToken_validToken_returnsTrue() {
        // Arrange
        when(jwtProvider.validateToken("valid-token")).thenReturn(true);

        // Act & Assert
        assertTrue(jwtTokenAdapter.validateToken("valid-token"));
    }

    @Test
    void testValidateToken_invalidToken_returnsFalse() {
        // Arrange
        when(jwtProvider.validateToken("invalid")).thenReturn(false);

        // Act & Assert
        assertFalse(jwtTokenAdapter.validateToken("invalid"));
    }

    @Test
    void testValidateToken_jwtException_returnsFalse() {
        // Arrange
        when(jwtProvider.validateToken("bad")).thenThrow(new io.jsonwebtoken.JwtException("invalid"));

        // Act & Assert
        assertFalse(jwtTokenAdapter.validateToken("bad"));
    }

    @Test
    void testValidateToken_genericException_returnsFalse() {
        // Arrange
        when(jwtProvider.validateToken("bad")).thenThrow(new RuntimeException("error"));

        // Act & Assert
        assertFalse(jwtTokenAdapter.validateToken("bad"));
    }

    @Test
    void testGetUsernameFromToken_success() {
        // Arrange
        when(jwtProvider.getUsernameFromToken("token")).thenReturn("admin");

        // Act
        String username = jwtTokenAdapter.getUsernameFromToken("token");

        // Assert
        assertEquals("admin", username);
    }

    @Test
    void testGetUsernameFromToken_jwtException_throwsUnauthorized() {
        // Arrange
        when(jwtProvider.getUsernameFromToken("bad")).thenThrow(new io.jsonwebtoken.JwtException("invalid"));

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> jwtTokenAdapter.getUsernameFromToken("bad"));
    }

    @Test
    void testGetUsernameFromToken_genericException_throwsUnauthorized() {
        // Arrange
        when(jwtProvider.getUsernameFromToken("bad")).thenThrow(new RuntimeException("error"));

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> jwtTokenAdapter.getUsernameFromToken("bad"));
    }
}
