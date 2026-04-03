package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.apikey.*;
import com.loyalty.service_admin.domain.entity.ApiKeyEntity;
import com.loyalty.service_admin.domain.repository.ApiKeyRepository;
import com.loyalty.service_admin.infrastructure.exception.ApiKeyNotFoundException;
import com.loyalty.service_admin.infrastructure.exception.EcommerceNotFoundException;
import com.loyalty.service_admin.infrastructure.rabbitmq.ApiKeyEventPublisher;
import com.loyalty.service_admin.infrastructure.util.HashingUtil;
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
     * 
     * Flujo de seguridad:
     * 1. Generar UUID v4 (plaintext)
     * 2. Hashear con SHA-256 para persistencia
     * 3. Guardar hash en BD
     * 4. Retornar el UUID plaintext al cliente (una sola vez, 201 Created)
     * 5. Publicar evento con el hash para sincronización a Engine
     * 
     * @param ecommerceId ID del ecommerce propietario
     * @return ApiKeyResponse con la key enmascarada (plaintext)
     * @throws EcommerceNotFoundException si el ecommerce no existe
     */
    @Transactional
    public ApiKeyResponse createApiKey(UUID ecommerceId) {
        // Validar que el ecommerce existe
        ecommerceService.validateEcommerceExists(ecommerceId);
        
        // 1. Generar UUID v4 plaintext
        String plainKeyValue = UUID.randomUUID().toString();
        
        // 2. Hashear con SHA-256
        String hashedKeyValue = HashingUtil.sha256(plainKeyValue);
        
        // 3. Crear entity con hash persistido
        ApiKeyEntity entity = new ApiKeyEntity();
        //entity.setKeyPrefix(plainKeyValue.substring(0, 8));
        entity.setHashedKey(hashedKeyValue);
        entity.setEcommerceId(ecommerceId);
        entity.setIsActive(true);
        
        // 4. Persistir en BD (solo el hash)
        ApiKeyEntity saved = apiKeyRepository.save(entity);
        
        // 5. Publicar evento de creación (con hash para Engine Service)
        ApiKeyEventPayload event = new ApiKeyEventPayload(
            "API_KEY_CREATED",
            saved.getId().toString(),
            hashedKeyValue,  // Pasar el hash, no el plaintext
            ecommerceId.toString(),
            Instant.now()
        );
        apiKeyEventPublisher.publishApiKeyCreated(event);
        
        log.info("API Key created for ecommerce: {}, keyId: {}", ecommerceId, saved.getId());
        
        // Retornar response con el plaintext enmascarado (solo para esta respuesta 201)
        return toApiKeyResponse(saved, plainKeyValue);
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
        
        // Publicar evento de eliminación ANTES de eliminar (incluir hash)
        ApiKeyEventPayload event = new ApiKeyEventPayload(
            "API_KEY_DELETED",
            key.getId().toString(),
            key.getHashedKey(),  // Pasar el hash, no plaintext
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
     * 
     * Nota: plainKeyValue solo se proporciona en el momento de creación (201 Created).
     * En GETs posteriores, no tenemos el plaintext, solo mostramos ****XXXX del hash.
     */
    private ApiKeyResponse toApiKeyResponse(ApiKeyEntity entity, String plainKeyValue) {
        return new ApiKeyResponse(
            entity.getId(),
            maskKey(plainKeyValue),  // Masking del plaintext
            entity.getExpiresAt(),
            entity.getEcommerceId(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
    
    /**
     * Convierte ApiKeyEntity a ApiKeyListResponse con key enmascarada (para GETs).
     * Nota: En listados, solo tenemos el hash, así que mostramos ****XXXX del hash.
     */
    private ApiKeyListResponse toApiKeyListResponse(ApiKeyEntity entity) {
        return new ApiKeyListResponse(
            entity.getId(),
            maskKey(entity.getHashedKey()),  // Masking del hash (fallback)
            entity.getExpiresAt(),
            entity.getIsActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
    
    /**
     * Enmascara una key al formato ****XXXX (últimos 4 caracteres).
     * Para UUID, extrae los últimos 4 chars.
     * Ejemplo: 550e8400-e29b-41d4-a716-446655440000 → ****0000
     * Ejemplo hash: a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3 → ****e3
     */
    private String maskKey(String keyString) {
        if (keyString == null || keyString.length() < 4) {
            return "****";
        }
        String lastFour = keyString.substring(keyString.length() - 4);
        return "****" + lastFour;
    }
}
