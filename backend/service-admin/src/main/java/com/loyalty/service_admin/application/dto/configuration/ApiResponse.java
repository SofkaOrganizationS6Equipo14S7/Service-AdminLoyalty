package com.loyalty.service_admin.application.dto.configuration;

public record ApiResponse<T>(
        boolean success,
        T data
) {
}
