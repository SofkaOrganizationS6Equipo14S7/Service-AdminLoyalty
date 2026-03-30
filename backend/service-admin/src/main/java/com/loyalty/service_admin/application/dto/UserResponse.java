package com.loyalty.service_admin.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO para respuesta con datos del usuario.
 * 
 * Utilizado en endpoints:
 * - GET /api/v1/auth/me (usuario actual)
 * - GET /api/v1/users (listar usuarios)
 * - GET /api/v1/users/{uid} (obtener usuario)
 * - POST /api/v1/users (crear usuario)
 * - PUT /api/v1/users/{uid} (actualizar usuario)
 * - DELETE /api/v1/users/{uid} (eliminar usuario)
 * 
 * Campos:
 * - uid: UUID generado por el sistema (SPEC-005 RN-02)
 * - username: identificador único globalmente (SPEC-005 CRITERIO-2.1.2)
 * - email: contacto único globalmente (SPEC-005 CRITERIO-2.1.4)
 * - role: SUPER_ADMIN | STORE_ADMIN | STORE_USER (SPEC-005 RN-05)
 * - ecommerceId: NULL para SUPER_ADMIN (RN-01), UUID para otros roles (SPEC-005 RN-03)
 * - active: es el usuario activo/eliminado lógicamente (false = eliminado)
 * - createdAt: timestamp UTC de creación (SPEC-005 RN-07)
 * - updatedAt: timestamp UTC de última modificación (SPEC-005 RN-07)
 * 
 * Implementa:
 * - SPEC-005: SUPERADMIN - Acceso Total a la Plataforma
 * - SPEC-003: Administración de Ecommerce por STORE_ADMIN (herencia)
 */
public record UserResponse(
    UUID uid,
    String username,
    String role,
    String email,
    UUID ecommerceId,
    Boolean active,
    Instant createdAt,
    Instant updatedAt
) {
}
