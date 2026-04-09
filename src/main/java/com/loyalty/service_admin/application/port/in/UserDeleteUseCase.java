package com.loyalty.service_admin.application.port.in;

import java.util.UUID;

/**
 * Puerto de entrada para casos de uso de eliminación de usuarios.
 * Define el contrato para hard delete de usuarios.
 *
 * Implementadores: UserDeleteService
 */
public interface UserDeleteUseCase {
    
    /**
     * Elimina permanentemente un usuario de forma física de la base de datos.
     * 
     * Validaciones:
     * - Usuario autenticado debe tener rol SUPER_ADMIN o STORE_ADMIN
     * - No puede auto-eliminarse (uid != currentUserUid)
     * - STORE_ADMIN solo puede eliminar usuarios de su ecommerce
     * - Usuario target debe existir
     * 
     * Acciones:
     * - Registra auditoría previa (auditUserDeletion)
     * - Ejecuta hard delete físico
     * - Log info de ejecución
     * 
     * @param uid UUID del usuario a eliminar
     * @throws com.loyalty.service_admin.infrastructure.exception.UnauthorizedException si usuario no autenticado
     * @throws com.loyalty.service_admin.infrastructure.exception.AuthorizationException si rol insuficiente o ecommerce distinto
     * @throws com.loyalty.service_admin.infrastructure.exception.BadRequestException si intenta auto-eliminarse
     * @throws com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException si usuario no existe
     */
    void hardDeleteUser(UUID uid);
}
