package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.domain.repository.RolePermissionRepository;
import com.loyalty.service_admin.infrastructure.security.SecurityContextHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionService Unit Tests")
class PermissionServiceTest {

    @Mock private RolePermissionRepository rolePermissionRepository;
    @Mock private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        // Common setup
    }

    @Test
    @DisplayName("testGetPermissionsForRole_ReturnsPermissionCodes")
    void testGetPermissionsForRole_ReturnsPermissionCodes() {
        // Arrange
        Set<String> expected = Set.of("promotion:read", "promotion:write", "user:read");
        when(rolePermissionRepository.findPermissionCodesByRoleName("STORE_ADMIN")).thenReturn(expected);

        // Act
        Set<String> result = permissionService.getPermissionsForRole("STORE_ADMIN");

        // Assert
        assertEquals(3, result.size());
        assertTrue(result.contains("promotion:read"));
    }

    @Test
    @DisplayName("testGetPermissionsForRole_EmptyRole_ReturnsEmpty")
    void testGetPermissionsForRole_EmptyRole_ReturnsEmpty() {
        // Arrange
        when(rolePermissionRepository.findPermissionCodesByRoleName("UNKNOWN")).thenReturn(Set.of());

        // Act
        Set<String> result = permissionService.getPermissionsForRole("UNKNOWN");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("testGetCurrentUserPermissions_ReturnsPermissionsForCurrentRole")
    void testGetCurrentUserPermissions_ReturnsPermissionsForCurrentRole() {
        // Arrange
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        Set<String> perms = Set.of("promotion:read", "promotion:write", "user:read", "user:write");
        when(rolePermissionRepository.findPermissionCodesByRoleName("SUPER_ADMIN")).thenReturn(perms);

        // Act
        Set<String> result = permissionService.getCurrentUserPermissions();

        // Assert
        assertEquals(4, result.size());
    }

    @Test
    @DisplayName("testHasPermission_UserHasPermission_ReturnsTrue")
    void testHasPermission_UserHasPermission_ReturnsTrue() {
        // Arrange
        when(securityContextHelper.getCurrentUserRole()).thenReturn("STORE_ADMIN");
        when(rolePermissionRepository.findPermissionCodesByRoleName("STORE_ADMIN"))
                .thenReturn(Set.of("promotion:read", "promotion:write"));

        // Act
        boolean result = permissionService.hasPermission("promotion:read");

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("testHasPermission_UserLacksPermission_ReturnsFalse")
    void testHasPermission_UserLacksPermission_ReturnsFalse() {
        // Arrange
        when(securityContextHelper.getCurrentUserRole()).thenReturn("STORE_USER");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(java.util.UUID.randomUUID());
        when(rolePermissionRepository.findPermissionCodesByRoleName("STORE_USER"))
                .thenReturn(Set.of("promotion:read"));

        // Act
        boolean result = permissionService.hasPermission("promotion:write");

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("testHasAllPermissions_UserHasAll_ReturnsTrue")
    void testHasAllPermissions_UserHasAll_ReturnsTrue() {
        // Arrange
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(rolePermissionRepository.findPermissionCodesByRoleName("SUPER_ADMIN"))
                .thenReturn(Set.of("promotion:read", "promotion:write", "user:read"));

        // Act
        boolean result = permissionService.hasAllPermissions("promotion:read", "promotion:write");

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("testHasAllPermissions_UserMissesOne_ReturnsFalse")
    void testHasAllPermissions_UserMissesOne_ReturnsFalse() {
        // Arrange
        when(securityContextHelper.getCurrentUserRole()).thenReturn("STORE_USER");
        when(rolePermissionRepository.findPermissionCodesByRoleName("STORE_USER"))
                .thenReturn(Set.of("promotion:read"));

        // Act
        boolean result = permissionService.hasAllPermissions("promotion:read", "promotion:write");

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("testHasAnyPermission_UserHasOne_ReturnsTrue")
    void testHasAnyPermission_UserHasOne_ReturnsTrue() {
        // Arrange
        when(securityContextHelper.getCurrentUserRole()).thenReturn("STORE_USER");
        when(rolePermissionRepository.findPermissionCodesByRoleName("STORE_USER"))
                .thenReturn(Set.of("promotion:read"));

        // Act
        boolean result = permissionService.hasAnyPermission("promotion:read", "promotion:write");

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("testHasAnyPermission_UserHasNone_ReturnsFalse")
    void testHasAnyPermission_UserHasNone_ReturnsFalse() {
        // Arrange
        when(securityContextHelper.getCurrentUserRole()).thenReturn("STORE_USER");
        when(rolePermissionRepository.findPermissionCodesByRoleName("STORE_USER"))
                .thenReturn(Set.of("dashboard:read"));

        // Act
        boolean result = permissionService.hasAnyPermission("promotion:read", "promotion:write");

        // Assert
        assertFalse(result);
    }
}
