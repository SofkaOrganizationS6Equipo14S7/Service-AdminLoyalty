package com.loyalty.service_engine.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad réplica para almacenar la configuración de tope máximo de descuentos (HU-09).
 * 
 * IMPORTANTE: Esta es una COPIA IDÉNTICA de la tabla en Service-Admin (master).
 * Se actualiza vía RabbitMQ cuando cambia en Admin.
 * Ver loyalty_engine BD (replica) y loyalty_admin BD (master).
 * 
 * Reglas:
 * - Solo una configuración activa por ecommerce
 * - maxDiscountLimit debe ser > 0
 * - currencyCode debe ser válido (ISO 4217)
 */
@Entity
@Table(name = "discount_config", indexes = {
        @Index(name = "idx_discount_config_ecommerce_id", columnList = "ecommerce_id"),
        @Index(name = "idx_discount_config_ecommerce_active", columnList = "ecommerce_id, is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscountConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uid")
    private UUID uid;

    @Column(name = "ecommerce_id", nullable = false)
    private UUID ecommerceId;

    @Column(name = "max_discount_limit", nullable = false, precision = 10, scale = 2)
    private BigDecimal maxDiscountLimit;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
