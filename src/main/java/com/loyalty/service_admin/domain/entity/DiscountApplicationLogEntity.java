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
@Table(name = "discount_application_log", indexes = {
    @Index(name = "idx_discount_log_ecommerce", columnList = "ecommerce_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscountApplicationLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ecommerce_id", nullable = false)
    private UUID ecommerceId;

    @Column(name = "external_order_id", length = 255)
    private String externalOrderId;

    @Column(name = "original_amount", nullable = false, precision = 12, scale = 4)
    private BigDecimal originalAmount;

    @Column(name = "discount_applied", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountApplied;

    @Column(name = "final_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "applied_rules_details", columnDefinition = "jsonb")
    private String appliedRulesDetails;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}