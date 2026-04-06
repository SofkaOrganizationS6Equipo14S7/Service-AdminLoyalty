package com.loyalty.service_admin.application.port.in;

import com.loyalty.service_admin.application.dto.apikey.ApiKeyCreatedResponse;
import com.loyalty.service_admin.application.dto.apikey.ApiKeyListResponse;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de entrada (Use Case) para operaciones de API Keys.
 * Define el contrato de negocio para gestión de claves de acceso,
 * desacoplando la implementación del controller.
 * 
 * Responsabilidades:
 * - Crear nuevas API Keys (UUID → SHA-256)
 * - Listar API Keys de un ecommerce (con masking)
 * - Eliminar API Keys (con validación de pertenencia)
 */
public interface ApiKeyUseCase {
    
    /**
     * Crea una nueva API Key para un ecommerce.
     *
     * @param ecommerceId ID del ecommerce propietario
     * @return respuesta con plainkey (SIN enmascarar), válido una sola vez
     * @throws com.loyalty.service_admin.infrastructure.exception.EcommerceNotFoundException
     *         si el ecommerce no existe
     */
    ApiKeyCreatedResponse createApiKey(UUID ecommerceId);
    
    /**
     * Lista todas las API Keys de un ecommerce.
     *
     * @param ecommerceId ID del ecommerce
     * @return lista de API Keys con masking (****XXXX)
     * @throws com.loyalty.service_admin.infrastructure.exception.EcommerceNotFoundException
     *         si el ecommerce no existe
     */
    List<ApiKeyListResponse> getApiKeysByEcommerce(UUID ecommerceId);
    
    /**
     * Elimina una API Key específica.
     *
     * @param ecommerceId ID del ecommerce propietario
     * @param keyId ID de la clave a eliminar
     * @throws com.loyalty.service_admin.infrastructure.exception.ApiKeyNotFoundException
     *         si la clave no existe o no pertenece al ecommerce
     */
    void deleteApiKey(UUID ecommerceId, UUID keyId);
}
