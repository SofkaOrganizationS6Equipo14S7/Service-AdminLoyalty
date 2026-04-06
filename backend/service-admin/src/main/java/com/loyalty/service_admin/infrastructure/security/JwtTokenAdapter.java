package com.loyalty.service_admin.infrastructure.security;

import com.loyalty.service_admin.application.port.out.JwtPort;
import com.loyalty.service_admin.infrastructure.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenAdapter implements JwtPort {

    private final JwtProvider jwtProvider;

    /**
     * Genera JWT token con claims de usuario
     * 
     * @param username nombre del usuario
     * @param userId ID único del usuario
     * @param role nombre del rol
     * @param ecommerceId ID del ecommerce
     * @return JWT token
     */
    @Override
    public String generateToken(String username, UUID userId, String role, UUID ecommerceId) {
        return jwtProvider.generateToken(username, userId, role, ecommerceId);
    }

    /**
     * Valida JWT token
     * 
     * @param token JWT a validar
     * @return true si válido y no expirado, false en caso contrario
     */
    @Override
    public boolean validateToken(String token) {
        try {
            return jwtProvider.validateToken(token);
        } catch (io.jsonwebtoken.JwtException e) {
            log.debug("Token inválido: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.debug("Error validando token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extrae username del JWT
     * 
     * @param token JWT a parsear
     * @return username contenido en claims
     * @throws UnauthorizedException si token no puede ser parseado
     */
    @Override
    public String getUsernameFromToken(String token) {
        try {
            return jwtProvider.getUsernameFromToken(token);
        } catch (io.jsonwebtoken.JwtException e) {
            log.debug("Error parseando token: {}", e.getMessage());
            throw new UnauthorizedException("Token no válido o expirado");
        } catch (Exception e) {
            log.debug("Error inesperado en getUsernameFromToken: {}", e.getMessage());
            throw new UnauthorizedException("Token no válido o expirado");
        }
    }
}
