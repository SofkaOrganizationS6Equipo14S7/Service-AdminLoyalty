package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.role.*;
import com.loyalty.service_admin.application.service.RoleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleController Unit Tests")
class RoleControllerTest {

    @Mock
    private RoleService roleService;

    @InjectMocks
    private RoleController roleController;

    @Test
    @DisplayName("listRoles returns 200 with role list")
    void listRoles_returns200() {
        RoleResponse role = new RoleResponse(UUID.randomUUID(), "SUPER_ADMIN", true, Instant.now(), Instant.now());
        when(roleService.listAllRoles()).thenReturn(List.of(role));

        ResponseEntity<List<RoleResponse>> result = roleController.listRoles();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
        assertEquals("SUPER_ADMIN", result.getBody().get(0).name());
    }

    @Test
    @DisplayName("getRoleDetails returns 200 with role and permissions")
    void getRoleDetails_returns200() {
        UUID roleId = UUID.randomUUID();
        PermissionResponse perm = new PermissionResponse(UUID.randomUUID(), "USER_READ", "Read users", "users", Instant.now(), Instant.now());
        RoleWithPermissionsResponse response = new RoleWithPermissionsResponse(
                roleId, "STORE_ADMIN", true, List.of(perm), Instant.now(), Instant.now());
        when(roleService.getRoleWithPermissions(roleId)).thenReturn(response);

        ResponseEntity<RoleWithPermissionsResponse> result = roleController.getRoleDetails(roleId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().permissions().size());
    }

    @Test
    @DisplayName("listPermissions without module returns all permissions")
    void listPermissions_noModule_returnsAll() {
        PermissionResponse perm = new PermissionResponse(UUID.randomUUID(), "USER_READ", "Read", "users", Instant.now(), Instant.now());
        when(roleService.listAllPermissions()).thenReturn(List.of(perm));

        ResponseEntity<List<PermissionResponse>> result = roleController.listPermissions(null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(roleService).listAllPermissions();
    }

    @Test
    @DisplayName("listPermissions with module filters by module")
    void listPermissions_withModule_filtersCorrectly() {
        PermissionResponse perm = new PermissionResponse(UUID.randomUUID(), "USER_READ", "Read", "users", Instant.now(), Instant.now());
        when(roleService.listPermissionsByModule("users")).thenReturn(List.of(perm));

        ResponseEntity<List<PermissionResponse>> result = roleController.listPermissions("users");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(roleService).listPermissionsByModule("users");
    }

    @Test
    @DisplayName("listPermissions with empty module returns all permissions")
    void listPermissions_emptyModule_returnsAll() {
        when(roleService.listAllPermissions()).thenReturn(List.of());

        ResponseEntity<List<PermissionResponse>> result = roleController.listPermissions("");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(roleService).listAllPermissions();
    }

    @Test
    @DisplayName("assignPermissionsToRole returns 200")
    void assignPermissionsToRole_returns200() {
        UUID roleId = UUID.randomUUID();
        List<UUID> permIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        RolePermissionsAssignRequest request = new RolePermissionsAssignRequest(permIds);
        RoleWithPermissionsResponse response = new RoleWithPermissionsResponse(
                roleId, "STORE_ADMIN", true, List.of(), Instant.now(), Instant.now());
        when(roleService.assignPermissionsToRole(roleId, request)).thenReturn(response);

        ResponseEntity<RoleWithPermissionsResponse> result = roleController.assignPermissionsToRole(roleId, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(roleService).assignPermissionsToRole(roleId, request);
    }
}
