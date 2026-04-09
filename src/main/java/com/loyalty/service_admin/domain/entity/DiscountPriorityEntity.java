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
    name = "discount_priorities",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_discount_setting_type",
            columnNames = {"discount_setting_id", "discount_type_id"}
        ),
        @UniqueConstraint(
            name = "uk_discount_setting_priority",
            columnNames = {"discount_setting_id", "priority_level"}
        )
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountPriorityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "discount_setting_id", nullable = false)
    private UUID discountSettingId;

    @Column(name = "discount_type_id", nullable = false)
    private UUID discountTypeId;

    @Column(name = "priority_level", nullable = false)
    private Integer priorityLevel;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

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
