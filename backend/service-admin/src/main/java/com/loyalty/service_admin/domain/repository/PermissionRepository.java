package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para la entidad Permission.
 * Query methods:
 * - findByCode(): obtener permiso por código único
 */
@Repository
public interface PermissionRepository extends JpaRepository<PermissionEntity, Long> {
    Optional<PermissionEntity> findByCode(String code);
}
