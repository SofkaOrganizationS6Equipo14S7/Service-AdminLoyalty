package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.role.*;
import com.loyalty.service_admin.domain.entity.PermissionEntity;
import com.loyalty.service_admin.domain.entity.RoleEntity;
import com.loyalty.service_admin.domain.entity.RolePermissionEntity;
import com.loyalty.service_admin.domain.repository.PermissionRepository;
import com.loyalty.service_admin.domain.repository.RolePermissionRepository;
import com.loyalty.service_admin.domain.repository.RoleRepository;
import com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleService Unit Tests")
class RoleServiceTest {

    @Mock private RoleRepository roleRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private RolePermissionRepository rolePermissionRepository;

    @InjectMocks
    private RoleService roleService;

    private UUID roleId;
    private RoleEntity roleEntity;
    private PermissionEntity permissionEntity;

    @BeforeEach
    void setUp() {
        roleId = UUID.randomUUID();

        roleEntity = new RoleEntity();
        roleEntity.setId(roleId);
        roleEntity.setName("STORE_ADMIN");
        roleEntity.setIsActive(true);
        roleEntity.setCreatedAt(Instant.now());
        roleEntity.setUpdatedAt(Instant.now());

        permissionEntity = new PermissionEntity();
        permissionEntity.setId(UUID.randomUUID());
        permissionEntity.setCode("promotion:read");
        permissionEntity.setDescription("Read promotions");
        permissionEntity.setModule("promotion");
        permissionEntity.setCreatedAt(Instant.now());
        permissionEntity.setUpdatedAt(Instant.now());
    }

    @Test
    @DisplayName("testListAllRoles_Success")
    void testListAllRoles_Success() {
        // Arrange
        when(roleRepository.findAll()).thenReturn(List.of(roleEntity));

        // Act
        List<RoleResponse> result = roleService.listAllRoles();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("STORE_ADMIN", result.get(0).name());
    }

    @Test
    @DisplayName("testGetRoleWithPermissions_Success")
    void testGetRoleWithPermissions_Success() {
        // Arrange
        RolePermissionEntity rpEntity = new RolePermissionEntity();
        rpEntity.setRole(roleEntity);
        rpEntity.setPermission(permissionEntity);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(roleEntity));
        when(rolePermissionRepository.findByRoleId(roleId)).thenReturn(List.of(rpEntity));

        // Act
        RoleWithPermissionsResponse result = roleService.getRoleWithPermissions(roleId);

        // Assert
        assertNotNull(result);
        assertEquals(roleId, result.id());
        assertEquals("STORE_ADMIN", result.name());
        assertEquals(1, result.permissions().size());
        assertEquals("promotion:read", result.permissions().get(0).code());
    }

    @Test
    @DisplayName("testGetRoleWithPermissions_NotFound_ThrowsResourceNotFoundException")
    void testGetRoleWithPermissions_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> roleService.getRoleWithPermissions(roleId));
    }

    @Test
    @DisplayName("testListAllPermissions_Success")
    void testListAllPermissions_Success() {
        // Arrange
        when(permissionRepository.findAll()).thenReturn(List.of(permissionEntity));

        // Act
        List<PermissionResponse> result = roleService.listAllPermissions();

        // Assert
        assertEquals(1, result.size());
        assertEquals("promotion:read", result.get(0).code());
    }

    @Test
    @DisplayName("testListPermissionsByModule_Success")
    void testListPermissionsByModule_Success() {
        // Arrange
        when(permissionRepository.findAll()).thenReturn(List.of(permissionEntity));

        // Act
        List<PermissionResponse> result = roleService.listPermissionsByModule("promotion");

        // Assert
        assertEquals(1, result.size());
        assertEquals("promotion", result.get(0).module());
    }

    @Test
    @DisplayName("testListPermissionsByModule_NoMatch_EmptyList")
    void testListPermissionsByModule_NoMatch_EmptyList() {
        // Arrange
        when(permissionRepository.findAll()).thenReturn(List.of(permissionEntity));

        // Act
        List<PermissionResponse> result = roleService.listPermissionsByModule("nonexistent");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("testAssignPermissionsToRole_Success")
    void testAssignPermissionsToRole_Success() {
        // Arrange
        UUID permId = permissionEntity.getId();
        RolePermissionsAssignRequest request = new RolePermissionsAssignRequest(List.of(permId));

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(roleEntity));
        when(permissionRepository.findAllById(anyCollection())).thenReturn(List.of(permissionEntity));
        when(rolePermissionRepository.findByRoleId(roleId)).thenReturn(List.of());

        // Act
        RoleWithPermissionsResponse result = roleService.assignPermissionsToRole(roleId, request);

        // Assert
        assertNotNull(result);
        assertEquals(roleId, result.id());
        assertEquals(1, result.permissions().size());
        verify(rolePermissionRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("testAssignPermissionsToRole_RoleNotFound_ThrowsResourceNotFoundException")
    void testAssignPermissionsToRole_RoleNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        RolePermissionsAssignRequest request = new RolePermissionsAssignRequest(List.of(UUID.randomUUID()));
        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> roleService.assignPermissionsToRole(roleId, request));
    }

    @Test
    @DisplayName("testAssignPermissionsToRole_PermissionNotFound_ThrowsResourceNotFoundException")
    void testAssignPermissionsToRole_PermissionNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        UUID missingPermId = UUID.randomUUID();
        RolePermissionsAssignRequest request = new RolePermissionsAssignRequest(List.of(missingPermId));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(roleEntity));
        when(permissionRepository.findAllById(anyCollection())).thenReturn(List.of());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> roleService.assignPermissionsToRole(roleId, request));
    }
}
