package com.loyalty.admin.service;

import com.loyalty.admin.dto.RangoFidelidadRequest;
import com.loyalty.admin.dto.RangoFidelidadResponse;
import com.loyalty.admin.entity.RangoFidelidad;
import com.loyalty.admin.exception.NotFoundException;
import com.loyalty.admin.repository.RangoFidelidadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RangoFidelidadService {

    private final RangoFidelidadRepository rangoRepository;
    private final EcommerceService ecommerceService;

    public List<RangoFidelidadResponse> findByEcommerceId(Long ecommerceId) {
        return rangoRepository.findByEcommerceId(ecommerceId).stream().map(this::toResponse).toList();
    }

    public RangoFidelidadResponse create(RangoFidelidadRequest request) {
        RangoFidelidad saved = rangoRepository.save(RangoFidelidad.builder()
                .ecommerce(ecommerceService.getEntity(request.getEcommerceId()))
                .nombre(request.getNombre())
                .minPuntos(request.getMinPuntos())
                .maxPuntos(request.getMaxPuntos())
                .porcentajeDescuento(request.getPorcentajeDescuento())
                .build());
        log.info("Rango fidelidad creado id={}", saved.getId());
        return toResponse(saved);
    }

    public RangoFidelidadResponse update(Long id, RangoFidelidadRequest request) {
        RangoFidelidad rango = getEntity(id);
        rango.setEcommerce(ecommerceService.getEntity(request.getEcommerceId()));
        rango.setNombre(request.getNombre());
        rango.setMinPuntos(request.getMinPuntos());
        rango.setMaxPuntos(request.getMaxPuntos());
        rango.setPorcentajeDescuento(request.getPorcentajeDescuento());
        RangoFidelidad saved = rangoRepository.save(rango);
        log.info("Rango fidelidad actualizado id={}", saved.getId());
        return toResponse(saved);
    }

    public void delete(Long id) {
        rangoRepository.delete(getEntity(id));
        log.info("Rango fidelidad eliminado id={}", id);
    }

    private RangoFidelidad getEntity(Long id) {
        return rangoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Rango fidelidad no encontrado"));
    }

    private RangoFidelidadResponse toResponse(RangoFidelidad rango) {
        return RangoFidelidadResponse.builder()
                .id(rango.getId())
                .ecommerceId(rango.getEcommerce().getId())
                .nombre(rango.getNombre())
                .minPuntos(rango.getMinPuntos())
                .maxPuntos(rango.getMaxPuntos())
                .porcentajeDescuento(rango.getPorcentajeDescuento())
                .build();
    }
}
