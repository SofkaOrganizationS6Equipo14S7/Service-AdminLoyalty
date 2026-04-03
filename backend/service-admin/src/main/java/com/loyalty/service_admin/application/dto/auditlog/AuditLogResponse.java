package com.loyalty.service_admin.application.dto.auditlog;

import java.time.Instant;
import java.util.UUID;

/**
 * Response para audit_log.
 * Solo lectura - sin endpoints PUT/DELETE (CRITERIO-9.4).
 * 
 * @param id UUID del registro de auditoría
 * @param userId UUID del usuario que realizó la acción
 * @param ecommerceId UUID del ecommerce afectado
 * @param action Tipo de acción (CREATE, UPDATE, DELETE)
 * @param entityName Nombre de la tabla afectada (app_user, ecommerce, etc.)
 * @param entityId UUID del registro afectado
 * @param oldValue Valor anterior (JSON)
 * @param newValue Valor nuevo (JSON)
 * @param createdAt Timestamp del cambio
 */
public record AuditLogResponse(
    UUID id,
    UUID userId,
    UUID ecommerceId,
    String action,
    String entityName,
    UUID entityId,
    String oldValue,
    String newValue,
    Instant createdAt
) {
}
