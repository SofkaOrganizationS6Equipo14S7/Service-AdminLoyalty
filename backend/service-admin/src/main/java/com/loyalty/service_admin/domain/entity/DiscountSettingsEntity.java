package com.loyalty.service_admin.domain.entity;

import com.loyalty.service_admin.domain.model.CapAppliesTo;
import com.loyalty.service_admin.domain.model.CapType;
import com.loyalty.service_admin.domain.model.RoundingRule;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "discount_settings", indexes = {
    @Index(name = "idx_discount_settings_ecommerce", columnList = "ecommerce_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountSettingsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ecommerce_id", nullable = false)
    private UUID ecommerceId;

    @Column(name = "max_discount_cap", nullable = false, precision = 12, scale = 4)
    private BigDecimal maxDiscountCap;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "USD";

    @Column(name = "allow_stacking")
    private Boolean allowStacking = true;

    @Column(name = "rounding_rule", nullable = false, length = 20)
    private String roundingRule = "ROUND_HALF_UP";

    @Column(name = "cap_type", length = 20)
    @Enumerated(EnumType.STRING)
    private CapType capType;

    @Column(name = "cap_value", precision = 12, scale = 4)
    private BigDecimal capValue;

    @Column(name = "cap_applies_to", length = 20)
    @Enumerated(EnumType.STRING)
    private CapAppliesTo capAppliesTo;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "version", nullable = false)
    private Long version = 1L;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "discount_setting_id")
    private List<DiscountPriorityEntity> priorities = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void replacePriorities(List<DiscountPriorityEntity> newPriorities) {
        this.priorities.clear();
        if (newPriorities != null) {
            this.priorities.addAll(newPriorities);
        }
    }

    public List<DiscountPriorityEntity> getPriorities() {
        return this.priorities;
    }

    public Long getVersion() {
        return this.version;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        version = (version != null ? version : 1L) + 1;
    }
}
