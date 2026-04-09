package com.loyalty.service_admin.application.dto.configuration;

import com.loyalty.service_admin.domain.model.RoundingRule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ConfigurationPatchRequest(
        @Size(min = 3, max = 3) String currency,
        RoundingRule roundingRule,
        @Valid CapRequest cap,
        List<@Valid DiscountPriorityRequest> priority
) {
}
