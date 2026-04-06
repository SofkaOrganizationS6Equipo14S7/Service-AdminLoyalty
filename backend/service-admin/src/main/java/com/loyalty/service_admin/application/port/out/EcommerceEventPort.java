package com.loyalty.service_admin.application.port.out;

import java.util.UUID;

/**
 * Puerto de Salida: Eventos
 * 
 * Define operaciones de mensajería asíncrona para Ecommerce.
 * Esta interfaz abstrae el broker de mensajes (RabbitMQ, Kafka, SQS, etc.),
 * permitiendo cambiar el mecanismo de publicación sin modificar la lógica de negocio.
 * 
 * SPEC-015: Ecommerce Onboarding con Arquitectura Hexagonal
 * Implementado por: RabbitMqEcommerceAdapter en infrastructure/messaging/rabbitmq
 */
public interface EcommerceEventPort {
    
    /**
     * Publica evento cuando se crea un nuevo ecommerce.
     * 
     * @param ecommerceId uuid del nuevo ecommerce
     * @param name nombre del ecommerce
     * @param slug slug único del ecommerce
     * @throws RuntimeException si falla la publicación (causa rollback transaccional)
     */
    void publishEcommerceCreated(UUID ecommerceId, String name, String slug);
    
    /**
     * Publica evento cuando cambia el status de un ecommerce.
     * 
     * Especialmente importante cuando cambia a INACTIVE para que
     * otros servicios (service-engine) puedan invalidar datos en caché.
     * 
     * @param ecommerceId uuid del ecommerce
     * @param newStatus nuevo status (ACTIVE o INACTIVE)
     * @throws RuntimeException si falla la publicación (causa rollback transaccional)
     */
    void publishEcommerceStatusChanged(UUID ecommerceId, String newStatus);
}
