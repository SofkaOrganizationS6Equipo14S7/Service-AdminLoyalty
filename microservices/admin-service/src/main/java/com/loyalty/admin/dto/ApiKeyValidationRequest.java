package com.loyalty.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApiKeyValidationRequest {
    @NotBlank(message = "apiKey es obligatoria")
    private String apiKey;
}
