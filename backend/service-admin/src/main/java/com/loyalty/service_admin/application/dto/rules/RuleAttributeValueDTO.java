package com.loyalty.service_admin.application.dto.rules;

import java.util.UUID;

public record RuleAttributeValueDTO(
        UUID attributeId,
        String attributeName,
        String value
) {}
