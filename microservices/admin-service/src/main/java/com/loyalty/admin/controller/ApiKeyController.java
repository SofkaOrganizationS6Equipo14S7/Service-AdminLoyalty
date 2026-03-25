package com.loyalty.admin.controller;

import com.loyalty.admin.dto.ApiKeyValidationRequest;
import com.loyalty.admin.dto.ApiKeyValidationResponse;
import com.loyalty.admin.service.ApiKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/apikey")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @PostMapping("/validar")
    public ResponseEntity<ApiKeyValidationResponse> validate(@Valid @RequestBody ApiKeyValidationRequest request) {
        return ResponseEntity.ok(apiKeyService.validate(request.getApiKey()));
    }
}
