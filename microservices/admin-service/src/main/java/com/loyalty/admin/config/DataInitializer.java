package com.loyalty.admin.config;

import com.loyalty.admin.entity.ConfiguracionDescuento;
import com.loyalty.admin.entity.Ecommerce;
import com.loyalty.admin.entity.Regla;
import com.loyalty.admin.entity.ReglaTipo;
import com.loyalty.admin.entity.User;
import com.loyalty.admin.repository.ConfiguracionDescuentoRepository;
import com.loyalty.admin.repository.EcommerceRepository;
import com.loyalty.admin.repository.ReglaRepository;
import com.loyalty.admin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final EcommerceRepository ecommerceRepository;
    private final ConfiguracionDescuentoRepository configuracionRepository;
    private final ReglaRepository reglaRepository;

    @Bean
    CommandLineRunner seedData() {
        return args -> {
            if (userRepository.count() == 0) {
                userRepository.save(User.builder()
                        .username("admin")
                        .password("admin123")
                        .role("ADMIN")
                        .active(true)
                        .build());
            }

            if (ecommerceRepository.count() == 0) {
                Ecommerce ecommerce = ecommerceRepository.save(Ecommerce.builder()
                        .nombre("tienda-demo")
                        .apiKey("demo-api-key")
                        .activo(true)
                        .build());

                configuracionRepository.save(ConfiguracionDescuento.builder()
                        .ecommerce(ecommerce)
                        .topeMaximo(BigDecimal.valueOf(120.00))
                        .prioridadGlobal(1)
                        .build());

                reglaRepository.save(Regla.builder()
                        .ecommerce(ecommerce)
                        .tipo(ReglaTipo.TEMPORADA)
                        .nombre("Black Friday")
                        .descripcion("Descuento de temporada")
                        .porcentajeDescuento(BigDecimal.valueOf(10.00))
                        .prioridad(1)
                        .activa(true)
                        .temporada("BLACK_FRIDAY")
                        .build());
            }
        };
    }
}
