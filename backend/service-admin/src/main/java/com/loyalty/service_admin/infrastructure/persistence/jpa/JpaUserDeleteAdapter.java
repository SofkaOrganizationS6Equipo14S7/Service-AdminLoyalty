package com.loyalty.service_admin.infrastructure.persistence.jpa;

import com.loyalty.service_admin.application.port.out.UserDeletePersistencePort;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter JPA que implementa el puerto de persistencia para eliminación de usuarios.
 *
 * Responsabilidad: Delegar operaciones CRUD al JpaRepository.
 * Aquí SÍ es permitido inyectar UserRepository (es un adapter).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JpaUserDeleteAdapter implements UserDeletePersistencePort {
    
    private final UserRepository userRepository;
    
    @Override
    public Optional<UserEntity> findById(UUID uid) {
        return userRepository.findById(uid);
    }
    
    @Override
    public void deleteUser(UserEntity user) {
        userRepository.delete(user);
        log.debug("Usuario eliminado físicamente de BD: uid={}", user.getId());
    }
}
