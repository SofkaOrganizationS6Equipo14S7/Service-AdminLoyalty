package com.loyalty.service_admin.application.dto;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID uid,
    String username,
    String role,
    String email,
    UUID ecommerceId,
    Boolean active,
    Instant createdAt,
    Instant updatedAt
) {
}
