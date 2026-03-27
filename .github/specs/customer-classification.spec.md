---
id: SPEC-008
status: APPROVED
feature: customer-classification
created: 2026-03-26
updated: 2026-03-26
author: spec-generator
version: "1.0"
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

**CRITERIO-8.5: Fallback cuando caché está vacía o expirada**
```gherkin
Dado que:  la caché de Caffeine está vacía o ha expirado (TTL vencido)
Cuando:    llega una solicitud de clasificación
Entonces:  el motor consulta la base de datos para recargar la matriz
  And:     popula nuevamente la caché con la nueva matriz
  And:     procede a clasificar al cliente
```

### Reglas de Negocio
1. **Matriz de Clasificación:** Debe estar predefinida con al menos un tier (Bronce es el tier base obligatorio)
2. **Atributos Obligatorios:** `total_spent` (BigDecimal), `order_count` (int), ambos >= 0
3. **Unicidad de Asignación:** Cada cliente recibe exactamente un tier. Si múltiples tiers aplican, se asigna el tier de mayor jerarquía (mayor nivel de beneficio)
4. **Determinismo:** Mismo payload + misma configuración = siempre el mismo tier. El algoritmo NO puede tener variables aleatorias
5. **TTL de Caché:** 10 minutos para evitar stale data
6. **Invalidación:** Cuando la matriz cambia, el caché se invalida vía evento RabbitMQ (Fanout)
7. **Error Handling:** Payloads inválidos no generan tier, solo error 400 con descripción clara
8. **Auth:** La clasificación se ejecuta en `/api/v1/classification/calculate` con autenticación por API Key (sin X-User-ID)

---

## 2. DISEÑO

### Modelos de Datos

#### Entidades afectadas
| Entidad | Almacén | Cambios | Descripción |
|---------|---------|---------|-------------|
| `CustomerTierEntity` | tabla `customer_tiers` | **nueva** | Define los tiers (Bronce, Plata, Oro, Platino) con rangos de atributos |
| `ClassificationRuleEntity` | tabla `classification_rules` | **nueva** | Reglas individuales (rango total_spent, rango order_count) asociadas a un tier |

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
| `priority` | int | sí | >= 1 | Prioridad de evaluación (menor = evalúa primero). Usado para desempate si múltiples reglas aplican |
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

### API Endpoints

#### POST /api/v1/classification/calculate
- **Descripción**: Clasifica un cliente en un tier de fidelidad según atributos (total_spent, order_count)
- **Auth requerida**: sí (API Key en header `X-API-Key`)
- **Request Body**:
  ```json
  {
    "total_spent": "1500.50",
    "order_count": 7
  }
  ```
- **Response 200**:
  ```json
  {
    "tier_uid": "00000000-0000-0000-0000-000000000002",
    "tier_name": "Plata",
    "tier_level": 2,
    "total_spent": "1500.50",
    "order_count": 7,
    "calculated_at": "2026-03-26T14:30:00Z"
  }
  ```
- **Response 400**: Campo obligatorio faltante, valor negativo o payload inválido
  ```json
  {
    "error": "Bad Request",
    "message": "total_spent is required and must be >= 0",
    "timestamp": "2026-03-26T14:30:00Z"
  }
  ```
- **Response 401**: API Key ausente o inválida
  ```json
  {
    "error": "Unauthorized",
    "message": "API Key missing or invalid",
    "timestamp": "2026-03-26T14:30:00Z"
  }
  ```
- **Response 503**: Caché vacía y DB no disponible
  ```json
  {
    "error": "Service Unavailable",
    "message": "Classification matrix unavailable",
    "timestamp": "2026-03-26T14:30:00Z"
  }
  ```

#### GET /api/v1/classification/tiers
- **Descripción**: Lista todos los tiers activos (Bronce, Plata, Oro, Platino)
- **Auth requerida**: sí (API Key o JWT)
- **Response 200**:
  ```json
  [
    {
      "tier_uid": "00000000-0000-0000-0000-000000000001",
      "name": "Bronce",
      "level": 1,
      "is_active": true,
      "created_at": "2026-03-20T10:00:00Z",
      "updated_at": "2026-03-26T14:00:00Z"
    },
    {
      "tier_uid": "00000000-0000-0000-0000-000000000002",
      "name": "Plata",
      "level": 2,
      "is_active": true,
      "created_at": "2026-03-20T10:00:00Z",
      "updated_at": "2026-03-26T14:00:00Z"
    }
  ]
  ```

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
- `com.loyalty.service_engine.presentation.controller`: `ClassificationController`, `ClassificationAdminController`

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
- Registrar `ClassificationController` y `ClassificationAdminController`
- Inicializar `ClassificationMatrixCache` en startup
- Cargar matriz de BD en la primera llamada de `/calculate` (lazy initialization)
- Registrar event consumer `ClassificationEventConsumer` en RabbitMQ

### Notas de Implementación
1. **BigDecimal para dinero**: Todos los montos (`total_spent`, `min_total_spent`, `max_total_spent`) son `BigDecimal` para precisión
2. **Algoritmo de clasificación determinístico**:
   - Evalúa TODAS las reglas activas contra el payload
   - Selecciona aquellas cuyo rango aplica (min <= atributo <= max, NULL = sin límite)
   - Si múltiples tiers aplican → selecciona el tier de mayor LEVEL (Platino > Oro > Plata > Bronce)
   - Retorna un único tier
3. **Cache TTL = 10 minutos**: Caffeine con `expireAfterWrite(10, TimeUnit.MINUTES)`
4. **Virtual Threads**: Usar `spring.threads.virtual.enabled=true` si Java 21+ (ya en proyecto)
5. **API Key en lugar de JWT para `/calculate`**: El e-commerce se autentica con API Key, no token JWT
6. **Graceful fallback**: Si caché vacía, consultar DB directamente (no fallar)

---

## 3. LISTA DE TAREAS

> Checklist accionable para Backend Developer, Test Engineer Backend, y Documentation Agent.
> Marcar cada ítem (`[x]`) al completarlo.

### Backend

#### Implementación — Domain Layer
- [ ] Crear `CustomerTierEntity` con campos: uid, name (ENUM), level, is_active, timestamps en `com.loyalty.service_engine.domain.entity`
- [ ] Crear `ClassificationRuleEntity` con campos: uid, customer_tier_uid, min/max_total_spent, min/max_order_count, priority, is_active, timestamps
- [ ] Crear `CustomerTierRepository` con método `findByIsActiveTrue()` ordenado por level DESC
- [ ] Crear `ClassificationRuleRepository` con métodos: `findByIsActiveTrueOrderByPriority()`, `findByCustomerTierUidAndIsActiveTrue()`
- [ ] Crear `ClassificationMatrixCache` interface/contract

#### Implementación — Application Layer (Services)
- [ ] Crear `CustomerTierService`: operaciones CRUD (crear tier, listar activos)
- [ ] Crear `ClassificationRuleService`: operaciones CRUD (crear regla, actualizar, listar activas)
- [ ] Crear `ClassificationMatrixService`: cargar matriz desde DB, invalidar caché (invocado por event consumer)
- [ ] Crear `ClassificationEngine`: método `classify(total_spent, order_count)` → StudentTierEntity (determinístico)
  - Cargar matriz activa desde caché (fallback a DB si vacía)
  - Evaluar todas las reglas
  - Seleccionar tier de mayor level entre los aplicables
  - Retornar ResponseDTO único tier
- [ ] Crear DTOs (Java Records):
  - `ClassificationCalculateRequest`: total_spent (BigDecimal), order_count (int)
  - `ClassificationCalculateResponse`: tier_uid, tier_name, tier_level, total_spent, order_count, calculated_at
  - `CustomerTierResponse`: tier_uid, name, level, is_active, timestamps
  - `ClassificationRuleRequest`: customer_tier_uid, min/max_total_spent, min/max_order_count, priority
  - `ClassificationRuleResponse`: rule_uid, customer_tier_uid, min/max_total_spent, min/max_order_count, priority, is_active, timestamps

#### Implementación — Presentation Layer (Controllers)
- [ ] Crear `ClassificationController`:
  - autentica por API Key, ejecuta `classificationEngine.classify()`, retorna 200 con tier
  - `GET /api/v1/classification/tiers` — lista`POST /api/v1/classification/calculate` —  tiers activos, retorna 200
- [ ] Crear `ClassificationAdminController` (endpoints ADMIN):
  - `POST /api/v1/admin/classification/tiers` — crea tier, retorna 201
  - `POST /api/v1/admin/classification/rules` — crea regla, publica evento, retorna 201
  - `PUT /api/v1/admin/classification/rules/{rule_uid}` — actualiza regla, publica evento, retorna 200
  - `GET /api/v1/admin/classification/rules` — lista reglas, retorna 200

#### Implementación — Infrastructure Layer (Cache + Events)
- [ ] Crear `CaffeineClassificationMatrixCache` implementando `ClassificationMatrixCache`
  - Inyectar en `ClassificationMatrixService`
  - TTL 10 minutos
  - Cargar matriz como: `Map<String, List<ClassificationRuleEntity>>` con key = "MATRIX" o by tier_uid
- [ ] Crear `ClassificationEventPublisher`: publica `CustomerClassificationMatrixUpdated` cuando regla se crea/actualiza
- [ ] Crear `ClassificationEventConsumer`: consume evento, invoca `classificationMatrixService.invalidateAndReload()`
- [ ] Crear `RabbitMQClassificationConfig`:
  - Declare exchange `classification-exchange` (Fanout)
  - Declare queue `classification.matrix.queue`
  - Declare binding entre exchange y queue
  - DLX para eventos muertos

#### Migrations
- [ ] Crear `V5__Create_customer_tiers_table.sql`:
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
- [ ] Crear `V6__Create_classification_rules_table.sql`:
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
- [ ] Crear `V7__Seed_default_tiers.sql`: Inserts Bronce, Plata, Oro, Platino
- [ ] Crear `V8__Seed_default_classification_rules.sql`: Inserts reglas de ejemplo

#### Exception Handlers
- [ ] Crear `ClassificationValidationException` si payload inválido (extiende `BadRequestException`)
- [ ] Registrar en `GlobalExceptionHandler` → HTTP 400 + mensaje descriptivo
- [ ] Crear `ClassificationMatrixUnavailableException` si caché y DB no disponibles → HTTP 503

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

### Documentation
- [ ] Actualizar README.md con descripción de clasificación de tiers y matriz de reglas
- [ ] Documentar matriz de clasificación por defecto (Bronce, Plata, Oro, Platino) y rangos

---

## Anexos

### A. Matriz de Clasificación Predeterminada (Seeders)

```
Tier: Bronce (Level 1)
  Regla-1: total_spent [0, 1000) AND order_count [0, 5)

Tier: Plata (Level 2)
  Regla-2: total_spent [1000, 5000) AND order_count [5, 10)

Tier: Oro (Level 3)
  Regla-3: total_spent [5000, 20000) AND order_count [10, 20)

Tier: Platino (Level 4)
  Regla-4: total_spent >= 20000 OR order_count >= 20
```

### B. Flujo de Evaluación Determinística

```
Input: { total_spent: 7500, order_count: 12 }

Paso 1: Cargar matriz de caché (o DB si caché vacía)
Paso 2: Evaluar todas las reglas activas:
  - Regla-1 (Bronce): 7500 en [0-1000)? NO
  - Regla-2 (Plata): 7500 en [1000-5000)? NO
  - Regla-3 (Oro): 7500 en [5000-20000)? SÍ, 12 en [10-20)? SÍ → Aplicable
  - Regla-4 (Platino): 7500 >= 20000? NO, 12 >= 20? NO → No aplicable
Paso 3: Tiers aplicables = [Oro]
Paso 4: Seleccionar tier de mayor level = Oro (level 3)
Paso 5: Retornar { tier_uid: "...", tier_name: "Oro", tier_level: 3 }

Output: Oro tier asignado determinísticamente
```

### C. Endpoint Context (Sin X-User-ID)

```http
POST /api/v1/classification/calculate
X-API-Key: 123e4567-e89b-12d3-a456-426614174000
Content-Type: application/json

{
  "total_spent": "7500.50",
  "order_count": 12
}

→ Response 200:
{
  "tier_uid": "00000000-0000-0000-0000-000000000003",
  "tier_name": "Oro",
  "tier_level": 3,
  "total_spent": "7500.50",
  "order_count": 12,
  "calculated_at": "2026-03-26T14:30:00Z"
}
```

---

**Generado por:** spec-generator  
**Fecha:** 2026-03-26  
**Versión:** 1.0
