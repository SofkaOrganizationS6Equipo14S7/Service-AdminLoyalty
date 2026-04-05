package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.rules.discount.DiscountConfigCreateRequest;
import com.loyalty.service_admin.application.dto.rules.discount.DiscountConfigResponse;
import com.loyalty.service_admin.domain.entity.DiscountSettingsEntity;
import com.loyalty.service_admin.domain.repository.DiscountConfigRepository;
import com.loyalty.service_admin.domain.repository.DiscountLimitPriorityRepository;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException;
import com.loyalty.service_admin.infrastructure.rabbitmq.DiscountConfigEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Servicio de lógica de negocio para configuración de límite de descuentos.
 * Implementa validaciones y manejo de eventos.
 */
@Service
@Slf4j
public class DiscountConfigService {

    private final DiscountConfigRepository discountConfigRepository;
    private final DiscountLimitPriorityRepository priorityRepository;
    private final DiscountConfigEventPublisher eventPublisher;

    public DiscountConfigService(
            DiscountConfigRepository discountConfigRepository,
            DiscountLimitPriorityRepository priorityRepository,
            DiscountConfigEventPublisher eventPublisher
    ) {
        this.discountConfigRepository = discountConfigRepository;
        this.priorityRepository = priorityRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Crea o actualiza la configuración de límite de descuentos.
     * Solo una configuración activa por ecommerce.
     * CRITERIO-4.1, CRITERIO-4.5: Valida maxDiscountCap > 0
     */
    @Transactional
    public DiscountConfigResponse updateConfig(DiscountConfigCreateRequest request) {
        // Validar currencyCode (ISO 4217)
        validateCurrencyCode(request.currencyCode());
        
        UUID ecommerceId = request.ecommerceId();

        // Marcar anterior config como inactiva
        discountConfigRepository.findActiveByEcommerceId(ecommerceId)
            .ifPresent(oldConfig -> {
                oldConfig.setIsActive(false);
                discountConfigRepository.save(oldConfig);
                log.info("Marked previous config as inactive for ecommerce: {}", ecommerceId);
            });

        // Crear nueva config con todos los campos
        DiscountSettingsEntity newConfig = new DiscountSettingsEntity();
        newConfig.setEcommerceId(ecommerceId);
        newConfig.setMaxDiscountCap(request.maxDiscountCap());
        newConfig.setCurrencyCode(request.currencyCode() != null ? request.currencyCode().toUpperCase() : "USD");
        newConfig.setAllowStacking(request.allowStacking() != null ? request.allowStacking() : true);
        newConfig.setRoundingRule(request.roundingRule() != null ? request.roundingRule() : "ROUND_HALF_UP");
        newConfig.setIsActive(true);
        newConfig.setVersion(1L);

        DiscountSettingsEntity saved = discountConfigRepository.save(newConfig);
        log.info("Discount config created for ecommerce: {}", ecommerceId);

        // Publicar evento
        eventPublisher.publishDiscountConfigUpdated(saved);

        return toResponse(saved);
    }

    /**
     * Obtiene la configuración activa para un ecommerce.
     */
    @Transactional(readOnly = true)
    public DiscountConfigResponse getActiveConfig(UUID ecommerceId) {
        DiscountSettingsEntity config = discountConfigRepository.findActiveByEcommerceId(ecommerceId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No existe configuración activa de descuentos para el ecommerce: " + ecommerceId
            ));

        return toResponse(config);
    }

    /**
     * Obtiene la entidad de configuración activa (para uso interno).
     */
    @Transactional(readOnly = true)
    public DiscountSettingsEntity getActiveConfigEntity(UUID ecommerceId) {
        return discountConfigRepository.findActiveByEcommerceId(ecommerceId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No existe configuración activa de descuentos para el ecommerce: " + ecommerceId
            ));
    }

    /**
     * Valida que currencyCode sea un código ISO 4217 válido (3 caracteres).
     */
    private void validateCurrencyCode(String currencyCode) {
        if (currencyCode == null || currencyCode.length() != 3) {
            throw new BadRequestException("currencyCode debe ser un código ISO 4217 válido (3 caracteres)");
        }
    }

    /**
     * Convierte una entidad a DTO response.
     */
    private DiscountConfigResponse toResponse(DiscountSettingsEntity entity) {
        return new DiscountConfigResponse(
            entity.getId(),
            entity.getEcommerceId(),
            entity.getMaxDiscountCap(),
            entity.getCurrencyCode(),
            entity.getAllowStacking(),
            entity.getRoundingRule(),
            entity.getIsActive(),
            entity.getVersion(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
