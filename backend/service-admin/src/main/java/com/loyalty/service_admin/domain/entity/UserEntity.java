package com.loyalty.service_admin.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad de Usuario para LOYALTY system.
 * 
 * SPEC-002: Gestión de Usuarios por Ecommerce (v2.0)
 * 
 * Dos roles únicos:
 * - SUPER_ADMIN: ecommerce_id = NULL (sin restricción)
 * - USER: ecommerce_id NOT NULL (obligatoriamente vinculado a ecommerce)
 * 
 * CHECK constraint: (role='SUPER_ADMIN' AND ecommerce_id IS NULL) OR (role='USER' AND ecommerce_id IS NOT NULL)
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_username", columnList = "username", unique = true),
    @Index(name = "idx_ecommerce_id", columnList = "ecommerce_id"),
    @Index(name = "idx_active", columnList = "active"),
    @Index(name = "idx_role", columnList = "role"),
    @Index(name = "idx_uid", columnList = "uid", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "uid", nullable = false, unique = true)
    private UUID uid;
    
    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;
    
    @Column(name = "password", nullable = false, length = 255)
    private String password;
    
    @Column(name = "role", nullable = false, length = 50)
    private String role;
    
    @Column(name = "ecommerce_id", nullable = true)
    private UUID ecommerceId;
    
    @Column(name = "active", nullable = false)
    private Boolean active;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (uid == null) {
            uid = UUID.randomUUID();
        }
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (active == null) {
            active = true;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
