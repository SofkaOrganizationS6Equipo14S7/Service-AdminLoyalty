package com.loyalty.engine.controller;

import com.loyalty.engine.dto.CalcularDescuentoRequest;
import com.loyalty.engine.dto.CalcularDescuentoResponse;
import com.loyalty.engine.dto.ClasificarClienteRequest;
import com.loyalty.engine.dto.ClasificarClienteResponse;
import com.loyalty.engine.service.ClasificacionClienteService;
import com.loyalty.engine.service.DiscountCalculatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class DiscountController {

    private final DiscountCalculatorService discountCalculatorService;
    private final ClasificacionClienteService clasificacionClienteService;

    @PostMapping("/calcular-descuento")
    public ResponseEntity<CalcularDescuentoResponse> calcular(@Valid @RequestBody CalcularDescuentoRequest request) {
        return ResponseEntity.ok(discountCalculatorService.calcular(request));
    }

    @PostMapping("/clasificar-cliente")
    public ResponseEntity<ClasificarClienteResponse> clasificar(@Valid @RequestBody ClasificarClienteRequest request) {
        return ResponseEntity.ok(clasificacionClienteService.clasificarConDescripcion(request.getPuntosFidelidad()));
    }
}
