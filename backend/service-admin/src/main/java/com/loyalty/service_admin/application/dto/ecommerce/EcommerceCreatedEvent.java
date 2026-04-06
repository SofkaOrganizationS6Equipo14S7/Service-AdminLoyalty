package com.loyalty.service_admin.application.dto.ecommerce;

import java.time.Instant;

public record EcommerceCreatedEvent(
    String eventType,
    String ecommerceId,
    String name,
    String slug,
    String status,
    Instant timestamp
) {
}
