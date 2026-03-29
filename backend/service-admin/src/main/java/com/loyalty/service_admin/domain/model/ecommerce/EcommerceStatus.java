package com.loyalty.service_admin.domain.model.ecommerce;

/**
 * Estados posibles de un ecommerce.
 * 
 * SPEC-001: Registro y Gestión de Ecommerces
 * - ACTIVE: Ecommerce operando normalmente
 * - INACTIVE: Ecommerce desactivado (soft delete)
 */
public enum EcommerceStatus {
    ACTIVE("Ecommerce activo y operando"),
    INACTIVE("Ecommerce desactivado, usuarios sin acceso");
    
    private final String description;
    
    EcommerceStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
