package com.loyalty.service_admin.domain.model.ecommerce;

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
