package com.loyalty.service_admin.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Proveedor de JWT usando librería jjwt (RFC 7519).
 * 
 * Responsabilidades:
 * - Generación de tokens JWT con claims: username, userId, role, iat, exp
 * - Validación de tokens (firma + expiración)
 * - Extracción de claims individuales
 * 
 * Cumple con SPEC-001 v1.1 RN-05 (JWT real con jjwt, NO home-made tokens)
 */
@Component
@Slf4j
public class JwtProvider {
    
    private final String secret;
    private final long expirationMs;
    private final SecretKey key;
    
    public JwtProvider(
        @Value("${app.jwt.secret:loyalty-secret-key-v1-please-change-in-production}") String secret,
        @Value("${app.jwt.expiration:86400000}") long expirationMs
    ) {
        if (secret == null || secret.length() < 32) {
            log.warn("⚠️ JWT secret length < 32 chars. Minimum 256 bits recommended for HMAC-SHA256");
        }
        this.secret = secret;
        this.expirationMs = expirationMs;
        // SecretKey generada a partir del secret (mínimo 256 bits para HS256)
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Genera un token JWT con claims: username, userId, role, ecommerce_id, iat, exp.
     * Estructura: Header.Payload.Signature (RFC 7519)
     * 
     * Implementa SPEC-002 punto 4: Propagación de ecommerce_id en JWT para aislamiento multi-tenant.
     * 
     * @param username del usuario
     * @param userId id único del usuario
     * @param role rol del usuario (ej. "ADMIN")
     * @param ecommerceId UUID del ecommerce (null si super admin)
     * @return JWT firmado y codificado
     */
    public String generateToken(String username, Long userId, String role, java.util.UUID ecommerceId) {
        return generateTokenFull(null, username, userId, role, ecommerceId);
    }
    
    /**
     * Versión completa de generateToken que incluye uid.
     * Usada cuando se dispone del uid desde UserEntity (posterior a @PrePersist).
     * 
     * @param uid UUID único del usuario (generado en @PrePersist de UserEntity)
     * @param username del usuario
     * @param userId id numérico del usuario
     * @param role rol del usuario  
     * @param ecommerceId UUID del ecommerce (null si super admin)
     * @return JWT firmado con uid claim
     */
    public String generateTokenFull(java.util.UUID uid, String username, Long userId, String role, java.util.UUID ecommerceId) {
        long nowMs = System.currentTimeMillis();
        long expiryMs = nowMs + expirationMs;
        
        try {
            var builder = Jwts.builder()
                // Header: alg=HS256, typ=JWT (automático)
                // Payload: claims
                .setSubject(username)                       // "sub" claim
                .claim("userId", userId)                    // custom claim
                .claim("role", role)                        // custom claim
                .setIssuedAt(new Date(nowMs))               // "iat" claim
                .setExpiration(new Date(expiryMs));         // "exp" claim
            
            // Agregar uid si está disponible (SPEC-002 CRITERIO-3.1)
            if (uid != null) {
                builder.claim("uid", uid.toString());
            }
            
            // Agregar ecommerce_id si no es super admin
            if (ecommerceId != null) {
                builder.claim("ecommerce_id", ecommerceId.toString());  // custom claim para multi-tenant
            }
            
            String token = builder
                // Signature: HMAC-SHA256 con secret key
                .signWith(key)
                .compact();
            
            log.debug("JWT generado exitosamente para usuario: {} (uid: {}) con ecommerce_id: {}", username, uid, ecommerceId);
            return token;
        } catch (Exception e) {
            log.error("Error generando JWT para usuario {}: {}", username, e.getMessage());
            throw new RuntimeException("Error generando token JWT", e);
        }
    }
    
    /**
     * Valida que el token JWT sea válido.
     * Verifica: firma criptográfica + expiración.
     * 
     * @param token JWT a validar
     * @return true si válido, false si expirado o inválido
     */
    public boolean validateToken(String token) {
        try {
            String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(cleanToken);
            
            log.debug("Token JWT válido");
            return true;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("Token JWT expirado: {}", e.getMessage());
            return false;
        } catch (io.jsonwebtoken.JwtException e) {
            log.warn("Token JWT inválido: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("Error validando token JWT: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Extrae el username (subject) del token.
     * 
     * @param token JWT
     * @return username desde el claim "sub"
     */
    public String getUsernameFromToken(String token) {
        try {
            String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(cleanToken)
                .getBody();
            
            String username = claims.getSubject();
            log.debug("Username extraído del token: {}", username);
            return username;
        } catch (Exception e) {
            log.warn("Error extrayendo username del token: {}", e.getMessage());
            throw new io.jsonwebtoken.JwtException("Error extrayendo username del token", e);
        }
    }
    
    /**
     * Extrae el userId (custom claim) del token.
     * 
     * @param token JWT
     * @return userId
     */
    public Long getUserIdFromToken(String token) {
        try {
            String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(cleanToken)
                .getBody();
            
            return claims.get("userId", Long.class);
        } catch (Exception e) {
            log.warn("Error extrayendo userId del token: {}", e.getMessage());
            throw new io.jsonwebtoken.JwtException("Error extrayendo userId del token", e);
        }
    }
    
    /**
     * Extrae el uid (custom claim) del token.
     * Implementa SPEC-002 CRITERIO-3.1: uid como identificador público.
     * 
     * @param token JWT
     * @return UUID del usuario, o null si no está presente (tokens generados antes de esta feature)
     */
    public java.util.UUID getUidFromToken(String token) {
        try {
            String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(cleanToken)
                .getBody();
            
            String uidStr = claims.get("uid", String.class);
            if (uidStr == null) {
                log.debug("uid no presente en token (probablemente generado antes de SPEC-002)");
                return null;
            }
            
            return java.util.UUID.fromString(uidStr);
        } catch (Exception e) {
            log.debug("Error extrayendo uid del token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extrae el role (custom claim) del token.
     * 
     * @param token JWT
     * @return role
     */
    public String getRoleFromToken(String token) {
        try {
            String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(cleanToken)
                .getBody();
            
            return claims.get("role", String.class);
        } catch (Exception e) {
            log.warn("Error extrayendo role del token: {}", e.getMessage());
            throw new io.jsonwebtoken.JwtException("Error extrayendo role del token", e);
        }
    }
    
    /**
     * Extrae el ecommerce_id (custom claim) del token.
     * Retorna null si el usuario es SUPER_ADMIN (sin restricción de ecommerce).
     * 
     * Implementa SPEC-002 punto 4: Propagación de ecommerce_id en JWT
     * 
     * @param token JWT
     * @return UUID del ecommerce, o null si super admin
     */
    public java.util.UUID getEcommerceIdFromToken(String token) {
        try {
            String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(cleanToken)
                .getBody();
            
            String ecommerceIdStr = claims.get("ecommerce_id", String.class);
            if (ecommerceIdStr == null) {
                log.debug("ecommerce_id no presente en token (usuario probablemente SUPER_ADMIN)");
                return null;
            }
            
            return java.util.UUID.fromString(ecommerceIdStr);
        } catch (Exception e) {
            log.debug("ecommerce_id no encontrado en token o formato inválido: {}", e.getMessage());
            return null; // No es error crítico, usuarios super admin no tienen este claim
        }
    }
}

