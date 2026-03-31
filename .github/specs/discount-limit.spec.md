---
id: SPEC-0009
status: APPROVED
feature: discount-limit
created: 2026-03-30
updated: 2026-03-30 (v1.1 — ajustes arquitectónicos: Admin maestro, Engine réplica, internalize applyDiscountLimit)
author: spec-generator
version: "1.1"
related-specs:
  - SPEC-0008 (fidelity-ranges)
  - SPEC-0011 (engine-calculate) — integración de applyDiscountLimit
---

# Spec: Límite y Prioridad de Descuentos (HU-09)

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de iniciar implementación.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPRECATED

---

## 1. REQUERIMIENTOS

### Descripción

El sistema LOYALTY debe permitir a los administradores definir un tope máximo global de descuentos y una prioridad de aplicación por tipo de descuento. Cuando una transacción califica para múltiples descuentos, el motor debe resolverlos en orden de prioridad y limitar el total acumulado al máximo configurado, protegiendo así la rentabilidad del negocio.

### Requerimiento de Negocio

Como usuario de LOYALTY, quiero definir el tope máximo y la prioridad de descuentos, para proteger la rentabilidad del negocio.

### Historias de Usuario

#### HU-09: Límite y Prioridad de Descuentos

```
Como:        Administrador (service-admin)
Quiero:      Configurar un tope máximo de descuento acumulado y definir un orden de prioridad por tipo de descuento
Para:        Que el motor de cálculo respete estos límites y no sobre-descuente transacciones

Prioridad:   Alta
Estimación:  L
Dependencias: Ninguna (core feature)
Capa:        Backend (service-engine para cálculo) + Frontend (admin dashboard)
```

#### Criterios de Aceptación — HU-09

**CRITERIO-1.1: Configuración válida de tope y prioridad (Happy Path)**
```gherkin
Dado que:    Existen tipos de descuento disponibles (fidelity, seasonal, promotional)
  Y         El tope máximo propuesto es un valor positivo > 0
  Y         La prioridad define un orden único entre descuentos (ej: fidelity=1, seasonal=2, promotional=3)
Cuando:      Se registra la configuración de topes y prioridad en POST /api/v1/discount-config
Entonces:    La configuración queda vigente para el ecommerce (isActive=true)
  Y         El motor utiliza ese orden para resolver descuentos concurrentes
  Y         El servidor retorna 201 Created con la configuración completa
```

**CRITERIO-1.2: Validación de tope máximo (Error Path)**
```gherkin
Dado que:    Existe una configuración vigente
Cuando:      Se intenta registrar un tope máximo ≤ 0 (ej: -50, 0, null)
Entonces:    La configuración es rechazada
  Y         Se retorna 400 Bad Request con mensaje: "maxDiscountLimit debe ser un valor positivo mayor a cero"
  Y         Se mantiene la última configuración válida sin cambios
```

**CRITERIO-1.3: Validación de prioridad ambigua (Error Path)**
```gherkin
Dado que:    Se intenta registrar una prioridad de descuentos
Cuando:      Hay empates en niveles (ej: fidelity=1, seasonal=1) o hay huecos (ej: 1, 3, 5)
Entonces:    La configuración es rechazada
  Y         Se retorna 400 Bad Request con mensaje: "Las prioridades deben ser secuenciales comenzando en 1 sin duplicados"
  Y         No se altera la prioridad vigente
```

**CRITERIO-1.4: Aplicación del tope ante acumulación (Happy Path)**
```gherkin
Dado que:    Existe una configuración de tope=100.00 en moneda local
  Y         Una transacción califica para: fidelity=50.00 (prioridad 1) + seasonal=40.00 (prioridad 2) + promotional=30.00 (prioridad 3)
Cuando:      Se invoca POST /api/v1/discount-calculate con estos descuentos
Entonces:    El módulo calcula acumulación: 50.00 + 40.00 + 30.00 = 120.00 > 100.00 (límite)
  Y         Aplica descuentos por prioridad hasta el límite: fidelity(50) + seasonal(40) + promotional(10)
  Y         El descuento total final es 100.00 (limitado)
  Y         La respuesta incluye breakdown: original vs applied, indicador de límite excedido
```

**CRITERIO-1.5: Edge case - Transacción dentro del límite (Happy Path)**
```gherkin
Dado que:    Existe una configuración de tope=150.00
  Y         Una transacción califica para: fidelity=50.00 + seasonal=40.00 = 90.00 total
Cuando:      Se invoca POST /api/v1/discount-calculate
Entonces:    El descuento total aplicado es 90.00 (dentro del límite)
  Y         No hay truncamiento, se aplican todos los descuentos
```

**CRITERIO-1.6: Recuperación ante fallo de RabbitMQ (Resilience)**
```gherkin
Dado que:    Se actualiza la configuración de tope/prioridad
  Y         RabbitMQ está momentáneamente caído
Cuando:      El evento de actualización falla en ser publicado
Entonces:    La aplicación fallback a lectura directa de BD (sin caché)
  Y         Los cálculos posteriores usan la nueva configuración correcta
  Y         Se registra un log de warning sobre la falta de comunicación con RabbitMQ
```

### Reglas de Negocio

1. **Validación de tope:** El tope máximo (`maxDiscountLimit`) es obligatorio, debe ser un `BigDecimal` > 0, y se almacena con la moneda configurada (`currencyCode`).

2. **Secuencia de prioridades:** Los niveles de prioridad deben ser secuenciales comienziando en 1 (1, 2, 3, ..., N) sin duplicados ni huecos. Cada tipo de descuento tiene una prioridad única.

3. **Atomicidad de configuración:** Solo puede haber UNA configuración activa (`isActive=true`) por ecommerce en un momento dado. Las migraciones deben asegurar un unique partial index en `(ecommerce_id, is_active)` donde `is_active = true`.

4. **Precedencia de aplicación:** Cuando múltiples descuentos califican, se aplican en orden de prioridad (1 = máxima prioridad). Ver CRITERIO-1.4.

5. **Moneda en todas las operaciones:** El tope, los descuentos y los cálculos siempre usan `BigDecimal`. No se permiten operaciones con `double` o `float`.

6. **Integridad referencial:** Las tablas `discount_config` y `discount_priority` tienen constraint de `ecommerce_id` para multi-tenancy.

7. **Soft-delete histórico:** Las configuraciones anteriores se guardan (no se eliminan) con `is_active=false` para auditoría.

---

## 2. DISEÑO

### Modelos de Datos

#### Entidades afectadas

| Entidad | Base de Datos | Servicio | Cambios | Descripción |
|---------|---|---|---------|-------------||
| `DiscountConfigEntity` | **loyalty_admin** (maestra) | service-admin | nueva | Tope máximo de descuentos (source of truth) |
| `DiscountConfigEntity` | **loyalty_engine** (réplica) | service-engine | nueva | Replica para cold-start y autonomía (caché local) |
| `DiscountPriorityEntity` | **loyalty_admin** (maestra) | service-admin | nueva | Prioridades (source of truth) |
| `DiscountPriorityEntity` | **loyalty_engine** (réplica) | service-engine | nueva | Replica para cálculos en Engine |

#### Campos del modelo — DiscountConfigEntity

| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `uid` | UUID | sí | auto-generado | Identificador único |
| `ecommerceId` | UUID | sí | FK → ecommerce.uid | ID del ecommerce (multi-tenancy) |
| `maxDiscountLimit` | DECIMAL(10,2) | sí | > 0 | Tope máximo de descuento acumulado (en BD como DECIMAL) |
| `currencyCode` | VARCHAR(3) | sí | ISO 4217 (ej: COP, USD) | Código de moneda |
| `isActive` | BOOLEAN | sí | default true | Flag de activación (solo 1 por ecommerce) |
| `createdAt` | TIMESTAMPTZ (UTC) | sí | auto-generado | Timestamp creación (en Java: OffsetDateTime) |
| `updatedAt` | TIMESTAMPTZ (UTC) | sí | auto-generado | Timestamp última actualización |

#### Campos del modelo — DiscountPriorityEntity

| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `uid` | UUID | sí | auto-generado | Identificador único |
| `discountConfigId` | UUID | sí | FK → discount_config.uid | Referencia a la configuración |
| `discountType` | VARCHAR(50) | sí | Enum: FIDELITY, SEASONAL, PROMOTIONAL | Tipo de descuento |
| `priorityLevel` | INTEGER | sí | secuencial 1..N sin huecos | Orden de aplicación (1 = máxima prioridad) |
| `createdAt` | TIMESTAMPTZ (UTC) | sí | auto-generado | Timestamp creación |

#### Índices / Constraints

**Service-Admin (BD master — loyalty_admin):**

*Tabla `discount_config`*
- PK: `uid`
- UK Partial: `(ecommerce_id, is_active)` WHERE `is_active = true` → evita múltiples configs activas
- FK: `ecommerce_id` → `ecommerce(uid)`
- Índice: `idx_discount_config_ecommerce_active` para queries rápidas

*Tabla `discount_priority`*
- PK: `uid`
- FK: `discount_config_id` → `discount_config(uid)` ON DELETE CASCADE
- UK: `(discount_config_id, discount_type)` → cada tipo una sola vez por config
- UK: `(discount_config_id, priority_level)` → prioridades únicas
- Índice: `idx_discount_priority_config_id` para queries rápidas

**Service-Engine (BD réplica — loyalty_engine):**

Misma estructura que Admin (Cold-Start Autonomy). Si el Engine se reinicia y RabbitMQ falla, puede cargar su última configuración conocida desde disk.

### API Endpoints

#### Service-Admin (8081) — Gestión de Configuración

**POST /api/v1/discount-config** (ADMINISTRADOR ONLY)
- **Descripción**: Crea o actualiza la configuración de tope de descuentos (solo una activa por vez). Publica evento a RabbitMQ.
- **Auth requerida**: sí (Bearer token, Administrador)
- **Request Body**:
  ```json
  {
    "ecommerceId": "uuid-string",
    "maxDiscountLimit": "100.00",
    "currencyCode": "COP"
  }
  ```
- **Validación en Controller**:
  - `maxDiscountLimit` > 0 → 400 Bad Request si no
  - `currencyCode` válido (ISO 4217) → 400 si no
- **Response 201**:
  ```json
  {
    "uid": "uuid",
    "ecommerceId": "uuid",
    "maxDiscountLimit": "100.00",
    "currencyCode": "COP",
    "isActive": true,
    "createdAt": "2026-03-30T10:30:00Z",
    "updatedAt": "2026-03-30T10:30:00Z"
  }
  ```
- **Response 400**: ValidationException si límite ≤ 0 o moneda inválida
- **Response 401**: token ausente o expirado
- **Response 403**: permiso insuficiente (no es Administrador)
- **Side Effect**: Marca la configuración anterior como `isActive=false`; publica evento `DiscountConfigUpdated` a RabbitMQ

**GET /api/v1/discount-config**
- **Descripción**: Obtiene la configuración activa de descuentos del ecommerce
- **Auth requerida**: sí
- **Query Params**: `ecommerceId` (UUID)
- **Response 200**:
  ```json
  {
    "uid": "uuid",
    "ecommerceId": "uuid",
    "maxDiscountLimit": "100.00",
    "currencyCode": "COP",
    "isActive": true,
    "createdAt": "2026-03-30T10:30:00Z",
    "updatedAt": "2026-03-30T10:30:00Z"
  }
  ```
- **Response 404**: No existe configuración activa para el ecommerce
- **Response 401**: sin token

**POST /api/v1/discount-priority** (ADMINISTRADOR ONLY)
- **Descripción**: Define el orden de prioridad de tipos de descuento. Publica evento a RabbitMQ.
- **Auth requerida**: sí (Administrador)
- **Request Body**:
  ```json
  {
    "discountConfigId": "uuid",
    "priorities": [
      { "discountType": "FIDELITY", "priorityLevel": 1 },
      { "discountType": "SEASONAL", "priorityLevel": 2 },
      { "discountType": "PROMOTIONAL", "priorityLevel": 3 }
    ]
  }
  ```
- **Validación en Service**:
  - Niveles secuenciales 1..N sin duplicados ni huecos
  - Cada tipo de descuento una sola vez
- **Response 201**:
  ```json
  {
    "uid": "uuid",
    "discountConfigId": "uuid",
    "priorities": [
      { "discountType": "FIDELITY", "priorityLevel": 1, "createdAt": "..." },
      { "discountType": "SEASONAL", "priorityLevel": 2, "createdAt": "..." },
      { "discountType": "PROMOTIONAL", "priorityLevel": 3, "createdAt": "..." }
    ],
    "createdAt": "2026-03-30T10:30:00Z"
  }
  ```
- **Response 400**: Prioridades duplicadas, con huecos, o discountConfigId inválido
- **Response 401/403**: auth/permission errors
- **Side Effect**: Publica evento `DiscountPriorityUpdated` a RabbitMQ

**GET /api/v1/discount-priority**
- **Descripción**: Obtiene las prioridades vigentes
- **Auth requerida**: sí
- **Query Params**: `discountConfigId` (UUID)
- **Response 200**:
  ```json
  {
    "uid": "uuid",
    "discountConfigId": "uuid",
    "priorities": [
      { "discountType": "FIDELITY", "priorityLevel": 1, "createdAt": "..." },
      { "discountType": "SEASONAL", "priorityLevel": 2, "createdAt": "..." },
      { "discountType": "PROMOTIONAL", "priorityLevel": 3, "createdAt": "..." }
    ],
    "createdAt": "2026-03-30T10:30:00Z"
  }
  ```
- **Response 404**: No existe prioridad para esa config
- **Response 401**: sin token

#### Service-Engine (8082) — Cálculo y Lectura

**GET /api/v1/discount-config** (READ-ONLY)
- **Descripción**: Obtiene la configuración activa desde BD réplica (para auditoría/debugging)
- **Auth requerida**: sí
- **Query Params**: `ecommerceId` (UUID)
- **Response 200**: misma estructura que Admin
- **Response 404**: No existe en réplica local

**GET /api/v1/discount-priority** (READ-ONLY)
- **Descripción**: Obtiene las prioridades desde BD réplica (para auditoría/debugging)
- **Auth requerida**: sí
- **Query Params**: `discountConfigId` (UUID)
- **Response 200**: misma estructura que Admin

**Servicio Interno: `applyDiscountLimit()` — NO ES ENDPOINT**
- **Descripción**: Servicio interno invocado por SPEC-011 durante `/calculate` para aplicar el truncamiento de límite
- **Parámetro**: `DiscountCalculateRequest` (ecommerceId, transactionId, discounts[])
- **Retorna**: `DiscountCalculateResponse` (breakdown con truncamiento, limitExceeded flag)
- **No expone HTTP**: Llamado internamente desde `CalculationService` (SPEC-011)
- **Lógica**:
  1. Obtener config activa y prioridades (caché Caffeine 10 min TTL + fallback BD réplica)
  2. Ordenar descuentos por `priorityLevel` ASC (1 = primero)
  3. Acumular montos hasta `maxDiscountLimit`
  4. Truncar descuentos que excedan el límite (ver CRITERIO-1.4)
  5. Retornar breakdown con `limitExceeded` flag

### Diseño Frontend

#### Componentes nuevos

| Componente | Archivo | Props principales | Descripción |
|------------|---------|------------------|-------------|
| `DiscountConfigForm` | `components/admin/DiscountConfigForm` | `onSubmit, loading, error` | Formulario para tope de descuentos |
| `DiscountPriorityTable` | `components/admin/DiscountPriorityTable` | `priorities, onSave, onDelete, editable` | Tabla con drag-drop para reordenar prioridades |
| `DiscountLimitDashboard` | `pages/admin/DiscountLimitDashboard` | — | Pantalla principal de configuración |

#### Páginas nuevas

| Página | Archivo | Ruta | Protegida |
|--------|---------|------|-----------|
| `DiscountLimitDashboard` | `pages/admin/DiscountLimitDashboard` | `/admin/discount-config` | sí (Administrador) |

#### Hooks y State

| Hook | Archivo | Retorna | Descripción |
|------|---------|---------|-------------|
| `useDiscountConfig` | `hooks/useDiscountConfig` | `{ config, loading, error, updateConfig }` | CRUD y lectura de config |
| `useDiscountPriority` | `hooks/useDiscountPriority` | `{ priorities, loading, error, savePriorities }` | Gestión de prioridades |

#### Services (llamadas API)

| Función | Archivo | Endpoint (Admin) |
|---------|---------|----------|
| `getDiscountConfig(ecommerceId, token)` | `services/discountService` | `GET /api/v1/discount-config` (Admin o Engine) |
| `updateDiscountConfig(payload, token)` | `services/discountService` | `POST /api/v1/discount-config` (Admin only) |
| `getDiscountPriority(configId, token)` | `services/discountService` | `GET /api/v1/discount-priority` (Admin o Engine) |
| `savePriorities(payload, token)` | `services/discountService` | `POST /api/v1/discount-priority` (Admin only) |

#### Flujo de UI

1. **Pantalla inicial:** Mostrar config actual (tope, moneda, estado)
2. **Editar tope:** Modal o formulario inline para cambiar `maxDiscountLimit`
3. **Gestionar prioridades:** Tabla con tipos de descuento + nivel de prioridad (drag-drop para reordenar)
4. **Validaciones en cliente:**
   - `maxDiscountLimit` > 0
   - Prioridades secuenciales (1, 2, 3, ...)
5. **Estados de carga/error:** Spinners y mensajes de error de API
6. **Confirmación:** Modal antes de guardar cambios

### Arquitectura y Dependencias

#### Paquetes nuevos requeridos (Backend)

**Service-Engine (Java 21 + Spring Boot 3.x):**
- Ninguno adicional (usa JPA, Validation, Spring Web, RabbitMQ que ya existen)

#### Servicios externos

- **RabbitMQ:** Exchange `discount-exchange`, Routing key `discount.config.updated`, Cola `discount.config.queue` (duplicar en consumer para suscriptores)
- **Caché distribuida:** Caffeine (ya configurada en el proyecto)
- **BD:** PostgreSQL (migraciones Flyway)

#### Impacto en punto de entrada de la app

**Service-Admin:**
- Registrar `DiscountConfigController` como componente REST
- Configurar `RabbitMQConfigAdmin` para publicar eventos al Engine

**Service-Engine:**
- Configurar `RabbitMQConfigEngine` para consumir eventos de Admin
- Inicializar `DiscountCacheConfig` con TTL de 10 minutos + fallback a BD réplica
- Inicializar `DiscountConfigStartupLoader` para cold-start desde BD réplica

**Frontend:**
- Registrar ruta `/admin/discount-config` en router
- Agregar permission/role check para acceso (solo Administrador)

---

## 3. LISTA DE TAREAS

> Checklist accionable para todos los agentes. Marcar cada ítem (`[x]`) al completarlo.
> El Orchestrator monitorea este checklist para determinar el progreso.

### Backend (Service-Admin) — Gestión de Configuración

#### Implementación — Domain Layer
- [ ] Crear `DiscountConfigEntity` (uid, ecommerceId, maxDiscountLimit, currencyCode, isActive, timestamps)
- [ ] Crear `DiscountPriorityEntity` (uid, configId, discountType, priorityLevel, createdAt)
- [ ] Crear `DiscountConfigRepository` con `findByEcommerceIdAndIsActiveTrue()` y `findAllByEcommerceId()`
- [ ] Crear `DiscountPriorityRepository` con queries por configId, type

#### Implementación — Application Layer (DTOs + Services)
- [ ] Crear record `DiscountConfigCreateRequest` (ecommerceId, maxDiscountLimit, currencyCode)
- [ ] Crear record `DiscountConfigResponse` (uid, ecommerceId, maxDiscountLimit, currencyCode, isActive, timestamps)
- [ ] Crear record `DiscountPriorityRequest` (configId, priorityList[discountType, priorityLevel])
- [ ] Crear record `DiscountPriorityResponse` (uid, configId, priorities[], createdAt)
- [ ] Implementar `DiscountConfigService` (updateConfig, getActiveConfig)
- [ ] Implementar `DiscountPriorityService` (savePriorities con validación secuencial 1..N, getPriorities)
- [ ] Validador `DiscountPriorityValidator` (verificar secuencia, no duplicados)

#### Implementación — Presentation Layer (Admin)
- [ ] Crear `DiscountConfigController` (admin only)
  - [ ] POST /api/v1/discount-config → updateConfig (201 Created, publica evento)
  - [ ] GET /api/v1/discount-config?ecommerceId=... → getActiveConfig (200 OK)
  - [ ] POST /api/v1/discount-priority → savePriorities (201 Created, publica evento)
  - [ ] GET /api/v1/discount-priority?configId=... → getPriorities (200 OK)

#### Implementación — Infrastructure Layer (Admin)
- [ ] Crear `DiscountConfigEventPublisher` (publica DiscountConfigUpdated + DiscountPriorityUpdated a RabbitMQ)
- [ ] Crear `RabbitMQConfigAdmin` (declare exchange, queue, bindings para publicar)
- [ ] Crear excepciones: `ResourceNotFoundException`, `BadRequestException`

#### Database Migrations (loyalty_admin)
- [ ] Crear `V##__Create_discount_config_table.sql` con UK parcial `(ecommerce_id, is_active)` WHERE `is_active = true`, TIMESTAMPTZ
- [ ] Crear `V##__Create_discount_priority_table.sql` con UKs en `(discount_config_id, discount_type)` y `(discount_config_id, priority_level)`, TIMESTAMPTZ
- [ ] Seeds (opcional): Default discount config

#### Tests Backend (Service-Admin)
- [ ] `test_discountConfigService_updateConfig_success` — crear/actualizar config válida
- [ ] `test_discountConfigService_updateConfig_invalid_limit` — rechazar límite ≤ 0
- [ ] `test_discountConfigService_updateConfig_invalid_currency` — rechazar ISO 4217 inválido
- [ ] `test_discountPriorityService_savePriorities_success` — guardar válidas (1, 2, 3)
- [ ] `test_discountPriorityService_savePriorities_duplicate_levels` — rechazar duplicados
- [ ] `test_discountPriorityService_savePriorities_missing_levels` — rechazar huecos
- [ ] `test_discountConfigController_post_returns_201` — endpoint POST config
- [ ] `test_discountConfigController_get_returns_200` — endpoint GET config
- [ ] `test_discountConfigController_post_returns_401_no_token` — sin auth
- [ ] `test_discountConfigEventPublisher_publishes_event` — evento a RabbitMQ

### Backend (Service-Engine) — Cálculo y Réplica

#### Implementación — Domain Layer (Réplica)
- [ ] Crear `DiscountConfigEntity` (uid, ecommerceId, maxDiscountLimit, currencyCode, isActive, timestamps) — **IDÉNTICA a Admin**
- [ ] Crear `DiscountPriorityEntity` (uid, configId, discountType, priorityLevel, createdAt) — **IDÉNTICA a Admin**
- [ ] Crear `DiscountConfigRepository` con `findByEcommerceIdAndIsActiveTrue()` (lectura desde réplica)
- [ ] Crear `DiscountPriorityRepository` con queries por configId

#### Implementación — Application Layer (DTOs + Services)
- [ ] Crear records idénticos a Admin: `DiscountConfigResponse`, `DiscountPriorityResponse`
- [ ] Crear records de cálculo: `DiscountCalculateRequest` (ecommerceId, transactionId, discounts[])
- [ ] Crear record `DiscountCalculateResponse` (transactionId, originalTotal, appliedTotal, limitExceeded, breakdown[], timestamp)
- [ ] Implementar `DiscountLimitService` (interno, NO endpoint)
  - Métodos: `applyDiscountLimit(ecommerceId, discounts)` → retorna breakdown con truncamiento
  - Carga config + prioridades de caché, fallback a BD réplica
- [ ] Implementar `DiscountConfigReplicaService` (lectura desde réplica)
  - Métodos: `getActiveConfig(ecommerceId)`, `getPriorities(configId)`

#### Implementación — Infrastructure Layer (Engine)
- [ ] Crear `DiscountConfigConsumer` (consumer RabbitMQ, actualiza BD réplica e invalida caché)
- [ ] Crear `RabbitMQConfigEngine` (declare queue, bindings para consumir)
- [ ] Crear `DiscountCacheConfig` (Caffeine cache con 10 min TTL)
- [ ] Crear `DiscountConfigStartupLoader` — carga réplica desde BD al iniciar (cold-start)
- [ ] Crear `DiscountConfigEventListener` (listener RabbitMQ con retry/deadletter)

#### Presentation Layer (Engine — READ-ONLY endpoints)
- [ ] Crear `DiscountConfigReadController` (sin lógica de escritura)
  - [ ] GET /api/v1/discount-config?ecommerceId=... → lectura réplica (audit/debug)
  - [ ] GET /api/v1/discount-priority?configId=... → lectura réplica (audit/debug)
- [ ] **NO exponen endpoint de cálculo al público** — `applyDiscountLimit()` es servicio interno

#### Database Migrations (loyalty_engine)
- [ ] Crear `V##__Create_discount_config_replica_table.sql` — estructura idéntica a Admin, TIMESTAMPTZ
- [ ] Crear `V##__Create_discount_priority_replica_table.sql` — estructura idéntica a Admin, TIMESTAMPTZ

#### Tests Backend (Service-Engine)
- [ ] `test_discountLimitService_apply_under_limit` — 60 < 100 → returnAll
- [ ] `test_discountLimitService_apply_over_limit` — 120 > 100 → truncar a 100
- [ ] `test_discountLimitService_apply_exact_limit` — 100 = 100 → exact
- [ ] `test_discountLimitService_priority_order` — descuentos aplicados en priorityLevel ASC
- [ ] `test_discountConfigConsumer_updates_replica_on_event` — evento actualiza réplica
- [ ] `test_discountConfigConsumer_invalidates_cache_on_event` — evento invalida caché
- [ ] `test_discountCache_ttl_10_minutes` — caché expira
- [ ] `test_discountCache_fallback_to_replica` — si caché falla, carga desde réplica
- [ ] `test_discountConfigStartupLoader_loads_on_startup` — cold-start OK
- [ ] `test_discountConfigController_get_returns_200_reads_from_replica` — GET lee réplica

### Frontend

#### Implementación — Services
- [ ] Crear `discountService.js` con funciones: getConfig, updateConfig, getPriority, savePriorities
- [ ] Manejo de errores (4xx, 5xx) con mensajes descriptivos
- [ ] Puntos de conexión: Admin (8081) para POST, Admin/Engine para GET

#### Implementación — Hooks
- [ ] Crear `useDiscountConfig` hook (state, loading, error, updateConfig, refetch)
- [ ] Crear `useDiscountPriority` hook (state, loading, error, savePriorities)

#### Implementación — Componentes
- [ ] Crear `DiscountConfigForm` component (input para maxLimit, select para moneda, submit, error display)
- [ ] Crear `DiscountPriorityTable` component (tabla con drag-drop, reordenamiento, validación inline)
- [ ] Crear `DiscountLimitDashboard` page (layout + composition de Form + Table)
- [ ] Registrar ruta `/admin/discount-config` en router (role=Administrador)

#### Tests Frontend
- [ ] `[DiscountConfigForm] submits config with valid data to Admin`
- [ ] `[DiscountConfigForm] shows error for maxLimit <= 0`
- [ ] `[DiscountPriorityTable] renders priorities in order`
- [ ] `[DiscountPriorityTable] prevents duplicate priority levels`
- [ ] `[useDiscountConfig] loads config on mount`
- [ ] `[useDiscountConfig] updates config and refetches`
- [ ] `[DiscountLimitDashboard] integrates form + table`

### QA

- [ ] Ejecutar skill `/gherkin-case-generator` → generar escenarios Gherkin (CRITERIO-1.1 a 1.6)
- [ ] Ejecutar skill `/risk-identifier` → clasificación ASD de riesgos (Alto/Medio/Bajo)
- [ ] Ejecutar skill `/automation-flow-proposer` → qué flujos automatizar (API, UI)
- [ ] Revisar cobertura de tests vs criterios de aceptación (TOTAL ≥ 90%)
- [ ] Validar que todas las reglas de negocio están cubiertas
- [ ] Cold-start test: reiniciar Engine sin RabbitMQ, verificar que carga réplica
- [ ] RabbitMQ resilience test: caída momentánea, recuperación automática
- [ ] Actualizar estado spec: `status: IMPLEMENTED`
