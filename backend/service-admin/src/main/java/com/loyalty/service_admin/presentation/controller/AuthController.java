package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.LoginRequest;
import com.loyalty.service_admin.application.dto.LoginResponse;
import com.loyalty.service_admin.application.dto.UserResponse;
import com.loyalty.service_admin.application.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador de autenticación.
 * Expone endpoints para login, logout y obtener datos del usuario actual.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * Endpoint de login.
     * Valida credenciales del usuario y retorna token JWT.
     * 
     * @param request debe contener username y password válidos
     * @return LoginResponse con token JWT y datos del usuario
     * 
     * Status:
     * - 200 OK: Login exitoso
     * - 400 Bad Request: Validación fallida (campos obligatorios)
     * - 401 Unauthorized: Credenciales inválidas o usuario inactivo
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.debug("Intento de login para usuario: {}", request.username());
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Endpoint de logout.
     * Invalida la sesión del usuario.
     * IMPORTANTE: En v1, el logout es principalmente frontend (limpiar localStorage).
     * En v2, se implementará token blacklist.
     * 
     * Auth requerida: sí (token en header Authorization: Bearer <token>)
     * 
     * @param authHeader header Authorization con token Bearer
     * @return 204 No Content (siempre exitoso)
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            authService.logout(token);
        }
        log.debug("Logout realizado (no hay rastreo de token actualmente)");
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Endpoint para obtener datos del usuario autenticado.
     * Valida el token y retorna información del usuario.
     * 
     * Auth requerida: sí (token en header Authorization: Bearer <token>)
     * 
     * @param authHeader header Authorization con token Bearer
     * @return UserResponse con datos del usuario
     * 
     * Status:
     * - 200 OK: Token válido
     * - 401 Unauthorized: Token inválido, expirado o usuario desactivado
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String token = authHeader.substring(7);
        UserResponse user = authService.getCurrentUser(token);
        return ResponseEntity.ok(user);
    }
}
