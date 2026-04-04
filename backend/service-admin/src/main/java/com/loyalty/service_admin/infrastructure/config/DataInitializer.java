package com.loyalty.service_admin.infrastructure.config;

import com.loyalty.service_admin.domain.entity.RoleEntity;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.domain.repository.RoleRepository;
import com.loyalty.service_admin.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

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
    public CommandLineRunner initDatabase(
            UserRepository userRepository,
            RoleRepository roleRepository,
            @Value("${app.bootstrap.admin-ecommerce-id:00000000-0000-0000-0000-000000000001}") UUID adminEcommerceId
    ) {
        return args -> {
            // Verificar si ya existen usuarios
            if (userRepository.count() == 0) {
                // Obtener el rol SUPER_ADMIN
                RoleEntity superAdminRole = roleRepository.findByName("SUPER_ADMIN")
                        .orElseThrow(() -> new RuntimeException("SUPER_ADMIN role not found"));
                
                String hashedPassword = BCrypt.hashpw("admin123", BCrypt.gensalt());
                
                UserEntity adminUser = UserEntity.builder()
                        .username("admin")
                        .email("admin@system.local")
                        .passwordHash(hashedPassword)
                        .role(superAdminRole)
                        .isActive(true)
                        .build();
                
                userRepository.save(adminUser);
                log.info("Usuario administrador creado: admin (ecommerceId={})", adminEcommerceId);
            } else {
                log.info("Base de datos ya contiene usuarios, no se crea usuario por defecto");
            }
        };
    }
}
