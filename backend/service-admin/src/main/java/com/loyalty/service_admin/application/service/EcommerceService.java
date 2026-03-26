package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.infrastructure.exception.EcommerceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Servicio para validaciones de ecommerce.
 * Nota: En una implementación real, esto consultaría una tabla ecommerces.
 */
@Service
public class EcommerceService {
    
    /**
     * Valida que un ecommerce existe.
     * Por ahora, acepta cualquier UUID válido.
     * En producción, consultar tabla ecommerces.
     * @throws EcommerceNotFoundException si no existe
     */
    public void validateEcommerceExists(UUID ecommerceId) {
        // En una implementación real:
        // Optional<Ecommerce> ecommerce = ecommerceRepository.findById(ecommerceId);
        // if (ecommerce.isEmpty()) throw new EcommerceNotFoundException(...);
        
        // Para este sprint, asumimos que cualquier UUID válido es un ecommerce válido
        if (ecommerceId == null) {
            throw new EcommerceNotFoundException("Ecommerce ID no puede ser null");
        }
    }
}
