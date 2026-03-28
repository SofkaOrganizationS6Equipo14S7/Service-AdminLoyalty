package com.loyalty.service_engine.application.dto.calculate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DiscountCalculateResponseV2(
        UUID ecommerceId,
        String currency,
        String roundingRule,
        BigDecimal totalRequested,
        BigDecimal capAmount,
        BigDecimal totalApplied,
        boolean capped,
        List<AppliedDiscount> appliedDiscounts,
        Instant calculatedAt
) {
    public record AppliedDiscount(
            String type,
            BigDecimal requestedAmount,
            BigDecimal appliedAmount,
            int order
    ) {
    }
}
