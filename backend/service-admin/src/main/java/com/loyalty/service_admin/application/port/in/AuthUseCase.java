package com.loyalty.service_admin.application.port.in;

import com.loyalty.service_admin.application.dto.auth.LoginRequest;
import com.loyalty.service_admin.application.dto.auth.LoginResponse;
import com.loyalty.service_admin.application.dto.user.UserResponse;

/**
 * Puerto de entrada para casos de uso de autenticación.
 * 
 * Define las operaciones de autenticación que el controller debe usar.
 * Las implementaciones concretas (AuthServiceImpl) deben inyectar puertos de salida
 * (AuthPersistencePort, JwtPort) para coordinar persistencia y JWT.
 */
public interface AuthUseCase {

    /**
     * Autentica usuario con credenciales y genera JWT token
     * 
     * @param request contiene username y password
     * @return LoginResponse con JWT token válido
     * @throws com.loyalty.service_admin.infrastructure.exception.UnauthorizedException 
     *         si credenciales son inválidas o usuario inactivo
     */
    LoginResponse login(LoginRequest request);

    /**
     * Obtiene datos del usuario autenticado (validando token)
     * 
     * @param token JWT token RFC 7519
     * @return UserResponse con datos del usuario
     * @throws com.loyalty.service_admin.infrastructure.exception.UnauthorizedException 
     *         si token inválido, expirado o usuario no existe/inactivo
     */
    UserResponse getCurrentUser(String token);

    /**
     * Logout stateless (confirmación del lado servidor)
     * 
     * @param token JWT token a invalidar (lado cliente lo elimina del storage)
     */
    void logout(String token);
}
