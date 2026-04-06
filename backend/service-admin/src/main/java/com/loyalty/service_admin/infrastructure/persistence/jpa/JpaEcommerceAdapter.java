package com.loyalty.service_admin.infrastructure.persistence.jpa;

import com.loyalty.service_admin.application.port.out.EcommercePersistencePort;
import com.loyalty.service_admin.domain.entity.EcommerceEntity;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.domain.entity.ApiKeyEntity;
import com.loyalty.service_admin.domain.repository.EcommerceRepository;
import com.loyalty.service_admin.domain.repository.UserRepository;
import com.loyalty.service_admin.domain.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de Persistencia: JPA/Hibernate
 * 
 * Implementa el puerto EcommercePersistencePort usando Spring Data JPA.
 * Delegación pura a los repositories sin lógica de negocio adicional.
 * 
 * SPEC-015: Ecommerce Onboarding con Arquitectura Hexagonal
 * Hexagonal Architecture: Adapter pattern para persistencia
 * 
 * Si en el futuro necesitamos cambiar a MongoDB o cualquier otra BD,
 * solo crearemos un nuevo adaptador sin afectar la lógica de negocio.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JpaEcommerceAdapter implements EcommercePersistencePort {
    
    private final EcommerceRepository ecommerceRepository;
    private final UserRepository userRepository;
    private final ApiKeyRepository apiKeyRepository;
    
    // ==================== Operaciones CRUD en Ecommerce ====================
    
    @Override
    @Transactional
    public EcommerceEntity save(EcommerceEntity entity) {
        log.debug("Persistiendo ecommerce: uid={}, name={}, slug={}", 
                entity.getId(), entity.getName(), entity.getSlug());
        return ecommerceRepository.save(entity);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<EcommerceEntity> findById(UUID id) {
        log.debug("Buscando ecommerce por uid: {}", id);
        return ecommerceRepository.findById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<EcommerceEntity> findAll(Specification<EcommerceEntity> spec, Pageable pageable) {
        log.debug("Listando ecommerces con especificación, page={}, size={}", 
                pageable.getPageNumber(), pageable.getPageSize());
        return ecommerceRepository.findAll(spec, pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsBySlug(String slug) {
        log.debug("Verificando si slug existe: {}", slug);
        return ecommerceRepository.existsBySlug(slug);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        log.debug("Verificando si ecommerce existe: {}", id);
        return ecommerceRepository.existsById(id);
    }
    
    // ==================== Cascada: Usuarios ====================
    
    @Override
    @Transactional(readOnly = true)
    public List<UserEntity> findUsersByEcommerceId(UUID ecommerceId) {
        log.debug("Buscando usuarios vinculados al ecommerce: {}", ecommerceId);
        return userRepository.findByEcommerceId(ecommerceId);
    }
    
    @Override
    @Transactional
    public void inactivateUsers(List<UserEntity> users) {
        if (users == null || users.isEmpty()) {
            log.debug("No hay usuarios para inactivar");
            return;
        }
        
        log.info("Inactivando {} usuarios por cascada de ecommerce", users.size());
        users.forEach(user -> {
            user.setIsActive(false);
            log.debug("Usuario marcado como inactivo: uid={}, username={}", user.getId(), user.getUsername());
        });
        
        userRepository.saveAll(users);
        log.info("Usuarios inactivados exitosamente: count={}", users.size());
    }
    
    // ==================== Cascada: API Keys ====================
    
    @Override
    @Transactional(readOnly = true)
    public List<ApiKeyEntity> findApiKeysByEcommerceId(UUID ecommerceId) {
        log.debug("Buscando API Keys vinculadas al ecommerce: {}", ecommerceId);
        return apiKeyRepository.findByEcommerceId(ecommerceId);
    }
    
    @Override
    @Transactional
    public void deactivateApiKeys(List<ApiKeyEntity> keys) {
        if (keys == null || keys.isEmpty()) {
            log.debug("No hay API Keys para desactivar");
            return;
        }
        
        log.info("Desactivando {} API Keys por cascada de ecommerce", keys.size());
        keys.forEach(key -> {
            key.setIsActive(false);
            log.debug("API Key marcada como inactiva: uid={}", key.getId());
        });
        
        apiKeyRepository.saveAll(keys);
        log.info("API Keys desactivadas exitosamente: count={}", keys.size());
    }
}
