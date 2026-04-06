package com.loyalty.service_admin.infrastructure.messaging.rabbitmq;

import com.loyalty.service_admin.application.port.out.EcommerceEventPort;
import com.loyalty.service_admin.infrastructure.rabbitmq.EcommerceEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Adaptador de Eventos: RabbitMQ
 * 
 * Implementa el puerto EcommerceEventPort usando RabbitMQ/Spring AMQP.
 * Delegación pura al EcommerceEventPublisher sin lógica adicional.
 * 
 * SPEC-015: Ecommerce Onboarding con Arquitectura Hexagonal
 * Hexagonal Architecture: Adapter pattern para eventos
 * 
 * Si en el futuro necesitamos cambiar a Kafka o Google Pub/Sub,
 * solo crearemos un nuevo adaptador sin afectar la lógica de negocio.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMqEcommerceAdapter implements EcommerceEventPort {
    
    private final EcommerceEventPublisher eventPublisher;
    
    @Override
    public void publishEcommerceCreated(UUID ecommerceId, String name, String slug) {
        log.debug("Delegando publicación de evento ECOMMERCE_CREATED al publisher");
        eventPublisher.publishEcommerceCreated(ecommerceId, name, slug);
    }
    
    @Override
    public void publishEcommerceStatusChanged(UUID ecommerceId, String newStatus) {
        log.debug("Delegando publicación de evento ECOMMERCE_STATUS_CHANGED al publisher");
        // Convertir String a EcommerceStatus enum
        com.loyalty.service_admin.domain.model.ecommerce.EcommerceStatus status = 
            com.loyalty.service_admin.domain.model.ecommerce.EcommerceStatus.valueOf(newStatus);
        eventPublisher.publishEcommerceStatusChanged(ecommerceId, status);
    }
}
