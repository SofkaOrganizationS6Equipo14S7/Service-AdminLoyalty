package com.loyalty.service_admin.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "discount_priorities", indexes = {
        @Index(name = "idx_discount_priority_cfg_id", columnList = "configuration_id"),
        @Index(name = "idx_discount_priority_cfg_order", columnList = "configuration_id, priority_order", unique = true),
        @Index(name = "idx_discount_priority_cfg_type", columnList = "configuration_id, discount_type", unique = true)
})
@Getter
@Setter
public class DiscountPriorityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "configuration_id", nullable = false)
    private DiscountConfigurationEntity configuration;

    @Column(name = "discount_type", nullable = false, length = 50)
    private String discountType;

    @Column(name = "priority_order", nullable = false)
    private Integer order;
}
