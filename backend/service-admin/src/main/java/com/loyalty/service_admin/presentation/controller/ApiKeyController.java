package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.ApiKeyCreateRequest;
import com.loyalty.service_admin.application.dto.ApiKeyListResponse;
import com.loyalty.service_admin.application.dto.ApiKeyResponse;
import com.loyalty.service_admin.application.service.ApiKeyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ecommerces/{ecommerceId}/api-keys")
@Slf4j
public class ApiKeyController {
    
    private final ApiKeyService apiKeyService;
    
    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }
    
    /**
     * POST /api/v1/ecommerces/{ecommerceId}/api-keys
     * Crea una nueva API Key para un ecommerce.
     * 
     * @param ecommerceId UUID del ecommerce
     * @param request body vacío
     * @return HTTP 201 Created con ApiKeyResponse
     */
    @PostMapping
    public ResponseEntity<ApiKeyResponse> createApiKey(
        @PathVariable UUID ecommerceId,
        @RequestBody(required = false) ApiKeyCreateRequest request
    ) {
        log.info("Creating API Key for ecommerce: {}", ecommerceId);
        ApiKeyResponse response = apiKeyService.createApiKey(ecommerceId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * GET /api/v1/ecommerces/{ecommerceId}/api-keys
     * Lista todas las API Keys de un ecommerce.
     * 
     * @param ecommerceId UUID del ecommerce
     * @return HTTP 200 OK con lista de ApiKeyListResponse
     */
    @GetMapping
    public ResponseEntity<List<ApiKeyListResponse>> getApiKeys(
        @PathVariable UUID ecommerceId
    ) {
        log.info("Listing API Keys for ecommerce: {}", ecommerceId);
        List<ApiKeyListResponse> keys = apiKeyService.getApiKeysByEcommerce(ecommerceId);
        return ResponseEntity.ok(keys);
    }
    
    /**
     * DELETE /api/v1/ecommerces/{ecommerceId}/api-keys/{keyId}
     * Elimina (revoca) una API Key.
     * 
     * @param ecommerceId UUID del ecommerce propietario
     * @param keyId UUID de la API Key a eliminar
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{keyId}")
    public ResponseEntity<Void> deleteApiKey(
        @PathVariable UUID ecommerceId,
        @PathVariable UUID keyId
    ) {
        log.info("Deleting API Key {} for ecommerce: {}", keyId, ecommerceId);
        apiKeyService.deleteApiKey(ecommerceId, keyId);
        return ResponseEntity.noContent().build();
    }
}
