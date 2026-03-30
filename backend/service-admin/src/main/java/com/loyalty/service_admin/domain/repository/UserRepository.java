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
     * Busca un usuario por su UID (identificador único generado por el sistema).
     * Utilizado en endpoints GET/PUT/DELETE /api/v1/users/{uid}.
     * UID es único en toda la plataforma (SPEC-002 CRITERIO-3.1).
     *
     * @param uid UUID único del usuario
     * @return Optional con el usuario si existe
     */
    Optional<UserEntity> findByUid(UUID uid);
    
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
    
    /**
     * Busca usuarios por rol y ecommerce específicos.
     * Utilizado para análisis de usuarios de un rol dentro de un ecommerce.
     * 
     * EXAMPLE: findByRoleAndEcommerceId("USER", uuid) → List<UserEntity>
     *
     * @param role nombre del rol (e.g., "STORE_USER", "SUPER_ADMIN")
     * @param ecommerceId UUID del ecommerce (null para SUPER_ADMIN)
     * @return Lista de usuarios con ese rol en ese ecommerce
     */
    List<UserEntity> findByRoleAndEcommerceId(String role, UUID ecommerceId);
    
    /**
     * Busca un usuario por su email (búsqueda global, no por ecommerce).
     * Utilizado en endpoints para validar unicidad global de email.
     * Email es único en toda la plataforma (SPEC-003 RN-04).
     *
     * @param email dirección de correo del usuario
     * @return Optional con el usuario si existe
     */
    Optional<UserEntity> findByEmail(String email);
    
    /**
     * Busca usuarios de un ecommerce cuyo username contiene el patrón (búsqueda case-insensitive).
     * Utilizado en GET /api/v1/users?search=pattern para búsqueda dentro del ecommerce.
     * 
     * @param ecommerceId UUID del ecommerce
     * @param usernameLike patrón de búsqueda en username (ej. "admin")
     * @return Lista de usuarios del ecommerce que coinciden
     */
    List<UserEntity> findByEcommerceIdAndUsernameIgnoreCaseContaining(UUID ecommerceId, String usernameLike);
    
    /**
     * Busca usuarios de un ecommerce cuyo email contiene el patrón (búsqueda case-insensitive).
     * Utilizado en GET /api/v1/users?search=pattern para búsqueda dentro del ecommerce.
     * 
     * @param ecommerceId UUID del ecommerce
     * @param emailLike patrón de búsqueda en email (ej. "admin@")
     * @return Lista de usuarios del ecommerce que coinciden
     */
    List<UserEntity> findByEcommerceIdAndEmailIgnoreCaseContaining(UUID ecommerceId, String emailLike);
}
