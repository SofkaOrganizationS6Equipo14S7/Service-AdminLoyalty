package com.loyalty.service_admin.application.validation;

import com.loyalty.service_admin.domain.entity.FidelityRangeEntity;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Validator para asegurar que los rangos de fidelidad tienen progresión jerárquica
 * 
 * Regla: Los minPoints deben ser ascendentes entre los rangos existentes
 * Ej: Si existen rangos con minPoints [0, 1000, 5000], el nuevo debe mantener este orden
 */
@Component
public class FidelityRangeProgressionValidator {

    /**
     * Validar que minPoints respeta la progresión ascendente
     * 
     * @param newMinPoints minPoints del nuevo rango
     * @param existingRanges rangos existentes (activos, ordenados por minPoints ascendente)
     * @throws BadRequestException si la progresión es inválida
     */
    public void validateProgression(Integer newMinPoints, List<FidelityRangeEntity> existingRanges) {
        if (newMinPoints == null || existingRanges == null || existingRanges.isEmpty()) {
            return;
        }

        // Los rangos ya están ordenados por minPoints
        // Solo verifica que el nuevo minPoints no viole la progresión
        // (esto se asegura mediante noOverlap, pero agregamos una verificación adicional)

        for (FidelityRangeEntity range : existingRanges) {
            if (newMinPoints.equals(range.getMinPoints())) {
                throw new BadRequestException(
                    String.format("Ya existe un rango con minPoints = %d", newMinPoints)
                );
            }
        }
    }
}
