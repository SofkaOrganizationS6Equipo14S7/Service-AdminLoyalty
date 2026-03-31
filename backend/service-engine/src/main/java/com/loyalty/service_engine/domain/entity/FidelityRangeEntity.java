package com.loyalty.service_engine.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity representing fidelity ranges in Engine Service.
 * This is a replica synchronized from Admin Service via RabbitMQ.
 * Used only for Cold Start initialization; production queries use Caffeine cache.
 */
@Entity
@Table(name = "fidelity_ranges", indexes = {
    @Index(name = "idx_fidelity_ranges_ecommerce_id", columnList = "ecommerce_id"),
    @Index(name = "idx_fidelity_ranges_active", columnList = "is_active"),
    @Index(name = "idx_fidelity_ranges_ecommerce_active", columnList = "ecommerce_id,is_active")
})
@Getter
@Setter
@NoArgsConstructor
public class FidelityRangeEntity {
    @Id
    private UUID uid;

    @Column(name = "ecommerce_id", nullable = false)
    private UUID ecommerceId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "min_points", nullable = false)
    private Integer minPoints;

    @Column(name = "max_points", nullable = false)
    private Integer maxPoints;

    @Column(name = "discount_percentage", nullable = false)
    private BigDecimal discountPercentage;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
