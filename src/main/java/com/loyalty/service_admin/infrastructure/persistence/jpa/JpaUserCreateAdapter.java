package com.loyalty.service_admin.infrastructure.persistence.jpa;

import com.loyalty.service_admin.application.port.out.UserCreatePersistencePort;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adapter JPA que implementa el puerto de persistencia para creación de usuarios.
 *
 * Responsabilidad: Delegar operaciones CRUD al JpaRepository.
 * Aquí SÍ es permitido inyectar UserRepository (es un adapter).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JpaUserCreateAdapter implements UserCreatePersistencePort {
    
    private final UserRepository userRepository;
    
    @Override
    public boolean existsByUsername(String username) {
        return userRepository.findByUsername(username).isPresent();
    }
    
    @Override
    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
    
    @Override
    public UserEntity save(UserEntity user) {
        UserEntity saved = userRepository.save(user);
        log.debug("Usuario creado en BD: uid={}, username={}", saved.getId(), saved.getUsername());
        return saved;
    }
}
