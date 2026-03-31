---
id: SPEC-006
status: APPROVED
feature: seasonal-rules
created: 2026-03-30
updated: 2026-03-30
author: spec-generator
version: "1.0"
related-specs: []
---

# Spec: Gestión de Reglas de Temporada (HU-06)

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de iniciar implementación.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPRECATED

---

## 1. REQUERIMIENTOS

### Descripción
Sistema para crear, editar y eliminar reglas de descuento de temporada que se aplican automáticamente durante períodos específicos del año. Las reglas se evalúan en memoria en el Engine Service para no impactar la latencia del endpoint `/calculate`, con sincronización en tiempo real hacia el Engine via RabbitMQ cuando cambian en Admin Service.

### Requerimiento de Negocio
Como usuario de LOYALTY, quiero crear, editar y eliminar reglas de temporada para automatizar las promociones por demanda de temporada.

### Historias de Usuario

#### HU-06.1: Crear una regla de temporada

```
Como:        Administrador del ecommerce
Quiero:      crear una nueva regla de descuento con vigencia desde una fecha hasta otra
Para:        automatizar promociones en temporadas específicas (Black Friday, Navidad, etc.)

Prioridad:   Alta
Estimación:  M
Dependencias: Ninguna
Capa:        Backend (Service-Admin) + Frontend (Admin Dashboard)
```

#### Criterios de Aceptación — HU-06.1

**Happy Path**
```gherkin
CRITERIO-1.1: Creación exitosa de una regla de temporada
  Dado que:   no hay una regla activa (con superposición de fechas) para ese ecommerce
  Cuando:     se registra una regla con nombre, fecha inicio, fecha fin, % descuento, tipo válidos
  Entonces:   la regla queda almacenada en base de datos
  Y:          la regla se publica a RabbitMQ (evento SeasonalRuleCreated)
  Y:          queda disponible inmediatamente en caché del Engine
  Y:          retorna 201 Created con uid, nombre, fechas, % descuento, timestamps
```

**Error Path**
```gherkin
CRITERIO-1.2: Rechazo por superposición de fechas
  Dado que:   existe una regla activa para el ecommerce con rango 2026-12-01 a 2026-12-31
  Cuando:     se intenta crear una regla con rango 2026-12-15 a 2026-12-25 (superpone)
  Entonces:   el sistema rechaza el registro con 409 Conflict
  Y:          el mensaje de error especifica: "Existe una regla de temporada activa en el rango especificado"
```

**Edge Case**
```gherkin
CRITERIO-1.3: Rechazo por descuento fuera de límites
  Dado que:   los límites globales de descuento son mín=5% y máx=50%
  Cuando:     se intenta crear una regla con descuento=60%
  Entonces:   el sistema rechaza con 400 Bad Request
  Y:          el error es: "Descuento fuera de los límites permitidos (5% - 50%)"
```

#### HU-06.2: Editar una regla de temporada

```
Como:        Administrador del ecommerce
Quiero:      actualizar las condiciones de una regla (fechas, %, tipo) sin cambiar su uid
Para:        ajustar promociones según cambios en la demanda

Prioridad:   Alta
Estimación:  M
Dependencias: HU-06.1 (debe existir una regla)
Capa:        Backend + Frontend
```

#### Criterios de Aceptación — HU-06.2

**Happy Path**
```gherkin
CRITERIO-2.1: Edición exitosa de una regla
  Dado que:   existe una regla con uid=abc-123
  Cuando:     se actualiza con nuevos valores: fecha_fin=2026-12-31, descuento=35%
  Entonces:   la regla se actualiza en base de datos
  Y:          se publica evento SeasonalRuleUpdated a RabbitMQ
  Y:          el caché del Engine se invalida inmediatamente
  Y:          retorna 200 OK con la regla actualizada
```

**Error Path**
```gherkin
CRITERIO-2.2: Rechazo por edición de regla inexistente
  Dado que:   no existe una regla con uid=xyz-999
  Cuando:     se solicita actualizar ese uid
  Entonces:   el sistema rechaza con 404 Not Found
  Y:          el error es: "Regla de temporada no encontrada para el identificador xyz-999"
```

```gherkin
CRITERIO-2.3: Rechazo por superposición después de edición
  Dado que:   existe regla A (2026-12-01 a 2026-12-31) y regla B (2026-12-15 a 2026-12-25)
  Cuando:     se intenta editar B para overlappear con A (modificando fecha_inicio)
  Entonces:   el sistema rechaza con 409 Conflict
  Y:          mantiene la última configuración válida de B
```

#### HU-06.3: Eliminar una regla de temporada

```
Como:        Administrador del ecommerce
Quiero:      eliminar una regla que ya no va a usarse
Para:        limpiar el sistema de temporadas obsoletas

Prioridad:   Media
Estimación:  S
Dependencias: HU-06.1
Capa:        Backend + Frontend
```

#### Criterios de Aceptación — HU-06.3

**Happy Path**
```gherkin
CRITERIO-3.1: Eliminación exitosa de una regla
  Dado que:   existe una regla con uid=abc-123
  Cuando:     se confirma su eliminación
  Entonces:   la regla se marca como eliminada en base de datos (soft delete: is_active=false)
  Y:          se publica evento SeasonalRuleDeleted a RabbitMQ
  Y:          la regla deja de participar en futuras evaluaciones del Engine
  Y:          retorna 204 No Content
```

**Error Path**
```gherkin
CRITERIO-3.2: Rechazo de eliminación de regla inexistente
  Dado que:   no existe una regla con uid=xyz-999
  Cuando:     se solicita su eliminación
  Entonces:   el sistema rechaza con 404 Not Found
  Y:          el error es: "Regla de temporada no encontrada para el identificador xyz-999"
```

#### HU-06.4: Listar reglas de temporada

```
Como:        Administrador del ecommerce
Quiero:      ver todas las reglas de temporada registradas
Para:        revisar y administrar las promociones activas

Prioridad:   Media
Estimación:  S
Dependencias: HU-06.1
Capa:        Backend + Frontend
```

#### Criterios de Aceptación — HU-06.4

**Happy Path**
```gherkin
CRITERIO-4.1: Listado exitoso de reglas
  Dado que:   existen 3 reglas de temporada para el ecommerce
  Cuando:     se solicita GET /api/v1/seasonal-rules
  Entonces:   retorna 200 OK con array de reglas (solo activas: is_active=true)
  Y:          cada regla incluye: uid, nombre, fecha_inicio, fecha_fin, descuento, tipo, timestamps
```

---

## 2. DISEÑO

### Modelos de Datos

#### Entidades afectadas

| Entidad | Almacén | Tipo | Descripción |
|---------|---------|------|-------------|
| `SeasonalRuleEntity` | tabla `seasonal_rules` en `loyalty_admin` | **nueva (Service-Admin)** | System of Record: datos maestros de reglas |
| `SeasonalRuleEntity` | tabla `seasonal_rules` en `loyalty_engine` | **nueva (Service-Engine)** | Réplica: sincronizada via RabbitMQ, usada en caché Caffeine |

#### Tabla: `seasonal_rules` (Service-Admin: loyalty_admin + Service-Engine: loyalty_engine)

Misma estructura en ambas BDs. Admin es fuente de verdad (System of Record), Engine la replica.

```sql
CREATE TABLE seasonal_rules (
  uid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  ecommerce_id UUID NOT NULL,
  name VARCHAR(255) NOT NULL,
  description VARCHAR(1000),
  discount_percentage NUMERIC(5,2) NOT NULL,  -- BigDecimal: 0.00 - 100.00
  discount_type VARCHAR(50) NOT NULL,         -- PERCENTAGE, FIXED_AMOUNT
  start_date TIMESTAMP WITH TIME ZONE NOT NULL,
  end_date TIMESTAMP WITH TIME ZONE NOT NULL,
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  
  -- Constraints
  CONSTRAINT fk_ecommerce FOREIGN KEY (ecommerce_id) REFERENCES ecommerces(uid),
  CONSTRAINT discount_percentage_range CHECK (discount_percentage >= 0 AND discount_percentage <= 100),
  CONSTRAINT valid_date_range CHECK (start_date < end_date),
  CONSTRAINT unique_seasonal_rule_per_ecommerce 
    UNIQUE (ecommerce_id, start_date, end_date) WHERE is_active = true
);

CREATE INDEX idx_seasonal_rules_ecommerce_id ON seasonal_rules(ecommerce_id);
CREATE INDEX idx_seasonal_rules_date_range ON seasonal_rules(start_date, end_date);
CREATE INDEX idx_seasonal_rules_active ON seasonal_rules(is_active);
```

#### Campos del modelo

| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `uid` | UUID | sí | auto-generado | Identificador único |
| `ecommerce_id` | UUID | sí | FK a ecommerces | Ecommerce asociado |
| `name` | string | sí | 1-255 chars | Nombre de la temporada (ej: "Black Friday 2026") |
| `description` | string | no | max 1000 chars | Descripción adicional |
| `discount_percentage` | BigDecimal | sí | 0-100 | Porcentaje de descuento (6 dígitos totales, 2 decimales) |
| `discount_type` | enum | sí | PERCENTAGE, FIXED_AMOUNT | Tipo de descuento a aplicar |
| `start_date` | TIMESTAMP WITH TIME ZONE | sí | < end_date | Inicio de vigencia (ISO 8601, ej: 2026-11-28T00:00:00Z) |
| `end_date` | TIMESTAMP WITH TIME ZONE | sí | > start_date | Fin de vigencia (ISO 8601, ej: 2026-12-02T23:59:59Z) |
| `is_active` | boolean | sí | default true | Soft delete flag |
| `created_at` | datetime (UTC) | sí | auto-generado | Timestamp creación |
| `updated_at` | datetime (UTC) | sí | auto-generado | Timestamp actualización |

#### Índices / Constraints
- **Unique constraint**: `(ecommerce_id, start_date, end_date)` donde `is_active=true` — prevent overlapping dates per ecommerce
- **Index**: `ecommerce_id` para consultas por ecommerce
- **Index**: `(start_date, end_date)` para range queries eficientes
- **Index**: `is_active` para filtrar solo reglas activas

### API REST (Service-Admin — Sistema de Record)

**Arquitectura**: 
- **Service-Admin** (puerto 8081) expone estos 5 endpoints HTTP protegidos con JWT
- **Service-Engine** (puerto 8082) NO expone endpoints de escritura. Solo consume eventos RabbitMQ.
- Frontend siempre llama a **Service-Admin** (nunca al Engine)
- Cuando Admin cambia una regla, publica evento → Engine lo replica en su caché

#### POST /api/v1/seasonal-rules
- **Descripción**: Crear una nueva regla de temporada
- **Auth requerida**: sí (JWT con ecommerce_id)
- **Service**: Service-Admin (8081)
- **Request Body**:
  ```json
  {
    "name": "Black Friday 2026",
    "description": "Descuento especial Black Friday",
    "discount_percentage": 30,
    "discount_type": "PERCENTAGE",
    "start_date": "2026-11-28T00:00:00Z",
    "end_date": "2026-12-02T23:59:59Z"
  }
  ```
- **Response 201 Created**:
  ```json
  {
    "uid": "550e8400-e29b-41d4-a716-446655440000",
    "ecommerce_id": "123e4567-e89b-12d3-a456-426614174000",
    "name": "Black Friday 2026",
    "description": "Descuento especial Black Friday",
    "discount_percentage": 30,
    "discount_type": "PERCENTAGE",
    "start_date": "2026-11-28T00:00:00Z",
    "end_date": "2026-12-02T23:59:59Z",
    "is_active": true,
    "created_at": "2026-03-30T10:00:00Z",
    "updated_at": "2026-03-30T10:00:00Z"
  }
  ```
- **Response 400 Bad Request**: Campo obligatorio faltante, valores inválidos, o descuento fuera de límites
  ```json
  {
    "error": "VALIDATION_ERROR",
    "message": "Descuento fuera de los límites permitidos (5% - 50%)",
    "timestamp": "2026-03-30T10:00:00Z"
  }
  ```
- **Response 401 Unauthorized**: Token ausente o expirado
- **Response 409 Conflict**: Superposición de fechas con regla existente
  ```json
  {
    "error": "CONFLICT",
    "message": "Existe una regla de temporada activa en el rango especificado (2026-12-15 a 2026-12-25)",
    "timestamp": "2026-03-30T10:00:00Z"
  }
  ```

#### GET /api/v1/seasonal-rules
- **Descripción**: Listar todas las reglas de temporada activas del ecommerce autenticado
- **Auth requerida**: sí
- **Service**: Service-Admin (8081)
- **Query Params** (opcionales):
  - `page`: número de página (default: 0)
  - `size`: tamaño de página (default: 20, max: 100)
- **Response 200 OK**:
  ```json
  {
    "content": [
      {
        "uid": "550e8400-e29b-41d4-a716-446655440000",
        "ecommerce_id": "123e4567-e89b-12d3-a456-426614174000",
        "name": "Black Friday 2026",
        "discount_percentage": 30,
        "discount_type": "PERCENTAGE",
        "start_date": "2026-11-28T00:00:00Z",
        "end_date": "2026-12-02T23:59:59Z",
        "created_at": "2026-03-30T10:00:00Z"
      }
    ],
    "totalElements": 5,
    "totalPages": 1,
    "currentPage": 0
  }
  ```

#### GET /api/v1/seasonal-rules/{uid}
- **Descripción**: Obtener una regla de temporada por uid
- **Auth requerida**: sí
- **Service**: Service-Admin (8081)
- **Response 200 OK**: Regla completa (ver POST response)
- **Response 404 Not Found**:
  ```json
  {
    "error": "NOT_FOUND",
    "message": "Regla de temporada no encontrada para el identificador xyz-999",
    "timestamp": "2026-03-30T10:00:00Z"
  }
  ```

#### PUT /api/v1/seasonal-rules/{uid}
- **Descripción**: Actualizar una regla de temporada
- **Auth requerida**: sí
- **Service**: Service-Admin (8081)
- **Request Body** (fields are optional for partial updates):
  ```json
  {
    "name": "Black Friday 2026 Extended",
    "description": "",
    "discount_percentage": 35,
    "discount_type": "PERCENTAGE",
    "start_date": "2026-11-28T00:00:00Z",
    "end_date": "2026-12-03T23:59:59Z"
  }
  ```
- **Response 200 OK**: Regla actualizada
- **Response 400 Bad Request**: Validación falla (descuento fuera de límites, fechas inválidas, etc.)
- **Response 404 Not Found**: Regla inexistente
- **Response 409 Conflict**: Nueva superposición de fechas detectada

#### DELETE /api/v1/seasonal-rules/{uid}
- **Descripción**: Eliminar (soft delete) una regla de temporada
- **Auth requerida**: sí
- **Service**: Service-Admin (8081)
- **Response 204 No Content**: Eliminado exitosamente
- **Response 404 Not Found**: Regla inexistente

### RabbitMQ Event-Driven Communication

**Flujo**: Service-Admin publica → Service-Engine consume (asincrónico, en tiempo real)

```
Admin guarda regla      → Publica evento SeasonalRuleCreated
                        ↓ (RabbitMQ)
Engine Consumer         → Recibe evento
                        ↓
                        - Invalida caché Caffeine
                        - Persiste en tabla seasonal_rules (loyalty_engine)
                        - Precarga en caché para siguiente evaluación
                        ↓
Engine /calculate       → Consulta caché (sin DB hit)
```

**Responsabilidades**:
- **Service-Admin** (Publisher): Publica 3 eventos (created, updated, deleted) a RabbitMQ
- **Service-Engine** (Consumer): Escucha esos eventos e invalida caché + replica en BD

#### Events publicados por Service-Admin

#### SeasonalRuleCreated
Publicado **por Service-Admin** cuando se crea una regla con éxito.
```json
{
  "event_type": "SeasonalRuleCreated",
  "rule_uid": "550e8400-e29b-41d4-a716-446655440000",
  "ecommerce_id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "Black Friday 2026",
  "discount_percentage": 30,
  "discount_type": "PERCENTAGE",
  "start_date": "2026-11-28T00:00:00Z",
  "end_date": "2026-12-02T23:59:59Z",
  "timestamp": "2026-03-30T10:00:00Z"
}
```
**Consumer (Service-Engine)**: Recibe este evento e inserta la regla en tabla `seasonal_rules` (loyalty_engine) + invalida caché.

#### SeasonalRuleUpdated
Publicado **por Service-Admin** cuando se actualiza una regla.
```json
{
  "event_type": "SeasonalRuleUpdated",
  "rule_uid": "550e8400-e29b-41d4-a716-446655440000",
  "ecommerce_id": "123e4567-e89b-12d3-a456-426614174000",
  "discount_percentage": 35,
  "timestamp": "2026-03-30T10:00:00Z"
}
```

#### Event: `SeasonalRuleDeleted`
Publicado por **Service-Admin** cuando se elimina una regla.
```json
{
  "event_type": "SeasonalRuleDeleted",
  "rule_uid": "550e8400-e29b-41d4-a716-446655440000",
  "ecommerce_id": "123e4567-e89b-12d3-a456-426614174000",
  "timestamp": "2026-03-30T10:00:00Z"
}
```

**RabbitMQ Configuration**:
- **Exchange**: `seasonal-exchange` (type: **fanout**, DURABLE)
- **Queue**: `seasonal-rules-queue` (DURABLE, consumida por Service-Engine)
- **Consumer** (Service-Engine): Invalida caché Caffeine e inserta/actualiza en tabla `seasonal_rules` (DB `loyalty_engine`)
- **DLX** (Dead Letter Exchange): `seasonal-dlx` con retry logic por reintento
- **Message Durability**: Mensajes son persistentes (PERSISTENT = true)
- **Consumer Acknowledgment**: MANUAL (el consumer hace ACK solo después de guardar exitosamente)

#### Idempotencia y Tolerancia a Fallos (Patrón UPSERT)

**Problema**: Si RabbitMQ reintenta un evento (network timeout, consumer crash), el reader debe procesar el mismo mensaje 2+ veces sin duplicados ni inconsistencias.

**Solución**: Lógica UPSERT en el consumer:
```java
@RabbitListener(queues = "seasonal-rules-queue")
public void consumeSeasonalRuleEvent(SeasonalRuleEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
  try {
    // UPSERT pattern: buscar por (ecommerce_id, uid)
    Optional<SeasonalRuleEntity> existing = repository.findByUidAndEcommerceId(event.ruleUid(), event.ecommerceId());
    
    SeasonalRuleEntity entity = existing.orElseGet(() -> new SeasonalRuleEntity());
    entity.setUid(event.ruleUid());
    entity.setEcommerceId(event.ecommerceId());
    entity.setName(event.name());
    entity.setStartDate(event.startDate());
    entity.setEndDate(event.endDate());
    entity.setDiscountPercentage(event.discountPercentage());
    entity.setActive(event.isActive());
    entity.setUpdatedAt(Instant.now(Clock.systemUTC()));
    
    repository.save(entity);  // INSERT or UPDATE (idempotent)
    cacheManager.invalidate(event.ecommerceId());
    
    // Manual ACK: confirmar al broker que procesamos exitosamente
    channel.basicAck(deliveryTag, false);
    
  } catch (Exception e) {
    log.error("Failed to process seasonal rule event, will retry", e);
    // NACK + requeue: mensaje vuelve a la cola
    channel.basicNack(deliveryTag, true);
    throw e;  // Let container handle retry
  }
}
```

**Garantías**:
- Idempotencia: Procesamiento repetido del mismo evento = mismo resultado final (no duplicados)
- At-least-once delivery: RabbitMQ garantiza que el mensaje llega al menos 1 vez
- Combinadas: Reintento seguro sin efectos secundarios

### Validaciones y Reglas de Negocio

1. **Rango de descuento global (Contrato Admin ↔ Engine)**
   - El **Admin Service** define y almacena los límites globales de descuento (ej: mín 5%, máx 50%)
   - En tabla `discount_configuration` del Admin (ya existe)
   - Cuando el Admin **crea/edita** una regla de temporada, valida contra estos límites **localmente**
   - Si los límites globales **cambian** en Admin, publica evento `DiscountConfigurationUpdated` a RabbitMQ
   - El **Engine Service** consume este evento, actualiza su configuración en caché + réplica en DB
   - **Validación en POST/PUT**: `if (discountPercentage < config.minDiscount OR discountPercentage > config.maxDiscount) → 400 Bad Request`

2. **No superposición de fechas** (TIMESTAMP WITH TIME ZONE)
   - No puede haber dos reglas activas (`is_active=true`) del mismo ecommerce con períodos superpuestos
   - Validar: `SELECT * FROM seasonal_rules WHERE ecommerce_id=? AND is_active=true AND (start_date, end_date) OVERLAPS (?, ?)`
   - Esto permite reglas que terminan exactamente cuando otra empieza (ej: end_date=2026-12-02T23:59:59Z, next start_date=2026-12-03T00:00:00Z)

3. **Precisión de Tiempos**
   - `start_date` / `end_date` son **TIMESTAMP WITH TIME ZONE** en UTC (o zona horaria del ecommerce)
   - El "Black Friday" empieza exactamente a las 00:00:00 del primer día y termina a las 23:59:59 del último
   - Ejemplo: `start_date: "2026-11-28T00:00:00Z"`, `end_date: "2026-12-02T23:59:59Z"`
   - El Engine evalúa: `NOW() >= start_date AND NOW() < end_date` (usando TIMESTAMP)

4. **Validación de Fechas**
   - `start_date` < `end_date` (ambas obligatorias)
   - Ambas en futuro o presente (opcional según reglas de negocio)

5. **Soft Delete**
   - Usar flag `is_active=false`, no eliminar registros físicamente
   - Listados y cálculos solo leen reglas con `is_active=true`

### Diseño Frontend

#### Componentes nuevos
| Componente | Archivo | Props principales | Descripción |
|------------|---------|------------------|-------------|
| `SeasonalRuleCard` | `components/SeasonalRuleCard.jsx` | `rule, onDelete, onEdit` | Tarjeta de una regla (nombre, fechas, %) |
| `SeasonalRuleFormModal` | `components/SeasonalRuleFormModal.jsx` | `isOpen, rule, onSubmit, onClose` | Modal para crear/editar regla |
| `SeasonalRulePage` | `pages/SeasonalRulePage.jsx` | - | Página principal de administración |

#### Páginas nuevas
| Página | Archivo | Ruta | Protegida |
|--------|---------|------|-----------|
| `SeasonalRulePage` | `pages/SeasonalRulePage.jsx` | `/admin/seasonal-rules` | sí |

#### Hooks y State
| Hook | Archivo | Retorna | Descripción |
|------|---------|---------|-------------|
| `useSeasonalRules` | `hooks/useSeasonalRules.js` | `{ rules, loading, error, create, update, delete, refetch }` | CRUD + estado |

#### Services (llamadas API)

**Importante**: Las requests van a **Service-Admin (puerto 8081)**, que es el System of Record.

| Función | Archivo | Endpoint |
|---------|---------|---------|
| `getSeasonalRules(token)` | `services/seasonalRuleService.js` | `GET /api/v1/seasonal-rules` (Service-Admin) |
| `getSeasonalRule(uid, token)` | `services/seasonalRuleService.js` | `GET /api/v1/seasonal-rules/{uid}` (Service-Admin) |
| `createSeasonalRule(data, token)` | `services/seasonalRuleService.js` | `POST /api/v1/seasonal-rules` (Service-Admin) |
| `updateSeasonalRule(uid, data, token)` | `services/seasonalRuleService.js` | `PUT /api/v1/seasonal-rules/{uid}` (Service-Admin) |
| `deleteSeasonalRule(uid, token)` | `services/seasonalRuleService.js` | `DELETE /api/v1/seasonal-rules/{uid}` (Service-Admin) |

#### Estilos
- Modal para edición: `SeasonalRuleFormModal.module.css`
- Tarjeta de regla: `SeasonalRuleCard.module.css`
- Página principal: `SeasonalRulePage.module.css`
- Usar **CSS Modules**, no Tailwind ni clases globales

#### Validaciones Cliente
- Fechas: `start_date` < `end_date`
- Descuento: 5-50%
- Nombre: 1-255 caracteres
- Mostrar errores del servidor (409 Conflict, 400 Bad Request)

### Arquitectura y Dependencias

#### Service-Admin (Sistema de Record — CRUD)

**Base de datos**: `loyalty_admin`
- Tabla `seasonal_rules` con datos maestros
- Migraciones: `V11__Create_seasonal_rules_table.sql` en `service-admin/resources/db/migration`

**Nuevos paquetes**:
- `com.loyalty.service_admin.domain.entity.SeasonalRuleEntity`
- `com.loyalty.service_admin.domain.repository.SeasonalRuleRepository`
- `com.loyalty.service_admin.application.service.SeasonalRuleService`
- `com.loyalty.service_admin.application.dto.SeasonalRuleCreateRequest`, `SeasonalRuleUpdateRequest`, `SeasonalRuleResponse`
- `com.loyalty.service_admin.application.mapper.SeasonalRuleMapper`
- `com.loyalty.service_admin.presentation.controller.SeasonalRuleController` (5 endpoints)
- `com.loyalty.service_admin.infrastructure.event.SeasonalRuleEventPublisher` (publica eventos a RabbitMQ)

**Responsabilidades**:
- CRUD completo: POST, GET, GET/{uid}, PUT, DELETE
- Validaciones: superposición de fechas, descuento en rango, nombres únicos
- Publicar eventos SeasonalRuleCreated/Updated/Deleted a RabbitMQ

#### Service-Engine (Motor de Evaluación — Caché + Réplica)

**Base de datos**: `loyalty_engine`
- Tabla `seasonal_rules` (réplica de Admin Service)
- Tabla `discount_configuration` (réplica de Admin Service, ya existente)
- Migraciones: `V11__Create_seasonal_rules_table.sql` en `service-engine/resources/db/migration`

**Nuevos paquetes**:
- `com.loyalty.service_engine.domain.entity.SeasonalRuleEntity`
- `com.loyalty.service_engine.domain.repository.SeasonalRuleRepository`
- `com.loyalty.service_engine.infrastructure.event.SeasonalRuleEventConsumer` (escucha RabbitMQ)
- `com.loyalty.service_engine.infrastructure.cache.SeasonalRulesCacheManager` (Caffeine cache)

**Responsabilidades**:
- **Consumer RabbitMQ**: 
  - `SeasonalRuleCreated/Updated/Deleted` → Invalida caché, persiste en tabla
  - `DiscountConfigurationUpdated` → Actualiza límites (min/max) en caché
- **Cold Start**: Al iniciar, carga:
  - Reglas desde tabla `seasonal_rules` en caché Caffeine
  - Límites desde tabla `discount_configuration` en caché
- **Endpoint `/calculate`**: Consulta caché directo (sin acceso a DB) para latencia <100ms

#### RabbitMQ Configuration

Compartida entre ambos servicios:
- **Exchange**: `seasonal-exchange` (DLX: `seasonal-dlx`)
- **Queue**: `seasonal-rules-queue` (consumida por Service-Engine)
- **Routing Keys**: `seasonal.rule.created`, `seasonal.rule.updated`, `seasonal.rule.deleted`
- **DLX Queue**: `seasonal-rules-dlx-queue` (reintentos)

#### Dependencias Externas

- Spring Data JPA (repositorios)
- RabbitMQ (eventos Publisher/Consumer)
- Caffeine Cache (Service-Engine)
- Javax.validation (anotaciones `@Valid`, `@NotNull`, etc.)
- Flyway (migraciones)

### Notas de Implementación

> 1. **Separación de servicios**: Service-Admin es System of Record (CRUD + config maestro de límites), Service-Engine es motor de evaluación (caché).
> 2. **Flujo de eventos**: Cada cambio en Admin Service publica evento inmediatamente a RabbitMQ:
>    - `SeasonalRuleCreated` / `Updated` / `Deleted` → Engine consume e invalida caché
>    - `DiscountConfigurationUpdated` → Engine consume e actualiza límites máximos en caché
> 3. **Cold Start**: Al reiniciar Service-Engine, carga reglas desde tabla `seasonal_rules` (loyalty_engine) a caché Caffeine.
> 4. **Estrategia de Caché (Caffeine)**:
>    - TTL: **60 minutos** (fue 10, pero con RabbitMQ invalidando inmediatamente, es seguro aumentarlo)
>    - Clave: `ecommerceId` → Lista de reglas activas
>    - **Invalidación proactiva**: RabbitMQ events disparan invalidación inmediata del caché (no esperar TTL)
>    - Fallback: Si no hay caché, consulta tabla `loyalty_engine.seasonal_rules` directamente
> 5. **Precisión de Tiempos**: TIMESTAMP WITH TIME ZONE en todas partes. NO usar DATE. Permite preguntas como "¿es Black Friday ahora?" con exactitud de segundo.
> 6. **Soft Delete**: No eliminar registros, marcar `is_active=false`. Permite auditoría histórica.
> 7. **Contrato de Límites**: Admin define min/max descuento. Si cambian, publica evento → Engine actualiza caché. Admin valida POST/PUT localmente.
> 8. **BigDecimal obligatorio**: Todos los montos de descuento (BD como `NUMERIC(5,2)`, Java como `BigDecimal`).
> 9. **Tests**: Usar H2 con sufijo `_test` en application-test.properties. Testcontainers para PostgreSQL si H2 tiene issues con Flyway.

---

## 3. LISTA DE TAREAS

> Checklist accionable para todos los agentes. Marcar cada ítem (`[x]`) al completarlo.
> El Orchestrator monitorea este checklist para determinar el progreso.

### Backend (Service-Admin + Service-Engine)

#### Service-Admin (CRUD & Publisher)

**Implementación**
- [ ] Crear migration `V11__Create_seasonal_rules_table.sql` en `service-admin/resources/db/migration` con tabla, índices, constraints
- [ ] Crear `SeasonalRuleEntity` en domain/entity
- [ ] Crear `SeasonalRuleRepository` en domain/repository (con query: `findByEcommerceIdAndIsActive...(UUID, boolean)`)
- [ ] Crear DTOs: `SeasonalRuleCreateRequest`, `SeasonalRuleUpdateRequest`, `SeasonalRuleResponse`
- [ ] Crear `SeasonalRuleMapper` en application/mapper
- [ ] **Cargar configuración de límites**: Inyectar `DiscountConfigurationService` en `SeasonalRuleService`
  - Obtener límites (min/max) del ecommerce en cada POST/PUT
  - Validar: `if (discount < config.minDiscount OR discount > config.maxDiscount) → 400 Bad Request`
- [ ] Crear `SeasonalRuleService` en application/service con métodos:
  - `createSeasonalRule(request, ecommerceId)` — validar overlap, descuento vs límites, publish event
  - `getSeasonalRules(ecommerceId, page, size)` — list activas
  - `getSeasonalRule(uid, ecommerceId)` — by uid
  - `updateSeasonalRule(uid, request, ecommerceId)` — validar overlap, limits, publish event
  - `deleteSeasonalRule(uid, ecommerceId)` — soft delete, publish event
  - `validateDateOverlap(ecommerceId, startDate, endDate, excludeUid)` — check unique range
  - `validateDiscountRange(discountPercentage, config)` — check vs límites cargados
- [ ] Crear `SeasonalRuleEventPublisher` en infrastructure/event (publica a RabbitMQ)
- [ ] Crear `SeasonalRuleController` en presentation/controller con 5 endpoints REST (POST, GET, GET/{uid}, PUT, DELETE)
- [ ] Validaciones con `@Valid`, `@NotNull`, `@Min`, `@Max` en DTOs
- [ ] Exception handlers para ResourceNotFoundException, ConflictException (si no existen)

**Tests (Service-Admin)**
- [ ] `test_service_create_success` — happy path
- [ ] `test_service_create_overlap_raises_conflict` — overlap detection
- [ ] `test_service_create_discount_exceeds_limit_raises_error` — validation
- [ ] `test_service_delete_nonexistent_raises_not_found` — error handling
- [ ] `test_repository_find_by_ecommerce_id_and_active` — query logic
- [ ] `test_controller_post_returns_201` — HTTP endpoint
- [ ] `test_controller_post_returns_409_on_overlap` — HTTP error handling
- [ ] `test_controller_get_returns_200_paged_list` — paging
- [ ] `test_publisher_sends_event_on_create` — RabbitMQ event publish

#### Service-Engine (Consumer & Cache Manager)

**Implementación**
- [ ] Crear migration `V11__Create_seasonal_rules_table.sql` en `service-engine/resources/db/migration` con tabla, índices, constraints (misma estructura que Admin)
- [ ] Crear `SeasonalRuleEntity` en domain/entity
- [ ] Crear `SeasonalRuleRepository` en domain/repository
- [ ] Crear `SeasonalRulesCacheManager` en infrastructure/cache:
  - Caffeine cache con TTL: **60 minutos**, mapKey: `ecommerceId`
  - Métodos: `get(ecommerceId)`, `invalidate(ecommerceId)`, `invalidateAll()`
  - Estructura: `Map<UUID ecommerceId, List<SeasonalRuleEntity> rules>`
  - Caché de límites globales: `Map<UUID ecommerceId, DiscountConfiguration>`
- [ ] Crear `SeasonalRuleEventConsumer` en infrastructure/event — escucha 3 events (created, updated, deleted)
  - Invalida caché (ecommerceId)
  - Persiste/actualiza/elimina en tabla seasonal_rules (loyalty_engine)
- [ ] Crear `DiscountConfigurationEventConsumer` en infrastructure/event — escucha `DiscountConfigurationUpdated`
  - Actualiza caché de límites globales
  - Persiste cambios en tabla discount_configuration (loyalty_engine)
- [ ] Configuración RabbitMQ: `SeasonalRulesRabbitConfig` (exchange, queue, bindings, DLX)
- [ ] Crear `SeasonalRuleApplicationService` (opcional) — getter para caché con fallback a BD
- [ ] **Startup Event**: Al iniciar ServiceEngineApplication, cargar:
  - Todas las reglas activas de tabla en caché
  - Configuración de límites globales en caché

**Tests (Service-Engine)**
- [ ] `test_cache_loads_rules_on_startup` — cold start (todas las reglas activas en caché)
- [ ] `test_cache_loads_discount_limits_on_startup` — cold start (límites globales en caché)
- [ ] `test_cache_invalidates_on_created_event` — consumer invalida caché cuando SeasonalRuleCreated
- [ ] `test_cache_invalidates_on_deleted_event` — consumer invalida caché cuando SeasonalRuleDeleted
- [ ] `test_cache_updates_limits_on_config_event` — consumer actualiza límites cuando DiscountConfigurationUpdated
- [ ] `test_repository_insert_replica` — inserción en tabla durante replicación de evento
- [ ] `test_consumer_handles_create_event` — event processing (crea entrada en tabla)
- [ ] `test_consumer_handles_delete_event` — soft delete syncing (marca is_active=false)
- [ ] `test_concurrent_cache_access` — thread safety con virtual threads
- [ ] `test_cache_ttl_60_minutes` — verificar que TTL es 1 hora (no expiración temprana)

### Frontend (React/Vite)

#### Implementación
- [ ] Crear `seasonalRuleService.js` con funciones: `getSeasonalRules`, `createSeasonalRule`, `updateSeasonalRule`, `deleteSeasonalRule`, `getSeasonalRule`
- [ ] Crear `useSeasonalRules.js` hook con estado y acciones (create, update, delete, refetch)
- [ ] Crear `SeasonalRuleCard.jsx` componente que muestre: nombre, fechas, descuento, botones editar/eliminar
- [ ] Crear `SeasonalRuleFormModal.jsx` modal para crear y editar (validación cliente)
- [ ] Crear `SeasonalRulePage.jsx` página principal con: lista de reglas, botón crear, modal, loading/error
- [ ] Crear estilos CSS Modules para Card, FormModal, Page
- [ ] Registrar ruta `/admin/seasonal-rules` en `src/App.jsx` (protegida con `<ProtectedRoute>`)
- [ ] Validaciones cliente: fechas, rango descuento, nombre length
- [ ] Mensajes de error amigables al usuario (409 Conflict, 400 Bad Request)
- [ ] Feedback: loading spinner, success toast, error toast

#### Tests Frontend
- [ ] `SeasonalRuleCard renders name and dates correctly`
- [ ] `SeasonalRuleCard calls onDelete when delete button clicked`
- [ ] `SeasonalRuleCard calls onEdit when edit button clicked`
- [ ] `SeasonalRuleFormModal submits form with correct data`
- [ ] `SeasonalRuleFormModal validates date range`
- [ ] `SeasonalRuleFormModal validates discount percentage 5-50%`
- [ ] `useSeasonalRules loads rules on mount`
- [ ] `useSeasonalRules handles create error gracefully`
- [ ] `useSeasonalRules handles overlap conflict (409) with user message`
- [ ] `SeasonalRulePage renders list of rules`
- [ ] `SeasonalRulePage opens modal on create button click`

### QA
- [ ] Leer spec y entender criterios de aceptación
- [ ] Ejecutar skill `/gherkin-case-generator` con criterios CRITERIO-1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 3.1, 3.2, 4.1
- [ ] Ejecutar skill `/risk-identifier` para análisis ASD (Alto/Medio/Bajo)
- [ ] Revisar cobertura: todos los criterios de aceptación tienen test (unitario o integración)
- [ ] Validar edge cases:
  - Timestamps exactos: regla que termina a las 23:59:59 y otra que empieza a las 00:00:00 (no overlap)
  - Descuentos en límites: crear con min%, max%, por debajo/encima
  - Soft delete: regla eliminada no aparece en listados ni en evaluaciones
  - Cambio de límites globales: crear regla → cambiar límites en Admin → verificar que Engine los valida
- [ ] Testing manual: crear/editar/eliminar regla, verificar sincronización en tiempo real (RabbitMQ)
- [ ] Performance: verificar que evaluación de reglas <100ms en `/calculate` con 100+ reglas en caché (TTL 60 min)
- [ ] Verificar que caché se invalida **inmediatamente** en cada evento (no esperar TTL)
- [ ] Actualizar estado spec: `status: IMPLEMENTED` cuando TODO se complete

### Documentación (Opcional)
- [ ] Actualizar `README.md` con instrucción de nueva ruta admin
- [ ] Documentar eventos RabbitMQ en `.github/docs/events-architecture.md`
- [ ] Crear ADR (Architecture Decision Record) si hay cambios en evaluación de reglas en Engine
