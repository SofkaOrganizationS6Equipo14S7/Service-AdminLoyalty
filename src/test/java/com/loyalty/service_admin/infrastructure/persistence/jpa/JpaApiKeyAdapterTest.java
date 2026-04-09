package com.loyalty.service_admin.infrastructure.persistence.jpa;

import com.loyalty.service_admin.domain.entity.ApiKeyEntity;
import com.loyalty.service_admin.domain.repository.ApiKeyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaApiKeyAdapter Unit Tests")
class JpaApiKeyAdapterTest {

    @Mock
    private ApiKeyRepository repository;

    @InjectMocks
    private JpaApiKeyAdapter adapter;

    @Test
    void testSave_delegatesToRepository() {
        // Arrange
        ApiKeyEntity entity = new ApiKeyEntity();
        when(repository.save(entity)).thenReturn(entity);

        // Act
        ApiKeyEntity result = adapter.save(entity);

        // Assert
        assertEquals(entity, result);
        verify(repository).save(entity);
    }

    @Test
    void testFindById_found() {
        // Arrange
        UUID id = UUID.randomUUID();
        ApiKeyEntity entity = new ApiKeyEntity();
        when(repository.findById(id)).thenReturn(Optional.of(entity));

        // Act
        Optional<ApiKeyEntity> result = adapter.findById(id);

        // Assert
        assertTrue(result.isPresent());
    }

    @Test
    void testFindById_notFound() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        // Act
        Optional<ApiKeyEntity> result = adapter.findById(id);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindByEcommerceId() {
        // Arrange
        UUID ecommerceId = UUID.randomUUID();
        List<ApiKeyEntity> entities = List.of(new ApiKeyEntity());
        when(repository.findByEcommerceId(ecommerceId)).thenReturn(entities);

        // Act
        List<ApiKeyEntity> result = adapter.findByEcommerceId(ecommerceId);

        // Assert
        assertEquals(1, result.size());
    }

    @Test
    void testDeleteById() {
        // Arrange
        UUID id = UUID.randomUUID();

        // Act
        adapter.deleteById(id);

        // Assert
        verify(repository).deleteById(id);
    }

    @Test
    void testExistsByHashedKey_true() {
        // Arrange
        when(repository.existsByHashedKey("hash")).thenReturn(true);

        // Act & Assert
        assertTrue(adapter.existsByHashedKey("hash"));
    }

    @Test
    void testExistsByHashedKey_false() {
        // Arrange
        when(repository.existsByHashedKey("hash")).thenReturn(false);

        // Act & Assert
        assertFalse(adapter.existsByHashedKey("hash"));
    }
}
