package com.loyalty.service_admin.infrastructure.exception;

/**
 * Excepción cuando no se encuentra un recurso.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
