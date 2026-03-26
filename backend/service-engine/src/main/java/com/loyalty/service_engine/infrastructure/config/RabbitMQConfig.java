package com.loyalty.service_engine.infrastructure.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class RabbitMQConfig {
    
    @Value("${rabbitmq.exchange.config:loyalty.config.exchange}")
    private String configExchange;
    
    @Value("${rabbitmq.queue.api-keys:engine-api-keys-queue}")
    private String apiKeysQueue;
    
    /**
     * Queue para consumir eventos de API Key desde Admin Service.
     */
    @Bean
    public Queue apiKeysQueue() {
        return new Queue(apiKeysQueue, true, false, false);
    }
    
    /**
     * Fanout Exchange para recibir eventos de configuración desde Admin.
     */
    @Bean
    public FanoutExchange configExchange() {
        return new FanoutExchange(configExchange, true, false);
    }
    
    /**
     * Binding: API Keys Queue → Config Exchange
     */
    @Bean
    public Binding apiKeysBinding(Queue apiKeysQueue, FanoutExchange configExchange) {
        return BindingBuilder.bind(apiKeysQueue)
            .to(configExchange);
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
        return template;
    }
    
    /**
     * ObjectMapper para serialización/deserialización JSON.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
