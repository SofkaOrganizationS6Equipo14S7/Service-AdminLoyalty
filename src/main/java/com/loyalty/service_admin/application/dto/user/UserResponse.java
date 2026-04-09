package com.loyalty.service_admin.application.dto.user;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID uid,
    String username,
    UUID roleId,
    String roleName,
    String email,
    UUID ecommerceId,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {
}
