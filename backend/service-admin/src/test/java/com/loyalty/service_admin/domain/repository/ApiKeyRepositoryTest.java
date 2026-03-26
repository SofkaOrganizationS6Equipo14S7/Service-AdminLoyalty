package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.ApiKeyEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para ApiKeyRepository usando @DataJpaTest.
 * 
 * Cubre:
 * - Queries JPA (findByHashedKey, findByEcommerceId)
 * - Unicidad de hashed_key (UNIQUE constraint)
 * - Foreign key relationships
 * - Persistencia y recuperación de datos
 */
@DataJpaTest
class ApiKeyRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private ApiKeyRepository apiKeyRepository;
    
    private UUID testEcommerceId = UUID.fromString("220e8400-e29b-41d4-a716-446655440111");
    private UUID testEcommerceId2 = UUID.fromString("330e8400-e29b-41d4-a716-446655440222");
    
    @BeforeEach
    void setUp() {
        // DataJpaTest usa transacción. No es necesario limpiar después de cada test.
    }
    
    /**
     * CRITERIO-3.1.1: Crear API Key exitosamente
     * Verifica que una API Key se puede persistir y recuperar correctamente.
     */
    @Test
    void createAndSaveApiKey_success() {
        // Arrange
        String hashedKey = "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3";
        ApiKeyEntity entity = new ApiKeyEntity();
        entity.setHashedKey(hashedKey);
        entity.setEcommerceId(testEcommerceId);
        
        // Act
        ApiKeyEntity saved = apiKeyRepository.save(entity);
        entityManager.flush();
        
        // Assert
        assertNotNull(saved.getId());
        assertEquals(hashedKey, saved.getHashedKey());
        assertEquals(testEcommerceId, saved.getEcommerceId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }
    
    /**
     * Verifica que las API Keys con el mismo hash no pueden existir.
     * UNIQUE constraint en HASHED_KEY.
     */
    @Test
    void createApiKey_duplicateHashedKey_violatesUniqueConstraint() {
        // Arrange
        String hashedKey = "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3";
        
        ApiKeyEntity key1 = new ApiKeyEntity();
        key1.setHashedKey(hashedKey);
        key1.setEcommerceId(testEcommerceId);
        
        ApiKeyEntity key2 = new ApiKeyEntity();
        key2.setHashedKey(hashedKey);
        key2.setEcommerceId(testEcommerceId2);
        
        apiKeyRepository.save(key1);
        entityManager.flush();
        
        // Act & Assert
        assertThrows(Exception.class, () -> {
            apiKeyRepository.save(key2);
            entityManager.flush();
        });
    }
    
    /**
     * CRITERIO-3.2.1: Listar API Keys de un ecommerce
     * Verifica que findByEcommerceId retorna solo las keys del ecommerce solicitado.
     */
    @Test
    void findByEcommerceId_success() {
        // Arrange
        ApiKeyEntity key1 = createAndSaveKey("hash1", testEcommerceId);
        ApiKeyEntity key2 = createAndSaveKey("hash2", testEcommerceId);
        ApiKeyEntity key3 = createAndSaveKey("hash3", testEcommerceId2);
        
        // Act
        List<ApiKeyEntity> keysForEcommerce1 = apiKeyRepository.findByEcommerceId(testEcommerceId);
        List<ApiKeyEntity> keysForEcommerce2 = apiKeyRepository.findByEcommerceId(testEcommerceId2);
        
        // Assert
        assertEquals(2, keysForEcommerce1.size());
        assertEquals(1, keysForEcommerce2.size());
        assertTrue(keysForEcommerce1.stream().anyMatch(k -> k.getHashedKey().equals("hash1")));
        assertTrue(keysForEcommerce1.stream().anyMatch(k -> k.getHashedKey().equals("hash2")));
        assertTrue(keysForEcommerce2.stream().anyMatch(k -> k.getHashedKey().equals("hash3")));
    }
    
    /**
     * CRITERIO-3.2.2: Listar vacío si no hay keys
     */
    @Test
    void findByEcommerceId_empty() {
        // Arrange
        UUID unusedEcommerceId = UUID.randomUUID();
        
        // Act
        List<ApiKeyEntity> keys = apiKeyRepository.findByEcommerceId(unusedEcommerceId);
        
        // Assert
        assertTrue(keys.isEmpty());
    }
    
    /**
     * CRITERIO-3.3.1: Validar API Key en caché
     * Verifica que findByHashedKey retorna la key correcta para validación.
     */
    @Test
    void findByHashedKey_success() {
        // Arrange
        String hashedKey = "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3";
        createAndSaveKey(hashedKey, testEcommerceId);
        
        // Act
        Optional<ApiKeyEntity> found = apiKeyRepository.findByHashedKey(hashedKey);
        
        // Assert
        assertTrue(found.isPresent());
        assertEquals(hashedKey, found.get().getHashedKey());
        assertEquals(testEcommerceId, found.get().getEcommerceId());
    }
    
    /**
     * Verifica que findByHashedKey retorna empty para una key inexistente.
     */
    @Test
    void findByHashedKey_notFound() {
        // Arrange
        String nonExistentHash = "nonexistent";
        
        // Act
        Optional<ApiKeyEntity> found = apiKeyRepository.findByHashedKey(nonExistentHash);
        
        // Assert
        assertTrue(found.isEmpty());
    }
    
    /**
     * CRITERIO-3.4.1: Eliminar API Key exitosamente
     * Verifica que una key se puede eliminar correctamente.
     */
    @Test
    void deleteApiKey_success() {
        // Arrange
        ApiKeyEntity key = createAndSaveKey("hash_to_delete", testEcommerceId);
        UUID keyId = key.getId();
        
        assertTrue(apiKeyRepository.findById(keyId).isPresent());
        
        // Act
        apiKeyRepository.delete(key);
        entityManager.flush();
        
        // Assert
        assertTrue(apiKeyRepository.findById(keyId).isEmpty());
    }
    
    /**
     * Verifica que findById retorna empty para un ID inexistente.
     */
    @Test
    void findById_notFound() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        
        // Act
        Optional<ApiKeyEntity> found = apiKeyRepository.findById(nonExistentId);
        
        // Assert
        assertTrue(found.isEmpty());
    }
    
    /**
     * Verifica que todas las API Keys se pueden recuperar.
     */
    @Test
    void findAll_success() {
        // Arrange
        createAndSaveKey("hash1", testEcommerceId);
        createAndSaveKey("hash2", testEcommerceId);
        createAndSaveKey("hash3", testEcommerceId2);
        
        // Act
        List<ApiKeyEntity> allKeys = apiKeyRepository.findAll();
        
        // Assert
        assertEquals(3, allKeys.size());
    }
    
    /**
     * Verifica que findAll retorna lista vacía cuando no hay keys.
     */
    @Test
    void findAll_empty() {
        // Act
        List<ApiKeyEntity> allKeys = apiKeyRepository.findAll();
        
        // Assert
        assertTrue(allKeys.isEmpty());
    }
    
    /**
     * Verifica que el timestamp created_at se asigna automáticamente.
     */
    @Test
    void createApiKey_createdAtIsAutoAssigned() {
        // Arrange
        ApiKeyEntity entity = new ApiKeyEntity();
        entity.setHashedKey("hash_with_timestamp");
        entity.setEcommerceId(testEcommerceId);
        
        // Act
        ApiKeyEntity saved = apiKeyRepository.save(entity);
        entityManager.flush();
        
        // Assert
        assertNotNull(saved.getCreatedAt());
        assertFalse(saved.getCreatedAt().isAfter(Instant.now().plusSeconds(1)));
    }
    
    /**
     * Verifica que el timestamp updated_at se actualiza en UPDATE.
     */
    @Test
    void updateApiKey_updatedAtIsRefreshed() throws InterruptedException {
        // Arrange
        ApiKeyEntity entity = createAndSaveKey("hash_for_update", testEcommerceId);
        Instant createdAtOriginal = entity.getCreatedAt();
        Instant updatedAtOriginal = entity.getUpdatedAt();
        
        Thread.sleep(100); // Asegurar que hay diferencia de tiempo
        
        // Act
        entity.setEcommerceId(testEcommerceId2); // Cambiar algo
        ApiKeyEntity updated = apiKeyRepository.save(entity);
        entityManager.flush();
        
        // Assert
        assertEquals(createdAtOriginal, updated.getCreatedAt()); // created_at no cambia
        assertTrue(updated.getUpdatedAt().isAfter(updatedAtOriginal)); // updated_at cambia
    }
    
    /**
     * Helper para crear y persistir una API Key.
     */
    private ApiKeyEntity createAndSaveKey(String hashedKey, UUID ecommerceId) {
        ApiKeyEntity entity = new ApiKeyEntity();
        entity.setHashedKey(hashedKey);
        entity.setEcommerceId(ecommerceId);
        ApiKeyEntity saved = apiKeyRepository.save(entity);
        entityManager.flush();
        return saved;
    }
}
