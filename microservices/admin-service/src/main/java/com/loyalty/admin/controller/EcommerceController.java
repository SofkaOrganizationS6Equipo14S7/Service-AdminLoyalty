package com.loyalty.admin.controller;

import com.loyalty.admin.dto.EcommerceRequest;
import com.loyalty.admin.dto.EcommerceResponse;
import com.loyalty.admin.service.EcommerceService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ecommerce")
@RequiredArgsConstructor
public class EcommerceController {

    private final EcommerceService ecommerceService;

    @GetMapping
    public ResponseEntity<List<EcommerceResponse>> findAll() {
        return ResponseEntity.ok(ecommerceService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EcommerceResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ecommerceService.findById(id));
    }

    @PostMapping
    public ResponseEntity<EcommerceResponse> create(@Valid @RequestBody EcommerceRequest request) {
        return ResponseEntity.ok(ecommerceService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EcommerceResponse> update(@PathVariable Long id, @Valid @RequestBody EcommerceRequest request) {
        return ResponseEntity.ok(ecommerceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ecommerceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
