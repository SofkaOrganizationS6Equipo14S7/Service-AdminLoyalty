package com.loyalty.service_admin.application.validation;

import com.loyalty.service_admin.domain.entity.FidelityRangeEntity;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Validator para detectar rangos superpuestos en fidelidad
 * 
 * Regla: Dos rangos [min1, max1] y [min2, max2] se solapan si:
 *   min1 <= max2 AND max1 >= min2
 */
@Component
public class FidelityRangeOverlapValidator {

    /**
     * Verificar que el nuevo rango no se superpone con ninguno existente
     * 
     * @param newMinPoints mínimo del nuevo rango
     * @param newMaxPoints máximo del nuevo rango
     * @param existingRanges rangos existentes activos (sin incluir el que se está actualizando)
     * @throws BadRequestException si existe solapamiento
     */
    public void validateNoOverlap(Integer newMinPoints, Integer newMaxPoints, List<FidelityRangeEntity> existingRanges) {
        if (newMinPoints == null || newMaxPoints == null || existingRanges == null) {
            return;
        }

        for (FidelityRangeEntity range : existingRanges) {
            // Condición de solapamiento: min1 <= max2 AND max1 >= min2
            if (newMinPoints <= range.getMaxPoints() && newMaxPoints >= range.getMinPoints()) {
                throw new BadRequestException(
                    String.format("El rango [%d-%d] se superpone con el nivel existente '%s' [%d-%d]",
                        newMinPoints, newMaxPoints, range.getName(),
                        range.getMinPoints(), range.getMaxPoints())
                );
            }
        }
    }

    /**
     * Validar que minPoints < maxPoints
     * 
     * @param minPoints mínimo del rango
     * @param maxPoints máximo del rango
     * @throws BadRequestException si minPoints >= maxPoints
     */
    public void validateMinMaxOrder(Integer minPoints, Integer maxPoints) {
        if (minPoints != null && maxPoints != null && minPoints >= maxPoints) {
            throw new BadRequestException(
                String.format("El rango mínimo (%d) debe ser menor que el máximo (%d)",
                    minPoints, maxPoints)
            );
        }
    }
}
