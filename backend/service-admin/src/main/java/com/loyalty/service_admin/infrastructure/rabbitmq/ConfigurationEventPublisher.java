package com.loyalty.service_admin.infrastructure.rabbitmq;

import com.loyalty.service_admin.application.dto.configuration.ConfigurationUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConfigurationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.config:loyalty.config.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing.config-updated:config.updated}")
    private String routingKey;

    @Value("${rabbitmq.exchange.config-dlx:loyalty.config.dlx}")
    private String deadLetterExchange;

    @Value("${rabbitmq.routing.config-updated-dlq:config.updated.dlq}")
    private String deadLetterRoutingKey;

    public void publishConfigUpdated(ConfigurationUpdatedEvent event) {
        String eventId = event.configId() + ":" + event.version();
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event, message -> withStandardHeaders(message, event, eventId));
            log.info("event=config_updated_published eventId={} ecommerceId={} version={} exchange={} routingKey={}",
                    eventId, event.ecommerceId(), event.version(), exchange, routingKey);
        } catch (Exception ex) {
            log.error("event=config_updated_publish_failed eventId={} ecommerceId={} version={} exchange={} routingKey={}",
                    eventId, event.ecommerceId(), event.version(), exchange, routingKey, ex);
            rabbitTemplate.convertAndSend(deadLetterExchange, deadLetterRoutingKey, event,
                    message -> withStandardHeaders(message, event, eventId));
        }
    }

    private Message withStandardHeaders(Message message, ConfigurationUpdatedEvent event, String eventId) {
        message.getMessageProperties().setHeader("x-event-id", eventId);
        message.getMessageProperties().setHeader("x-event-type", event.eventType());
        message.getMessageProperties().setHeader("x-ecommerce-id", event.ecommerceId().toString());
        message.getMessageProperties().setHeader("x-version", event.version());
        return message;
    }
}
