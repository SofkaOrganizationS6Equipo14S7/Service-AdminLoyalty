---
id: SPEC-008
status: IMPLEMENTED
feature: rule-status-endpoint
created: 2026-04-08
updated: 2026-04-08
author: spec-generator
version: "1.0"
related-specs: []
---

# Spec: Endpoint Dedicado para Cambiar Status de Reglas

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de iniciar implementación.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPRECATED

---

## 1. REQUERIMIENTOS

### Descripción
Implementar un endpoint PATCH dedicado para cambiar el estado de activación/desactivación de reglas. Actualmente el cambio de estado se realiza mediante DELETE (soft delete) o PUT (actualización completa), pero no existe un endpoint específico que sincronice este cambio con el servicio Engine mediante eventos RabbitMQ.

### Requerimiento de Negocio
El requerimiento original de HU-14:

```
Implementar:
a) Nuevo endpoint PATCH:
   PATCH /api/v1/rules/{ruleId}/status
   Body: { "active": true | false }

b) Lógica:
   - Si active = true: cambia isActive = true (reactivar)
   - Si active = false: cambia isActive = false (desactivar)
   - Mantiene todos los demás datos intactos
   - No permite si la regla no existe (404)

c) Validaciones:
   - Verificar que la regla exista y pertenezca al ecommerce del usuario
   - Verificar tenant isolation

Ubicación:
RuleController.java y RuleService.java
```

### Historias de Usuario

#### HU-14: Cambiar Status de Regla mediante Endpoint Dedicado

```
Como:        Administrador de ecommerce
Quiero:      Cambiar el estado de activación de una regla sin actualizarla completamente
Para:        Simplificar la administración de reglas y sincronizar cambios con Engine

Prioridad:   Alta
Estimación:  M
Dependencias: HU-07 (Reglas base)
Capa:        Backend
```

#### Criterios de Aceptación — HU-14

**Happy Path: Desactivar regla activa**
```gherkin
CRITERIO-14.1: Cambiar estado de regla activa a inactiva
  Dado que:  Existe una regla con id={ruleId} perteneciente al ecommerce={ecommerceId}
             y la regla tiene isActive=true
  Cuando:    Se envía PATCH /api/v1/rules/{ruleId}/status
             con body { "active": false }
  Entonces:  La respuesta es 200 OK
             y el payload retorna la regla actualizada con isActive=false
             y el campo updated_at es la fecha/hora actual
             y los demás campos (name, description, discountPercentage) se mantienen intactos
             y se emite un evento RabbitMQ de tipo RuleStatusChanged al Engine Service
```

**Happy Path: Reactivar regla inactiva**
```gherkin
CRITERIO-14.2: Cambiar estado de regla inactiva a activa
  Dado que:  Existe una regla con id={ruleId} perteneciente al ecommerce={ecommerceId}
             y la regla tiene isActive=false
  Cuando:    Se envía PATCH /api/v1/rules/{ruleId}/status
             con body { "active": true }
  Entonces:  La respuesta es 200 OK
             y el payload retorna la regla actualizada con isActive=true
             y el campo updated_at es la fecha/hora actual
             y los demás campos se mantienen intactos
             y se emite un evento RabbitMQ de tipo RuleStatusChanged al Engine Service
```

**Error Path: Regla no existent**
```gherkin
CRITERIO-14.3: Intentar cambiar status de regla inexistente
  Dado que:  Se intenta cambiar una regla con id={invalidRuleId}
             que no existe en el ecommerce={ecommerceId}
  Cuando:    Se envía PATCH /api/v1/rules/{invalidRuleId}/status
             con body { "active": false }
  Entonces:  La respuesta es 404 Not Found
             y el payload es ApiErrorResponse con código NOT_FOUND
             y el mensaje dice "Regla con id {invalidRuleId} no encontrada"
```

**Error Path: Violación de Tenant Isolation**
```gherkin
CRITERIO-14.4: Intentar cambiar status de regla de otro ecommerce
  Dado que:  Un usuario autenticado con ecommerce={ecommerceId}
             intenta cambiar una regla que pertenece a otro ecommerce={otherEcommerceId}
  Cuando:    Se envía PATCH /api/v1/rules/{ruleId}/status
             con body { "active": false }
  Entonces:  La respuesta es 404 Not Found
             (no revelar que existe una regla del otro ecommerce)
             y sin cambios en BD
```

**Error Path: Cambio de status idempotente**
```gherkin
CRITERIO-14.5: Cambiar a status actual no genera error
  Dado que:  Existe una regla con id={ruleId}
             y tiene isActive=true
  Cuando:    Se envía PATCH /api/v1/rules/{ruleId}/status
             con body { "active": true }
  Entonces:  La respuesta es 200 OK
             (operación idempotente)
             y la regla sigue con isActive=true
             y se emite un evento RabbitMQ (aunque estado no cambie)
             y el campo updated_at se actualiza a la hora actual
```

**Error Path: Sin autenticación**
```gherkin
CRITERIO-14.6: Intentar sin token JWT
  Dado que:  Una solicitud PATCH sin header Authorization
  Cuando:    Se envía PATCH /api/v1/rules/{ruleId}/status
             sin token JWT
  Entonces:  La respuesta es 401 Unauthorized
```

**Error Path: Request inválido**
```gherkin
CRITERIO-14.7: Body con campo "active" ausente o tipo incorrecto
  Dado que:  Se envía una request PATCH
  Cuando:    El body es { } (falta "active")
             O el body es { "active": "string-invalido" }
             O el body es { "active": null }
  Entonces:  La respuesta es 400 Bad Request
             y el payload es ApiErrorResponse con código VALIDATION_ERROR
             y el mensaje describe el error (ej. "Campo 'active' obligatorio y debe ser booleano")
```

### Reglas de Negocio

1. **Soft Delete Pattern**: El cambio de status no elimina la regla de BD, solo marca `isActive` con true/false.
2. **Tenant Isolation**: El endpoint debe validar que la regla pertenezca al ecommerce del usuario autenticado (via JWT).
3. **Idempotencia**: Cambiar a status actual no genera error, es un cambio válido (operación safe).
4. **Event Emission**: Cada cambio de status debe emitir un evento RabbitMQ `RuleStatusChanged` al Engine Service para sincronización.
5. **Updated Timestamp**: El campo `updated_at` debe actualizarse a la hora actual (UTC) cada vez que se cambie el status.
6. **No Side Effects**: El cambio de status **no afecta** asignaciones de tiers (`RuleCustomerTier`), atributos dinámicos ni histórico. Solo cambia `isActive` y `updated_at`.

---

## 2. DISEÑO

### Modelos de Datos

#### Entidades afectadas
| Entidad | Almacén | Cambios | Descripción |
|---------|---------|---------|-------------|
| `RuleEntity` | tabla `rules` | actualización | Campo `isActive` y `updated_at` |

#### Campos involucrados
| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `id` | UUID | sí | auto-generado | Identificador único de la regla |
| `isActive` | Boolean | sí | no null | Flag de activación (true = activa, false = inactiva) |
| `updated_at` | Instant (UTC) | sí | auto-generado | Timestamp de última actualización |
| (resto intacto) | — | — | — | name, description, discountPercentage, ecommerceId, etc. |

#### Índices / Constraints
- `idx_rules_id_ecommerce` — búsqueda por (id, ecommerceId) [EXISTENTE]
- No nuevos índices requeridos

### DTO Nuevos

#### RuleStatusUpdateRequest (Request Body)
```java
public record RuleStatusUpdateRequest(
    @NotNull(message = "El campo 'active' es obligatorio")
    Boolean active
) {}
```

**Ubicación**: `presentation/dto/rules/RuleStatusUpdateRequest.java`

**Notas**:
- Valida que `active` sea Boolean (no null, no String)
- Spring Boot maneja automaticamente la conversión JSON → Boolean

### API Endpoints

#### PATCH /api/v1/rules/{ruleId}/status
- **Descripción**: Cambia el estado de activación (active/inactive) de una regla sin modificar otros campos
- **Auth requerida**: sí (JWT en header `Authorization: Bearer <token>`)
- **Path Parameters**:
  - `ruleId` (UUID): ID de la regla a modificar
- **Request Body**:
  ```json
  {
    "active": true | false
  }
  ```
- **Response 200 OK** — Cambio exitoso:
  ```json
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "ecommerceId": "550e8400-e29b-41d4-a716-446655440001",
    "discountPriorityId": "550e8400-e29b-41d4-a716-446655440002",
    "name": "Spring Promo 20%",
    "description": "Promoción de primavera",
    "discountPercentage": 20.00,
    "isActive": false,
    "attributes": [
      {
        "attributeId": "550e8400-e29b-41d4-a716-446655440003",
        "attributeName": "start_date",
        "value": "2026-04-01"
      }
    ],
    "createdAt": "2026-04-01T10:00:00Z",
    "updatedAt": "2026-04-08T15:30:45Z"
  }
  ```
  ✅ Retorna tipo `RuleResponse` (DTO existente)

- **Response 400 Bad Request** — Validación fallida:
  ```json
  {
    "timestamp": "2026-04-08T15:30:45Z",
    "status": 400,
    "error": "Bad Request",
    "code": "VALIDATION_ERROR",
    "message": "El campo 'active' es obligatorio",
    "path": "/api/v1/rules/550e8400-e29b-41d4-a716-446655440000/status"
  }
  ```

- **Response 401 Unauthorized** — Sin autenticación:
  ```json
  {
    "timestamp": "2026-04-08T15:30:45Z",
    "status": 401,
    "error": "Unauthorized",
    "code": "AUTHENTICATION_ERROR",
    "message": "Token ausente o expirado",
    "path": "/api/v1/rules/550e8400-e29b-41d4-a716-446655440000/status"
  }
  ```

- **Response 404 Not Found** — Regla no existe o no pertenece al ecommerce:
  ```json
  {
    "timestamp": "2026-04-08T15:30:45Z",
    "status": 404,
    "error": "Not Found",
    "code": "NOT_FOUND",
    "message": "Regla con id 550e8400-e29b-41d4-a716-446655440000 no encontrada en el ecommerce",
    "path": "/api/v1/rules/550e8400-e29b-41d4-a716-446655440000/status"
  }
  ```

### Evento RabbitMQ

#### RuleStatusChangedEvent (Producer)
Cuando se cambien validamente, se emite al Engine Service:

```java
public record RuleStatusChangedEvent(
    UUID ruleId,
    UUID ecommerceId,
    Boolean newStatus,          // true = activa, false = inactiva
    Boolean previousStatus,     // Estado anterior para auditoría
    Instant changedAt           // Timestamp UTC
) {}
```

**Ubicación**: `infrastructure/rabbitmq/event/RuleStatusChangedEvent.java`

**Propiedades RabbitMQ**:
- **Exchange**: `loyalty.events` (existente)
- **Routing Key**: `rule.status.changed`
- **Queue**: `engine-service-rule-status-changes` (nueva, si no existe)
- **Formato**: JSON (Spring Boot maneja serialización)

**Consumidor**: Engine Service escucha en queue `engine-service-rule-status-changes` y actualiza su caché Caffeine.

### Cambios en Capas

#### Presentation Layer (RuleController)
```java
@PatchMapping("/{ruleId}/status")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<RuleResponse> updateRuleStatus(
    @PathVariable UUID ruleId,
    @Valid @RequestBody RuleStatusUpdateRequest request,
    // Usuario desde el JWT es extraído por Spring Security
) throws RuleNotFoundException, TenantIsolationException {
    UUID ecommerceId = extractEcommerceIdFromJWT();  // vía SecurityContext
    RuleResponse response = ruleService.updateRuleStatus(
        ecommerceId,
        ruleId,
        request.active()
    );
    return ResponseEntity.ok(response);
}
```

#### Application Layer (RuleService)
```java
@Transactional
public RuleResponse updateRuleStatus(
    UUID ecommerceId,
    UUID ruleId,
    Boolean newStatus
) throws RuleNotFoundException {
    // 1. Buscar regla por ID y ecommerceId (tenant isolation)
    RuleEntity rule = ruleRepository
        .findByIdAndEcommerceId(ruleId, ecommerceId)
        .orElseThrow(() -> new RuleNotFoundException(
            "Regla con id " + ruleId + " no encontrada en el ecommerce"
        ));

    // 2. Guardar estado anterior para auditoría
    Boolean previousStatus = rule.getIsActive();

    // 3. Actualizar estado
    rule.setIsActive(newStatus);
    rule.setUpdatedAt(Instant.now());
    RuleEntity saved = ruleRepository.save(rule);

    // 4. Emitir evento RabbitMQ
    RuleStatusChangedEvent event = new RuleStatusChangedEvent(
        rule.getId(),
        rule.getEcommerceId(),
        newStatus,
        previousStatus,
        Instant.now()
    );
    rabbitTemplate.convertAndSend(
        "loyalty.events",
        "rule.status.changed",
        event
    );

    // 5. Retornar DTO (usa método existente toResponse)
    return toResponse(saved);
}
```

#### Domain Layer (RuleEntity)
- **Sin cambios** — entidad y repository interface ya existen

#### Infrastructure Layer
- **RuleRepository**: Ya tiene método `findByIdAndEcommerceId(UUID, UUID)` ✅
- **RabbitMQ**: Usar `RabbitTemplate` existente para emitir eventos

### Arquitectura y Dependencias

#### Paquetes / Clases nuevas
- `presentation/dto/rules/RuleStatusUpdateRequest.java` — Record DTO
- `infrastructure/rabbitmq/event/RuleStatusChangedEvent.java` — Record evento  

#### Dependencias modificadas
- `RuleController` — agregar método `@PatchMapping`
- `RuleService` — agregar método `updateRuleStatus()`
- `application.yml` / `application.properties` — configuración RabbitMQ (ya existe)

#### Servicios externos
- **RabbitMQ**: Emit a exchange `loyalty.events` (ya configurado)
- **Spring Security**: Extrae JWT y ecommerceId del contexto

#### Notas de Implementación
> - Usar el método existente `toResponse()` para convertir entity a DTO
> - Reutilizar excepciones existentes: `RuleNotFoundException`, `TenantIsolationException`
> - El evento RabbitMQ debe enviarse **después** de guardar en BD (no en pre-persist hook)
> - Considerar idempotencia: si status no cambia, igual emitir evento (spec lo requiere CRITERIO-14.5)
> - El campo `updated_at` se actualiza aunque el status sea igual (sea idempotente)
> - Si RabbitMQ falla, la excepción no debe rollback la transacción (usar `@Transactional(propagation = REQUIRES_NEW)` en envío si es crítico)

---

## 3. LISTA DE TAREAS

### Backend

#### Implementación
- [x] Crear DTO `RuleStatusUpdateRequest` en `presentation/dto/rules/`
- [x] Crear Event `RuleStatusChangedEvent` en `application/dto/rules/`
- [x] Implementar método `updateRuleStatus()` en `RuleService` con lógica de negocio
- [x] Implementar endpoint `@PatchMapping("/{ruleId}/status")` en `RuleController`
- [x] Verificar que `RuleRepository.findByIdAndEcommerceId()` existe [✅ EXISTE]
- [x] Registrar emisión del evento en `RuleService.updateRuleStatus()`

#### Tests Backend
- [ ] `test_updateRuleStatus_switch_active_to_inactive_success` — happy path desactivar
- [ ] `test_updateRuleStatus_switch_inactive_to_active_success` — happy path activar
- [ ] `test_updateRuleStatus_idempotent_no_change_on_same_status` — cambiar a status actual
- [ ] `test_updateRuleStatus_rule_not_found_returns_404` — regla inexistente
- [ ] `test_updateRuleStatus_tenant_isolation_returns_404` — regla de otro ecommerce
- [ ] `test_updateRuleStatus_no_auth_returns_401` — sin JWT
- [ ] `test_updateRuleStatus_invalid_body_returns_400` — active=null o tipo incorrecto
- [ ] `test_service_updateRuleStatus_emits_rabbit_event` — verifica que evento se emita
- [ ] `test_controller_patch_endpoint_returns_200_with_rule_response` — endpoint retorna 200 + RuleResponse

### QA
- [ ] Ejecutar skill `/gherkin-case-generator` → criterios CRITERIO-14.1 a 14.7
- [ ] Ejecutar skill `/risk-identifier` → clasificación ASD de riesgos
- [ ] Validar que endpoint PATCH no afecta asignaciones de tiers
- [ ] Validar que evento RabbitMQ se propaga correctamente al Engine
- [ ] Revisar cobertura de tests contra todos los criterios

### Documentación (opcional)
- [ ] Actualizar `API_DOCUMENTATION.md` con nuevo endpoint PATCH
- [ ] Actualizar `ENDPOINTS.md` con ruta, método, parámetros, respuestas
