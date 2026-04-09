package com.loyalty.service_admin.application.dto.rules;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record AssignCustomerTiersRequest(
        @NotEmpty(message = "customerTierIds list cannot be empty")
        List<UUID> customerTierIds
) {}
