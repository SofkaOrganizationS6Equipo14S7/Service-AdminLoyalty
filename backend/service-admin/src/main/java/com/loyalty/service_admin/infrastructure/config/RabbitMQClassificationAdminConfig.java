package com.loyalty.service_admin.infrastructure.config;

import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration for Admin Service.
 *
 * Role: Declares the Fanout Exchange for classification rule events.
 * This is the "source" side — Admin publishes to this exchange.
 *
 * Events broadcast to all consumers via Fanout topology:
 * - classification.rule.created
 * - classification.rule.updated
 * - classification.rule.deleted
 */
@Configuration
public class RabbitMQClassificationAdminConfig {

    @Value("${rabbitmq.exchange.classification:classification.exchange}")
    private String exchangeName;

    @Bean(name = "classificationExchange")
    public Exchange classificationExchange() {
        return ExchangeBuilder
            .fanoutExchange(exchangeName)
            .durable(true)
            .build();
    }
}
