package com.loyalty.service_admin.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidad para almacenar la prioridad de aplicación de descuentos.
 * Cada tipo de descuento tiene un nivel de prioridad único (1 = máxima prioridad).
 */
@Entity
@Table(
    name = "discount_priority",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_config_discount_type",
            columnNames = {"discount_config_id", "discount_type"}
        ),
        @UniqueConstraint(
            name = "uk_config_priority_level",
            columnNames = {"discount_config_id", "priority_level"}
        )
    },
    indexes = {
        @Index(name = "idx_config_id", columnList = "discount_config_id"),
        @Index(name = "idx_discount_type", columnList = "discount_type")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscountPriorityEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "discount_config_id", nullable = false)
    private UUID discountConfigId;
    
    @Column(name = "discount_type", nullable = false, length = 50)
    private String discountType;
    
    @Column(name = "priority_level", nullable = false)
    private Integer priorityLevel;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
