package com.loyalty.service_engine.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Read-only replica of customer tiers from service-admin.
 * Used for Cold Start autonomy and caché population.
 * Synchronized via RabbitMQ events.
 */
@Entity
@Table(name = "customer_tiers_replica", indexes = {
    @Index(name = "idx_tiers_replica_active_level", columnList = "is_active, level")
})
public class CustomerTierReplicaEntity {

    @Id
    private UUID uid;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private Integer level;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "last_synced", nullable = false)
    private Instant lastSynced;

    // Constructors
    public CustomerTierReplicaEntity() {
    }

    public CustomerTierReplicaEntity(UUID uid, String name, Integer level, Boolean isActive, Instant lastSynced) {
        this.uid = uid;
        this.name = name;
        this.level = level;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
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
