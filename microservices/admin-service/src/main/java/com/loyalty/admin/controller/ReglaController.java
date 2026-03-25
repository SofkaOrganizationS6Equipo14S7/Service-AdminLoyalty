package com.loyalty.admin.controller;

import com.loyalty.admin.dto.ReglaEstadoRequest;
import com.loyalty.admin.dto.ReglaRequest;
import com.loyalty.admin.dto.ReglaResponse;
import com.loyalty.admin.service.ReglaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/reglas")
@RequiredArgsConstructor
public class ReglaController {

    private final ReglaService reglaService;

    @GetMapping
    public ResponseEntity<List<ReglaResponse>> findAll(@RequestParam(required = false) Long ecommerceId) {
        if (ecommerceId != null) {
            return ResponseEntity.ok(reglaService.findByEcommerceId(ecommerceId));
        }
        return ResponseEntity.ok(reglaService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReglaResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(reglaService.findById(id));
    }

    @GetMapping("/ecommerce/{ecommerceId}")
    public ResponseEntity<List<ReglaResponse>> findByEcommercePath(@PathVariable Long ecommerceId) {
        return ResponseEntity.ok(reglaService.findByEcommerceId(ecommerceId));
    }

    @PostMapping
    public ResponseEntity<ReglaResponse> create(@Valid @RequestBody ReglaRequest request) {
        return ResponseEntity.ok(reglaService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReglaResponse> update(@PathVariable Long id, @Valid @RequestBody ReglaRequest request) {
        return ResponseEntity.ok(reglaService.update(id, request));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ReglaResponse> updateEstado(@PathVariable Long id, @RequestBody ReglaEstadoRequest request) {
        return ResponseEntity.ok(reglaService.updateEstado(id, request.isActiva()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reglaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
