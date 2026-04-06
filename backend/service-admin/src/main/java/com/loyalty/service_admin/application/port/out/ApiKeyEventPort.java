package com.loyalty.service_admin.application.port.out;

import com.loyalty.service_admin.application.dto.apikey.ApiKeyEventPayload;

/**
 * Puerto de salida para operaciones de publicación de eventos de API Keys.
 * Abstrae el broker de mensajes (RabbitMQ) del servicio de negocio.
 * 
 * Implementación: {@link com.loyalty.service_admin.infrastructure.messaging.ApiKeyEventAdapter}
 * 
 * Responsabilidades:
 * - Publicar eventos de creación y eliminación de API Keys
 * - Delegación al publisher RabbitMQ sin lógica de negocio
 * - Garantizar entrega asíncrona y confiable de eventos
 */
public interface ApiKeyEventPort {
    
    /**
     * Publica un evento de creación de API Key.
     *
     * @param event payload del evento con hashedKey (nunca plaintext)
     */
    void publishApiKeyCreated(ApiKeyEventPayload event);
    
    /**
     * Publica un evento de eliminación de API Key.
     *
     * @param event payload del evento con hashedKey
     */
    void publishApiKeyDeleted(ApiKeyEventPayload event);
}
