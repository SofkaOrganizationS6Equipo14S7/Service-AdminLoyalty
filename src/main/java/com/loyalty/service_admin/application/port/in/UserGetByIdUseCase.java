package com.loyalty.service_admin.application.port.in;

import com.loyalty.service_admin.application.dto.user.UserResponse;

import java.util.UUID;

/**
 * Puerto de entrada para casos de uso de obtención de usuario por ID.
 * Define el contrato para recuperar un usuario específico.
 *
 * Implementadores: UserGetByIdService
 */
public interface UserGetByIdUseCase {
    
    /**
     * Obtiene un usuario específico por su UID.
     * 
     * Validaciones:
     * - Usuario autenticado (UnauthorizedException si no)
     * - Usuario target existe (ResourceNotFoundException si no)
     * - Autorización según rol:
     *   - SUPER_ADMIN: acceso a cualquier usuario
     *   - STORE_ADMIN: solo usuarios de su ecommerce (AuthorizationException si no)
     *   - STANDARD: solo acceso a sí mismo (AuthorizationException si intenta leer otro)
     * 
     * Acciones:
     * - Validar autorización
     * - Recuperar usuario
     * - Retornar UserResponse
     * 
     * @param uid UUID del usuario a obtener
     * @return UserResponse con datos del usuario
     * @throws com.loyalty.service_admin.infrastructure.exception.UnauthorizedException si usuario no autenticado
     * @throws com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException si usuario no existe
     * @throws com.loyalty.service_admin.infrastructure.exception.AuthorizationException si sin permisos
     */
    UserResponse getUserById(UUID uid);
}
