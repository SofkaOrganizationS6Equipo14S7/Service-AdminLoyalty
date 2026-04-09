package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.user.UserResponse;
import com.loyalty.service_admin.application.port.out.UserListPersistencePort;
import com.loyalty.service_admin.domain.entity.RoleEntity;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.infrastructure.exception.AuthorizationException;
import com.loyalty.service_admin.infrastructure.exception.UnauthorizedException;
import com.loyalty.service_admin.infrastructure.security.SecurityContextHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserListService Unit Tests")
class UserListServiceTest {

    @Mock private UserListPersistencePort userListPersistencePort;
    @Mock private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private UserListService userListService;

    private UUID ecommerceId;
    private Pageable pageable;
    private UserEntity user1;

    @BeforeEach
    void setUp() {
        ecommerceId = UUID.randomUUID();
        pageable = PageRequest.of(0, 10);

        RoleEntity role = new RoleEntity();
        role.setId(UUID.randomUUID());
        role.setName("STORE_USER");

        user1 = UserEntity.builder()
                .id(UUID.randomUUID())
                .username("user1")
                .email("user1@test.com")
                .roleId(role.getId())
                .role(role)
                .ecommerceId(ecommerceId)
                .isActive(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("testListUsers_SuperAdmin_AllUsers")
    void testListUsers_SuperAdmin_AllUsers() {
        // Arrange
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        when(userListPersistencePort.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user1)));

        // Act
        Page<UserResponse> result = userListService.listUsers(null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(userListPersistencePort).findAll(pageable);
    }

    @Test
    @DisplayName("testListUsers_SuperAdmin_FilterByEcommerce")
    void testListUsers_SuperAdmin_FilterByEcommerce() {
        // Arrange
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        when(userListPersistencePort.findByEcommerceId(ecommerceId, pageable))
                .thenReturn(new PageImpl<>(List.of(user1)));

        // Act
        Page<UserResponse> result = userListService.listUsers(ecommerceId, pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        verify(userListPersistencePort).findByEcommerceId(ecommerceId, pageable);
    }

    @Test
    @DisplayName("testListUsers_StoreAdmin_OwnEcommerce")
    void testListUsers_StoreAdmin_OwnEcommerce() {
        // Arrange
        when(securityContextHelper.getCurrentUserRole()).thenReturn("STORE_ADMIN");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);
        when(userListPersistencePort.findByEcommerceId(ecommerceId, pageable))
                .thenReturn(new PageImpl<>(List.of(user1)));

        // Act
        Page<UserResponse> result = userListService.listUsers(null, pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        verify(userListPersistencePort).findByEcommerceId(ecommerceId, pageable);
    }

    @Test
    @DisplayName("testListUsers_StoreAdmin_CrossEcommerce_ThrowsAuthorizationException")
    void testListUsers_StoreAdmin_CrossEcommerce_ThrowsAuthorizationException() {
        // Arrange
        UUID otherEcommerce = UUID.randomUUID();
        when(securityContextHelper.getCurrentUserRole()).thenReturn("STORE_ADMIN");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);

        // Act & Assert
        assertThrows(AuthorizationException.class, () -> userListService.listUsers(otherEcommerce, pageable));
    }

    @Test
    @DisplayName("testListUsers_StandardUser_ThrowsAuthorizationException")
    void testListUsers_StandardUser_ThrowsAuthorizationException() {
        // Arrange
        when(securityContextHelper.getCurrentUserRole()).thenReturn("STORE_USER");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);

        // Act & Assert
        assertThrows(AuthorizationException.class, () -> userListService.listUsers(null, pageable));
    }

    @Test
    @DisplayName("testListUsers_Unauthenticated_ThrowsUnauthorized")
    void testListUsers_Unauthenticated_ThrowsUnauthorized() {
        // Arrange
        when(securityContextHelper.getCurrentUserRole()).thenThrow(new RuntimeException("No auth"));

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> userListService.listUsers(null, pageable));
    }

    @Test
    @DisplayName("testListUsers_NullRole_ThrowsUnauthorized")
    void testListUsers_NullRole_ThrowsUnauthorized() {
        // Arrange
        when(securityContextHelper.getCurrentUserRole()).thenReturn(null);
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> userListService.listUsers(null, pageable));
    }
}
