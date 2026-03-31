package com.loyalty.service_admin.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Represent a classification rule that associates metric thresholds to a customer tier.
 * Multiple rules can apply to the same tier.
 * Metric types: 'loyalty_points', 'total_spent', 'order_count', 'custom'
 */
@Entity
@Table(name = "classification_rules", indexes = {
    @Index(name = "idx_classification_rules_tier", columnList = "customer_tier_uid"),
    @Index(name = "idx_classification_rules_active", columnList = "is_active"),
    @Index(name = "idx_classification_rules_metric", columnList = "metric_type, is_active")
})
public class ClassificationRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uid;

    @Column(name = "customer_tier_uid", nullable = false)
    private UUID customerTierUid;

    @Column(name = "metric_type", nullable = false, length = 50)
    private String metricType; // 'loyalty_points', 'total_spent', 'order_count', 'custom'

    @Column(name = "min_value", nullable = false)
    private BigDecimal minValue;

    @Column(name = "max_value")
    private BigDecimal maxValue; // NULL = no upper limit

    @Column(nullable = false)
    private Integer priority; // Lower number = higher priority

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public ClassificationRuleEntity() {
    }

    public ClassificationRuleEntity(UUID customerTierUid, String metricType, BigDecimal minValue,
                                   BigDecimal maxValue, Integer priority) {
        this.customerTierUid = customerTierUid;
        this.metricType = metricType;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.priority = priority;
        this.isActive = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getUid() {
        return uid;
    }

    public void setUid(UUID uid) {
        this.uid = uid;
    }

    public UUID getCustomerTierUid() {
        return customerTierUid;
    }

    public void setCustomerTierUid(UUID customerTierUid) {
        this.customerTierUid = customerTierUid;
    }

    public String getMetricType() {
        return metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public BigDecimal getMinValue() {
        return minValue;
    }

    public void setMinValue(BigDecimal minValue) {
        this.minValue = minValue;
    }

    public BigDecimal getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(BigDecimal maxValue) {
        this.maxValue = maxValue;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
