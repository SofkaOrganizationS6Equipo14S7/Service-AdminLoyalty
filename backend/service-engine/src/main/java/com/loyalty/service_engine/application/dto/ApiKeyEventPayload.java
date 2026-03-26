package com.loyalty.service_engine.application.dto;

/**
 * Payload de evento recibido desde Admin Service.
 */
public record ApiKeyEventPayload(
    String eventType,           // API_KEY_CREATED o API_KEY_DELETED
    String keyId,               // UUID de la key
    String keyString,           // La key completa en formato UUID
    String ecommerceId,         // UUID del ecommerce propietario
    String timestamp            // ISO8601 timestamp del evento
) {
}
