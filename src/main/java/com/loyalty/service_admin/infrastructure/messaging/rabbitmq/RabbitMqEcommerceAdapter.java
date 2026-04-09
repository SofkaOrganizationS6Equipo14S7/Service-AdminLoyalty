package com.loyalty.service_admin.infrastructure.messaging.rabbitmq;

import com.loyalty.service_admin.application.port.out.EcommerceEventPort;
import com.loyalty.service_admin.infrastructure.rabbitmq.EcommerceEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

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
        com.loyalty.service_admin.domain.model.ecommerce.EcommerceStatus status =
            com.loyalty.service_admin.domain.model.ecommerce.EcommerceStatus.valueOf(newStatus);
        eventPublisher.publishEcommerceStatusChanged(ecommerceId, status);
    }
}
