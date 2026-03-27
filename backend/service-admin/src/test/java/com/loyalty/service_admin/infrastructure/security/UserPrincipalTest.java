package com.loyalty.service_admin.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests unitarios para UserPrincipal.
 * Valida el contrato UserDetails de Spring Security.
 * 
 * Responsabilidades:
 * - Almacenar uid del usuario
 * - Almacenar ecommerce_id para aislamiento multi-tenant
 * - Implementar UserDetails correctamente
 */
class UserPrincipalTest {

    private UUID testEcommerceId = UUID.randomUUID();

    private UserPrincipal buildPrincipal(
            UUID uid,
            String username,
            String role,
            UUID ecommerceId,
            boolean enabled
    ) {
        return new UserPrincipal(
                uid,
                username,
                "hashedPassword",
                role,
                ecommerceId,
                enabled
        );
    }

    // ========== CONSTRUCTORES Y GETTERS ==========

    /**
     * Happy Path: crear UserPrincipal con todos los parámetros
     */
    @Test
    @DisplayName("Crear UserPrincipal exitosamente")
    void testConstruct_success() {
        // Arrange & Act
        UUID uid = UUID.randomUUID();
        UserPrincipal principal = buildPrincipal(
                uid,
                "testuser",
                "ADMIN",
                testEcommerceId,
                true
        );

        // Assert
        assertEquals(uid, principal.getUid());
        assertEquals("testuser", principal.getUsername());
        assertEquals("ADMIN", principal.getRole());
        assertEquals(testEcommerceId, principal.getEcommerceId());
        assertTrue(principal.isEnabled());
    }

    /**
     * Edge case: UserPrincipal para SUPER_ADMIN (ecommerce_id es null)
     */
    @Test
    @DisplayName("Crear UserPrincipal para SUPER_ADMIN sin ecommerce_id")
    void testConstruct_superAdmin() {
        // Arrange & Act
        UUID uid = UUID.randomUUID();
        UserPrincipal principal = buildPrincipal(
                uid,
                "superadmin",
                "SUPER_ADMIN",
                null, // SUPER_ADMIN no tiene ecommerce_id
                true
        );

        // Assert
        assertNull(principal.getEcommerceId());
        assertEquals("SUPER_ADMIN", principal.getRole());
    }

    // ========== USERDETAILS CONTRACT ==========

    /**
     * Spring Security contract: getPassword() retorna contraseña hasheada
     */
    @Test
    @DisplayName("getPassword() retorna contraseña hasheada")
    void testGetPassword() {
        // Act
        UserPrincipal principal = buildPrincipal(
                UUID.randomUUID(),
                "testuser",
                "ADMIN",
                testEcommerceId,
                true
        );

        // Assert
        assertEquals("hashedPassword", principal.getPassword());
    }

    /**
     * Spring Security contract: isEnabled() retorna estado del usuario
     */
    @Test
    @DisplayName("isEnabled() retorna estado del usuario")
    void testIsEnabled() {
        // Arrange
        UserPrincipal enabledPrincipal = buildPrincipal(
                UUID.randomUUID(),
                "activeuser",
                "ADMIN",
                testEcommerceId,
                true
        );
        UserPrincipal disabledPrincipal = buildPrincipal(
                UUID.randomUUID(),
                "inactiveuser",
                "ADMIN",
                testEcommerceId,
                false
        );

        // Assert
        assertTrue(enabledPrincipal.isEnabled());
        assertFalse(disabledPrincipal.isEnabled());
    }

    /**
     * Spring Security contract: isAccountNonExpired() siempre true
     */
    @Test
    @DisplayName("isAccountNonExpired() siempre true")
    void testIsAccountNonExpired() {
        // Act
        UserPrincipal principal = buildPrincipal(
                UUID.randomUUID(),
                "testuser",
                "ADMIN",
                testEcommerceId,
                true
        );

        // Assert
        assertTrue(principal.isAccountNonExpired());
    }

    /**
     * Spring Security contract: isAccountNonLocked() siempre true
     */
    @Test
    @DisplayName("isAccountNonLocked() siempre true")
    void testIsAccountNonLocked() {
        // Act
        UserPrincipal principal = buildPrincipal(
                UUID.randomUUID(),
                "testuser",
                "ADMIN",
                testEcommerceId,
                true
        );

        // Assert
        assertTrue(principal.isAccountNonLocked());
    }

    /**
     * Spring Security contract: isCredentialsNonExpired() siempre true
     */
    @Test
    @DisplayName("isCredentialsNonExpired() siempre true")
    void testIsCredentialsNonExpired() {
        // Act
        UserPrincipal principal = buildPrincipal(
                UUID.randomUUID(),
                "testuser",
                "ADMIN",
                testEcommerceId,
                true
        );

        // Assert
        assertTrue(principal.isCredentialsNonExpired());
    }

    // ========== AUTHORITIES ==========

    /**
     * Spring Security contract: getAuthorities() retorna rol como ROLE_*
     */
    @Test
    @DisplayName("getAuthorities() retorna rol como ROLE_*")
    void testGetAuthorities() {
        // Arrange
        UserPrincipal principal = buildPrincipal(
                UUID.randomUUID(),
                "testuser",
                "ADMIN",
                testEcommerceId,
                true
        );

        // Act
        Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();

        // Assert
        assertEquals(1, authorities.size());
        assertTrue(authorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));
    }

    /**
     * Edge case: SUPER_ADMIN tiene autoridad ROLE_SUPER_ADMIN
     */
    @Test
    @DisplayName("SUPER_ADMIN tiene autoridad ROLE_SUPER_ADMIN")
    void testGetAuthorities_superAdmin() {
        // Arrange
        UserPrincipal principal = buildPrincipal(
                UUID.randomUUID(),
                "superadmin",
                "SUPER_ADMIN",
                null,
                true
        );

        // Act
        Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();

        // Assert
        assertTrue(authorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_SUPER_ADMIN")));
    }

    // ========== MULTI-TENANT HELPERS ==========

    /**
     * isEcommerceScoped() retorna true si ecommerce_id no es null
     */
    @Test
    @DisplayName("isEcommerceScoped() retorna true para usuario con ecommerce")
    void testIsEcommerceScoped_true() {
        // Arrange
        UserPrincipal principal = buildPrincipal(
                UUID.randomUUID(),
                "testuser",
                "ADMIN",
                testEcommerceId,
                true
        );

        // Assert
        assertTrue(principal.isEcommerceScoped());
    }

    /**
     * isEcommerceScoped() retorna false si ecommerce_id es null (SUPER_ADMIN)
     */
    @Test
    @DisplayName("isEcommerceScoped() retorna false para SUPER_ADMIN")
    void testIsEcommerceScoped_false() {
        // Arrange
        UserPrincipal principal = buildPrincipal(
                UUID.randomUUID(),
                "superadmin",
                "SUPER_ADMIN",
                null,
                true
        );

        // Assert
        assertFalse(principal.isEcommerceScoped());
    }

    // ========== ROLE CHECKS ==========

    /**
     * getRole() retorna el rol exacto asignado
     */
    @Test
    @DisplayName("getRole() retorna el rol asignado")
    void testGetRole() {
        // Arrange
        UserPrincipal principal = buildPrincipal(
                UUID.randomUUID(),
                "testuser",
                "ADMIN",
                testEcommerceId,
                true
        );

        // Assert
        assertEquals("ADMIN", principal.getRole());
    }

    /**
     * Edge case: diferentes roles
     */
    @Test
    @DisplayName("getRole() funciona con múltiples roles")
    void testGetRole_multipleRoles() {
        // Arrange
        UserPrincipal principalAdmin = buildPrincipal(
                UUID.randomUUID(),
                "admin",
                "ADMIN",
                testEcommerceId,
                true
        );
        UserPrincipal principalSuperAdmin = buildPrincipal(
                UUID.randomUUID(),
                "superadmin",
                "SUPER_ADMIN",
                null,
                true
        );
        UserPrincipal principalUser = buildPrincipal(
                UUID.randomUUID(),
                "user",
                "USER",
                testEcommerceId,
                true
        );

        // Assert
        assertEquals("ADMIN", principalAdmin.getRole());
        assertEquals("SUPER_ADMIN", principalSuperAdmin.getRole());
        assertEquals("USER", principalUser.getRole());
    }

    // ========== UID HANDLING ==========

    /**
     * getUid() retorna el UUID del usuario
     */
    @Test
    @DisplayName("getUid() retorna UUID del usuario")
    void testGetUid() {
        // Arrange
        UUID uid = UUID.randomUUID();
        UserPrincipal principal = buildPrincipal(
                uid,
                "testuser",
                "ADMIN",
                testEcommerceId,
                true
        );

        // Assert
        assertEquals(uid, principal.getUid());
    }

    /**
     * Edge case: múltiples UUIDs son diferentes
     */
    @Test
    @DisplayName("Diferentes instancias tienen diferentes UIDs")
    void testGetUid_differentInstances() {
        // Arrange
        UUID uid1 = UUID.randomUUID();
        UUID uid2 = UUID.randomUUID();
        UserPrincipal principal1 = buildPrincipal(uid1, "user1", "ADMIN", testEcommerceId, true);
        UserPrincipal principal2 = buildPrincipal(uid2, "user2", "ADMIN", testEcommerceId, true);

        // Assert
        assertEquals(uid1, principal1.getUid());
        assertEquals(uid2, principal2.getUid());
        assertFalse(uid1.equals(uid2));
    }
}
