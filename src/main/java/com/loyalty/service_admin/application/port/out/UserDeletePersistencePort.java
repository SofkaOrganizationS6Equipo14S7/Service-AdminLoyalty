package com.loyalty.service_admin.application.port.out;

import com.loyalty.service_admin.domain.entity.UserEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida para acceso a datos de eliminación de usuarios.
 * Abstrae la persistencia del servicio de negocio.
 *
 * Implementadores: JpaUserDeleteAdapter
 */
public interface UserDeletePersistencePort {
    
    /**
     * Busca un usuario por su UUID.
     * 
     * @param uid UUID del usuario
     * @return Optional con el usuario si existe
     */
    Optional<UserEntity> findById(UUID uid);
    
    /**
     * Elimina físicamente un usuario de la base de datos (hard delete).
     * 
     * IMPORTANTE: Debe ser llamado DESPUÉS de auditUserDeletion
     * para garantizar que los datos se registren antes de ser eliminados.
     * 
     * @param user entidad del usuario a eliminar
     */
    void deleteUser(UserEntity user);
}
