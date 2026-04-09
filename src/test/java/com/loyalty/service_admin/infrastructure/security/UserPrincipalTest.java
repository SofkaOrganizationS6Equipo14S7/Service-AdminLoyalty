package com.loyalty.service_admin.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserPrincipal Unit Tests")
class UserPrincipalTest {

    @Test
    void testConstructorAndGetters() {
        // Arrange
        UUID uid = UUID.randomUUID();
        UUID ecommerceId = UUID.randomUUID();

        // Act
        UserPrincipal principal = new UserPrincipal(uid, "admin", "password", "SUPER_ADMIN", ecommerceId, true);

        // Assert
        assertEquals(uid, principal.getUid());
        assertEquals("admin", principal.getUsername());
        assertEquals("password", principal.getPassword());
        assertEquals("SUPER_ADMIN", principal.getRole());
        assertEquals(ecommerceId, principal.getEcommerceId());
        assertTrue(principal.isEnabled());
    }

    @Test
    void testGetAuthorities_returnsRoleWithPrefix() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "user", "pass", "STORE_ADMIN", UUID.randomUUID(), true);

        // Act
        Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();

        // Assert
        assertEquals(1, authorities.size());
        assertEquals("ROLE_STORE_ADMIN", authorities.iterator().next().getAuthority());
    }

    @Test
    void testAccountFlags_allTrue() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "user", "pass", "ADMIN", null, true);

        // Act & Assert
        assertTrue(principal.isAccountNonExpired());
        assertTrue(principal.isAccountNonLocked());
        assertTrue(principal.isCredentialsNonExpired());
        assertTrue(principal.isEnabled());
    }

    @Test
    void testIsEnabled_false() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "user", "pass", "ADMIN", null, false);

        // Act & Assert
        assertFalse(principal.isEnabled());
    }

    @Test
    void testIsEcommerceScoped_withEcommerce_returnsTrue() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "user", "pass", "STORE_ADMIN", UUID.randomUUID(), true);

        // Act & Assert
        assertTrue(principal.isEcommerceScoped());
    }

    @Test
    void testIsEcommerceScoped_withoutEcommerce_returnsFalse() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "user", "pass", "SUPER_ADMIN", null, true);

        // Act & Assert
        assertFalse(principal.isEcommerceScoped());
    }

    @Test
    void testGetEcommerceId_null() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "user", "pass", "SUPER_ADMIN", null, true);

        // Act & Assert
        assertNull(principal.getEcommerceId());
    }
}
