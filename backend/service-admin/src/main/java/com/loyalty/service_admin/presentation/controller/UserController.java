package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.UserCreateRequest;
import com.loyalty.service_admin.application.dto.UserResponse;
import com.loyalty.service_admin.application.dto.UserUpdateRequest;
import com.loyalty.service_admin.application.dto.UpdateProfileRequest;
import com.loyalty.service_admin.application.dto.ChangePasswordRequest;
import com.loyalty.service_admin.application.dto.LoginResponse;
import com.loyalty.service_admin.application.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    
    private final UserService userService;
    
    /**
     * POST /api/v1/users
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
    
    /**
     * PUT /api/v1/users/me
     * Actualiza el perfil del usuario autenticado (nombre y email).
     * 
     * SPEC-004 HU-03: Actualizar mi información de perfil
     * CRITERIO-3.1: Actualización de nombre y email
     * 
     * El usuario puede cambiar:
     * - name: nombre completo (1-100 caracteres)
     * - email: dirección de email (debe ser único globalmente sin importar ecommerce)
     * 
     * Restricciones:
     * - No puede cambiar username (identificador de login)
     * - No puede cambiar role
     * - No puede cambiar ecommerce_id
     * - Email debe ser único globalmente (CRITERIO-3.3: validación global, no limitada al ecommerce)
     * - El cambio se registra en tabla de auditoría con timestamp y user UID
     * 
     * @param request datos a actualizar (name, email)
     * @return 200 OK con UserResponse actualizado {uid, username, email, name, role, ecommerceId, ...}
     * @throws BadRequestException si email inválido o nombre vacío
     * @throws ConflictException si email ya existe en otro usuario (409)
     * @throws AuthorizationException si no está autenticado
     */
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        log.info("Usuario autenticado actualizando su perfil");
        UserResponse updated = userService.updateProfile(request);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * PUT /api/v1/users/me/password
     * Cambia la contraseña del usuario autenticado.
     * 
     * SPEC-004 HU-03: Actualizar mi información de perfil
     * CRITERIO-3.2: Cambio seguro de contraseña
     * 
     * El usuario proporciona:
     * - currentPassword: contraseña actual (para validación) (CRITERIO-3.4)
     * - newPassword: nueva contraseña (mínimo 12 caracteres, mayúscula, minúscula, número)
     * - confirmPassword: confirmación (debe ser igual a newPassword)
     * 
     * Validaciones:
     * - currentPassword debe ser correcto (401 Unauthorized si falla) (CRITERIO-3.4)
     * - newPassword y confirmPassword deben coincidir (400) (CRITERIO-3.2)
     * - newPassword debe cumplir policy de complejidad
     * - El cambio se registra en tabla de auditoría
     * 
     * @param request datos (currentPassword, newPassword, confirmPassword)
     * @return 200 OK con nuevo JWT token generado automáticamente
     * @throws BadRequestException si nuevas contraseñas no coinciden (400)
     * @throws UnauthorizedException si contraseña actual es incorrecta (401) (CRITERIO-3.4)
     * @throws ConflictException si nueva contraseña es igual a la anterior
     * @throws AuthorizationException si no está autenticado
     */
    @PutMapping("/me/password")
    public ResponseEntity<LoginResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        log.info("Usuario autenticado cambiando su contraseña");
        LoginResponse response = userService.changePassword(request);
        return ResponseEntity.ok(response);
    }
}
