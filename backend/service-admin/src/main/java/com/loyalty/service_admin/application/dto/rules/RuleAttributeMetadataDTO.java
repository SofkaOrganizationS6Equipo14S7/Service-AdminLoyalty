package com.loyalty.service_admin.application.dto.rules;

import java.util.UUID;

public record RuleAttributeMetadataDTO(
        UUID id,
        String attributeName,
        String attributeType,
        Boolean isRequired,
        String description
) {}
