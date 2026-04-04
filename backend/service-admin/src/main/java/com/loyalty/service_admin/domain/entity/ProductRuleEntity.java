package com.loyalty.service_admin.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// DEPRECATED: Migrated to generic Rule + RuleAttribute architecture
// @Entity
// @Table(name = "product_rules", indexes = {
//     @Index(name = "idx_product_rules_ecommerce", columnList = "ecommerce_id"),
//     @Index(name = "idx_product_rules_type", columnList = "product_type"),
//     @Index(name = "idx_product_rules_active", columnList = "is_active")
// }, uniqueConstraints = {
//     @UniqueConstraint(name = "uk_product_rules_ecommerce_type_active", columnNames = {"ecommerce_id", "product_type", "is_active"})
// })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ecommerce_id", nullable = false)
    private UUID ecommerceId;

    @Column(name = "discount_type_id", nullable = false)
    private UUID discountTypeId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "product_type", nullable = false, length = 100)
    private String productType;

    @Column(name = "discount_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercentage;

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