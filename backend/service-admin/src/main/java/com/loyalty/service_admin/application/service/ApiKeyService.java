package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.ApiKeyEventPayload;
import com.loyalty.service_admin.application.dto.ApiKeyListResponse;
import com.loyalty.service_admin.application.dto.ApiKeyResponse;
import com.loyalty.service_admin.domain.entity.ApiKeyEntity;
import com.loyalty.service_admin.domain.repository.ApiKeyRepository;
import com.loyalty.service_admin.infrastructure.exception.ApiKeyNotFoundException;
import com.loyalty.service_admin.infrastructure.exception.EcommerceNotFoundException;
import com.loyalty.service_admin.infrastructure.rabbitmq.ApiKeyEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ApiKeyService {
    
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyEventPublisher apiKeyEventPublisher;
    private final EcommerceService ecommerceService;
    
    public ApiKeyService(
        ApiKeyRepository apiKeyRepository,
        ApiKeyEventPublisher apiKeyEventPublisher,
        EcommerceService ecommerceService
    ) {
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyEventPublisher = apiKeyEventPublisher;
        this.ecommerceService = ecommerceService;
    }
    
    /**
     * Crea una nueva API Key para un ecommerce.
     * @param ecommerceId ID del ecommerce propietario
     * @return ApiKeyResponse con la key enmascarada
     * @throws EcommerceNotFoundException si el ecommerce no existe
     */
    @Transactional
    public ApiKeyResponse createApiKey(UUID ecommerceId) {
        // Validar que el ecommerce existe
        ecommerceService.validateEcommerceExists(ecommerceId);
        
        // Generar UUID v4 para la key
        String keyString = UUID.randomUUID().toString();
        
        // Crear entity
        ApiKeyEntity entity = new ApiKeyEntity();
        entity.setKeyString(keyString);
        entity.setEcommerceId(ecommerceId);
        
        // Persistir
        ApiKeyEntity saved = apiKeyRepository.save(entity);
        
        // Publicar evento de creación
        ApiKeyEventPayload event = new ApiKeyEventPayload(
            "API_KEY_CREATED",
            saved.getId().toString(),
            keyString,
            ecommerceId.toString(),
            Instant.now()
        );
        apiKeyEventPublisher.publishApiKeyCreated(event);
        
        log.info("API Key created for ecommerce: {}, keyId: {}", ecommerceId, saved.getId());
        
        // Retornar response con key enmascarada
        return toApiKeyResponse(saved);
    }
    
    /**
     * Lista todas las API Keys de un ecommerce.
     * @param ecommerceId ID del ecommerce
     * @return lista de ApiKeyListResponse con keys enmascaradas
     * @throws EcommerceNotFoundException si el ecommerce no existe
     */
    @Transactional(readOnly = true)
    public List<ApiKeyListResponse> getApiKeysByEcommerce(UUID ecommerceId) {
        // Validar que el ecommerce existe
        ecommerceService.validateEcommerceExists(ecommerceId);
        
        // Obtener todas las keys del ecommerce
        List<ApiKeyEntity> keys = apiKeyRepository.findByEcommerceId(ecommerceId);
        
        // Convertir a DTO con key enmascarada
        return keys.stream()
            .map(this::toApiKeyListResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Elimina una API Key (revoca acceso).
     * @param ecommerceId ID del ecommerce propietario
     * @param keyId ID de la API Key a eliminar
     * @throws EcommerceNotFoundException si el ecommerce no existe
     * @throws ApiKeyNotFoundException si la key no existe o no pertenece al ecommerce
     */
    @Transactional
    public void deleteApiKey(UUID ecommerceId, UUID keyId) {
        // Validar que el ecommerce existe
        ecommerceService.validateEcommerceExists(ecommerceId);
        
        // Obtener la key
        ApiKeyEntity key = apiKeyRepository.findById(keyId)
            .orElseThrow(() -> new ApiKeyNotFoundException("API Key no encontrada"));
        
        // Validar que la key pertenece al ecommerce
        if (!key.getEcommerceId().equals(ecommerceId)) {
            throw new ApiKeyNotFoundException("API Key no pertenece a este ecommerce");
        }
        
        // Publicar evento de eliminación ANTES de eliminar
        ApiKeyEventPayload event = new ApiKeyEventPayload(
            "API_KEY_DELETED",
            key.getId().toString(),
            key.getKeyString(),
            ecommerceId.toString(),
            Instant.now()
        );
        apiKeyEventPublisher.publishApiKeyDeleted(event);
        
        // Eliminar de base de datos
        apiKeyRepository.delete(key);
        
        log.info("API Key deleted for ecommerce: {}, keyId: {}", ecommerceId, keyId);
    }
    
    /**
     * Convierte ApiKeyEntity a ApiKeyResponse con key enmascarada.
     */
    private ApiKeyResponse toApiKeyResponse(ApiKeyEntity entity) {
        return new ApiKeyResponse(
            entity.getId().toString(),
            maskKey(entity.getKeyString()),
            entity.getEcommerceId().toString(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
    
    /**
     * Convierte ApiKeyEntity a ApiKeyListResponse con key enmascarada.
     */
    private ApiKeyListResponse toApiKeyListResponse(ApiKeyEntity entity) {
        return new ApiKeyListResponse(
            entity.getId().toString(),
            maskKey(entity.getKeyString()),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
    
    /**
     * Enmascara una key al formato ****XXXX (últimos 4 caracteres).
     * Para UUID, extrae los últimos 4 chars antes del final.
     * Ejemplo: 550e8400-e29b-41d4-a716-446655440000 → ****0000
     */
    private String maskKey(String keyString) {
        if (keyString == null || keyString.length() < 4) {
            return "****";
        }
        String lastFour = keyString.substring(keyString.length() - 4);
        return "****" + lastFour;
    }
}
