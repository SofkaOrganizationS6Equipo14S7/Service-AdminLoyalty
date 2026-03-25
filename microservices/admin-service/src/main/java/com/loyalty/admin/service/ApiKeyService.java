package com.loyalty.admin.service;

import com.loyalty.admin.dto.ApiKeyValidationResponse;
import com.loyalty.admin.repository.EcommerceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final EcommerceRepository ecommerceRepository;

    public ApiKeyValidationResponse validate(String apiKey) {
        return ecommerceRepository.findByApiKey(apiKey)
                .filter(ecommerce -> ecommerce.isActivo())
                .map(ecommerce -> ApiKeyValidationResponse.builder()
                        .valid(true)
                        .ecommerceId(ecommerce.getId())
                        .ecommerceNombre(ecommerce.getNombre())
                        .build())
                .orElse(ApiKeyValidationResponse.builder().valid(false).build());
    }
}
