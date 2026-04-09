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
    name = "rule_attributes",
    indexes = {
        @Index(name = "idx_rule_attributes_discount_type_id", columnList = "discount_type_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_rule_attributes_type_name", columnNames = {"discount_type_id", "attribute_name"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleAttributeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "discount_type_id", nullable = false)
    private UUID discountTypeId;

    @Column(name = "attribute_name", nullable = false, length = 100)
    private String attributeName;

    @Column(name = "attribute_type", nullable = false, length = 20)
    private String attributeType;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    @Column(name = "default_value", length = 500)
    private String defaultValue;

    @Column(length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
