package com.loyalty.admin.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiKeyValidationResponse {
    private boolean valid;
    private Long ecommerceId;
    private String ecommerceNombre;
}
