package com.loyalty.service_engine.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for customer classification response.
 * Returned in POST /api/v1/customers/classify
 */
public record ClassifyResponseV1(
    UUID tierUid,
    String tierName,
    Integer tierLevel,
    String classificationReason,
    Instant calculatedAt
) {
}
