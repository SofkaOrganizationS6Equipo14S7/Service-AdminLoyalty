package com.loyalty.service_admin.application.port.in;

import com.loyalty.service_admin.application.dto.user.UserResponse;
import com.loyalty.service_admin.application.dto.user.UserUpdateRequest;

import java.util.UUID;

/**
 * Puerto de entrada para casos de uso de actualización de usuarios.
 * Define el contrato para modificar datos de un usuario existente.
 *
 * Implementadores: UserUpdateService
 */
public interface UserUpdateUseCase {
    
    /**
     * Actualiza datos de un usuario existente.
     * 
     * Validaciones:
     * - Usuario autenticado (UnauthorizedException si no)
     * - Usuario target existe (ResourceNotFoundException si no)
     * - Autorización según rol (AuthorizationException si sin permisos)
     * - Si cambian email: verificar que NO existe otro usuario con ese email (BadRequestException)
     * - Si cambian username: verificar que NO existe otro usuario con ese username (BadRequestException)
     * - Si cambian role: validar que rol existe (BadRequestException)
     * - STORE_ADMIN NO puede cambiar su propio role (BadRequestException)
     * - NO se puede cambiar roleId (siempre es BadRequestException)
     * 
     * Acciones:
     * - Validar autorización
     * - Validar cambios
     * - Ejecutar update
     * - Registrar auditoría (action="USER_UPDATE", capturar old_value y new_value)
     * - Retornar UserResponse actualizado
     * 
     * @param uid UUID del usuario a actualizar
     * @param request datos a actualizar
     * @return UserResponse con datos actualizados
     * @throws com.loyalty.service_admin.infrastructure.exception.UnauthorizedException si usuario no autenticado
     * @throws com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException si usuario no existe
     * @throws com.loyalty.service_admin.infrastructure.exception.AuthorizationException si sin permisos
     * @throws com.loyalty.service_admin.infrastructure.exception.BadRequestException si validación falla
     * @throws com.loyalty.service_admin.infrastructure.exception.ConflictException si email/username existen
     */
    UserResponse updateUser(UUID uid, UserUpdateRequest request);
}
