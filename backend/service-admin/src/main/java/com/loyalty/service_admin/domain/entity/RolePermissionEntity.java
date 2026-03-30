package com.loyalty.service_admin.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Tabla de relación: Rol <- Permiso (muchos a muchos).
 * SPEC-004 RN-04: Permisos granulares configurables por STORE_ADMIN.
 * 
 * Asigna un conjunto de permisos a cada rol:
 * - SUPER_ADMIN: todos los permisos
 * - STORE_ADMIN: permisos de gestión de usuarios y promociones de su ecommerce
 * - STORE_USER: permisos de lectura/escritura de promociones de su ecommerce
 */
@Entity
@Table(name = "role_permissions", 
    indexes = {
        @Index(name = "idx_role_permissions_role", columnList = "role"),
        @Index(name = "idx_role_permissions_permission", columnList = "permission_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"role", "permission_id"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermissionEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "role", nullable = false, length = 50)
    private String role;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    private PermissionEntity permission;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
