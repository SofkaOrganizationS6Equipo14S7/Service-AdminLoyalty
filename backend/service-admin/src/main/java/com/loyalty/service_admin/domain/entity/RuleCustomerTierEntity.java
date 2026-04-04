package com.loyalty.service_admin.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rule_customer_tiers", indexes = {
        @Index(name = "idx_rule_customer_tiers_rule", columnList = "rule_id"),
        @Index(name = "idx_rule_customer_tiers_tier", columnList = "customer_tier_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleCustomerTierEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private RuleEntity rule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_tier_id", nullable = false)
    private CustomerTierEntity customerTier;

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
