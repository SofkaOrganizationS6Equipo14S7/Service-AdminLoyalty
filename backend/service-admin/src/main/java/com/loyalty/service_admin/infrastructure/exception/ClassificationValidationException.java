package com.loyalty.service_admin.infrastructure.exception;

/**
 * Excepción lanzada cuando los datos de clasificación de clientes son inválidos.
 * Se utiliza cuando el payload contiene campos obligatorios faltantes o valores inválidos.
 * Corresponde al código HTTP 400 Bad Request.
 */
public class ClassificationValidationException extends BadRequestException {
    
    public ClassificationValidationException(String message) {
        super(message);
    }
    
    public ClassificationValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
