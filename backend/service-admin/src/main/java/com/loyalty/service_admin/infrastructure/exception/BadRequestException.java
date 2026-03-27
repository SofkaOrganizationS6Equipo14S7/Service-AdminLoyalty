package com.loyalty.service_admin.infrastructure.exception;

/**
 * Excepción lanzada cuando los datos de la solicitud son inválidos.
 * Corresponde al código HTTP 400 Bad Request.
 */
public class BadRequestException extends RuntimeException {
    
    public BadRequestException(String message) {
        super(message);
    }
    
    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
