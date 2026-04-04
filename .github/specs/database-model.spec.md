---
id: SPEC-DB-001
status: APPROVED
feature: database-model
created: 2026-04-02
updated: 2026-04-02
author: spec-generator
version: "1.0"
related-specs: 
  - ecommerce-onboarding
  - api-keys
  - discount-limit
  - seasonal-rules
  - product-rules
  - customer-classification
  - audit-log
---

# Spec: Modelo de Base de Datos Completo (PostgreSQL)

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de aplicar migrations.
>
> **Descripción:** Define el esquema físico completo de la base de datos PostgreSQL del proyecto LOYALTY. Incluye todas las tablas, campos, constraints, índices y relaciones necesarias para soportar las operaciones de identidad, conectividad S2S, estrategia de descuentos, reglas del motor, y observabilidad.

---

## 1. REQUERIMIENTOS

### Descripción
Establecer un modelo de datos unificado en PostgreSQL que integre cinco áreas de negocio:
1. **Identidad y Tenant** — ecommerce, roles, permisos, usuarios
2. **Conectividad S2S** — claves API
3. **Estrategia Global de Descuentos** — tipos, configuración, prioridades
4. **Reglas del Motor** — tiers, clasificación, promociones estacionales y por producto
5. **Observabilidad** — logs de descuentos y auditoría

### Requerimiento de Negocio
El proyecto requiere un modelo de datos centralizado que:
- Elimine campos redundantes y extraños en las tablas existentes
- Siga exactamente la estructura física definida en el archivo `update-bd.md`
- Soporte multi-tenant (por `ecommerce_id`)
- Mantenga trazabilidad completa mediante auditoría
- Optimice consultas con índices estratégicos
- Asegure integridad referencial con constraints claros

### Historias de Usuario

#### HU-01: Adoptar Modelo de Datos Completo

```
Como:        Arquitecto de Sistemas / DBA
Quiero:      Que la base de datos siga exactamente el esquema definido
Para:        Garantizar consistencia, eliminar datos redundantes y optimizar performance

Prioridad:   Crítica
Estimación:  M (requiere migration y posible rollback)
Dependencias: Ninguna (es la línea base)
Capa:        Backend / Database
```

#### Criterios de Aceptación — HU-01

**Happy Path**
```gherkin
CRITERIO-1.1: Adopción del schema sin errores
  Dado que:  La base de datos existe con estructura anterior
  Cuando:    Se aplican las migrations del nuevo schema
  Entonces:  Todas las tablas, campos, constraints e índices están presentes
             y la aplicación continúa funcionando sin errores
```

**Validaciones de Integridad**
```gherkin
CRITERIO-1.2: Validación de constraints de dominio
  Dado que:  Las tablas están creadas
  Cuando:    Se intenta insertar datos que violan constraints (ej. status inválido)
  Entonces:  La BD rechaza la inserción con error de CHECK constraint
```

```gherkin
CRITERIO-1.3: Validación de integridad referencial
  Dado que:  Las tablas tienen relaciones (foreignkeys)
  Cuando:    Se intenta eliminar una entidad padre sin cascade
  Entonces:  La BD rechaza la operación si hay registros dependientes
```

**Edge Cases**
```gherkin
CRITERIO-1.4: Generación de UUIDs y timestamps
  Dado que:  Se inserta un registro sin especificar id/created_at/updated_at
  Cuando:    El registro se persiste
  Entonces:  Los valores se auto-generan (UUID con pgcrypto, timestamp UTC)
```

---

## 2. DISEÑO

### Modelos de Datos

#### 2.1 IDENTIDAD Y TENANT

##### Tabla: `ecommerce`
**Purpose:** Representa una tienda o tenant en el sistema.

| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `id` | UUID | sí | auto-generado | Identificador único |
| `name` | VARCHAR(255) | sí | no nulo | Nombre legible de la tienda |
| `slug` | VARCHAR(255) | sí | `UNIQUE`, formato kebab-case | Identificador URL-safe |
| `status` | VARCHAR(20) | sí | CHECK: ACTIVE \| INACTIVE | Estado operativo |
| `created_at` | TIMESTAMP TZ | sí | auto-generado, UTC | Fecha de creación |
| `updated_at` | TIMESTAMP TZ | sí | auto-generado, UTC | Última modificación |

**Constraints:**
- PRIMARY KEY: `id`
- UNIQUE: `slug`
- CHECK: `status IN ('ACTIVE', 'INACTIVE')`
- CHECK: `slug ~ '^[a-z0-9]([a-z0-9-]{0,252}[a-z0-9])?$'` (formato kebab-case)

**Índices:**
- `idx_ecommerce_slug` — búsquedas por slug
- `idx_ecommerce_status` — filtrado por estado

---

##### Tabla: `roles`
**Purpose:** Define los roles disponibles en el sistema (SUPER_ADMIN, STORE_ADMIN, STORE_USER).

| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `id` | UUID | sí | auto-generado | Identificador único |
| `name` | VARCHAR(50) | sí | UNIQUE, CHECK: enumeración | Nombre del rol |
| `is_active` | BOOLEAN | sí | DEFAULT: TRUE | Si el rol está activo |
| `created_at` | TIMESTAMP TZ | sí | auto-generado, UTC | Fecha de creación |
| `updated_at` | TIMESTAMP TZ | sí | auto-generado, UTC | Última modificación |

**Constraints:**
- PRIMARY KEY: `id`
- UNIQUE: `name`
- CHECK: `name IN ('SUPER_ADMIN', 'STORE_ADMIN', 'STORE_USER')`

**Índices:**
- `idx_role_name` — búsquedas rápidas de rol por nombre

---

##### Tabla: `permissions`
**Purpose:** Define permisos granulares asignables a roles.

| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `id` | UUID | sí | auto-generado | Identificador único |
| `description` | VARCHAR(255) | no | máx 255 chars | Descripción legible |
| `code` | VARCHAR(50) | sí | UNIQUE | Código técnico del permiso (ej. USERS_READ) |
| `module` | VARCHAR(50) | sí | no nulo | Módulo al que pertenece (ej. USERS, DISCOUNTS) |
| `created_at` | TIMESTAMP TZ | sí | auto-generado, UTC | Fecha de creación |
| `updated_at` | TIMESTAMP TZ | sí | auto-generado, UTC | Última modificación |

**Constraints:**
- PRIMARY KEY: `id`
- UNIQUE: `code`

**Índices:**
- `idx_permission_code` — búsquedas por código
- `idx_permission_module` — filtrado por módulo

---

##### Tabla: `role_permissions`
**Purpose:** Tabla de unión (many-to-many) entre roles y permisos.

| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `id` | UUID | sí | auto-generado | Identificador único |
| `role_id` | UUID | sí | FOREIGN KEY → roles(id) | Rol asociado |
| `permission_id` | UUID | sí | FOREIGN KEY → permissions(id) | Permiso asociado |

**Constraints:**
- PRIMARY KEY: `id`
- FOREIGN KEY: `role_id` → `roles(id)` ON DELETE CASCADE
- FOREIGN KEY: `permission_id` → `permissions(id)` ON DELETE CASCADE

**Índices:**
- `idx_role_permissions_role` — búsquedas por rol
- `idx_role_permissions_permission` — búsquedas por permiso

---

##### Tabla: `app_user`
**Purpose:** Usuarios autenticados en el sistema (admins y operadores).

| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `id` | UUID | sí | auto-generado | Identificador único |
| `ecommerce_id` | UUID | no | FOREIGN KEY → ecommerce(id) | Tenant asociado (NULL = SUPER_ADMIN) |
| `role_id` | UUID | sí | FOREIGN KEY → roles(id) | Rol del usuario |
| `username` | VARCHAR(100) | sí | UNIQUE | Nombre de usuario único |
| `password_hash` | VARCHAR(255) | sí | hasheado (no plain) | Hash seguro de contraseña |
| `email` | VARCHAR(255) | no | válido o NULL | Correo de contacto |
| `is_active` | BOOLEAN | sí | DEFAULT: TRUE | Usuario activo |
| `last_login` | TIMESTAMP TZ | no | NULL inicialmente | Último acceso registrado |
| `created_at` | TIMESTAMP TZ | sí | auto-generado, UTC | Fecha de creación |
| `updated_at` | TIMESTAMP TZ | sí | auto-generado, UTC | Última modificación |

**Constraints:**
- PRIMARY KEY: `id`
- UNIQUE: `username`
- FOREIGN KEY: `role_id` → `roles(id)` ON DELETE RESTRICT
- FOREIGN KEY: `ecommerce_id` → `ecommerce(id)` ON DELETE RESTRICT

**Índices:**
- `idx_user_ecommerce` — búsquedas por tenant
- `idx_user_role` — búsquedas por rol
- `idx_user_active` — filtrado de usuarios activos

---

#### 2.2 CONECTIVIDAD S2S

##### Tabla: `api_key`
**Purpose:** Claves de API para autenticación entre servicios (S2S).

| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `id` | UUID | sí | auto-generado | Identificador único |
| `ecommerce_id` | UUID | sí | FOREIGN KEY → ecommerce(id) | Tenant propietario |
| `hashed_key` | VARCHAR(255) | sí | UNIQUE, hasheado | Hash de la clave (nunca almacenar plain) |
| `expires_at` | TIMESTAMP TZ | sí | CHECK: > CURRENT_TIMESTAMP | Expiración de la clave |
| `is_active` | BOOLEAN | sí | DEFAULT: TRUE | Clave activa |
| `created_at` | TIMESTAMP TZ | sí | auto-generado, UTC | Fecha de creación |
| `updated_at` | TIMESTAMP TZ | sí | auto-generado, UTC | Última modificación |

**Constraints:**
- PRIMARY KEY: `id`
- UNIQUE: `hashed_key`
- FOREIGN KEY: `ecommerce_id` → `ecommerce(id)` ON DELETE CASCADE
- CHECK: `expires_at > CURRENT_TIMESTAMP`

**Índices:**
- `idx_api_key_ecommerce` — búsquedas por tenant

---

#### 2.3 ESTRATEGIA GLOBAL

##### Tabla: `discount_types`
**Purpose:** Catálogo de tipos de descuentos predefinidos.

| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `id` | UUID | sí | auto-generado | Identificador único |
| `code` | VARCHAR(50) | sí | UNIQUE | Código técnico (FIDELITY, SEASONAL, PRODUCT) |
| `display_name` | VARCHAR(150) | no | legible | Nombre para mostrar en UI |

**Constraints:**
- PRIMARY KEY: `id`
- UNIQUE: `code`

**Índices:**
- `idx_discount_type_code` — búsquedas por código

---

##### Tabla: `discount_settings`
**Purpose:** Configuración global de descuentos por tenant.

| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `id` | UUID | sí | auto-generado | Identificador único |
| `ecommerce_id` | UUID | sí | FOREIGN KEY → ecommerce(id), UNIQUE+active | Tenant propietario |
| `max_discount_cap` | DECIMAL(12,4) | sí | CHECK: > 0 | Límite máximo de descuento |
| `currency_code` | VARCHAR(3) | sí | DEFAULT: 'USD' | Moneda (ej. USD, EUR) |
| `allow_stacking` | BOOLEAN | sí | DEFAULT: TRUE | Si se pueden combinar descuentos |
| `rounding_rule` | VARCHAR(20) | sí | DEFAULT: 'ROUND_HALF_UP' | Regla de redondeo (ROUND_HALF_UP, ROUND_DOWN, etc.) |
| `is_active` | BOOLEAN | sí | DEFAULT: TRUE | Configuración activa |
| `created_at` | TIMESTAMP TZ | sí | auto-generado, UTC | Fecha de creación |
| `updated_at` | TIMESTAMP TZ | sí | auto-generado, UTC | Última modificación |

**Constraints:**
- PRIMARY KEY: `id`
- FOREIGN KEY: `ecommerce_id` → `ecommerce(id)` ON DELETE CASCADE
- CHECK: `max_discount_cap > 0`

**Índices:**
- `idx_discount_settings_active` — búsquedas de config activa por tenant (UNIQUE si is_active=TRUE)

---

##### Tabla: `discount_priorities`
**Purpose:** Define el orden de aplicación de tipos de descuentos.

| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `id` | UUID | sí | auto-generado | Identificador único |
| `discount_setting_id` | UUID | sí | FOREIGN KEY → discount_settings(id) | Configuración asociada |
| `discount_type_id` | UUID | sí | FOREIGN KEY → discount_types(id) | Tipo de descuento |
| `priority_level` | INTEGER | sí | CHECK: > 0, UNIQUE per setting | Orden de aplicación (1=primero) |
| `created_at` | TIMESTAMP TZ | sí | auto-generado, UTC | Fecha de creación |
| `updated_at` | TIMESTAMP TZ | sí | auto-generado, UTC | Última modificación |

**Constraints:**
- PRIMARY KEY: `id`
- FOREIGN KEY: `discount_setting_id` → `discount_settings(id)` ON DELETE CASCADE
- FOREIGN KEY: `discount_type_id` → `discount_types(id)`
- UNIQUE: `(discount_setting_id, discount_type_id)`
- UNIQUE: `(discount_setting_id, priority_level)`
- CHECK: `priority_level > 0`

---

#### 2.4 REGLAS (ENGINE)

##### Tabla: `customer_tiers`
**Purpose:** Define niveles de fidelidad y sus descuentos asociados.

| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `id` | UUID | sí | auto-generado | Identificador único |
| `ecommerce_id` | UUID | sí | FOREIGN KEY → ecommerce(id) | Tenant propietario |
| `discount_type_id` | UUID | sí | FOREIGN KEY → discount_types(id) | Tipo de descuento asociado |
| `name` | VARCHAR(100) | sí | UNIQUE per ecommerce | Nombre del tier (ej. Gold, Silver) |
| `discount_percentage` | DECIMAL(5,2) | sí | validar 0-100 | Porcentaje de descuento |
| `hierarchy_level` | INTEGER | sí | único por tier | Nivel jerárquico (1=lowest, 5=highest) |
| `is_active` | BOOLEAN | sí | DEFAULT: TRUE | Tier activo |
| `created_at` | TIMESTAMP TZ | sí | auto-generado, UTC | Fecha de creación |
| `updated_at` | TIMESTAMP TZ | sí | auto-generado, UTC | Última modificación |

**Constraints:**
- PRIMARY KEY: `id`
- FOREIGN KEY: `ecommerce_id` → `ecommerce(id)` ON DELETE CASCADE
- FOREIGN KEY: `discount_type_id` → `discount_types(id)`
- UNIQUE: `(ecommerce_id, name)`

---

##### Tabla: `classification_rule`
**Purpose:** Define las métricas y rangos para clasificar clientes en tiers.

| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `id` | UUID | sí | auto-generado | Identificador único |
| `customer_tier_id` | UUID | sí | FOREIGN KEY → customer_tiers(id) | Tier destino |
| `metric_type` | VARCHAR(50) | sí | CHECK: enumeración | Métrica (total_spent, order_count, loyalty_points, custom) |
| `min_value` | NUMERIC(19,2) | sí | DEFAULT: 0 | Valor mínimo inclusivo |
| `max_value` | NUMERIC(19,2) | no | NULL = sin límite | Valor máximo inclusivo |
| `priority` | INTEGER | sí | DEFAULT: 1 | Prioridad si hay múltiples reglas |
| `is_active` | BOOLEAN | sí | DEFAULT: TRUE | Regla activa |
| `created_at` | TIMESTAMP TZ | sí | auto-generado, UTC | Fecha de creación |
| `updated_at` | TIMESTAMP TZ | sí | auto-generado, UTC | Última modificación |

**Constraints:**
- PRIMARY KEY: `id`
- FOREIGN KEY: `customer_tier_id` → `customer_tiers(id)` ON DELETE CASCADE
- CHECK: `metric_type IN ('total_spent', 'order_count', 'loyalty_points', 'custom')`

**Índices:**
- `idx_classification_rule_tier` — búsquedas por tier
- `idx_classification_rule_active` — búsquedas de reglas activas (UNIQUE por tier+priority si is_active)

---

##### Tabla: `seasonal_rules`
**Purpose:** Define descuentos estacionales con período de validez.

| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `id` | UUID | sí | auto-generado | Identificador único |
| `ecommerce_id` | UUID | sí | FOREIGN KEY → ecommerce(id) | Tenant propietario |
| `discount_type_id` | UUID | sí | FOREIGN KEY → discount_types(id) | Tipo de descuento |
| `name` | VARCHAR(255) | sí | no nulo | Nombre identificable (ej. "Black Friday 2026") |
| `description` | VARCHAR(1000) | no | legible | Descripción detallada |
| `discount_percentage` | NUMERIC(5,2) | sí | CHECK: 0-100 | Porcentaje de descuento |
| `start_date` | TIMESTAMP TZ | sí | CHECK: < end_date | Inicio de vigencia |
| `end_date` | TIMESTAMP TZ | sí | CHECK: > start_date | Fin de vigencia |
| `is_active` | BOOLEAN | sí | DEFAULT: TRUE | Regla activa |
| `created_at` | TIMESTAMP TZ | sí | auto-generado, UTC | Fecha de creación |
| `updated_at` | TIMESTAMP TZ | sí | auto-generado, UTC | Última modificación |

**Constraints:**
- PRIMARY KEY: `id`
- FOREIGN KEY: `ecommerce_id` → `ecommerce(id)` ON DELETE CASCADE
- FOREIGN KEY: `discount_type_id` → `discount_types(id)`
- CHECK: `discount_percentage >= 0 AND discount_percentage <= 100`
- CHECK: `start_date < end_date`

**Índices:**
- `idx_seasonal_rules_ecommerce` — búsquedas por tenant
- `idx_seasonal_rules_date` — búsquedas por rango de fechas
- `idx_seasonal_rules_active` — filtrado de reglas activas

---

##### Tabla: `product_rules`
**Purpose:** Define descuentos específicos por categoría/tipo de producto.

| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `id` | UUID | sí | auto-generado | Identificador único |
| `ecommerce_id` | UUID | sí | FOREIGN KEY → ecommerce(id) | Tenant propietario |
| `discount_type_id` | UUID | sí | FOREIGN KEY → discount_types(id) | Tipo de descuento |
| `name` | VARCHAR(255) | sí | no nulo | Nombre identificable |
| `product_type` | VARCHAR(100) | sí | UNIQUE per ecommerce+is_active | Tipo de producto (ej. "electronics") |
| `discount_percentage` | NUMERIC(5,2) | sí | CHECK: 0-100 | Porcentaje de descuento |
| `is_active` | BOOLEAN | sí | DEFAULT: TRUE | Regla activa |
| `created_at` | TIMESTAMP TZ | sí | auto-generado, UTC | Fecha de creación |
| `updated_at` | TIMESTAMP TZ | sí | auto-generado, UTC | Última modificación |

**Constraints:**
- PRIMARY KEY: `id`
- FOREIGN KEY: `ecommerce_id` → `ecommerce(id)` ON DELETE CASCADE
- FOREIGN KEY: `discount_type_id` → `discount_types(id)`
- CHECK: `discount_percentage >= 0 AND discount_percentage <= 100`
- UNIQUE: `(ecommerce_id, product_type, is_active)` — evita duplicados de tipos activos

**Índices:**
- `idx_product_rules_ecommerce` — búsquedas por tenant
- `idx_product_rules_type` — búsquedas por tipo de producto
- `idx_product_rules_active` — filtrado de reglas activas

---

#### 2.5 OBSERVABILIDAD

##### Tabla: `discount_application_log`
**Purpose:** Registro de cada descuento aplicado en transacciones.

| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `id` | UUID | sí | auto-generado | Identificador único |
| `ecommerce_id` | UUID | sí | FOREIGN KEY → ecommerce(id) | Tenant donde se aplicó |
| `external_order_id` | VARCHAR(255) | no | referencia externa | ID de orden en sistema externo |
| `original_amount` | DECIMAL(12,4) | sí | > 0 | Monto original antes de descuento |
| `discount_applied` | DECIMAL(12,2) | sí | >= 0 | Cantidad de descuento |
| `final_amount` | DECIMAL(12,2) | sí | > 0 | Monto final a pagar |
| `applied_rules_details` | JSONB | no | estructura libre | Detalle JSON de reglas aplicadas |
| `created_at` | TIMESTAMP TZ | sí | auto-generado, UTC | Momento del evento |

**Constraints:**
- PRIMARY KEY: `id`
- FOREIGN KEY: `ecommerce_id` → `ecommerce(id)` ON DELETE CASCADE

**Índices:**
- `idx_discount_log_ecommerce` — búsquedas por tenant para reportes

---

##### Tabla: `audit_log`
**Purpose:** Registro de todas las operaciones de modificación (CRUD) en el sistema.

| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `id` | UUID | sí | auto-generado | Identificador único |
| `user_id` | UUID | no | FOREIGN KEY → app_user(id) | Usuario que ejecutó la acción (NULL = sistema) |
| `ecommerce_id` | UUID | no | FOREIGN KEY → ecommerce(id) | Tenant afectado |
| `action` | VARCHAR(50) | sí | CREATE, READ, UPDATE, DELETE | Tipo de operación |
| `entity_name` | VARCHAR(100) | sí | nombre de tabla | Entidad modificada |
| `entity_id` | UUID | no | ID del registro modificado | Referencia al registro |
| `old_value` | JSONB | no | estado previo | Valores anteriores (para UPDATE/DELETE) |
| `new_value` | JSONB | no | estado nuevo | Valores nuevos (para CREATE/UPDATE) |
| `created_at` | TIMESTAMP TZ | sí | auto-generado, UTC | Momento de la acción |

**Constraints:**
- PRIMARY KEY: `id`
- FOREIGN KEY: `user_id` → `app_user(id)` ON DELETE SET NULL (permite auditar acciones de usuarios eliminados)
- FOREIGN KEY: `ecommerce_id` → `ecommerce(id)` ON DELETE RESTRICT (mantiene trazabilidad tenant)

**Índices:**
- `idx_audit_log_ecommerce` — búsquedas por tenant para compliance

---

### 2.2 Diagrama de Relaciones (ER)

```
┌─────────────────┐
│   ecommerce     │ (tenant raíz)
├─────────────────┤
│ id (PK)         │
│ name            │
│ slug            │
│ status          │
│ created_at      │
│ updated_at      │
└────────┬────────┘
         │ 1
         │
    ┌────┴───────────────────────────────────────┐
    │ N                                           │ N
    ↓                                             ↓
┌─────────────┐                          ┌──────────────────┐
│  app_user   │                          │   api_key        │
├─────────────┤                          ├──────────────────┤
│ id (PK)     │                          │ id (PK)          │
│ ecommerce_id│ (FK)                     │ ecommerce_id (FK)│
│ role_id (FK)├─────────────┐            │ hashed_key       │
│ username    │             │ 1          │ expires_at       │
│ password    │             ↓            │ is_active        │
│ email       │        ┌─────────┐       └──────────────────┘
│ is_active   │        │  roles  │
│ last_login  │        ├─────────┤
│ created_at  │        │ id (PK) │
│ updated_at  │        │ name (U)│
└─────────────┘        │ is_active
                       │ created_at
                       │ updated_at
                       └────┬────────┐
                            │ M      │ M
                            ↓        ↓
                      ┌─────────────────┐
                      │ role_permissions│
                      ├─────────────────┤
                      │ role_id (FK)    │
                      │ permission_id(FK)
                  M   └────────┬────────┘
                      ┌────────┘
                      │
                      ↓
                 ┌──────────────┐
                 │ permissions  │
                 ├──────────────┤
                 │ id (PK)      │
                 │ code (U)     │
                 │ module       │
                 │ description  │
                 │ created_at   │
                 │ updated_at   │
                 └──────────────┘

┌──────────────────────────────────────────────────────────────────┐
│ ESTRATEGIA DE DESCUENTOS (por ecommerce)                         │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────────┐           ┌──────────────────┐            │
│  │ discount_settings│           │ discount_types   │            │
│  ├──────────────────┤    N-1     ├──────────────────┤            │
│  │ id (PK)          ├──────────→ │ id (PK)          │ (catálogo) │
│  │ ecommerce_id (FK)├──┐         │ code (U)         │            │
│  │ max_discount_cap │  │    ┌─→  │ display_name     │            │
│  │ currency         │  │    │    └──────────────────┘            │
│  │ allow_stacking   │  │    │                                    │
│  │ rounding_rule    │  │    │    ┌──────────────────┐            │
│  │ is_active        │  │    └─── │discount_priorities
│  │ created_at       │  │    N-M  ├──────────────────┤            │
│  │ updated_at       │  └────────→│ discount_setting(FK)          │
│  └──────────────────┘            │ discount_type_id (FK)         │
│                                   │ priority_level   │            │
│                                   │ created_at       │            │
│                                   └──────────────────┘            │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│ REGLAS DEL MOTOR (por ecommerce)                                 │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────────┐         ┌──────────────────────────┐      │
│  │ customer_tiers   │ 1-N      │ classification_rule      │      │
│  ├──────────────────┤─────────→├──────────────────────────┤      │
│  │ id (PK)          │          │ id (PK)                  │      │
│  │ ecommerce_id(FK) │          │ customer_tier_id (FK)    │      │
│  │ discount_type_id │          │ metric_type              │      │
│  │ name (U)         │          │ min_value / max_value    │      │
│  │ discount_%       │          │ priority                 │      │
│  │ hierarchy_level  │          │ is_active                │      │
│  │ is_active        │          │ created_at, updated_at   │      │
│  │ created_at       │          └──────────────────────────┘      │
│  │ updated_at       │                                            │
│  └──────────────────┘                                            │
│                                                                  │
│  ┌──────────────────┐         ┌──────────────────────────┐      │
│  │ seasonal_rules   │          │ product_rules            │      │
│  ├──────────────────┤          ├──────────────────────────┤      │
│  │ id (PK)          │          │ id (PK)                  │      │
│  │ ecommerce_id(FK) │          │ ecommerce_id (FK)        │      │
│  │ discount_type_id │          │ discount_type_id (FK)    │      │
│  │ name             │          │ name                     │      │
│  │ description      │          │ product_type (U+active)  │      │
│  │ discount_%       │          │ discount_%               │      │
│  │ start_date       │          │ is_active                │      │
│  │ end_date         │          │ created_at, updated_at   │      │
│  │ is_active        │          └──────────────────────────┘      │
│  │ created_at       │                                            │
│  │ updated_at       │                                            │
│  └──────────────────┘                                            │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│ OBSERVABILIDAD (por ecommerce)           │
├──────────────────────────────────────────┤
│                                          │
│  ┌──────────────────────────┐            │
│  │discount_application_log  │            │
│  ├──────────────────────────┤            │
│  │ id (PK)                  │            │
│  │ ecommerce_id (FK)        │            │
│  │ external_order_id        │            │
│  │ original_amount          │            │
│  │ discount_applied         │            │
│  │ final_amount             │            │
│  │ applied_rules_details(JSON)           │
│  │ created_at               │            │
│  └──────────────────────────┘            │
│                                          │
│  ┌──────────────────────────┐            │
│  │    audit_log             │            │
│  ├──────────────────────────┤            │
│  │ id (PK)                  │            │
│  │ user_id (FK, nullable)   │            │
│  │ ecommerce_id (FK)        │            │
│  │ action                   │            │
│  │ entity_name              │            │
│  │ entity_id                │            │
│  │ old_value (JSONB)        │            │
│  │ new_value (JSONB)        │            │
│  │ created_at               │            │
│  └──────────────────────────┘            │
│                                          │
└──────────────────────────────────────────┘
```

---

## 3. REGLAS DE NEGOCIO

### Identidad y Tenant
1. **RN-1:** Toda tabla debe tener un `ecommerce_id` excepto `ecommerce`, `roles`, `permissions`, `discount_types` (catálogos globales).
2. **RN-2:** El slug de `ecommerce` debe ser único y seguir formato kebab-case.
3. **RN-3:** Solo tres valores permitidos en `roles.name`: SUPER_ADMIN, STORE_ADMIN, STORE_USER.
4. **RN-4:** El campo `password_hash` nunca debe contener contraseña en plain text.
5. **RN-5:** `app_user.ecommerce_id` puede ser NULL solo para SUPER_ADMIN.

### API Keys S2S
6. **RN-6:** `api_key.hashed_key` debe ser hasheado con algoritmo fuerte (bcrypt o similar).
7. **RN-7:** La clave debe tener una fecha de expiración en el futuro (`expires_at > CURRENT_TIMESTAMP`).
8. **RN-8:** Al eliminar un ecommerce, se eliminan todas sus claves API automáticamente (CASCADE).

### Estrategia de Descuentos
9. **RN-9:** `discount_settings.max_discount_cap` debe ser mayor a 0 (validación CHECK).
10. **RN-10:** Existe máximo una configuración activa por ecommerce (`idx_discount_settings_active`).
11. **RN-11:** Las prioridades en `discount_priorities` son únicas por setting y sequenciales.
12. **RN-12:** `allow_stacking` determina si pueden aplicarse múltiples descuentos simultáneamente.

### Reglas de Fidelidad (Tier)
13. **RN-13:** El nombre del tier es único por ecommerce.
14. **RN-14:** Los tiers deben tener un `hierarchy_level` que determine su precedencia.
15. **RN-15:** `discount_percentage` debe estar validado en el rango 0-100.
16. **RN-16:** Una `classification_rule` define qué clientes califican para un tier basado en `metric_type`.

### Reglas Estacionales
17. **RN-17:** La fecha de inicio (`start_date`) debe ser menor que la fecha final (`end_date`).
18. **RN-18:** Una regla estacional es vigente si `is_active=TRUE` Y `NOW() BETWEEN start_date AND end_date`.
19. **RN-19:** El `discount_percentage` debe estar en rango 0-100.

### Reglas de Productos
20. **RN-20:** El `product_type` debe ser único por ecommerce si `is_active=TRUE` (evita conflicto de reglas activas).
21. **RN-21:** El `discount_percentage` debe estar en rango 0-100.
22. **RN-22:** Al crear una nueva regla activa con un `product_type` existente, la anterior debe desactivarse.

### Observabilidad
23. **RN-23:** Cada operación CRUD debe registrarse en `audit_log` con antes/después (old_value, new_value).
24. **RN-24:** `discount_application_log` registra cada descuento aplicado con detalles de las reglas.
25. **RN-25:** Si `user_id` se elimina, el audit_log mantiene el registro con `user_id=NULL` (preserva trazabilidad).

---

## 4. NOTAS DE IMPLEMENTACIÓN

### Migration Strategy
- **Fase 1:** Crear extensión `pgcrypto` si no existe.
- **Fase 2:** Crear todas las tablas en orden de dependencias (sin FK al principio, luego agregar constraints).
- **Fase 3:** Crear índices optimizados.
- **Fase 4:** Seedear datos base: `discount_types`, roles predefinidos.
- **Rollback:** Mantener script de rollback que dropa tablas en orden inverso.

### Consideraciones de Performance
- **Tablas de operación frecuente** (`discount_application_log`, `audit_log`) pueden beneficiarse de particionamiento por fecha (`created_at`).
- **Índices de búsqueda:** prioritarios en `idx_seasonal_rules_date`, `idx_product_rules_active`.
- **JSONB en `applied_rules_details` y `audit_log`:** usar GIN indexes si necesarias búsquedas complejas.

### Integridad Referencial
- **ON DELETE CASCADE** para relaciones 1-N donde eliminar el padre no deja datos huérfanos.
- **ON DELETE RESTRICT** para relaciones críticas (ej. roles, ecommerce) donde la eliminación debe ser explícita.
- **ON DELETE SET NULL** para auditoría (si usuario/ecommerce se elimina, el log se preserva).

### Campos Auditables
Diseñar migraciones para que generadores de logs capturen automáticamente:
- Usuario que ejecutó la acción (`current_user_id` desde JWT)
- Timestamp UTC exacto
- Valores antes/después (snapshots JSONB)

### Nombrado de Convenciones
- Tablas: `snake_case`, singular o plural según estándar (ej. `app_user`, `roles`).
- Columnas: `snake_case` con sufijos claros (`_id` para FKs, `_at` para timestamps, `_percentage` para decimales).
- Índices: `idx_<table>_<columns>` (ej. `idx_seasonal_rules_date`).
- Constraints: usar nombres explícitos en ALTER TABLE si aplica.

---

## 5. STATUS Y APROBACIÓN

Este documento está en estado **DRAFT**. 

**Cambios requeridos antes de APPROVED:**
- [ ] Revisión de arquitecto/DBA confirma alineación con infraestructura
- [ ] Validar que todas las FK referencia tablas existentes
- [ ] Revisar política de CASCADE vs RESTRICT vs SET NULL
- [ ] Confirmar que índices cumplen patrones de búsqueda observados
- [ ] Plan de migración detallado con estrategia de rollback

**Una vez aprobado:**
- Migration se genera automáticamente → Flyway/Liquibase
- Backend developer lee esquema y genera entidades JPA
- Tests de integridad DB se activan

---
