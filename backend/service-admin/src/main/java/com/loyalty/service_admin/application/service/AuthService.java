package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.LoginRequest;
import com.loyalty.service_admin.application.dto.LoginResponse;
import com.loyalty.service_admin.application.dto.UserResponse;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.domain.repository.UserRepository;
import com.loyalty.service_admin.infrastructure.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * Servicio de autenticación.
 * Gestiona login, logout y validación de tokens.
 * 
 * IMPORTANTE: En esta versión v1, los tokens NO se invalidan en backend.
 * El logout se realiza limpiando localStorage en el frontend.
 * En futuras versiones se implementará token blacklist.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    
    @Value("${app.jwt.secret:loyalty-secret-key}")
    private String secret;
    
    /**
     * Autentica un usuario con credenciales y genera token JWT.
     * 
     * @param request debe contener username y password válidos
     * @return LoginResponse con token JWT
     * @throws UnauthorizedException si las credenciales son inválidas o el usuario está inactivo
     */
    public LoginResponse login(LoginRequest request) {
        // Buscar usuario por username
        UserEntity user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> {
                    log.warn("Intento de login fallido: usuario {} no encontrado", request.username());
                    return new UnauthorizedException("Usuario no válido");
                });
        
        // Validar que el usuario esté activo
        if (!user.getActive()) {
            log.warn("Intento de login fallido: usuario {} está inactivo", request.username());
            throw new UnauthorizedException("Usuario no válido");
        }
        
        // Validar password (comparación plain text en v1, será hashing en v2)
        if (!user.getPassword().equals(request.password())) {
            log.warn("Intento de login fallido: password incorrecto para usuario {}", request.username());
            throw new UnauthorizedException("Credenciales inválidas");
        }
        
        // Generar token JWT (formato: username:role:timestamp:secret)
        String payload = user.getUsername() + ":" + user.getRole() + ":" + Instant.now().toEpochMilli();
        String token = Base64.getEncoder()
                .encodeToString((payload + ":" + secret).getBytes(StandardCharsets.UTF_8));
        
        log.info("Login exitoso para usuario: {}", user.getUsername());
        
        return new LoginResponse(token, "Bearer", user.getUsername(), user.getRole());
    }
    
    /**
     * Obtiene los datos del usuario autenticado a partir del token.
     * Valida que el token sea válido y no esté expirado.
     * 
     * @param token token JWT en formato Bearer
     * @return UserResponse con datos del usuario
     * @throws UnauthorizedException si el token es inválido o expirado
     */
    public UserResponse getCurrentUser(String token) {
        try {
            // Extraer "Bearer " del token si está presente
            String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            
            // Decodificar token
            String decoded = new String(Base64.getDecoder().decode(cleanToken), StandardCharsets.UTF_8);
            
            // Formato: username:role:timestamp:secret
            String[] parts = decoded.split(":");
            if (parts.length != 4) {
                throw new UnauthorizedException("Formato de token inválido");
            }
            
            String username = parts[0];
            String role = parts[1];
            long timestamp = Long.parseLong(parts[2]);
            String tokenSecret = parts[3];
            
            // Validar secret
            if (!tokenSecret.equals(secret)) {
                throw new UnauthorizedException("Token no válido o expirado");
            }
            
            // Validar que no haya expirado (24 horas = 86400000 ms)
            long now = Instant.now().toEpochMilli();
            long tokenAge = now - timestamp;
            if (tokenAge > 86400000) { // 24 horas
                log.warn("Token expirado para usuario: {}", username);
                throw new UnauthorizedException("Token expirado");
            }
            
            // Buscar usuario
            UserEntity user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UnauthorizedException("Usuario no válido"));
            
            // Validar que siga activo
            if (!user.getActive()) {
                throw new UnauthorizedException("Usuario desactivado");
            }
            
            return new UserResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getRole(),
                    user.getActive(),
                    user.getCreatedAt(),
                    user.getUpdatedAt()
            );
        } catch (IllegalArgumentException e) {
            // Incluye NumberFormatException y otros errores de decodificación
            log.warn("Error decodificando o procesando token: {}", e.getMessage());
            throw new UnauthorizedException("Token no válido o expirado");
        }
    }
    
    /**
     * Logout del usuario.
     * En esta versión v1, simplemente registra el logout.
     * En futuras versiones, agregará el token a una blacklist.
     * 
     * @param token token JWT del usuario que cierra sesión
     */
    public void logout(String token) {
        try {
            UserResponse user = getCurrentUser(token);
            log.info("Logout exitoso para usuario: {}", user.username());
            // TODO: En v2, agregar token a blacklist
        } catch (Exception e) {
            log.warn("Logout con token inválido: {}", e.getMessage());
            // No relanzar excepción, logout siempre devuelve 204
        }
    }
}
