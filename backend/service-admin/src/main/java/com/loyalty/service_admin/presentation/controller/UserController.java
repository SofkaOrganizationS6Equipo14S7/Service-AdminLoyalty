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
 * Controller para gestión de usuarios por ecommerce.
 * 
 * Endpoints:
 * - POST   /api/v1/users           — Crear usuario (solo SUPER_ADMIN)
 * - GET    /api/v1/users           — Listar usuarios (filtrado según ecommerce del usuario)
 * - GET    /api/v1/users/{uid}     — Obtener un usuario
 * - PUT    /api/v1/users/{uid}     — Actualizar usuario (solo SUPER_ADMIN)
 * - DELETE /api/v1/users/{uid}     — Eliminar usuario (solo SUPER_ADMIN)
 * 
 * Implementa SPEC-002: Gestión de Usuarios por Ecommerce
 * - HU-01: Crear usuario vinculado a ecommerce
 * - HU-02: Validar acceso según ecommerce del usuario
 * - HU-03: Listar usuarios por ecommerce
 * - HU-04: Actualizar usuario (cambio de ecommerce)
 * - HU-05: Eliminar usuario
 * 
 * Notas de Implementación:
 * - AuthenticationFilter DEBE estar registrado como @Bean (no @Component)
 * - UserPrincipal almacena ecommerce_id para aislamiento automático
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    
    private final UserService userService;
    
    /**
     * POST /api/v1/users
     * Crea un nuevo usuario vinculado a un ecommerce.
     * 
     * Requiere: rol SUPER_ADMIN
     * 
     * @param request datos del nuevo usuario
     * @return 201 Created con UserResponse
     * @apiNote CRITERIO-1.1: Crear usuario exitosamente con ecommerce válido
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        log.info("Creando usuario: {} en ecommerce: {}", request.username(), request.ecommerceId());
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * GET /api/v1/users
     * Lista usuarios según el contexto del usuario actual.
     * 
     * - Si no es super admin: retorna solo usuarios de su ecommerce
     * - Si es super admin: retorna todos (o filtra por parámetro ecommerceId)
     * 
     * @param ecommerceId parámetro de filtro (opcional, solo super admin)
     * @return 200 OK con lista de UserResponse
     * @apiNote CRITERIO-2.1, 2.2, 3.1, 3.2: Listar usuarios con aislamiento
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
     * Obtiene un usuario por su UID.
     * 
     * Valida que el usuario actual tiene permiso (mismo ecommerce o super admin).
     * 
     * @param uid UUID del usuario
     * @return 200 OK con UserResponse
     * @apiNote CRITERIO-2.1: Usuario valida acceso según ecommerce
     */
    @GetMapping("/{uid}")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID uid) {
        log.info("Obteniendo usuario: {}", uid);
        UserResponse user = userService.getUserByUid(uid);
        return ResponseEntity.ok(user);
    }
    
    /**
     * PUT /api/v1/users/{uid}
     * Actualiza un usuario (solo username y ecommerceId).
     * 
     * Requiere: rol SUPER_ADMIN
     * 
     * @param uid UUID del usuario
     * @param request datos a actualizar (campos opcionales)
     * @return 200 OK con UserResponse actualizado
     * @apiNote CRITERIO-4.1, 4.2: Actualizar usuario con validaciones
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
     * Elimina un usuario permanentemente.
     * 
     * Requiere: rol SUPER_ADMIN
     * Validaciones:
     * - Usuario no puede eliminarse a sí mismo
     * 
     * @param uid UUID del usuario
     * @return 204 No Content
     * @apiNote CRITERIO-5.1, 5.2: Eliminar con validaciones
     */
    @DeleteMapping("/{uid}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID uid) {
        log.info("Eliminando usuario: {}", uid);
        userService.deleteUser(uid);
        return ResponseEntity.noContent().build();
    }
}
