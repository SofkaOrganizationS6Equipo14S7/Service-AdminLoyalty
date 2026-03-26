package com.loyalty.service_engine.infrastructure.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * Utilidad para validar y extraer datos de JWT (JSON Web Tokens).
 * 
 * Formato JWT (Base64 encoded):
 * Username:Role:Timestamp:Secret
 * 
 * Ejemplo: admin:ADMIN:1743134400000:loyalty-secret-key
 */
@Component
@Slf4j
public class JwtUtil {
    
    @Value("${app.jwt.secret:loyalty-secret-key}")
    private String secret;
    
    @Value("${app.jwt.expiry:86400000}")  // 24 horas por defecto
    private long expiryMs;
    
    /**
     * Valida un token JWT.
     * 
     * @param token token JWT (sin "Bearer " prefix)
     * @return true si el token es válido, false en caso contrario
     */
    public boolean validateToken(String token) {
        try {
            String decoded = new String(
                Base64.getDecoder().decode(token),
                StandardCharsets.UTF_8
            );
            
            String[] parts = decoded.split(":");
            if (parts.length != 4) {
                log.warn("Token tiene formato inválido. Partes: {}", parts.length);
                return false;
            }
            
            String tokenSecret = parts[3];
            if (!tokenSecret.equals(secret)) {
                log.warn("Secret del token no coincide");
                return false;
            }
            
            // Validar expiración (24 horas)
            long timestamp = Long.parseLong(parts[2]);
            long now = Instant.now().toEpochMilli();
            long tokenAge = now - timestamp;
            
            if (tokenAge > expiryMs) {
                log.warn("Token expirado. Edad: {}", tokenAge);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.warn("Error validando token: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Extrae el username del token.
     * 
     * @param token token JWT (sin "Bearer " prefix)
     * @return username del usuario
     * @throws IllegalArgumentException si el token es inválido
     */
    public String extractUsername(String token) {
        String decoded = decode(token);
        String[] parts = decoded.split(":");
        if (parts.length < 1) {
            throw new IllegalArgumentException("Token no contiene username");
        }
        return parts[0];
    }
    
    /**
     * Extrae el role del token.
     * 
     * @param token token JWT (sin "Bearer " prefix)
     * @return role del usuario
     * @throws IllegalArgumentException si el token es inválido
     */
    public String extractRole(String token) {
        String decoded = decode(token);
        String[] parts = decoded.split(":");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Token no contiene role");
        }
        return "ROLE_" + parts[1];  // Spring Security prefija con ROLE_
    }
    
    /**
     * Extrae el timestamp del token.
     * 
     * @param token token JWT (sin "Bearer " prefix)
     * @return timestamp en milisegundos
     * @throws IllegalArgumentException si el token es inválido
     */
    public long extractTimestamp(String token) {
        String decoded = decode(token);
        String[] parts = decoded.split(":");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Token no contiene timestamp");
        }
        return Long.parseLong(parts[2]);
    }
    
    /**
     * Decodifica un token JWT desde Base64.
     * 
     * @param token token JWT (sin "Bearer " prefix)
     * @return decoded payload
     * @throws IllegalArgumentException si la decodificación falla
     */
    private String decode(String token) {
        try {
            return new String(
                Base64.getDecoder().decode(token),
                StandardCharsets.UTF_8
            );
        } catch (IllegalArgumentException e) {
            log.warn("Error decodificando token: {}", e.getMessage());
            throw e;
        }
    }
}
