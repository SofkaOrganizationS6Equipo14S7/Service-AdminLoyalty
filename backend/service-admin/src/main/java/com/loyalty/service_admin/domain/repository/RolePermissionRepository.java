package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.RolePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Repositorio para la entidad RolePermission.
 * Queries:
 * - findByRoleName(): obtener todos los permisos de un rol
 * - findPermissionCodesByRoleName(): obtener códigos de permisos de un rol (optimizado, sin cargar entidad completa)
 */
@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, UUID> {
    
    @Query("SELECT rp FROM RolePermissionEntity rp WHERE rp.role.name = :roleName")
    List<RolePermissionEntity> findByRoleName(String roleName);
    
    @Query("SELECT p.code FROM PermissionEntity p JOIN RolePermissionEntity rp ON p.id = rp.permission.id WHERE rp.role.name = :roleName")
    Set<String> findPermissionCodesByRoleName(String roleName);
}
