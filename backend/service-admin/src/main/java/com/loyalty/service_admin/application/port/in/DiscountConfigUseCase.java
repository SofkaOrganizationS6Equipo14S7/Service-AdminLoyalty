package com.loyalty.service_admin.application.port.in;

import com.loyalty.service_admin.application.dto.discount.DiscountConfigCreateRequest;
import com.loyalty.service_admin.application.dto.discount.DiscountConfigResponse;

import java.util.UUID;

/**
 * DiscountConfigUseCase - Puerto de entrada para operaciones sobre configuración de descuentos.
 *
 * Abarca:
 * - HU-09: Discount Limits Configuration
 */
public interface DiscountConfigUseCase {

    /**
     * Crear o actualizar configuración de descuentos.
     */
    DiscountConfigResponse updateConfig(DiscountConfigCreateRequest request);

    /**
     * Obtener configuración activa de descuentos para un ecommerce.
     */
    DiscountConfigResponse getActiveConfig(UUID ecommerceId);
}
