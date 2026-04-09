package com.loyalty.service_admin.application.port.in;

import com.loyalty.service_admin.application.dto.user.UserCreateRequest;
import com.loyalty.service_admin.application.dto.user.UserResponse;

/**
 * Puerto de entrada para casos de uso de creación de usuarios.
 * Define el contrato para crear un nuevo usuario.
 *
 * Implementadores: UserCreateService
 */
public interface UserCreateUseCase {
    
    /**
     * Crea un nuevo usuario en el sistema.
     * 
     * Validaciones:
     * - Usuario autenticado debe tener rol SUPER_ADMIN o STORE_ADMIN
     * - El username NO debe existir (BadRequestException si existe)
     * - El email NO debe existir (BadRequestException si existe)
     * - El role debe existir (BadRequestException si no existe)
     * - El ecommerce debe existir (BadRequestException si no existe)
     * - STORE_ADMIN solo puede crear usuarios en su ecommerce (AuthorizationException)
     * - Password debe cumplir validaciones
     * 
     * Acciones:
     * - Hash password usando BCrypt
     * - Crear usuario
     * - Registrar auditoría (action="USER_CREATE")
     * - Retornar UserResponse
     * 
     * @param request datos del usuario a crear
     * @return UserResponse con datos creados
     * @throws com.loyalty.service_admin.infrastructure.exception.UnauthorizedException si usuario no autenticado
     * @throws com.loyalty.service_admin.infrastructure.exception.AuthorizationException si rol insuficiente o ecommerce distinto
     * @throws com.loyalty.service_admin.infrastructure.exception.BadRequestException si validación falla
     * @throws com.loyalty.service_admin.infrastructure.exception.ConflictException si email o username existen
     */
    UserResponse createUser(UserCreateRequest request);
}
