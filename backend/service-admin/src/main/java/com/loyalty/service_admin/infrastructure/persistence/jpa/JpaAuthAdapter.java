package com.loyalty.service_admin.infrastructure.persistence.jpa;

import com.loyalty.service_admin.application.port.out.AuthPersistencePort;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adapter de persistencia para AuthPersistencePort.
 * 
 * Implementa las operaciones de persistencia delegando al UserRepository (JPA).
 * Reduce el acoplamiento: AuthServiceImpl solo conoce el puerto, no el repositorio.
 */
@Component
@RequiredArgsConstructor
public class JpaAuthAdapter implements AuthPersistencePort {

    private final UserRepository userRepository;

    /**
     * Busca usuario por username
     * 
     * @param username nombre de usuario
     * @return Optional con UserEntity si existe
     */
    @Override
    public Optional<UserEntity> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
