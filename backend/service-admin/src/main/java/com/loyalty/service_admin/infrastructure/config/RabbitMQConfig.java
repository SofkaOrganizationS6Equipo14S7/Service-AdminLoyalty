package com.loyalty.service_admin.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
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
        return template;
    }

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(5);
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(500);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(5000);

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
