package com.loyalty.service_admin.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a product rule is created, updated or deleted.
 * Consumed by Service-Engine to update its cache.
 */
public record ProductRuleEvent(
    String eventType,               // "PRODUCT_RULE_CREATED", "PRODUCT_RULE_UPDATED", "PRODUCT_RULE_DELETED"
    UUID uid,                       // Rule identifier
    UUID ecommerceId,               // Ecommerce identifier
    String productType,
    BigDecimal discountPercentage,
    String benefit,
    Boolean isActive,
    Instant timestamp
) {}
