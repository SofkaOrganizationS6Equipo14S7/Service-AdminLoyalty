package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.user.UserCreateRequest;
import com.loyalty.service_admin.application.dto.user.UserResponse;
import com.loyalty.service_admin.application.port.out.UserCreatePersistencePort;
import com.loyalty.service_admin.domain.entity.RoleEntity;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.domain.repository.RoleRepository;
import com.loyalty.service_admin.infrastructure.exception.AuthorizationException;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import com.loyalty.service_admin.infrastructure.exception.ConflictException;
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
@DisplayName("UserCreateService Unit Tests")
class UserCreateServiceTest {

    @Mock private UserCreatePersistencePort userCreatePersistencePort;
    @Mock private SecurityContextHelper securityContextHelper;
    @Mock private RoleRepository roleRepository;
    @Mock private EcommerceService ecommerceService;
    @Mock private AuditService auditService;
    @Mock private PasswordValidator passwordValidator;
    @Mock private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UserCreateService userCreateService;

    private UUID currentUserUid;
    private UUID ecommerceId;
    private UUID roleId;
    private RoleEntity storeAdminRole;
    private RoleEntity superAdminRole;

    @BeforeEach
    void setUp() {
        currentUserUid = UUID.randomUUID();
        ecommerceId = UUID.randomUUID();
        roleId = UUID.randomUUID();

        storeAdminRole = new RoleEntity();
        storeAdminRole.setId(roleId);
        storeAdminRole.setName("STORE_ADMIN");

        superAdminRole = new RoleEntity();
        superAdminRole.setId(UUID.randomUUID());
        superAdminRole.setName("SUPER_ADMIN");
    }

    @Test
    @DisplayName("testCreateUser_Success_AsSuperAdmin")
    void testCreateUser_Success_AsSuperAdmin() {
        // Arrange
        UserCreateRequest request = new UserCreateRequest(
                "newuser", "new@test.com", "Password123456", roleId, ecommerceId);

        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        when(securityContextHelper.getCurrentUserUid()).thenReturn(currentUserUid);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(storeAdminRole));
        when(userCreatePersistencePort.existsByEmail("new@test.com")).thenReturn(false);
        when(userCreatePersistencePort.existsByUsername("newuser")).thenReturn(false);
        when(passwordValidator.isValid("Password123456")).thenReturn(true);
        when(passwordEncoder.encode("Password123456")).thenReturn("hashedpw");
        when(userCreatePersistencePort.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            u.setCreatedAt(Instant.now());
            u.setUpdatedAt(Instant.now());
            return u;
        });

        // Act
        UserResponse result = userCreateService.createUser(request);

        // Assert
        assertNotNull(result);
        assertEquals("newuser", result.username());
        assertEquals("new@test.com", result.email());
        assertTrue(result.isActive());
        verify(userCreatePersistencePort).save(any(UserEntity.class));
        verify(auditService).auditUserCreation(any(UserEntity.class), eq(currentUserUid));
    }

    @Test
    @DisplayName("testCreateUser_Success_AsStoreAdmin_OwnEcommerce")
    void testCreateUser_Success_AsStoreAdmin_OwnEcommerce() {
        // Arrange
        UserCreateRequest request = new UserCreateRequest(
                "newuser", "new@test.com", "Password123456", roleId, ecommerceId);

        when(securityContextHelper.getCurrentUserRole()).thenReturn("STORE_ADMIN");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);
        when(securityContextHelper.getCurrentUserUid()).thenReturn(currentUserUid);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(storeAdminRole));
        when(userCreatePersistencePort.existsByEmail("new@test.com")).thenReturn(false);
        when(userCreatePersistencePort.existsByUsername("newuser")).thenReturn(false);
        when(passwordValidator.isValid("Password123456")).thenReturn(true);
        when(passwordEncoder.encode("Password123456")).thenReturn("hashedpw");
        when(userCreatePersistencePort.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            u.setCreatedAt(Instant.now());
            u.setUpdatedAt(Instant.now());
            return u;
        });

        // Act
        UserResponse result = userCreateService.createUser(request);

        // Assert
        assertNotNull(result);
        verify(ecommerceService).validateEcommerceExists(ecommerceId);
    }

    @Test
    @DisplayName("testCreateUser_Unauthenticated_ThrowsUnauthorized")
    void testCreateUser_Unauthenticated_ThrowsUnauthorized() {
        // Arrange
        UserCreateRequest request = new UserCreateRequest(
                "newuser", "new@test.com", "Password123456", roleId, ecommerceId);
        when(securityContextHelper.getCurrentUserRole()).thenThrow(new RuntimeException("No auth"));

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> userCreateService.createUser(request));
    }

    @Test
    @DisplayName("testCreateUser_NullRole_ThrowsUnauthorized")
    void testCreateUser_NullRole_ThrowsUnauthorized() {
        // Arrange
        UserCreateRequest request = new UserCreateRequest(
                "newuser", "new@test.com", "Password123456", roleId, ecommerceId);
        when(securityContextHelper.getCurrentUserRole()).thenReturn(null);
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);
        when(securityContextHelper.getCurrentUserUid()).thenReturn(currentUserUid);

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> userCreateService.createUser(request));
    }

    @Test
    @DisplayName("testCreateUser_StandardRole_ThrowsAuthorizationException")
    void testCreateUser_StandardRole_ThrowsAuthorizationException() {
        // Arrange
        UserCreateRequest request = new UserCreateRequest(
                "newuser", "new@test.com", "Password123456", roleId, ecommerceId);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("STORE_USER");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);
        when(securityContextHelper.getCurrentUserUid()).thenReturn(currentUserUid);

        // Act & Assert
        assertThrows(AuthorizationException.class, () -> userCreateService.createUser(request));
    }

    @Test
    @DisplayName("testCreateUser_InvalidRoleId_ThrowsBadRequest")
    void testCreateUser_InvalidRoleId_ThrowsBadRequest() {
        // Arrange
        UserCreateRequest request = new UserCreateRequest(
                "newuser", "new@test.com", "Password123456", roleId, ecommerceId);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        when(securityContextHelper.getCurrentUserUid()).thenReturn(currentUserUid);
        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BadRequestException.class, () -> userCreateService.createUser(request));
    }

    @Test
    @DisplayName("testCreateUser_SuperAdminWithEcommerce_ThrowsBadRequest")
    void testCreateUser_SuperAdminRoleWithEcommerce_ThrowsBadRequest() {
        // Arrange
        UserCreateRequest request = new UserCreateRequest(
                "newuser", "new@test.com", "Password123456", superAdminRole.getId(), ecommerceId);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        when(securityContextHelper.getCurrentUserUid()).thenReturn(currentUserUid);
        when(roleRepository.findById(superAdminRole.getId())).thenReturn(Optional.of(superAdminRole));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> userCreateService.createUser(request));
    }

    @Test
    @DisplayName("testCreateUser_NonSuperAdminWithoutEcommerce_ThrowsBadRequest")
    void testCreateUser_NonSuperAdminWithoutEcommerce_ThrowsBadRequest() {
        // Arrange
        UserCreateRequest request = new UserCreateRequest(
                "newuser", "new@test.com", "Password123456", roleId, null);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        when(securityContextHelper.getCurrentUserUid()).thenReturn(currentUserUid);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(storeAdminRole));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> userCreateService.createUser(request));
    }

    @Test
    @DisplayName("testCreateUser_StoreAdminCrossEcommerce_ThrowsAuthorizationException")
    void testCreateUser_StoreAdminCrossEcommerce_ThrowsAuthorizationException() {
        // Arrange
        UUID otherEcommerceId = UUID.randomUUID();
        UserCreateRequest request = new UserCreateRequest(
                "newuser", "new@test.com", "Password123456", roleId, otherEcommerceId);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("STORE_ADMIN");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);
        when(securityContextHelper.getCurrentUserUid()).thenReturn(currentUserUid);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(storeAdminRole));

        // Act & Assert
        assertThrows(AuthorizationException.class, () -> userCreateService.createUser(request));
    }

    @Test
    @DisplayName("testCreateUser_DuplicateEmail_ThrowsConflict")
    void testCreateUser_DuplicateEmail_ThrowsConflict() {
        // Arrange
        UserCreateRequest request = new UserCreateRequest(
                "newuser", "dup@test.com", "Password123456", roleId, ecommerceId);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        when(securityContextHelper.getCurrentUserUid()).thenReturn(currentUserUid);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(storeAdminRole));
        when(userCreatePersistencePort.existsByEmail("dup@test.com")).thenReturn(true);

        // Act & Assert
        assertThrows(ConflictException.class, () -> userCreateService.createUser(request));
    }

    @Test
    @DisplayName("testCreateUser_DuplicateUsername_ThrowsConflict")
    void testCreateUser_DuplicateUsername_ThrowsConflict() {
        // Arrange
        UserCreateRequest request = new UserCreateRequest(
                "dupuser", "new@test.com", "Password123456", roleId, ecommerceId);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        when(securityContextHelper.getCurrentUserUid()).thenReturn(currentUserUid);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(storeAdminRole));
        when(userCreatePersistencePort.existsByEmail("new@test.com")).thenReturn(false);
        when(userCreatePersistencePort.existsByUsername("dupuser")).thenReturn(true);

        // Act & Assert
        assertThrows(ConflictException.class, () -> userCreateService.createUser(request));
    }

    @Test
    @DisplayName("testCreateUser_WeakPassword_ThrowsBadRequest")
    void testCreateUser_WeakPassword_ThrowsBadRequest() {
        // Arrange
        UserCreateRequest request = new UserCreateRequest(
                "newuser", "new@test.com", "weak", roleId, ecommerceId);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        when(securityContextHelper.getCurrentUserUid()).thenReturn(currentUserUid);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(storeAdminRole));
        when(userCreatePersistencePort.existsByEmail("new@test.com")).thenReturn(false);
        when(userCreatePersistencePort.existsByUsername("newuser")).thenReturn(false);
        when(passwordValidator.isValid("weak")).thenReturn(false);
        when(passwordValidator.getErrorMessage("weak")).thenReturn("Password too weak");

        // Act & Assert
        assertThrows(BadRequestException.class, () -> userCreateService.createUser(request));
    }
}
