package com.loyalty.service_engine.application.service;

import com.loyalty.service_engine.application.dto.ClassificationResult;
import com.loyalty.service_engine.application.dto.DiscountCalculateRequest;
import com.loyalty.service_engine.application.dto.DiscountCalculateResponse;
import com.loyalty.service_engine.application.dto.FidelityRangeDTO;
import com.loyalty.service_engine.domain.entity.DiscountConfigEntity;
import com.loyalty.service_engine.domain.entity.DiscountPriorityEntity;
import com.loyalty.service_engine.infrastructure.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Motor crítico para calcular descuentos respetando prioridad y límite máximo.
 * Incluye clasificación automática de fidelidad del cliente.
 * Garantiza que el descuento total nunca supere el límite configurado.
 * Utiliza caché en memoria (Caffeine) para acceso rápido a configuración.
 */
@Service
@Slf4j
public class DiscountCalculationEngine {
    
    private final DiscountConfigService discountConfigService;
    private final DiscountPriorityService discountPriorityService;
    private final FidelityClassificationService fidelityClassificationService;
    
    public DiscountCalculationEngine(
        DiscountConfigService discountConfigService,
        DiscountPriorityService discountPriorityService,
        FidelityClassificationService fidelityClassificationService
    ) {
        this.discountConfigService = discountConfigService;
        this.discountPriorityService = discountPriorityService;
        this.fidelityClassificationService = fidelityClassificationService;
    }
    
    /**
     * Calcula el total de descuentos respetando prioridad y límite máximo.
     * 
     * Algoritmo:
     * 1. Obtiene configuración vigente (límite + prioridad)
     * 2. Ordena descuentos por prioridad (1 = máxima)
     * 3. Acumula descuentos en orden hasta alcanzar el límite
     * 4. Retorna desglose de descuentos aplicados vs omitidos
     * 
     * @param request Datos de transacción y descuentos a aplicar
     * @return DiscountCalculateResponse con cálculo final
     * @throws ResourceNotFoundException si no existe configuración
     */
    public DiscountCalculateResponse calculateDiscounts(DiscountCalculateRequest request) {
        log.debug("Calculating discounts for transaction: {}", request.transactionId());
        
        // Obtener configuración vigente desde caché o BD
        DiscountConfigEntity config = discountConfigService.getActiveConfigEntity();
        if (config == null) {
            log.warn("No active discount config found for transaction: {}", request.transactionId());
            throw new ResourceNotFoundException("discount_config_not_found: please configure max_limit and priority first");
        }
        
        BigDecimal maxLimit = config.getMaxDiscountLimit();
        
        // Obtener prioridades vigentes desde caché o BD
        List<DiscountPriorityEntity> priorities = discountPriorityService.getActivePrioritiesEntity();
        if (priorities.isEmpty()) {
            log.warn("No discount priorities found for transaction: {}", request.transactionId());
            throw new ResourceNotFoundException("discount_priority_not_found: please configure discount priorities");
        }
        
        // Crear mapa de prioridad por tipo de descuento
        Map<String, Integer> priorityMap = priorities.stream()
            .collect(Collectors.toMap(
                DiscountPriorityEntity::getDiscountType,
                DiscountPriorityEntity::getPriorityLevel
            ));
        
        // Calcular total original de descuentos
        BigDecimal totalOriginal = request.discounts().stream()
            .map(DiscountCalculateRequest.DiscountItem::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        log.debug("Original discount total: {} for transaction: {}", totalOriginal, request.transactionId());
        
        // Ordenar descuentos por prioridad
        List<DiscountCalculateRequest.DiscountItem> sortedDiscounts = request.discounts().stream()
            .sorted((d1, d2) -> {
                Integer priority1 = priorityMap.getOrDefault(d1.discountType(), Integer.MAX_VALUE);
                Integer priority2 = priorityMap.getOrDefault(d2.discountType(), Integer.MAX_VALUE);
                return priority1.compareTo(priority2);
            })
            .collect(Collectors.toList());
        
        // Aplicar límite máximo
        BigDecimal accumulatedDiscount = BigDecimal.ZERO;
        List<DiscountCalculateResponse.DiscountItem> appliedDiscounts = new ArrayList<>();
        
        for (DiscountCalculateRequest.DiscountItem discount : sortedDiscounts) {
            BigDecimal discountAmount = discount.amount();
            BigDecimal remaining = maxLimit.subtract(accumulatedDiscount);
            
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                // Se alcanzó el límite, no aplicar más descuentos
                log.debug("Discount limit reached for transaction: {}. Discount type: {} not applied",
                    request.transactionId(), discount.discountType());
                continue;
            }
            
            // Aplicar el menor entre el descuento y el límite restante
            BigDecimal appliedAmount = discountAmount.min(remaining);
            appliedDiscounts.add(
                new DiscountCalculateResponse.DiscountItem(
                    discount.discountType(),
                    appliedAmount
                )
            );
            accumulatedDiscount = accumulatedDiscount.add(appliedAmount);
            
            log.debug("Discount applied. Type: {}, Amount: {}, Running Total: {}, Transaction: {}",
                discount.discountType(), appliedAmount, accumulatedDiscount, request.transactionId());
        }
        
        boolean limitExceeded = totalOriginal.compareTo(maxLimit) > 0;
        
        log.info("Discount calculation complete. Transaction: {}, Original: {}, Applied: {}, Limit: {}, Exceeded: {}",
            request.transactionId(), totalOriginal, accumulatedDiscount, maxLimit, limitExceeded);
        
        // Convertir descuentos originales al tipo response
        List<DiscountCalculateResponse.DiscountItem> originalDiscountsResponse = request.discounts().stream()
            .map(item -> new DiscountCalculateResponse.DiscountItem(
                item.discountType(),
                item.amount()
            ))
            .collect(Collectors.toList());
        
        // Clasificar cliente por fidelidad
        ClassificationResult fidelityClassification = fidelityClassificationService.classify(
            request.ecommerceId(),
            request.clientFidelityPoints()
        );
        
        DiscountCalculateResponse.FidelityClassification fidelityResponse = 
            buildFidelityClassificationResponse(request.clientFidelityPoints(), fidelityClassification);
        
        return new DiscountCalculateResponse(
            request.transactionId(),
            originalDiscountsResponse,
            appliedDiscounts,
            totalOriginal,
            accumulatedDiscount,
            maxLimit,
            limitExceeded,
            fidelityResponse,
            Instant.now()
        );
    }

    /**
     * Build fidelity classification response from ClassificationResult
     */
    private DiscountCalculateResponse.FidelityClassification buildFidelityClassificationResponse(
        Integer clientPoints,
        ClassificationResult classification
    ) {
        if (!classification.isClassified()) {
            // Client did not qualify for any level (NONE)
            return new DiscountCalculateResponse.FidelityClassification(
                clientPoints,
                null,    // no level
                null,    // no level name
                false,   // not classified
                null     // no discount
            );
        }

        FidelityRangeDTO range = classification.getRange();
        return new DiscountCalculateResponse.FidelityClassification(
            clientPoints,
            range.uid(),
            range.name(),
            true,
            range.discountPercentage()
        );
    }
}
