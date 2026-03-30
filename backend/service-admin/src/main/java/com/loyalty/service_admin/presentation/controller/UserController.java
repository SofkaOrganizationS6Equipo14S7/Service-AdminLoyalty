package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.UserCreateRequest;
import com.loyalty.service_admin.application.dto.UserResponse;
import com.loyalty.service_admin.application.dto.UserUpdateRequest;
import com.loyalty.service_admin.application.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller para gestión de usuarios con control de acceso multi-contexto.
 * 
 * Endpoints:
 * - POST   /api/v1/users           — Crear usuario (SUPER_ADMIN y STORE_ADMIN)
 * - GET    /api/v1/users           — Listar usuarios (solo SUPER_ADMIN y STORE_ADMIN; STORE_USER rechazado)
 * - GET    /api/v1/users/{uid}     — Obtener un usuario (SUPER_ADMIN global, STORE_ADMIN su ecommerce, STORE_USER self-access)
 * - PUT    /api/v1/users/{uid}     — Actualizar usuario (roles específicos y field-level restrictions)
 * - DELETE /api/v1/users/{uid}     — Eliminar usuario (SUPER_ADMIN y STORE_ADMIN contexto-aware)
 * 
 * Implementa:
 * - SPEC-005: SUPERADMIN - Acceso Total a la Plataforma
 *   - HU-02.1: Crear STORE_ADMIN para ecommerce (SUPER_ADMIN)
 *   - RN-05: Gestión de usuarios por contexto de autorización (3 tiers)
 *   - RN-01: SUPER_ADMIN sin vinculación a ecommerce
 * - SPEC-003: Administración de Ecommerce por STORE_ADMIN (herencia)
 * 
 * Autorización por Rol (SPEC-005 RN-05):
 * - SUPER_ADMIN (Global):
 *   - Crear cualquier rol (SUPER_ADMIN, STORE_ADMIN, STORE_USER) en cualquier ecommerce
 *   - Listar todos los usuarios
 *   - Leer/Actualizar/Eliminar cualquier usuario
 * - STORE_ADMIN (Tenant):
 *   - Crear STORE_ADMIN o STORE_USER solo en su ecommerce
 *   - Listar usuarios solo de su ecommerce
 *   - Leer/Actualizar/Eliminar usuarios solo de su ecommerce
 * - STORE_USER (Self):
 *   - NO puede listar usuarios (403 Forbidden)
 *   - NO puede crear usuarios (403 Forbidden)
 *   - Puede leer/actualizar solo su propio perfil (no cambiar ecommerce_id ni active)
 *   - NO puede eliminar usuarios (403 Forbidden)
 * 
 * Notas de Implementación:
 * - AuthenticationFilter DEBE estar registrado como @Bean (no @Component)
 * - UserPrincipal almacena ecommerce_id para aislamiento automático
 * - TenantInterceptor valida autorización general en TODAS las operaciones (GET, POST, PUT, DELETE)
 * - SecurityContextHelper + UserService validan field-level restrictions (ecommerce_id, active, role)
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    
    private final UserService userService;
    
    /**
     * POST /api/v1/users
     * Crea un nuevo usuario con validaciones de rol y contexto.
     * 
     * SPEC-005 HU-02.1: Crear STORE_ADMIN para ecommerce
     * SPEC-005 RN-05: Gestión de usuarios por contexto de autorización
     * 
     * Autorización:
     * - SUPER_ADMIN: puede crear SUPER_ADMIN, STORE_ADMIN o STORE_USER
     *   - Si role == SUPER_ADMIN: ecommerce_id DEBE ser NULL (RN-01)
     *   - Si role != SUPER_ADMIN: ecommerce_id DEBE ser obligatorio
     * - STORE_ADMIN: puede crear STORE_ADMIN o STORE_USER SOLO en su ecommerce
     * - STORE_USER: 403 Forbidden (CRITERIO-2.1.3)
     * 
     * @param request datos del nuevo usuario (role, username, email, password, ecommerceId)
     * @return 201 Created con UserResponse{uid, username, email, role, ecommerceId, active, createdAt, updatedAt}
     * @throws AuthorizationException si no es SUPER_ADMIN/STORE_ADMIN o intenta crear fuera de su ecommerce
     * @throws BadRequestException si role inválido o ecommerce_id no cumple regla RN-01
     * @throws ConflictException si username/email duplicado globalmente
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        log.info("Creando usuario: {} en ecommerce: {}", request.username(), request.ecommerceId());
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * GET /api/v1/users
     * Lista usuarios según el contexto del usuario actual (SPEC-005 RN-05).
     * 
     * Autorización:
     * - SUPER_ADMIN: retorna todos los usuarios (opcionalmente filtrada por ecommerceId)
     * - STORE_ADMIN: retorna solo usuarios de su ecommerce (ignora parámetro ecommerceId)
     * - STORE_USER: 403 Forbidden (CRITERIO-2.2.2 - "Solo administradores")
     * 
     * @param ecommerceId parámetro de filtro (solo SUPER_ADMIN puede usarlo para filtrar otros ecommerces)
     * @return 200 OK con lista de UserResponse{uid, username, email, role, ecommerceId, active, createdAt, updatedAt}[]
     * @throws AuthorizationException si STORE_USER intenta acceder (CRITERIO-2.2.2)
     * @apiNote SPEC-005 HU-02.2: Listar usuarios por contexto
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> listUsers(
            @RequestParam(name = "ecommerceId", required = false) UUID ecommerceId) {
        log.info("Listando usuarios, filtro ecommerce: {}", ecommerceId);
        List<UserResponse> users = userService.listUsers(ecommerceId);
        return ResponseEntity.ok(users);
    }
    
    /**
     * GET /api/v1/users/{uid}
     * Obtiene un usuario por su UID con validaciones contexto-aware (SPEC-005 RN-05).
     * 
     * Autorización:
     * - SUPER_ADMIN: puede leer cualquier usuario
     * - STORE_ADMIN: puede leer usuarios solo de su ecommerce
     * - STORE_USER: puede leer solo su propio perfil (CRITERIO-2.3.1C - "Consultarel propio")
     * 
     * @param uid UUID del usuario a obtener
     * @return 200 OK con UserResponse{uid, username, email, role, ecommerceId, active, createdAt, updatedAt}
     * @throws AuthorizationException si STORE_ADMIN intenta leer usuario de otro ecommerce
     * @throws AuthorizationException si STORE_USER intenta leer a otro usuario
     * @throws NotFoundException si usuario no existe
     * @apiNote SPEC-005 CRITERIO-2.3.1: Leer usuarios por contexto
     */
    @GetMapping("/{uid}")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID uid) {
        log.info("Obteniendo usuario: {}", uid);
        UserResponse user = userService.getUserByUid(uid);
        return ResponseEntity.ok(user);
    }
    
    /**
     * PUT /api/v1/users/{uid}
     * Actualiza un usuario con validaciones contexto-aware (SPEC-005 RN-05).
     * 
     * Autorización y Restricciones por Rol:
     * - SUPER_ADMIN (Global):
     *   - Puede actualizar cualquier usuario
     *   - Puede cambiar username, email, password, ecommerce_id, active
     *   - Nota: No puede cambiar usuario SUPER_ADMIN a otro role (RN-01 protege via DB constraint)
     * - STORE_ADMIN (Tenant):
     *   - Puede actualizar usuarios de su ecommerce
     *   - Puede cambiar username, email, password
     *   - NO puede cambiar ecommerce_id (CRITERIO-2.3.1E)
     *   - NO puede cambiar active (CRITERIO-2.3.1F)
     * - STORE_USER (Self):
     *   - Puede actualizar solo su propio perfil
     *   - Puede cambiar solo su password
     *   - NO puede cambiar username, email, ecommerce_id (CRITERIO-2.3.1D, 2.3.1E)
     *   - NO puede cambiar active, role
     * 
     * @param uid UUID del usuario a actualizar
     * @param request datos a actualizar (campos opcionales: username, email, password, ecommerceId, active)
     * @return 200 OK con UserResponse actualizado
     * @throws AuthorizationException si no tiene permisos para actualizar al usuario o campos específicos
     * @throws BadRequestException si ecommerce_id se envía pero usuario no es SUPER_ADMIN
     * @throws NotFoundException si usuario no existe
     * @apiNote SPEC-005 CRITERIO-2.3.1: Actualizar usuarios por contexto con field-level restrictions
     */
    @PutMapping("/{uid}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID uid,
            @Valid @RequestBody UserUpdateRequest request) {
        log.info("Actualizando usuario: {} con datos: {}", uid, request);
        UserResponse updated = userService.updateUser(uid, request);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * DELETE /api/v1/users/{uid}
     * Elimina un usuario permanentemente con validaciones contexto-aware (SPEC-005 RN-05).
     * 
     * Autorización:
     * - SUPER_ADMIN: puede eliminar cualquier usuario (excepto a sí mismo)
     * - STORE_ADMIN: puede eliminar usuarios de su ecommerce (excepto a sí mismo)
     * - STORE_USER: 403 Forbidden (CRITERIO-2.4.1 - "Solo administradores")
     * 
     * Validaciones:
     * - Usuario NO puede eliminarse a sí mismo (403 - misma validación que su ecommerce)
     * - Eliminación es permanente (no soft delete)
     * 
     * @param uid UUID del usuario a eliminar
     * @return 204 No Content (eliminación exitosa)
     * @throws AuthorizationException si no es SUPER_ADMIN/STORE_ADMIN, o intenta eliminar a sí mismo o usuario de otro ecommerce
     * @throws NotFoundException si usuario no existe
     * @apiNote SPEC-005 CRITERIO-2.4.1: Eliminar usuarios por contexto
     */
    @DeleteMapping("/{uid}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID uid) {
        log.info("Eliminando usuario: {}", uid);
        userService.deleteUser(uid);
        return ResponseEntity.noContent().build();
    }
}
