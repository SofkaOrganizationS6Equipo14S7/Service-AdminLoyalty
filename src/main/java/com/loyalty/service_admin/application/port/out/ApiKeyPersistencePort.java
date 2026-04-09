package com.loyalty.service_admin.application.port.out;

import com.loyalty.service_admin.domain.entity.ApiKeyEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida para operaciones de persistencia de API Keys.
 * Abstrae el acceso a datos (JPA, BD, caché) del servicio de negocio.
 * 
 * Implementación: {@link com.loyalty.service_admin.infrastructure.persistence.jpa.JpaApiKeyAdapter}
 * 
 * Responsabilidades:
 * - CRUD básico de entidades de API Keys
 * - Búsquedas especializadas (por ecommerce, por hash)
 * - Delegación al repositorio JPA sin lógica de negocio
 */
public interface ApiKeyPersistencePort {
    
    /**
     * Persiste una API Key en la base de datos.
     *
     * @param entity entidad a guardar
     * @return entidad guardada con ID asignado
     */
    ApiKeyEntity save(ApiKeyEntity entity);
    
    /**
     * Recupera una API Key por su ID.
     *
     * @param id identificador único
     * @return Optional con la entidad si existe
     */
    Optional<ApiKeyEntity> findById(UUID id);
    
    /**
     * Lista todas las API Keys de un ecommerce.
     *
     * @param ecommerceId ID del ecommerce
     * @return lista de entidades (puede estar vacía)
     */
    List<ApiKeyEntity> findByEcommerceId(UUID ecommerceId);
    
    /**
     * Elimina una API Key de la base de datos.
     *
     * @param id identificador único a eliminar
     */
    void deleteById(UUID id);
    
    /**
     * Verifica si existe una API Key con el hash dado (para evitar duplicados).
     *
     * @param hashedKey hash SHA-256 de la clave
     * @return true si existe, false en caso contrario
     */
    boolean existsByHashedKey(String hashedKey);
}
