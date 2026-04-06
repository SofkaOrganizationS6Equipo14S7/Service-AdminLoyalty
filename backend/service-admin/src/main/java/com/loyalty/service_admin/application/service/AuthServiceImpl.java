package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.auth.LoginRequest;
import com.loyalty.service_admin.application.dto.auth.LoginResponse;
import com.loyalty.service_admin.application.dto.user.UserResponse;
import com.loyalty.service_admin.application.port.in.AuthUseCase;
import com.loyalty.service_admin.application.port.out.AuthPersistencePort;
import com.loyalty.service_admin.application.port.out.JwtPort;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.infrastructure.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

/**
 * Implementación de AuthUseCase siguiendo arquitectura hexagonal.
 * 
 * Coordina:
 * - AuthPersistencePort: acceso a datos de usuario
 * - JwtPort: generación y validación de JWT
 * 
 * Contiene la lógica de negocio de autenticación.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthUseCase {

    private final AuthPersistencePort authPersistencePort;
    private final JwtPort jwtPort;

    /**
     * Autentica usuario con credenciales (username + password)
     * 
     * Flujo:
     * 1. Buscar usuario por username en BD (puerto persistencia)
     * 2. Validar que usuario existe y está activo
     * 3. Validar password contra hash BCrypt
     * 4. Generar JWT token (puerto JWT)
     * 5. Retornar LoginResponse
     * 
     * @param request LoginRequest(username, password)
     * @return LoginResponse con JWT token válido
     * @throws UnauthorizedException si credenciales son inválidas o usuario inactivo
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        log.debug("Intento de login para usuario: {}", request.username());

        // 1. Buscar usuario por username
        UserEntity user = authPersistencePort.findByUsername(request.username())
                .orElseThrow(() -> {
                    log.warn("Intento de login fallido: usuario {} no encontrado", request.username());
                    return new UnauthorizedException("Credenciales inválidas");
                });

        // 2. Validar que usuario está activo
        if (!user.getIsActive()) {
            log.warn("Intento de login fallido: usuario {} está inactivo", request.username());
            throw new UnauthorizedException("Credenciales inválidas");
        }

        // 3. Validar password
        if (!BCrypt.checkpw(request.password(), user.getPasswordHash())) {
            log.warn("Intento de login fallido: password incorrecto para usuario {}", request.username());
            throw new UnauthorizedException("Credenciales inválidas");
        }

        // 4. Generar JWT token usando puerto JWT
        String token = jwtPort.generateToken(
                user.getUsername(),
                user.getId(),
                user.getRole().getName(),
                user.getEcommerceId()
        );

        log.info("Login exitoso para usuario: {} con role: {} y ecommerce_id: {}",
                user.getUsername(), user.getRole().getName(), user.getEcommerceId());

        // 5. Retornar respuesta
        return new LoginResponse(token, "Bearer", user.getUsername(), user.getRole().getName());
    }

    /**
     * Obtiene datos del usuario autenticado validando el token JWT
     * 
     * Flujo:
     * 1. Validar token (puerto JWT)
     * 2. Extraer username del token
     * 3. Buscar usuario en BD (puerto persistencia)
     * 4. Validar que usuario existe y está activo
     * 5. Retornar UserResponse
     * 
     * @param token JWT token
     * @return UserResponse con datos del usuario
     * @throws UnauthorizedException si token inválido o usuario no existe/inactivo
     */
    @Override
    public UserResponse getCurrentUser(String token) {
        log.debug("Solicitando datos del usuario autenticado...");

        try {
            // 1. Validar token
            if (!jwtPort.validateToken(token)) {
                log.warn("Token JWT inválido o expirado");
                throw new UnauthorizedException("Token no válido o expirado");
            }

            // 2. Extraer username del token
            String username = jwtPort.getUsernameFromToken(token);

            // 3. Buscar usuario en BD
            UserEntity user = authPersistencePort.findByUsername(username)
                    .orElseThrow(() -> {
                        log.warn("Usuario {} no encontrado en BD", username);
                        return new UnauthorizedException("Usuario no válido");
                    });

            // 4. Validar que usuario está activo
            if (!user.getIsActive()) {
                log.warn("Usuario {} fue desactivado después de emitir token", username);
                throw new UnauthorizedException("Usuario desactivado");
            }

            log.info("Usuario actual retornado: {}", username);

            // 5. Retornar respuesta
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
        } catch (Exception e) {
            log.error("Error inesperado en getCurrentUser: {}", e.getMessage());
            throw new UnauthorizedException("Token no válido o expirado");
        }
    }

    /**
     * Logout stateless (solo confirmación del lado servidor)
     * 
     * En arquitectura stateless (JWT), el logout se maneja del lado cliente
     * eliminando el token del localStorage. Este método solo valida y loguea.
     * 
     * @param token JWT token
     */
    @Override
    public void logout(String token) {
        log.debug("Logout realizado...");

        try {
            if (jwtPort.validateToken(token)) {
                String username = jwtPort.getUsernameFromToken(token);
                log.info("Logout exitoso para usuario: {}", username);
            } else {
                log.warn("Logout con token inválido");
            }
        } catch (Exception e) {
            log.warn("Logout con token inválido: {}", e.getMessage());
        }
    }
}
