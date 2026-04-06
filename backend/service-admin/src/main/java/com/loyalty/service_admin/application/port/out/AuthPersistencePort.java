package com.loyalty.service_admin.application.port.out;

import com.loyalty.service_admin.domain.entity.UserEntity;

import java.util.Optional;

/**
 * Puerto de salida para persistencia de autenticación.
 * 
 * Abstrae operaciones de acceso a datos de usuario.
 * Implementación: JpaAuthAdapter (infrastructure/persistence/jpa/)
 */
public interface AuthPersistencePort {

    /**
     * Busca usuario por username
     * 
     * @param username nombre de usuario
     * @return Optional con UserEntity si existe
     */
    Optional<UserEntity> findByUsername(String username);
}
