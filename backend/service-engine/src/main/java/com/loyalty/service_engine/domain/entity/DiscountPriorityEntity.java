package com.loyalty.service_engine.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad réplica para almacenar la prioridad de aplicación de descuentos (HU-09).
 * 
 * IMPORTANTE: Esta es una COPIA IDÉNTICA de la tabla en Service-Admin (master).
 * Se actualiza vía RabbitMQ cuando cambia en Admin.
 * Ver loyalty_engine BD (replica) y loyalty_admin BD (master).
 * 
 * Reglas:
 * - Los niveles de prioridad deben ser secuenciales (1, 2, 3, ..., N)
 * - Cada tipo de descuento debe tener una prioridad única por configuración
 * - Cada configuración puede tener múltiples prioridades
 */
@Entity
@Table(name = "discount_limit_priority", indexes = {
        @Index(name = "idx_discount_limit_priority_config_id", columnList = "discount_config_id"),
        @Index(name = "idx_discount_limit_priority_config_type", columnList = "discount_config_id, discount_type", unique = true),
        @Index(name = "idx_discount_limit_priority_config_level", columnList = "discount_config_id, priority_level", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscountPriorityEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uid")
    private UUID uid;
    
    @Column(name = "discount_config_id", nullable = false)
    private UUID discountConfigId;
    
    @Column(name = "discount_type", nullable = false, length = 50)
    private String discountType;
    
    @Column(name = "priority_level", nullable = false)
    private Integer priorityLevel;
    
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
