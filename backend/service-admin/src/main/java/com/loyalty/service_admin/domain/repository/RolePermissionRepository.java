package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.RolePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * Repositorio para la entidad RolePermission.
 * Queries:
 * - findByRole(): obtener todos los permisos de un rol
 * - findPermissionCodesByRole(): obtener códigos de permisos de un rol (optimizado, sin cargar entidad completa)
 */
@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, Long> {
    List<RolePermissionEntity> findByRole(String role);
    
    @Query("SELECT p.code FROM RolePermissionEntity rp JOIN rp.permission p WHERE rp.role = :role")
    Set<String> findPermissionCodesByRole(String role);
}
