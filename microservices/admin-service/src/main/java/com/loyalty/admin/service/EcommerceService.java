package com.loyalty.admin.service;

import com.loyalty.admin.dto.EcommerceRequest;
import com.loyalty.admin.dto.EcommerceResponse;
import com.loyalty.admin.entity.Ecommerce;
import com.loyalty.admin.exception.NotFoundException;
import com.loyalty.admin.repository.EcommerceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EcommerceService {

    private final EcommerceRepository ecommerceRepository;

    public List<EcommerceResponse> findAll() {
        return ecommerceRepository.findAll().stream().map(this::toResponse).toList();
    }

    public EcommerceResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    public Ecommerce getEntity(Long id) {
        return ecommerceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ecommerce no encontrado"));
    }

    public EcommerceResponse create(EcommerceRequest request) {
        Ecommerce saved = ecommerceRepository.save(Ecommerce.builder()
                .nombre(request.getNombre())
                .apiKey(request.getApiKey())
                .activo(request.isActivo())
                .build());
        log.info("Ecommerce creado id={}", saved.getId());
        return toResponse(saved);
    }

    public EcommerceResponse update(Long id, EcommerceRequest request) {
        Ecommerce ecommerce = getEntity(id);
        ecommerce.setNombre(request.getNombre());
        ecommerce.setApiKey(request.getApiKey());
        ecommerce.setActivo(request.isActivo());
        Ecommerce saved = ecommerceRepository.save(ecommerce);
        log.info("Ecommerce actualizado id={}", saved.getId());
        return toResponse(saved);
    }

    public void delete(Long id) {
        ecommerceRepository.delete(getEntity(id));
        log.info("Ecommerce eliminado id={}", id);
    }

    public EcommerceResponse toResponse(Ecommerce ecommerce) {
        return EcommerceResponse.builder()
                .id(ecommerce.getId())
                .nombre(ecommerce.getNombre())
                .apiKey(ecommerce.getApiKey())
                .activo(ecommerce.isActivo())
                .build();
    }
}
