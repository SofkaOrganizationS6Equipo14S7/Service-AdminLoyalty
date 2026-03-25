package com.loyalty.engine.dto;

import lombok.Data;

@Data
public class ApiKeyValidationResponse {
    private boolean valid;
    private Long ecommerceId;
    private String ecommerceNombre;
}
