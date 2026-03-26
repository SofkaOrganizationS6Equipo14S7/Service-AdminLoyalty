package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    
    /**
     * Busca un usuario por su username.
     * Utilizado en el login para validar credenciales.
     *
     * @param username nombre del usuario
     * @return Optional con el usuario si existe
     */
    Optional<UserEntity> findByUsername(String username);
}
