package com.loyalty.service_admin.application.dto.rules;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Map;

public record RuleCreateRequest(
        @NotBlank(message = "name is required")
        @Size(min = 3, max = 255, message = "name must be between 3 and 255 characters")
        String name,

        @Size(max = 1000, message = "description must not exceed 1000 characters")
        String description,

        @NotNull(message = "discountPercentage is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "discountPercentage must be >= 0")
        @DecimalMax(value = "100.0", inclusive = true, message = "discountPercentage must be <= 100")
        BigDecimal discountPercentage,

        @NotNull(message = "discountPriorityId is required")
        String discountPriorityId,

        @NotNull(message = "attributes map is required")
        Map<String, String> attributes
) {}
