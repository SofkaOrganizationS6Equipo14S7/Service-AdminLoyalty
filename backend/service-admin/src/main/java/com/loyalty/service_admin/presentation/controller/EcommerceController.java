package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.EcommerceCreateRequest;
import com.loyalty.service_admin.application.dto.EcommerceResponse;
import com.loyalty.service_admin.application.dto.EcommerceUpdateStatusRequest;
import com.loyalty.service_admin.application.service.EcommerceService;
import com.loyalty.service_admin.infrastructure.exception.AuthorizationException;
import com.loyalty.service_admin.infrastructure.security.SecurityContextHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller para gestión de ecommerces (multi-tenant).
 * 
 * SPEC-001: Registro y Gestión de Ecommerces
 * 
 * Endpoints:
 * - POST   /api/v1/ecommerces              → Crear ecommerce (SUPER_ADMIN)
 * - GET    /api/v1/ecommerces              → Listar ecommerces (SUPER_ADMIN)
 * - GET    /api/v1/ecommerces/{uid}        → Obtener ecommerce por uid (SUPER_ADMIN)
 * - PUT    /api/v1/ecommerces/{uid}        → Actualizar status (SUPER_ADMIN)
 * 
 * Autenticación:
 * - Requerida: JWT token en header Authorization: Bearer <token>
 * - Autorización: Solo SUPER_ADMIN puede acceder a estos endpoints
 * 
 * Implementa:
 * - HU-13.1: Registro exitoso de un nuevo ecommerce
 * - HU-13.2: Listar y obtener ecommerces
 * - HU-13.3: Actualizar estado de un ecommerce
 * 
 * Notas:
 * - @PreAuthorize revisa el rol en cada request
 * - Constructor injection para dependencias (obligatorio)
 * - Métodos transaccionales delegados a servicio
 */
@RestController
@RequestMapping("/api/v1/ecommerces")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("isAuthenticated()")
public class EcommerceController {
    
    private final EcommerceService ecommerceService;
    private final SecurityContextHelper securityContextHelper;
    
    /**
     * POST /api/v1/ecommerces
     * Crea un nuevo ecommerce.
     * 
     * Requiere: rol SUPER_ADMIN + token JWT válido
     * 
     * Request Body:
     * {
     *   "name": "Tienda Nike",
     *   "slug": "nike-store"
     * }
     * 
     * Response 201 Created:
     * {
     *   "uid": "550e8400-e29b-41d4-a716-446655440000",
     *   "name": "Tienda Nike",
     *   "slug": "nike-store",
     *   "status": "ACTIVE",
     *   "createdAt": "2026-03-29T10:30:00Z",
     *   "updatedAt": "2026-03-29T10:30:00Z"
     * }
     * 
     * Errores:
     * - 400: datos incompletos o formato inválido
     * - 401: token ausente o expirado
     * - 403: rol insuficiente (no SUPER_ADMIN)
     * - 409: slug duplicado
     * 
     * @param request EcommerceCreateRequest (name, slug)
     * @return ResponseEntity<EcommerceResponse> 201 Created
     * @apiNote CRITERIO-1.1, 1.2, 1.3, 1.4, 1.5
     */
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<EcommerceResponse> createEcommerce(
            @Valid @RequestBody EcommerceCreateRequest request) {
        log.info("POST /api/v1/ecommerces - Creando ecommerce: slug={}", request.slug());
        
        EcommerceResponse response = ecommerceService.createEcommerce(request);
        
        log.info("Ecommerce creado exitosamente: uid={}", response.uid());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * GET /api/v1/ecommerces
     * Lista ecommerces con paginación y filtrado por status.
     * 
     * Requiere: rol SUPER_ADMIN + token JWT válido
     * 
     * Query Parameters:
     * - status: ACTIVE o INACTIVE (opcional)
     * - page: número de página (default: 0)
     * - size: tamaño de página (default: 50)
     * 
     * Response 200 OK (Page):
     * {
     *   "content": [
     *     {
     *       "uid": "550e8400-e29b-41d4-a716-446655440000",
     *       "name": "Tienda Nike",
     *       "slug": "nike-store",
     *       "status": "ACTIVE",
     *       "createdAt": "...",
     *       "updatedAt": "..."
     *     }
     *   ],
     *   "totalElements": 1,
     *   "totalPages": 1,
     *   "currentPage": 0,
     *   "size": 20
     * }
     * 
     * @param status filtro por estado (opcional)
     * @param page número de página (default: 0)
     * @param size tamaño de página (default: 50)
     * @return ResponseEntity<Page<EcommerceResponse>> 200 OK
     * @apiNote CRITERIO-2.1
     */
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<EcommerceResponse>> listEcommerces(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size) {
        log.info("GET /api/v1/ecommerces - Listando ecommerces: status={}, page={}, size={}", 
                status, page, size);
        
        Page<EcommerceResponse> response = ecommerceService.listEcommerces(status, page, size);
        
        log.info("Listado completado: {} elementos", response.getTotalElements());
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /api/v1/ecommerces/{uid}
     * Obtiene un ecommerce específico por UUID.
     * 
     * Requiere: rol SUPER_ADMIN + token JWT válido
     * 
     * Path Parameters:
     * - uid: UUID del ecommerce
     * 
     * Response 200 OK:
     * {
     *   "uid": "550e8400-e29b-41d4-a716-446655440000",
     *   "name": "Tienda Nike",
     *   "slug": "nike-store",
     *   "status": "ACTIVE",
     *   "createdAt": "2026-03-29T10:30:00Z",
     *   "updatedAt": "2026-03-29T10:30:00Z"
     * }
     * 
     * Errores:
     * - 404: ecommerce no existe
     * - 401: token ausente o expirado
     * - 403: rol insuficiente
     * 
     * @param uid identificador único del ecommerce
     * @return ResponseEntity<EcommerceResponse> 200 OK
     * @apiNote CRITERIO-2.2, 2.3
     */
    @GetMapping("/{uid}")
    public ResponseEntity<EcommerceResponse> getEcommerceById(
            @PathVariable UUID uid) {
        log.info("GET /api/v1/ecommerces/{} - Obteniendo ecommerce", uid);
        
        String currentRole = securityContextHelper.getCurrentUserRole();
        
        // ============ VALIDAR ACCESO ============
        // SUPER_ADMIN: puede acceder a cualquier ecommerce
        // STORE_ADMIN/STORE_USER: solo pueden acceder a su propio ecommerce (SPEC-004 CRITERIO-3.5)
        if (!"SUPER_ADMIN".equals(currentRole)) {
            UUID userEcommerceId = securityContextHelper.getCurrentUserEcommerceId();
            if (userEcommerceId == null || !userEcommerceId.equals(uid)) {
                log.warn("Intento de acceso a ecommerce no autorizado: role={}, requested_uid={}, user_ecommerce={}", 
                        currentRole, uid, userEcommerceId);
                throw new AuthorizationException(
                    "No tienes permiso para acceder a este ecommerce"
                );
            }
        }
        
        EcommerceResponse response = ecommerceService.getEcommerceById(uid);
        
        log.info("Ecommerce obtenido: {}", uid);
        return ResponseEntity.ok(response);
    }
    
    /**
     * PUT /api/v1/ecommerces/{uid}
     * Actualiza el status de un ecommerce (ACTIVE ↔ INACTIVE).
     * 
     * Requiere: rol SUPER_ADMIN + token JWT válido
     * 
     * Path Parameters:
     * - uid: UUID del ecommerce
     * 
     * Request Body:
     * {
     *   "status": "INACTIVE"
     * }
     * 
     * Response 200 OK:
     * {
     *   "uid": "550e8400-e29b-41d4-a716-446655440000",
     *   "name": "Tienda Nike",
     *   "slug": "nike-store",
     *   "status": "INACTIVE",
     *   "createdAt": "2026-03-29T10:30:00Z",
     *   "updatedAt": "2026-03-29T11:00:00Z"
     * }
     * 
     * Side Effects (Cascada):
     * - Evento RabbitMQ a Fanout Exchange loyalty.events
     * - service-admin invalida JWT de usuarios
     * - service-engine invalida API Keys en caché
     * 
     * Errores:
     * - 400: status inválido o intento de cambiar otros campos
     * - 404: ecommerce no existe
     * - 401: token ausente o expirado
     * - 403: rol insuficiente
     * 
     * @param uid identificador único del ecommerce
     * @param request EcommerceUpdateStatusRequest (status)
     * @return ResponseEntity<EcommerceResponse> 200 OK
     * @apiNote CRITERIO-3.1, 3.2, 3.3, 3.4
     */
    @PutMapping("/{uid}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<EcommerceResponse> updateEcommerceStatus(
            @PathVariable UUID uid,
            @Valid @RequestBody EcommerceUpdateStatusRequest request) {
        log.info("PUT /api/v1/ecommerces/{} - Actualizando status: newStatus={}", uid, request.status());
        
        EcommerceResponse response = ecommerceService.updateEcommerceStatus(uid, request);
        
        log.info("Ecommerce actualizado exitosamente: uid={}, newStatus={}", uid, response.status());
        return ResponseEntity.ok(response);
    }
}
