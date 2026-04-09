package com.loyalty.service_admin.infrastructure.persistence.jpa;

import com.loyalty.service_admin.application.port.out.UserListPersistencePort;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adapter JPA que implementa el puerto de persistencia para listado de usuarios.
 *
 * Responsabilidad: Delegar operaciones CRUD al JpaRepository.
 * Aquí SÍ es permitido inyectar UserRepository (es un adapter).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JpaUserListAdapter implements UserListPersistencePort {
    
    private final UserRepository userRepository;
    
    @Override
    public Page<UserEntity> findAll(Pageable pageable) {
        log.debug("Buscando todos los usuarios con paginación: page={}, size={}", 
                pageable.getPageNumber(), pageable.getPageSize());
        return userRepository.findAll(pageable);
    }
    
    @Override
    public Page<UserEntity> findByEcommerceId(UUID ecommerceId, Pageable pageable) {
        log.debug("Buscando usuarios por ecommerce: ecommerceId={}, page={}, size={}", 
                ecommerceId, pageable.getPageNumber(), pageable.getPageSize());
        return userRepository.findByEcommerceId(ecommerceId, pageable);
    }
}
