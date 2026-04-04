package com.loyalty.service_admin.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "rule_attribute_values",
    indexes = {
        @Index(name = "idx_rule_attribute_values_rule", columnList = "rule_id"),
        @Index(name = "idx_rule_attribute_values_attribute", columnList = "attribute_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_rule_attribute_values_rule_attr", columnNames = {"rule_id", "attribute_id"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleAttributeValueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "rule_id", nullable = false)
    private UUID ruleId;

    @Column(name = "attribute_id", nullable = false)
    private UUID attributeId;

    @Column(nullable = false, length = 1000)
    private String value;

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
