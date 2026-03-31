package com.loyalty.service_admin.infrastructure.rabbitmq;

import com.loyalty.service_admin.application.dto.ProductRuleEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProductRuleEventPublisher {
    
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${rabbitmq.exchange.product-rules:product-rules.exchange}")
    private String exchangeName;

    @Value("${rabbitmq.routing.product-rules:product.rules.*}")
    private String routingKey;

    @Value("${rabbitmq.exchange.product-rules-dlx:product-rules.dlx}")
    private String deadLetterExchange;

    @Value("${rabbitmq.routing.product-rules-dlq:product.rules.dlq}")
    private String deadLetterRoutingKey;
    
    public ProductRuleEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Publica evento de creación de regla de producto.
     */
    public void publishProductRuleCreated(ProductRuleEvent event) {
        publishEvent(event);
        log.info("event=product_rule_created_published ruleUid={} ecommerceId={} productType={}",
            event.uid(), event.ecommerceId(), event.productType());
    }
    
    /**
     * Publica evento de actualización de regla de producto.
     */
    public void publishProductRuleUpdated(ProductRuleEvent event) {
        publishEvent(event);
        log.info("event=product_rule_updated_published ruleUid={} ecommerceId={} productType={}",
            event.uid(), event.ecommerceId(), event.productType());
    }
    
    /**
     * Publica evento de eliminación de regla de producto.
     */
    public void publishProductRuleDeleted(ProductRuleEvent event) {
        publishEvent(event);
        log.info("event=product_rule_deleted_published ruleUid={} ecommerceId={} productType={}",
            event.uid(), event.ecommerceId(), event.productType());
    }
    
    /**
     * Publica un evento en el exchange de reglas de producto.
     */
    private void publishEvent(ProductRuleEvent event) {
        String eventId = event.eventType() + ":" + event.uid() + ":" + event.timestamp();
        try {
            String payload = objectMapper.writeValueAsString(event);
            String eventRoutingKey = buildRoutingKey(event.eventType());
            rabbitTemplate.convertAndSend(exchangeName, eventRoutingKey, payload, message -> withHeaders(message, event, eventId));
            log.info("event=product_rule_event_published eventId={} type={} ruleUid={} ecommerceId={} exchange={} routingKey={}",
                    eventId, event.eventType(), event.uid(), event.ecommerceId(), exchangeName, eventRoutingKey);
        } catch (Exception e) {
            log.error("event=product_rule_event_publish_failed eventId={} type={} ruleUid={} ecommerceId={}",
                    eventId, event.eventType(), event.uid(), event.ecommerceId(), e);
            try {
                rabbitTemplate.convertAndSend(deadLetterExchange, deadLetterRoutingKey, objectMapper.writeValueAsString(event),
                        message -> withHeaders(message, event, eventId));
            } catch (Exception dlxException) {
                log.error("event=product_rule_dlx_publish_failed eventId={}", eventId, dlxException);
            }
        }
    }

    private Message withHeaders(Message message, ProductRuleEvent event, String eventId) {
        message.getMessageProperties().setHeader("x-event-id", eventId);
        message.getMessageProperties().setHeader("x-event-type", event.eventType());
        message.getMessageProperties().setHeader("x-rule-uid", event.uid().toString());
        message.getMessageProperties().setHeader("x-ecommerce-id", event.ecommerceId().toString());
        message.getMessageProperties().setHeader("x-product-type", event.productType());
        return message;
    }

    private String buildRoutingKey(String eventType) {
        return "product.rules." + eventType.toLowerCase();
    }
}
