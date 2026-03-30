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
 * SPEC-003: Administración de Ecommerce por STORE_ADMIN
 * 
 * Tres roles únicos:
 * - SUPER_ADMIN: ecommerce_id = NULL (sin restricción)
 * - STORE_ADMIN: ecommerce_id NOT NULL (vinculado a exactamente un ecommerce, puede gestionar usuarios)
 * - STORE_USER: ecommerce_id NOT NULL (vinculado a exactamente un ecommerce, usuario estándar)
 * 
 * Username y email son globalmente únicos para simplificar Login sin requerir slug de tienda.
 * 
 * CHECK constraint: (role='SUPER_ADMIN' AND ecommerce_id IS NULL) OR (role IN ('STORE_ADMIN', 'STORE_USER') AND ecommerce_id IS NOT NULL)
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_username", columnList = "username", unique = true),
    @Index(name = "idx_email", columnList = "email", unique = true),
    @Index(name = "idx_ecommerce_id", columnList = "ecommerce_id"),
    @Index(name = "idx_ecommerce_id_active", columnList = "ecommerce_id, active"),
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
    
    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;
    
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
