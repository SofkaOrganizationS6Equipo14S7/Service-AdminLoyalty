package com.loyalty.service_admin.application.port.in;

import com.loyalty.service_admin.application.dto.user.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Puerto de entrada para casos de uso de listado de usuarios.
 * Define el contrato para recuperar usuarios con paginación y filtros.
 *
 * Implementadores: UserListService
 */
public interface UserListUseCase {
    
    /**
     * Lista usuarios del sistema con soporte para paginación y filtrado.
     * 
     * Validaciones:
     * - Usuario autenticado (UnauthorizedException si no)
     * - Si STORE_ADMIN: solo usuarios de su ecommerce
     * - Si SUPER_ADMIN: todos los usuarios
     * - Si STANDARD: lanza AuthorizationException (no puede listar usuarios)
     * 
     * Acciones:
     * - Recuperar usuarios según rol
     * - Retornar Page<UserResponse> paginada
     * 
     * @param ecommerceId filtro opcional por ecommerce (ignorado si no es SUPER_ADMIN)
     * @param pageable información de paginación
     * @return Page<UserResponse> con usuarios paginados
     * @throws com.loyalty.service_admin.infrastructure.exception.UnauthorizedException si usuario no autenticado
     * @throws com.loyalty.service_admin.infrastructure.exception.AuthorizationException si intenta acceso no autorizado
     */
    Page<UserResponse> listUsers(UUID ecommerceId, Pageable pageable);
}
