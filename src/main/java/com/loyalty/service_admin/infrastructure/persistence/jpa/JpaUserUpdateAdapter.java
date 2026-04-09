package com.loyalty.service_admin.infrastructure.persistence.jpa;

import com.loyalty.service_admin.application.port.out.UserUpdatePersistencePort;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter JPA que implementa el puerto de persistencia para actualización de usuarios.
 *
 * Responsabilidad: Delegar operaciones CRUD al JpaRepository.
 * Aquí SÍ es permitido inyectar UserRepository (es un adapter).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JpaUserUpdateAdapter implements UserUpdatePersistencePort {
    
    private final UserRepository userRepository;
    
    @Override
    public Optional<UserEntity> findById(UUID uid) {
        return userRepository.findById(uid);
    }
    
    @Override
    public boolean existsByEmailExcludingUid(String email, UUID excludeUid) {
        Optional<UserEntity> existing = userRepository.findByEmail(email);
        if (existing.isEmpty()) {
            return false;
        }
        return !existing.get().getId().equals(excludeUid);
    }
    
    @Override
    public boolean existsByUsernameExcludingUid(String username, UUID excludeUid) {
        Optional<UserEntity> existing = userRepository.findByUsername(username);
        if (existing.isEmpty()) {
            return false;
        }
        return !existing.get().getId().equals(excludeUid);
    }
    
    @Override
    public UserEntity save(UserEntity user) {
        UserEntity saved = userRepository.save(user);
        log.debug("Usuario actualizado en BD: uid={}, username={}", saved.getId(), saved.getUsername());
        return saved;
    }
}
