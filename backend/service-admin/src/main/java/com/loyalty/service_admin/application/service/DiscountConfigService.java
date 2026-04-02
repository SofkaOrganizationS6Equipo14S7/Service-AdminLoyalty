package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.DiscountConfigCreateRequest;
import com.loyalty.service_admin.application.dto.DiscountConfigResponse;
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
     */
    @Transactional
    public DiscountConfigResponse updateConfig(DiscountConfigCreateRequest request) {
        // Validar maxDiscountLimit
        BigDecimal maxLimit = validateAndParseMaxDiscountLimit(request.maxDiscountLimit());
        
        // Validar currencyCode (ISO 4217)
        validateCurrencyCode(request.currencyCode());
        
        UUID ecommerceId = UUID.fromString(request.ecommerceId());

        // Marcar anterior config como inactiva
        discountConfigRepository.findActiveByEcommerceId(ecommerceId)
            .ifPresent(oldConfig -> {
                oldConfig.setIsActive(false);
                discountConfigRepository.save(oldConfig);
                log.info("Marked previous config as inactive for ecommerce: {}", ecommerceId);
            });

        // Crear nueva config
        DiscountSettingsEntity newConfig = new DiscountSettingsEntity();
        newConfig.setEcommerceId(ecommerceId);
        newConfig.setMaxDiscountCap(maxLimit);
        newConfig.setCurrencyCode(request.currencyCode().toUpperCase());
        newConfig.setIsActive(true);

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
    public DiscountConfigResponse getActiveConfig(String ecommerceId) {
        UUID ecommerceUuid = UUID.fromString(ecommerceId);
        
        DiscountSettingsEntity config = discountConfigRepository.findActiveByEcommerceId(ecommerceUuid)
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
     * Valida que maxDiscountLimit sea un número positivo mayor a cero.
     */
    private BigDecimal validateAndParseMaxDiscountLimit(String limitStr) {
        try {
            BigDecimal limit = new BigDecimal(limitStr);
            if (limit.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("maxDiscountLimit debe ser un valor positivo mayor a cero");
            }
            return limit;
        } catch (NumberFormatException e) {
            throw new BadRequestException("maxDiscountLimit debe ser un número válido: " + limitStr);
        }
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
            entity.getId().toString(),
            entity.getEcommerceId().toString(),
            entity.getMaxDiscountCap().toPlainString(),
            entity.getCurrencyCode(),
            entity.getIsActive(),
            entity.getCreatedAt().atOffset(java.time.ZoneOffset.UTC),
            entity.getUpdatedAt().atOffset(java.time.ZoneOffset.UTC)
        );
    }
}
