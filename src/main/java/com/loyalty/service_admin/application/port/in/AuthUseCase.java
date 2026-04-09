package com.loyalty.service_admin.application.port.in;

import com.loyalty.service_admin.application.dto.auth.LoginRequest;
import com.loyalty.service_admin.application.dto.auth.LoginResponse;
import com.loyalty.service_admin.application.dto.user.UserResponse;

public interface AuthUseCase {

    /**
     * Autentica usuario con credenciales y genera JWT token
     *
     * @param request contiene username y password
     * @return LoginResponse con JWT token válido
     * @throws com.loyalty.service_admin.infrastructure.exception.UnauthorizedException 
     */
    LoginResponse login(LoginRequest request);

    /**
     * Obtiene datos del usuario autenticado (validando token)
     * 
     * @param token JWT token RFC 7519
     * @return UserResponse con datos del usuario
     * @throws com.loyalty.service_admin.infrastructure.exception.UnauthorizedException 
     */
    UserResponse getCurrentUser(String token);

    /**
     * Logout stateless (confirmación del lado servidor)
     * 
     * @param token JWT token a invalidar (lado cliente lo elimina del storage)
     */
    void logout(String token);
}
