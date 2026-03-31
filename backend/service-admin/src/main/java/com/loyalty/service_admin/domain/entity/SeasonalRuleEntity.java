package com.loyalty.service_admin.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "seasonal_rules", indexes = {
    @Index(name = "idx_seasonal_rules_ecommerce_id", columnList = "ecommerce_id"),
    @Index(name = "idx_seasonal_rules_date_range", columnList = "start_date, end_date"),
    @Index(name = "idx_seasonal_rules_active", columnList = "is_active")
})
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

    // Constructors
    public SeasonalRuleEntity() {
    }

    public SeasonalRuleEntity(UUID uid, UUID ecommerceId, String name, BigDecimal discountPercentage,
                            String discountType, Instant startDate, Instant endDate) {
        this.uid = uid;
        this.ecommerceId = ecommerceId;
        this.name = name;
        this.discountPercentage = discountPercentage;
        this.discountType = discountType;
        this.startDate = startDate;
        this.endDate = endDate;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(BigDecimal discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
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
}
