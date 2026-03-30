package com.loyalty.service_admin.application.dto.configuration;

import java.util.UUID;

public record ConfigurationWriteData(
        UUID configId,
        long version
) {
}
