package com.loyalty.service_admin.application.dto;

import java.time.Instant;

/**
 * Payload de evento para sincronización con Engine vía RabbitMQ.
 */
public record ApiKeyEventPayload(
    String eventType,           // API_KEY_CREATED o API_KEY_DELETED
    String keyId,               // UUID de la key
    String keyString,           // La key completa en formato UUID
    String ecommerceId,         // UUID del ecommerce propietario
    Instant timestamp           // Momento del evento (UTC)
) {
}
