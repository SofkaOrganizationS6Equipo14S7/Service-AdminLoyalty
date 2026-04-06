package com.loyalty.service_admin.infrastructure.persistence.jpa;

import com.loyalty.service_admin.application.port.out.UserGetPersistencePort;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter JPA que implementa el puerto de persistencia para obtención de usuarios.
 *
 * Responsabilidad: Delegar operaciones CRUD al JpaRepository.
 * Aquí SÍ es permitido inyectar UserRepository (es un adapter).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JpaUserGetAdapter implements UserGetPersistencePort {
    
    private final UserRepository userRepository;
    
    @Override
    public Optional<UserEntity> findById(UUID uid) {
        log.debug("Buscando usuario por UID: {}", uid);
        return userRepository.findById(uid);
    }
}
