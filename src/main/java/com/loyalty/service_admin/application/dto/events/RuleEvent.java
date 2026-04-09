package com.loyalty.service_admin.application.dto.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Generic unified event for all rule types (FIDELITY, SEASONAL, PRODUCT, CLASSIFICATION).
 * Published by Admin Service RuleService on CREATE, UPDATE, DELETE operations.
 * Consumed by Engine Service RuleEventConsumer to sync engine_rules table.
 *
 * SPEC-010: Unifies CLASSIFICATION, PRODUCT, SEASONAL, FIDELITY rule events.
 *
 * Example payload:
 * {
 *   "eventType": "RULE_CREATED",
 *   "ruleId": "550e8400-e29b-41d4-a716-446655440000",
 *   "ecommerceId": "550e8400-e29b-41d4-a716-446655440001",
 *   "name": "Summer Discount FIDELITY",
 *   "description": "20% discount for Gold tier",
 *   "discountTypeCode": "FIDELITY",
 *   "discountValue": 20.00,
 *   "priorityLevel": 1,
 *   "logicConditions": {
 *     "tierCode": "GOLD",
 *     "minSpend": 1000.00,
 *     "applicableTo": "all_categories"
 *   },
 *   "isActive": true,
 *   "appliedWith": "INDIVIDUAL",
 *   "timestamp": "2026-04-08T15:30:45Z"
 * }
 */
public record RuleEvent(
    String eventType,                      // "RULE_CREATED" | "RULE_UPDATED" | "RULE_DELETED"
    UUID ruleId,                           // Rule ID (uuid from Admin)
    UUID ecommerceId,                      // Tenant ID (multi-tenant isolation)
    String name,                           // Rule display name
    String description,                    // Rule description (nullable)
    String discountTypeCode,               // "FIDELITY" | "SEASONAL" | "PRODUCT" | "CLASSIFICATION"
    BigDecimal discountValue,              // Discount amount or percentage
    Integer priorityLevel,                 // Evaluation order (1, 2, 3, ...)
    Map<String, Object> logicConditions,   // Flexible JSONB: {start_date, end_date, product_type, min_value, ...}
    Boolean isActive,                      // true = active, false = inactive
    String appliedWith,                    // "INDIVIDUAL" | "STACKED" (optional, defaults to "INDIVIDUAL")
    Instant timestamp                      // UTC timestamp of event
) {
}
