package com.loyalty.admin.controller;

import com.loyalty.admin.dto.RangoFidelidadRequest;
import com.loyalty.admin.dto.RangoFidelidadResponse;
import com.loyalty.admin.service.RangoFidelidadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/rangos-fidelidad")
@RequiredArgsConstructor
public class RangoFidelidadController {

    private final RangoFidelidadService rangoFidelidadService;

    @GetMapping
    public ResponseEntity<List<RangoFidelidadResponse>> findByEcommerce(@RequestParam Long ecommerceId) {
        return ResponseEntity.ok(rangoFidelidadService.findByEcommerceId(ecommerceId));
    }

    @PostMapping
    public ResponseEntity<RangoFidelidadResponse> create(@Valid @RequestBody RangoFidelidadRequest request) {
        return ResponseEntity.ok(rangoFidelidadService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RangoFidelidadResponse> update(@PathVariable Long id, @Valid @RequestBody RangoFidelidadRequest request) {
        return ResponseEntity.ok(rangoFidelidadService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        rangoFidelidadService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
