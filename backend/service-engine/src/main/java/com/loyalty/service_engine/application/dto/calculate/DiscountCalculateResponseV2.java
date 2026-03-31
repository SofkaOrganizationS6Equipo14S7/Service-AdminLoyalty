package com.loyalty.service_engine.application.dto.calculate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response con descuentos calculados e información de clasificación.
 * Incluye el tier de fidelidad asignado al cliente automáticamente.
 */
public record DiscountCalculateResponseV2(
        UUID ecommerceId,
        String currency,
        String roundingRule,
        BigDecimal totalRequested,
        BigDecimal capAmount,
        BigDecimal totalApplied,
        boolean capped,
        List<AppliedDiscount> appliedDiscounts,
        ClassificationInfo classification,
        Instant calculatedAt
) {
    public record AppliedDiscount(
            String type,
            BigDecimal requestedAmount,
            BigDecimal appliedAmount,
            int order
    ) {
    }

    /**
     * Información de clasificación de fidelidad asignada automáticamente.
     */
    public record ClassificationInfo(
            UUID tierUid,
            String tierName,
            Integer tierLevel,
            String classificationReason
    ) {
    }
}
