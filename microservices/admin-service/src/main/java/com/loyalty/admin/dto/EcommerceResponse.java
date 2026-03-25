package com.loyalty.admin.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EcommerceResponse {
    private Long id;
    private String nombre;
    private String apiKey;
    private boolean activo;
}
