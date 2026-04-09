package com.loyalty.service_admin.infrastructure.persistence.jpa;

import com.loyalty.service_admin.domain.entity.ApiKeyEntity;
import com.loyalty.service_admin.domain.entity.EcommerceEntity;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.domain.repository.ApiKeyRepository;
import com.loyalty.service_admin.domain.repository.EcommerceRepository;
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
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaEcommerceAdapter Unit Tests")
class JpaEcommerceAdapterTest {

    @Mock
    private EcommerceRepository ecommerceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @InjectMocks
    private JpaEcommerceAdapter adapter;

    @Test
    void testSave() {
        // Arrange
        EcommerceEntity entity = new EcommerceEntity();
        when(ecommerceRepository.save(entity)).thenReturn(entity);

        // Act & Assert
        assertEquals(entity, adapter.save(entity));
    }

    @Test
    void testFindById_found() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(ecommerceRepository.findById(id)).thenReturn(Optional.of(new EcommerceEntity()));

        // Act & Assert
        assertTrue(adapter.findById(id).isPresent());
    }

    @Test
    void testFindById_notFound() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(ecommerceRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertTrue(adapter.findById(id).isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testFindAll() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<EcommerceEntity> page = new PageImpl<>(List.of(new EcommerceEntity()));
        when(ecommerceRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        // Act
        Page<EcommerceEntity> result = adapter.findAll(mock(Specification.class), pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testExistsBySlug() {
        // Arrange
        when(ecommerceRepository.existsBySlug("test-slug")).thenReturn(true);

        // Act & Assert
        assertTrue(adapter.existsBySlug("test-slug"));
    }

    @Test
    void testExistsById() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(ecommerceRepository.existsById(id)).thenReturn(true);

        // Act & Assert
        assertTrue(adapter.existsById(id));
    }

    @Test
    void testFindUsersByEcommerceId() {
        // Arrange
        UUID ecommerceId = UUID.randomUUID();
        List<UserEntity> users = List.of(new UserEntity());
        when(userRepository.findByEcommerceId(ecommerceId)).thenReturn(users);

        // Act & Assert
        assertEquals(1, adapter.findUsersByEcommerceId(ecommerceId).size());
    }

    @Test
    void testInactivateUsers_success() {
        // Arrange
        UserEntity user1 = new UserEntity();
        user1.setIsActive(true);
        user1.setId(UUID.randomUUID());
        user1.setUsername("user1");
        List<UserEntity> users = new ArrayList<>(List.of(user1));

        // Act
        adapter.inactivateUsers(users);

        // Assert
        assertFalse(user1.getIsActive());
        verify(userRepository).saveAll(users);
    }

    @Test
    void testInactivateUsers_emptyList() {
        // Act
        adapter.inactivateUsers(new ArrayList<>());

        // Assert
        verify(userRepository, never()).saveAll(any());
    }

    @Test
    void testInactivateUsers_nullList() {
        // Act
        adapter.inactivateUsers(null);

        // Assert
        verify(userRepository, never()).saveAll(any());
    }

    @Test
    void testFindApiKeysByEcommerceId() {
        // Arrange
        UUID ecommerceId = UUID.randomUUID();
        List<ApiKeyEntity> keys = List.of(new ApiKeyEntity());
        when(apiKeyRepository.findByEcommerceId(ecommerceId)).thenReturn(keys);

        // Act & Assert
        assertEquals(1, adapter.findApiKeysByEcommerceId(ecommerceId).size());
    }

    @Test
    void testDeactivateApiKeys_success() {
        // Arrange
        ApiKeyEntity key = new ApiKeyEntity();
        key.setIsActive(true);
        key.setId(UUID.randomUUID());
        List<ApiKeyEntity> keys = new ArrayList<>(List.of(key));

        // Act
        adapter.deactivateApiKeys(keys);

        // Assert
        assertFalse(key.getIsActive());
        verify(apiKeyRepository).saveAll(keys);
    }

    @Test
    void testDeactivateApiKeys_emptyList() {
        // Act
        adapter.deactivateApiKeys(new ArrayList<>());

        // Assert
        verify(apiKeyRepository, never()).saveAll(any());
    }

    @Test
    void testDeactivateApiKeys_nullList() {
        // Act
        adapter.deactivateApiKeys(null);

        // Assert
        verify(apiKeyRepository, never()).saveAll(any());
    }
}
