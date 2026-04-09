package com.loyalty.service_admin.application.port.out;

import com.loyalty.service_admin.domain.entity.UserEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida para acceso a datos de obtención de usuarios.
 * Abstrae la persistencia del servicio de negocio.
 *
 * Implementadores: JpaUserGetAdapter
 */
public interface UserGetPersistencePort {
    
    /**
     * Busca un usuario por su UID único.
     * 
     * @param uid identificador único del usuario
     * @return Optional con el usuario si existe
     */
    Optional<UserEntity> findById(UUID uid);
}
