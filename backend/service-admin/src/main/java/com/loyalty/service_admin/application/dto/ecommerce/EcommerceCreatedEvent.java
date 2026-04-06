package com.loyalty.service_admin.application.dto.ecommerce;

import java.time.Instant;

/**
 * Payload de evento para creación de ecommerce vía RabbitMQ.
 * 
 * SPEC-015: Ecommerce Onboarding con Arquitectura Hexagonal
 * HU-15a: Completar publicación de eventos en createEcommerce
 * 
 * Evento publicado cuando un nuevo ecommerce es registrado exitosamente.
 * Consumidores: service-engine para sincronizar réplicas en caché.
 * 
 * Formato JSON para RabbitMQ:
 * {
 *   "eventType": "ECOMMERCE_CREATED",
 *   "ecommerceId": "550e8400-e29b-41d4-a716-446655440000",
 *   "name": "Nike Store",
 *   "slug": "nike-store",
 *   "status": "ACTIVE",
 *   "timestamp": "2026-04-05T10:00:00Z"
 * }
 */
public record EcommerceCreatedEvent(
    String eventType,           // "ECOMMERCE_CREATED"
    String ecommerceId,         // UUID del ecommerce
    String name,                // Nombre del ecommerce
    String slug,                // Slug único del ecommerce
    String status,              // Status inicial (siempre "ACTIVE")
    Instant timestamp           // Momento del evento (UTC)
) {
}
