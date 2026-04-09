package com.loyalty.service_admin.application.port.out;

import com.loyalty.service_admin.domain.entity.UserEntity;

import java.util.Optional;

public interface AuthPersistencePort {

    /**
     * Busca usuario por username
     * 
     * @param username nombre de usuario
     * @return Optional con UserEntity si existe
     */
    Optional<UserEntity> findByUsername(String username);
}
