package com.loyalty.service_admin.infrastructure.exception;

/**
 * Excepción cuando no se encuentra una API Key.
 */
public class ApiKeyNotFoundException extends RuntimeException {
    public ApiKeyNotFoundException(String message) {
        super(message);
    }
    
    public ApiKeyNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
