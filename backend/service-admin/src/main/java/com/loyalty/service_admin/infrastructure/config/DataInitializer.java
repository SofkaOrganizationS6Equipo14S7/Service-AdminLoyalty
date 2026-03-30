package com.loyalty.service_admin.infrastructure.config;

import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Inicializador de datos para la base de datos.
 * Crea un usuario por defecto si la tabla está vacía.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    /**
     * Bean que se ejecuta al iniciar la aplicación.
     * Crea un usuario administrador por defecto.
     */
    @Bean
    public CommandLineRunner initDatabase(UserRepository userRepository) {
        return args -> {
            // Verificar si ya existen usuarios
            if (userRepository.count() == 0) {
                String hashedPassword = BCrypt.hashpw("admin123", BCrypt.gensalt());
                
                UserEntity adminUser = UserEntity.builder()
                        .username("admin")
                        .email("admin@system.local")
                        .password(hashedPassword)
                        .role("SUPER_ADMIN")
                        .active(true)
                        .build();
                
                userRepository.save(adminUser);
                log.info("Usuario administrador creado: admin");
            } else {
                log.info("Base de datos ya contiene usuarios, no se crea usuario por defecto");
            }
        };
    }
}
