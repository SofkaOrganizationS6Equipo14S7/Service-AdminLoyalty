---
id: SPEC-010
status: APPROVED
feature: customer-classification
created: 2026-03-26
updated: 2026-03-30
author: spec-generator
version: "1.1"
related-specs: []
---

# Spec: Clasificación Dinámica de Clientes (Loyalty Tiers)

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de iniciar implementación.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPRECATED

---

## 1. REQUERIMIENTOS

### Descripción
El motor de fidelidad clasifica dinámicamente a los clientes en un nivel de fidelidad (Bronce, Plata, Oro, Platino) según atributos del perfil (gasto total, cantidad de órdenes) recibidos del e-commerce. Esto permite aplicar reglas de descuento específicas a cada tier con determinismo y sincronización en tiempo real.

### Requerimiento de Negocio
El requerimiento original tal como fue proporcionado en `.github/requirements/customer-classification.md`:

**HU-08: Clasificación Dinámica de Clientes (Loyalty Tiers)**

Como Engine Service, quiero clasificar al cliente en un nivel de fidelidad (Bronce, Plata, Oro, Platino) según el payload recibido del e-commerce, para determinar qué reglas de descuento aplicarle.

### Historias de Usuario

#### HU-08: Clasificación Dinámica de Clientes en Tiers de Fidelidad

```
Como:        Engine Service (Backend)
Quiero:      Evaluar atributos del cliente contra una matriz de reglas en caché
Para:        Asignar automáticamente un nivel de fidelidad (Tier) que determine los descuentos aplicables

Prioridad:   Alta
Estimación:  L
Dependencias: HU-07 (Discount Config/Priority), RabbitMQ Event Sync
Capa:        Backend (Service-Engine)
```

#### Criterios de Aceptación — HU-08

**CRITERIO-8.1: Clasificación exitosa contra matriz de reglas**
```gherkin
Dado que:  existe una matriz de clasificación en la caché de Caffeine (Engine)
  And:     el payload del e-commerce es válido (total_spent, order_count)
Cuando:    el motor evalúa el payload contra la matriz de reglas
Entonces:  el cliente queda asignado a un único nivel de fidelidad (uid de Tier)
  And:     la respuesta incluye el nombre del tier (Bronce, Plata, Oro, Platino)
```

**CRITERIO-8.2: Rechazo por datos inválidos o incompletos**
```gherkin
Dado que:  el payload del cliente tiene campos obligatorios faltantes (ej. sin total_spent)
  Or:      contiene valores negativos (ej. order_count = -5)
Cuando:    el motor intenta clasificar
Entonces:  rechaza la clasificación con HTTP 400 Bad Request
  And:     devuelve un mensaje descriptivo indicando qué campos son obligatorios
  And:     no se aplica ningún tier ni descuento de fidelidad
```

**CRITERIO-8.3: Sincronización de Matriz (Admin -> Engine)**
```gherkin
Dado que:  el Administrador actualiza la matriz de clasificación en service-admin
Cuando:    publica un evento CustomerClassificationMatrixUpdated en RabbitMQ
Entonces:  service-engine consume el evento inmediatamente
  And:     invalida la caché local (Caffeine)
  And:     carga la nueva matriz en memoria para futuras evaluaciones
```

**CRITERIO-8.4: Consistencia y determinismo**
```gherkin
Dado que:  se evalúa el mismo payload dos veces bajo la misma configuración
Cuando:    ambas evaluaciones se ejecutan dentro de la TTL del caché (10 minutos)
Entonces:  el resultado de clasificación es idéntico en ambos casos
  And:     el algoritmo de comparación de rangos es determinístico (sin aleatoriedad)
```

**CRITERIO-8.5: Fallback (Cold Start) — Réplica Local Autónoma**
```gherkin
Dado que:  el Engine acaba de reiniciarse o la caché Caffeine está vacía
Cuando:    intentar clasificar a un cliente
Entonces:  el Engine carga automáticamente la matriz desde su tabla réplica (PostgreSQL local)
  And:     popula el caché Caffeine en milisegundos
  And:     procede a clasificar al cliente
  And:     **no requiere contactar al Admin** — operación 100% autónoma
```

### Reglas de Negocio
1. **Matriz de Clasificación:** Debe estar predefinida con al menos un tier (Bronce es el tier base obligatorio)
2. **Atributos Obligatorios:** El payload puede contener `total_spent` (BigDecimal), `order_count` (int), `loyalty_points` (int), o métricas personalizadas. Todos >= 0.
3. **Unicidad de Asignación:** Cada cliente recibe exactamente un tier. **Si múltiples tiers aplican, se asigna el de MAYOR `level`** (Platino > Oro > Plata > Bronce). Esto incentiva el ascenso de fidelidad.
4. **Determinismo:** Mismo payload + misma configuración = siempre el mismo tier. El algoritmo NO puede tener variables aleatorias.
5. **TTL de Caché:** 10 minutos para evitar stale data
6. **Invalidación:** Cuando la matriz cambia, el caché Caffeine se invalida vía evento RabbitMQ (Fanout). Sincronización asíncrona y confiable.
7. **Error Handling:** Payloads inválidos no generan tier, solo error 400 con descripción clara
8. **Auth Segregada:** Admin usa JWT (SUPER_ADMIN), Engine usa API Key (e-commerce). No se puede usar X-User-ID en clasificación.
9. **Autonomía Cold Start:** Si el Engine se reinicia sin conectar al Admin, carga su réplica local desde PostgreSQL → Caffeine en milisegundos. Sigue operativo 100%.
10. **Flexibilidad de Métrica:** Las reglas soportan múltiples tipos de métrica (loyalty_points, total_spent, order_count, custom). Permite extensibilidad sin cambios de código.

---

## 2. DISEÑO

### Modelos de Datos

#### Entidades afectadas
| Entidad | Almacén | Cambios | Descripción |
|---------|---------|---------|-------------|
| `CustomerTierEntity` | tabla `customer_tiers` (Admin) | **nueva** | Define los tiers (Bronce, Plata, Oro, Platino) con rangos de atributos |
| `CustomerTierReplicaEntity` | tabla `customer_tiers_replica` (Engine) | **nueva** | Réplica read-only del Admin. Permite Cold Start autonomo |
| `ClassificationRuleEntity` | tabla `classification_rules` (Admin) | **nueva** | Reglas individuales asociadas a un tier |
| `ClassificationRuleReplicaEntity` | tabla `classification_rules_replica` (Engine) | **nueva** | Réplica read-only del Admin. Permite Cold Start autonomo |

#### Campos del modelo — CustomerTierEntity
| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `uid` | UUID | sí | auto-generado | Identificador único del tier |
| `name` | string | sí | ENUM: Bronce, Plata, Oro, Platino | Nombre del tier de fidelidad |
| `level` | int | sí | 1, 2, 3, 4 (Bronce=1, Plata=2, Oro=3, Platino=4) | Jerarquía del tier (usado para desempate) |
| `is_active` | boolean | sí | default true | Si el tier está activo o archivado |
| `created_at` | datetime (UTC) | sí | auto-generado | Timestamp creación |
| `updated_at` | datetime (UTC) | sí | auto-generado | Timestamp actualización |

#### Campos del modelo — ClassificationRuleEntity
| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `uid` | UUID | sí | auto-generado | Identificador único de la regla |
| `customer_tier_uid` | UUID | sí | FK a customer_tiers.uid | Tier asignado cuando la regla aplica |
| `min_total_spent` | BigDecimal | sí | >= 0 | Mínimo de gasto acumulado (inclusive) |
| `max_total_spent` | BigDecimal | no | >= min_total_spent o NULL (sin límite) | Máximo de gasto (inclusive). NULL = sin límite superior |
| `min_order_count` | int | sí | >= 0 | Mínimas órdenes completadas (inclusive) |
| `max_order_count` | int | no | >= min_order_count o NULL (sin límite) | Máximas órdenes permitidas (inclusive). NULL = sin límite superior |
| `metric_type` | string | sí | 'loyalty_points' \| 'total_spent' \| 'order_count' \| 'custom' | **[NEW]** Métrica evaluada. Permite extensibilidad futura sin cambios de código. El cliente se evalúa contra múltiples métricas simultáneamente |
| `priority` | int | sí | >= 1 | Prioridad de evaluación (menor = evalúa primero). Si múltiples tiers aplican, se asigna el de MAYOR `level` (Platino > Oro > Plata > Bronce) |
| `is_active` | boolean | sí | default true | Si la regla está activa |
| `created_at` | datetime (UTC) | sí | auto-generado | Timestamp creación |
| `updated_at` | datetime (UTC) | sí | auto-generado | Timestamp actualización |

#### Índices / Constraints
- **customer_tiers**: Unique constraint en `name` (Bronce, Plata, Oro, Platino)
- **customer_tiers**: Index en `is_active=true, level` (búsqueda frecuente)
- **classification_rules**: Index en `customer_tier_uid` (búsqueda de reglas por tier)
- **classification_rules**: Index en `is_active=true` (solo reglas activas)
- **classification_rules**: Composite unique constraint pending evaluation: `(customer_tier_uid, priority)` para evitar prioridades duplicadas por tier

#### Seeders (Data Inicial)
```sql
-- Inserts de ejemplo:
INSERT INTO customer_tiers (uid, name, level, is_active, created_at, updated_at)
VALUES 
  ('00000000-0000-0000-0000-000000000001', 'Bronce', 1, true, now(), now()),
  ('00000000-0000-0000-0000-000000000002', 'Plata', 2, true, now(), now()),
  ('00000000-0000-0000-0000-000000000003', 'Oro', 3, true, now(), now()),
  ('00000000-0000-0000-0000-000000000004', 'Platino', 4, true, now(), now());

-- Reglas de ejemplo:
INSERT INTO classification_rules (uid, customer_tier_uid, min_total_spent, max_total_spent, min_order_count, max_order_count, priority, is_active, created_at, updated_at)
VALUES 
  -- Bronce: 0-1000 de gasto, 0-5 órdenes
  ('10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 0, 1000, 0, 5, 1, true, now(), now()),
  -- Plata: 1000-5000 de gasto ou 5-10 órdenes
  ('10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002', 1000, 5000, 5, 10, 2, true, now(), now()),
  -- Oro: 5000-20000 de gasto ou 10-20 órdenes
  ('10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000003', 5000, 20000, 10, 20, 3, true, now(), now()),
  -- Platino: > 20000 de gasto o > 20 órdenes
  ('10000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000004', 20000, NULL, 20, NULL, 4, true, now(), now());
```

---

#### Tablas de Réplica en Service-Engine (Cold Start)

> **Arquitectura de Autonomía:** El Engine DEBE tener tablas réplica (`customer_tiers_replica`, `classification_rules_replica`) para garantizar operación autónoma. Si el Engine se reinicia sin contactar al Admin, carga su réplica desde PostgreSQL → Caffeine en milisegundos. Esto es crítico para High Availability.

##### CustomerTierReplicaEntity (Engine)
| Campo | Tipo | Descrición |
|-------|------|-----------|
| `uid` | UUID | PK (copiado del Admin) |
| `name` | Enum | BRONZE, SILVER, GOLD, PLATINUM |
| `level` | int | 1-4 (jerarquía) |
| `is_active` | boolean | Soft-delete flag |
| `last_synced` | DateTime | Timestamp de sincronización desde Admin |

##### ClassificationRuleReplicaEntity (Engine)
| Campo | Tipo | Descrición |
|-------|------|-----------|
| `uid` | UUID | PK (copiado del Admin) |
| `tier_uid` | UUID | FK a customer_tiers_replica |
| `metric_type` | string | 'loyalty_points' \| 'total_spent' \| 'order_count' \| 'custom' |
| `min_value` | BigDecimal | Mínimo de métrica |
| `max_value` | BigDecimal | Máximo de métrica (NULL = sin límite) |
| `priority` | int | Orden de evaluación |
| `is_active` | boolean | Soft-delete flag |
| `last_synced` | DateTime | Timestamp de sincronización |

**Índices en Réplica:**
- **idx_replica_active**: (is_active=true, tier_uid) — caché lookup
- **idx_replica_metric**: (metric_type, is_active=true) — búsqueda por métrica

### API Endpoints

#### [ADMIN ENDPOINTS] POST /api/v1/admin/tiers
- **Ubicación**: Service-Admin (8081)
- **Descripción**: Crea un nuevo tier de fidelidad (Admin)
- **Auth requerida**: sí (JWT, rol SUPER_ADMIN)
- **Request Body**:
  ```json
  {
    "name": "Plata",
    "level": 2,
    "is_active": true
  }
  ```
- **Response 201**: Tier creado. Publica evento `CustomerTierCreatedEvent` en RabbitMQ
- **Response 401**: Sin autenticación
- **Response 403**: Rol insuficiente

#### [ADMIN ENDPOINTS] POST /api/v1/admin/classification-rules
- **Ubicación**: Service-Admin (8081)
- **Descripción**: Crea una regla de clasificación (Admin)
- **Auth requerida**: sí (JWT, rol SUPER_ADMIN)
- **Request Body**:
  ```json
  {
    "tier_uid": "00000000-0000-0000-0000-000000000002",
    "metric_type": "total_spent",
    "min_value": "1000.00",
    "max_value": "5000.00",
    "priority": 2,
    "is_active": true
  }
  ```
- **Response 201**: Regla creada. Publica evento `ClassificationRuleCreatedEvent` en RabbitMQ

#### [ADMIN ENDPOINTS] PUT /api/v1/admin/classification-rules/{uid}
- **Ubicación**: Service-Admin (8081)
- **Descripción**: Actualiza una regla de clasificación
- **Auth requerida**: sí (JWT, rol SUPER_ADMIN)
- **Response 200**: Regla actualizada. Publica evento `ClassificationRuleUpdatedEvent` en RabbitMQ

#### [ADMIN ENDPOINTS] DELETE /api/v1/admin/classification-rules/{uid}
- **Ubicación**: Service-Admin (8081)
- **Descripción**: Marca regla como inactiva (soft-delete)
- **Response 204**: Soft-delete aplicado. Publica evento `ClassificationRuleDeletedEvent` en RabbitMQ

#### [ADMIN ENDPOINTS] GET /api/v1/admin/classification-rules
- **Ubicación**: Service-Admin (8081)
- **Descripción**: Lista todas las reglas activas
- **Auth requerida**: sí (JWT)
- **Response 200**: Array de reglas



#### POST /api/v1/admin/classification/tiers (ADMIN ONLY)
- **Descripción**: Crea un nuevo tier (uso limitado; los tiers base Bronce, Plata, Oro, Platino son seeders)
- **Auth requerida**: sí (JWT de Administrador en service-admin)
- **Request Body**:
  ```json
  {
    "name": "Platino",
    "level": 4
  }
  ```
- **Response 201**: Tier creado
  ```json
  {
    "tier_uid": "uuid",
    "name": "Platino",
    "level": 4,
    "is_active": true,
    "created_at": "2026-03-26T14:30:00Z",
    "updated_at": "2026-03-26T14:30:00Z"
  }
  ```
- **Response 409**: Tier con ese nombre ya existe

#### POST /api/v1/admin/classification/rules (ADMIN ONLY)
- **Descripción**: Define reglas de clasificación asociadas a tiers
- **Auth requerida**: sí (JWT de Administrador)
- **Request Body**:
  ```json
  {
    "customer_tier_uid": "00000000-0000-0000-0000-000000000002",
    "min_total_spent": "1000.00",
    "max_total_spent": "5000.00",
    "min_order_count": 5,
    "max_order_count": 10,
    "priority": 2
  }
  ```
- **Response 201**: Regla creada
  ```json
  {
    "rule_uid": "uuid",
    "customer_tier_uid": "00000000-0000-0000-0000-000000000002",
    "min_total_spent": "1000.00",
    "max_total_spent": "5000.00",
    "min_order_count": 5,
    "max_order_count": 10,
    "priority": 2,
    "is_active": true,
    "created_at": "2026-03-26T14:30:00Z",
    "updated_at": "2026-03-26T14:30:00Z"
  }
  ```
- **Response 400**: Prioridad duplicada para el mismo tier o rango inválido
- **Response 409**: Regla conflictiva

#### PUT /api/v1/admin/classification/rules/{rule_uid} (ADMIN ONLY)
- **Descripción**: Actualiza una regla de clasificación
- **Auth requerida**: sí (JWT de Administrador)
- **Request Body**: Campos opcionales a actualizar
- **Response 200**: Regla actualizada
- **Response 404**: Regla no encontrada
- **Observer Effect**: Al actualizar una regla, se publica evento `CustomerClassificationMatrixUpdated` en RabbitMQ para invalidar cachés del Engine

#### GET /api/v1/admin/classification/rules
- **Descripción**: Lista todas las reglas de clasificación
- **Auth requerida**: sí (JWT)
- **Response 200**: Array de reglas activas

### Eventos RabbitMQ

#### `CustomerClassificationMatrixUpdated` (Publisher: service-admin, Consumer: service-engine)
**Exchange:** `classification-exchange` (Fanout)  
**Routing Key:** `classification.matrix.updated`  
**Queue:** `classification.matrix.queue` (en service-engine)

**Payload:**
```json
{
  "event_id": "uuid",
  "timestamp": "2026-03-26T14:30:00Z",
  "event_type": "CustomerClassificationMatrixUpdated",
  "source": "service-admin",
  "data": {
    "triggered_by": "rule_update",
    "tier_uid": "00000000-0000-0000-0000-000000000002",
    "message": "Classification rules updated, invalidate cache"
  }
}
```

**Consumer Handler (service-engine):**
- Recibe evento
- Invalida `classificationMatrixCache` en Caffeine
- En la siguiente solicitud de `/classification/calculate`, recarga desde DB

### Arquitectura y Dependencias

#### Paquetes nuevos en service-engine
- `com.loyalty.service_engine.domain.entity`: `CustomerTierEntity`, `ClassificationRuleEntity`
- `com.loyalty.service_engine.domain.repository`: `CustomerTierRepository`, `ClassificationRuleRepository`
- `com.loyalty.service_engine.application.service`: `CustomerTierService`, `ClassificationMatrixService`, `ClassificationEngine`
- `com.loyalty.service_engine.application.dto`: `ClassificationCalculateRequest`, `ClassificationCalculateResponse`, `CustomerTierResponse`, `ClassificationRuleRequest`, `ClassificationRuleResponse`
- `com.loyalty.service_engine.infrastructure.cache`: `ClassificationMatrixCache`
- `com.loyalty.service_engine.infrastructure.rabbitmq`: `ClassificationEventPublisher`, `ClassificationEventConsumer`, `RabbitMQClassificationConfig`
- `com.loyalty.service_engine.presentation.controller`: *Sin controladores en v1.0 (Data Plane interno)*

#### Dependencias externas
- **RabbitMQ**: Para sincronización asíncrona de matriz (ya configurado en proyecto)
- **Caffeine**: Para caché en memoria (versión >= 3.1.0)
- **Spring Data JPA + PostgreSQL**: Para persistencia de tiers y reglas
- **Spring Security + API Key Validator**: Para autenticación del `/calculate` endpoint

#### Servicios existentes reutilizados
- `ApiKeyValidator` (de service-admin via service-engine) para validar X-API-Key
- `SecurityContext` para extraer `ecommerce_id` (sin X-User-ID)
- `DiscountCalculationEngine` (HU-07) para aplicar descuentos según tier (future integration)

#### Impacto en punto de entrada (ServiceEngineApplication.java)
- Inicializar `ClassificationMatrixCache` en startup
- Registrar event consumer `ClassificationEventConsumer` en RabbitMQ
- **No requiere registro de controllers** (Data Plane interno, sin endpoints públicos en v1.0)

### Arquitectura: Separación Admin ↔ Engine 🔀

> **Principio crítico:** El Engine es el "músculo" (Data Plane). El Admin es el "cerebro" (Control Plane).
> Los datos fluyen **hacia el Engine** (Admin → RabbitMQ → Engine), pero nunca viceversa.

```
┌─────────────────────────────────┐         ┌─────────────────────────────────┐
│    Service-Admin (8081)         │         │   Service-Engine (8082)         │
│  [Control Plane — JWT]          │         │  [Data Plane — API Key]         │
├─────────────────────────────────┤         ├─────────────────────────────────┤
│  DB Maestra:                    │         │  DB Réplica:                    │
│  - customer_tiers               │         │  - customer_tiers_replica       │
│  - classification_rules         │         │  - classification_rules_replica │
├─────────────────────────────────┤         ├─────────────────────────────────┤
│  Endpoints (CRUD):              │         │  Endpoints Públicos:            │
│  POST   /admin/tiers            │         │  (Ninguno en v1.0 — interno)    │
│  POST   /admin/rules            │         │  Escucha RabbitMQ               │
│  PUT    /admin/rules/{uid}      │         │  Carga BD réplica en startup    │
│  DELETE /admin/rules/{uid}      │         │  Caché:                         │
│  GET    /admin/rules            │         │  - Caffeine (TTL: 10 min)       │
├─────────────────────────────────┤         ├─────────────────────────────────┤
│  Events Published:              │  RMQ    │  Events Consumed:               │
│  - TierCreated                  │────────→│  - TierCreated                  │
│  - RuleCreated                  │         │  - RuleCreated / Updated        │
│  - RuleUpdated                  │         │  - RuleDeleted                  │
│  - RuleDeleted                  │         │  (invalida caché, recarga DB)   │
└─────────────────────────────────┘         └─────────────────────────────────┘
        ↑ Humanos (JWT)                              ↑ E-commerce (API Key)
        |                                            |
   Admin UI                                   POST /classify payload
    (Futura HU)                           (total_spent, order_count, ...)
```

**Flujo de Sincronización:**
1. **Admin crea/actualiza regla** → POST `/admin/rules` + JWT
2. **Admin DB actualizada** → Publica evento `RuleUpdatedEvent` en RabbitMQ
3. **Engine consume evento** (listener en background)
4. **Engine invalida Caffeine** → Carga nueva matriz desde su DB réplica
5. **Engine responde `/classify`** con matriz actualizada

**Si Engine se reinicia (Cold Start):**
- Lee su tabla réplica del PostgreSQL local
- Popula Caffeine en milisegundos
- Sigue clasificando sin esperar a Admin
- **100% autónomo**

---

### Notas de Implementación

1. **BigDecimal para dinero**: Todos los montos (`total_spent`, `min_total_spent`, `max_total_spent`) son `BigDecimal` para precisión
2. **Algoritmo de clasificación determinístico**:
   - Evalúa TODAS las reglas activas contra el payload
   - Selecciona aquellas cuyo rango aplica (min <= atributo <= max, NULL = sin límite)
   - **Si múltiples tiers aplican → selecciona el de MAYOR `level`** (Platino=4 > Oro=3 > Plata=2 > Bronce=1)
   - Retorna un único tier
3. **Métrica flexible**: El campo `metric_type` permite clasificación por puntos, gasto, órdenes o métricas custom sin cambios de código
4. **Cache TTL = 10 minutos**: Caffeine con `expireAfterWrite(10, TimeUnit.MINUTES)`
5. **Virtual Threads**: Usar `spring.threads.virtual.enabled=true` si Java 21+ (ya en proyecto)
6. **API Key (futuro)**: Cuando se agreguen endpoints de clasificación en futuras HUs, se usará API Key para e-commerce (en lugar de JWT)
7. **Graceful fallback**: Si caché vacía, consultar DB réplica directamente (no fallar)
8. **Sincronización confiable**: RabbitMQ con retry policy (max 5 intentos, backoff exponencial 500ms→5000ms)
9. **CRÍTICO**: Nunca poner endpoints CRUD en el Engine. Engine es sordo y mudo a humanos; solo escucha RabbitMQ

---

## 3. LISTA DE TAREAS

> Checklist accionable para Backend Developer, Test Engineer Backend, y Documentation Agent.
> Marcar cada ítem (`[x]`) al completarlo.

### Backend

#### Implementación — Domain Layer
- [x] Crear `CustomerTierEntity` con campos: uid, name (ENUM), level, is_active, timestamps en `com.loyalty.service_engine.domain.entity`
- [x] Crear `ClassificationRuleEntity` con campos: uid, customer_tier_uid, min/max_total_spent, min/max_order_count, priority, is_active, timestamps
- [x] Crear `CustomerTierRepository` con método `findByIsActiveTrue()` ordenado por level DESC
- [x] Crear `ClassificationRuleRepository` con métodos: `findByIsActiveTrueOrderByPriority()`, `findByCustomerTierUidAndIsActiveTrue()`
- [x] Crear `ClassificationMatrixCache` interface/contract

#### Implementación — Application Layer (Services)
- [x] Crear `CustomerTierService`: operaciones CRUD (crear tier, listar activos)
- [x] Crear `ClassificationRuleService`: operaciones CRUD (crear regla, actualizar, listar activas)
- [x] Crear `ClassificationMatrixService`: cargar matriz desde DB, invalidar caché (invocado por event consumer)
- [x] Crear `ClassificationEngine`: método `classify(total_spent, order_count)` → StudentTierEntity (determinístico)
  - Cargar matriz activa desde caché (fallback a DB si vacía)
  - Evaluar todas las reglas
  - Seleccionar tier de mayor level entre los aplicables
  - Retornar ResponseDTO único tier
- [x] Crear DTOs (Java Records):
  - `ClassificationCalculateRequest`: total_spent (BigDecimal), order_count (int)
  - `ClassificationCalculateResponse`: tier_uid, tier_name, tier_level, total_spent, order_count, calculated_at
  - `CustomerTierResponse`: tier_uid, name, level, is_active, timestamps
  - `ClassificationRuleRequest`: customer_tier_uid, min/max_total_spent, min/max_order_count, priority
  - `ClassificationRuleResponse`: rule_uid, customer_tier_uid, min/max_total_spent, min/max_order_count, priority, is_active, timestamps

#### Implementación — Presentation Layer (Controllers)
- [x] **Crear SOLO en Service-ADMIN:**
  - `ClassificationAdminController` (endpoints CRUD):
    - `POST /api/v1/admin/tiers` — crea tier, publica evento, retorna 201
    - `POST /api/v1/admin/classification-rules` — crea regla, publica evento, retorna 201
    - `PUT /api/v1/admin/classification-rules/{uid}` — actualiza, publica evento, retorna 200
    - `DELETE /api/v1/admin/classification-rules/{uid}` — soft-delete, publica evento, retorna 204
    - `GET /api/v1/admin/classification-rules` — lista reglas, retorna 200
    - Auth: JWT, rol SUPER_ADMIN
- [x] Service-Engine: **Sin endpoints públicos**
  - Engine es Data Plane (interno): solo escucha RabbitMQ
  - Réplicas de tablas + Caché + Consumer (ver Infrastructure Layer)
  - Endpoints públicos serán agregados en futuras HUs

#### Implementación — Infrastructure Layer (Cache + Events)

**Service-Admin (Publisher):**
- [x] Crear `ClassificationEventPublisher`: publica eventos cuando se crea/actualiza/elimina regla
  - `ClassificationRuleCreatedEvent`, `ClassificationRuleUpdatedEvent`, `ClassificationRuleDeletedEvent`
  - Intercambio: `classification-exchange` (Fanout)

**Service-Engine (Consumer + Cache):**
- [x] Crear caché service: `ClassificationMatrixCaffeineCacheService`
  - Implementa `ClassificationMatrixCache`
  - TTL: 10 minutos (`expireAfterWrite`)
  - Métodos: `get()`, `upsert()`, `invalidate()`, `isEmpty()`
  
- [x] Crear `ClassificationMatrixStartupLoader`
  - Ejecuta **post-construction** (con `@EventListener(ContextRefreshedEvent.class)`)
  - Lee tablas réplica (`customer_tiers_replica`, `classification_rules_replica`)
  - Popula Caffeine en milisegundos
  - **Garantiza operación autónoma si Admin no está disponible**
  
- [x] Crear `ClassificationEventConsumer`
  - Escucha queue `classification.matrix.queue`
  - Consume `ClassificationRuleCreatedEvent`, `ClassificationRuleUpdatedEvent`, `ClassificationRuleDeletedEvent`
  - Invalida Caffeine y recarga desde réplica
  - Idempotente: si evento duplicado, sin efectos secundarios
  
- [x] Crear `RabbitMQClassificationConfig` (Service-Engine):
  - Declare queue `classification.matrix.queue`
  - Declare binding a exchange `classification-exchange` (Fanout)
  - DLX para eventos muertos
  - Retry policy: max 5 intentos, backoff exponencial

#### Migrations

**En Service-Admin:**
- [x] Crear `V13__Create_customer_tiers_table.sql`:
  ```sql
  CREATE TABLE customer_tiers (
    uid UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    level INTEGER NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (level BETWEEN 1 AND 4)
  );
  CREATE INDEX idx_customer_tiers_active_level ON customer_tiers(is_active, level);
  ```
- [x] Crear `V14__Create_classification_rules_table.sql`:
  ```sql
  CREATE TABLE classification_rules (
    uid UUID PRIMARY KEY,
    customer_tier_uid UUID NOT NULL REFERENCES customer_tiers(uid),
    min_total_spent NUMERIC(19,2) NOT NULL DEFAULT 0,
    max_total_spent NUMERIC(19,2),
    min_order_count INTEGER NOT NULL DEFAULT 0,
    max_order_count INTEGER,
    priority INTEGER NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tier FOREIGN KEY (customer_tier_uid) REFERENCES customer_tiers(uid),
    CONSTRAINT priority_range CHECK (priority > 0),
    CONSTRAINT total_spent_range CHECK (max_total_spent IS NULL OR max_total_spent >= min_total_spent),
    CONSTRAINT order_count_range CHECK (max_order_count IS NULL OR max_order_count >= min_order_count)
  );
  CREATE INDEX idx_classification_rules_tier ON classification_rules(customer_tier_uid);
  CREATE INDEX idx_classification_rules_active ON classification_rules(is_active);
  ```
- [x] Crear `V15__Seed_default_tiers.sql`: Inserts Bronce, Plata, Oro, Platino
- [x] Crear `V16__Seed_default_classification_rules.sql`: Inserts reglas de ejemplo

**En Service-Engine (Réplicas para Cold Start):**
- [x] Crear `V13__Create_customer_tiers_replica_table.sql`:
  ```sql
  CREATE TABLE customer_tiers_replica (
    uid UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    level INTEGER NOT NULL,
    is_active BOOLEAN DEFAULT true,
    last_synced TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (level BETWEEN 1 AND 4)
  );
  CREATE INDEX idx_tiers_replica_active_level ON customer_tiers_replica(is_active, level);
  ```
- [x] Crear `V14__Create_classification_rules_replica_table.sql`:
  ```sql
  CREATE TABLE classification_rules_replica (
    uid UUID PRIMARY KEY,
    tier_uid UUID NOT NULL REFERENCES customer_tiers_replica(uid),
    metric_type VARCHAR(50) NOT NULL,
    min_value NUMERIC(19,2) NOT NULL DEFAULT 0,
    max_value NUMERIC(19,2),
    priority INTEGER NOT NULL,
    is_active BOOLEAN DEFAULT true,
    last_synced TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT priority_range CHECK (priority > 0)
  );
  CREATE INDEX idx_rules_replica_active_metric ON classification_rules_replica(is_active, metric_type);
  CREATE INDEX idx_rules_replica_tier ON classification_rules_replica(tier_uid);
  ```
- [x] Crear `V15__Seed_tiers_replica_initial.sql`: Copy inicial desde Admin (si está disponible), o seeders manuales
- [x] Crear `V16__Seed_rules_replica_initial.sql`: Copy inicial desde Admin

#### Exception Handlers
- [x] Crear `ClassificationValidationException` si payload inválido (extiende `BadRequestException`)
- [ ] Registrar en `GlobalExceptionHandler` → HTTP 400 + mensaje descriptivo
- [x] Crear `ClassificationMatrixUnavailableException` si caché y DB no disponibles → HTTP 503

#### Tests Backend
- [ ] `test_classificationEngine_classify_bronce_success` — payload bajo aplica a Bronce
- [ ] `test_classificationEngine_classify_platino_success` — payload alto aplica a Platino
- [ ] `test_classificationEngine_classify_deterministic` — mismo payload 2x = mismo resultado
- [ ] `test_classificationEngine_classify_multiple_rules_selects_highest_tier` — múltiples tiers aplicables → mayor level
- [ ] `test_classificationController_post_calculate_returns_200` — happy path
- [ ] `test_classificationController_post_calculate_returns_400_missing_field` — payload inválido
- [ ] `test_classificationController_post_calculate_returns_401_no_api_key` — sin autenticación

### Frontend
- [ ] **Opcional (futura HU)**: Dashboard admin para visualizar tiers y reglas (tabla de tiers, tabla de reglas, crear/editar modal)

### QA / Integration Tests
- [ ] Crear Gherkin scenarios basados en CRITERIO-8.1 a 8.5 (para `/gherkin-case-generator`)
- [ ] Test de end-to-end: Admin actualiza regla → RabbitMQ publica → Engine recibe → caché se invalida → siguiente `/calculate` usa matriz nueva
- [ ] Performance test: `/classification/calculate` debe ser < 50ms (in-memory cache hit)
- [ ] Test de Cold Start: Engine recarga matriz desde réplica local sin conexión a Admin

### Documentation
- [ ] Actualizar README.md con descripción de clasificación de tiers y matriz de reglas
- [ ] Documentar matriz de clasificación por defecto (Bronce, Plata, Oro, Platino) y rangos
- [ ] Documentar arquitectura Admin ↔ Engine y sincronización RabbitMQ
