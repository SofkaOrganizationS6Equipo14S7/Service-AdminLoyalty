package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.apikey.*;
import com.loyalty.service_admin.application.service.ApiKeyService;
import com.loyalty.service_admin.infrastructure.exception.AuthorizationException;
import com.loyalty.service_admin.infrastructure.security.SecurityContextHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ecommerces/{ecommerceId}/api-keys")
@Slf4j
@PreAuthorize("isAuthenticated()")
public class ApiKeyController {
    
    private final ApiKeyService apiKeyService;
    private final SecurityContextHelper securityContextHelper;
    
    public ApiKeyController(ApiKeyService apiKeyService, SecurityContextHelper securityContextHelper) {
        this.apiKeyService = apiKeyService;
        this.securityContextHelper = securityContextHelper;
    }
    
    /**
     * Crear nueva API Key para un ecommerce
     * @param ecommerceId ecommerce identifier
     * @param request empty body
     * @return HTTP 201 Created with ApiKeyCreatedResponse (key sin enmascarar)
     */
    @PostMapping
    public ResponseEntity<ApiKeyCreatedResponse> createApiKey(
        @PathVariable UUID ecommerceId,
        @RequestBody(required = false) ApiKeyCreateRequest request
    ) {
        // Validar autorización: STORE_ADMIN solo puede crear en su propio ecommerce
        validateEcommerceAccess(ecommerceId);
        
        ApiKeyCreatedResponse response = apiKeyService.createApiKey(ecommerceId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Listar API Keys de un ecommerce
     * @param ecommerceId ecommerce identifier
     * @return HTTP 200 OK with list of ApiKeyListResponse
     */
    @GetMapping
    public ResponseEntity<List<ApiKeyListResponse>> getApiKeys(
        @PathVariable UUID ecommerceId
    ) {
        // Validar autorización: STORE_ADMIN solo puede listar su propio ecommerce
        validateEcommerceAccess(ecommerceId);
        
        List<ApiKeyListResponse> keys = apiKeyService.getApiKeysByEcommerce(ecommerceId);
        return ResponseEntity.ok(keys);
    }
    
    /**
     * Eliminar una API Key
     * @param ecommerceId ecommerce owner identifier
     * @param keyId API key identifier to delete
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{keyId}")
    public ResponseEntity<Void> deleteApiKey(
        @PathVariable UUID ecommerceId,
        @PathVariable UUID keyId
    ) {
        // Validar autorización: STORE_ADMIN solo puede eliminar keys de su propio ecommerce
        validateEcommerceAccess(ecommerceId);
        
        apiKeyService.deleteApiKey(ecommerceId, keyId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Valida que el usuario actual tiene acceso al ecommerce especificado.
     * SUPER_ADMIN: acceso a todos
     * STORE_ADMIN: acceso solo a su propio ecommerce
     */
    private void validateEcommerceAccess(UUID ecommerceId) {
        String currentRole = securityContextHelper.getCurrentUserRole();
        
        if (!"SUPER_ADMIN".equals(currentRole)) {
            UUID userEcommerceId = securityContextHelper.getCurrentUserEcommerceId();
            if (userEcommerceId == null || !userEcommerceId.equals(ecommerceId)) {
                throw new AuthorizationException(
                    "No tienes permiso para acceder a las API keys de este ecommerce"
                );
            }
        }
    }
}
