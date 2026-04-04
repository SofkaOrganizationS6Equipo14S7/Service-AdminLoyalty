package com.loyalty.service_admin.application.dto.ecommerce;

import java.time.Instant;
import java.util.UUID;

public record EcommerceResponse(
    UUID uid,
    String name,
    String slug,
    String status,
    Instant createdAt,
    Instant updatedAt
) {}
