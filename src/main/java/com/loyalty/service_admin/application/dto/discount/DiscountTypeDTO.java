package com.loyalty.service_admin.application.dto.discount;

import java.time.Instant;
import java.util.UUID;

public record DiscountTypeDTO(
        UUID id,
        String code,
        String displayName,
        Instant createdAt
) {}
