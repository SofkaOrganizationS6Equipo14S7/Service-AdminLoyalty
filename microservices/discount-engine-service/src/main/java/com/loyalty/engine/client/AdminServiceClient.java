package com.loyalty.engine.client;

import com.loyalty.engine.dto.AdminConfiguracionResponse;
import com.loyalty.engine.dto.AdminReglaResponse;

import java.util.List;

public interface AdminServiceClient {
    AdminConfiguracionResponse getConfiguracion(Long ecommerceId);
    List<AdminReglaResponse> getReglas(Long ecommerceId);
    boolean validateApiKey(String apiKey);
}
