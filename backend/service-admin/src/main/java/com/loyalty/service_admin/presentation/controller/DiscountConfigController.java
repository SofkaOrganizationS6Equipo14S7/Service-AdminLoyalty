package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.rules.discount.DiscountConfigCreateRequest;
import com.loyalty.service_admin.application.dto.rules.discount.DiscountConfigResponse;
import com.loyalty.service_admin.application.dto.rules.discount.DiscountLimitPriorityRequest;
import com.loyalty.service_admin.application.dto.rules.discount.DiscountLimitPriorityResponse;
import com.loyalty.service_admin.application.service.DiscountConfigService;
import com.loyalty.service_admin.application.service.DiscountLimitPriorityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Slf4j
public class DiscountConfigController {

    private final DiscountConfigService discountConfigService;
    private final DiscountLimitPriorityService priorityService;

    public DiscountConfigController(
            DiscountConfigService discountConfigService,
            DiscountLimitPriorityService priorityService
    ) {
        this.discountConfigService = discountConfigService;
        this.priorityService = priorityService;
    }

    /**
     * @param request discount configuration data
     * @return HTTP 201 Created with DiscountConfigResponse
     */
    @PostMapping("/discount-config")
    public ResponseEntity<DiscountConfigResponse> updateDiscountConfig(
            @RequestBody DiscountConfigCreateRequest request
    ) {
        DiscountConfigResponse response = discountConfigService.updateConfig(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * CRITERIO-4.2: Obtener configuración activa de descuentos
     * @param ecommerceId ecommerce identifier (UUID)
     * @return HTTP 200 OK with active DiscountConfigResponse
     */
    @GetMapping("/discount-config")
    public ResponseEntity<DiscountConfigResponse> getDiscountConfig(
            @RequestParam UUID ecommerceId
    ) {
        DiscountConfigResponse response = discountConfigService.getActiveConfig(ecommerceId);
        return ResponseEntity.ok(response);
    }

    /**
     * @param request priority configuration data
     * @return HTTP 201 Created with DiscountLimitPriorityResponse
     */
    @PostMapping("/discount-priority")
    public ResponseEntity<DiscountLimitPriorityResponse> savePriorities(
            @RequestBody DiscountLimitPriorityRequest request
    ) {
        DiscountLimitPriorityResponse response = priorityService.savePriorities(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * CRITERIO-4.4: Obtener prioridades configuradas
     * @param discountSettingId discount configuration identifier (UUID)
     * @return HTTP 200 OK with DiscountLimitPriorityResponse
     */
    @GetMapping("/discount-priority")
    public ResponseEntity<DiscountLimitPriorityResponse> getPriorities(
            @RequestParam UUID discountSettingId
    ) {
        DiscountLimitPriorityResponse response = priorityService.getPriorities(discountSettingId);
        return ResponseEntity.ok(response);
    }
}
