package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    
    /**
     * Busca un usuario por su username (búsqueda global, no por ecommerce).
     * Utilizado en el login para validar credenciales.
     * Username es único en toda la plataforma (SPEC-002 RN-03).
     *
     * @param username nombre del usuario
     * @return Optional con el usuario si existe
     */
    Optional<UserEntity> findByUsername(String username);
    
    /**
     * Busca todos los usuarios de un ecommerce específico.
     * Utilizado en GET /api/v1/users?ecommerceId=X (solo super admin).
     * 
     * @param ecommerceId UUID del ecommerce
     * @return Lista de usuarios del ecommerce
     */
    List<UserEntity> findByEcommerceId(UUID ecommerceId);
}
