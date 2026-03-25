package com.loyalty.engine.service;

import com.loyalty.engine.client.AdminServiceClient;
import com.loyalty.engine.dto.AdminConfiguracionResponse;
import com.loyalty.engine.dto.AdminReglaResponse;
import com.loyalty.engine.dto.CalcularDescuentoRequest;
import com.loyalty.engine.dto.CalcularDescuentoResponse;
import com.loyalty.engine.exception.EngineException;
import com.loyalty.engine.model.NivelFidelidad;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscountCalculatorService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final AdminServiceClient adminServiceClient;
    private final ClasificacionClienteService clasificacionClienteService;

    public CalcularDescuentoResponse calcular(CalcularDescuentoRequest request) {
        if (!adminServiceClient.validateApiKey(request.getApiKey())) {
            throw new EngineException("apiKey invalida");
        }

        AdminConfiguracionResponse config = adminServiceClient.getConfiguracion(request.getEcommerceId());
        List<AdminReglaResponse> reglas = adminServiceClient.getReglas(request.getEcommerceId());

        BigDecimal subtotal = request.getItems().stream()
                .map(item -> item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal descuentoAcumulado = BigDecimal.ZERO;
        List<String> reglasAplicadas = new ArrayList<>();

        List<AdminReglaResponse> reglasOrdenadas = reglas.stream()
                .filter(AdminReglaResponse::isActiva)
                .sorted(Comparator.comparing(AdminReglaResponse::getPrioridad, Comparator.nullsLast(Integer::compareTo)))
                .toList();

        for (AdminReglaResponse regla : reglasOrdenadas) {
            if (!cumpleRegla(request, regla)) {
                continue;
            }

            BigDecimal descuentoRegla = subtotal
                    .multiply(regla.getPorcentajeDescuento())
                    .divide(CIEN, 2, RoundingMode.HALF_UP);

            BigDecimal saldoTope = config.getTopeMaximo().subtract(descuentoAcumulado);
            if (saldoTope.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal descuentoAplicable = descuentoRegla.min(saldoTope);
            if (descuentoAplicable.compareTo(BigDecimal.ZERO) > 0) {
                descuentoAcumulado = descuentoAcumulado.add(descuentoAplicable);
                reglasAplicadas.add(regla.getNombre());
            }
        }

        NivelFidelidad nivel = clasificacionClienteService.clasificar(request.getPuntosFidelidad());

        BigDecimal total = subtotal.subtract(descuentoAcumulado).max(BigDecimal.ZERO);
        log.info("Descuento calculado clienteId={} subtotal={} descuento={} total={}",
                request.getClienteId(), subtotal, descuentoAcumulado, total);

        return CalcularDescuentoResponse.builder()
                .clienteId(request.getClienteId())
                .nivelFidelidad(nivel.name())
                .subtotal(subtotal)
                .descuentoAplicado(descuentoAcumulado)
                .total(total)
                .reglasAplicadas(reglasAplicadas)
                .build();
    }

    private boolean cumpleRegla(CalcularDescuentoRequest request, AdminReglaResponse regla) {
        if ("PRODUCTO".equalsIgnoreCase(regla.getTipo())) {
            return request.getItems().stream()
                    .map(CalcularDescuentoRequest.ItemRequest::getSku)
                    .anyMatch(sku -> Objects.equals(sku, regla.getProductoSku()));
        }

        if ("TEMPORADA".equalsIgnoreCase(regla.getTipo())) {
            return Objects.equals(request.getTemporada(), regla.getTemporada());
        }

        return false;
    }
}
