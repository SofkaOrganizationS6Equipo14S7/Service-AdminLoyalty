package com.loyalty.service_admin.application.port.out;

import java.util.UUID;

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
     * @param ecommerceId uuid del ecommerce
     * @param newStatus nuevo status (ACTIVE o INACTIVE)
     * @throws RuntimeException si falla la publicación (causa rollback transaccional)
     */
    void publishEcommerceStatusChanged(UUID ecommerceId, String newStatus);
}
