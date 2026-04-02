package com.loyalty.service_admin.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "classification_rule", indexes = {
    @Index(name = "idx_classification_rule_tier", columnList = "customer_tier_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassificationRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_tier_id", nullable = false)
    private UUID customerTierId;

    @Column(name = "metric_type", nullable = false, length = 50)
    private String metricType;

    @Column(name = "min_value", nullable = false)
    private BigDecimal minValue;

    @Column(name = "max_value")
    private BigDecimal maxValue;

    @Column(nullable = false)
    private Integer priority = 1;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}