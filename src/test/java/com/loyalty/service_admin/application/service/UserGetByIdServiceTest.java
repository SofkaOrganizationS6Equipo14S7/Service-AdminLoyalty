package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.user.UserResponse;
import com.loyalty.service_admin.application.port.out.UserGetPersistencePort;
import com.loyalty.service_admin.domain.entity.RoleEntity;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.infrastructure.exception.AuthorizationException;
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

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserGetByIdService Unit Tests")
class UserGetByIdServiceTest {

    @Mock private UserGetPersistencePort userGetPersistencePort;
    @Mock private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private UserGetByIdService userGetByIdService;

    private UUID targetUid;
    private UUID currentUserUid;
    private UUID ecommerceId;
    private UserEntity targetUser;

    @BeforeEach
    void setUp() {
        targetUid = UUID.randomUUID();
        currentUserUid = UUID.randomUUID();
        ecommerceId = UUID.randomUUID();

        RoleEntity role = new RoleEntity();
        role.setId(UUID.randomUUID());
        role.setName("STORE_USER");

        targetUser = UserEntity.builder()
                .id(targetUid)
                .username("target")
                .email("target@test.com")
                .roleId(role.getId())
                .role(role)
                .ecommerceId(ecommerceId)
                .isActive(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("testGetUserById_SuperAdmin_Success")
    void testGetUserById_SuperAdmin_Success() {
        // Arrange
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(currentUserUid);
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        when(userGetPersistencePort.findById(targetUid)).thenReturn(Optional.of(targetUser));

        // Act
        UserResponse result = userGetByIdService.getUserById(targetUid);

        // Assert
        assertNotNull(result);
        assertEquals(targetUid, result.uid());
        assertEquals("target", result.username());
    }

    @Test
    @DisplayName("testGetUserById_StoreAdmin_OwnEcommerce_Success")
    void testGetUserById_StoreAdmin_OwnEcommerce_Success() {
        // Arrange
        when(securityContextHelper.getCurrentUserRole()).thenReturn("STORE_ADMIN");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(currentUserUid);
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);
        when(userGetPersistencePort.findById(targetUid)).thenReturn(Optional.of(targetUser));

        // Act
        UserResponse result = userGetByIdService.getUserById(targetUid);

        // Assert
        assertNotNull(result);
        assertEquals(targetUid, result.uid());
    }

    @Test
    @DisplayName("testGetUserById_StoreAdmin_CrossEcommerce_ThrowsAuthorizationException")
    void testGetUserById_StoreAdmin_CrossEcommerce_ThrowsAuthorizationException() {
        // Arrange
        UUID otherEcommerce = UUID.randomUUID();
        when(securityContextHelper.getCurrentUserRole()).thenReturn("STORE_ADMIN");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(currentUserUid);
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(otherEcommerce);
        when(userGetPersistencePort.findById(targetUid)).thenReturn(Optional.of(targetUser));

        // Act & Assert
        assertThrows(AuthorizationException.class, () -> userGetByIdService.getUserById(targetUid));
    }

    @Test
    @DisplayName("testGetUserById_Standard_Self_Success")
    void testGetUserById_Standard_Self_Success() {
        // Arrange
        when(securityContextHelper.getCurrentUserRole()).thenReturn("STORE_USER");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(targetUid);
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);
        when(userGetPersistencePort.findById(targetUid)).thenReturn(Optional.of(targetUser));

        // Act
        UserResponse result = userGetByIdService.getUserById(targetUid);

        // Assert
        assertNotNull(result);
        assertEquals(targetUid, result.uid());
    }

    @Test
    @DisplayName("testGetUserById_Standard_OtherUser_ThrowsAuthorizationException")
    void testGetUserById_Standard_OtherUser_ThrowsAuthorizationException() {
        // Arrange
        when(securityContextHelper.getCurrentUserRole()).thenReturn("STORE_USER");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(currentUserUid);
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);
        when(userGetPersistencePort.findById(targetUid)).thenReturn(Optional.of(targetUser));

        // Act & Assert
        assertThrows(AuthorizationException.class, () -> userGetByIdService.getUserById(targetUid));
    }

    @Test
    @DisplayName("testGetUserById_NotFound_ThrowsResourceNotFoundException")
    void testGetUserById_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserUid()).thenReturn(currentUserUid);
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        when(userGetPersistencePort.findById(targetUid)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> userGetByIdService.getUserById(targetUid));
    }

    @Test
    @DisplayName("testGetUserById_Unauthenticated_ThrowsUnauthorized")
    void testGetUserById_Unauthenticated_ThrowsUnauthorized() {
        // Arrange
        when(securityContextHelper.getCurrentUserRole()).thenThrow(new RuntimeException("No auth"));

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> userGetByIdService.getUserById(targetUid));
    }

    @Test
    @DisplayName("testGetUserById_NullRole_ThrowsUnauthorized")
    void testGetUserById_NullRole_ThrowsUnauthorized() {
        // Arrange
        when(securityContextHelper.getCurrentUserRole()).thenReturn(null);
        when(securityContextHelper.getCurrentUserUid()).thenReturn(currentUserUid);
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> userGetByIdService.getUserById(targetUid));
    }
}
