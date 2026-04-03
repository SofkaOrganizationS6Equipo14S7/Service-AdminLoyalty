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
     * @param ecommerceId ecommerce identifier
     * @return HTTP 200 OK with active DiscountConfigResponse
     */
    @GetMapping("/discount-config")
    public ResponseEntity<DiscountConfigResponse> getDiscountConfig(
            @RequestParam String ecommerceId
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
     * @param configId discount configuration identifier
     * @return HTTP 200 OK with DiscountLimitPriorityResponse
     */
    @GetMapping("/discount-priority")
    public ResponseEntity<DiscountLimitPriorityResponse> getPriorities(
            @RequestParam String configId
    ) {
        DiscountLimitPriorityResponse response = priorityService.getPriorities(configId);
        return ResponseEntity.ok(response);
    }
}
