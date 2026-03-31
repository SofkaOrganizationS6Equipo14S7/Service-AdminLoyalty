package com.loyalty.service_admin.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fidelity_ranges", indexes = {
    @Index(name = "idx_fidelity_ranges_ecommerce_id", columnList = "ecommerce_id"),
    @Index(name = "idx_fidelity_ranges_active", columnList = "is_active"),
    @Index(name = "idx_fidelity_ranges_ecommerce_active", columnList = "ecommerce_id,is_active"),
    @Index(name = "idx_fidelity_ranges_min_points", columnList = "min_points")
})
public class FidelityRangeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
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
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public FidelityRangeEntity() {
    }

    public FidelityRangeEntity(UUID ecommerceId, String name, Integer minPoints, Integer maxPoints,
                             BigDecimal discountPercentage) {
        this.ecommerceId = ecommerceId;
        this.name = name;
        this.minPoints = minPoints;
        this.maxPoints = maxPoints;
        this.discountPercentage = discountPercentage;
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

    public UUID getEcommerceId() {
        return ecommerceId;
    }

    public void setEcommerceId(UUID ecommerceId) {
        this.ecommerceId = ecommerceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMinPoints() {
        return minPoints;
    }

    public void setMinPoints(Integer minPoints) {
        this.minPoints = minPoints;
    }

    public Integer getMaxPoints() {
        return maxPoints;
    }

    public void setMaxPoints(Integer maxPoints) {
        this.maxPoints = maxPoints;
    }

    public BigDecimal getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(BigDecimal discountPercentage) {
        this.discountPercentage = discountPercentage;
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

    @Override
    public String toString() {
        return "FidelityRangeEntity{" +
                "uid=" + uid +
                ", ecommerceId=" + ecommerceId +
                ", name='" + name + '\'' +
                ", minPoints=" + minPoints +
                ", maxPoints=" + maxPoints +
                ", discountPercentage=" + discountPercentage +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
