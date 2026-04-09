package com.loyalty.service_admin.infrastructure.exception;

/**
 * Excepción lanzada cuando hay un conflicto en los datos (ej: unicidad).
 * Corresponde al código HTTP 409 Conflict.
 */
public class ConflictException extends RuntimeException {
    
    public ConflictException(String message) {
        super(message);
    }
    
    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
