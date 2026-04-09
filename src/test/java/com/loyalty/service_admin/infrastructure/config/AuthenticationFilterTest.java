package com.loyalty.service_admin.infrastructure.config;

import com.loyalty.service_admin.application.service.AuthService;
import com.loyalty.service_admin.infrastructure.security.JwtProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationFilter Unit Tests")
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

    @InjectMocks
    private AuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    // ==================== Public Endpoints ====================
    @Nested
    @DisplayName("Public Endpoints")
    class PublicEndpoints {

        @Test
        void testLoginEndpointPassesThrough() throws IOException, ServletException {
            when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
            filter.doFilter(request, response, filterChain);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        void testLogoutEndpointPassesThrough() throws IOException, ServletException {
            when(request.getRequestURI()).thenReturn("/api/v1/auth/logout");
            filter.doFilter(request, response, filterChain);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        void testHealthEndpointPassesThrough() throws IOException, ServletException {
            when(request.getRequestURI()).thenReturn("/api/v1/health");
            filter.doFilter(request, response, filterChain);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        void testSwaggerEndpointPassesThrough() throws IOException, ServletException {
            when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");
            filter.doFilter(request, response, filterChain);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        void testApiDocsEndpointPassesThrough() throws IOException, ServletException {
            when(request.getRequestURI()).thenReturn("/v3/api-docs");
            filter.doFilter(request, response, filterChain);
            verify(filterChain).doFilter(request, response);
        }
    }

    // ==================== Missing/Invalid Auth Header ====================
    @Nested
    @DisplayName("Missing or Invalid Authorization Header")
    class MissingAuthHeader {

        @Test
        void testNoAuthHeader_returns401() throws IOException, ServletException {
            when(request.getRequestURI()).thenReturn("/api/v1/users");
            when(request.getHeader("Authorization")).thenReturn(null);
            StringWriter sw = new StringWriter();
            when(response.getWriter()).thenReturn(new PrintWriter(sw));

            filter.doFilter(request, response, filterChain);

            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            verify(response).setContentType("application/json");
            assertTrue(sw.toString().contains("Token requerido"));
            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        void testAuthHeaderWithoutBearerPrefix_returns401() throws IOException, ServletException {
            when(request.getRequestURI()).thenReturn("/api/v1/users");
            when(request.getHeader("Authorization")).thenReturn("Basic abc123");
            StringWriter sw = new StringWriter();
            when(response.getWriter()).thenReturn(new PrintWriter(sw));

            filter.doFilter(request, response, filterChain);

            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            assertTrue(sw.toString().contains("Token requerido"));
        }
    }

    // ==================== Invalid Token ====================
    @Nested
    @DisplayName("Invalid Token")
    class InvalidToken {

        @Test
        void testInvalidToken_returns401() throws IOException, ServletException {
            when(request.getRequestURI()).thenReturn("/api/v1/users");
            when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
            when(jwtProvider.validateToken("invalid-token")).thenReturn(false);
            StringWriter sw = new StringWriter();
            when(response.getWriter()).thenReturn(new PrintWriter(sw));

            filter.doFilter(request, response, filterChain);

            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            assertTrue(sw.toString().contains("Token no válido o expirado"));
        }
    }

    // ==================== Valid Token ====================
    @Nested
    @DisplayName("Valid Token")
    class ValidToken {

        @Test
        void testValidToken_setsSecurityContext() throws IOException, ServletException {
            String token = "valid-jwt-token";
            UUID userId = UUID.randomUUID();
            UUID uid = UUID.randomUUID();
            UUID ecommerceId = UUID.randomUUID();

            when(request.getRequestURI()).thenReturn("/api/v1/users");
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtProvider.validateToken(token)).thenReturn(true);
            when(jwtProvider.getUsernameFromToken(token)).thenReturn("admin@test.com");
            when(jwtProvider.getUserIdFromToken(token)).thenReturn(userId);
            when(jwtProvider.getRoleFromToken(token)).thenReturn("SUPER_ADMIN");
            when(jwtProvider.getEcommerceIdFromToken(token)).thenReturn(ecommerceId);
            when(jwtProvider.getUidFromToken(token)).thenReturn(uid);

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertNotNull(SecurityContextHolder.getContext().getAuthentication());
            assertEquals("admin@test.com", SecurityContextHolder.getContext().getAuthentication().getName());
        }

        @Test
        void testValidToken_uidNull_fallsBackToUserId() throws IOException, ServletException {
            String token = "valid-jwt-token";
            UUID userId = UUID.randomUUID();

            when(request.getRequestURI()).thenReturn("/api/v1/users");
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtProvider.validateToken(token)).thenReturn(true);
            when(jwtProvider.getUsernameFromToken(token)).thenReturn("user@test.com");
            when(jwtProvider.getUserIdFromToken(token)).thenReturn(userId);
            when(jwtProvider.getRoleFromToken(token)).thenReturn("STORE_ADMIN");
            when(jwtProvider.getEcommerceIdFromToken(token)).thenReturn(UUID.randomUUID());
            when(jwtProvider.getUidFromToken(token)).thenReturn(null);

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        void testValidToken_superAdminNoEcommerce() throws IOException, ServletException {
            String token = "valid-jwt-token";
            UUID userId = UUID.randomUUID();

            when(request.getRequestURI()).thenReturn("/api/v1/users");
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtProvider.validateToken(token)).thenReturn(true);
            when(jwtProvider.getUsernameFromToken(token)).thenReturn("superadmin@test.com");
            when(jwtProvider.getUserIdFromToken(token)).thenReturn(userId);
            when(jwtProvider.getRoleFromToken(token)).thenReturn("SUPER_ADMIN");
            when(jwtProvider.getEcommerceIdFromToken(token)).thenReturn(null);
            when(jwtProvider.getUidFromToken(token)).thenReturn(userId);

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
    }

    // ==================== Exception During Token Processing ====================
    @Nested
    @DisplayName("Exception During Token Processing")
    class ExceptionHandling {

        @Test
        void testExceptionDuringClaims_returns401() throws IOException, ServletException {
            when(request.getRequestURI()).thenReturn("/api/v1/users");
            when(request.getHeader("Authorization")).thenReturn("Bearer some-token");
            when(jwtProvider.validateToken("some-token")).thenThrow(new RuntimeException("Parse error"));
            StringWriter sw = new StringWriter();
            when(response.getWriter()).thenReturn(new PrintWriter(sw));

            filter.doFilter(request, response, filterChain);

            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            assertTrue(sw.toString().contains("Token inválido"));
            verify(filterChain, never()).doFilter(request, response);
        }
    }

    // ==================== Init / Destroy ====================
    @Test
    void testInit_doesNotThrow() {
        assertDoesNotThrow(() -> filter.init(null));
    }

    @Test
    void testDestroy_doesNotThrow() {
        assertDoesNotThrow(() -> filter.destroy());
    }
}
