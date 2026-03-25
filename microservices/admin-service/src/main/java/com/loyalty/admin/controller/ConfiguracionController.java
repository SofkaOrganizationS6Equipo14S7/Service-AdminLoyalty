package com.loyalty.admin.controller;

import com.loyalty.admin.dto.ConfiguracionDescuentoRequest;
import com.loyalty.admin.dto.ConfiguracionDescuentoResponse;
import com.loyalty.admin.service.ConfiguracionDescuentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/configuracion")
@RequiredArgsConstructor
public class ConfiguracionController {

    private final ConfiguracionDescuentoService configuracionService;

    @PostMapping
    public ResponseEntity<ConfiguracionDescuentoResponse> upsert(@Valid @RequestBody ConfiguracionDescuentoRequest request) {
        return ResponseEntity.ok(configuracionService.upsert(request));
    }

    @GetMapping("/{ecommerceId}")
    public ResponseEntity<ConfiguracionDescuentoResponse> findByEcommerceId(@PathVariable Long ecommerceId) {
        return ResponseEntity.ok(configuracionService.findByEcommerceId(ecommerceId));
    }
}
