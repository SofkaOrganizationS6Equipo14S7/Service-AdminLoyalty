package com.loyalty.service_engine.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Read-only replica of classification rules from service-admin.
 * Used for Cold Start autonomy and caché population.
 * Synchronized via RabbitMQ events.
 */
@Entity
@Table(name = "classification_rules_replica", indexes = {
    @Index(name = "idx_rules_replica_active_metric", columnList = "is_active, metric_type"),
    @Index(name = "idx_rules_replica_tier", columnList = "tier_uid")
})
public class ClassificationRuleReplicaEntity {

    @Id
    private UUID uid;

    @Column(name = "tier_uid", nullable = false)
    private UUID tierUid;

    @Column(name = "metric_type", nullable = false, length = 50)
    private String metricType; // 'loyalty_points', 'total_spent', 'order_count', 'custom'

    @Column(name = "min_value", nullable = false)
    private BigDecimal minValue;

    @Column(name = "max_value")
    private BigDecimal maxValue; // NULL = no upper limit

    @Column(nullable = false)
    private Integer priority;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "last_synced", nullable = false)
    private Instant lastSynced;

    // Constructors
    public ClassificationRuleReplicaEntity() {
    }

    public ClassificationRuleReplicaEntity(UUID uid, UUID tierUid, String metricType, BigDecimal minValue,
                                          BigDecimal maxValue, Integer priority, Boolean isActive, Instant lastSynced) {
        this.uid = uid;
        this.tierUid = tierUid;
        this.metricType = metricType;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.priority = priority;
        this.isActive = isActive;
        this.lastSynced = lastSynced;
    }

    // Getters and Setters
    public UUID getUid() {
        return uid;
    }

    public void setUid(UUID uid) {
        this.uid = uid;
    }

    public UUID getTierUid() {
        return tierUid;
    }

    public void setTierUid(UUID tierUid) {
        this.tierUid = tierUid;
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

    public Instant getLastSynced() {
        return lastSynced;
    }

    public void setLastSynced(Instant lastSynced) {
        this.lastSynced = lastSynced;
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastSynced = Instant.now();
    }
}
