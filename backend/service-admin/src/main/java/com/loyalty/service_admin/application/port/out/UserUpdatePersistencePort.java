package com.loyalty.service_admin.application.port.out;

import com.loyalty.service_admin.domain.entity.UserEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida para acceso a datos de actualización de usuarios.
 * Abstrae la persistencia del servicio de negocio.
 *
 * Implementadores: JpaUserUpdateAdapter
 */
public interface UserUpdatePersistencePort {
    
    /**
     * Busca un usuario por su UUID.
     * 
     * @param uid UUID del usuario
     * @return Optional con el usuario si existe
     */
    Optional<UserEntity> findById(UUID uid);
    
    /**
     * Verifica si existe un usuario con el email dado (excluyendo un UID específico).
     * Utilizado para validar cambio de email sin conflictos.
     * 
     * @param email email a buscar
     * @param excludeUid UID a excluir de la búsqueda (el usuario actual)
     * @return true si existe otro usuario con ese email, false en otro caso
     */
    boolean existsByEmailExcludingUid(String email, UUID excludeUid);
    
    /**
     * Verifica si existe un usuario con el username dado (excluyendo un UID específico).
     * Utilizado para validar cambio de username sin conflictos.
     * 
     * @param username username a buscar
     * @param excludeUid UID a excluir de la búsqueda (el usuario actual)
     * @return true si existe otro usuario con ese username, false en otro caso
     */
    boolean existsByUsernameExcludingUid(String username, UUID excludeUid);
    
    /**
     * Actualiza un usuario en la base de datos.
     * 
     * @param user entidad del usuario a actualizar
     * @return el usuario actualizado
     */
    UserEntity save(UserEntity user);
}
