package com.loyalty.service_admin.infrastructure.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

/**
 * Handler global de excepciones.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * Maneja cuando no se encuentra una API Key.
     */
    @ExceptionHandler(ApiKeyNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleApiKeyNotFound(ApiKeyNotFoundException e) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "timestamp", Instant.now(),
                "status", HttpStatus.NOT_FOUND.value(),
                "error", "Not Found",
                "message", e.getMessage()
            ));
    }
    
    /**
     * Maneja cuando no se encuentra un ecommerce.
     */
    @ExceptionHandler(EcommerceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEcommerceNotFound(EcommerceNotFoundException e) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "timestamp", Instant.now(),
                "status", HttpStatus.NOT_FOUND.value(),
                "error", "Not Found",
                "message", e.getMessage()
            ));
    }
    
    /**
     * Manejador genérico para cualquier excepción.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of(
                "timestamp", Instant.now(),
                "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "error", "Internal Server Error",
                "message", e.getMessage()
            ));
    }
}
