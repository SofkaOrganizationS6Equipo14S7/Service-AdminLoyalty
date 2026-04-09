package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.user.UserResponse;
import com.loyalty.service_admin.application.dto.user.UserUpdateRequest;
import com.loyalty.service_admin.application.port.out.UserUpdatePersistencePort;
import com.loyalty.service_admin.domain.entity.RoleEntity;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.domain.repository.RoleRepository;
import com.loyalty.service_admin.infrastructure.exception.AuthorizationException;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import com.loyalty.service_admin.infrastructure.exception.ConflictException;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserUpdateService Unit Tests")
class UserUpdateServiceTest {

    @Mock private UserUpdatePersistencePort userUpdatePersistencePort;
    @Mock private SecurityContextHelper securityContextHelper;
    @Mock private RoleRepository roleRepository;
    @Mock private EcommerceService ecommerceService;
    @Mock private AuditService auditService;
    @Mock private PasswordValidator passwordValidator;
    @Mock private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UserUpdateService userUpdateService;

    private UUID targetUid;
    private UUID currentUserUid;
    private UUID ecommerceId;
    private UserEntity targetUser;
    private RoleEntity role;

    @BeforeEach
    void setUp() {
        targetUid = UUID.randomUUID();
        currentUserUid = UUID.randomUUID();
        ecommerceId = UUID.randomUUID();

        role = new RoleEntity();
        role.setId(UUID.randomUUID());
        role.setName("STORE_ADMIN");

        targetUser = UserEntity.builder()
                .id(targetUid)
                .username("olduser")
                .email("old@test.com")
                .passwordHash("oldhash")
                .roleId(role.getId())
                .role(role)
                .ecommerceId(ecommerceId)
                .isActive(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("testUpdateUser_Success_UpdateUsername")
    void testUpdateUser_Success_UpdateUsername() {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest("newuser", null, null, null, null, null);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(currentUserUid);
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        when(userUpdatePersistencePort.findById(targetUid)).thenReturn(Optional.of(targetUser));
        when(securityContextHelper.canActOnUser(ecommerceId, targetUid)).thenReturn(true);
        when(userUpdatePersistencePort.existsByUsernameExcludingUid("newuser", targetUid)).thenReturn(false);
        when(userUpdatePersistencePort.save(any(UserEntity.class))).thenReturn(targetUser);

        // Act
        UserResponse result = userUpdateService.updateUser(targetUid, request);

        // Assert
        assertNotNull(result);
        verify(userUpdatePersistencePort).save(any(UserEntity.class));
        verify(auditService).auditUserUpdate(any(UserEntity.class), eq(currentUserUid));
    }

    @Test
    @DisplayName("testUpdateUser_Success_UpdateEmail")
    void testUpdateUser_Success_UpdateEmail() {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest(null, "newemail@test.com", null, null, null, null);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(currentUserUid);
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        when(userUpdatePersistencePort.findById(targetUid)).thenReturn(Optional.of(targetUser));
        when(securityContextHelper.canActOnUser(ecommerceId, targetUid)).thenReturn(true);
        when(userUpdatePersistencePort.existsByEmailExcludingUid("newemail@test.com", targetUid)).thenReturn(false);
        when(userUpdatePersistencePort.save(any(UserEntity.class))).thenReturn(targetUser);

        // Act
        UserResponse result = userUpdateService.updateUser(targetUid, request);

        // Assert
        assertNotNull(result);
    }

    @Test
    @DisplayName("testUpdateUser_Success_UpdatePassword")
    void testUpdateUser_Success_UpdatePassword() {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest(null, null, "NewPassword123!", null, null, null);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(currentUserUid);
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        when(userUpdatePersistencePort.findById(targetUid)).thenReturn(Optional.of(targetUser));
        when(securityContextHelper.canActOnUser(ecommerceId, targetUid)).thenReturn(true);
        when(passwordValidator.isValid("NewPassword123!")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("newhash");
        when(userUpdatePersistencePort.save(any(UserEntity.class))).thenReturn(targetUser);

        // Act
        UserResponse result = userUpdateService.updateUser(targetUid, request);

        // Assert
        assertNotNull(result);
        verify(passwordEncoder).encode("NewPassword123!");
    }

    @Test
    @DisplayName("testUpdateUser_Unauthenticated_ThrowsUnauthorized")
    void testUpdateUser_Unauthenticated_ThrowsUnauthorized() {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest("newuser", null, null, null, null, null);
        when(securityContextHelper.getCurrentUserRole()).thenThrow(new RuntimeException("No auth"));

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> userUpdateService.updateUser(targetUid, request));
    }

    @Test
    @DisplayName("testUpdateUser_NullRole_ThrowsUnauthorized")
    void testUpdateUser_NullRole_ThrowsUnauthorized() {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest("newuser", null, null, null, null, null);
        when(securityContextHelper.getCurrentUserRole()).thenReturn(null);
        when(securityContextHelper.getCurrentUserUid()).thenReturn(currentUserUid);
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> userUpdateService.updateUser(targetUid, request));
    }

    @Test
    @DisplayName("testUpdateUser_UserNotFound_ThrowsResourceNotFound")
    void testUpdateUser_UserNotFound_ThrowsResourceNotFound() {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest("newuser", null, null, null, null, null);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(currentUserUid);
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        when(userUpdatePersistencePort.findById(targetUid)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> userUpdateService.updateUser(targetUid, request));
    }

    @Test
    @DisplayName("testUpdateUser_NoPermission_ThrowsAuthorizationException")
    void testUpdateUser_NoPermission_ThrowsAuthorizationException() {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest("newuser", null, null, null, null, null);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("STORE_USER");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(currentUserUid);
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);
        when(userUpdatePersistencePort.findById(targetUid)).thenReturn(Optional.of(targetUser));
        when(securityContextHelper.canActOnUser(ecommerceId, targetUid)).thenReturn(false);

        // Act & Assert
        assertThrows(AuthorizationException.class, () -> userUpdateService.updateUser(targetUid, request));
    }

    @Test
    @DisplayName("testUpdateUser_ChangeRoleId_ThrowsBadRequest")
    void testUpdateUser_ChangeRoleId_ThrowsBadRequest() {
        // Arrange
        UUID newRoleId = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest(null, null, null, null, null, newRoleId);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(currentUserUid);
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        when(userUpdatePersistencePort.findById(targetUid)).thenReturn(Optional.of(targetUser));
        when(securityContextHelper.canActOnUser(ecommerceId, targetUid)).thenReturn(true);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> userUpdateService.updateUser(targetUid, request));
    }

    @Test
    @DisplayName("testUpdateUser_ChangeEcommerce_NonSuperAdmin_ThrowsAuthorizationException")
    void testUpdateUser_ChangeEcommerce_NonSuperAdmin_ThrowsAuthorizationException() {
        // Arrange
        UUID newEcommerceId = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest(null, null, null, newEcommerceId, null, null);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("STORE_ADMIN");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(currentUserUid);
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);
        when(userUpdatePersistencePort.findById(targetUid)).thenReturn(Optional.of(targetUser));
        when(securityContextHelper.canActOnUser(ecommerceId, targetUid)).thenReturn(true);

        // Act & Assert
        assertThrows(AuthorizationException.class, () -> userUpdateService.updateUser(targetUid, request));
    }

    @Test
    @DisplayName("testUpdateUser_ChangeActive_NonSuperAdmin_ThrowsAuthorizationException")
    void testUpdateUser_ChangeActive_NonSuperAdmin_ThrowsAuthorizationException() {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest(null, null, null, null, false, null);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("STORE_ADMIN");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(currentUserUid);
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);
        when(userUpdatePersistencePort.findById(targetUid)).thenReturn(Optional.of(targetUser));
        when(securityContextHelper.canActOnUser(ecommerceId, targetUid)).thenReturn(true);

        // Act & Assert
        assertThrows(AuthorizationException.class, () -> userUpdateService.updateUser(targetUid, request));
    }

    @Test
    @DisplayName("testUpdateUser_DuplicateUsername_ThrowsConflict")
    void testUpdateUser_DuplicateUsername_ThrowsConflict() {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest("takenuser", null, null, null, null, null);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(currentUserUid);
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        when(userUpdatePersistencePort.findById(targetUid)).thenReturn(Optional.of(targetUser));
        when(securityContextHelper.canActOnUser(ecommerceId, targetUid)).thenReturn(true);
        when(userUpdatePersistencePort.existsByUsernameExcludingUid("takenuser", targetUid)).thenReturn(true);

        // Act & Assert
        assertThrows(ConflictException.class, () -> userUpdateService.updateUser(targetUid, request));
    }

    @Test
    @DisplayName("testUpdateUser_DuplicateEmail_ThrowsConflict")
    void testUpdateUser_DuplicateEmail_ThrowsConflict() {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest(null, "taken@test.com", null, null, null, null);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(currentUserUid);
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        when(userUpdatePersistencePort.findById(targetUid)).thenReturn(Optional.of(targetUser));
        when(securityContextHelper.canActOnUser(ecommerceId, targetUid)).thenReturn(true);
        when(userUpdatePersistencePort.existsByEmailExcludingUid("taken@test.com", targetUid)).thenReturn(true);

        // Act & Assert
        assertThrows(ConflictException.class, () -> userUpdateService.updateUser(targetUid, request));
    }

    @Test
    @DisplayName("testUpdateUser_WeakPassword_ThrowsBadRequest")
    void testUpdateUser_WeakPassword_ThrowsBadRequest() {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest(null, null, "weak", null, null, null);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(currentUserUid);
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        when(userUpdatePersistencePort.findById(targetUid)).thenReturn(Optional.of(targetUser));
        when(securityContextHelper.canActOnUser(ecommerceId, targetUid)).thenReturn(true);
        when(passwordValidator.isValid("weak")).thenReturn(false);
        when(passwordValidator.getErrorMessage("weak")).thenReturn("Too weak");

        // Act & Assert
        assertThrows(BadRequestException.class, () -> userUpdateService.updateUser(targetUid, request));
    }
}
