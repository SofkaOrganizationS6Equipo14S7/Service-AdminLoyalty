package com.loyalty.service_admin.infrastructure.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SecurityContextHelper Unit Tests")
class SecurityContextHelperTest {

    private SecurityContextHelper helper;
    private UUID uid;
    private UUID ecommerceId;

    @BeforeEach
    void setUp() {
        helper = new SecurityContextHelper();
        uid = UUID.randomUUID();
        ecommerceId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthentication(String role, UUID ecommId) {
        UserPrincipal principal = new UserPrincipal(uid, "testuser", "pass", role, ecommId, true);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void testGetCurrentUserEcommerceId_returnsEcommerceId() {
        // Arrange
        setAuthentication("STORE_ADMIN", ecommerceId);

        // Act & Assert
        assertEquals(ecommerceId, helper.getCurrentUserEcommerceId());
    }

    @Test
    void testGetCurrentUserEcommerceId_superAdmin_returnsNull() {
        // Arrange
        setAuthentication("SUPER_ADMIN", null);

        // Act & Assert
        assertNull(helper.getCurrentUserEcommerceId());
    }

    @Test
    void testGetCurrentUserUid() {
        // Arrange
        setAuthentication("ADMIN", ecommerceId);

        // Act & Assert
        assertEquals(uid, helper.getCurrentUserUid());
    }

    @Test
    void testGetCurrentUserRole() {
        // Arrange
        setAuthentication("STORE_ADMIN", ecommerceId);

        // Act & Assert
        assertEquals("STORE_ADMIN", helper.getCurrentUserRole());
    }

    @Test
    void testIsCurrentUserEcommerceScoped_withEcommerce_returnsTrue() {
        // Arrange
        setAuthentication("STORE_ADMIN", ecommerceId);

        // Act & Assert
        assertTrue(helper.isCurrentUserEcommerceScoped());
    }

    @Test
    void testIsCurrentUserEcommerceScoped_superAdmin_returnsFalse() {
        // Arrange
        setAuthentication("SUPER_ADMIN", null);

        // Act & Assert
        assertFalse(helper.isCurrentUserEcommerceScoped());
    }

    @Test
    void testIsCurrentUserSuperAdmin_true() {
        // Arrange
        setAuthentication("SUPER_ADMIN", null);

        // Act & Assert
        assertTrue(helper.isCurrentUserSuperAdmin());
    }

    @Test
    void testIsCurrentUserSuperAdmin_false() {
        // Arrange
        setAuthentication("STORE_ADMIN", ecommerceId);

        // Act & Assert
        assertFalse(helper.isCurrentUserSuperAdmin());
    }

    @Test
    void testGetCurrentUserPrincipal_noPrincipal_throwsException() {
        // Arrange - no authentication set
        SecurityContextHolder.clearContext();

        // Act & Assert
        assertThrows(Exception.class, () -> helper.getCurrentUserPrincipal());
    }

    @Test
    void testGetCurrentUserPrincipal_wrongPrincipalType_throwsIllegalState() {
        // Arrange
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("stringPrincipal", null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> helper.getCurrentUserPrincipal());
    }

    // ==================== canActOnUser Tests ====================

    @Test
    void testCanActOnUser_superAdmin_canActOnAnyone() {
        // Arrange
        setAuthentication("SUPER_ADMIN", null);
        UUID targetEcommerce = UUID.randomUUID();
        UUID targetUid = UUID.randomUUID();

        // Act & Assert
        assertTrue(helper.canActOnUser(targetEcommerce, targetUid));
    }

    @Test
    void testCanActOnUser_storeAdmin_sameEcommerce_canAct() {
        // Arrange
        setAuthentication("STORE_ADMIN", ecommerceId);
        UUID targetUid = UUID.randomUUID();

        // Act & Assert
        assertTrue(helper.canActOnUser(ecommerceId, targetUid));
    }

    @Test
    void testCanActOnUser_storeAdmin_differentEcommerce_cannotAct() {
        // Arrange
        setAuthentication("STORE_ADMIN", ecommerceId);
        UUID differentEcommerce = UUID.randomUUID();
        UUID targetUid = UUID.randomUUID();

        // Act & Assert
        assertFalse(helper.canActOnUser(differentEcommerce, targetUid));
    }

    @Test
    void testCanActOnUser_storeUser_ownProfile_canAct() {
        // Arrange
        setAuthentication("STORE_USER", ecommerceId);

        // Act & Assert - uid is the same as the authenticated user
        assertTrue(helper.canActOnUser(ecommerceId, uid));
    }

    @Test
    void testCanActOnUser_storeUser_otherUser_cannotAct() {
        // Arrange
        setAuthentication("STORE_USER", ecommerceId);
        UUID otherUid = UUID.randomUUID();

        // Act & Assert
        assertFalse(helper.canActOnUser(ecommerceId, otherUid));
    }

    @Test
    void testCanActOnUser_unknownRole_cannotAct() {
        // Arrange
        setAuthentication("UNKNOWN_ROLE", ecommerceId);
        UUID targetUid = UUID.randomUUID();

        // Act & Assert
        assertFalse(helper.canActOnUser(ecommerceId, targetUid));
    }
}
