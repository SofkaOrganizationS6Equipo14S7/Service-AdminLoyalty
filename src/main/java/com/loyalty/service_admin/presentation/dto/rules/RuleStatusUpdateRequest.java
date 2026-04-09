package com.loyalty.service_admin.presentation.dto.rules;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for PATCH /api/v1/rules/{ruleId}/status
 * 
 * SPEC-008: Endpoint Dedicado para Cambiar Status de Reglas
 * HU-14: Cambiar Status de Regla mediante Endpoint Dedicado
 */
public record RuleStatusUpdateRequest(
    @NotNull(message = "El campo 'active' es obligatorio")
    Boolean active
) {}
