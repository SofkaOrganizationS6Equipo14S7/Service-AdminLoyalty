package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.auth.ChangePasswordRequest;
import com.loyalty.service_admin.application.dto.auth.LoginResponse;
import com.loyalty.service_admin.application.dto.user.*;
import com.loyalty.service_admin.domain.entity.RoleEntity;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.domain.repository.RoleRepository;
import com.loyalty.service_admin.domain.repository.UserRepository;
import com.loyalty.service_admin.infrastructure.exception.*;
import com.loyalty.service_admin.infrastructure.security.JwtProvider;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private EcommerceService ecommerceService;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private SecurityContextHelper securityContextHelper;
    @Mock private JwtProvider jwtProvider;
    @Mock private AuditService auditService;
    @Mock private PasswordValidator passwordValidator;

    @InjectMocks
    private UserService userService;

    private final UUID uid = UUID.randomUUID();
    private final UUID ecommerceId = UUID.randomUUID();
    private final UUID roleId = UUID.randomUUID();
    private RoleEntity role;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        role = new RoleEntity(roleId, "STORE_ADMIN", true, Instant.now(), Instant.now());
        user = UserEntity.builder()
                .id(uid).username("testuser").email("test@mail.com")
                .passwordHash("hashed").roleId(roleId).role(role)
                .ecommerceId(ecommerceId).isActive(true)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
    }

    // ===== createUser =====

    @Test
    @DisplayName("createUser - SUPER_ADMIN creates STORE_ADMIN user successfully")
    void createUser_superAdmin_success() {
        UserCreateRequest request = new UserCreateRequest("newuser", "new@mail.com", "StrongPass123!", roleId, ecommerceId);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@mail.com")).thenReturn(Optional.empty());
        when(passwordValidator.isValid(anyString())).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(user);

        UserResponse result = userService.createUser(request);

        assertNotNull(result);
        verify(userRepository).save(any());
    }

    @Test
    @DisplayName("createUser - unauthorized role throws AuthorizationException")
    void createUser_unauthorizedRole_throws() {
        UserCreateRequest request = new UserCreateRequest("u", "e@m.com", "Pass12345678!", roleId, ecommerceId);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("STORE_USER");

        assertThrows(AuthorizationException.class, () -> userService.createUser(request));
    }

    @Test
    @DisplayName("createUser - invalid roleId throws BadRequestException")
    void createUser_invalidRole_throws() {
        UserCreateRequest request = new UserCreateRequest("u", "e@m.com", "Pass12345678!", roleId, ecommerceId);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> userService.createUser(request));
    }

    @Test
    @DisplayName("createUser - SUPER_ADMIN role with ecommerceId throws BadRequestException")
    void createUser_superAdminWithEcommerce_throws() {
        RoleEntity superRole = new RoleEntity(roleId, "SUPER_ADMIN", true, Instant.now(), Instant.now());
        UserCreateRequest request = new UserCreateRequest("u", "e@m.com", "Pass12345678!", roleId, ecommerceId);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(superRole));

        assertThrows(BadRequestException.class, () -> userService.createUser(request));
    }

    @Test
    @DisplayName("createUser - non-SUPER_ADMIN role without ecommerceId throws BadRequestException")
    void createUser_nonSuperAdminNoEcommerce_throws() {
        UserCreateRequest request = new UserCreateRequest("u", "e@m.com", "Pass12345678!", roleId, null);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        assertThrows(BadRequestException.class, () -> userService.createUser(request));
    }

    @Test
    @DisplayName("createUser - STORE_ADMIN creating in other ecommerce throws")
    void createUser_storeAdmin_otherEcommerce_throws() {
        UUID otherEcommerce = UUID.randomUUID();
        UserCreateRequest request = new UserCreateRequest("u", "e@m.com", "Pass12345678!", roleId, otherEcommerce);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("STORE_ADMIN");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        assertThrows(AuthorizationException.class, () -> userService.createUser(request));
    }

    @Test
    @DisplayName("createUser - duplicate username throws ConflictException")
    void createUser_duplicateUsername_throws() {
        UserCreateRequest request = new UserCreateRequest("existinguser", "new@mail.com", "Pass12345678!", roleId, ecommerceId);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(userRepository.findByUsername("existinguser")).thenReturn(Optional.of(user));

        assertThrows(ConflictException.class, () -> userService.createUser(request));
    }

    @Test
    @DisplayName("createUser - duplicate email throws ConflictException")
    void createUser_duplicateEmail_throws() {
        UserCreateRequest request = new UserCreateRequest("newuser", "existing@mail.com", "Pass12345678!", roleId, ecommerceId);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("existing@mail.com")).thenReturn(Optional.of(user));

        assertThrows(ConflictException.class, () -> userService.createUser(request));
    }

    @Test
    @DisplayName("createUser - weak password throws BadRequestException")
    void createUser_weakPassword_throws() {
        UserCreateRequest request = new UserCreateRequest("newuser", "new@mail.com", "weak", roleId, ecommerceId);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@mail.com")).thenReturn(Optional.empty());
        when(passwordValidator.isValid("weak")).thenReturn(false);
        when(passwordValidator.getErrorMessage("weak")).thenReturn("Password too weak");

        assertThrows(BadRequestException.class, () -> userService.createUser(request));
    }

    // ===== listUsers =====

    @Test
    @DisplayName("listUsers - SUPER_ADMIN lists all users")
    void listUsers_superAdmin_listsAll() {
        when(securityContextHelper.isCurrentUserSuperAdmin()).thenReturn(true);
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserResponse> result = userService.listUsers(null);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("listUsers - SUPER_ADMIN with ecommerceId filter")
    void listUsers_superAdmin_withFilter() {
        when(securityContextHelper.isCurrentUserSuperAdmin()).thenReturn(true);
        when(userRepository.findByEcommerceId(ecommerceId)).thenReturn(List.of(user));

        List<UserResponse> result = userService.listUsers(ecommerceId);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("listUsers - non-super admin lists own ecommerce users")
    void listUsers_storeAdmin_listsOwnEcommerce() {
        when(securityContextHelper.isCurrentUserSuperAdmin()).thenReturn(false);
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);
        when(userRepository.findByEcommerceId(ecommerceId)).thenReturn(List.of(user));

        List<UserResponse> result = userService.listUsers(null);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("listUsers - non-super admin accessing other ecommerce throws")
    void listUsers_storeAdmin_otherEcommerce_throws() {
        UUID otherEcommerce = UUID.randomUUID();
        when(securityContextHelper.isCurrentUserSuperAdmin()).thenReturn(false);
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);

        assertThrows(AuthorizationException.class, () -> userService.listUsers(otherEcommerce));
    }

    // ===== getUserByUid =====

    @Test
    @DisplayName("getUserByUid - SUPER_ADMIN gets any user")
    void getUserByUid_superAdmin_success() {
        when(userRepository.findById(uid)).thenReturn(Optional.of(user));
        when(securityContextHelper.isCurrentUserSuperAdmin()).thenReturn(true);

        UserResponse result = userService.getUserByUid(uid);

        assertNotNull(result);
    }

    @Test
    @DisplayName("getUserByUid - user not found throws")
    void getUserByUid_notFound_throws() {
        when(userRepository.findById(uid)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserByUid(uid));
    }

    @Test
    @DisplayName("getUserByUid - non-super admin accessing different ecommerce throws")
    void getUserByUid_crossEcommerce_throws() {
        when(userRepository.findById(uid)).thenReturn(Optional.of(user));
        when(securityContextHelper.isCurrentUserSuperAdmin()).thenReturn(false);
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(UUID.randomUUID());

        assertThrows(AuthorizationException.class, () -> userService.getUserByUid(uid));
    }

    // ===== updateUser =====

    @Test
    @DisplayName("updateUser - successful update")
    void updateUser_success() {
        UserUpdateRequest request = new UserUpdateRequest("updated", null, null, null, null, null);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(UUID.randomUUID());
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        when(securityContextHelper.canActOnUser(any(), any())).thenReturn(true);
        when(userRepository.findById(uid)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("updated")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenReturn(user);

        UserResponse result = userService.updateUser(uid, request);

        assertNotNull(result);
    }

    @Test
    @DisplayName("updateUser - roleId in request throws BadRequestException")
    void updateUser_roleId_throws() {
        UserUpdateRequest request = new UserUpdateRequest(null, null, null, null, null, UUID.randomUUID());
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(UUID.randomUUID());
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        when(securityContextHelper.canActOnUser(any(), any())).thenReturn(true);
        when(userRepository.findById(uid)).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class, () -> userService.updateUser(uid, request));
    }

    @Test
    @DisplayName("updateUser - cannotAct throws AuthorizationException")
    void updateUser_cannotAct_throws() {
        UserUpdateRequest request = new UserUpdateRequest("u", null, null, null, null, null);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("STORE_USER");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(UUID.randomUUID());
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);
        when(securityContextHelper.canActOnUser(any(), any())).thenReturn(false);
        when(userRepository.findById(uid)).thenReturn(Optional.of(user));

        assertThrows(AuthorizationException.class, () -> userService.updateUser(uid, request));
    }

    @Test
    @DisplayName("updateUser - non-SUPER_ADMIN changing ecommerceId throws")
    void updateUser_nonSuperAdmin_changeEcommerce_throws() {
        UserUpdateRequest request = new UserUpdateRequest(null, null, null, UUID.randomUUID(), null, null);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("STORE_ADMIN");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(UUID.randomUUID());
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);
        when(securityContextHelper.canActOnUser(any(), any())).thenReturn(true);
        when(userRepository.findById(uid)).thenReturn(Optional.of(user));

        assertThrows(AuthorizationException.class, () -> userService.updateUser(uid, request));
    }

    @Test
    @DisplayName("updateUser - non-SUPER_ADMIN changing active throws")
    void updateUser_nonSuperAdmin_changeActive_throws() {
        UserUpdateRequest request = new UserUpdateRequest(null, null, null, null, false, null);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("STORE_ADMIN");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(UUID.randomUUID());
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);
        when(securityContextHelper.canActOnUser(any(), any())).thenReturn(true);
        when(userRepository.findById(uid)).thenReturn(Optional.of(user));

        assertThrows(AuthorizationException.class, () -> userService.updateUser(uid, request));
    }

    @Test
    @DisplayName("updateUser - duplicate username throws ConflictException")
    void updateUser_duplicateUsername_throws() {
        UserUpdateRequest request = new UserUpdateRequest("existing", null, null, null, null, null);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(UUID.randomUUID());
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        when(securityContextHelper.canActOnUser(any(), any())).thenReturn(true);
        when(userRepository.findById(uid)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("existing")).thenReturn(Optional.of(user));

        assertThrows(ConflictException.class, () -> userService.updateUser(uid, request));
    }

    @Test
    @DisplayName("updateUser - SUPER_ADMIN can change ecommerceId and active")
    void updateUser_superAdmin_changeEcommerceAndActive() {
        UUID newEcommerce = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest(null, null, null, newEcommerce, false, null);
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(UUID.randomUUID());
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        when(securityContextHelper.canActOnUser(any(), any())).thenReturn(true);
        when(userRepository.findById(uid)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        UserResponse result = userService.updateUser(uid, request);
        assertNotNull(result);
    }

    // ===== deleteUser =====

    @Test
    @DisplayName("deleteUser - successful delete")
    void deleteUser_success() {
        UUID actorUid = UUID.randomUUID();
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(actorUid);
        when(securityContextHelper.canActOnUser(any(), any())).thenReturn(true);
        when(userRepository.findById(uid)).thenReturn(Optional.of(user));

        userService.deleteUser(uid);

        verify(userRepository).delete(user);
        verify(auditService).auditUserDeletion(user, actorUid);
    }

    @Test
    @DisplayName("deleteUser - self-deletion throws BadRequestException")
    void deleteUser_selfDelete_throws() {
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(uid);
        when(userRepository.findById(uid)).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class, () -> userService.deleteUser(uid));
    }

    @Test
    @DisplayName("deleteUser - cannotAct throws AuthorizationException")
    void deleteUser_noPermission_throws() {
        when(securityContextHelper.getCurrentUserRole()).thenReturn("STORE_USER");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(UUID.randomUUID());
        when(securityContextHelper.canActOnUser(any(), any())).thenReturn(false);
        when(userRepository.findById(uid)).thenReturn(Optional.of(user));

        assertThrows(AuthorizationException.class, () -> userService.deleteUser(uid));
    }

    // ===== updateProfile =====

    @Test
    @DisplayName("updateProfile - successful email update")
    void updateProfile_success() {
        UpdateProfileRequest request = new UpdateProfileRequest("newemail@mail.com");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(uid);
        when(userRepository.findById(uid)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("newemail@mail.com")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenReturn(user);

        UserResponse result = userService.updateProfile(request);

        assertNotNull(result);
    }

    @Test
    @DisplayName("updateProfile - duplicate email throws ConflictException")
    void updateProfile_duplicateEmail_throws() {
        UpdateProfileRequest request = new UpdateProfileRequest("existing@mail.com");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(uid);
        when(userRepository.findById(uid)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("existing@mail.com")).thenReturn(Optional.of(user));

        assertThrows(ConflictException.class, () -> userService.updateProfile(request));
    }

    @Test
    @DisplayName("updateProfile - same email no change")
    void updateProfile_sameEmail_noChange() {
        UpdateProfileRequest request = new UpdateProfileRequest("test@mail.com");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(uid);
        when(userRepository.findById(uid)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        UserResponse result = userService.updateProfile(request);
        assertNotNull(result);
    }

    // ===== changePassword =====

    @Test
    @DisplayName("changePassword - successful password change")
    void changePassword_success() {
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass12345!", "NewPass12345!", "NewPass12345!");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(uid);
        when(userRepository.findById(uid)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass12345!", "hashed")).thenReturn(true);
        when(passwordValidator.isValid("NewPass12345!")).thenReturn(true);
        when(passwordEncoder.matches("NewPass12345!", "hashed")).thenReturn(false);
        when(passwordEncoder.encode("NewPass12345!")).thenReturn("newhashed");
        when(userRepository.save(any())).thenReturn(user);
        when(jwtProvider.generateTokenFull(any(), any(), any(), any(), any())).thenReturn("new-jwt-token");

        LoginResponse result = userService.changePassword(request);

        assertEquals("new-jwt-token", result.token());
        assertEquals("Bearer", result.tipo());
    }

    @Test
    @DisplayName("changePassword - wrong current password throws UnauthorizedException")
    void changePassword_wrongCurrentPassword_throws() {
        ChangePasswordRequest request = new ChangePasswordRequest("wrong", "New12345678!", "New12345678!");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(uid);
        when(userRepository.findById(uid)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> userService.changePassword(request));
    }

    @Test
    @DisplayName("changePassword - mismatched passwords throws BadRequestException")
    void changePassword_mismatch_throws() {
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass12345!", "New12345678!", "Different12345!");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(uid);
        when(userRepository.findById(uid)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass12345!", "hashed")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> userService.changePassword(request));
    }

    @Test
    @DisplayName("changePassword - weak new password throws BadRequestException")
    void changePassword_weakPassword_throws() {
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass12345!", "weak", "weak");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(uid);
        when(userRepository.findById(uid)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass12345!", "hashed")).thenReturn(true);
        when(passwordValidator.isValid("weak")).thenReturn(false);
        when(passwordValidator.getErrorMessage("weak")).thenReturn("Too weak");

        assertThrows(BadRequestException.class, () -> userService.changePassword(request));
    }

    @Test
    @DisplayName("changePassword - same password throws ConflictException")
    void changePassword_samePassword_throws() {
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass12345!", "OldPass12345!", "OldPass12345!");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(uid);
        when(userRepository.findById(uid)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass12345!", "hashed")).thenReturn(true);
        when(passwordValidator.isValid("OldPass12345!")).thenReturn(true);
        when(passwordEncoder.matches("OldPass12345!", "hashed")).thenReturn(true);

        assertThrows(ConflictException.class, () -> userService.changePassword(request));
    }
}
