package com.loyalty.service_admin.application.dto;

import java.time.Instant;

/**
 * Payload de evento para cambio de status de ecommerce vía RabbitMQ.
 * 
 * SPEC-001: Registro y Gestión de Ecommerces
 * HU-13.3: Actualizar estado de un ecommerce
 * 
 * Cascada de desactivación:
 * Cuando un ecommerce cambia a INACTIVE, este evento se publica a un Fanout Exchange
 * para que múltiples consumidores reaccionen:
 * - service-admin: invalida JWT de usuarios y sesiones activas
 * - service-engine: invalida API Keys en caché Caffeine
 * 
 * Formato JSON para RabbitMQ:
 * {
 *   "eventType": "ECOMMERCE_STATUS_CHANGED",
 *   "ecommerceId": "550e8400-e29b-41d4-a716-446655440000",
 *   "newStatus": "INACTIVE",
 *   "oldStatus": "ACTIVE",
 *   "timestamp": "2026-03-29T11:00:00Z"
 * }
 */
public record EcommerceStatusChangedEvent(
    String eventType,           // "ECOMMERCE_STATUS_CHANGED"
    String ecommerceId,         // UUID del ecommerce
    String newStatus,           // ACTIVE o INACTIVE
    String oldStatus,           // ACTIVE o INACTIVE (status anterior)
    Instant timestamp           // Momento del evento (UTC)
) {
}
