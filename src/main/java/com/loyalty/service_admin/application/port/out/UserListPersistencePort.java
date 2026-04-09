package com.loyalty.service_admin.application.port.out;

import com.loyalty.service_admin.domain.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Puerto de salida para acceso a datos de listado de usuarios.
 * Abstrae la persistencia del servicio de negocio.
 *
 * Implementadores: JpaUserListAdapter
 */
public interface UserListPersistencePort {
    
    /**
     * Lista todos los usuarios del sistema con paginación.
     * 
     * @param pageable información de paginación
     * @return Page<UserEntity> paginada con todos los usuarios
     */
    Page<UserEntity> findAll(Pageable pageable);
    
    /**
     * Lista usuarios de un ecommerce específico con paginación.
     * 
     * @param ecommerceId UUID del ecommerce
     * @param pageable información de paginación
     * @return Page<UserEntity> paginada con usuarios del ecommerce
     */
    Page<UserEntity> findByEcommerceId(UUID ecommerceId, Pageable pageable);
}
