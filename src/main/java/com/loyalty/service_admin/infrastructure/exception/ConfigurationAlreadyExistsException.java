package com.loyalty.service_admin.infrastructure.exception;

public class ConfigurationAlreadyExistsException extends RuntimeException {
    public ConfigurationAlreadyExistsException(String message) {
        super(message);
    }
}
