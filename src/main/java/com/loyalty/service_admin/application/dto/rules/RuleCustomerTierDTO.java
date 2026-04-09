package com.loyalty.service_admin.application.dto.rules;

import java.util.UUID;

public record RuleCustomerTierDTO(
        UUID customerTierId,
        String customerTierName
) {}
