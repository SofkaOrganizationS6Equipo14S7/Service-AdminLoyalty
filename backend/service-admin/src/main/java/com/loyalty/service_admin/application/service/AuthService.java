package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.auth.LoginRequest;
import com.loyalty.service_admin.application.dto.auth.LoginResponse;
import com.loyalty.service_admin.application.dto.user.UserResponse;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.domain.repository.UserRepository;
import com.loyalty.service_admin.infrastructure.exception.UnauthorizedException;
import com.loyalty.service_admin.infrastructure.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    
    /**
     * @param request debe contener username y password válidos
     * @return LoginResponse con token JWT (RFC 7519)
     * @throws UnauthorizedException si las credenciales son inválidas o el usuario está inactivo
     */
    public LoginResponse login(LoginRequest request) {
        log.debug("Intento de login para usuario: {}", request.username());
        
        UserEntity user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> {
                    log.warn("Intento de login fallido: usuario {} no encontrado", request.username());
                    return new UnauthorizedException("Credenciales inválidas");
                });
        
        if (!user.getIsActive()) {
            log.warn("Intento de login fallido: usuario {} está inactivo", request.username());
            throw new UnauthorizedException("Credenciales inválidas");
        }
        
        if (!BCrypt.checkpw(request.password(), user.getPasswordHash())) {
            log.warn("Intento de login fallido: password incorrecto para usuario {}", request.username());
            throw new UnauthorizedException("Credenciales inválidas");
        }
        
        String token = jwtProvider.generateToken(
            user.getUsername(), 
            user.getId(), 
            user.getRole().getName(),
            user.getEcommerceId()
        );
        
        log.info("Login exitoso para usuario: {} con role: {} y ecommerce_id: {}", 
                user.getUsername(), user.getRole().getName(), user.getEcommerceId());
        
        return new LoginResponse(token, "Bearer", user.getUsername(), user.getRole().getName());
    }
    
    /**
     * @param token token JWT en formato "Bearer <token>" o sin prefijo
     * @return UserResponse con datos del usuario
     * @throws UnauthorizedException si el token es inválido, expirado o usuario no existe/inactivo
     */
    public UserResponse getCurrentUser(String token) {
        log.debug("Solicitando datos del usuario autenticado...");
        
        try {
            if (!jwtProvider.validateToken(token)) {
                log.warn("Token JWT inválido o expirado");
                throw new UnauthorizedException("Token no válido o expirado");
            }
            
            String username = jwtProvider.getUsernameFromToken(token);
            
            UserEntity user = userRepository.findByUsername(username)
                    .orElseThrow(() -> {
                        log.warn("Usuario {} no encontrado en BD", username);
                        return new UnauthorizedException("Usuario no válido");
                    });
            
            if (!user.getIsActive()) {
                log.warn("Usuario {} fue desactivado después de emitir token", username);
                throw new UnauthorizedException("Usuario desactivado");
            }
            
            log.info("Usuario actual retornado: {}", username);
            
            return new UserResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getRoleId(),
                    user.getRole().getName(),
                    user.getEmail(),
                    user.getEcommerceId(),
                    user.getIsActive(),
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
