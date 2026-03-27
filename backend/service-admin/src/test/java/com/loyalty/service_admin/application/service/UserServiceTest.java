package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.UserCreateRequest;
import com.loyalty.service_admin.application.dto.UserResponse;
import com.loyalty.service_admin.application.dto.UserUpdateRequest;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.domain.repository.UserRepository;
import com.loyalty.service_admin.infrastructure.exception.AuthorizationException;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import com.loyalty.service_admin.infrastructure.exception.ConflictException;
import com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException;
import com.loyalty.service_admin.infrastructure.security.SecurityContextHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para UserService.
 * Cubre CRUD + validaciones de ecommerce + unicidad global de username + aislamiento multi-tenant.
 * 
 * Implementa SPEC-002:
 * - HU-01: Crear usuario vinculado a ecommerce
 * - HU-02: Validar acceso según ecommerce del usuario
 * - HU-03: Listar usuarios por ecommerce
 * - HU-04: Actualizar usuario (cambio de ecommerce)
 * - HU-05: Eliminar usuario
 * 
 * Cobertura: lógica de negocio, validaciones, reglas de negocio
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EcommerceService ecommerceService;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private UserService userService;

    // Fixtures
    private UUID testEcommerceId = UUID.randomUUID();
    private UUID testEcommerce2Id = UUID.randomUUID();
    private UUID testUserId = UUID.randomUUID();
    private UUID testUserId2 = UUID.randomUUID();
    private Long testUserLongId = 1L;
    private Long testUserLongId2 = 2L;

    private UserEntity buildUser(Long id, String username, UUID ecommerceId) {
        return UserEntity.builder()
                .id(id)
                .username(username)
                .password("hashedPassword")
                .role("ADMIN")
                .ecommerceId(ecommerceId)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    /**
     * Calcula el Long ID que esperaría el servicio a partir de un UUID
     * usando la misma fórmula que findByUidOrThrow
     */
    private Long getIdFromUuid(UUID uid) {
        return uid.getMostSignificantBits() % Integer.MAX_VALUE;
    }

    // ========== CREATE USER ==========

    /**
     * CRITERIO-1.1: Crear usuario exitosamente con ecommerce válido
     * Happy Path: usuario creado exitosamente
     */
    @Test
    @DisplayName("Crear usuario exitosamente con ecommerce válido")
    void testCreateUser_success() {
        // Arrange
        UserCreateRequest request = new UserCreateRequest(
                "newadmin",
                "SecureP@ss123",
                "ADMIN",
                "admin@shop.com",
                testEcommerceId
        );
        UserEntity savedUser = buildUser(1L, "newadmin", testEcommerceId);

        doNothing().when(ecommerceService).validateEcommerceExists(testEcommerceId);
        when(userRepository.findByUsername("newadmin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("SecureP@ss123")).thenReturn("hashedPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        // Act
        UserResponse result = userService.createUser(request);

        // Assert
        assertNotNull(result);
        assertEquals("newadmin", result.username());
        assertEquals(testEcommerceId, result.ecommerceId());
        assertTrue(result.active());
        verify(ecommerceService, times(1)).validateEcommerceExists(testEcommerceId);
        verify(userRepository, times(1)).findByUsername("newadmin");
        verify(userRepository, times(1)).save(any(UserEntity.class));
    }

    /**
     * CRITERIO-1.2: Rechazar creación si ecommerce no existe
     * Error Path: BadRequestException
     */
    @Test
    @DisplayName("Rechazar creación si ecommerce no existe")
    void testCreateUser_ecommerceNotFound() {
        // Arrange
        UserCreateRequest request = new UserCreateRequest(
                "newadmin",
                "SecureP@ss123",
                "ADMIN",
                "admin@shop.com",
                UUID.randomUUID()
        );

        doThrow(new BadRequestException("El ecommerce no existe"))
                .when(ecommerceService).validateEcommerceExists(request.ecommerceId());

        // Act & Assert
        assertThrows(BadRequestException.class, () -> userService.createUser(request));
        verify(ecommerceService, times(1)).validateEcommerceExists(request.ecommerceId());
        verify(userRepository, never()).save(any());
    }

    /**
     * CRITERIO-1.3: Rechazar creación si username es globalmente duplicado
     * Error Path: ConflictException (global uniqueness)
     */
    @Test
    @DisplayName("Rechazar creación si username es globalmente duplicado")
    void testCreateUser_usernameDuplicate() {
        // Arrange
        UserCreateRequest request = new UserCreateRequest(
                "existingadmin",
                "SecureP@ss123",
                "ADMIN",
                "admin@shop.com",
                testEcommerceId
        );
        UserEntity existingUser = buildUser(1L, "existingadmin", testEcommerce2Id);

        doNothing().when(ecommerceService).validateEcommerceExists(testEcommerceId);
        when(userRepository.findByUsername("existingadmin")).thenReturn(Optional.of(existingUser));

        // Act & Assert
        assertThrows(ConflictException.class, () -> userService.createUser(request));
        verify(ecommerceService, times(1)).validateEcommerceExists(testEcommerceId);
        verify(userRepository, times(1)).findByUsername("existingadmin");
        verify(userRepository, never()).save(any());
    }

    /**
     * Edge case: password hasheado antes de guardar
     */
    @Test
    @DisplayName("Password debe ser hasheado antes de guardar")
    void testCreateUser_passwordHashed() {
        // Arrange
        UserCreateRequest request = new UserCreateRequest(
                "newuser",
                "PlainPassword123",
                "ADMIN",
                "user@example.com",
                testEcommerceId
        );
        UserEntity savedUser = buildUser(1L, "newuser", testEcommerceId);

        doNothing().when(ecommerceService).validateEcommerceExists(testEcommerceId);
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("PlainPassword123")).thenReturn("$2a$10$hashedPasswordValue");
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        // Act
        userService.createUser(request);

        // Assert
        verify(passwordEncoder, times(1)).encode("PlainPassword123");
        verify(userRepository).save(any());
    }

    // ========== LIST USERS ==========

    /**
     * CRITERIO-3.1: Listar usuarios de un ecommerce exitosamente
     * Happy Path: Super Admin lista un ecommerce específico
     */
    @Test
    @DisplayName("Super Admin lista usuarios de un ecommerce específico")
    void testListUsers_superAdminFiltered() {
        // Arrange
        List<UserEntity> users = Arrays.asList(
                buildUser(1L, "admin1", testEcommerceId),
                buildUser(2L, "admin2", testEcommerceId)
        );
        when(securityContextHelper.isCurrentUserSuperAdmin()).thenReturn(true);
        when(userRepository.findByEcommerceId(testEcommerceId)).thenReturn(users);

        // Act
        List<UserResponse> result = userService.listUsers(testEcommerceId);

        // Assert
        assertEquals(2, result.size());
        assertEquals("admin1", result.get(0).username());
        assertEquals("admin2", result.get(1).username());
        verify(userRepository, times(1)).findByEcommerceId(testEcommerceId);
    }

    /**
     * CRITERIO-3.2: Listar sin parámetro ecommerceId (Super Admin ve todos)
     * Happy Path: Super Admin ve todos los usuarios de todos los ecommerce
     */
    @Test
    @DisplayName("Super Admin lista todos los usuarios sin filtro")
    void testListUsers_superAdminAll() {
        // Arrange
        List<UserEntity> allUsers = Arrays.asList(
                buildUser(1L, "admin1", testEcommerceId),
                buildUser(2L, "admin2", testEcommerce2Id),
                buildUser(3L, "admin3", testEcommerceId)
        );
        when(securityContextHelper.isCurrentUserSuperAdmin()).thenReturn(true);
        when(userRepository.findAll()).thenReturn(allUsers);

        // Act
        List<UserResponse> result = userService.listUsers(null);

        // Assert
        assertEquals(3, result.size());
        verify(userRepository, times(1)).findAll();
    }

    /**
     * CRITERIO-2.1: Usuario no-super-admin ve solo su ecommerce
     * Happy Path: Usuario ADMIN ve solo usuarios de su ecommerce
     */
    @Test
    @DisplayName("Usuario no-super-admin ve solo usuarios de su ecommerce")
    void testListUsers_nonSuperAdminFiltered() {
        // Arrange
        List<UserEntity> userEcommerceUsers = Arrays.asList(
                buildUser(1L, "admin1", testEcommerceId),
                buildUser(2L, "admin2", testEcommerceId)
        );
        when(securityContextHelper.isCurrentUserSuperAdmin()).thenReturn(false);
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(testEcommerceId);
        when(userRepository.findByEcommerceId(testEcommerceId)).thenReturn(userEcommerceUsers);

        // Act
        List<UserResponse> result = userService.listUsers(null);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(u -> u.ecommerceId().equals(testEcommerceId)));
        verify(userRepository, times(1)).findByEcommerceId(testEcommerceId);
    }

    /**
     * CRITERIO-2.3: Bloquear acceso cruzado entre ecommerce
     * Error Path: Usuario intenta filtrar otro ecommerce
     */
    @Test
    @DisplayName("Bloquear acceso cruzado a otro ecommerce")
    void testListUsers_crossEcommerceAccessDenied() {
        // Arrange
        when(securityContextHelper.isCurrentUserSuperAdmin()).thenReturn(false);
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(testEcommerceId);

        // Act & Assert
        assertThrows(AuthorizationException.class, () -> 
                userService.listUsers(testEcommerce2Id)
        );
        verify(userRepository, never()).findByEcommerceId(testEcommerce2Id);
    }

    // ========== GET USER BY UID ==========

    /**
     * Happy Path: obtener usuario por UID (super admin)
     */
    @Test
    @DisplayName("Obtener usuario por UID exitosamente")
    void testGetUserByUid_success() {
        // Arrange
        Long id = getIdFromUuid(testUserId);
        UserEntity user = buildUser(id, "admin1", testEcommerceId);
        when(securityContextHelper.isCurrentUserSuperAdmin()).thenReturn(true);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        // Act
        UserResponse result = userService.getUserByUid(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals("admin1", result.username());
        verify(userRepository, times(1)).findById(id);
    }

    /**
     * Error Path: usuario no existe
     */
    @Test
    @DisplayName("Retornar 404 si usuario no existe")
    void testGetUserByUid_notFound() {
        // Arrange
        Long id = getIdFromUuid(testUserId);
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> 
                userService.getUserByUid(testUserId)
        );
    }

    /**
     * Error Path: usuario no-super-admin intenta ver usuario de otro ecommerce
     */
    @Test
    @DisplayName("Bloquear acceso a usuario de otro ecommerce")
    void testGetUserByUid_crossEcommerceAccessDenied() {
        // Arrange
        Long id = getIdFromUuid(testUserId);
        UserEntity user = buildUser(id, "admin1", testEcommerce2Id);
        when(securityContextHelper.isCurrentUserSuperAdmin()).thenReturn(false);
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(testEcommerceId);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        // Act & Assert
        assertThrows(AuthorizationException.class, () -> 
                userService.getUserByUid(testUserId)
        );
    }

    // ========== UPDATE USER ==========

    /**
     * CRITERIO-4.1: Cambiar ecommerce de usuario exitosamente
     * Happy Path: actualizar ecommerce
     */
    @Test
    @DisplayName("Cambiar ecommerce de usuario exitosamente")
    void testUpdateUser_changeEcommerce() {
        // Arrange
        Long id = getIdFromUuid(testUserId);
        UserEntity user = buildUser(id, "admin1", testEcommerceId);
        UserUpdateRequest request = new UserUpdateRequest(null, testEcommerce2Id);
        UserEntity updatedUser = buildUser(id, "admin1", testEcommerce2Id);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        doNothing().when(ecommerceService).validateEcommerceExists(testEcommerce2Id);
        when(userRepository.save(any(UserEntity.class))).thenReturn(updatedUser);

        // Act
        UserResponse result = userService.updateUser(testUserId, request);

        // Assert
        assertNotNull(result);
        assertEquals(testEcommerce2Id, result.ecommerceId());
        verify(ecommerceService, times(1)).validateEcommerceExists(testEcommerce2Id);
        verify(userRepository, times(1)).save(any(UserEntity.class));
    }

    /**
     * CRITERIO-4.2: Actualizar solo campos permitidos (username y ecommerceId)
     * Happy Path: cambiar username
     */
    @Test
    @DisplayName("Actualizar username de usuario exitosamente")
    void testUpdateUser_changeUsername() {
        // Arrange
        Long id = getIdFromUuid(testUserId);
        UserEntity user = buildUser(id, "oldusername", testEcommerceId);
        UserUpdateRequest request = new UserUpdateRequest("newusername", null);
        UserEntity updatedUser = buildUser(id, "newusername", testEcommerceId);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("newusername")).thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class))).thenReturn(updatedUser);

        // Act
        UserResponse result = userService.updateUser(testUserId, request);

        // Assert
        assertEquals("newusername", result.username());
        verify(userRepository, times(1)).findByUsername("newusername");
        verify(userRepository, times(1)).save(any(UserEntity.class));
    }

    /**
     * CRITERIO-4.3: Rechazar cambio a ecommerce inválido
     */
    @Test
    @DisplayName("Rechazar cambio a ecommerce inválido")
    void testUpdateUser_invalidEcommerce() {
        // Arrange
        Long id = getIdFromUuid(testUserId);
        UserEntity user = buildUser(id, "admin1", testEcommerceId);
        UUID invalidEcommerceId = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest(null, invalidEcommerceId);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        doThrow(new BadRequestException("El ecommerce no existe"))
                .when(ecommerceService).validateEcommerceExists(invalidEcommerceId);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> 
                userService.updateUser(testUserId, request)
        );
        verify(userRepository, never()).save(any());
    }

    /**
     * CRITERIO-4.4: Rechazar si nuevo username es globalmente duplicado
     */
    @Test
    @DisplayName("Rechazar si nuevo username es globalmente duplicado")
    void testUpdateUser_usernameDuplicate() {
        // Arrange
        Long id = getIdFromUuid(testUserId);
        UserEntity user = buildUser(id, "admin1", testEcommerceId);
        UserEntity existingUser = buildUser(testUserLongId2, "existingusername", testEcommerce2Id);
        UserUpdateRequest request = new UserUpdateRequest("existingusername", null);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("existingusername")).thenReturn(Optional.of(existingUser));

        // Act & Assert
        assertThrows(ConflictException.class, () -> 
                userService.updateUser(testUserId, request)
        );
        verify(userRepository, never()).save(any());
    }

    /**
     * Edge case: usuario no existe
     */
    @Test
    @DisplayName("Retornar 404 si usuario a actualizar no existe")
    void testUpdateUser_notFound() {
        // Arrange
        Long id = getIdFromUuid(testUserId);
        UserUpdateRequest request = new UserUpdateRequest("newname", null);
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> 
                userService.updateUser(testUserId, request)
        );
    }

    // ========== DELETE USER ==========

    /**
     * CRITERIO-5.1: Eliminar usuario exitosamente
     * Happy Path: usuario eliminado
     */
    @Test
    @DisplayName("Eliminar usuario exitosamente")
    void testDeleteUser_success() {
        // Arrange
        Long id = getIdFromUuid(testUserId);
        UserEntity user = buildUser(id, "admin1", testEcommerceId);
        UUID currentUserId = UUID.randomUUID();

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(securityContextHelper.getCurrentUserId()).thenReturn(currentUserId);

        // Act
        userService.deleteUser(testUserId);

        // Assert
        verify(userRepository, times(1)).delete(user);
    }

    /**
     * CRITERIO-5.2: No permitir que usuario se elimine a sí mismo
     */
    @Test
    @DisplayName("No permitir auto-eliminación")
    void testDeleteUser_cannotDeleteSelf() {
        // Arrange
        Long id = getIdFromUuid(testUserId);
        UserEntity user = buildUser(id, "admin1", testEcommerceId);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(securityContextHelper.getCurrentUserId()).thenReturn(testUserId);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> 
                userService.deleteUser(testUserId)
        );
        verify(userRepository, never()).delete(any());
    }

    /**
     * CRITERIO-5.3: Retornar 404 si usuario no existe
     */
    @Test
    @DisplayName("Retornar 404 si usuario a eliminar no existe")
    void testDeleteUser_notFound() {
        // Arrange
        Long id = getIdFromUuid(testUserId);
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> 
                userService.deleteUser(testUserId)
        );
    }
}
