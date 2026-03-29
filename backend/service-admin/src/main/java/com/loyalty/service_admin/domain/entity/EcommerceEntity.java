package com.loyalty.service_admin.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidad de Ecommerce para LOYALTY system — Multi-tenant support.
 * 
 * SPEC-001: Registro y Gestión de Ecommerces (Onboarding)
 * 
 * Cada ecommerce es un tenant independiente con:
 * - UUID único (uid) generado automáticamente
 * - slug único (identificador amigable para URLs)
 * - estado (ACTIVE/INACTIVE para soft delete)
 * - timestamps de auditoría (created_at, updated_at)
 * 
 * Relaciones:
 * - users.ecommerce_id → ecommerce.uid (FK NO DELETE)
 * - api_keys.ecommerce_id → ecommerce.uid (FK NO DELETE)
 * 
 * UUID Mapping Crítico:
 * El campo ecommerce_id en tablas relacionadas apunta al UUID (uid),
 * NO a un ID secuencial, para consistencia arquitectónica.
 */
@Entity
@Table(name = "ecommerces", indexes = {
    @Index(name = "idx_ecommerces_slug_unique", columnList = "slug", unique = true),
    @Index(name = "idx_ecommerces_status", columnList = "status"),
    @Index(name = "idx_ecommerces_created_at", columnList = "created_at"),
    @Index(name = "idx_ecommerces_name", columnList = "name")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcommerceEntity {
    
    @Id
    @Column(name = "uid", columnDefinition = "UUID", nullable = false)
    private UUID uid;
    
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    
    @Column(name = "slug", nullable = false, length = 255, unique = true)
    private String slug;
    
    @Column(name = "status", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private com.loyalty.service_admin.domain.model.ecommerce.EcommerceStatus status;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    /**
     * Hook JPA: ejecutado antes de INSERT.
     * - Genera UUID automáticamente si no está seteado
     * - Establece timestamps
     * - Seteea status por defecto a ACTIVE
     */
    @PrePersist
    protected void onCreate() {
        if (this.uid == null) {
            this.uid = UUID.randomUUID();
        }
        if (this.status == null) {
            this.status = com.loyalty.service_admin.domain.model.ecommerce.EcommerceStatus.ACTIVE;
        }
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }
    
    /**
     * Hook JPA: ejecutado antes de UPDATE.
     * - Actualiza timestamp de modificación
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
