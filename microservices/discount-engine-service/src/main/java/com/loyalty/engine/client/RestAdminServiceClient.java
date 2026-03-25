package com.loyalty.engine.client;

import com.loyalty.engine.dto.AdminConfiguracionResponse;
import com.loyalty.engine.dto.AdminReglaResponse;
import com.loyalty.engine.dto.ApiKeyValidationRequest;
import com.loyalty.engine.dto.ApiKeyValidationResponse;
import com.loyalty.engine.exception.EngineException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RestAdminServiceClient implements AdminServiceClient {

    private final RestTemplate restTemplate;

    @Value("${admin.service.base-url}")
    private String adminBaseUrl;

    @Override
    public AdminConfiguracionResponse getConfiguracion(Long ecommerceId) {
        try {
            String url = adminBaseUrl + "/configuracion/" + ecommerceId;
            return restTemplate.getForObject(url, AdminConfiguracionResponse.class);
        } catch (RestClientException ex) {
            log.error("Error consultando configuracion ecommerceId={}", ecommerceId, ex);
            throw new EngineException("No fue posible consultar configuracion del admin-service");
        }
    }

    @Override
    public List<AdminReglaResponse> getReglas(Long ecommerceId) {
        try {
            String url = adminBaseUrl + "/reglas/ecommerce/" + ecommerceId;
            return restTemplate.exchange(url,
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<List<AdminReglaResponse>>() {
                            })
                    .getBody();
        } catch (RestClientException ex) {
            log.error("Error consultando reglas ecommerceId={}", ecommerceId, ex);
            throw new EngineException("No fue posible consultar reglas del admin-service");
        }
    }

    @Override
    public boolean validateApiKey(String apiKey) {
        try {
            String url = adminBaseUrl + "/apikey/validar";
            ApiKeyValidationRequest request = new ApiKeyValidationRequest();
            request.setApiKey(apiKey);
            ApiKeyValidationResponse response = restTemplate.postForObject(url,
                    new HttpEntity<>(request),
                    ApiKeyValidationResponse.class);
            return response != null && response.isValid();
        } catch (RestClientException ex) {
            log.error("Error validando apiKey", ex);
            throw new EngineException("No fue posible validar apiKey");
        }
    }
}
