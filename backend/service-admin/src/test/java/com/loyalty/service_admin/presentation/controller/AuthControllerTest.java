package com.loyalty.service_admin.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyalty.service_admin.application.dto.auth.LoginRequest;
import com.loyalty.service_admin.application.dto.auth.LoginResponse;
import com.loyalty.service_admin.application.dto.user.UserResponse;
import com.loyalty.service_admin.application.port.in.AuthUseCase;
import com.loyalty.service_admin.infrastructure.exception.UnauthorizedException;
import com.loyalty.service_admin.infrastructure.security.SecurityContextHelper;
import com.loyalty.service_admin.infrastructure.security.TenantInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AuthController.class})
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController Unit Tests (TDD)")
class AuthControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public SecurityContextHelper securityContextHelper() {
            return Mockito.mock(SecurityContextHelper.class);
        }

        @Bean
        public TenantInterceptor tenantInterceptor(SecurityContextHelper helper) {
            return new TenantInterceptor(helper);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthUseCase authUseCase;

    /**
     * Controllers test for POST /api/v1/auth/login success
     *
     * Escenario: Login exitoso con credenciales válidas
     * - Request: LoginRequest(username="admin", password="password123")
     * - Mock: authUseCase.login() retorna LoginResponse válido
     * - Expected: HTTP 200 OK con token, tipo, username, role
     */
    @Test
    @DisplayName("testLoginSuccess_ValidCredentials_Returns200WithToken")
    void testLoginSuccess_ValidCredentials_Returns200WithToken() throws Exception {
        LoginRequest request = new LoginRequest("admin", "password123");
        LoginResponse response = new LoginResponse(
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
            "Bearer",
            "admin",
            "SUPER_ADMIN"
        );
        
        when(authUseCase.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("SUPER_ADMIN"));
    }

    /**
     * Controllers test for POST /api/v1/auth/login unauthorized
     * 
     * Escenario: Login falla con credenciales inválidas
     * - Request: LoginRequest con password incorrecto
     * - Mock: authUseCase.login() lanza UnauthorizedException
     * - Expected: HTTP 401 Unauthorized
     */
    @Test
    @DisplayName("testLoginFail_InvalidCredentials_Returns401")
    void testLoginFail_InvalidCredentials_Returns401() throws Exception {
        LoginRequest request = new LoginRequest("admin", "wrongpassword");
        
        when(authUseCase.login(any(LoginRequest.class)))
            .thenThrow(new UnauthorizedException("Credenciales inválidas"));

        mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Escenario: Login con usuario no encontrado
     * - Request: LoginRequest con username inexistente
     * - Mock: authUseCase.login() lanza UnauthorizedException
     * - Expected: HTTP 401 Unauthorized
     */
    @Test
    @DisplayName("testLoginFail_UserNotFound_Returns401")
    void testLoginFail_UserNotFound_Returns401() throws Exception {
        LoginRequest request = new LoginRequest("nonexistent", "password123");
        
        when(authUseCase.login(any(LoginRequest.class)))
            .thenThrow(new UnauthorizedException("Credenciales inválidas"));

        mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Escenario: Login con campos faltantes (validación @Valid)
     * - Request: LoginRequest con username en blanco
     * - Expected: HTTP 400 Bad Request (error de validación de Spring)
     */
    @Test
    @DisplayName("testLoginFail_MissingUsername_Returns400")
    void testLoginFail_MissingUsername_Returns400() throws Exception {
        LoginRequest request = new LoginRequest("", "password123");

        mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Escenario: Login con password faltante (validación @Valid)
     * - Request: LoginRequest con password en blanco
     * - Expected: HTTP 400 Bad Request
     */
    @Test
    @DisplayName("testLoginFail_MissingPassword_Returns400")
    void testLoginFail_MissingPassword_Returns400() throws Exception {
        LoginRequest request = new LoginRequest("admin", "");

        mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Escenario: Logout exitoso con token válido
     * - Request: POST /api/v1/auth/logout con Authorization header
     * - Mock: authUseCase.logout() completado sin error
     * - Expected: HTTP 204 No Content
     */
    @Test
    @DisplayName("testLogout_ValidToken_Returns204")
    void testLogout_ValidToken_Returns204() throws Exception {
        String validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
        
        doNothing().when(authUseCase).logout(anyString());

        mockMvc.perform(post("/api/v1/auth/logout")
                    .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNoContent());
    }

    /**
     * Escenario: Logout sin Authorization header
     * - Request: POST /api/v1/auth/logout sin header
     * - Expected: HTTP 204 No Content (logout es stateless, no falla sin token)
     */
    @Test
    @DisplayName("testLogout_NoToken_Returns204")
    void testLogout_NoToken_Returns204() throws Exception {
        doNothing().when(authUseCase).logout(anyString());

        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent());
    }

    /**
     * Controllers test for GET /api/v1/auth/me success
     * 
     * Escenario: GetCurrentUser exitoso con token válido
     * - Request: GET /api/v1/auth/me con Authorization header
     * - Mock: authUseCase.getCurrentUser() retorna UserResponse válido
     * - Expected: HTTP 200 OK con datos del usuario
     */
    @Test
    @DisplayName("testGetCurrentUser_ValidToken_Returns200WithUser")
    void testGetCurrentUser_ValidToken_Returns200WithUser() throws Exception {
        String validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID ecommerceId = UUID.randomUUID();
        
        UserResponse userResponse = new UserResponse(
            userId,
            "admin",
            roleId,
            "SUPER_ADMIN",
            "admin@loyalty.com",
            ecommerceId,
            true,
            Instant.now().minusSeconds(3600),
            Instant.now()
        );
        
        when(authUseCase.getCurrentUser(anyString())).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/auth/me")
                    .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.roleName").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.email").value("admin@loyalty.com"));
    }

    /**
     * Escenario: GetCurrentUser con token inválido
     * - Request: GET /api/v1/auth/me con token expirado/inválido
     * - Mock: authUseCase.getCurrentUser() lanza UnauthorizedException
     * - Expected: HTTP 401 Unauthorized
     */
    @Test
    @DisplayName("testGetCurrentUser_InvalidToken_Returns401")
    void testGetCurrentUser_InvalidToken_Returns401() throws Exception {
        String invalidToken = "invalid-token";
        
        when(authUseCase.getCurrentUser(anyString()))
            .thenThrow(new UnauthorizedException("Token no válido o expirado"));

        mockMvc.perform(get("/api/v1/auth/me")
                    .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Escenario: GetCurrentUser con token expirado
     * - Request: GET /api/v1/auth/me con token expirado
     * - Mock: authUseCase.getCurrentUser() lanza UnauthorizedException
     * - Expected: HTTP 401 Unauthorized
     */
    @Test
    @DisplayName("testGetCurrentUser_ExpiredToken_Returns401")
    void testGetCurrentUser_ExpiredToken_Returns401() throws Exception {
        String expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.expired...";
        
        when(authUseCase.getCurrentUser(anyString()))
            .thenThrow(new UnauthorizedException("Token no válido o expirado"));

        mockMvc.perform(get("/api/v1/auth/me")
                    .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Escenario: GetCurrentUser sin Authorization header
     * - Request: GET /api/v1/auth/me sin Authorization
     * - Expected: HTTP 401 Unauthorized
     */
    @Test
    @DisplayName("testGetCurrentUser_NoAuthHeader_Returns401")
    void testGetCurrentUser_NoAuthHeader_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Escenario: GetCurrentUser con usuario desactivado
     * - Request: GET /api/v1/auth/me con token válido pero usuario desactivado
     * - Mock: authUseCase.getCurrentUser() lanza UnauthorizedException
     * - Expected: HTTP 401 Unauthorized
     */
    @Test
    @DisplayName("testGetCurrentUser_InactiveUser_Returns401")
    void testGetCurrentUser_InactiveUser_Returns401() throws Exception {
        String validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
        
        when(authUseCase.getCurrentUser(anyString()))
            .thenThrow(new UnauthorizedException("Usuario desactivado"));

        mockMvc.perform(get("/api/v1/auth/me")
                    .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isUnauthorized());
    }
}