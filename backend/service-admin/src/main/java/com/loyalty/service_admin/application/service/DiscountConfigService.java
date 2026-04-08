package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.rules.discount.DiscountConfigCreateRequest;
import com.loyalty.service_admin.application.dto.rules.discount.DiscountConfigResponse;
import com.loyalty.service_admin.application.port.in.DiscountConfigUseCase;
import com.loyalty.service_admin.application.port.out.DiscountConfigPersistencePort;
import com.loyalty.service_admin.application.port.out.DiscountConfigEventPort;
import com.loyalty.service_admin.domain.entity.DiscountSettingsEntity;
import com.loyalty.service_admin.domain.repository.DiscountLimitPriorityRepository;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.Instant;
import java.util.UUID;

/**
 * Servicio de lógica de negocio para configuración de límite de descuentos.
 * Implementa DiscountConfigUseCase con puertos para persistencia y eventos.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class DiscountConfigService implements DiscountConfigUseCase {

    private final DiscountConfigPersistencePort persistencePort;
    private final DiscountConfigEventPort eventPort;
    private final DiscountLimitPriorityRepository priorityRepository;

    @Override
    @Transactional
    public DiscountConfigResponse updateConfig(DiscountConfigCreateRequest request) {
        // Validar currencyCode (ISO 4217)
        validateCurrencyCode(request.currencyCode());
        
        UUID ecommerceId = request.ecommerceId();

        // Marcar anterior config como inactiva
        persistencePort.findActiveConfigByEcommerce(ecommerceId)
            .ifPresent(oldConfig -> {
                oldConfig.setIsActive(false);
                persistencePort.saveConfig(oldConfig);
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

        DiscountSettingsEntity saved = persistencePort.saveConfig(newConfig);
        log.info("Discount config created for ecommerce: {}", ecommerceId);

        // Publicar evento via puerto
        eventPort.publishConfigUpdated(saved, ecommerceId);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DiscountConfigResponse getActiveConfig(UUID ecommerceId) {
        DiscountSettingsEntity config = persistencePort.findActiveConfigByEcommerce(ecommerceId)
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
        return persistencePort.findActiveConfigByEcommerce(ecommerceId)
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
