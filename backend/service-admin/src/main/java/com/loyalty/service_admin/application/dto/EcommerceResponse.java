package com.loyalty.service_admin.application.dto;

import java.time.Instant;

/**
 * DTO para respuesta de un ecommerce.
 * 
 * SPEC-001: Registro y Gestión de Ecommerces
 * 
 * Contiene toda la información pública de un ecommerce:
 * - uid: identificador único (UUID)
 * - name: nombre del ecommerce
 * - slug: identificador amigable
 * - status: ACTIVE o INACTIVE
 * - createdAt: timestamp de creación
 * - updatedAt: timestamp de última actualización
 * 
 * Se usa en:
 * - POST /api/v1/ecommerces → 201 Created
 * - GET /api/v1/ecommerces → 200 OK (lista)
 * - GET /api/v1/ecommerces/{uid} → 200 OK (detalle)
 * - PUT /api/v1/ecommerces/{uid} → 200 OK (actualización)
 */
public record EcommerceResponse(
    String uid,
    String name,
    String slug,
    String status,
    Instant createdAt,
    Instant updatedAt
) {}
