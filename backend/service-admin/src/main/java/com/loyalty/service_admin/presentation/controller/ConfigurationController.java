package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.configuration.ApiResponse;
import com.loyalty.service_admin.application.dto.configuration.ConfigurationCreateRequest;
import com.loyalty.service_admin.application.dto.configuration.ConfigurationPatchRequest;
import com.loyalty.service_admin.application.dto.configuration.ConfigurationWriteData;
import com.loyalty.service_admin.application.port.in.ConfigurationUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/configurations")
@RequiredArgsConstructor
public class ConfigurationController {

    private final ConfigurationUseCase configurationService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ConfigurationWriteData>> create(@Valid @RequestBody ConfigurationCreateRequest request) {
        ConfigurationWriteData data = configurationService.createConfiguration(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, data));
    }

    @PatchMapping("/{ecommerceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ConfigurationWriteData>> patch(
            @PathVariable UUID ecommerceId,
            @Valid @RequestBody ConfigurationPatchRequest request
    ) {
        ConfigurationWriteData data = configurationService.patchConfiguration(ecommerceId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, data));
    }
}
