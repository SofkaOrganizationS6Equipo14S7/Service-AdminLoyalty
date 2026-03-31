package com.loyalty.service_engine.application.service;

import com.loyalty.service_engine.domain.entity.DiscountConfigEntity;
import com.loyalty.service_engine.domain.entity.DiscountPriorityEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio INTERNO de cálculo de límite de descuentos (HU-09).
 * 
 * IMPORTANTE: Este NO es un servicio HTTP. Es invocado internamente por:
 * - SPEC-011 (engine-calculate) durante cálculo de transacción
 * - Otros servicios internos del Engine
 * 
 * Responsabilidades:
 * 1. Obtener config vigente y prioridades de descuentos
 * 2. Aplicar algoritmo greedy: tipos en orden de prioridad (1=máxima)
 * 3. Acumular montos hasta alcanzar maxDiscountLimit
 * 4. Truncar descuentos que excedan el límite
 * 5. Retornar breakdown con limitExceeded flag
 * 
 * Ejemplo:
 * - Config: maxLimit = 100.00 COP
 * - Prioridades: FIDELITY=1, SEASONAL=2, PROMOTIONAL=3
 * - Descuentos elegibles: FIDELITY=50, SEASONAL=40, PROMOTIONAL=30
 * - Aplicación: FIDELITY=50 + SEASONAL=40 + PROMOTIONAL=10 = 100
 * - Resultado: limitExceeded=true, truncations=[PROMOTIONAL reducido de 30 a 10]
 */
@Service
@Slf4j
public class DiscountLimitService {
    
    private final DiscountConfigService discountConfigService;
    private final DiscountPriorityService priorityService;
    
    public DiscountLimitService(
        DiscountConfigService discountConfigService,
        DiscountPriorityService priorityService
    ) {
        this.discountConfigService = discountConfigService;
        this.priorityService = priorityService;
    }
    
    /**
     * Aplica el límite de descuentos usando algoritmo greedy por prioridad.
     * 
     * Flujo:
     * 1. Obtiene config activa para ecommerce
     * 2. Obtiene prioridades ordenadas (1..N)
     * 3. Ordena descuentos por priorityLevel ASC
     * 4. Acumula hasta maxDiscountLimit, trunca los que excedan
     * 5. Retorna breakdown con flags
     * 
     * @param ecommerceId UUID del ecommerce
     * @param discountsInput Lista de descuentos elegibles por tipo (ej: FIDELITY=50.00, SEASONAL=40.00)
     * @return DiscountLimitBreakdown con original vs applied + limitExceeded flag
     * @throws IllegalArgumentException si config o descuentos son inválidos
     */
    @Transactional(readOnly = true)
    public DiscountLimitBreakdown applyDiscountLimit(
        String ecommerceId,
        List<DiscountInput> discountsInput
    ) {
        log.debug("Applying discount limit for ecommerce: {}. Input discounts count: {}", 
            ecommerceId, discountsInput.size());
        
        // 1. Obtener config activa
        DiscountConfigEntity config = discountConfigService.getActiveConfigEntityByEcommerce(
            UUID.fromString(ecommerceId)
        );
        
        if (config == null) {
            log.warn("No active discount config for ecommerce: {}. Approving all discounts (no limit)", ecommerceId);
            // Si no hay config, pasar todos los descuentos
            return buildBreakdownNoConfig(discountsInput);
        }
        
        // 2. Obtener prioridades ordenadas
        List<DiscountPriorityEntity> priorities = priorityService.getPrioritiesEntityByConfigId(config.getUid());
        
        if (priorities.isEmpty()) {
            log.warn("No discount priorities configured for config: {}. Approving all discounts", config.getUid());
            return buildBreakdownNoConfig(discountsInput);
        }
        
        // 3. Crear mapa de prioridades por tipo
        Map<String, Integer> priorityMap = priorities.stream()
            .collect(Collectors.toMap(DiscountPriorityEntity::getDiscountType, DiscountPriorityEntity::getPriorityLevel));
        
        // 4. Filtrar y ordenar descuentos por prioridad
        List<DiscountInput> sortedDiscounts = discountsInput.stream()
            .filter(d -> priorityMap.containsKey(d.discountType))  // Solo tipos configurados
            .sorted(Comparator.comparingInt(d -> priorityMap.getOrDefault(d.discountType, Integer.MAX_VALUE)))
            .collect(Collectors.toList());
        
        if (sortedDiscounts.isEmpty()) {
            log.debug("No configured discount types in input. Approving all (edge case)");
            return buildBreakdownNoConfig(discountsInput);
        }
        
        // 5. Aplicar greedy algorithm: acumular hasta maxLimit
        BigDecimal maxLimit = config.getMaxDiscountLimit();
        BigDecimal accumulated = BigDecimal.ZERO;
        List<DiscountApplied> applied = new ArrayList<>();
        boolean limitExceeded = false;
        
        for (DiscountInput discount : sortedDiscounts) {
            BigDecimal amount = discount.amount;
            BigDecimal remaining = maxLimit.subtract(accumulated);
            
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                // Límite agotado, truncar
                applied.add(new DiscountApplied(discount.discountType, BigDecimal.ZERO, amount));
                limitExceeded = true;
                log.debug("Limit reached. Truncating {} amount {}", discount.discountType, amount);
            } else if (amount.compareTo(remaining) <= 0) {
                // Aplica completamente
                applied.add(new DiscountApplied(discount.discountType, amount, BigDecimal.ZERO));
                accumulated = accumulated.add(amount);
            } else {
                // Aplica parcialmente
                BigDecimal applied_amt = remaining;
                BigDecimal truncated_amt = amount.subtract(applied_amt);
                applied.add(new DiscountApplied(discount.discountType, applied_amt, truncated_amt));
                accumulated = maxLimit;
                limitExceeded = true;
                log.debug("Partial truncation: {} applied {}, truncated {}", 
                    discount.discountType, applied_amt, truncated_amt);
            }
        }
        
        BigDecimal totalOriginal = discountsInput.stream()
            .map(d -> d.amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalApplied = applied.stream()
            .map(a -> a.appliedAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        log.info("Discount limit applied. Total original: {}, Total applied: {}, Limited: {}, Config: {} {}",
            totalOriginal, totalApplied, limitExceeded, maxLimit, config.getCurrencyCode());
        
        return new DiscountLimitBreakdown(
            config.getUid().toString(),
            config.getMaxDiscountLimit(),
            config.getCurrencyCode(),
            totalOriginal,
            totalApplied,
            limitExceeded,
            applied
        );
    }
    
    /**
     * Crea breakdown cuando no hay config (grace fallback)
     */
    private DiscountLimitBreakdown buildBreakdownNoConfig(List<DiscountInput> inputs) {
        BigDecimal total = inputs.stream()
            .map(d -> d.amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        List<DiscountApplied> applied = inputs.stream()
            .map(d -> new DiscountApplied(d.discountType, d.amount, BigDecimal.ZERO))
            .collect(Collectors.toList());
        
        return new DiscountLimitBreakdown(
            null,  // No config
            null,
            null,
            total,
            total,
            false,  // No limit exceeded (no limit configured)
            applied
        );
    }
    
    /**
     * Input: descuento elegible con tipo y monto
     */
    public record DiscountInput(
        String discountType,      // FIDELITY, SEASONAL, PROMOTIONAL, etc
        BigDecimal amount         // Monto elegible (ej 50.00)
    ) {}
    
    /**
     * Output después de aplicar límite: desglose por tipo
     */
    public record DiscountApplied(
        String discountType,      // FIDELITY, SEASONAL, PROMOTIONAL
        BigDecimal appliedAmount, // Monto efectivamente aplicado
        BigDecimal truncatedAmount // Monto truncado por límite (=0 si sin truncamiento)
    ) {}
    
    /**
     * Response del servicio: desglose completo de límite
     */
    public record DiscountLimitBreakdown(
        String configUid,                    // UUID de config (null si sin config)
        BigDecimal configuredLimit,          // Límite máximo configurado
        String currencyCode,                 // Moneda (ej COP)
        BigDecimal totalOriginalAmount,      // Suma de todos los descuentos elegibles
        BigDecimal totalAppliedAmount,       // Suma de descuentos efectivamente aplicados
        Boolean limitExceeded,               // true si totalOriginal > configuredLimit
        List<DiscountApplied> breakdown      // Desglose por tipo
    ) {}
}
