package com.loyalty.service_admin.infrastructure.persistence.jpa;

import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.domain.repository.UserRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JPA User Adapters Unit Tests")
class JpaUserAdaptersTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JpaUserCreateAdapter createAdapter;

    @InjectMocks
    private JpaUserDeleteAdapter deleteAdapter;

    @InjectMocks
    private JpaUserGetAdapter getAdapter;

    @InjectMocks
    private JpaUserListAdapter listAdapter;

    @InjectMocks
    private JpaUserUpdateAdapter updateAdapter;

    // ==================== Create Adapter ====================

    @Test
    void testCreate_existsByUsername_true() {
        // Arrange
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(new UserEntity()));

        // Act & Assert
        assertTrue(createAdapter.existsByUsername("admin"));
    }

    @Test
    void testCreate_existsByUsername_false() {
        // Arrange
        when(userRepository.findByUsername("new")).thenReturn(Optional.empty());

        // Act & Assert
        assertFalse(createAdapter.existsByUsername("new"));
    }

    @Test
    void testCreate_existsByEmail_true() {
        // Arrange
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(new UserEntity()));

        // Act & Assert
        assertTrue(createAdapter.existsByEmail("test@test.com"));
    }

    @Test
    void testCreate_existsByEmail_false() {
        // Arrange
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertFalse(createAdapter.existsByEmail("new@test.com"));
    }

    @Test
    void testCreate_save() {
        // Arrange
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setUsername("newuser");
        when(userRepository.save(user)).thenReturn(user);

        // Act
        UserEntity result = createAdapter.save(user);

        // Assert
        assertEquals(user, result);
    }

    // ==================== Delete Adapter ====================

    @Test
    void testDelete_findById_found() {
        // Arrange
        UUID uid = UUID.randomUUID();
        when(userRepository.findById(uid)).thenReturn(Optional.of(new UserEntity()));

        // Act & Assert
        assertTrue(deleteAdapter.findById(uid).isPresent());
    }

    @Test
    void testDelete_findById_notFound() {
        // Arrange
        UUID uid = UUID.randomUUID();
        when(userRepository.findById(uid)).thenReturn(Optional.empty());

        // Act & Assert
        assertTrue(deleteAdapter.findById(uid).isEmpty());
    }

    @Test
    void testDelete_deleteUser() {
        // Arrange
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());

        // Act
        deleteAdapter.deleteUser(user);

        // Assert
        verify(userRepository).delete(user);
    }

    // ==================== Get Adapter ====================

    @Test
    void testGet_findById() {
        // Arrange
        UUID uid = UUID.randomUUID();
        when(userRepository.findById(uid)).thenReturn(Optional.of(new UserEntity()));

        // Act & Assert
        assertTrue(getAdapter.findById(uid).isPresent());
    }

    // ==================== List Adapter ====================

    @Test
    void testList_findAll() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserEntity> page = new PageImpl<>(List.of(new UserEntity()));
        when(userRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<UserEntity> result = listAdapter.findAll(pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testList_findByEcommerceId() {
        // Arrange
        UUID ecommerceId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserEntity> page = new PageImpl<>(List.of(new UserEntity()));
        when(userRepository.findByEcommerceId(ecommerceId, pageable)).thenReturn(page);

        // Act
        Page<UserEntity> result = listAdapter.findByEcommerceId(ecommerceId, pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
    }

    // ==================== Update Adapter ====================

    @Test
    void testUpdate_findById() {
        // Arrange
        UUID uid = UUID.randomUUID();
        when(userRepository.findById(uid)).thenReturn(Optional.of(new UserEntity()));

        // Act & Assert
        assertTrue(updateAdapter.findById(uid).isPresent());
    }

    @Test
    void testUpdate_existsByEmailExcludingUid_exists() {
        // Arrange
        UUID excludeUid = UUID.randomUUID();
        UUID otherUid = UUID.randomUUID();
        UserEntity existing = new UserEntity();
        existing.setId(otherUid);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(existing));

        // Act & Assert
        assertTrue(updateAdapter.existsByEmailExcludingUid("test@test.com", excludeUid));
    }

    @Test
    void testUpdate_existsByEmailExcludingUid_sameUser() {
        // Arrange
        UUID uid = UUID.randomUUID();
        UserEntity existing = new UserEntity();
        existing.setId(uid);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(existing));

        // Act & Assert
        assertFalse(updateAdapter.existsByEmailExcludingUid("test@test.com", uid));
    }

    @Test
    void testUpdate_existsByEmailExcludingUid_notExists() {
        // Arrange
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertFalse(updateAdapter.existsByEmailExcludingUid("new@test.com", UUID.randomUUID()));
    }

    @Test
    void testUpdate_existsByUsernameExcludingUid_exists() {
        // Arrange
        UUID excludeUid = UUID.randomUUID();
        UUID otherUid = UUID.randomUUID();
        UserEntity existing = new UserEntity();
        existing.setId(otherUid);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existing));

        // Act & Assert
        assertTrue(updateAdapter.existsByUsernameExcludingUid("admin", excludeUid));
    }

    @Test
    void testUpdate_existsByUsernameExcludingUid_sameUser() {
        // Arrange
        UUID uid = UUID.randomUUID();
        UserEntity existing = new UserEntity();
        existing.setId(uid);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existing));

        // Act & Assert
        assertFalse(updateAdapter.existsByUsernameExcludingUid("admin", uid));
    }

    @Test
    void testUpdate_existsByUsernameExcludingUid_notExists() {
        // Arrange
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());

        // Act & Assert
        assertFalse(updateAdapter.existsByUsernameExcludingUid("newuser", UUID.randomUUID()));
    }

    @Test
    void testUpdate_save() {
        // Arrange
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setUsername("updated");
        when(userRepository.save(user)).thenReturn(user);

        // Act
        UserEntity result = updateAdapter.save(user);

        // Assert
        assertEquals(user, result);
    }
}
