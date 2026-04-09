package com.loyalty.service_admin.infrastructure.exception;

/**
 * Excepción cuando no se encuentra un ecommerce.
 */
public class EcommerceNotFoundException extends RuntimeException {
    public EcommerceNotFoundException(String message) {
        super(message);
    }
    
    public EcommerceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
