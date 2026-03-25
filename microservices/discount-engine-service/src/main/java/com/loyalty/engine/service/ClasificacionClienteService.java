package com.loyalty.engine.service;

import com.loyalty.engine.dto.ClasificarClienteResponse;
import com.loyalty.engine.model.NivelFidelidad;
import org.springframework.stereotype.Service;

@Service
public class ClasificacionClienteService {

    public NivelFidelidad clasificar(int puntosFidelidad) {
        if (puntosFidelidad < 1000) {
            return NivelFidelidad.BASICO;
        }
        if (puntosFidelidad < 5000) {
            return NivelFidelidad.PLATA;
        }
        return NivelFidelidad.ORO;
    }

    public ClasificarClienteResponse clasificarConDescripcion(int puntosFidelidad) {
        NivelFidelidad nivel = clasificar(puntosFidelidad);
        String descripcion = switch (nivel) {
            case BASICO -> "Cliente de entrada al programa";
            case PLATA -> "Cliente recurrente con beneficios medios";
            case ORO -> "Cliente premium con mayor fidelidad";
        };
        return ClasificarClienteResponse.builder()
                .nivel(nivel.name())
                .descripcion(descripcion)
                .build();
    }
}
