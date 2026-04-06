package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.apikey.ApiKeyCreatedResponse;
import com.loyalty.service_admin.application.dto.apikey.ApiKeyEventPayload;
import com.loyalty.service_admin.application.dto.apikey.ApiKeyListResponse;
import com.loyalty.service_admin.application.port.in.ApiKeyUseCase;
import com.loyalty.service_admin.application.port.out.ApiKeyEventPort;
import com.loyalty.service_admin.application.port.out.ApiKeyPersistencePort;
import com.loyalty.service_admin.domain.entity.ApiKeyEntity;
import com.loyalty.service_admin.infrastructure.exception.ApiKeyNotFoundException;
import com.loyalty.service_admin.infrastructure.util.HashingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementación del use case de API Keys usando Arquitectura Hexagonal.
 * 
 * Responsabilidades:
 * - Orquestar la lógica de negocio de creación, listado y eliminación de API Keys
 * - Delegar persistencia al puerto out (ApiKeyPersistencePort)
 * - Delegar eventos al puerto out (ApiKeyEventPort)
 * - Validar existencia de ecommerce (delegado a EcommerceService)
 * - Garantizar que la lógica es testeable sin Spring Context
 * 
 * Reglas de Negocio:
 * - API Keys se generan como UUID v4
 * - En BD se guardan como SHA-256 (nunca plaintext)
 * - En respuesta 201 se devuelven sin enmascarar (una sola vez)
 * - En respuesta GET se devuelven enmascaradas (****XXXX)
 * - Expiración: 365 días desde creación
 * - Eventos siempre se publican (con o sin persistencia)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyServiceImpl implements ApiKeyUseCase {
    
    private final ApiKeyPersistencePort persistencePort;
    private final ApiKeyEventPort eventPort;
    private final EcommerceService ecommerceService;
    
    @Override
    @Transactional
    public ApiKeyCreatedResponse createApiKey(UUID ecommerceId) {
        // Validar que el ecommerce existe
        ecommerceService.validateEcommerceExists(ecommerceId);
        
        // Generar plainkey (UUID v4)
        String plainKeyValue = UUID.randomUUID().toString();
        String hashedKeyValue = HashingUtil.sha256(plainKeyValue);
        
        // Crear entidad
        ApiKeyEntity entity = new ApiKeyEntity();
        entity.setHashedKey(hashedKeyValue);
        entity.setEcommerceId(ecommerceId);
        entity.setIsActive(true);
        entity.setExpiresAt(Instant.now().plus(java.time.Duration.ofDays(365)));
        
        // Persistir a través del puerto
        ApiKeyEntity saved = persistencePort.save(entity);
        
        // Publicar evento
        ApiKeyEventPayload event = new ApiKeyEventPayload(
            "API_KEY_CREATED",
            saved.getId().toString(),
            hashedKeyValue,
            ecommerceId.toString(),
            Instant.now()
        );
        eventPort.publishApiKeyCreated(event);
        
        log.info("API Key created for ecommerce: {}, keyId: {}", ecommerceId, saved.getId());
        
        // Retornar response con plainkey (SIN enmascarar)
        return toApiKeyCreatedResponse(saved, plainKeyValue);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ApiKeyListResponse> getApiKeysByEcommerce(UUID ecommerceId) {
        // Validar que el ecommerce existe
        ecommerceService.validateEcommerceExists(ecommerceId);
        
        // Consultar a través del puerto
        List<ApiKeyEntity> keys = persistencePort.findByEcommerceId(ecommerceId);
        
        // Mapear a responses con masking
        return keys.stream()
            .map(this::toApiKeyListResponse)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void deleteApiKey(UUID ecommerceId, UUID keyId) {
        // Validar que el ecommerce existe
        ecommerceService.validateEcommerceExists(ecommerceId);
        
        // Recuperar la key a través del puerto
        ApiKeyEntity key = persistencePort.findById(keyId)
            .orElseThrow(() -> new ApiKeyNotFoundException("API Key no encontrada"));
        
        // Validar que pertenece al ecommerce
        if (!key.getEcommerceId().equals(ecommerceId)) {
            throw new ApiKeyNotFoundException("API Key no pertenece a este ecommerce");
        }
        
        // Publicar evento ANTES de eliminar
        ApiKeyEventPayload event = new ApiKeyEventPayload(
            "API_KEY_DELETED",
            key.getId().toString(),
            key.getHashedKey(),
            ecommerceId.toString(),
            Instant.now()
        );
        eventPort.publishApiKeyDeleted(event);
        
        // Eliminar a través del puerto
        persistencePort.deleteById(keyId);
        
        log.info("API Key deleted for ecommerce: {}, keyId: {}", ecommerceId, keyId);
    }
    
    /**
     * Convierte entidad a response con plainkey sin enmascarar.
     * Se usa en response 201 Create (una sola ocasión para copiar la clave).
     */
    private ApiKeyCreatedResponse toApiKeyCreatedResponse(ApiKeyEntity entity, String plainKeyValue) {
        return new ApiKeyCreatedResponse(
            entity.getId(),
            plainKeyValue,
            entity.getExpiresAt(),
            entity.getEcommerceId(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
    
    /**
     * Convierte entidad a response con masking.
     * Se usa en response 200 Get (listado de claves).
     */
    private ApiKeyListResponse toApiKeyListResponse(ApiKeyEntity entity) {
        return new ApiKeyListResponse(
            entity.getId(),
            maskKey(entity.getHashedKey()),
            entity.getExpiresAt(),
            entity.getIsActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
    
    /**
     * Enmascara una clave para mostrar solo los últimos 4 caracteres.
     * Formato: ****XXXX (donde XXXX son los últimos 4 caracteres)
     */
    private String maskKey(String keyString) {
        if (keyString == null || keyString.length() < 4) {
            return "****";
        }
        String lastFour = keyString.substring(keyString.length() - 4);
        return "****" + lastFour;
    }
}
