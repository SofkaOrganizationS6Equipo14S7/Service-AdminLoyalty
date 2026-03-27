package com.loyalty.service_admin.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyalty.service_admin.application.dto.UserCreateRequest;
import com.loyalty.service_admin.application.dto.UserResponse;
import com.loyalty.service_admin.application.dto.UserUpdateRequest;
import com.loyalty.service_admin.application.service.UserService;
import com.loyalty.service_admin.infrastructure.exception.AuthorizationException;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import com.loyalty.service_admin.infrastructure.exception.ConflictException;
import com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración para UserController (@SpringBootTest).
 * Cubre los 5 endpoints REST: POST, GET (list), GET (by uid), PUT, DELETE.
 * 
 * Implementa SPEC-002:
 * - HU-01: Crear usuario (POST /api/v1/users)
 * - HU-02: Validar acceso según ecommerce
 * - HU-03: Listar usuarios (GET /api/v1/users)
 * - HU-04: Actualizar usuario (PUT /api/v1/users/{uid})
 * - HU-05: Eliminar usuario (DELETE /api/v1/users/{uid})
 * 
 * Cobertura: HTTP status codes, validaciones, error handling
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    // Fixtures
    private UUID testEcommerceId = UUID.randomUUID();
    private UUID testUserId = UUID.randomUUID();
    private UUID testEcommerce2Id = UUID.randomUUID();

    private UserResponse buildUserResponse(UUID uid, String username, UUID ecommerceId) {
        return new UserResponse(
                uid,
                username,
                "ADMIN",
                "user@example.com",
                ecommerceId,
                true,
                Instant.now(),
                Instant.now()
        );
    }

    // ========== POST /api/v1/users ==========

    /**
     * CRITERIO-1.1: Crear usuario exitosamente con ecommerce válido
     * Happy Path: respuesta 201 Created con datos del usuario
     */
    @Test
    void testCreateUser_success() throws Exception {
        // Arrange
        UserCreateRequest request = new UserCreateRequest(
                "newadmin",
                "SecureP@ss123",
                "ADMIN",
                "admin@shop.com",
                testEcommerceId
        );
        UserResponse expectedResponse = buildUserResponse(testUserId, "newadmin", testEcommerceId);
        when(userService.createUser(any(UserCreateRequest.class))).thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.uid").exists())
            .andExpect(jsonPath("$.username").value("newadmin"))
            .andExpect(jsonPath("$.ecommerceId").value(testEcommerceId.toString()))
            .andExpect(jsonPath("$.active").value(true));

        verify(userService, times(1)).createUser(any(UserCreateRequest.class));
    }

    /**
     * CRITERIO-1.2: Rechazar creación si ecommerce no existe
     * Error Path: respuesta 400 Bad Request
     */
    @Test
    void testCreateUser_ecommerceNotFound() throws Exception {
        // Arrange
        UserCreateRequest request = new UserCreateRequest(
                "newadmin",
                "SecureP@ss123",
                "ADMIN",
                "admin@shop.com",
                UUID.randomUUID()
        );
        when(userService.createUser(any(UserCreateRequest.class)))
                .thenThrow(new BadRequestException("El ecommerce no existe"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(userService, times(1)).createUser(any(UserCreateRequest.class));
    }

    /**
     * CRITERIO-1.3: Rechazar creación si username es globalmente duplicado
     * Error Path: respuesta 409 Conflict
     */
    @Test
    void testCreateUser_usernameDuplicate() throws Exception {
        // Arrange
        UserCreateRequest request = new UserCreateRequest(
                "existingadmin",
                "SecureP@ss123",
                "ADMIN",
                "admin@shop.com",
                testEcommerceId
        );
        when(userService.createUser(any(UserCreateRequest.class)))
                .thenThrow(new ConflictException(
                        "El username ya existe en otra organización, debe ser único globalmente"
                ));

        // Act & Assert
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());

        verify(userService, times(1)).createUser(any(UserCreateRequest.class));
    }

    /**
     * Edge case: request con username vacío
     */
    @Test
    void testCreateUser_invalidUsername() throws Exception {
        // Arrange
        String invalidRequest = """
            {
              "username": "",
              "password": "SecureP@ss123",
              "role": "ADMIN",
              "ecommerceId": "%s"
            }
            """.formatted(testEcommerceId);

        // Act & Assert
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
            .andExpect(status().isBadRequest());
    }

    /**
     * Edge case: request con password demasiado corto
     */
    @Test
    void testCreateUser_weakPassword() throws Exception {
        // Arrange
        String invalidRequest = """
            {
              "username": "newuser",
              "password": "short",
              "role": "ADMIN",
              "ecommerceId": "%s"
            }
            """.formatted(testEcommerceId);

        // Act & Assert
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
            .andExpect(status().isBadRequest());
    }

    // ========== GET /api/v1/users ==========

    /**
     * CRITERIO-3.1: Listar usuarios de un ecommerce exitosamente
     * Happy Path: respuesta 200 OK con array de usuarios
     */
    @Test
    void testListUsers_success() throws Exception {
        // Arrange
        UserResponse user1 = buildUserResponse(UUID.randomUUID(), "admin1", testEcommerceId);
        UserResponse user2 = buildUserResponse(UUID.randomUUID(), "admin2", testEcommerceId);
        List<UserResponse> expectedUsers = Arrays.asList(user1, user2);
        when(userService.listUsers(testEcommerceId)).thenReturn(expectedUsers);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users")
                .param("ecommerceId", testEcommerceId.toString())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].username").value("admin1"))
            .andExpect(jsonPath("$[1].username").value("admin2"));

        verify(userService, times(1)).listUsers(testEcommerceId);
    }

    /**
     * CRITERIO-3.2: Listar sin parámetro ecommerceId (Super Admin ve todos)
     * Happy Path: respuesta 200 OK
     */
    @Test
    void testListUsers_noFilter() throws Exception {
        // Arrange
        UserResponse user1 = buildUserResponse(UUID.randomUUID(), "admin1", testEcommerceId);
        UserResponse user2 = buildUserResponse(UUID.randomUUID(), "admin2", testEcommerce2Id);
        List<UserResponse> expectedUsers = Arrays.asList(user1, user2);
        when(userService.listUsers(null)).thenReturn(expectedUsers);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));

        verify(userService, times(1)).listUsers(null);
    }

    /**
     * CRITERIO-2.3: Bloquear acceso cruzado entre ecommerce
     * Error Path: respuesta 403 Forbidden
     */
    @Test
    void testListUsers_crossEcommerceAccessDenied() throws Exception {
        // Arrange
        when(userService.listUsers(testEcommerce2Id))
                .thenThrow(new AuthorizationException(
                        "No tiene permiso para acceder a este ecommerce"
                ));

        // Act & Assert
        mockMvc.perform(get("/api/v1/users")
                .param("ecommerceId", testEcommerce2Id.toString())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());

        verify(userService, times(1)).listUsers(testEcommerce2Id);
    }

    // ========== GET /api/v1/users/{uid} ==========

    /**
     * Happy Path: obtener un usuario por uid
     */
    @Test
    void testGetUser_success() throws Exception {
        // Arrange
        UserResponse expectedUser = buildUserResponse(testUserId, "admin1", testEcommerceId);
        when(userService.getUserByUid(testUserId)).thenReturn(expectedUser);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/{uid}", testUserId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.uid").value(testUserId.toString()))
            .andExpect(jsonPath("$.username").value("admin1"));

        verify(userService, times(1)).getUserByUid(testUserId);
    }

    /**
     * Error Path: usuario no existe
     */
    @Test
    void testGetUser_notFound() throws Exception {
        // Arrange
        when(userService.getUserByUid(testUserId))
                .thenThrow(new ResourceNotFoundException("Usuario no encontrado"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/{uid}", testUserId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());

        verify(userService, times(1)).getUserByUid(testUserId);
    }

    /**
     * CRITERIO-2.1: Bloquear acceso a usuario de otro ecommerce
     */
    @Test
    void testGetUser_crossEcommerceAccessDenied() throws Exception {
        // Arrange
        when(userService.getUserByUid(testUserId))
                .thenThrow(new AuthorizationException(
                        "No tiene permiso para acceder a este usuario"
                ));

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/{uid}", testUserId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());

        verify(userService, times(1)).getUserByUid(testUserId);
    }

    // ========== PUT /api/v1/users/{uid} ==========

    /**
     * CRITERIO-4.1: Cambiar ecommerce de usuario exitosamente
     * Happy Path: respuesta 200 OK
     */
    @Test
    void testUpdateUser_changeEcommerce() throws Exception {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest(null, testEcommerce2Id);
        UserResponse expectedResponse = buildUserResponse(testUserId, "admin1", testEcommerce2Id);
        when(userService.updateUser(testUserId, request)).thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{uid}", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ecommerceId").value(testEcommerce2Id.toString()));

        verify(userService, times(1)).updateUser(testUserId, request);
    }

    /**
     * CRITERIO-4.2: Actualizar solo campos permitidos (username y ecommerceId)
     * Happy Path: respuesta 200 OK
     */
    @Test
    void testUpdateUser_changeUsername() throws Exception {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest("newusername", null);
        UserResponse expectedResponse = buildUserResponse(testUserId, "newusername", testEcommerceId);
        when(userService.updateUser(testUserId, request)).thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{uid}", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("newusername"));

        verify(userService, times(1)).updateUser(testUserId, request);
    }

    /**
     * CRITERIO-4.3: Rechazar cambio a ecommerce inválido
     */
    @Test
    void testUpdateUser_invalidEcommerce() throws Exception {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest(null, UUID.randomUUID());
        when(userService.updateUser(testUserId, request))
                .thenThrow(new BadRequestException("El ecommerce no existe"));

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{uid}", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(userService, times(1)).updateUser(testUserId, request);
    }

    /**
     * CRITERIO-4.4: Rechazar si nuevo username es globalmente duplicado
     */
    @Test
    void testUpdateUser_usernameDuplicate() throws Exception {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest("existingusername", null);
        when(userService.updateUser(testUserId, request))
                .thenThrow(new ConflictException(
                        "El username ya existe en otra organización, debe ser único globalmente"
                ));

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{uid}", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());

        verify(userService, times(1)).updateUser(testUserId, request);
    }

    /**
     * Edge case: usuario no existe
     */
    @Test
    void testUpdateUser_notFound() throws Exception {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest("newname", null);
        when(userService.updateUser(testUserId, request))
                .thenThrow(new ResourceNotFoundException("Usuario no encontrado"));

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{uid}", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());

        verify(userService, times(1)).updateUser(testUserId, request);
    }

    // ========== DELETE /api/v1/users/{uid} ==========

    /**
     * CRITERIO-5.1: Eliminar usuario exitosamente
     * Happy Path: respuesta 204 No Content
     */
    @Test
    void testDeleteUser_success() throws Exception {
        // Arrange
        doNothing().when(userService).deleteUser(testUserId);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/users/{uid}", testUserId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteUser(testUserId);
    }

    /**
     * CRITERIO-5.2: No permitir que usuario se elimine a sí mismo
     */
    @Test
    void testDeleteUser_cannotDeleteSelf() throws Exception {
        // Arrange
        doThrow(new BadRequestException("No puede eliminarse a sí mismo"))
                .when(userService).deleteUser(testUserId);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/users/{uid}", testUserId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());

        verify(userService, times(1)).deleteUser(testUserId);
    }

    /**
     * CRITERIO-5.3: Retornar 404 si usuario no existe
     */
    @Test
    void testDeleteUser_notFound() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("Usuario no encontrado"))
                .when(userService).deleteUser(testUserId);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/users/{uid}", testUserId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());

        verify(userService, times(1)).deleteUser(testUserId);
    }
}
