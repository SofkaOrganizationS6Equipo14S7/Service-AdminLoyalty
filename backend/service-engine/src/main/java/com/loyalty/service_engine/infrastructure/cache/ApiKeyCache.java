package com.loyalty.service_engine.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.loyalty.service_engine.domain.entity.ApiKeyEntity;
import com.loyalty.service_engine.domain.repository.ApiKeyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Caché Caffeine para API Keys.
 * Estructura: {keyString → ecommerceId}
 * Se sincroniza con BD y RabbitMQ.
 */
@Component
@Slf4j
public class ApiKeyCache {
    
    private final Cache<String, String> cache;
    private final ApiKeyRepository apiKeyRepository;
    
    public ApiKeyCache(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
        
        // Configurar caché con expiración de 1 día
        this.cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(1, TimeUnit.DAYS)
            .recordStats()
            .build();
        
        // Cargar keys al inicializar
        loadFromDatabase();
    }
    
    /**
     * Carga todas las API Keys desde BD y las inserta en caché.
     * Ejecutado al startup de Engine.
     */
    public void loadFromDatabase() {
        try {
            log.info("Loading API Keys from database into cache...");
            List<ApiKeyEntity> keys = apiKeyRepository.findAll();
            
            for (ApiKeyEntity key : keys) {
                cache.put(key.getKeyString(), key.getEcommerceId().toString());
            }
            
            log.info("Loaded {} API Keys into cache", keys.size());
        } catch (Exception e) {
            log.error("Error loading API Keys from database", e);
            // No lanzar excepción, permitir que Engine arranque con caché vacía
        }
    }
    
    /**
     * Valida si una API Key es válida.
     * Búsqueda en caché (< 1ms esperado).
     * @param keyString la clave a validar
     * @return true si la clave existe en caché, false si no
     */
    public boolean validateKey(String keyString) {
        if (keyString == null || keyString.isEmpty()) {
            return false;
        }
        
        String ecommerceId = cache.getIfPresent(keyString);
        boolean isValid = ecommerceId != null;
        
        log.debug("API Key validation: {} - {}", 
            keyString.substring(0, Math.min(8, keyString.length())) + "...", 
            isValid ? "VALID" : "INVALID");
        
        return isValid;
    }
    
    /**
     * Obtiene el ecommerce asociado a una API Key.
     * @param keyString la clave
     * @return ecommerceId si existe, null si no
     */
    public String getEcommerceId(String keyString) {
        if (keyString == null || keyString.isEmpty()) {
            return null;
        }
        return cache.getIfPresent(keyString);
    }
    
    /**
     * Agrega una nueva API Key a caché y BD.
     * Llamado cuando se recibe evento API_KEY_CREATED desde RabbitMQ.
     * @param keyString la clave
     * @param ecommerceId el ecommerce propietario
     */
    public void addKey(String keyString, String ecommerceId) {
        try {
            // Insertar en BD
            ApiKeyEntity entity = new ApiKeyEntity();
            entity.setKeyString(keyString);
            entity.setEcommerceId(UUID.fromString(ecommerceId));
            apiKeyRepository.save(entity);
            
            // Insertar en caché
            cache.put(keyString, ecommerceId);
            
            log.info("API Key added: keyString={}, ecommerceId={}",
                keyString.substring(0, Math.min(8, keyString.length())) + "...",
                ecommerceId);
        } catch (Exception e) {
            log.error("Error adding API Key to cache", e);
            throw new RuntimeException("Error adding API Key", e);
        }
    }
    
    /**
     * Elimina una API Key de caché y BD.
     * Llamado cuando se recibe evento API_KEY_DELETED desde RabbitMQ.
     * @param keyString la clave a eliminar
     */
    public void removeKey(String keyString) {
        try {
            // Eliminar de BD
            apiKeyRepository.findByKeyString(keyString)
                .ifPresent(apiKeyRepository::delete);
            
            // Eliminar de caché
            cache.invalidate(keyString);
            
            log.info("API Key removed: keyString={}",
                keyString.substring(0, Math.min(8, keyString.length())) + "...");
        } catch (Exception e) {
            log.error("Error removing API Key from cache", e);
            throw new RuntimeException("Error removing API Key", e);
        }
    }
    
    /**
     * Obtiene estadísticas de caché.
     */
    public CacheStats getCacheStats() {
        return cache.stats();
    }
    
    /**
     * Limpia toda la caché y la recarga de BD.
     * Útil para resincronización.
     */
    public void refresh() {
        cache.invalidateAll();
        loadFromDatabase();
        log.info("Cache refreshed");
    }
}
