package com.loyalty.service_admin.infrastructure.rabbitmq;

import com.loyalty.service_admin.application.dto.configuration.ConfigurationUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
        } catch (Exception ex) {
            log.error("Failed publishing CONFIG_UPDATED. Sending to DLQ exchange. ecommerce={} version={}",
                    event.ecommerceId(), event.version(), ex);
            rabbitTemplate.convertAndSend(deadLetterExchange, deadLetterRoutingKey, event);
        }
    }
}
