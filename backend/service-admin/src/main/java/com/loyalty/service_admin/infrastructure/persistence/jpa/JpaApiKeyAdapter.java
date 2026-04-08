package com.loyalty.service_admin.infrastructure.persistence.jpa;

import com.loyalty.service_admin.application.port.out.ApiKeyPersistencePort;
import com.loyalty.service_admin.domain.entity.ApiKeyEntity;
import com.loyalty.service_admin.domain.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter que implementa el puerto de persistencia usando JPA.
 * Delega todas las operaciones al repositorio JPA sin agregar lógica adicional.
 * 
 * Responsabilidades:
 * - Traducir llamadas del puerto a operaciones del repositorio
 * - Mantener la interfaz limpia y agnóstica de la implementación
 */
@Component
@RequiredArgsConstructor
public class JpaApiKeyAdapter implements ApiKeyPersistencePort {
    
    private final ApiKeyRepository repository;
    
    @Override
    public ApiKeyEntity save(ApiKeyEntity entity) {
        return repository.save(entity);
    }
    
    @Override
    public Optional<ApiKeyEntity> findById(UUID id) {
        return repository.findById(id);
    }
    
    @Override
    public List<ApiKeyEntity> findByEcommerceId(UUID ecommerceId) {
        return repository.findByEcommerceId(ecommerceId);
    }
    
    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
    
    @Override
    public boolean existsByHashedKey(String hashedKey) {
        return repository.existsByHashedKey(hashedKey);
    }
}
