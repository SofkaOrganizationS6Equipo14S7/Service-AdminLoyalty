package com.loyalty.service_admin.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entidad de Permiso para el sistema de control de acceso granular.
 * SPEC-004 RN-04: Permisos granulares configurables por STORE_ADMIN.
 * 
 * Estructura:
 * - code: identificador único (ej "promotion:read", "user:write")
 * - description: descripción legible
 * - module: módulo al que pertenece (promotion, user, ecommerce, etc)
 * - action: acción permitida (read, write, delete)
 * 
 * Relaciones:
 * - role_permissions: muchos roles pueden tener este permiso
 * - user_permissions (futuro): sobrescrituras específicas del usuario
 */
@Entity
@Table(name = "permissions", indexes = {
    @Index(name = "idx_permission_code", columnList = "code", unique = true),
    @Index(name = "idx_permission_module", columnList = "module")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;
    
    @Column(name = "description", nullable = false, length = 255)
    private String description;
    
    @Column(name = "module", nullable = false, length = 50)
    private String module;
    
    @Column(name = "action", nullable = false, length = 50)
    private String action;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
