package com.loyalty.admin.service;

import com.loyalty.admin.dto.ConfiguracionDescuentoRequest;
import com.loyalty.admin.dto.ConfiguracionDescuentoResponse;
import com.loyalty.admin.dto.RangoFidelidadResponse;
import com.loyalty.admin.entity.ConfiguracionDescuento;
import com.loyalty.admin.exception.NotFoundException;
import com.loyalty.admin.repository.ConfiguracionDescuentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfiguracionDescuentoService {

    private final ConfiguracionDescuentoRepository configuracionRepository;
    private final EcommerceService ecommerceService;
    private final RangoFidelidadService rangoFidelidadService;

    public ConfiguracionDescuentoResponse upsert(ConfiguracionDescuentoRequest request) {
        ConfiguracionDescuento configuracion = configuracionRepository.findByEcommerceId(request.getEcommerceId())
                .orElseGet(ConfiguracionDescuento::new);

        configuracion.setEcommerce(ecommerceService.getEntity(request.getEcommerceId()));
        configuracion.setTopeMaximo(request.getTopeMaximo());
        configuracion.setPrioridadGlobal(request.getPrioridadGlobal());

        ConfiguracionDescuento saved = configuracionRepository.save(configuracion);
        log.info("Configuracion upsert ecommerceId={}", request.getEcommerceId());

        return toResponse(saved, rangoFidelidadService.findByEcommerceId(request.getEcommerceId()));
    }

    public ConfiguracionDescuentoResponse findByEcommerceId(Long ecommerceId) {
        ConfiguracionDescuento configuracion = configuracionRepository.findByEcommerceId(ecommerceId)
                .orElseThrow(() -> new NotFoundException("Configuracion no encontrada"));
        return toResponse(configuracion, rangoFidelidadService.findByEcommerceId(ecommerceId));
    }

    private ConfiguracionDescuentoResponse toResponse(ConfiguracionDescuento configuracion,
                                                      List<RangoFidelidadResponse> rangos) {
        return ConfiguracionDescuentoResponse.builder()
                .ecommerceId(configuracion.getEcommerce().getId())
                .topeMaximo(configuracion.getTopeMaximo())
                .prioridadGlobal(configuracion.getPrioridadGlobal())
                .rangosFidelidad(rangos)
                .build();
    }
}
