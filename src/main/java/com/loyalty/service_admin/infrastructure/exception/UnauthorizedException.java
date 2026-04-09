package com.loyalty.service_admin.infrastructure.exception;

/**
 * Excepción lanzada cuando falla la autenticación o autorización.
 * Corresponde al código HTTP 401 Unauthorized.
 */
public class UnauthorizedException extends RuntimeException {
    
    public UnauthorizedException(String message) {
        super(message);
    }
    
    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
