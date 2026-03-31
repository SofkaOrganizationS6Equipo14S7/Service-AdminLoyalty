package com.loyalty.service_engine.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * SeasonalRuleEntity for Service-Engine
 * 
 * This is a replica of the SeasonalRule from Service-Admin
 * Synchronized via RabbitMQ events (created, updated, deleted)
 * Used primarily for caching and high-speed evaluation
 */
@Entity
@Table(name = "seasonal_rules", indexes = {
    @Index(name = "idx_seasonal_rules_ecommerce_id", columnList = "ecommerce_id"),
    @Index(name = "idx_seasonal_rules_date_range", columnList = "start_date, end_date"),
    @Index(name = "idx_seasonal_rules_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeasonalRuleEntity {

    @Id
    private UUID uid;

    @Column(nullable = false)
    private UUID ecommerceId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private BigDecimal discountPercentage;

    @Column(nullable = false, length = 50)
    private String discountType;

    @Column(nullable = false)
    private Instant startDate;

    @Column(nullable = false)
    private Instant endDate;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;
}
