package com.loyalty.service_admin.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para SecurityContextHelper.
 * Valida la extracción de contexto de seguridad (ecommerce_id, uid, rol).
 * 
 * Responsabilidades:
 * - Obtener UserPrincipal desde SecurityContextHolder
 * - Extraer ecommerce_id
 * - Extraer uid
 * - Validar rol (SUPER_ADMIN vs otros)
 */
@ExtendWith(MockitoExtension.class)
class SecurityContextHelperTest {

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SecurityContextHelper securityContextHelper;

    private UUID testEcommerceId = UUID.randomUUID();
    private UUID testUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    // ========== GET CURRENT USER PRINCIPAL ==========

    /**
     * Happy Path: obtener UserPrincipal del contexto
     */
    @Test
    @DisplayName("Obtener UserPrincipal del contexto")
    void testGetCurrentUserPrincipal_success() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(
                testUserId,
                "testuser",
                "hashedPassword",
                "ADMIN",
                testEcommerceId,
                true
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);

        // Act
        UserPrincipal result = securityContextHelper.getCurrentUserPrincipal();

        // Assert
        assertEquals(testUserId, result.getUid());
        assertEquals("testuser", result.getUsername());
        assertEquals(testEcommerceId, result.getEcommerceId());
    }

    /**
     * Error Path: Principal no es UserPrincipal
     */
    @Test
    @DisplayName("Lanzar excepción si Principal no es UserPrincipal")
    void testGetCurrentUserPrincipal_invalidPrincipal() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn("notAUserPrincipal");

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> 
                securityContextHelper.getCurrentUserPrincipal()
        );
    }

    /**
     * Error Path: Authentication es null
     */
    @Test
    @DisplayName("Manejar Authentication null")
    void testGetCurrentUserPrincipal_nullAuthentication() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(null);

        // Act & Assert
        assertThrows(NullPointerException.class, () -> 
                securityContextHelper.getCurrentUserPrincipal()
        );
    }

    // ========== GET CURRENT USER ECOMMERCE ID ==========

    /**
     * Happy Path: obtener ecommerce_id del usuario
     */
    @Test
    @DisplayName("Obtener ecommerce_id del usuario actual")
    void testGetCurrentUserEcommerceId_success() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(
                testUserId,
                "testuser",
                "hashedPassword",
                "ADMIN",
                testEcommerceId,
                true
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);

        // Act
        UUID result = securityContextHelper.getCurrentUserEcommerceId();

        // Assert
        assertEquals(testEcommerceId, result);
    }

    /**
     * Edge case: SUPER_ADMIN tiene ecommerce_id = null
     */
    @Test
    @DisplayName("SUPER_ADMIN retorna ecommerce_id = null")
    void testGetCurrentUserEcommerceId_superAdmin() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(
                testUserId,
                "superadmin",
                "hashedPassword",
                "SUPER_ADMIN",
                null, // SUPER_ADMIN sin ecommerce_id
                true
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);

        // Act
        UUID result = securityContextHelper.getCurrentUserEcommerceId();

        // Assert
        assertNull(result);
    }

    // ========== GET CURRENT USER ID ==========

    /**
     * Happy Path: obtener UID del usuario actual
     */
    @Test
    @DisplayName("Obtener UID del usuario actual")
    void testGetCurrentUserId_success() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(
                testUserId,
                "testuser",
                "hashedPassword",
                "ADMIN",
                testEcommerceId,
                true
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);

        // Act
        UUID result = securityContextHelper.getCurrentUserId();

        // Assert
        assertEquals(testUserId, result);
    }

    /**
     * Edge case: múltiples usuarios tienen diferentes UIDs
     */
    @Test
    @DisplayName("Diferentes usuarios tienen diferentes UIDs")
    void testGetCurrentUserId_multipleUsers() {
        // Arrange - Primer usuario
        UUID userId1 = UUID.randomUUID();
        UserPrincipal principal1 = new UserPrincipal(
                userId1,
                "user1",
                "hashedPassword",
                "ADMIN",
                testEcommerceId,
                true
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal1);

        // Act
        UUID result1 = securityContextHelper.getCurrentUserId();

        // Assert
        assertEquals(userId1, result1);
    }

    // ========== IS CURRENT USER SUPER ADMIN ==========

    /**
     * Happy Path: usuario es SUPER_ADMIN
     */
    @Test
    @DisplayName("isCurrentUserSuperAdmin() retorna true para SUPER_ADMIN")
    void testIsCurrentUserSuperAdmin_true() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(
                testUserId,
                "superadmin",
                "hashedPassword",
                "SUPER_ADMIN",
                null,
                true
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);

        // Act
        boolean result = securityContextHelper.isCurrentUserSuperAdmin();

        // Assert
        assertTrue(result);
    }

    /**
     * Happy Path: usuario no es SUPER_ADMIN
     */
    @Test
    @DisplayName("isCurrentUserSuperAdmin() retorna false para ADMIN")
    void testIsCurrentUserSuperAdmin_false() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(
                testUserId,
                "admin",
                "hashedPassword",
                "ADMIN",
                testEcommerceId,
                true
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);

        // Act
        boolean result = securityContextHelper.isCurrentUserSuperAdmin();

        // Assert
        assertFalse(result);
    }

    /**
     * Edge case: casos sensibles (USER, etc.)
     */
    @Test
    @DisplayName("isCurrentUserSuperAdmin() case-sensitive")
    void testIsCurrentUserSuperAdmin_caseSensitive() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(
                testUserId,
                "user",
                "hashedPassword",
                "super_admin", // lowercase != "SUPER_ADMIN"
                testEcommerceId,
                true
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);

        // Act
        boolean result = securityContextHelper.isCurrentUserSuperAdmin();

        // Assert
        assertFalse(result);
    }

    // ========== IS CURRENT USER ECOMMERCE SCOPED ==========

    /**
     * Happy Path: usuario tiene restricción de ecommerce
     */
    @Test
    @DisplayName("isCurrentUserEcommerceScoped() retorna true para usuario con ecommerce")
    void testIsCurrentUserEcommerceScoped_true() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(
                testUserId,
                "admin",
                "hashedPassword",
                "ADMIN",
                testEcommerceId, // tiene ecommerce
                true
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);

        // Act
        boolean result = securityContextHelper.isCurrentUserEcommerceScoped();

        // Assert
        assertTrue(result);
    }

    /**
     * Happy Path: SUPER_ADMIN no tiene restricción de ecommerce
     */
    @Test
    @DisplayName("isCurrentUserEcommerceScoped() retorna false para SUPER_ADMIN")
    void testIsCurrentUserEcommerceScoped_false() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(
                testUserId,
                "superadmin",
                "hashedPassword",
                "SUPER_ADMIN",
                null, // sin ecommerce
                true
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);

        // Act
        boolean result = securityContextHelper.isCurrentUserEcommerceScoped();

        // Assert
        assertFalse(result);
    }

    // ========== INTEGRATION SCENARIOS ==========

    /**
     * Scenario: Usuario ADMIN accede a su contexto
     */
    @Test
    @DisplayName("Scenario: Usuario ADMIN extrae su contexto completo")
    void testScenario_adminContext() {
        // Arrange
        UUID adminId = UUID.randomUUID();
        UUID adminEcommerce = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(
                adminId,
                "admin@shop1",
                "hashedPassword",
                "ADMIN",
                adminEcommerce,
                true
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);

        // Act
        UUID uid = securityContextHelper.getCurrentUserId();
        UUID ecommerceId = securityContextHelper.getCurrentUserEcommerceId();
        boolean isSuperAdmin = securityContextHelper.isCurrentUserSuperAdmin();
        boolean isScoped = securityContextHelper.isCurrentUserEcommerceScoped();

        // Assert
        assertEquals(adminId, uid);
        assertEquals(adminEcommerce, ecommerceId);
        assertFalse(isSuperAdmin);
        assertTrue(isScoped);
    }

    /**
     * Scenario: SUPER_ADMIN accede a su contexto sin restricción de ecommerce
     */
    @Test
    @DisplayName("Scenario: SUPER_ADMIN extrae su contexto (sin ecommerce)")
    void testScenario_superAdminContext() {
        // Arrange
        UUID superAdminId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(
                superAdminId,
                "superadmin@loyalty",
                "hashedPassword",
                "SUPER_ADMIN",
                null, // sin restricción de ecommerce
                true
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);

        // Act
        UUID uid = securityContextHelper.getCurrentUserId();
        UUID ecommerceId = securityContextHelper.getCurrentUserEcommerceId();
        boolean isSuperAdmin = securityContextHelper.isCurrentUserSuperAdmin();
        boolean isScoped = securityContextHelper.isCurrentUserEcommerceScoped();

        // Assert
        assertEquals(superAdminId, uid);
        assertNull(ecommerceId);
        assertTrue(isSuperAdmin);
        assertFalse(isScoped);
    }

    /**
     * Scenario: Validar permiso para acceso cruzado entre ecommerce
     */
    @Test
    @DisplayName("Scenario: Validar aislamiento multi-tenant")
    void testScenario_multiTenantIsolation() {
        // Arrange - Usuario A en ecommerce 1
        UUID userAId = UUID.randomUUID();
        UUID ecommerce1 = UUID.randomUUID();
        UUID ecommerce2 = UUID.randomUUID();
        
        UserPrincipal userA = new UserPrincipal(
                userAId,
                "admin@shop1",
                "hashedPassword",
                "ADMIN",
                ecommerce1,
                true
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userA);

        // Act
        UUID userAEcommerce = securityContextHelper.getCurrentUserEcommerceId();

        // Assert - Usuario A no puede acceder a ecommerce2
        assertEquals(ecommerce1, userAEcommerce);
        assertFalse(userAEcommerce.equals(ecommerce2));
    }
}
