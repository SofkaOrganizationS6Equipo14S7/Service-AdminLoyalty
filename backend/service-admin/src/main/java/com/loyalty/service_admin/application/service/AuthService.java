package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.LoginRequest;
import com.loyalty.service_admin.application.dto.LoginResponse;
import com.loyalty.service_admin.application.dto.UserResponse;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.domain.repository.UserRepository;
import com.loyalty.service_admin.infrastructure.exception.UnauthorizedException;
import com.loyalty.service_admin.infrastructure.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

/**
 * Servicio de autenticación.
 * 
 * Gestiona:
 * - Login: validación de credenciales, generación de JWT (RFC 7519 via jjwt)
 * - GetCurrentUser: validación de token, recuperación de datos del usuario
 * - Logout: invalidación del token (v1: logging, v2: blacklist)
 * 
 * Cumple con SPEC-001 v1.1:
 * - RN-05: JWT real con jjwt (Header.Payload.Signature), NO home-made tokens
 * - RN-08: Logout valida token ANTES de disparar el log
 * - RN-03/04: BCrypt para validación de passwords
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    
    /**
     * Autentica un usuario con credenciales y genera token JWT.
     * 
     * @param request debe contener username y password válidos
     * @return LoginResponse con token JWT (RFC 7519)
     * @throws UnauthorizedException si las credenciales son inválidas o el usuario está inactivo
     */
    public LoginResponse login(LoginRequest request) {
        log.debug("Intento de login para usuario: {}", request.username());
        
        // Buscar usuario por username
        UserEntity user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> {
                    log.warn("Intento de login fallido: usuario {} no encontrado", request.username());
                    return new UnauthorizedException("Credenciales inválidas");
                });
        
        // Validar que el usuario esté activo
        if (!user.getActive()) {
            log.warn("Intento de login fallido: usuario {} está inactivo", request.username());
            throw new UnauthorizedException("Credenciales inválidas");
        }
        
        // Validar password usando BCrypt (evita timing attacks)
        if (!BCrypt.checkpw(request.password(), user.getPassword())) {
            log.warn("Intento de login fallido: password incorrecto para usuario {}", request.username());
            throw new UnauthorizedException("Credenciales inválidas");
        }
        
        // Generar token JWT (RFC 7519) usando jjwt
        String token = jwtProvider.generateToken(user.getUsername(), user.getId(), user.getRole());
        
        log.info("Login exitoso para usuario: {}", user.getUsername());
        
        return new LoginResponse(token, "Bearer", user.getUsername(), user.getRole());
    }
    
    /**
     * Obtiene los datos del usuario autenticado a partir del token.
     * Valida que el token sea válido (firma + expiración).
     * 
     * @param token token JWT en formato "Bearer <token>" o sin prefijo
     * @return UserResponse con datos del usuario
     * @throws UnauthorizedException si el token es inválido, expirado o usuario no existe/inactivo
     */
    public UserResponse getCurrentUser(String token) {
        log.debug("Solicitando datos del usuario autenticado...");
        
        try {
            // Validar token (firma criptográfica + expiración)
            if (!jwtProvider.validateToken(token)) {
                log.warn("Token JWT inválido o expirado");
                throw new UnauthorizedException("Token no válido o expirado");
            }
            
            // Extraer username del token
            String username = jwtProvider.getUsernameFromToken(token);
            
            // Buscar usuario
            UserEntity user = userRepository.findByUsername(username)
                    .orElseThrow(() -> {
                        log.warn("Usuario {} no encontrado en BD", username);
                        return new UnauthorizedException("Usuario no válido");
                    });
            
            // Validar que usuario siga activo
            if (!user.getActive()) {
                log.warn("Usuario {} fue desactivado después de emitir token", username);
                throw new UnauthorizedException("Usuario desactivado");
            }
            
            log.info("Usuario actual retornado: {}", username);
            
            // Convertir Long id a UUID
            java.util.UUID uid = java.util.UUID.nameUUIDFromBytes(("user-" + user.getId()).getBytes());
            
            return new UserResponse(
                    uid,
                    user.getUsername(),
                    user.getRole(),
                    null, // email no está disponible aquí
                    user.getEcommerceId(),
                    user.getActive(),
                    user.getCreatedAt(),
                    user.getUpdatedAt()
            );
        } catch (UnauthorizedException e) {
            throw e;
        } catch (io.jsonwebtoken.JwtException e) {
            log.warn("Error procesando JWT: {}", e.getMessage());
            throw new UnauthorizedException("Token no válido o expirado");
        } catch (Exception e) {
            log.error("Error inesperado en getCurrentUser: {}", e.getMessage());
            throw new UnauthorizedException("Token no válido o expirado");
        }
    }
    
    /**
     * Logout del usuario.
     * 
     * v1: Valida el token ANTES de disparar el log (para identificar usuario)
     * Si token válido: registra logout exitoso con usuario identificado
     * Si token inválido: registra warning pero NO lanza excepción (responde 204 igual)
     * 
     * v2 (future): Token será añadido a blacklist en caché (Redis/Caffeine)
     * 
     * @param token token JWT del usuario que cierra sesión
     */
    public void logout(String token) {
        log.debug("Logout realizado...");
        
        try {
            // Validar y extraer username del token
            if (jwtProvider.validateToken(token)) {
                String username = jwtProvider.getUsernameFromToken(token);
                log.info("Logout exitoso para usuario: {}", username);
                // TODO: En v2, agregar token a blacklist
            } else {
                log.warn("Logout con token inválido");
                // No relanzar excepción, logout siempre devuelve 204
            }
        } catch (Exception e) {
            log.warn("Logout con token inválido: {}", e.getMessage());
            // No relanzar excepción, logout siempre devuelve 204 (stateless)
        }
    }
}
