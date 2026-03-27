package com.loyalty.service_admin.infrastructure.config;

import com.loyalty.service_admin.application.dto.UserResponse;
import com.loyalty.service_admin.application.service.AuthService;
import com.loyalty.service_admin.infrastructure.security.JwtProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests para AuthenticationFilter.
 * Valida que el filtro procesa correctamente tokens válidos e inválidos.
 * 
 * El filtro debe:
 * 1. Permitir acceso a endpoints públicos sin token
 * 2. Requerir token válido para endpoints protegidos
 * 3. Rechazar requests sin token en header Authorization
 * 4. Rechazar tokens expirados o inválidos
 * 5. Permitir requests con token válido
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationFilter Tests")
class AuthenticationFilterTest {

    @Mock
    private AuthService authService;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private PrintWriter printWriter;

    private AuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new AuthenticationFilter(authService, jwtProvider);
    }

    @Test
    @DisplayName("Filtro permite request a endpoint público sin token")
    void testFilter_publicEndpoint_allowsRequest() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(401);
    }

    @Test
    @DisplayName("Filtro rechaza request protegido sin token (sin header Authorization)")
    void testFilter_protectedEndpoint_missingToken_returns401() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/v1/auth/me");
        when(request.getHeader("Authorization")).thenReturn(null);
        when(response.getWriter()).thenReturn(printWriter);

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(response).setStatus(401);
        verify(filterChain, never()).doFilter(request, response);
        verify(response).getWriter();
    }

    @Test
    @DisplayName("Filtro rechaza request con header Authorization inválido (sin 'Bearer ')")
    void testFilter_protectedEndpoint_invalidAuthHeader_returns401() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/v1/auth/me");
        when(request.getHeader("Authorization")).thenReturn("InvalidToken123");
        when(response.getWriter()).thenReturn(printWriter);

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(response).setStatus(401);
        verify(filterChain, never()).doFilter(request, response);
        verify(response).getWriter();
    }

    @Test
    @DisplayName("Filtro rechaza request con token expirado o inválido")
    void testFilter_protectedEndpoint_expiredToken_returns401() throws ServletException, IOException {
        // Arrange
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.token";
        when(request.getRequestURI()).thenReturn("/api/v1/auth/me");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);
        when(authService.getCurrentUser(invalidToken))
                .thenThrow(new IllegalArgumentException("Token no válido o expirado"));
        when(response.getWriter()).thenReturn(printWriter);

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(response).setStatus(401);
        verify(filterChain, never()).doFilter(request, response);
        verify(response).getWriter();
    }

    @Test
    @DisplayName("Filtro permite request a endpoint protegido con token válido")
    void testFilter_protectedEndpoint_validToken_continuesRequest() throws ServletException, IOException {
        // Arrange
        String validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
        Instant now = Instant.now();
        UUID testEcommerce = UUID.randomUUID();
        when(request.getRequestURI()).thenReturn("/api/v1/auth/me");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(authService.getCurrentUser(validToken))
                .thenReturn(new UserResponse(
                        UUID.randomUUID(),
                        "admin",
                        "ADMIN",
                        "admin@example.com",
                        testEcommerce,
                        true,
                        now,
                        now
                ));

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(401);
    }

    @Test
    @DisplayName("Filtro rechaza request a endpoint protegido con Authorization header vacío")
    void testFilter_protectedEndpoint_emptyAuthHeader_returns401() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/v1/auth/me");
        when(request.getHeader("Authorization")).thenReturn("");
        when(response.getWriter()).thenReturn(printWriter);

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(response).setStatus(401);
        verify(filterChain, never()).doFilter(request, response);
        verify(response).getWriter();
    }

    @Test
    @DisplayName("Filtro permite request a /api/v1/health sin token (endpoint público)")
    void testFilter_healthEndpoint_isPublic_allowsRequest() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/v1/health");

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(401);
    }

    @Test
    @DisplayName("Filtro permite request a /api/v1/auth/logout sin token (endpoint público)")
    void testFilter_logoutEndpoint_isPublic_allowsRequest() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/v1/auth/logout");

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(401);
    }
}
