package com.loyalty.service_admin.domain.entity;

import com.loyalty.service_admin.domain.model.CapAppliesTo;
import com.loyalty.service_admin.domain.model.CapType;
import com.loyalty.service_admin.domain.model.RoundingRule;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "discount_configurations", indexes = {
        @Index(name = "idx_discount_cfg_ecommerce_id", columnList = "ecommerce_id", unique = true)
})
@Getter
@Setter
public class DiscountConfigurationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ecommerce_id", nullable = false, unique = true)
    private UUID ecommerceId;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "rounding_rule", nullable = false, length = 20)
    private RoundingRule roundingRule;

    @Enumerated(EnumType.STRING)
    @Column(name = "cap_type", nullable = false, length = 20)
    private CapType capType;

    @Column(name = "cap_value", nullable = false, precision = 12, scale = 4)
    private BigDecimal capValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "cap_applies_to", nullable = false, length = 20)
    private CapAppliesTo capAppliesTo;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "configuration", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<DiscountPriorityEntity> priorities = new ArrayList<>();

    @jakarta.persistence.PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (version == null) {
            version = 0L;
        }
    }

    @jakarta.persistence.PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }

    public void replacePriorities(List<DiscountPriorityEntity> newPriorities) {
        priorities.clear();
        for (DiscountPriorityEntity priority : newPriorities) {
            priority.setConfiguration(this);
            priorities.add(priority);
        }
    }
}
