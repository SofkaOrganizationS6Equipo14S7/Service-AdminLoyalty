package com.loyalty.service_admin.infrastructure.messaging;

import com.loyalty.service_admin.application.port.out.ApiKeyEventPort;
import com.loyalty.service_admin.application.dto.apikey.ApiKeyEventPayload;
import com.loyalty.service_admin.infrastructure.rabbitmq.ApiKeyEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Adapter que implementa el puerto de eventos usando RabbitMQ.
 * Delega todas las operaciones al publisher RabbitMQ sin agregar lógica adicional.
 * 
 * Responsabilidades:
 * - Traducir llamadas del puerto a operaciones del publisher
 * - Mantener la interfaz limpia y agnóstica de la implementación de mensajería
 */
@Component
@RequiredArgsConstructor
public class ApiKeyEventAdapter implements ApiKeyEventPort {
    
    private final ApiKeyEventPublisher publisher;
    
    @Override
    public void publishApiKeyCreated(ApiKeyEventPayload event) {
        publisher.publishApiKeyCreated(event);
    }
    
    @Override
    public void publishApiKeyDeleted(ApiKeyEventPayload event) {
        publisher.publishApiKeyDeleted(event);
    }
}
