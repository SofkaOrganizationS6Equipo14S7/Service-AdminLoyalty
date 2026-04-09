package com.loyalty.service_admin.application.dto.rules;

import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when a rule's status changes (active/inactive)
 * 
 * SPEC-008: Endpoint Dedicado para Cambiar Status de Reglas
 * HU-14: Cambiar Status de Regla mediante Endpoint Dedicado
 * 
 * Emitted to RabbitMQ exchange "loyalty.events" with routing key "rule.status.changed"
 * Engine Service listens and updates its Caffeine cache accordingly
 */
public record RuleStatusChangedEvent(
    UUID ruleId,          // Unique identifier of the rule
    UUID ecommerceId,     // Tenant ID
    Boolean newStatus,    // true = active, false = inactive
    Boolean previousStatus,  // Previous status for audit trail
    Instant changedAt     // Timestamp (UTC) when the change occurred
) {}
