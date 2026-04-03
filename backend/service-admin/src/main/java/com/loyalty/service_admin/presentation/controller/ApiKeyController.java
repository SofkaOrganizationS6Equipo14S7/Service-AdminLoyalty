package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.apikey.*;
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
     * @param ecommerceId ecommerce identifier
     * @param request empty body
     * @return HTTP 201 Created with ApiKeyResponse
     */
    @PostMapping
    public ResponseEntity<ApiKeyResponse> createApiKey(
        @PathVariable UUID ecommerceId,
        @RequestBody(required = false) ApiKeyCreateRequest request
    ) {
        ApiKeyResponse response = apiKeyService.createApiKey(ecommerceId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * @param ecommerceId ecommerce identifier
     * @return HTTP 200 OK with list of ApiKeyListResponse
     */
    @GetMapping
    public ResponseEntity<List<ApiKeyListResponse>> getApiKeys(
        @PathVariable UUID ecommerceId
    ) {
        List<ApiKeyListResponse> keys = apiKeyService.getApiKeysByEcommerce(ecommerceId);
        return ResponseEntity.ok(keys);
    }
    
    /**
     * @param ecommerceId ecommerce owner identifier
     * @param keyId API key identifier to delete
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{keyId}")
    public ResponseEntity<Void> deleteApiKey(
        @PathVariable UUID ecommerceId,
        @PathVariable UUID keyId
    ) {
        apiKeyService.deleteApiKey(ecommerceId, keyId);
        return ResponseEntity.noContent().build();
    }
}
