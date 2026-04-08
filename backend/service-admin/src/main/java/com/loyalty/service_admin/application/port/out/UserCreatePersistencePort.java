package com.loyalty.service_admin.application.port.out;

import com.loyalty.service_admin.domain.entity.UserEntity;

/**
 * Puerto de salida para acceso a datos de creación de usuarios.
 * Abstrae la persistencia del servicio de negocio.
 *
 * Implementadores: JpaUserCreateAdapter
 */
public interface UserCreatePersistencePort {
    
    /**
     * Verifica si existe un usuario con el username dado.
     * 
     * @param username nombre de usuario a buscar
     * @return true si existe, false en otro caso
     */
    boolean existsByUsername(String username);
    
    /**
     * Verifica si existe un usuario con el email dado.
     * 
     * @param email email a buscar
     * @return true si existe, false en otro caso
     */
    boolean existsByEmail(String email);
    
    /**
     * Persiste un nuevo usuario en la base de datos.
     * 
     * @param user entidad del usuario a crear
     * @return el usuario creado con ID asignado
     */
    UserEntity save(UserEntity user);
}
