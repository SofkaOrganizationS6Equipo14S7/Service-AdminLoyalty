package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.port.in.UserDeleteUseCase;
import com.loyalty.service_admin.application.port.out.UserDeletePersistencePort;
import com.loyalty.service_admin.domain.entity.RoleEntity;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.infrastructure.exception.AuthorizationException;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException;
import com.loyalty.service_admin.infrastructure.exception.UnauthorizedException;
import com.loyalty.service_admin.infrastructure.security.SecurityContextHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.InOrder;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit Tests para UserDeleteService (TDD).
 *
 * Responsabilidad: Validar la lógica de negocio del hard delete de usuarios.
 * - Autorización (rol y ecommerce)
 * - Prohibición de auto-eliminación
 * - Auditoría previa
 * - Ejecución del hard delete físico
 *
 * Patrón: Mockea puertos (UserDeletePersistencePort, AuditService, SecurityContextHelper)
 * para aislar la lógica de negocio y verificar comportamiento correcto.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserDeleteService Unit Tests (TDD)")
@MockitoSettings(strictness = Strictness.LENIENT)
class UserDeleteServiceTest {

    @Mock
    private UserDeletePersistencePort userDeletePersistencePort;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private UserDeleteService userDeleteService;

    private UUID superAdminUid;
    private UUID storeAdminUid;
    private UUID standardUserUid;
    private UUID ecommerceId;
    private UUID ecommerceIdOther;
    private UUID roleIdSuperAdmin;
    private UUID roleIdStoreAdmin;
    private UUID roleIdStandard;

    private RoleEntity superAdminRole;
    private RoleEntity storeAdminRole;
    private RoleEntity standardRole;

    private UserEntity superAdminUser;
    private UserEntity storeAdminUser;
    private UserEntity targetUserSameEcommerce;
    private UserEntity targetUserOtherEcommerce;

    @BeforeEach
    void setUp() {
        // Preparar datos de prueba
        superAdminUid = UUID.randomUUID();
        storeAdminUid = UUID.randomUUID();
        standardUserUid = UUID.randomUUID();
        ecommerceId = UUID.randomUUID();
        ecommerceIdOther = UUID.randomUUID();

        roleIdSuperAdmin = UUID.randomUUID();
        roleIdStoreAdmin = UUID.randomUUID();
        roleIdStandard = UUID.randomUUID();

        // Crear roles
        superAdminRole = new RoleEntity();
        superAdminRole.setId(roleIdSuperAdmin);
        superAdminRole.setName("SUPER_ADMIN");

        storeAdminRole = new RoleEntity();
        storeAdminRole.setId(roleIdStoreAdmin);
        storeAdminRole.setName("STORE_ADMIN");

        standardRole = new RoleEntity();
        standardRole.setId(roleIdStandard);
        standardRole.setName("STANDARD");

        // Crear usuario SUPER_ADMIN
        superAdminUser = new UserEntity();
        superAdminUser.setId(superAdminUid);
        superAdminUser.setUsername("super-admin");
        superAdminUser.setEmail("super@loyalty.com");
        superAdminUser.setRole(superAdminRole);
        superAdminUser.setEcommerceId(ecommerceId);
        superAdminUser.setIsActive(true);
        superAdminUser.setCreatedAt(Instant.now().minusSeconds(3600));
        superAdminUser.setUpdatedAt(Instant.now());

        // Crear usuario STORE_ADMIN
        storeAdminUser = new UserEntity();
        storeAdminUser.setId(storeAdminUid);
        storeAdminUser.setUsername("store-admin");
        storeAdminUser.setEmail("store@loyalty.com");
        storeAdminUser.setRole(storeAdminRole);
        storeAdminUser.setEcommerceId(ecommerceId);
        storeAdminUser.setIsActive(true);
        storeAdminUser.setCreatedAt(Instant.now().minusSeconds(3600));
        storeAdminUser.setUpdatedAt(Instant.now());

        // Crear usuario STANDARD a eliminar (mismo ecommerce)
        targetUserSameEcommerce = new UserEntity();
        targetUserSameEcommerce.setId(standardUserUid);
        targetUserSameEcommerce.setUsername("target-user");
        targetUserSameEcommerce.setEmail("target@loyalty.com");
        targetUserSameEcommerce.setRole(standardRole);
        targetUserSameEcommerce.setEcommerceId(ecommerceId);
        targetUserSameEcommerce.setIsActive(true);
        targetUserSameEcommerce.setCreatedAt(Instant.now().minusSeconds(3600));
        targetUserSameEcommerce.setUpdatedAt(Instant.now());

        // Crear usuario STANDARD a eliminar (otro ecommerce)
        targetUserOtherEcommerce = new UserEntity();
        targetUserOtherEcommerce.setId(UUID.randomUUID());
        targetUserOtherEcommerce.setUsername("target-user-other");
        targetUserOtherEcommerce.setEmail("target-other@loyalty.com");
        targetUserOtherEcommerce.setRole(standardRole);
        targetUserOtherEcommerce.setEcommerceId(ecommerceIdOther);
        targetUserOtherEcommerce.setIsActive(true);
        targetUserOtherEcommerce.setCreatedAt(Instant.now().minusSeconds(3600));
        targetUserOtherEcommerce.setUpdatedAt(Instant.now());
    }

    // ==================== CRITERIO-2.5.1: Happy Path - SUPER_ADMIN ====================

    /**
     * CRITERIO-2.5.1: Hard delete exitoso de usuario estándar por SUPER_ADMIN
     *
     * Escenario: Un SUPER_ADMIN autenticado elimina un usuario existente de su ecommerce
     * - Given: SUPER_ADMIN está autenticado y es el usuario actual
     * - And: Existe un usuario STANDARD (target) en el mismo ecommerce
     * - When: Se ejecuta hardDeleteUser(targetUserUid)
     * - Then: 
     *   - El usuario target se recupera del repositorio
     *   - AuditService registra la auditoría previa
     *   - UserDeletePersistencePort elimina al usuario
     *   - No se lanza excepción
     */
    @Test
    @DisplayName("testHardDeleteUser_Success_SUPER_ADMIN_DeletesStandardUser")
    void testHardDeleteUser_Success_SUPER_ADMIN_DeletesStandardUser() {
        // Arrange
        when(securityContextHelper.getCurrentUserUid()).thenReturn(superAdminUid);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);
        doReturn(true).when(securityContextHelper).canActOnUser(eq(ecommerceId), eq(targetUserSameEcommerce.getId()));

        when(userDeletePersistencePort.findById(targetUserSameEcommerce.getId()))
            .thenReturn(Optional.of(targetUserSameEcommerce));

        // Act
        userDeleteService.hardDeleteUser(targetUserSameEcommerce.getId());

        // Assert
        verify(userDeletePersistencePort, times(1)).findById(targetUserSameEcommerce.getId());
        verify(auditService, times(1)).auditUserDeletion(targetUserSameEcommerce, superAdminUid);
        verify(userDeletePersistencePort, times(1)).deleteUser(targetUserSameEcommerce);
    }

    // ==================== CRITERIO-2.5.2: Happy Path - STORE_ADMIN ====================

    /**
     * CRITERIO-2.5.2: Hard delete exitoso por STORE_ADMIN en su ecommerce
     *
     * Escenario: Un STORE_ADMIN autenticado elimina un usuario de su propio ecommerce
     * - Given: STORE_ADMIN está autenticado y pertenece a ecommerceId
     * - And: Target user pertenece al mismo ecommerceId
     * - When: Se ejecuta hardDeleteUser(targetUserUid)
     * - Then:
     *   - canActOnUser retorna true (STORE_ADMIN su ecommerce)
     *   - Auditoría se registra con los datos correctos
     *   - Hard delete se ejecuta
     */
    @Test
    @DisplayName("testHardDeleteUser_Success_STORE_ADMIN_OwnEcommerce")
    void testHardDeleteUser_Success_STORE_ADMIN_OwnEcommerce() {
        // Arrange
        when(securityContextHelper.getCurrentUserUid()).thenReturn(storeAdminUid);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("STORE_ADMIN");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);
        doReturn(true).when(securityContextHelper).canActOnUser(eq(ecommerceId), eq(targetUserSameEcommerce.getId()));

        when(userDeletePersistencePort.findById(targetUserSameEcommerce.getId()))
            .thenReturn(Optional.of(targetUserSameEcommerce));

        // Act
        userDeleteService.hardDeleteUser(targetUserSameEcommerce.getId());

        // Assert
        verify(userDeletePersistencePort, times(1)).findById(targetUserSameEcommerce.getId());
        verify(auditService, times(1)).auditUserDeletion(targetUserSameEcommerce, storeAdminUid);
        verify(userDeletePersistencePort, times(1)).deleteUser(targetUserSameEcommerce);
    }

    // ==================== CRITERIO-2.5.3: Error Path - Self-Delete ====================

    /**
     * CRITERIO-2.5.3: Prevenir auto-eliminación (hard delete prohibido de sí mismo)
     *
     * Escenario: Un usuario intenta eliminarse a sí mismo
     * - Given: currentUserUid == targetUserUid, usuario autenticado
     * - When: Se ejecuta hardDeleteUser(currentUserUid)
     * - Then:
     *   - BadRequestException es lanzada con mensaje "No puede eliminarse a sí mismo"
     *   - auditService.auditUserDeletion() NO es llamado
     *   - deleteUser() NO es llamado
     */
    @Test
    @DisplayName("testHardDeleteUser_SelfDelete_ThrowsBadRequestException")
    void testHardDeleteUser_SelfDelete_ThrowsBadRequestException() {
        // Arrange
        when(securityContextHelper.getCurrentUserUid()).thenReturn(superAdminUid);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);

        // Target user es el mismo usuario actual
        when(userDeletePersistencePort.findById(superAdminUid))
            .thenReturn(Optional.of(superAdminUser));

        // Act & Assert
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> userDeleteService.hardDeleteUser(superAdminUid)
        );

        assertEquals("No puede eliminarse a sí mismo", exception.getMessage());
        verify(auditService, never()).auditUserDeletion(any(), any());
        verify(userDeletePersistencePort, never()).deleteUser(any());
    }

    // ==================== CRITERIO-2.5.6: Error Path - Standard User Cannot Delete ====================

    /**
     * CRITERIO-2.5.6: Validar que solo ADMIN roles pueden eliminar
     *
     * Escenario: Usuario STANDARD intenta eliminar otro usuario
     * - Given: currentUser es STANDARD role
     * - When: Se ejecuta hardDeleteUser(otherUserUid)
     * - Then:
     *   - AuthorizationException es lanzada
     *   - canActOnUser retorna false
     *   - auditService y deleteUser NO son llamados
     */
    @Test
    @DisplayName("testHardDeleteUser_StandardUserCannotDelete_ThrowsAuthorizationException")
    void testHardDeleteUser_StandardUserCannotDelete_ThrowsAuthorizationException() {
        // Arrange
        UUID standardUid = UUID.randomUUID();
        when(securityContextHelper.getCurrentUserUid()).thenReturn(standardUid);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("STANDARD");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);
        doReturn(false).when(securityContextHelper).canActOnUser(eq(ecommerceId), eq(targetUserSameEcommerce.getId()));

        when(userDeletePersistencePort.findById(targetUserSameEcommerce.getId()))
            .thenReturn(Optional.of(targetUserSameEcommerce));

        // Act & Assert
        AuthorizationException exception = assertThrows(
            AuthorizationException.class,
            () -> userDeleteService.hardDeleteUser(targetUserSameEcommerce.getId())
        );

        assertNotNull(exception.getMessage());
        verify(auditService, never()).auditUserDeletion(any(), any());
        verify(userDeletePersistencePort, never()).deleteUser(any());
    }

    // ==================== CRITERIO-2.5.5: Error Path - Cross-Ecommerce Access ====================

    /**
     * CRITERIO-2.5.5: Prevenir acceso cruzado entre ecommerce
     *
     * Escenario: STORE_ADMIN intenta eliminar usuario de otro ecommerce
     * - Given: STORE_ADMIN autenticado con ecommerceId1
     * - And: Target user pertenece a ecommerceId2 (diferente)
     * - When: Se ejecuta hardDeleteUser(targetUserUid)
     * - Then:
     *   - canActOnUser retorna false
     *   - AuthorizationException es lanzada
     *   - auditService y deleteUser NO son llamados
     */
    @Test
    @DisplayName("testHardDeleteUser_StoreAdminCrossEcommerce_ThrowsAuthorizationException")
    void testHardDeleteUser_StoreAdminCrossEcommerce_ThrowsAuthorizationException() {
        // Arrange
        UUID otherStoreAdminUid = UUID.randomUUID();
        when(securityContextHelper.getCurrentUserUid()).thenReturn(otherStoreAdminUid);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("STORE_ADMIN");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);
        doReturn(false).when(securityContextHelper).canActOnUser(eq(ecommerceId), eq(targetUserOtherEcommerce.getId()));

        when(userDeletePersistencePort.findById(targetUserOtherEcommerce.getId()))
            .thenReturn(Optional.of(targetUserOtherEcommerce));

        // Act & Assert
        AuthorizationException exception = assertThrows(
            AuthorizationException.class,
            () -> userDeleteService.hardDeleteUser(targetUserOtherEcommerce.getId())
        );

        assertNotNull(exception.getMessage());
        verify(auditService, never()).auditUserDeletion(any(), any());
        verify(userDeletePersistencePort, never()).deleteUser(any());
    }

    // ==================== CRITERIO-2.5.4: Error Path - User Not Found ====================

    /**
     * CRITERIO-2.5.4: Hard delete de usuario inexistente
     *
     * Escenario: Se intenta eliminar usuario que no existe
     * - Given: SUPER_ADMIN autenticado
     * - When: Se ejecuta hardDeleteUser(nonExistentUuid)
     * - Then:
     *   - userDeletePersistencePort.findById() retorna Optional.empty()
     *   - ResourceNotFoundException es lanzada con "Usuario no encontrado"
     *   - auditService y deleteUser NO son llamados
     */
    @Test
    @DisplayName("testHardDeleteUser_UserNotFound_ThrowsResourceNotFoundException")
    void testHardDeleteUser_UserNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        UUID nonExistentUid = UUID.randomUUID();
        when(securityContextHelper.getCurrentUserUid()).thenReturn(superAdminUid);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);

        when(userDeletePersistencePort.findById(nonExistentUid))
            .thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> userDeleteService.hardDeleteUser(nonExistentUid)
        );

        assertEquals("Usuario no encontrado", exception.getMessage());
        verify(auditService, never()).auditUserDeletion(any(), any());
        verify(userDeletePersistencePort, never()).deleteUser(any());
    }

    // ==================== Edge Case: Unauthenticated User ====================

    /**
     * Edge Case: Usuario no autenticado
     *
     * Escenario: No hay contexto de seguridad (usuario no autenticado)
     * - Given: SecurityContextHelper.getCurrentUserUid() retorna null
     * - When: Se ejecuta hardDeleteUser(targetUserUid)
     * - Then:
     *   - UnauthorizedException es lanzada
     *   - auditService y deleteUser NO son llamados
     */
    @Test
    @DisplayName("testHardDeleteUser_NoAuthenticationContext_ThrowsUnauthorizedException")
    void testHardDeleteUser_NoAuthenticationContext_ThrowsUnauthorizedException() {
        // Arrange
        when(securityContextHelper.getCurrentUserUid()).thenThrow(new RuntimeException("No auth context"));

        // Act & Assert
        UnauthorizedException exception = assertThrows(
            UnauthorizedException.class,
            () -> userDeleteService.hardDeleteUser(targetUserSameEcommerce.getId())
        );

        assertEquals("Usuario no autenticado", exception.getMessage());
        verify(auditService, never()).auditUserDeletion(any(), any());
        verify(userDeletePersistencePort, never()).deleteUser(any());
    }

    // ==================== Integration-like: Verify Audit Before Delete ====================

    /**
     * CRITERIO-2.5.7 (implícito): Registro de auditoría previa al delete
     *
     * Escenario: Debe verificarse que auditUserDeletion() se llama ANTES que deleteUser()
     * - Given: SUPER_ADMIN autenticado con usuario válido a eliminar
     * - When: Se ejecuta hardDeleteUser()
     * - Then:
     *   - auditService.auditUserDeletion() es llamado primero
     *   - userDeletePersistencePort.deleteUser() es llamado segundo
     *   - Orden de invocación es crítica para GDPR (auditoría previa)
     */
    @Test
    @DisplayName("testHardDeleteUser_CreatesAuditLogBeforeDelete")
    void testHardDeleteUser_CreatesAuditLogBeforeDelete() {
        // Arrange
        InOrder inOrder = inOrder(auditService, userDeletePersistencePort);

        when(securityContextHelper.getCurrentUserUid()).thenReturn(superAdminUid);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);
        doReturn(true).when(securityContextHelper).canActOnUser(eq(ecommerceId), eq(targetUserSameEcommerce.getId()));

        when(userDeletePersistencePort.findById(targetUserSameEcommerce.getId()))
            .thenReturn(Optional.of(targetUserSameEcommerce));

        // Act
        userDeleteService.hardDeleteUser(targetUserSameEcommerce.getId());

        // Assert - Verificar orden de invocación
        inOrder.verify(auditService, times(1)).auditUserDeletion(targetUserSameEcommerce, superAdminUid);
        inOrder.verify(userDeletePersistencePort, times(1)).deleteUser(targetUserSameEcommerce);
    }
}
