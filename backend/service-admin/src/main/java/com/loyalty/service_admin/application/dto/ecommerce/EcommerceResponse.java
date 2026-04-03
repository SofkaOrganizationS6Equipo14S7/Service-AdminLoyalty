package com.loyalty.service_admin.application.dto.ecommerce;

import java.time.Instant;

public record EcommerceResponse(
    String uid,
    String name,
    String slug,
    String status,
    Instant createdAt,
    Instant updatedAt
) {}
