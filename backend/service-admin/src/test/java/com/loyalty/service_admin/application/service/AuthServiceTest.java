package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.LoginRequest;
import com.loyalty.service_admin.application.dto.LoginResponse;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.domain.repository.UserRepository;
import com.loyalty.service_admin.infrastructure.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    private UserEntity validUser;
    private UserEntity inactiveUser;

    @BeforeEach
    void setUp() {
        validUser = UserEntity.builder()
                .id(1L)
                .username("admin")
                .password("admin123")
                .role("ADMIN")
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        inactiveUser = UserEntity.builder()
                .id(2L)
                .username("inactive")
                .password("password123")
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
        LoginRequest request = new LoginRequest("admin", "admin123");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(validUser));

        // Act
        LoginResponse response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.token());
        assertEquals("Bearer", response.tipo());
        assertEquals("admin", response.username());
        assertEquals("ADMIN", response.role());
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
        assertEquals("Usuario no válido", exception.getMessage());
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
        assertEquals("Usuario no válido", exception.getMessage());
    }

    @Test
    @DisplayName("getCurrentUser con token inválido lanza UnauthorizedException")
    void testGetCurrentUser_invalidToken_throwsUnauthorizedException() {
        // Arrange
        String invalidToken = "invalid-token-format";

        // Act & Assert
        assertThrows(UnauthorizedException.class,
                () -> authService.getCurrentUser(invalidToken));
    }

    @Test
    @DisplayName("logout con token inválido no lanza excepción (es tolerante)")
    void testLogout_invalidToken_doesNotThrowException() {
        // Arrange
        String invalidToken = "invalid-token";

        // Act & Assert - logout no lanza excepción, es tolerante
        assertDoesNotThrow(() -> authService.logout(invalidToken));
    }
}
