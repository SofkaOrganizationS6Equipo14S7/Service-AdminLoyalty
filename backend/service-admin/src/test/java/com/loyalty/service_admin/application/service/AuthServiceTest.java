package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.LoginRequest;
import com.loyalty.service_admin.application.dto.LoginResponse;
import com.loyalty.service_admin.application.dto.UserResponse;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.domain.repository.UserRepository;
import com.loyalty.service_admin.infrastructure.exception.UnauthorizedException;
import com.loyalty.service_admin.infrastructure.security.JwtProvider;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mindrot.jbcrypt.BCrypt;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private AuthService authService;

    private UserEntity validUser;
    private UserEntity inactiveUser;
    private static final String PLAIN_PASSWORD = "admin123";
    private static final String VALID_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInVzZXJJZCI6MSwicm9sZSI6IkFETUluIn0.token";

    @BeforeEach
    void setUp() {
        String hashedPassword = BCrypt.hashpw(PLAIN_PASSWORD, BCrypt.gensalt());
        
        validUser = UserEntity.builder()
                .id(1L)
                .username("admin")
                .password(hashedPassword)
                .role("ADMIN")
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        inactiveUser = UserEntity.builder()
                .id(2L)
                .username("inactive")
                .password(BCrypt.hashpw("password123", BCrypt.gensalt()))
                .role("USER")
                .active(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Login exitoso retorna token JWT válido")
    void testLogin_success_returnsToken() {
        // Arrange
        LoginRequest request = new LoginRequest("admin", PLAIN_PASSWORD);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(validUser));
        when(jwtProvider.generateToken("admin", 1L, "ADMIN")).thenReturn(VALID_TOKEN);

        // Act
        LoginResponse response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.token());
        assertEquals(VALID_TOKEN, response.token());
        assertEquals("Bearer", response.tipo());
        assertEquals("admin", response.username());
        assertEquals("ADMIN", response.role());
        
        // Verify JwtProvider was called with correct parameters
        verify(jwtProvider, times(1)).generateToken("admin", 1L, "ADMIN");
    }

    @Test
    @DisplayName("Login con password incorrecto lanza UnauthorizedException")
    void testLogin_invalidPassword_throwsUnauthorizedException() {
        // Arrange
        LoginRequest request = new LoginRequest("admin", "wrongpassword");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(validUser));

        // Act & Assert
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> authService.login(request));
        assertEquals("Credenciales inválidas", exception.getMessage());
        
        // Verify JwtProvider was NOT called (failed at password validation)
        verify(jwtProvider, never()).generateToken(anyString(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Login con usuario no encontrado lanza UnauthorizedException")
    void testLogin_userNotFound_throwsUnauthorizedException() {
        // Arrange
        LoginRequest request = new LoginRequest("nonexistent", "password123");
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> authService.login(request));
        assertEquals("Credenciales inválidas", exception.getMessage());
        
        // Verify JwtProvider was NOT called
        verify(jwtProvider, never()).generateToken(anyString(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Login con usuario inactivo lanza UnauthorizedException")
    void testLogin_inactiveUser_throwsUnauthorizedException() {
        // Arrange
        LoginRequest request = new LoginRequest("inactive", "password123");
        when(userRepository.findByUsername("inactive")).thenReturn(Optional.of(inactiveUser));

        // Act & Assert
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> authService.login(request));
        assertEquals("Credenciales inválidas", exception.getMessage());
        
        // Verify JwtProvider was NOT called (failed at active status check)
        verify(jwtProvider, never()).generateToken(anyString(), anyLong(), anyString());
    }

    @Test
    @DisplayName("getCurrentUser con token válido retorna datos del usuario")
    void testGetCurrentUser_validToken_returnsUserResponse() {
        // Arrange
        when(jwtProvider.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtProvider.getUsernameFromToken(VALID_TOKEN)).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(validUser));

        // Act
        UserResponse response = authService.getCurrentUser(VALID_TOKEN);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("admin", response.username());
        assertEquals("ADMIN", response.role());
        assertTrue(response.active());
        assertNotNull(response.createdAt());
        assertNotNull(response.updatedAt());
        
        // Verify calls
        verify(jwtProvider, times(1)).validateToken(VALID_TOKEN);
        verify(jwtProvider, times(1)).getUsernameFromToken(VALID_TOKEN);
        verify(userRepository, times(1)).findByUsername("admin");
    }

    @Test
    @DisplayName("getCurrentUser con token inválido lanza UnauthorizedException")
    void testGetCurrentUser_invalidToken_throwsUnauthorizedException() {
        // Arrange
        String invalidToken = "invalid-token-format";
        when(jwtProvider.validateToken(invalidToken)).thenReturn(false);

        // Act & Assert
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> authService.getCurrentUser(invalidToken));
        assertEquals("Token no válido o expirado", exception.getMessage());
        
        // Verify only validateToken was called, no user lookup
        verify(jwtProvider, times(1)).validateToken(invalidToken);
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    @DisplayName("getCurrentUser con usuario desactivado lanza UnauthorizedException")
    void testGetCurrentUser_userDeactivated_throwsUnauthorizedException() {
        // Arrange
        String tokenForInactiveUser = "token-for-inactive";
        when(jwtProvider.validateToken(tokenForInactiveUser)).thenReturn(true);
        when(jwtProvider.getUsernameFromToken(tokenForInactiveUser)).thenReturn("inactive");
        when(userRepository.findByUsername("inactive")).thenReturn(Optional.of(inactiveUser));

        // Act & Assert
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> authService.getCurrentUser(tokenForInactiveUser));
        assertEquals("Usuario desactivado", exception.getMessage());
        
        // Verify calls
        verify(jwtProvider, times(1)).validateToken(tokenForInactiveUser);
        verify(jwtProvider, times(1)).getUsernameFromToken(tokenForInactiveUser);
        verify(userRepository, times(1)).findByUsername("inactive");
    }

    @Test
    @DisplayName("logout con token válido registra logout exitoso sin lanzar excepción")
    void testLogout_validToken_logoutSuccess() {
        // Arrange
        String validLogoutToken = "valid-logout-token";
        when(jwtProvider.validateToken(validLogoutToken)).thenReturn(true);
        when(jwtProvider.getUsernameFromToken(validLogoutToken)).thenReturn("admin");

        // Act & Assert
        assertDoesNotThrow(() -> authService.logout(validLogoutToken));
        
        // Verify token validation and username extraction occurred
        verify(jwtProvider, times(1)).validateToken(validLogoutToken);
        verify(jwtProvider, times(1)).getUsernameFromToken(validLogoutToken);
    }

    @Test
    @DisplayName("logout con token inválido no lanza excepción (es tolerante)")
    void testLogout_invalidToken_doesNotThrowException() {
        // Arrange
        String invalidToken = "invalid-token";
        when(jwtProvider.validateToken(invalidToken)).thenReturn(false);

        // Act & Assert - logout no lanza excepción, es tolerante
        assertDoesNotThrow(() -> authService.logout(invalidToken));
        
        // Verify only validateToken was called
        verify(jwtProvider, times(1)).validateToken(invalidToken);
        // Should NOT have called getUsernameFromToken for invalid token
        verify(jwtProvider, never()).getUsernameFromToken(anyString());
    }

    @Test
    @DisplayName("validateToken lanza JwtException y es capturada en getCurrentUser")
    void testGetCurrentUser_jwtExceptionDuringValidation_throwsUnauthorizedException() {
        // Arrange
        String malformedToken = "malformed.jwt.token";
        when(jwtProvider.validateToken(malformedToken)).thenThrow(new JwtException("Invalid JWT"));

        // Act & Assert
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> authService.getCurrentUser(malformedToken));
        assertEquals("Token no válido o expirado", exception.getMessage());
        
        // Verify error was caught and handled gracefully
        verify(jwtProvider, times(1)).validateToken(malformedToken);
    }
}
