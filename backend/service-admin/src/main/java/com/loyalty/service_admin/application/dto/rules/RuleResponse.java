package com.loyalty.service_admin.application.dto.rules;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RuleResponse(
        UUID id,
        UUID ecommerceId,
        UUID discountPriorityId,
        String name,
        String description,
        BigDecimal discountPercentage,
        Boolean isActive,
        List<RuleAttributeValueDTO> attributes,
        Instant createdAt,
        Instant updatedAt
) {}
