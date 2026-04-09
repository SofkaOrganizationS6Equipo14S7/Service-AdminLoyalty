package com.loyalty.service_admin.domain.model;

/**
 * Tipos de descuento disponibles en el sistema.
 * Utilizados para priorización en HU-09.
 */
public enum DiscountType {
    FIDELITY("Descuento por fidelización"),
    SEASONAL("Descuento estacional"),
    PROMOTIONAL("Descuento promocional");

    private final String description;

    DiscountType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
