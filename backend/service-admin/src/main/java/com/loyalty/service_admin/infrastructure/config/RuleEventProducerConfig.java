package com.loyalty.service_admin.infrastructure.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for generic rule events (SPEC-010).
 * Publishes RuleEvent for all discount types: FIDELITY, SEASONAL, PRODUCT, CLASSIFICATION.
 *
 * Producer Side (Admin Service):
 * - Publishes to exchange: loyalty.events
 * - Routing key: rule.*  (universal for all rule types)
 *
 * Consumer Side (Engine Service):
 * - Receives on queue: loyalty.rules.queue
 * - Dead Letter Queue: loyalty.dlq for permanent failures
 */
@Configuration
public class RuleEventProducerConfig {

    @Value("${rabbitmq.exchange.events:loyalty.events}")
    private String eventsExchange;

    @Value("${rabbitmq.exchange.events-dlx:loyalty.events.dlx}")
    private String eventsDlxExchange;

    @Value("${rabbitmq.queue.rules:loyalty.rules.queue}")
    private String rulesQueue;

    @Value("${rabbitmq.queue.rules-dlq:loyalty.dlq}")
    private String rulesDlq;

    @Value("${rabbitmq.routing.rules:rule.*}")
    private String rulesRoutingKey;

    /**
     * Main Topic Exchange for all event types.
     * Shared with other consumers (Classification, CustomerTier, etc.)
     */
    @Bean(name = "eventsExchangeRule")
    public TopicExchange eventsExchange() {
        return new TopicExchange(eventsExchange, true, false);
    }

    /**
     * Dead Letter Exchange for permanently failed events.
     */
    @Bean(name = "eventsDlxExchangeRule")
    public DirectExchange eventsDlxExchange() {
        return new DirectExchange(eventsDlxExchange, true, false);
    }

    /**
     * Main queue for rule events (consumed by Engine Service).
     * Routes to engine.RuleEventConsumer via @RabbitListener.
     */
    @Bean(name = "rulesQueue")
    public Queue rulesQueue() {
        return QueueBuilder
            .durable(rulesQueue)
            .withArguments(java.util.Map.of(
                "x-dead-letter-exchange", eventsDlxExchange,
                "x-dead-letter-routing-key", "rule.dlq"
            ))
            .build();
    }

    /**
     * Dead Letter Queue for permanently failed rule events.
     * Manual inspection and reprocessing.
     */
    @Bean(name = "rulesDlq")
    public Queue rulesDlq() {
        return QueueBuilder.durable(rulesDlq).build();
    }

    /**
     * Binding: rulesQueue ← eventsExchange with routing key rule.*
     */
    @Bean
    public Binding rulesBinding(Queue rulesQueue, TopicExchange eventsExchange) {
        return BindingBuilder
            .bind(rulesQueue)
            .to(eventsExchange)
            .with(rulesRoutingKey);
    }

    /**
     * Binding: rulesDlq ← eventsDlxExchange
     * Uses @Qualifier to explicitly select the correct DirectExchange bean
     * when multiple DirectExchange beans exist in the context.
     */
    @Bean
    public Binding rulesDlqBinding(
            Queue rulesDlq,
            @Qualifier("eventsDlxExchangeRule") DirectExchange eventsDlxExchange) {
        return BindingBuilder
            .bind(rulesDlq)
            .to(eventsDlxExchange)
            .with("rule.dlq");
    }
}
