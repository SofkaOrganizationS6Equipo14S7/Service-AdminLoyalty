package com.loyalty.service_admin.infrastructure.exception;

/**
 * Excepción cuando no se encuentra un recurso (usuario, api key, etc.).
 * Corresponde al código HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
