package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.auth.LoginRequest;
import com.loyalty.service_admin.application.dto.auth.LoginResponse;
import com.loyalty.service_admin.application.dto.user.UserResponse;
import com.loyalty.service_admin.application.port.out.AuthPersistencePort;
import com.loyalty.service_admin.application.port.out.JwtPort;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.domain.entity.RoleEntity;
import com.loyalty.service_admin.infrastructure.exception.UnauthorizedException;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit Tests para AuthServiceImpl (TDD)
 * 
 * Mockea AuthPersistencePort y JwtPort para aislar la lógica de negocio.
 * Verifica que la lógica de login, logout y getCurrentUser sea correcta.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Unit Tests (TDD)")
class AuthServiceImplTest {

    @Mock
    private AuthPersistencePort authPersistencePort;

    @Mock
    private JwtPort jwtPort;

    @InjectMocks
    private AuthServiceImpl authService;

    private UserEntity testUser;
    private RoleEntity testRole;
    private UUID userId;
    private UUID roleId;
    private UUID ecommerceId;

    @BeforeEach
    void setUp() {
        // Preparar datos de prueba
        userId = UUID.randomUUID();
        roleId = UUID.randomUUID();
        ecommerceId = UUID.randomUUID();

        testRole = new RoleEntity();
        testRole.setId(roleId);
        testRole.setName("SUPER_ADMIN");

        testUser = new UserEntity();
        testUser.setId(userId);
        testUser.setUsername("admin");
        testUser.setEmail("admin@loyalty.com");
        testUser.setPasswordHash(BCrypt.hashpw("password123", BCrypt.gensalt()));
        testUser.setIsActive(true);
        testUser.setRole(testRole);
        testUser.setEcommerceId(ecommerceId);
        testUser.setCreatedAt(Instant.now().minusSeconds(3600));
        testUser.setUpdatedAt(Instant.now());
    }

    // ==================== Tests de LOGIN ====================

    /**
     * CRITERIO-4.2: Lógica de login exitosa
     * 
     * Escenario: Login exitoso con credenciales válidas
     * - Arrange: Usuario existe y password es correcto
     * - Act: authService.login(LoginRequest)
     * - Assert: Retorna LoginResponse con JWT token válido
     */
    @Test
    @DisplayName("testLogin_Success_ReturnsValidToken")
    void testLogin_Success_ReturnsValidToken() {
        // Arrange
        LoginRequest request = new LoginRequest("admin", "password123");
        String expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

        when(authPersistencePort.findByUsername("admin"))
            .thenReturn(Optional.of(testUser));
        when(jwtPort.generateToken(
                eq("admin"),
                eq(userId),
                eq("SUPER_ADMIN"),
                eq(ecommerceId)))
            .thenReturn(expectedToken);

        // Act
        LoginResponse result = authService.login(request);

        // Assert
        assertNotNull(result);
        assertEquals(expectedToken, result.token());
        assertEquals("Bearer", result.tipo());
        assertEquals("admin", result.username());
        assertEquals("SUPER_ADMIN", result.role());
        verify(authPersistencePort).findByUsername("admin");
        verify(jwtPort).generateToken(eq("admin"), eq(userId), eq("SUPER_ADMIN"), eq(ecommerceId));
    }

    /**
     * CRITERIO-4.3: Login falla con credenciales inválidas (usuario no existe)
     * 
     * Escenario: Usuario inexistente
     * - Arrange: Usuario no existe en BD
     * - Act: authService.login(LoginRequest)
     * - Assert: Lanza UnauthorizedException
     */
    @Test
    @DisplayName("testLogin_UserNotFound_ThrowsUnauthorizedException")
    void testLogin_UserNotFound_ThrowsUnauthorizedException() {
        // Arrange
        LoginRequest request = new LoginRequest("nonexistent", "password123");

        when(authPersistencePort.findByUsername("nonexistent"))
            .thenReturn(Optional.empty());

        // Act & Assert
        UnauthorizedException exception = assertThrows(
            UnauthorizedException.class,
            () -> authService.login(request)
        );

        assertEquals("Credenciales inválidas", exception.getMessage());
        verify(authPersistencePort).findByUsername("nonexistent");
    }

    /**
     * CRITERIO-4.3: Login falla con password incorrecto
     * 
     * Escenario: Usuario existe pero password no coincide
     * - Arrange: Usuario existe pero BCrypt.checkpw() retorna false
     * - Act: authService.login(LoginRequest)
     * - Assert: Lanza UnauthorizedException
     */
    @Test
    @DisplayName("testLogin_IncorrectPassword_ThrowsUnauthorizedException")
    void testLogin_IncorrectPassword_ThrowsUnauthorizedException() {
        // Arrange
        LoginRequest request = new LoginRequest("admin", "wrongpassword");

        when(authPersistencePort.findByUsername("admin"))
            .thenReturn(Optional.of(testUser));

        // Act & Assert
        UnauthorizedException exception = assertThrows(
            UnauthorizedException.class,
            () -> authService.login(request)
        );

        assertEquals("Credenciales inválidas", exception.getMessage());
        verify(authPersistencePort).findByUsername("admin");
    }

    /**
     * Escenario: Usuario inactivo no puede hacer login
     * - Arrange: Usuario existe pero isActive = false
     * - Act: authService.login(LoginRequest)
     * - Assert: Lanza UnauthorizedException
     */
    @Test
    @DisplayName("testLogin_InactiveUser_ThrowsUnauthorizedException")
    void testLogin_InactiveUser_ThrowsUnauthorizedException() {
        // Arrange
        testUser.setIsActive(false);
        LoginRequest request = new LoginRequest("admin", "password123");

        when(authPersistencePort.findByUsername("admin"))
            .thenReturn(Optional.of(testUser));

        // Act & Assert
        UnauthorizedException exception = assertThrows(
            UnauthorizedException.class,
            () -> authService.login(request)
        );

        assertEquals("Credenciales inválidas", exception.getMessage());
    }

    // ==================== Tests de GET CURRENT USER ====================

    /**
     * Escenario: GetCurrentUser exitoso con token válido
     * - Arrange: Token es válido, usuario existe y está activo
     * - Act: authService.getCurrentUser(String token)
     * - Assert: Retorna UserResponse con datos del usuario
     */
    @Test
    @DisplayName("testGetCurrentUser_ValidToken_ReturnsUserResponse")
    void testGetCurrentUser_ValidToken_ReturnsUserResponse() {
        // Arrange
        String validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

        when(jwtPort.validateToken(validToken)).thenReturn(true);
        when(jwtPort.getUsernameFromToken(validToken)).thenReturn("admin");
        when(authPersistencePort.findByUsername("admin"))
            .thenReturn(Optional.of(testUser));

        // Act
        UserResponse result = authService.getCurrentUser(validToken);

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.uid());
        assertEquals("admin", result.username());
        assertEquals("SUPER_ADMIN", result.roleName());
        assertEquals("admin@loyalty.com", result.email());
        assertTrue(result.isActive());
        verify(jwtPort).validateToken(validToken);
        verify(jwtPort).getUsernameFromToken(validToken);
        verify(authPersistencePort).findByUsername("admin");
    }

    /**
     * Escenario: GetCurrentUser con token inválido
     * - Arrange: Token es inválido o expirado
     * - Act: authService.getCurrentUser(String token)
     * - Assert: Lanza UnauthorizedException
     */
    @Test
    @DisplayName("testGetCurrentUser_InvalidToken_ThrowsUnauthorizedException")
    void testGetCurrentUser_InvalidToken_ThrowsUnauthorizedException() {
        // Arrange
        String invalidToken = "invalid-token";

        when(jwtPort.validateToken(invalidToken)).thenReturn(false);

        // Act & Assert
        UnauthorizedException exception = assertThrows(
            UnauthorizedException.class,
            () -> authService.getCurrentUser(invalidToken)
        );

        assertEquals("Token no válido o expirado", exception.getMessage());
        verify(jwtPort).validateToken(invalidToken);
    }

    /**
     * Escenario: GetCurrentUser con token válido pero usuario no existe
     * - Arrange: Token es válido pero usuario fue eliminado de BD
     * - Act: authService.getCurrentUser(String token)
     * - Assert: Lanza UnauthorizedException
     */
    @Test
    @DisplayName("testGetCurrentUser_UserNotFound_ThrowsUnauthorizedException")
    void testGetCurrentUser_UserNotFound_ThrowsUnauthorizedException() {
        // Arrange
        String validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

        when(jwtPort.validateToken(validToken)).thenReturn(true);
        when(jwtPort.getUsernameFromToken(validToken)).thenReturn("admin");
        when(authPersistencePort.findByUsername("admin"))
            .thenReturn(Optional.empty());

        // Act & Assert
        UnauthorizedException exception = assertThrows(
            UnauthorizedException.class,
            () -> authService.getCurrentUser(validToken)
        );

        assertEquals("Usuario no válido", exception.getMessage());
    }

    /**
     * Escenario: GetCurrentUser con usuario desactivado después de emitir token
     * - Arrange: Token es válido pero usuario fue desactivado
     * - Act: authService.getCurrentUser(String token)
     * - Assert: Lanza UnauthorizedException
     */
    @Test
    @DisplayName("testGetCurrentUser_InactiveUser_ThrowsUnauthorizedException")
    void testGetCurrentUser_InactiveUser_ThrowsUnauthorizedException() {
        // Arrange
        testUser.setIsActive(false);
        String validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

        when(jwtPort.validateToken(validToken)).thenReturn(true);
        when(jwtPort.getUsernameFromToken(validToken)).thenReturn("admin");
        when(authPersistencePort.findByUsername("admin"))
            .thenReturn(Optional.of(testUser));

        // Act & Assert
        UnauthorizedException exception = assertThrows(
            UnauthorizedException.class,
            () -> authService.getCurrentUser(validToken)
        );

        assertEquals("Usuario desactivado", exception.getMessage());
    }

    // ==================== Tests de LOGOUT ====================

    /**
     * Escenario: Logout exitoso (stateless)
     * - Arrange: Token es válido
     * - Act: authService.logout(String token)
     * - Assert: Método completa sin error (void)
     */
    @Test
    @DisplayName("testLogout_ValidToken_CompletesSuccessfully")
    void testLogout_ValidToken_CompletesSuccessfully() {
        // Arrange
        String validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

        when(jwtPort.validateToken(validToken)).thenReturn(true);
        when(jwtPort.getUsernameFromToken(validToken)).thenReturn("admin");

        // Act - debe no lanzar excepciones
        assertDoesNotThrow(() -> authService.logout(validToken));

        // Assert
        verify(jwtPort).validateToken(validToken);
    }

    /**
     * Escenario: Logout con token inválido
     * - Arrange: Token es inválido
     * - Act: authService.logout(String token)
     * - Assert: Método completa sin error (logout es tolerante a errores)
     */
    @Test
    @DisplayName("testLogout_InvalidToken_CompletesSuccessfully")
    void testLogout_InvalidToken_CompletesSuccessfully() {
        // Arrange
        String invalidToken = "invalid-token";

        when(jwtPort.validateToken(invalidToken)).thenReturn(false);

        // Act - debe no lanzar excepciones
        assertDoesNotThrow(() -> authService.logout(invalidToken));

        // Assert
        verify(jwtPort).validateToken(invalidToken);
    }

    /**
     * Escenario: Logout es stateless (no requiere BD)
     * - Act: authService.logout()
     * - Assert: Never interacts with persistence port
     */
    @Test
    @DisplayName("testLogout_DoesNotAccessPersistence")
    void testLogout_DoesNotAccessPersistence() {
        // Arrange
        String validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

        when(jwtPort.validateToken(validToken)).thenReturn(true);
        when(jwtPort.getUsernameFromToken(validToken)).thenReturn("admin");

        // Act
        authService.logout(validToken);

        // Assert - verify that we never called persistence port
        org.mockito.Mockito.verifyNoInteractions(authPersistencePort);
    }
}
