package com.loyalty.service_admin.application.port.out;

import java.util.UUID;

/**
 * Puerto de salida para operaciones JWT.
 * 
 * Abstrae generación, validación y parsing de tokens JWT.
 * Implementación: JwtTokenAdapter (infrastructure/security/)
 */
public interface JwtPort {

    /**
     * Genera JWT token con claims de usuario
     * 
     * @param username nombre del usuario
     * @param userId ID único del usuario (UUID)
     * @param role nombre del rol (ej: SUPER_ADMIN, ADMIN, USER)
     * @param ecommerceId ID del ecommerce asociado
     * @return JWT token en formato string
     */
    String generateToken(String username, UUID userId, String role, UUID ecommerceId);

    /**
     * Valida JWT token
     * 
     * @param token JWT a validar
     * @return true si válido y no expirado, false en caso contrario
     */
    boolean validateToken(String token);

    /**
     * Extrae username del JWT
     * 
     * @param token JWT a parsear
     * @return username contenido en claims
     * @throws com.loyalty.service_admin.infrastructure.exception.UnauthorizedException 
     *         si token inválido o no puede ser parseado
     */
    String getUsernameFromToken(String token);
}
