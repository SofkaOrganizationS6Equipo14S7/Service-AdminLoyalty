package com.loyalty.admin.service;

import com.loyalty.admin.dto.ReglaRequest;
import com.loyalty.admin.dto.ReglaResponse;
import com.loyalty.admin.entity.Regla;
import com.loyalty.admin.exception.NotFoundException;
import com.loyalty.admin.repository.ReglaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReglaService {

    private final ReglaRepository reglaRepository;
    private final EcommerceService ecommerceService;

    public List<ReglaResponse> findAll() {
        return reglaRepository.findAll().stream().map(this::toResponse).toList();
    }

    public ReglaResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    public List<ReglaResponse> findByEcommerceId(Long ecommerceId) {
        return reglaRepository.findByEcommerceId(ecommerceId).stream().map(this::toResponse).toList();
    }

    public ReglaResponse create(ReglaRequest request) {
        Regla saved = reglaRepository.save(Regla.builder()
                .ecommerce(ecommerceService.getEntity(request.getEcommerceId()))
                .tipo(request.getTipo())
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .porcentajeDescuento(request.getPorcentajeDescuento())
                .prioridad(request.getPrioridad())
                .activa(request.isActiva())
                .productoSku(request.getProductoSku())
                .temporada(request.getTemporada())
                .build());
        log.info("Regla creada id={}", saved.getId());
        return toResponse(saved);
    }

    public ReglaResponse update(Long id, ReglaRequest request) {
        Regla regla = getEntity(id);
        regla.setEcommerce(ecommerceService.getEntity(request.getEcommerceId()));
        regla.setTipo(request.getTipo());
        regla.setNombre(request.getNombre());
        regla.setDescripcion(request.getDescripcion());
        regla.setPorcentajeDescuento(request.getPorcentajeDescuento());
        regla.setPrioridad(request.getPrioridad());
        regla.setActiva(request.isActiva());
        regla.setProductoSku(request.getProductoSku());
        regla.setTemporada(request.getTemporada());
        Regla saved = reglaRepository.save(regla);
        log.info("Regla actualizada id={}", saved.getId());
        return toResponse(saved);
    }

    public ReglaResponse updateEstado(Long id, boolean activa) {
        Regla regla = getEntity(id);
        regla.setActiva(activa);
        Regla saved = reglaRepository.save(regla);
        log.info("Estado de regla actualizado id={} activa={}", id, activa);
        return toResponse(saved);
    }

    public void delete(Long id) {
        reglaRepository.delete(getEntity(id));
        log.info("Regla eliminada id={}", id);
    }

    private Regla getEntity(Long id) {
        return reglaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Regla no encontrada"));
    }

    private ReglaResponse toResponse(Regla regla) {
        return ReglaResponse.builder()
                .id(regla.getId())
                .ecommerceId(regla.getEcommerce().getId())
                .tipo(regla.getTipo())
                .nombre(regla.getNombre())
                .descripcion(regla.getDescripcion())
                .porcentajeDescuento(regla.getPorcentajeDescuento())
                .prioridad(regla.getPrioridad())
                .activa(regla.isActiva())
                .productoSku(regla.getProductoSku())
                .temporada(regla.getTemporada())
                .build();
    }
}
