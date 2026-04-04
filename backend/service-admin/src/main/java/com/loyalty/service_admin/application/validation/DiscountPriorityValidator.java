package com.loyalty.service_admin.application.validation;

import com.loyalty.service_admin.application.dto.rules.discount.DiscountLimitPriorityRequest;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validador para asegurar que las prioridades de descuentos son válidas.
 * 
 * Reglas:
 * - Los niveles deben ser secuenciales comenzando en 1 (1, 2, 3, ..., N)
 * - No pueden haber duplicados
 * - No pueden haber huecos
 */
@Component
public class DiscountPriorityValidator {

    /**
     * Valida que las prioridades cumplan con las reglas de negocio.
     * 
     * @param request DTO con las prioridades a validar
     * @throws BadRequestException si las prioridades no son válidas
     */
    public void validatePriorities(DiscountLimitPriorityRequest request) {
        if (request.priorities() == null || request.priorities().isEmpty()) {
            throw new BadRequestException("Las prioridades no pueden estar vacías");
        }

        List<Integer> levels = request.priorities()
            .stream()
            .map(DiscountLimitPriorityRequest.PriorityEntry::priorityLevel)
            .toList();

        // Verificar duplicados
        Set<Integer> uniqueLevels = new HashSet<>(levels);
        if (uniqueLevels.size() != levels.size()) {
            throw new BadRequestException("Las prioridades deben ser secuenciales comenzando en 1 sin duplicados");
        }

        // Verificar que comienzan en 1 y son secuenciales
        int expectedMin = 1;
        int expectedMax = levels.size();

        for (int i = expectedMin; i <= expectedMax; i++) {
            if (!uniqueLevels.contains(i)) {
                throw new BadRequestException("Las prioridades deben ser secuenciales comenzando en 1 sin duplicados");
            }
        }

        // Verificar que cada tipo de descuento sea único
        Set<String> uniqueTypes = new HashSet<>();
        for (DiscountLimitPriorityRequest.PriorityEntry entry : request.priorities()) {
            if (!uniqueTypes.add(entry.discountType())) {
                throw new BadRequestException("No pueden haber tipos de descuento duplicados");
            }
        }
    }
}
