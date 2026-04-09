package com.loyalty.service_admin.infrastructure.exception;

/**
 * Excepción lanzada cuando el usuario autenticado no tiene permiso para la acción.
 * Corresponde al código HTTP 403 Forbidden.
 * 
 * Diferencia:
 * - UnauthorizedException (401): El usuario no está autenticado o el token es inválido
 * - AuthorizationException (403): El usuario está autenticado pero no tiene permiso
 */
public class AuthorizationException extends RuntimeException {
    
    public AuthorizationException(String message) {
        super(message);
    }
    
    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
