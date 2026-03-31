package com.loyalty.service_admin.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Represent a customer loyalty tier (Bronze, Silver, Gold, Platinum).
 * This is the source of truth for tier definitions.
 */
@Entity
@Table(name = "customer_tiers", indexes = {
    @Index(name = "idx_customer_tiers_active_level", columnList = "is_active, level"),
    @Index(name = "idx_customer_tiers_name", columnList = "name")
})
public class CustomerTierEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uid;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(nullable = false)
    private Integer level;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public CustomerTierEntity() {
    }

    public CustomerTierEntity(String name, Integer level) {
        this.name = name;
        this.level = level;
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
