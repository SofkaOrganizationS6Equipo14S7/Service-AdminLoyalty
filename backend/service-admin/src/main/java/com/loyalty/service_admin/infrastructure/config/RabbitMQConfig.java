package com.loyalty.service_admin.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Map;

@Configuration
@Slf4j
public class RabbitMQConfig {
    
    @Value("${rabbitmq.exchange.config:loyalty.config.exchange}")
    private String configExchange;

    @Value("${rabbitmq.exchange.config-dlx:loyalty.config.dlx}")
    private String configDeadLetterExchange;

    @Value("${rabbitmq.queue.config-updated:loyalty.config.updated.queue}")
    private String configUpdatedQueue;

    @Value("${rabbitmq.queue.config-updated-dlq:loyalty.config.updated.dlq}")
    private String configUpdatedDlq;

    @Value("${rabbitmq.routing.config-updated:config.updated}")
    private String configUpdatedRoutingKey;

    @Value("${rabbitmq.routing.config-updated-dlq:config.updated.dlq}")
    private String configUpdatedDlqRoutingKey;

    // Seasonal Rules exchange and queue
    @Value("${rabbitmq.exchange.seasonal:loyalty.seasonal.exchange}")
    private String seasonalExchange;

    @Value("${rabbitmq.exchange.seasonal-dlx:loyalty.seasonal.dlx}")
    private String seasonalDeadLetterExchange;

    @Value("${rabbitmq.queue.seasonal-rules:loyalty.seasonal.rules.queue}")
    private String seasonalRulesQueue;

    @Value("${rabbitmq.queue.seasonal-rules-dlq:loyalty.seasonal.rules.dlq}")
    private String seasonalRulesDlq;

    @Value("${rabbitmq.routing.seasonal-created:seasonal.rule.created}")
    private String seasonalCreatedRoutingKey;

    @Value("${rabbitmq.routing.seasonal-updated:seasonal.rule.updated}")
    private String seasonalUpdatedRoutingKey;

    @Value("${rabbitmq.routing.seasonal-deleted:seasonal.rule.deleted}")
    private String seasonalDeletedRoutingKey;

    @Value("${rabbitmq.routing.seasonal-dlq:seasonal.rule.dlq}")
    private String seasonalDlqRoutingKey;

    @Value("${rabbitmq.retry.max-attempts:5}")
    private int maxAttempts;

    @Value("${rabbitmq.retry.initial-interval-ms:500}")
    private long initialIntervalMs;

    @Value("${rabbitmq.retry.multiplier:2.0}")
    private double retryMultiplier;

    @Value("${rabbitmq.retry.max-interval-ms:5000}")
    private long maxIntervalMs;
    
    /**
     * Fanout Exchange para propagación de cambios de configuración.
     * Admin Service publica eventos aquí, Engine Service consume.
     */
    @Bean
    public DirectExchange configExchange() {
        return new DirectExchange(configExchange, true, false);
    }

    @Bean
    public DirectExchange configDeadLetterExchange() {
        return new DirectExchange(configDeadLetterExchange, true, false);
    }

    @Bean
    public Queue configUpdatedQueue() {
        return new Queue(configUpdatedQueue, true, false, false, Map.of(
                "x-dead-letter-exchange", configDeadLetterExchange,
                "x-dead-letter-routing-key", configUpdatedDlqRoutingKey
        ));
    }

    @Bean
    public Queue configUpdatedDlq() {
        return new Queue(configUpdatedDlq, true);
    }

    @Bean
    public Binding configUpdatedBinding(DirectExchange configExchange, Queue configUpdatedQueue) {
        return BindingBuilder.bind(configUpdatedQueue).to(configExchange).with(configUpdatedRoutingKey);
    }

    @Bean
    public Binding configUpdatedDlqBinding(DirectExchange configDeadLetterExchange, Queue configUpdatedDlq) {
        return BindingBuilder.bind(configUpdatedDlq).to(configDeadLetterExchange).with(configUpdatedDlqRoutingKey);
    }
    
    // ============================================================================
    // Seasonal Rules Exchange, Queues, and Bindings
    // ============================================================================
    
    /**
     * Fanout Exchange for seasonal rule events
     * Admin Service publishes: CREATED, UPDATED, DELETED events
     * Engine Service consumes from this exchange
     */
    @Bean
    public FanoutExchange seasonalExchange() {
        return new FanoutExchange(seasonalExchange, true, false);
    }

    @Bean
    public DirectExchange seasonalDeadLetterExchange() {
        return new DirectExchange(seasonalDeadLetterExchange, true, false);
    }

    /**
     * Queue for seasonal rules events (consumed by Service-Engine)
     * Dead Letter: seasonal-rules-dlq with DLX configured
     */
    @Bean
    public Queue seasonalRulesQueue() {
        return new Queue(seasonalRulesQueue, true, false, false, Map.of(
                "x-dead-letter-exchange", seasonalDeadLetterExchange,
                "x-dead-letter-routing-key", seasonalDlqRoutingKey
        ));
    }

    @Bean
    public Queue seasonalRulesDlq() {
        return new Queue(seasonalRulesDlq, true);
    }

    /**
     * Bindings for seasonal rule events (Fanout - no routing keys needed)
     */
    @Bean
    public Binding seasonalCreatedBinding(FanoutExchange seasonalExchange, Queue seasonalRulesQueue) {
        return BindingBuilder.bind(seasonalRulesQueue).to(seasonalExchange);
    }

    @Bean
    public Binding seasonalUpdatedBinding(FanoutExchange seasonalExchange, Queue seasonalRulesQueue) {
        return BindingBuilder.bind(seasonalRulesQueue).to(seasonalExchange);
    }

    @Bean
    public Binding seasonalDeletedBinding(FanoutExchange seasonalExchange, Queue seasonalRulesQueue) {
        return BindingBuilder.bind(seasonalRulesQueue).to(seasonalExchange);
    }

    @Bean
    public Binding seasonalDlqBinding(DirectExchange seasonalDeadLetterExchange, Queue seasonalRulesDlq) {
        return BindingBuilder.bind(seasonalRulesDlq).to(seasonalDeadLetterExchange).with(seasonalDlqRoutingKey);
    }
    
    /**
     * Converter JSON para mensajes.
     */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter();
    }
    
    /**
     * RabbitTemplate para enviar mensajes.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setRetryTemplate(retryTemplate());
        template.setMandatory(true);
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("event=rabbit_publish_nack correlationId={} cause={}",
                        correlationData != null ? correlationData.getId() : "n/a",
                        cause);
            }
        });
        template.setReturnsCallback(returned -> log.error(
                "event=rabbit_returned exchange={} routingKey={} replyCode={} replyText={}",
                returned.getExchange(),
                returned.getRoutingKey(),
                returned.getReplyCode(),
                returned.getReplyText()
        ));
        return template;
    }

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(maxAttempts);
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(initialIntervalMs);
        backOffPolicy.setMultiplier(retryMultiplier);
        backOffPolicy.setMaxInterval(maxIntervalMs);

        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        return retryTemplate;
    }
    
    /**
     * ObjectMapper para serialización/deserialización JSON.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
