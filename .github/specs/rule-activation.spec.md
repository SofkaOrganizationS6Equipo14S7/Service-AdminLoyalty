---
id: SPEC-014
status: DRAFT
feature: rule-activation
created: 2026-03-30
updated: 2026-03-30
author: spec-generator
version: "1.0"
related-specs: ["SPEC-012"]
---

# Spec: Activación/Desactivación de Reglas

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de iniciar implementación.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPRECATED

---

## 1. REQUERIMIENTOS

### Descripción
Permitir a los usuarios administrativos activar o desactivar reglas (Seasonal Rules y Fidelity Ranges) con un solo clic, reflejando el cambio inmediatamente en el motor de cálculo sin perder la configuración. Las reglas inactivas no participan en el cálculo de descuentos.

### Requerimiento de Negocio
[Extraído de `.github/requirements/rule-activation.md`]

**Historia de Usuario:**
Como usuario de LOYALTY, quiero poder activar o desactivar reglas con un solo click, para reaccionar rápidamente a cambios en el mercado sin tener que borrar la configuración.

**Notas Técnicas (Constraints):**
- **Stack:** Java 21 + Spring Boot 3.x.
- **Auth:** JWT para gestión de reglas.
- **Almacenamiento:** PostgreSQL con campo `status` (ACTIVE/INACTIVE) en tablas de reglas.
- **Performance:** Cambio de estado debe reflejarse inmediatamente en caché del Engine Service.
- **Sync:** Publicación de evento de cambio de estado vía RabbitMQ al Engine Service.
- **Estados:** ACTIVE, INACTIVE.
- La activación/desactivación es una operación de cambio de estado, no elimina los datos.
- Las reglas inactivas no participan en el cálculo de descuentos.
- El historial de cambios debe registrar estas activaciones/desactivaciones (HU-11).

### Historias de Usuario

#### HU-14.1: Desactivación inmediata de una regla activa

```
Como:        Administrador de LOYALTY
Quiero:      Desactivar una regla de descuento o fidelidad con un solo click
Para:        Suspender temporalmente su aplicación sin perder la configuración

Prioridad:   Alta
Estimación:  M
Dependencias: Ninguna
Capa:        Backend + Frontend
```

#### Criterios de Aceptación — HU-14.1

**Happy Path**
```gherkin
CRITERIO-14.1.1: Desactivar una regla activa
  Dado que:  existe una regla de descuento por Temporada en estado "ACTIVE"
  Cuando:    el administrador hace PATCH /api/v1/seasonal-rules/{uid}/status con body { "status": "INACTIVE" }
  Entonces:  el sistema retorna 200 OK con la regla actualizada
  Y:         el campo `is_active` cambia a `false`
  Y:         el timestamp `updated_at` se actualiza
  Y:         se publica un evento SeasonalRuleStatusChanged a RabbitMQ
  Y:         el Engine Service recibe el evento e invalida la caché
  Y:         la regla no participa en futuros cálculos de descuento
```

**Validación de Datos**
```gherkin
CRITERIO-14.1.2: Validar estado en transición
  Dado que:  existe una regla en estado "ACTIVE"
  Cuando:    se intenta cambiar a un estado inválido (ej. { "status": "UNKNOWN" })
  Entonces:  el sistema retorna 400 Bad Request
  Y:         el mensaje de error es: "El estado debe ser ACTIVE o INACTIVE"
  Y:         la regla no cambia de estado
```

**Error Path**
```gherkin
CRITERIO-14.1.3: Cambio de estado en regla inexistente
  Dado que:  el uid proporcionado no existe en la base de datos
  Cuando:    se intenta hacer PATCH /api/v1/seasonal-rules/{uid}/status
  Entonces:  el sistema retorna 404 Not Found
  Y:         el mensaje de error es: "Regla no encontrada"
```

```gherkin
CRITERIO-14.1.4: Cambio de estado sin autenticación
  Dado que:  la solicitud no incluye un token JWT válido
  Cuando:    se intenta hacer PATCH /api/v1/seasonal-rules/{uid}/status
  Entonces:  el sistema retorna 401 Unauthorized
```

```gherkin
CRITERIO-14.1.5: Cambio de estado sin permisos
  Dado que:  el usuario autenticado no tiene rol ADMIN
  Cuando:    se intenta hacer PATCH /api/v1/seasonal-rules/{uid}/status
  Entonces:  el sistema retorna 403 Forbidden
```

---

#### HU-14.2: Reactivación de regla sin pérdida de datos

```
Como:        Administrador de LOYALTY
Quiero:      Reactivar una regla que fue desactivada previamente
Para:        Volver a aplicar la regla en los cálculos con sus parámetros originales

Prioridad:   Alta
Estimación:  S
Dependencias: HU-14.1
Capa:        Backend + Frontend
```

#### Criterios de Aceptación — HU-14.2

**Happy Path**
```gherkin
CRITERIO-14.2.1: Reactivar una regla inactiva
  Dado que:  existe una regla de fidelidad oro en estado "INACTIVE"
  Y:         todos sus parámetros originales están intactos
  Cuando:    el administrador hace PATCH /api/v1/seasonal-rules/{uid}/status con body { "status": "ACTIVE" }
  Entonces:  el sistema retorna 200 OK con la regla actualizada
  Y:         el campo `is_active` cambia a `true`
  Y:         el timestamp `updated_at` se actualiza
  Y:         se publica un evento SeasonalRuleStatusChanged a RabbitMQ
  Y:         el Engine Service recibe el evento e invalida la caché
  Y:         la regla participa nuevamente en futuros cálculos de descuento con parámetros intactos
```

**Idempotencia**
```gherkin
CRITERIO-14.2.2: Cambio al mismo estado no produce cambios
  Dado que:  existe una regla con estado "ACTIVE"
  Cuando:    se intenta hacer PATCH /api/v1/seasonal-rules/{uid}/status con body { "status": "ACTIVE" }
  Entonces:  el sistema retorna 200 OK
  Y:         no se publica un evento innecesario (o se publica pero es idempotente)
  Y:         el timestamp `updated_at` puede o no cambiar (decisión de negocio)
```

---

### Reglas de Negocio

1. **Estados válidos:** Solo `ACTIVE` e `INACTIVE`. No hay otros estados intermedios.
2. **Cambios de estado:** El cambio es atómico y se registra en el timestamp `updated_at`.
3. **Sincronización:** Todo cambio de estado debe publicarse por RabbitMQ al Engine Service inmediatamente para invalidar caché.
4. **Permisos:** Solo usuarios con rol `ADMIN` pueden cambiar el estado de una regla.
5. **Historial:** El cambio de estado se registra en la tabla de auditoría (HU-11 — Audit Logs).
6. **Integridad:** Un cambio de estado no modifica los parámetros de la regla (descuentos, fechas, etc.).
7. **No eliminación:** Cambiar a `INACTIVE` no elimina la regla; los datos se conservan completos.

---

## 2. DISEÑO

### Modelos de Datos

#### Entidades afectadas

| Entidad | Almacén | Cambios | Descripción |
|---------|---------|---------|-------------|
| `SeasonalRuleEntity` | tabla `seasonal_rules` | campo `is_active` ya existe | Regla de descuento por temporada |
| `FidelityRangeEntity` | tabla `fidelity_ranges` | agregar campo `is_active` | Rango de fidelidad (ej. Oro, Plata) |

#### Campos del modelo — `seasonal_rules`

| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `uid` | UUID | sí | auto-generado (gen_random_uuid) | Identificador único |
| `is_active` | BOOLEAN | sí | TRUE/FALSE | Estado: activa = true, inactiva = false |
| `updated_at` | TIMESTAMP UTC | sí | auto-actualizado | Última modificación |

**Nota:** El campo `is_active` ya existe en la tabla `seasonal_rules`. No hay cambios de migración necesarios para esta entidad.

#### Campos del modelo — `fidelity_ranges`

| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `is_active` | BOOLEAN | sí | TRUE/FALSE | Estado: activo = true, inactivo = false |

**Nota:** El campo `is_active` ya fue creado en SPEC-008 (Rangos de Fidelidad) con valor por defecto `true`. No hay migraciones nuevas necesarias.

#### Índices / Constraints

- **Índice existente:** `idx_seasonal_rules_active ON seasonal_rules(is_active)` — ya existe; acelera queries filtrando por estado.
- **Índice existente:** `idx_fidelity_ranges_active ON fidelity_ranges(is_active)` — creado en SPEC-008; acelera queries de rangos activos.

---

### API Endpoints

#### PATCH /api/v1/seasonal-rules/{uid}/status

- **Descripción:** Cambia el estado de una regla de temporada entre ACTIVE e INACTIVE
- **Método:** PATCH (cambio parcial de recurso)
- **Auth requerida:** sí (JWT)
- **Roles requeridos:** ADMIN
- **Path Parameters:**
  - `uid` (UUID): Identificador único de la regla

- **Request Body:**
  ```json
  {
    "status": "ACTIVE" | "INACTIVE"
  }
  ```

- **Response 200 OK:**
  ```json
  {
    "uid": "550e8400-e29b-41d4-a716-446655440000",
    "ecommerce_id": "123e4567-e89b-12d3-a456-426614174000",
    "name": "Descuento Navidad",
    "description": "Descuento especial periodo navideño",
    "discount_percentage": 20.00,
    "discount_type": "PERCENTAGE",
    "start_date": "2024-12-01T00:00:00Z",
    "end_date": "2024-12-31T23:59:59Z",
    "is_active": false,
    "created_at": "2024-01-15T10:30:00Z",
    "updated_at": "2026-03-30T14:25:00Z"
  }
  ```

- **Response 400 Bad Request:**
  - Cuando `status` no es `ACTIVE` o `INACTIVE`
  - Body:
    ```json
    {
      "error": "INVALID_STATUS",
      "message": "El estado debe ser ACTIVE o INACTIVE"
    }
    ```

- **Response 401 Unauthorized:**
  - Token ausente o expirado
  - Body:
    ```json
    {
      "error": "UNAUTHORIZED",
      "message": "Token JWT inválido o expirado"
    }
    ```

- **Response 403 Forbidden:**
  - Usuario no tiene rol ADMIN
  - Body:
    ```json
    {
      "error": "FORBIDDEN",
      "message": "No tienes permiso para realizar esta acción"
    }
    ```

- **Response 404 Not Found:**
  - Regla no existe
  - Body:
    ```json
    {
      "error": "NOT_FOUND",
      "message": "Regla no encontrada"
    }
    ```

---

#### PATCH /api/v1/fidelity-ranges/{uid}/status

- **Descripción:** Cambia el estado de un rango de fidelidad entre ACTIVE e INACTIVE
- **Método:** PATCH
- **Auth requerida:** sí (JWT)
- **Roles requeridos:** ADMIN
- **Path Parameters:**
  - `uid` (UUID): Identificador único del rango

- **Request Body:**
  ```json
  {
    "status": "ACTIVE" | "INACTIVE"
  }
  ```

- **Response 200 OK:** (análogo a seasonal-rules)
- **Response 4xx:** (análogo a seasonal-rules)

---

### Eventos RabbitMQ

#### SeasonalRuleStatusChanged

**Publicado por:** Service-Admin (cuando cambia el estado en PATCH endpoint)
**Consumido por:** Service-Engine (invalida caché)

**Payload:**
```java
public record SeasonalRuleStatusChangedEvent(
    String event_type,        // "SEASONAL_RULE_STATUS_CHANGED"
    UUID uid,                 // uid de la regla
    UUID ecommerce_id,        // ecommerce propietaria
    String previous_status,   // "ACTIVE" o "INACTIVE"
    String new_status,        // "ACTIVE" o "INACTIVE"
    Instant timestamp         // Instant.now()
) {}
```

**Configuración RabbitMQ:**
- **Exchange:** `loyalty.seasonal.exchange` (DirectExchange)
- **Routing Key:** `seasonal.rule.status.changed`
- **Queue:** New queue `loyalty.seasonal.status.queue` (o reutilizar existente con binding)
- **Dead Letter:** `loyalty.seasonal.dlx` (configurado)

**Acción en Engine Service (Cold Start Autonomy):**
```
1. Recibe evento SeasonalRuleStatusChangedEvent
2. Actualiza el campo is_active en la tabla réplica local (seasonal_rules)
3. Invalida la entrada correspondiente en el caché Caffeine
4. Siguiente cálculo cargará el nuevo estado desde la réplica local
5. Logs: "event=seasonal_rule_status_changed_received ruleUid=X newStatus=Y updated_in_replica"
```

**Justificación:** Si el Engine Service se reinicia justo después de recibir el evento, los cambios de estado se persisten en la réplica local y no se pierden.

---

#### FidelityRangeStatusChanged

**Publicado por:** Service-Admin
**Consumido por:** Service-Engine

**Payload:**
```java
public record FidelityRangeStatusChangedEvent(
    String event_type,        // "FIDELITY_RANGE_STATUS_CHANGED"
    UUID uid,
    UUID ecommerce_id,
    String previous_status,
    String new_status,
    Instant timestamp
) {}
```

**Configuración RabbitMQ:**
- **Exchange:** `loyalty.fidelity.exchange` (o crear nuevo)
- **Routing Key:** `fidelity.range.status.changed`

---

### Diseño Frontend

#### Componentes nuevos

| Componente | Archivo | Props principales | Descripción |
|------------|---------|------------------|-------------|
| `StatusToggle` | `components/StatusToggle.jsx` | `ruleUid, currentStatus, onToggle, loading` | Botón/switch para cambiar estado |
| `StatusChangeModal` | `components/StatusChangeModal.jsx` | `isOpen, ruleUid, targetStatus, onConfirm, onCancel` | Modal de confirmación antes de cambiar |

#### Páginas nuevas
| Página | Archivo | Ruta | Protegida |
|--------|---------|------|-----------|
| N/A — cambios en páginas existentes | — | — | — |

**Notas:** Los cambios es frontend se integran en `SeasonalRulePage` (lista) y `SeasonalRuleDetailPage` (detalle). No hay nuevas páginas.

#### Hooks y State

| Hook | Archivo | Retorna | Descripción |
|------|---------|---------|-------------|
| `useRuleStatusToggle` | `hooks/useRuleStatusToggle.js` | `{ toggleStatus, loading, error, isSuccess }` | PATCH status con validaciones |

#### Services (llamadas API)

| Función | Archivo | Endpoint |
|---------|---------|---------|
| `updateRuleStatus(uid, status, token)` | `services/seasonalRuleService.js` | `PATCH /api/v1/seasonal-rules/{uid}/status` |
| `updateFidelityRangeStatus(uid, status, token)` | `services/fidelityService.js` | `PATCH /api/v1/fidelity-ranges/{uid}/status` |

#### UI/UX Considerations
1. **Confirmación:** Mostrar modal de confirmación antes de cambiar estado.
2. **Feedback:** Loader mientras se procesa la solicitud.
3. **Toast/Aler:** Mostrar éxito o error con mensaje descriptivo.
4. **Deshabilitado:** Botón deshabilitado durante la transición.
5. **Rollback:** Si la API retorna error, revertir el estado visual.

---

### Arquitectura y Dependencias

- **Paquetes nuevos requeridos:** Ninguno
- **Servicios externos:** RabbitMQ (ya configurado)
- **Impactop en punto de entrada de la app:** 
  - En Service-Admin: registrar evento `SeasonalRuleStatusChangedEvent` y `FidelityRangeStatusChangedEvent` en declaraciones de RabbitMQ (si no existen ya).
  - En Service-Engine: crear listener para nuevos eventos de status change.



## 3. LISTA DE TAREAS

> Checklist accionable para todos los agentes. Marcar cada ítem (`[x]`) al completarlo.
> El Orchestrator monitorea este checklist para determinar el progreso.

### Backend (Service-Admin)

#### Implementación

- [ ] Crear DTO: `StatusChangeRequest` (field: `String status`)
- [ ] Crear evento: `SeasonalRuleStatusChangedEvent` — record con campos (event_type, uid, ecommerce_id, previous_status, new_status, timestamp)
- [ ] Crear evento: `FidelityRangeStatusChangedEvent` — record con campos análogos
- [ ] Agregar método a `SeasonalRuleService.changeStatus(uid, newStatus)` con orden: 1) Validar, 2) Guardar en BD, 3) Invocar `AuditLogService` (ANTES de responder), 4) Publicar evento
- [ ] Agregar método a `FidelityRangeService.changeStatus(uid, newStatus)` (si existe el servicio)
- [ ] Crear endpoint `PATCH /api/v1/seasonal-rules/{uid}/status` en `SeasonalRuleController`
- [ ] Crear endpoint `PATCH /api/v1/fidelity-ranges/{uid}/status` en `FidelityRangeController` (si existe)
- [ ] Agregar publicación de evento en `SeasonalRuleEventPublisher.publishSeasonalRuleStatusChanged(...)`
- [ ] Registrar nuevo evento en RabbitMQ config (routing key, queue si es necesario)

#### Tests Backend

- [ ] `test_seasonalRuleService_changeStatus_to_inactive_success` — cambio ACTIVE → INACTIVE
- [ ] `test_seasonalRuleService_changeStatus_to_active_success` — cambio INACTIVE → ACTIVE
- [ ] `test_seasonalRuleService_changeStatus_same_status_idempotent` — ACTIVE → ACTIVE no causa cambios
- [ ] `test_seasonalRuleService_changeStatus_not_found_raises_error` — uid no existe → error
- [ ] `test_seasonalRuleService_changeStatus_publishes_event` — verifica evento publicado
- [ ] `test_seasonalRuleController_patchStatus_returns_200` — endpoint PATCH retorna 200
- [ ] `test_seasonalRuleController_patchStatus_returns_400_invalid_status` — status inválido → 400
- [ ] `test_seasonalRuleController_patchStatus_returns_401_no_token` — sin token → 401
- [ ] `test_seasonalRuleController_patchStatus_returns_403_no_admin` — no ADMIN → 403
- [ ] `test_seasonalRuleController_patchStatus_returns_404_not_found` — uid no existe → 404

### Backend (Service-Engine)

#### Implementación

- [ ] Crear record: `SeasonalRuleStatusChangedEvent` (matching Service-Admin)
- [ ] Crear record: `FidelityRangeStatusChangedEvent`
- [ ] Crear listener para `SeasonalRuleStatusChangedEvent` en `EngineConfigurationCacheService`
- [ ] Implementar flujo: 1) Recibe evento, 2) Actualiza tabla réplica local `seasonal_rules`, 3) Invalida caché Caffeine, 4) Logs
- [ ] Crear listener para `FidelityRangeStatusChangedEvent` en caché service análogamente
- [ ] Agregar binding en RabbitMQ config para nueva routing key `seasonal.rule.status.changed`
- [ ] Agregar logs detallados: evento recibido, réplica actualizada, caché invalidada

#### Tests Backend

- [ ] `test_cacheService_invalidateSeasonalRuleCache_on_status_changed_event` — evento invalida caché
- [ ] `test_cacheService_loads_updated_rule_after_status_change` — recarga desde BD después de invalidación
- [ ] `test_rabbitListenerConfig_bindings_seasonal_status_change` — verifica que listener está registrado
- [ ] `test_engine_receives_status_changed_event_and_updates` — integración E2E mock

### Frontend

#### Implementación

- [ ] Crear componente `StatusToggle.jsx` — toggle button/switch para ACTIVE ↔ INACTIVE
- [ ] Crear componente `StatusChangeModal.jsx` — modal de confirmación antes de cambiar
- [ ] Crear hook `useRuleStatusToggle.js` — PATCH a API + manejo de estado
- [ ] Crear función `updateRuleStatus(uid, status, token)` en `services/seasonalRuleService.js`
- [ ] Integrar `StatusToggle` en `SeasonalRuleCard` o `SeasonalRuleDetailPage`
- [ ] Agregar manejo de errores: mostrar toast con error
- [ ] Agregar loading state: deshabilitar botón durante PATCH
- [ ] `updateFidelityRangeStatus(...)` en `services/fidelityService.js`
- [ ] Integrar toggle en `FidelityRangePage` o componente relevante
- [ ] CSS Module para `StatusToggle.module.css` y `StatusChangeModal.module.css`

#### Tests Frontend

- [ ] `test_statusToggle_renders_current_state` — muestra estado actual
- [ ] `test_statusToggle_opens_confirmation_modal_on_click` — click abre modal
- [ ] `test_statusChangeModal_calls_api_on_confirm` — modal → API call
- [ ] `test_useRuleStatusToggle_updates_status_success` — hook actualiza estado
- [ ] `test_useRuleStatusToggle_handles_error_displays_toast` — hook muestra error
- [ ] `test_useRuleStatusToggle_disables_on_loading` — botón deshabilitado durante carga
- [ ] `test_seasonalRuleService_updateRuleStatus_returns_200` — servicio retorna respuesta
- [ ] `test_seasonalRuleService_updateRuleStatus_includes_auth_header` — token en header

### QA & Integración

- [ ] Gherkin cases: escenarios happy path + error paths (usar `/gherkin-case-generator`)
- [ ] Plan de tests de rendimiento: cambios de estado no deben afectar latencia (usar `/performance-analyzer`)
- [ ] Identificar riesgos: depuración de RabbitMQ, caché inconsistencia, race conditions (usar `/risk-identifier`)
