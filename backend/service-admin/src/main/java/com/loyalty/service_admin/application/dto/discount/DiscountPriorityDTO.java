package com.loyalty.service_admin.application.dto.discount;

import java.time.Instant;
import java.util.UUID;

public record DiscountPriorityDTO(
        UUID id,
        UUID discountTypeId,
        Integer priorityLevel,
        Boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {}
