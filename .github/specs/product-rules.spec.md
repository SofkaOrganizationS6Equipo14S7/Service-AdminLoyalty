---
id: SPEC-007
status: DRAFT
feature: product-rules
created: 2026-03-30
updated: 2026-03-30
author: spec-generator
version: "1.0"
related-specs: []
---

# Spec: Gestión de Reglas por Tipo de Producto

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de iniciar implementación.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPRECATED

---

## 1. REQUERIMIENTOS

### Descripción
Capacidad para crear, editar y eliminar reglas de descuento por tipo de producto, asignando un beneficio específico y porcentaje de descuento. Las reglas se almacenan en base de datos PostgreSQL en el Admin Service (fuente de verdad), se validan contra límites globales de descuento, y se sincronizan automáticamente vía RabbitMQ al Engine Service. El Engine mantiene su propia réplica local de las reglas para garantizar autonomía total post-restart (cold start recovery) sin depender de llamadas al Admin Service durante el cálculo de descuentos. La evaluación ocurre en caché Caffeine para mantener latencia baja (<100ms).

### Requerimiento de Negocio
Como usuario de LOYALTY, quiero crear, editar y eliminar reglas por tipo de producto, para automatizar las promociones en base a inventario.

### Historias de Usuario

#### HU-07.1: Crear regla por tipo de producto

```
Como:        Administrador de ecommerce
Quiero:      Crear una nueva regla de descuento asociada a un tipo de producto
Para:        Automatizar promociones basadas en inventario sin duplicidad

Prioridad:   Alta
Estimación:  M
Dependencias: Ninguna
Capa:        Backend + Frontend
```

#### Criterios de Aceptación — HU-07.1

**Happy Path**
```gherkin
CRITERIO-7.1.1: Creación exitosa de regla para un nuevo tipo de producto
  Dado que:  estoy autenticado como Administrador
  Cuando:    creo una productRule con nombre="Premium", productType="ELECTRONICS", 
             discountPercentage=15, benefit="Free Shipping"
  Entonces:  la regla se registra con uid único, status 201 Created, 
             y se publica evento PRODUCT_RULE_CREATED en RabbitMQ
```

**Error Path**
```gherkin
CRITERIO-7.1.2: Rechazo si ya existe regla activa para ese productType
  Dado que:  existe una regla activa con productType="ELECTRONICS"
  Cuando:    intento crear otra regla con productType="ELECTRONICS"
  Entonces:  recibo 409 Conflict con mensaje "Active rule already exists for product_type"

CRITERIO-7.1.3: Rechazo por campos incompletos
  Dado que:  no proporciono el campo "discountPercentage"
  Cuando:    envío POST /api/v1/product-rules con body incompleto
  Entonces:  recibo 400 Bad Request con detalle del campo faltante

CRITERIO-7.1.4: Rechazo por descuento fuera de rango
  Dado que:  existe configuración global con min_discount=0 y max_discount=50
  Cuando:    intento crear regla con discountPercentage=75
  Entonces:  recibo 400 Bad Request con mensaje "Discount exceeds maximum allowed"

CRITERIO-7.1.5: Rechazo por falta de autenticación
  Dado que:  no incluyo token JWT en Authorization header
  Cuando:    POST /api/v1/product-rules
  Entonces:  recibo 401 Unauthorized
```

#### HU-07.2: Obtener y listar reglas por tipo de producto

```
Como:        Administrador de ecommerce
Quiero:      Consultar las reglas activas de mi ecommerce
Para:        Validar las promociones configuradas

Prioridad:   Alta
Estimación:  S
Dependencias: HU-07.1
Capa:        Backend + Frontend
```

#### Criterios de Aceptación — HU-07.2

**Happy Path**
```gherkin
CRITERIO-7.2.1: Listado exitoso de reglas para ecommerce
  Dado que:  soy Administrador autenticado
  Cuando:    GET /api/v1/product-rules?page=0&size=20
  Entonces:  recibo 200 OK con list de reglas activas, paginado

CRITERIO-7.2.2: Obtención exitosa de una regla por uid
  Dado que:  existe regla con uid="550e8400-e29b-41d4-a716-446655440000"
  Cuando:    GET /api/v1/product-rules/550e8400-e29b-41d4-a716-446655440000
  Entonces:  recibo 200 OK con la regla completa (uid, name, productType, discountPercentage, benefit, active, timestamps)
```

**Error Path**
```gherkin
CRITERIO-7.2.3: Rechazo por regla no encontrada
  Dado que:  solicito una regla con uid inexistente
  Cuando:    GET /api/v1/product-rules/00000000-0000-0000-0000-000000000000
  Entonces:  recibo 404 Not Found
```

#### HU-07.3: Editar regla por tipo de producto

```
Como:        Administrador de ecommerce
Quiero:      Modificar los parámetros de una regla existente
Para:        Ajustar promociones según cambios en inventario

Prioridad:   Alta
Estimación:  M
Dependencias: HU-07.1
Capa:        Backend + Frontend
```

#### Criterios de Aceptación — HU-07.3

**Happy Path**
```gherkin
CRITERIO-7.3.1: Edición exitosa de parámetros dentro de límites
  Dado que:  existe regla activa con uid="550e8400-e29b-41d4-a716-446655440000"
  Cuando:    PUT /api/v1/product-rules/{uid} con name="Premium Plus", discountPercentage=20
  Entonces:  la regla se actualiza, updated_at se modifica, 
             recibo 200 OK, y se publica PRODUCT_RULE_UPDATED en RabbitMQ
```

**Error Path**
```gherkin
CRITERIO-7.3.2: Rechazo por datos incompletos en edición
  Dado que:  intento editar una regla
  Cuando:    envío PUT con body vacío {} 
  Entonces:  recibo 400 Bad Request

CRITERIO-7.3.3: Rechazo por descuento fuera de rango
  Dado que:  intento actualizar discountPercentage a valor inválido
  Cuando:    envío PUT con discountPercentage=200
  Entonces:  recibo 400 Bad Request

CRITERIO-7.3.4: Rechazo por regla no encontrada
  Dado que:  solicito editar una regla inexistente
  Cuando:    PUT /api/v1/product-rules/00000000-0000-0000-0000-000000000000
  Entonces:  recibo 404 Not Found
```

#### HU-07.4: Eliminar regla por tipo de producto

```
Como:        Administrador de ecommerce
Quiero:      Eliminar una regla para que no se aplique en futuras transacciones
Para:        Detener promociones que ya no aplican

Prioridad:   Alta
Estimación:  S
Dependencias: HU-07.1
Capa:        Backend + Frontend
```

#### Criterios de Aceptación — HU-07.4

**Happy Path**
```gherkin
CRITERIO-7.4.1: Eliminación exitosa de regla
  Dado que:  existe regla activa con uid="550e8400-e29b-41d4-a716-446655440000"
  Cuando:    DELETE /api/v1/product-rules/{uid}
  Entonces:  la regla se marca como inactiva (no se elimina del DB físicamente),
             recibo 204 No Content, y se publica PRODUCT_RULE_DELETED en RabbitMQ
```

**Error Path**
```gherkin
CRITERIO-7.4.2: Rechazo por regla no encontrada
  Dado que:  solicito eliminar una regla inexistente
  Cuando:    DELETE /api/v1/product-rules/00000000-0000-0000-0000-000000000000
  Entonces:  recibo 404 Not Found

CRITERIO-7.4.3: Rechazo por falta de autenticación
  Dado que:  no incluyo token JWT
  Cuando:    DELETE /api/v1/product-rules/{uid}
  Entonces:  recibo 401 Unauthorized
```

### Reglas de Negocio
1. **Unicidad por tipo de producto:** Solo puede existir UNA regla activa por `product_type`. El código debe validar ANTES de insertar y lanzar `409 Conflict` si la regla ya existe.
2. **No eliminación física:** Las reglas eliminadas se marcan con `active=false`, no se borran de la BD. Esto mantiene auditoría.
3. **Validación de descuento:** Todo `discount_percentage` debe estar entre 0 y 100. Además, si existe `discount_configuration` para el ecommerce, respetar sus límites (min_discount, max_discount).
4. **Sincronización RabbitMQ:** Cada operación (CREATE, UPDATE, DELETE) publica un evento JSON a la cola del Engine Service. El payload incluye: `{ "eventType": "PRODUCT_RULE_CREATED|UPDATED|DELETED", "uid": "...", "productType": "...", "discountPercentage": "...", "benefit": "...", "active": true/false, "timestamp": "iso8601" }`.
5. **Autenticación obligatoria:** Todos los endpoints requieren JWT válido en header `Authorization: Bearer <token>`.
6. **Alcance por ecommerce:** Las reglas están asociadas a un ecommerce (extraído del JWT). Un administrador solo ve/edita reglas de su ecommerce.
7. **Campos requeridos:** name, product_type, discount_percentage, benefit. Todos los demás (description, etc.) son opcionales.

---

## 2. DISEÑO

### Modelos de Datos

#### Entidades afectadas
| Entidad | Almacén | Cambios | Descripción |
|---------|---------|---------|-------------|
| `ProductRuleEntity` | tabla `product_rules` | nueva | Regla de descuento por tipo de producto |

#### Tabla: `product_rules`
```sql
CREATE TABLE product_rules (
    uid UUID PRIMARY KEY,
    ecommerce_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    product_type VARCHAR(100) NOT NULL,
    discount_percentage NUMERIC(5,2) NOT NULL 
        CHECK (discount_percentage >= 0 AND discount_percentage <= 100),
    benefit VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(ecommerce_id) REFERENCES ecommerces(id) ON DELETE CASCADE,
    UNIQUE(ecommerce_id, product_type, is_active)  -- Solo 1 activa por producto tipo
);

CREATE INDEX idx_product_rules_ecommerce_id ON product_rules(ecommerce_id);
CREATE INDEX idx_product_rules_product_type ON product_rules(product_type);
CREATE INDEX idx_product_rules_active ON product_rules(is_active);
CREATE INDEX idx_product_rules_ecommerce_active 
    ON product_rules(ecommerce_id, is_active);
```

#### Campos del modelo
| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `uid` | UUID | sí | auto-generado (UUID.randomUUID()) | Identificador único de la regla |
| `ecommerce_id` | UUID | sí | FK a `ecommerces.id` | Comercio electrónico propietario |
| `name` | VARCHAR(255) | sí | max 255 chars, no vacío | Nombre de la regla (ej. "Premium", "Clearance") |
| `product_type` | VARCHAR(100) | sí | max 100 chars, no vacío | Tipo de producto (ej. "ELECTRONICS", "CLOTHING") |
| `discount_percentage` | NUMERIC(5,2) | sí | 0 ≤ valor ≤ 100 | Porcentaje de descuento (ej. 15.50) |
| `benefit` | VARCHAR(255) | no | max 255 chars | Beneficio asociado (ej. "Free Shipping", "Extended Warranty") |
| `is_active` | BOOLEAN | sí | default=TRUE | Estado de la regla (TRUE=activa, FALSE=eliminada) |
| `created_at` | TIMESTAMP WITH TIME ZONE | sí | auto-generado (CURRENT_TIMESTAMP) | Timestamp de creación (UTC) |
| `updated_at` | TIMESTAMP WITH TIME ZONE | sí | auto-generado, actualizar en UPDATE (CURRENT_TIMESTAMP) | Timestamp última actualización (UTC) |

#### Índices / Constraints
- **UNIQUE(ecommerce_id, product_type, is_active)** — Garantiza solo 1 regla activa por producto_type por ecommerce. Permite múltiples inactivas (historial).
- **FK(ecommerce_id)** — Integridad referencial con ecommerces.
- **idx_product_rules_ecommerce_id** — Búsqueda rápida por ecommerce.
- **idx_product_rules_product_type** — Búsqueda por tipo de producto.
- **idx_product_rules_active** — Filtrado de reglas activas/inactivas.
- **idx_product_rules_ecommerce_active** — Búsqueda combinada (ecommerce + activo).

### API Endpoints

#### POST /api/v1/product-rules
- **Descripción**: Crea una nueva regla de descuento por tipo de producto
- **Auth requerida**: sí (JWT Bearer)
- **Autorización**: ADMIN role
- **Request Body**:
  ```json
  {
    "name": "string (required, max 255)",
    "productType": "string (required, max 100)",
    "discountPercentage": "number (required, 0-100)",
    "benefit": "string (optional, max 255)"
  }
  ```
- **Response 201 Created**:
  ```json
  {
    "uid": "550e8400-e29b-41d4-a716-446655440000",
    "ecommerceId": "550e8400-e29b-41d4-a716-446655440001",
    "name": "Premium",
    "productType": "ELECTRONICS",
    "discountPercentage": 15.50,
    "benefit": "Free Shipping",
    "isActive": true,
    "createdAt": "2026-03-30T10:30:00Z",
    "updatedAt": "2026-03-30T10:30:00Z"
  }
  ```
- **Response 400 Bad Request**: Campo obligatorio faltante, valores inválidos (ej. discountPercentage > 100), descuento fuera de límites globales
  ```json
  {
    "status": 400,
    "message": "Discount exceeds maximum allowed (50%)",
    "timestamp": "2026-03-30T10:30:00Z"
  }
  ```
- **Response 401 Unauthorized**: Token ausente, expirado o inválido
- **Response 409 Conflict**: Ya existe regla activa para ese `product_type`
  ```json
  {
    "status": 409,
    "message": "Active rule already exists for product_type: ELECTRONICS",
    "timestamp": "2026-03-30T10:30:00Z"
  }
  ```

#### GET /api/v1/product-rules
- **Descripción**: Lista todas las reglas (activas e inactivas) del ecommerce autenticado, paginado
- **Auth requerida**: sí (JWT Bearer)
- **Autorización**: ADMIN role
- **Query params**:
  - `page` (default: 0) — Número de página (0-indexed)
  - `size` (default: 20, max: 100) — Cantidad por página
  - `active` (optional, default: true) — Filtrar por estado (true=activas, false=inactivas, omitir=todas)
- **Response 200 OK**:
  ```json
  {
    "content": [
      {
        "uid": "550e8400-e29b-41d4-a716-446655440000",
        "ecommerceId": "550e8400-e29b-41d4-a716-446655440001",
        "name": "Premium",
        "productType": "ELECTRONICS",
        "discountPercentage": 15.50,
        "benefit": "Free Shipping",
        "isActive": true,
        "createdAt": "2026-03-30T10:30:00Z",
        "updatedAt": "2026-03-30T10:30:00Z"
      }
    ],
    "totalPages": 1,
    "totalElements": 1,
    "currentPage": 0,
    "pageSize": 20
  }
  ```
- **Response 401 Unauthorized**: Token ausente o inválido

#### GET /api/v1/product-rules/{uid}
- **Descripción**: Obtiene una regla específica por su UID
- **Auth requerida**: sí (JWT Bearer)
- **Autorización**: ADMIN role
- **Path params**:
  - `uid` — UUID de la regla
- **Response 200 OK**: Objeto completo de la regla (ver POST response)
- **Response 401 Unauthorized**: Token ausente o inválido
- **Response 404 Not Found**: La regla no existe para este ecommerce
  ```json
  {
    "status": 404,
    "message": "Product rule not found with uid: 550e8400-e29b-41d4-a716-446655440000",
    "timestamp": "2026-03-30T10:30:00Z"
  }
  ```

#### PUT /api/v1/product-rules/{uid}
- **Descripción**: Actualiza una regla existente
- **Auth requerida**: sí (JWT Bearer)
- **Autorización**: ADMIN role
- **Path params**:
  - `uid` — UUID de la regla
- **Request Body** (todos los campos opcionales, solo se actualiza los enviados):
  ```json
  {
    "name": "string (optional)",
    "discountPercentage": "number (optional, 0-100)",
    "benefit": "string (optional)"
  }
  ```
  Nota: `product_type` NO se puede editar (se considera identificador lógico junto con ecommerce_id)
- **Response 200 OK**: Objeto actualizado (ver POST response)
- **Response 400 Bad Request**: Valores inválidos, descuento fuera de límites
- **Response 401 Unauthorized**: Token ausente o inválido
- **Response 404 Not Found**: La regla no existe

#### DELETE /api/v1/product-rules/{uid}
- **Descripción**: Elimina (marca como inactiva) una regla
- **Auth requerida**: sí (JWT Bearer)
- **Autorización**: ADMIN role
- **Path params**:
  - `uid` — UUID de la regla
- **Response 204 No Content**: Eliminación exitosa (sin body)
- **Response 401 Unauthorized**: Token ausente o inválido
- **Response 404 Not Found**: La regla no existe

### DTOs y Modelos Internos

#### Domain Layer (Entities)

**ProductRuleEntity.java**
```java
@Entity
@Table(name = "product_rules", indexes = {
    @Index(name = "idx_product_rules_ecommerce_id", columnList = "ecommerce_id"),
    @Index(name = "idx_product_rules_product_type", columnList = "product_type"),
    @Index(name = "idx_product_rules_active", columnList = "is_active"),
    @Index(name = "idx_product_rules_ecommerce_active", columnList = "ecommerce_id, is_active")
})
@UniqueConstraint(name = "uq_product_rules_ecommerce_product_active", 
                 columnNames = {"ecommerce_id", "product_type", "is_active"})
public class ProductRuleEntity {
    @Id private UUID uid;
    @Column(nullable = false) private UUID ecommerceId;
    @Column(nullable = false, length = 255) private String name;
    @Column(nullable = false, length = 100) private String productType;
    @Column(nullable = false) private BigDecimal discountPercentage;
    @Column(length = 255) private String benefit;
    @Column(nullable = false) private Boolean isActive = true;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    // Getters, Setters, Constructors...
}
```

#### Application Layer (DTOs)

**ProductRuleCreateRequest.java (Java Record)**
```java
public record ProductRuleCreateRequest(
    String name,
    String productType,
    BigDecimal discountPercentage,
    String benefit
) {}
```

**ProductRuleUpdateRequest.java (Java Record)**
```java
public record ProductRuleUpdateRequest(
    String name,
    BigDecimal discountPercentage,
    String benefit
) {}
```

**ProductRuleResponse.java (Java Record)**
```java
public record ProductRuleResponse(
    UUID uid,
    UUID ecommerceId,
    String name,
    String productType,
    BigDecimal discountPercentage,
    String benefit,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
```

#### Infrastructure Layer (Events)

**ProductRuleEvent.java (Java Record)**
```java
public record ProductRuleEvent(
    String eventType,  // PRODUCT_RULE_CREATED, PRODUCT_RULE_UPDATED, PRODUCT_RULE_DELETED
    UUID uid,
    UUID ecommerceId,
    String productType,
    BigDecimal discountPercentage,
    String benefit,
    Boolean isActive,
    Instant timestamp
) {}
```

### Diseño Frontend

#### Componentes nuevos
| Componente | Archivo | Props principales | Descripción |
|------------|---------|------------------|-------------|
| `ProductRuleCard` | `components/ProductRuleCard.jsx` | `rule: Object`, `onEdit: Function`, `onDelete: Function` | Tarjeta que muestra una regla con botones de editar/eliminar |
| `ProductRuleFormModal` | `components/ProductRuleFormModal.jsx` | `isOpen: Boolean`, `rule: Object (opcional)`, `onSubmit: Function`, `onClose: Function` | Modal de creación/edición de regla |
| `ProductRulesList` | `components/ProductRulesList.jsx` | `rules: Array`, `loading: Boolean`, `onEdit: Function`, `onDelete: Function` | Lista paginada de reglas |

#### Páginas nuevas
| Página | Archivo | Ruta | Protegida |
|--------|---------|------|-----------|
| `ProductRulesPage` | `pages/ProductRulesPage.jsx` | `/product-rules` | sí |

#### Hooks y State
| Hook | Archivo | Retorna | Descripción |
|------|---------|---------|-------------|
| `useProductRules` | `hooks/useProductRules.js` | `{ rules, loading, error, page, pageSize, createRule, updateRule, deleteRule, fetchRules }` | CRUD + paginación del feature |

#### Services (llamadas API)
| Función | Archivo | Endpoint | Método |
|---------|---------|---------|--------|
| `getProductRules(token, page, size, active)` | `services/productRuleService.js` | `GET /api/v1/product-rules` | GET |
| `getProductRule(uid, token)` | `services/productRuleService.js` | `GET /api/v1/product-rules/{uid}` | GET |
| `createProductRule(data, token)` | `services/productRuleService.js` | `POST /api/v1/product-rules` | POST |
| `updateProductRule(uid, data, token)` | `services/productRuleService.js` | `PUT /api/v1/product-rules/{uid}` | PUT |
| `deleteProductRule(uid, token)` | `services/productRuleService.js` | `DELETE /api/v1/product-rules/{uid}` | DELETE |

### Arquitectura y Dependencias

#### Backend
**Capas involucradas:**
- **Presentation:** `ProductRuleController` — HTTP layer
- **Application:** `ProductRuleService` — Lógica de negocio, validaciones
- **Domain:** `ProductRuleEntity`, `ProductRuleRepository` (interfaz) — Modelos y contratos
- **Infrastructure:** 
  - `ProductRuleRepositoryImpl` — Implementación JPA
  - `ProductRuleEventPublisher` — Publicación a RabbitMQ
  - `ProductRuleMapper` — Conversión Entity ↔ DTO
  - `ConflictException`, `ResourceNotFoundException` — Custom exceptions

**Dependencias externas:**
- **Spring Data JPA** — Persistencia
- **Flyway** — Migraciones BD (crear V13__Create_product_rules_table.sql)
- **RabbitMQ** — Sincronización con Engine Service
- **Jakarta Validation** — Validación de Request Body

**Packages nuevos:**
```
src/main/java/com/loyalty/service_admin/
├── domain/
│   ├── entity/
│   │   └── ProductRuleEntity.java
│   └── repository/
│       └── ProductRuleRepository.java
├── application/
│   ├── service/
│   │   └── ProductRuleService.java
│   ├── dto/
│   │   ├── ProductRuleCreateRequest.java
│   │   ├── ProductRuleUpdateRequest.java
│   │   └── ProductRuleResponse.java
│   └── mapper/
│       └── ProductRuleMapper.java
├── presentation/
│   └── controller/
│       └── ProductRuleController.java
└── infrastructure/
    ├── event/
    │   ├── ProductRuleEvent.java
    │   └── ProductRuleEventPublisher.java
    ├── exception/
    │   ├── ConflictException.java (si no existe)
    │   └── ResourceNotFoundException.java (si no existe)
    └── persistence/
        └── ProductRuleRepositoryImpl.java
```

### Notas de Implementación

> **RabbitMQ Synchronization (Event-Driven Replication):**
> - Crear exchange `product-rules.exchange` (type: topic)
> - Crear queue `engine-service.product-rules` (durable=true)
> - Binding: `engine-service.product-rules` → `product-rules.exchange` con routing key `product.rules.*`
> - Publicar eventos JSON en método `ProductRuleEventPublisher.publish(ProductRuleEvent)` tras cada CREATE/UPDATE/DELETE
> - El payload debe ser: `{ "eventType": "PRODUCT_RULE_CREATED|UPDATED|DELETED", "uid": "...", "ecommerceId": "...", "productType": "...", "discountPercentage": "...", "benefit": "...", "isActive": true/false, "timestamp": "iso8601" }`
> - **Consumo en Engine:** El Engine Service consume estos eventos, actualiza su tabla de réplica local (`product_rules` en `loyalty_engine`) e invalida/actualiza su caché Caffeine
> - **Cold Start Recovery:** Al iniciar, el Engine carga todas las reglas activas de su tabla local en el caché Caffeine (sin esperar a Admin), garantizando autonomía total
>
> **Tabla de Réplica en Engine Service:**
> - El Engine tiene una copia completa de `product_rules` en su BD local (`loyalty_engine`)
> - Se sincroniza automáticamente vía eventos RabbitMQ
> - Permite reconstruir el caché en milisegundos después de un restart (Cyber Monday proof)
> - **Patrón:** Event Sourcing + Local Cache + Eventual Consistency
>
> **Discount Limits Validation:**
> - Antes de crear/actualizar, consultar `DiscountConfigurationRepository.findByEcommerceId(ecommerceId)`
> - Si existe config, validar: `request.discountPercentage >= config.minDiscount && request.discountPercentage <= config.maxDiscount`
> - Si no existe config, permitir cualquier valor entre 0-100
>
> **Uniqueness Constraint:**
> - La BD garantiza via `UNIQUE(ecommerce_id, product_type, is_active)`, pero el código DEBE validar ANTES de insertar
> - En `ProductRuleService.createProductRule()`: consultar `ProductRuleRepository.findByEcommerceIdAndProductTypeAndIsActive(ecommerceId, productType, true)`
> - Si existe → lanzar `ConflictException`
>
> **Soft Delete Pattern:**
> - DELETE endpoint NO borra físicamente; marca `is_active=false` + actualiza `updated_at`
> - GET endpoints filtran por `is_active=true` por defecto (query param `?active=false` para ver inactivas)
> - Mantiene auditoría e historial completo
>
> **Transaction Management:**
> - Métodos de service decorados con `@Transactional` para garantizar ACID
> - Rollback automático si excepción en validación de descuento o persistencia
>
> **Security & Multi-Tenancy:**
> - Extraer `ecommerceId` del JWT (claim o Principal)
> - NUNCA permitir que un admin edite reglas de otro ecommerce
> - Validar en cada endpoint que la regla pertenece al ecommerce del JWT
>
> **Cold Start Recovery (Engine Service - CRÍTICO):**
> - Al iniciar, Engine ejecuta `ProductRuleStartupLoader.loadProductRulesIntoCache()` en `@PostConstruct`
> - Consulta tabla `product_rules` local (réplica sincronizada) y carga todas las reglas activas en Caffeine
> - Garantiza que NO hay downtime ni depende del Admin Service durante startup
> - TTL del caché: 10 minutos (invalidación lazy) + invalidación explícita en eventos RabbitMQ
> - **Escenario:** Cyber Monday, miles de transacciones/seg. Engine se reinicia (deploy o falla). Sin réplica local, tendría que llamar al Admin para 1,000 e-commerces. CON réplica, lee su disco en milisegundos y está al 100%.

---

## 3. LISTA DE TAREAS

> Checklist accionable para todos los agentes. Marcar cada ítem (`[x]`) al completarlo.
> El Orchestrator monitorea este checklist para determinar el progreso.

### Backend

#### Migración y Base de Datos
- [ ] Crear archivo `V13__Create_product_rules_table.sql` en `backend/service-admin/src/main/resources/db/migration/`
- [ ] Definir tabla `product_rules` con todos los campos, constraints e índices según especificación
- [ ] Validar que Flyway ejecute la migración al iniciar la aplicación

#### Implementación Domain Layer
- [ ] Crear `ProductRuleEntity.java` en `domain/entity/` — JPA entity con anotaciones `@Entity`, `@Table`, índices y constraints
- [ ] Crear `ProductRuleRepository.java` interfaz en `domain/repository/` — métodos: `findByEcommerceIdAndProductTypeAndIsActive()`, `findByEcommerceIdAndUid()`, `findByEcommerceId()`

#### Implementación Application Layer
- [ ] Crear `ProductRuleCreateRequest.java` record en `application/dto/`
- [ ] Crear `ProductRuleUpdateRequest.java` record en `application/dto/`
- [ ] Crear `ProductRuleResponse.java` record en `application/dto/`
- [ ] Crear `ProductRuleService.java` en `application/service/`
  - [ ] Método `createProductRule(request, ecommerceId)` — validar duplicidad, descuento, persistir, publicar evento RabbitMQ
  - [ ] Método `getProductRules(ecommerceId, page, size, active)` — listar paginado
  - [ ] Método `getProductRuleByUid(ecommerceId, uid)` — obtener una regla
  - [ ] Método `updateProductRule(ecommerceId, uid, request)` — validar descuento, persistir, publicar evento
  - [ ] Método `deleteProductRule(ecommerceId, uid)` — marcar como inactiva, publicar evento
  - [ ] Validaciones: descuento 0-100, respetar límites globales (consultar DiscountConfigurationRepository), validar campos obligatorios
- [ ] Crear `ProductRuleMapper.java` en `application/mapper/` — conversión Entity ↔ Request/Response

#### Implementación Infrastructure Layer
- [ ] Crear `ProductRuleRepositoryImpl.java` en `infrastructure/persistence/` — implementar métodos de repository usando Spring Data JPA
- [ ] Crear `ProductRuleEvent.java` record en `infrastructure/event/` — estructura del evento RabbitMQ
- [ ] Crear `ProductRuleEventPublisher.java` en `infrastructure/event/` — publicar eventos a RabbitMQ (exchange: `product-rules.exchange`, routing key: `product.rules.{eventType}`)
- [ ] Verificar que `ConflictException.java` y `ResourceNotFoundException.java` existen; si no, crearlas

#### Implementación Presentation Layer
- [ ] Crear `ProductRuleController.java` en `presentation/controller/`
  - [ ] Endpoint `POST /api/v1/product-rules` — crear regla, validar auth, extraer ecommerceId del JWT, retornar 201
  - [ ] Endpoint `GET /api/v1/product-rules` — listar paginado, soportar query param `?page=&size=&active=`
  - [ ] Endpoint `GET /api/v1/product-rules/{uid}` — obtener una regla, validar pertenencia a ecommerce
  - [ ] Endpoint `PUT /api/v1/product-rules/{uid}` — actualizar regla, validar pertenencia y descuento
  - [ ] Endpoint `DELETE /api/v1/product-rules/{uid}` — eliminar (soft delete), validar pertenencia
  - [ ] Todas las rutas protegidas con `@PreAuthorize("hasRole('ADMIN')")`
  - [ ] Manejo de excepciones: `ConflictException` → 409, `ResourceNotFoundException` → 404, `IllegalArgumentException` → 400, `Unauthorized` → 401
- [ ] Registrar controller en punto de entrada de la app (si usa auto-scan de `@RestController`, está automático)

#### Tests Backend
- [ ] `ProductRuleServiceTest.test_createProductRule_success` — happy path
- [ ] `ProductRuleServiceTest.test_createProductRule_duplicateProductType_throwsConflict` — 409
- [ ] `ProductRuleServiceTest.test_createProductRule_discountOutOfRange_throwsValidationError` — 400
- [ ] `ProductRuleServiceTest.test_updateProductRule_success` — actualizar campos válidos
- [ ] `ProductRuleServiceTest.test_updateProductRule_rulNotFound_throwsNotFound` — 404
- [ ] `ProductRuleServiceTest.test_deleteProductRule_success` — soft delete
- [ ] `ProductRuleServiceTest.test_getProductRuleByUid_success` — obtener por uid
- [ ] `ProductRuleRepositoryTest.test_findByEcommerceIdAndProductTypeAndIsActive_returnsRule` — repo layer
- [ ] `ProductRuleControllerTest.test_postCreateProductRule_returns201` — endpoint
- [ ] `ProductRuleControllerTest.test_postCreateProductRule_returns409_onDuplicate` — endpoint conflict
- [ ] `ProductRuleControllerTest.test_getProductRules_returns200_withPagination` — endpoint list
- [ ] `ProductRuleControllerTest.test_getProductRule_returns404_notFound` — endpoint not found
- [ ] `ProductRuleControllerTest.test_deleteProductRule_returns204` — endpoint delete
- [ ] `ProductRuleControllerTest.test_allEndpoints_return401_noToken` — autenticación

#### Backend: Engine Service (Service-Engine)
*La sincronización event-driven requiere que el Engine tenga su propia réplica local para cold start recovery.*

#### Base de Datos (Engine)
- [ ] Crear archivo `V14__Create_product_rules_replica_table.sql` en `backend/service-engine/src/main/resources/db/migration/`
- [ ] Tabla `product_rules` idéntica a la del Admin (mismos campos, índices, constraints)
- [ ] Validar que Flyway ejecute la migración al iniciar el Engine

#### Domain Layer (Engine)
- [ ] Crear `ProductRuleEntity.java` en `domain/entity/` (réplica del Admin, misma estructura)
- [ ] Crear `ProductRuleRepository.java` interfaz en `domain/repository/` — métodos: `findActiveByProductType()`, `findAllActive()`, `save()`, `update()`

#### Application Layer (Engine)
- [ ] Crear `ProductRuleSyncService.java` — servicio que consume eventos RabbitMQ
  - [ ] Método `handleProductRuleCreated(ProductRuleEvent)` — insertar en tabla local
  - [ ] Método `handleProductRuleUpdated(ProductRuleEvent)` — actualizar en tabla local
  - [ ] Método `handleProductRuleDeleted(ProductRuleEvent)` — marcar como inactiva
  - [ ] Método `invalidateProductRuleCache(productType)` — invalidar caché Caffeine

#### Infrastructure Layer (Engine)
- [ ] Crear `ProductRuleEventListener.java` — escucha eventos RabbitMQ vía `@RabbitListener`
  - [ ] Vinculado a queue `engine-service.product-rules`
  - [ ] Llama a `ProductRuleSyncService` para sincronizar BD local
- [ ] Crear `ProductRuleCache.java` config — caché Caffeine para reglas
  - [ ] Keys: `productType` (STRING)
  - [ ] Values: List<ProductRuleEntity>
  - [ ] TTL: 10 minutos
- [ ] Crear `ProductRuleStartupLoader.java` — @Component con @PostConstruct
  - [ ] Lee todas las reglas ACTIVAS de tabla local al iniciar
  - [ ] Carga en caché Caffeine (cold start recovery)
  - [ ] Log: "Loaded N product rules into cache at startup"
- [ ] Crear `ProductRuleRepositoryImpl.java` — implementación JPA

#### Tests (Engine)
- [ ] `ProductRuleSyncServiceTest.test_handleProductRuleCreated_insertsInLocalTable`
- [ ] `ProductRuleSyncServiceTest.test_handleProductRuleUpdated_updatesLocalTable`
- [ ] `ProductRuleSyncServiceTest.test_handleProductRuleDeleted_marksAsInactive`
- [ ] `ProductRuleStartupLoaderTest.test_loadRulesOnStartup_populatesCacheFromLocalDB`
- [ ] `ProductRuleEventListenerTest.test_listenProductRuleEvents_callsSyncService`
- [ ] `ProductRuleCacheTest.test_cache_expiriesAfter10Minutes`

### Frontend

#### Implementación Services
- [ ] Crear `services/productRuleService.js`
  - [ ] Función `getProductRules(token, page=0, size=20, active=true)` — GET /api/v1/product-rules
  - [ ] Función `getProductRule(uid, token)` — GET /api/v1/product-rules/{uid}
  - [ ] Función `createProductRule(data, token)` — POST /api/v1/product-rules
  - [ ] Función `updateProductRule(uid, data, token)` — PUT /api/v1/product-rules/{uid}
  - [ ] Función `deleteProductRule(uid, token)` — DELETE /api/v1/product-rules/{uid}
  - [ ] Manejo de errores: 400, 401, 404, 409 → retornar mensaje descriptivo

#### Implementación Hooks
- [ ] Crear `hooks/useProductRules.js`
  - [ ] Estado: `rules` (array), `loading` (bool), `error` (string), `page` (number), `pageSize` (number)
  - [ ] Acción `fetchRules(page, size, active)` — llamar service y actualizar estado
  - [ ] Acción `createRule(data)` — validación local, llamada service
  - [ ] Acción `updateRule(uid, data)` — idem
  - [ ] Acción `deleteRule(uid)` — idem
  - [ ] useEffect en montaje: cargar reglas iniciales
  - [ ] Manejo de errores: derivar `error.message` al estado

#### Implementación Componentes
- [ ] Crear `components/ProductRuleCard.jsx`
  - [ ] Props: `rule` (object), `onEdit` (function), `onDelete` (function)
  - [ ] Mostrar: name, productType, discountPercentage, benefit, isActive
  - [ ] Botones: "Editar" (onClick onEdit) y "Eliminar" (onClick onDelete con confirmación)
  - [ ] Estilos: card/box, badges por estado
- [ ] Crear `components/ProductRuleFormModal.jsx`
  - [ ] Props: `isOpen` (bool), `rule` (object, opcional para edición), `onSubmit` (function), `onClose` (function)
  - [ ] Formulario: name, productType (select con opciones), discountPercentage (number 0-100), benefit (textarea)
  - [ ] Validación local: campos obligatorios, descuento 0-100
  - [ ] Botones: "Guardar" (submit), "Cancelar" (close)
  - [ ] Modo creación: vacío, disabled productType. Modo edición: pre-llenar, disabled productType
  - [ ] Mostrar loading mientras se procesa request
  - [ ] Mostrar error si falla request (409, 400, etc.)
- [ ] Crear `components/ProductRulesList.jsx`
  - [ ] Props: `rules` (array), `loading` (bool), `onEdit` (function), `onDelete` (function)
  - [ ] Renderizar lista de ProductRuleCard
  - [ ] Mostrar spinner si `loading=true`
  - [ ] Mostrar "No hay reglas" si array vacío

#### Implementación Páginas
- [ ] Crear `pages/ProductRulesPage.jsx`
  - [ ] Usar hook `useProductRules`
  - [ ] Estructura: header con título, botón "Nueva Regla", búsqueda/filtro opcional, ProductRulesList
  - [ ] Paginación: botones Prev/Next o componente Pagination
  - [ ] ProductRuleFormModal: isOpen si `isCreating || isEditing`, regla pre-cargada si edición
  - [ ] Modal confirmación: antes de eliminar con `onConfirm=deleteRule`
  - [ ] Mostrar toast/snackbar si operación exitosa o error

#### Registro de Rutas
- [ ] Registrar ruta `/product-rules` → `ProductRulesPage` en el sistema de rutas de la aplicación (React Router)
- [ ] Proteger ruta con PrivateRoute si existe (redirigir a login si no autenticado)

#### Tests Frontend
- [ ] `ProductRuleCard.test.jsx` — renderiza nombre y botones correctamente
- [ ] `ProductRuleCard.test.jsx` — onClick onEdit y onDelete
- [ ] `ProductRuleFormModal.test.jsx` — renderiza formulario vacío en creación
- [ ] `ProductRuleFormModal.test.jsx` — renderiza formulario pre-llenado en edición
- [ ] `ProductRuleFormModal.test.jsx` — desactiva productType en edición
- [ ] `ProductRuleFormModal.test.jsx` — onSubmit con datos válidos
- [ ] `ProductRuleFormModal.test.jsx` — validación local de descuento (0-100)
- [ ] `useProductRules.test.js` — hook carga reglas en montaje
- [ ] `useProductRules.test.js` — createRule() publica evento correcto
- [ ] `useProductRules.test.js` — deleteRule() publica evento correcto
- [ ] `useProductRules.test.js` — error handling (409, 404, 400)
- [ ] `ProductRulesPage.test.jsx` — renderiza lista de reglas
- [ ] `ProductRulesPage.test.jsx` — botón "Nueva Regla" abre modal
- [ ] `ProductRulesPage.test.jsx` — paginación funciona

### QA

#### Análisis De Riesgos
- [ ] Ejecutar skill `/risk-identifier` sobre esta spec
- [ ] Clasificar riesgos en Alto/Medio/Bajo según regla ASD
- [ ] Documentar en `docs/output/qa/product-rules-risks.md`

#### Casos Gherkin
- [ ] Ejecutar skill `/gherkin-case-generator` sobre esta spec
- [ ] Generar escenarios Given-When-Then para CRITERIO-7.1.1 a 7.4.3
- [ ] Incluir datos de prueba (fixtures de productType, descuento, ecommerce)
- [ ] Documentar en `docs/output/qa/product-rules-gherkin.feature`

#### Validación De Cobertura
- [ ] Verificar que test backend cubre todos los criterios de aceptación
- [ ] Verificar que test frontend cubre UI y flujos de usuario
- [ ] Revisar que tests validadores contrastan con reglas de negocio

#### Actualización De Spec
- [ ] Una vez implementados backend + frontend + tests, cambiar `status: DRAFT` → `status: APPROVED`
- [ ] Agregar `updated: YYYY-MM-DD` con fecha actual
- [ ] Confirmar que checklist está 100% completo antes de APPROVED

