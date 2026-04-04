---
id: SPEC-002
status: APPROVED
feature: controllers-alignment-new-database
created: 2026-04-02
updated: 2026-04-02
author: spec-generator
version: "1.0"
related-specs: []
---

# Spec: Alineación de Controllers con Nueva Estructura de Base de Datos

> **Estado:** `DRAFT` — requiere aprobación antes de implementación.
> **Prioridad:** Alta
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED

---

## 1. REQUERIMIENTOS

### Descripción
Revisar, actualizar y alinear todos los controllers REST del microservicio service-admin con la nueva estructura de base de datos rediseñada para soportar múltiples tenants (ecommerce), gestión modular de usuarios, roles, permisos, descuentos, reglas estacionales, reglas de producto y observabilidad.

**Problema:** La base de datos fue rediseñada para ser más robusta y modular, pero los controllers existentes aún mantienen patrones de la estructura anterior, causando inconsistencias entre la capa de datos y la lógica de negocio.

**Solución:** Revisar cada controller, validar alineación con el nuevo esquema, y actualizar endpoints, DTOs, y lógica de negocio según sea necesario.

### Requerimiento de Negocio (original)

La nueva base de datos incluye:

- **Tabla ecommerce**: tenant raíz con id, name, slug, status, timestamps
- **Tabla app_user**: usuarios con ecommerce_id, role_id, fields, is_active
- **Tabla roles**: SUPER_ADMIN, STORE_ADMIN, STORE_USER
- **Tabla permissions**: códigos de permisos por módulo
- **Tabla api_key**: conectividad S2S con hashed_key, expires_at, is_active
- **Tabla discount_settings**: configuración global de descuentos por ecommerce
- **Tabla discount_priorities**: prioridad de tipos de descuento
- **Tabla customer_tiers**: niveles de clasificación de clientes
- **Tabla classification_rule**: reglas de clasificación por métrica (total_spent, order_count, loyalty_points, custom)
- **Tabla seasonal_rules**: descuentos estacionales con rango de fechas
- **Tabla product_rules**: descuentos por tipo de producto
- **Tabla discount_application_log**: auditoría de descuentos aplicados
- **Tabla audit_log**: auditoría de cambios de datos

### Historias de Usuario

#### HU-01: Validar y Alinear Controller de Usuarios

```
Como:        Administrador de Sistema
Quiero:      Asegurar que UserController gestione correctamente la tabla app_user
Para:        Garantizar que todos los endpoints CRUD reflejen la nueva estructura (ecommerce_id, role_id, is_active)

Prioridad:   Alta
Estimación:  M
Dependencias: Ninguna
Capa:        Backend
```

**Criterios de Aceptación — HU-01**

```gherkin
CRITERIO-1.1: Crear usuario con todos los campos requeridos
  Dado que:   el usuario autenticado es SUPER_ADMIN
  Cuando:     ejecuta POST /api/v1/users con { username, email, password, roleId, ecommerceId }
  Entonces:   se crea el usuario con isActive=true y se retorna HTTP 201 con UserResponse (uid, username, email, roleId, ecommerceId, isActive, createdAt, updatedAt)

CRITERIO-1.2: Listar usuarios filtrados por ecommerce_id
  Dado que:   existen usuarios de múltiples ecommerce
  Cuando:     ejecuta GET /api/v1/users?ecommerceId={uuid}
  Entonces:   retorna HTTP 200 con lista de usuarios de ese ecommerce solamente

CRITERIO-1.3: Actualizar usuario (username, email, password, ecommerce_id, is_active)
  Dado que:   existe un usuario con uid
  Cuando:     ejecuta PUT /api/v1/users/{uid} con campos a actualizar
  Entonces:   actualiza solo los campos enviados y retorna HTTP 200 con UserResponse actualizado

CRITERIO-1.4: Validar que no se pueda actualizar role_id por este endpoint
  Dado que:   existe un usuario
  Cuando:     intenta PUT /api/v1/users/{uid} con { roleId: new_role }
  Entonces:   retorna HTTP 400 Bad Request (roleId es inmutable)

CRITERIO-1.5: Eliminar usuario (soft delete con isActive=false)
  Dado que:   existe un usuario activo
  Cuando:     ejecuta DELETE /api/v1/users/{uid}
  Entonces:   marca isActive=false y retorna HTTP 204 No Content
```

---

#### HU-02: Validar y Alinear Controller de Ecommerce

```
Como:        SUPER_ADMIN
Quiero:      Gestionar ecommerces (Create, Read, Update status)
Para:        Soportar multi-tenancia y control de acceso por ecommerce

Prioridad:   Alta
Estimación:  M
Dependencias: Ninguna
Capa:        Backend
```

**Criterios de Aceptación — HU-02**

```gherkin
CRITERIO-2.1: Crear ecommerce con name y slug
  Dado que:   usuario autenticado es SUPER_ADMIN
  Cuando:     ejecuta POST /api/v1/ecommerces con { name: "Store X", slug: "store-x" }
  Entonces:   crea ecommerce con status=ACTIVE y retorna HTTP 201 con EcommerceResponse

CRITERIO-2.2: Validar slug format con regex ^[a-z0-9]([a-z0-9-]{0,252}[a-z0-9])?$
  Dado que:   usuario intenta crear ecommerce
  Cuando:     envía slug inválido (ej. "Store X", "UPPERCASE", "-invalid")
  Entonces:   retorna HTTP 400 Bad Request con mensaje de validación

CRITERIO-2.3: Listar ecommerces con paginación y filtro por status
  Dado que:   existen múltiples ecommerces
  Cuando:     ejecuta GET /api/v1/ecommerces?status=ACTIVE&page=0&size=50
  Entonces:   retorna HTTP 200 con Page<EcommerceResponse> paginada

CRITERIO-2.4: Obtener detalle de ecommerce
  Dado que:   STORE_ADMIN intenta GET /api/v1/ecommerces/{uid} de su propio ecommerce
  Cuando:     uid es su ecommerceId
  Entonces:   retorna HTTP 200 con EcommerceResponse

CRITERIO-2.5: Rechazar acceso a ecommerce ajeno
  Dado que:   STORE_ADMIN intenta GET /api/v1/ecommerces/{another_uid}
  Cuando:     another_uid pertenece a otro ecommerce
  Entonces:   retorna HTTP 403 Forbidden (AuthorizationException)

CRITERIO-2.6: Actualizar status de ecommerce (ACTIVE ↔ INACTIVE)
  Dado que:   SUPER_ADMIN intenta PUT /api/v1/ecommerces/{uid}/status
  Cuando:     envía { status: "INACTIVE" }
  Entonces:   actualiza y retorna HTTP 200 con EcommerceResponse actualizado
```

---

#### HU-03: Validar y Alinear Controller de API Keys

```
Como:        Administrador de Ecommerce
Quiero:      Crear y gestionar API keys para conectividad S2S
Para:        Permitir que sistemas externos se autentiquen contra el discount engine

Prioridad:   Alta
Estimación:  M
Dependencias: HU-02 (Ecommerce debe existir)
Capa:        Backend
```

**Criterios de Aceptación — HU-03**

```gherkin
CRITERIO-3.1: Crear API key para un ecommerce
  Dado que:   usuario autenticado es STORE_ADMIN de cierto ecommerce
  Cuando:     ejecuta POST /api/v1/ecommerces/{ecommerceId}/api-keys
  Entonces:   genera API key con hashedKey, expiresAt, isActive=true y retorna HTTP 201 con ApiKeyResponse (uid, maskedKey, expiresAt)

CRITERIO-3.2: Listar API keys de un ecommerce
  Dado que:   usuario intenta GET /api/v1/ecommerces/{ecommerceId}/api-keys
  Cuando:     ecommerceId es su ecommerce
  Entonces:   retorna HTTP 200 con List<ApiKeyListResponse> (uid, maskedKey, expiresAt, isActive, createdAt)

CRITERIO-3.3: Eliminar API key
  Dado que:   usuario intenta DELETE /api/v1/ecommerces/{ecommerceId}/api-keys/{keyId}
  Cuando:     keyId pertenece a su ecommerce
  Entonces:   elimina la API key (delete físico) y retorna HTTP 204 No Content

CRITERIO-3.4: Validar que API key expirada no se permita usar
  Dado que:   API key tiene expiresAt < NOW()
  Cuando:     sistema intenta validarla en un request
  Entonces:   rechaza con HTTP 401 Unauthorized (key expired)

CRITERIO-3.5: Rechazar acceso a API keys de otro ecommerce
  Dado que:   usuario intenta acceder a API keys de ecommerce ajeno
  Cuando:     GET /api/v1/ecommerces/{otherEcommerceId}/api-keys
  Entonces:   retorna HTTP 403 Forbidden
```

---

#### HU-04: Validar y Alinear Controller de Configuración de Descuentos

```
Como:        STORE_ADMIN
Quiero:      Configurar límites, rueda de prioridades y reglas globales de descuentos
Para:        Controlar la estrategia de descuentos del ecommerce

Prioridad:   Alta
Estimación:  M
Dependencias: HU-02 (Ecommerce debe existir)
Capa:        Backend
```

**Criterios de Aceptación — HU-04**

```gherkin
CRITERIO-4.1: Crear o actualizar discount_settings por ecommerce
  Dado que:   STORE_ADMIN intenta POST /api/v1/discount-config
  Cuando:     envía { ecommerceId, maxDiscountCap, currencyCode, allowStacking, roundingRule }
  Entonces:   crea o actualiza discount_settings y retorna HTTP 201/200 con DiscountConfigResponse

CRITERIO-4.2: Obtener configuración activa de descuentos
  Dado que:   usuario intenta GET /api/v1/discount-config?ecommerceId={uuid}
  Cuando:     ecommerceId existe
  Entonces:   retorna HTTP 200 con DiscountConfigResponse (id, maxDiscountCap, currencyCode, allowStacking, roundingRule, isActive, version)

CRITERIO-4.3: Crear prioridades de tipos de descuento
  Dado que:   STORE_ADMIN intenta POST /api/v1/discount-priority
  Cuando:     envía { discountSettingId, priorities: [{ discountTypeId, priorityLevel }] }
  Entonces:   crea discount_priorities y retorna HTTP 201 con DiscountLimitPriorityResponse

CRITERIO-4.4: Obtener prioridades configuradas
  Dado que:   usuario intenta GET /api/v1/discount-priority?discountSettingId={uuid}
  Cuando:     discountSettingId existe
  Entonces:   retorna HTTP 200 con DiscountLimitPriorityResponse (priorities: [{ discountTypeId, priorityLevel }])

CRITERIO-4.5: Validar maxDiscountCap > 0
  Dado que:   usuario intenta crear config
  Cuando:     envía maxDiscountCap <= 0
  Entonces:   retorna HTTP 400 Bad Request
```

---

#### HU-05: Validar y Alinear Controller de Reglas Estacionales

```
Como:        STORE_ADMIN
Quiero:      Crear, listar, actualizar y eliminar reglas estacionales de descuento
Para:        Aplicar descuentos en períodos específicos (Black Friday, Navidad, etc.)

Prioridad:   Alta
Estimación:  M
Dependencias: HU-02, HU-04
Capa:        Backend
```

**Criterios de Aceptación — HU-05**

```gherkin
CRITERIO-5.1: Crear regla estacional (endpoint unificado con type=SEASONAL)
  Dado que:   STORE_ADMIN de ecommerce X intenta POST /api/v1/rules
  Cuando:     envía { type: "SEASONAL", name, description, discountPercentage (0-100), startDate, endDate, discountTypeId }
  Entonces:   crea rule con type=SEASONAL, isActive=true, ecommerceId inferido del token, y retorna HTTP 201

CRITERIO-5.2: Validar rango de fechas (startDate < endDate) para SEASONAL
  Dado que:   usuario intenta crear regla tipo SEASONAL
  Cuando:     startDate >= endDate
  Entonces:   retorna HTTP 400 Bad Request

CRITERIO-5.3: Listar reglas estacionales de mi ecommerce con paginación
  Dado que:   STORE_ADMIN intenta GET /api/v1/rules?type=SEASONAL&page=0&size=20
  Cuando:     ecommerceId está en el token
  Entonces:   retorna HTTP 200 con Page<RuleResponse> filtrado por type=SEASONAL de su ecommerce

CRITERIO-5.4: Obtener detalles de una regla específica
  Dado que:   usuario intenta GET /api/v1/rules/{uid}
  Cuando:     regla (type=SEASONAL) existe en su ecommerce
  Entonces:   retorna HTTP 200 con RuleResponse (id, type, name, description, discountPercentage, startDate, endDate, discountTypeId, isActive, createdAt, updatedAt)

CRITERIO-5.5: Actualizar regla estacional
  Dado que:   usuario intenta PUT /api/v1/rules/{uid}
  Cuando:     envía campos a actualizar para rule type=SEASONAL (name, description, discountPercentage, startDate, endDate)
  Entonces:   actualiza solo los campos enviados, valida fechas y retorna HTTP 200

CRITERIO-5.6: Eliminar regla estacional (soft delete con isActive=false)
  Dado que:   usuario intenta DELETE /api/v1/rules/{uid}
  Cuando:     regla (type=SEASONAL) existe en su ecommerce
  Entonces:   marca isActive=false y retorna HTTP 204 No Content
```

---

#### HU-06: Validar y Alinear Controller de Reglas de Producto

```
Como:        STORE_ADMIN
Quiero:      Crear, listar, actualizar y eliminar reglas de descuento por tipo de producto
Para:        Aplicar descuentos específicos a categorías de productos

Prioridad:   Alta
Estimación:  M
Dependencias: HU-02, HU-04
Capa:        Backend
```

**Criterios de Aceptación — HU-06**

```gherkin
CRITERIO-6.1: Crear regla de producto (endpoint unificado con type=PRODUCT)
  Dado que:   STORE_ADMIN intenta POST /api/v1/rules
  Cuando:     envía { type: "PRODUCT", name, productType, discountPercentage (0-100), discountTypeId }
  Entonces:   crea rule con type=PRODUCT, isActive=true y retorna HTTP 201

CRITERIO-6.2: Validar unicidad (ecommerceId, productType, isActive) para PRODUCT
  Dado que:   usuario intenta crear dos reglas con mismo productType (activas, type=PRODUCT)
  Cuando:     envía el segundo POST /api/v1/rules con type=PRODUCT
  Entonces:   retorna HTTP 409 Conflict (UNIQUE constraint)

CRITERIO-6.3: Listar reglas de producto con filtros
  Dado que:   usuario intenta GET /api/v1/rules?type=PRODUCT&isActive=true&page=0&size=20
  Cuando:     ejecuta la búsqueda
  Entonces:   retorna HTTP 200 con Page<RuleResponse> filtrado por type=PRODUCT (solo activas si isActive=true)

CRITERIO-6.4: Obtener detalles de una regla
  Dado que:   usuario intenta GET /api/v1/rules/{uid}
  Cuando:     regla (type=PRODUCT) existe
  Entonces:   retorna HTTP 200 con RuleResponse (incluye productType)

CRITERIO-6.5: Actualizar regla de producto
  Dado que:   usuario intenta PUT /api/v1/rules/{uid}
  Cuando:     envía actualizaciones para rule type=PRODUCT { name, productType, discountPercentage }
  Entonces:   actualiza y valida, retorna HTTP 200

CRITERIO-6.6: Eliminar regla (soft delete con isActive=false)
  Dado que:   usuario intenta DELETE /api/v1/rules/{uid}
  Cuando:     regla (type=PRODUCT) existe
  Entonces:   marca isActive=false y retorna HTTP 204 No Content
```

---

#### HU-07: Validar y Alinear Controller de Reglas de Clasificación de Clientes

```
Como:        STORE_ADMIN
Quiero:      Crear tiers de clasificación de clientes y reglas de métrica asociadas
Para:        Aplicar descuentos basados en customer_tiers (gastos, órdenes, puntos, custom)

Prioridad:   Alta
Estimación:  L
Dependencias: HU-02, HU-04
Capa:        Backend
```

**Criterios de Aceptación — HU-07**

```gherkin
CRITERIO-7.1: Crear customer_tier
  Dado que:   STORE_ADMIN intenta POST /api/v1/customer-tiers
  Cuando:     envía { name, discountPercentage, hierarchyLevel, discountTypeId }
  Entonces:   crea customer_tier con isActive=true y retorna HTTP 201

CRITERIO-7.2: Validar unicidad (ecommerceId, name)
  Dado que:   usuario intenta crear tier con nombre duplicado
  Cuando:     POST /api/v1/customer-tiers con name existente
  Entonces:   retorna HTTP 409 Conflict

CRITERIO-7.3: Crear classification_rule para un tier (usando endpoint unificado type=CLASSIFICATION)
  Dado que:   STORE_ADMIN intenta POST /api/v1/customer-tiers/{tierId}/classification-rules
  Cuando:     envía { metricType (total_spent|order_count|loyalty_points|custom), minValue, maxValue, priority }
  Entonces:   crea classification_rule (que internamente es type=CLASSIFICATION) y retorna HTTP 201

CRITERIO-7.4: Validar metricType está en enum
  Dado que:   usuario intenta crear rule de clasificación
  Cuando:     envía metricType=INVALID
  Entonces:   retorna HTTP 400 Bad Request

CRITERIO-7.5: Listar customer_tiers con paginación
  Dado que:   usuario intenta GET /api/v1/customer-tiers?page=0&size=20
  Cuando:     ejecuta
  Entonces:   retorna HTTP 200 con Page<CustomerTierResponse>

CRITERIO-7.6: Obtener detalles de tier con sus rules
  Dado que:   usuario intenta GET /api/v1/customer-tiers/{tierId}
  Cuando:     tier existe
  Entonces:   retorna HTTP 200 con CustomerTierResponse + classificationRules: []

CRITERIO-7.7: Actualizar classification_rule
  Dado que:   usuario intenta PUT /api/v1/customer-tiers/{tierId}/classification-rules/{ruleId}
  Cuando:     envía actualizaciones
  Entonces:   actualiza y retorna HTTP 200

CRITERIO-7.8: Eliminar tier (soft delete)
  Dado que:   usuario intenta DELETE /api/v1/customer-tiers/{tierId}
  Cuando:     tier existe
  Entonces:   marca is_active=false y retorna HTTP 204
```

---

#### HU-08: Crear Controller de Roles y Permisos (NUEVO)

```
Como:        SUPER_ADMIN
Quiero:      Gestionar roles y permisos del sistema
Para:        Definir políticas de acceso y delegación de responsabilidades

Prioridad:   Media
Estimación:  M
Dependencias: Ninguna (tablas existentes: roles, permissions)
Capa:        Backend
```

**Criterios de Aceptación — HU-08**

```gherkin
CRITERIO-8.1: Listar roles existentes
  Dado que:   SUPER_ADMIN intenta GET /api/v1/roles
  Cuando:     ejecuta
  Entonces:   retorna HTTP 200 con List<RoleResponse> (id, name, isActive, createdAt, updatedAt)

CRITERIO-8.2: Obtener detalles de un rol
  Dado que:   usuario intenta GET /api/v1/roles/{roleId}
  Cuando:     roleId existe
  Entonces:   retorna HTTP 200 con RoleResponse + permissions: []

CRITERIO-8.3: Listar permisos disponibles
  Dado que:   usuario intenta GET /api/v1/permissions
  Cuando:     ejecuta
  Entonces:   retorna HTTP 200 con List<PermissionResponse> (id, code, description, module, createdAt)

CRITERIO-8.4: Filtrar permisos por módulo
  Dado que:   usuario intenta GET /api/v1/permissions?module=DISCOUNT_CONFIG
  Cuando:     ejecuta
  Entonces:   retorna HTTP 200 con List<PermissionResponse> solo de ese módulo

CRITERIO-8.5: Asignar permisos a rol
  Dado que:   SUPER_ADMIN intenta POST /api/v1/roles/{roleId}/permissions
  Cuando:     envía { permissionIds: [...] }
  Entonces:   asigna permisos y retorna HTTP 201 con RoleResponse actualizado
```

---

#### HU-09: Crear Endpoints de Auditoría (NUEVO)

```
Como:        Auditor / SUPER_ADMIN
Quiero:      Consultar logs de auditoría (audit_log y discount_application_log)
Para:        Rastrear cambios de datos y aplicaciones de descuentos

Prioridad:   Media
Estimación:  M
Dependencias: Ninguna (tablas existentes: audit_log, discount_application_log)
Capa:        Backend
```

**Criterios de Aceptación — HU-09**

```gherkin
CRITERIO-9.1: Listar audit logs con filtros
  Dado que:   SUPER_ADMIN intenta GET /api/v1/audit-logs?entityName=app_user&ecommerceId={uuid}&page=0&size=50
  Cuando:     ejecuta
  Entonces:   retorna HTTP 200 con Page<AuditLogResponse> (id, userId, action, entityName, entityId, oldValue, newValue, createdAt)

CRITERIO-9.2: Obtener detalles de un cambio de auditoría
  Dado que:   usuario intenta GET /api/v1/audit-logs/{logId}
  Cuando:     logId existe
  Entonces:   retorna HTTP 200 con AuditLogResponse completa (incluye oldValue y newValue como JSON)

CRITERIO-9.3: Listar descuentos aplicados con filtros
  Dado que:   usuario intenta GET /api/v1/discount-logs?ecommerceId={uuid}&externalOrderId={externalOrderId}
  Cuando:     ejecuta
  Entonces:   retorna HTTP 200 con Page<DiscountApplicationLogResponse> (id, externalOrderId, originalAmount, discountApplied, finalAmount, appliedRulesDetails, createdAt)

CRITERIO-9.4: No permitir modificación de logs
  Dado que:   usuario intenta PUT/DELETE /api/v1/audit-logs/{logId}
  Cuando:     intenta hacerlo
  Entonces:   retorna HTTP 405 Method Not Allowed
```

---

## 2. DISEÑO

### 2.1 Análisis de Controllers Actuales vs. Requerimientos

| Controller | Ubicación | Tablas Mapeadas | Estado | Cambios Necesarios |
|:---|:---|:---|:---|:---|
| UserController | service-admin | app_user, roles | ✅ Alineado | Validar roleId inmutable, ecommerceId requerido en creación |
| EcommerceController | service-admin | ecommerce | ✅ Alineado | Añadir endpoints de actualización de status |
| ApiKeyController | service-admin | api_key | ✅ Alineado | Validar control de acceso por ecommerceId |
| DiscountConfigController | service-admin | discount_settings, discount_priorities | ✅ Alineado | Refactor: separar endpoints, usar UUID en lugar de String |
| **RuleController (Unificado)** | service-admin | rules (type=SEASONAL\|PRODUCT\|CLASSIFICATION) | ✅ Implementado | Consolidado: todos los tipos de reglas en una tabla con parámetro type |
| **CustomerTierController** | service-admin | customer_tiers, classification_rule | ✅ Por crear | Gestiona tiers y classification rules anidadas |
| ConfigurationController | service-admin | (obsoleto) | ⚠️ Revisar | Podría ser redundante con DiscountConfigController |
| AuthController | service-admin | (auth) | ✅ Alineado | Mantener como está |
| RoleController | service-admin | roles, permissions, role_permissions | ❌ FALTA | **CREAR** (HU-08) |
| AuditLogController | service-admin | audit_log | ❌ FALTA | **CREAR** (HU-09) |
| DiscountApplicationLogController | service-admin | discount_application_log | ❌ FALTA | **CREAR** (HU-09) |
| DiscountConfigController (service-engine) | service-engine | discount_settings | ✅ Alineado | Sincronizar con service-admin, si es necesario |
| DiscountCalculationControllerV2 | service-engine | (engine logic) | ✅ Alineado | Sin cambios en spec, mantener como está |

---

### 2.2 Estructura de Endpoints Actualizada

#### **Backend: service-admin**

```
# USUARIOS
POST   /api/v1/users
GET    /api/v1/users                           (query: ?ecommerceId=uuid)
GET    /api/v1/users/{uid}
PUT    /api/v1/users/{uid}
DELETE /api/v1/users/{uid}

# ECOMMERCE
POST   /api/v1/ecommerces
GET    /api/v1/ecommerces                      (query: ?status=ACTIVE&page=0&size=50)
GET    /api/v1/ecommerces/{uid}
PUT    /api/v1/ecommerces/{uid}/status         (NEW)

# API KEYS
POST   /api/v1/ecommerces/{ecommerceId}/api-keys
GET    /api/v1/ecommerces/{ecommerceId}/api-keys
DELETE /api/v1/ecommerces/{ecommerceId}/api-keys/{keyId}

# DISCOUNT CONFIGURATION
POST   /api/v1/discount-config
GET    /api/v1/discount-config                 (query: ?ecommerceId=uuid)
POST   /api/v1/discount-priority
GET    /api/v1/discount-priority               (query: ?discountSettingId=uuid)

# UNIFIED RULES ENDPOINT (Seasonal, Product, Classification by Type)
# All rules (SEASONAL, PRODUCT, CLASSIFICATION) share the same endpoint with type parameter
POST   /api/v1/rules                           (body: { type: "SEASONAL"|"PRODUCT"|"CLASSIFICATION", ... })
GET    /api/v1/rules                           (query: ?type=SEASONAL|PRODUCT|CLASSIFICATION&page=0&size=20)
GET    /api/v1/rules/{uid}
PUT    /api/v1/rules/{uid}
DELETE /api/v1/rules/{uid}

# CUSTOMER TIERS (Classification Tiers Management)
POST   /api/v1/customer-tiers
GET    /api/v1/customer-tiers                  (query: ?page=0&size=20)
GET    /api/v1/customer-tiers/{tierId}
PUT    /api/v1/customer-tiers/{tierId}
DELETE /api/v1/customer-tiers/{tierId}

POST   /api/v1/customer-tiers/{tierId}/classification-rules
PUT    /api/v1/customer-tiers/{tierId}/classification-rules/{ruleId}
DELETE /api/v1/customer-tiers/{tierId}/classification-rules/{ruleId}

# ROLES & PERMISSIONS (NEW)
GET    /api/v1/roles
GET    /api/v1/roles/{roleId}
GET    /api/v1/permissions                     (query: ?module=MODULE_NAME)
POST   /api/v1/roles/{roleId}/permissions

# AUDIT & OBSERVABILITY (NEW)
GET    /api/v1/audit-logs                      (query: ?entityName=app_user&ecommerceId=uuid)
GET    /api/v1/audit-logs/{logId}
GET    /api/v1/discount-logs                   (query: ?ecommerceId=uuid&externalOrderId=order_id)
```

---

### 2.3 DTOs Requeridos

**Request / Response DTOs (resumen):**

```java
// USER
UserCreateRequest       { username, email, password, roleId, ecommerceId }
UserUpdateRequest       { username, email, password, ecommerceId, isActive }
UserResponse            { uid, username, email, roleId, ecommerceId, isActive, createdAt, updatedAt }

// ECOMMERCE
EcommerceCreateRequest  { name, slug }
EcommerceUpdateStatusRequest { status } // "ACTIVE" | "INACTIVE"
EcommerceResponse       { uid, name, slug, status, createdAt, updatedAt }

// API KEY
ApiKeyResponse          { uid, maskedKey, expiresAt, isActive }
ApiKeyListResponse      { uid, maskedKey, expiresAt, isActive, createdAt }

// DISCOUNT CONFIG
DiscountConfigCreateRequest { ecommerceId, maxDiscountCap, currencyCode, allowStacking, roundingRule }
DiscountConfigResponse      { id, ecommerceId, maxDiscountCap, currencyCode, allowStacking, roundingRule, isActive, version, createdAt, updatedAt }

DiscountLimitPriorityRequest { discountSettingId, priorities: [{ discountTypeId, priorityLevel }] }
DiscountLimitPriorityResponse { discountSettingId, priorities: [...] }

// SEASONAL RULE
SeasonalRuleCreateRequest   { name, description, discountPercentage, startDate, endDate, discountTypeId }
SeasonalRuleUpdateRequest   { name, description, discountPercentage, startDate, endDate, discountTypeId }
SeasonalRuleResponse        { id, name, description, discountPercentage, startDate, endDate, discountTypeId, isActive, createdAt, updatedAt }

// PRODUCT RULE
ProductRuleCreateRequest    { name, productType, discountPercentage, discountTypeId }
ProductRuleUpdateRequest    { name, productType, discountPercentage, discountTypeId }
ProductRuleResponse         { id, name, productType, discountPercentage, discountTypeId, isActive, createdAt, updatedAt }

// CUSTOMER TIER
CustomerTierCreateRequest   { name, discountPercentage, hierarchyLevel, discountTypeId }
CustomerTierUpdateRequest   { name, discountPercentage, hierarchyLevel, discountTypeId }
CustomerTierResponse        { id, name, discountPercentage, hierarchyLevel, isActive, classificationRules: [...], createdAt, updatedAt }

ClassificationRuleCreateRequest { metricType, minValue, maxValue, priority }
ClassificationRuleUpdateRequest { metricType, minValue, maxValue, priority }
ClassificationRuleResponse      { id, customerTierId, metricType, minValue, maxValue, priority, isActive, createdAt, updatedAt }

// ROLE & PERMISSION
RoleResponse        { id, name, isActive, permissions: [...] }
PermissionResponse  { id, code, description, module, createdAt }

// AUDIT
AuditLogResponse                    { id, userId, ecommerceId, action, entityName, entityId, oldValue (JSON), newValue (JSON), createdAt }
DiscountApplicationLogResponse       { id, ecommerceId, externalOrderId, originalAmount, discountApplied, finalAmount, appliedRulesDetails (JSON), createdAt }
```

---

### 2.4 Cambios de Seguridad & Autorización

| Endpoint | Rol Requerido | Notas |
|:---|:---|:---|
| POST /users | SUPER_ADMIN | crear usuarios globales |
| GET /users | STORE_ADMIN (su ecommerce) / SUPER_ADMIN | listar usuarios de ecommerce |
| PUT /users/{uid} | STORE_ADMIN (mismo ecommerce) / SUPER_ADMIN | actualizar perfil |
| DELETE /users/{uid} | SUPER_ADMIN | soft delete |
| POST /ecommerces | SUPER_ADMIN | crear ecommerce |
| GET /ecommerces | SUPER_ADMIN | listar todos |
| GET /ecommerces/{uid} | SUPER_ADMIN / STORE_ADMIN (mismo) | validar acceso |
| PUT /ecommerces/{uid}/status | SUPER_ADMIN | cambiar status |
| POST(/GET/DELETE) /api-keys | STORE_ADMIN (mismo ecommerce) | S2S access |
| POST /discount-config | STORE_ADMIN | configurar su ecommerce |
| GET /discount-config | STORE_ADMIN / SUPER_ADMIN | leer configuración |
| POST(/GET/PUT/DELETE) /rules?type=SEASONAL/PRODUCT/CLASSIFICATION | STORE_ADMIN | gestionar reglas unificadas |
| POST(/GET/PUT/DELETE) /customer-tiers | STORE_ADMIN | gestionar tiers de clasificación |
| GET /roles | SUPER_ADMIN | consultar roles |
| POST /roles/{id}/permissions | SUPER_ADMIN | asignar permisos |
| GET /audit-logs | SUPER_ADMIN / AUDITOR | auditoría (si existe rol) |

---

### 2.5 Cambios en Arquitectura

**Cambios en Naming:**
- Renombrar `DiscountLimitPriorityService` → `DiscountPriorityService` (más conciso)
- Refactor tablas de request/response para reducir confusiones entre `DiscountConfig` y `DiscountSettings`
- Usar `uid` (UUID) de forma consistente en lugar de mezclar con `id`
- Todos los DTOs usan **camelCase** (ej. `ecommerceId`, `discountTypeId`, `isActive`, `createdAt`)

**Cambios en Flujo:**
1. **Validaciones de Constraint**: Los DTOs deben reflejar las restricciones de BD (ej. `discountPercentage` entre 0-100, `priorityLevel` > 0)
2. **Soft Deletes**: Los endpoints DELETE implementan soft delete (`isActive = false`) en lugar de eliminación física, excepto API keys
3. **Timestamps UTC**: Todos los `createdAt` y `updatedAt` en UTC (Spring Boot lo maneja con `@Temporal`)
4. **Multi-tenancy**: Todos los endpoints de gestión deben extraer `ecommerceId` del token JWT o del path parameter

---

## 3. LISTA DE TAREAS

### 3.1 Backend Implementation Tasks

- [ ] **REVISAR UserController**
  - [ ] Validar que `role_id` es inmutable tras creación
  - [ ] Asegurar `ecommerce_id` sea requerido en creación
  - [ ] Implementar soft delete (is_active = false)
  - [ ] Validar que STORE_ADMIN solo accede a sus usuarios

- [ ] **REVISAR EcommerceController**
  - [ ] Añadir endpoint PUT `/api/v1/ecommerces/{uid}/status` para cambiar ACTIVE ↔ INACTIVE
  - [ ] Validar slug format con regex
  - [ ] Implementar paginación en listado

- [ ] **REVISAR ApiKeyController**
  - [ ] Validar `expires_at > CURRENT_TIMESTAMP` en creación
  - [ ] Implementar endpoint de rotación/renovación de keys (opcional, recomendado)
  - [ ] Enmascarar hashed_key en respuestas (mostrar solo primeros/últimos chars)

- [ ] **REVISAR DiscountConfigController**
  - [ ] Refactor para separar endpoints de config y priorities
  - [ ] Cambiar `configId` (String) por UUID
  - [ ] Añadir validación de `max_discount_cap > 0`
  - [ ] Implementar versionado de configuración

- [ ] **REFACTOR RuleController (Unificado — HU-05, HU-06, HU-07)**
  - [ ] Implementar POST/GET/PUT/DELETE /api/v1/rules con parámetro `type` (SEASONAL|PRODUCT|CLASSIFICATION)
  - [ ] Validar `start_date < end_date` para rules con type=SEASONAL
  - [ ] Validar `discount_percentage` entre 0-100 para todos los types
  - [ ] Validar unicidad (ecommerce_id, productType, is_active) para type=PRODUCT
  - [ ] Validar unicidad de seasonality (start_date, end_date, is_active) para type=SEASONAL
  - [ ] Implementar soft delete (is_active = false) para todos los types
  - [ ] Asegurar multi-tenancy (ecommerce_id extraído del JWT token)
  - [ ] Implementar filtros por type en GET /api/v1/rules?type=SEASONAL|PRODUCT|CLASSIFICATION

- [ ] **CREAR CustomerTierController (HU-07 — Tiers y Classification Rules)**
  - [ ] POST /api/v1/customer-tiers — crear tier
  - [ ] GET /api/v1/customer-tiers — listar con paginación
  - [ ] GET /api/v1/customer-tiers/{tierId} — detalle + classificationRules anidadas
  - [ ] PUT /api/v1/customer-tiers/{tierId} — actualizar tier
  - [ ] DELETE /api/v1/customer-tiers/{tierId} — soft delete
  - [ ] POST /api/v1/customer-tiers/{tierId}/classification-rules — crear rule anidada
  - [ ] PUT /api/v1/customer-tiers/{tierId}/classification-rules/{ruleId} — actualizar rule
  - [ ] DELETE /api/v1/customer-tiers/{tierId}/classification-rules/{ruleId} — eliminar rule
  - [ ] Validar `hierarchy_level` único por ecommerce
  - [ ] Validar `metric_type` en enum (total_spent, order_count, loyalty_points, custom)
  - [ ] Validar `min_value` < `max_value` en classification_rules

- [ ] **CREAR RoleController** (NUEVO)
  - [ ] GET `/api/v1/roles` — listar todos los roles
  - [ ] GET `/api/v1/roles/{roleId}` — detalles + permisos asignados
  - [ ] GET `/api/v1/permissions` — listar permisos, con filtro opcional por module
  - [ ] POST `/api/v1/roles/{roleId}/permissions` — asignar permisos a un rol

- [ ] **CREAR AuditLogController** (NUEVO)
  - [ ] GET `/api/v1/audit-logs` — listar con filtros (entityName, ecommerceId, userId, action)
  - [ ] GET `/api/v1/audit-logs/{logId}` — detalles
  - [ ] Bloquear POST/PUT/DELETE (read-only)

- [ ] **CREAR DiscountApplicationLogController** (NUEVO)
  - [ ] GET `/api/v1/discount-logs` — listar aplicaciones, con filtros (ecommerceId, externalOrderId, date range)
  - [ ] GET `/api/v1/discount-logs/{logId}` — detalles
  - [ ] Bloquear POST/PUT/DELETE (read-only)

- [ ] **CREAR/REVISAR DTOs**
  - [ ] Crear todos los Request/Response DTOs listados en sección 2.3
  - [ ] Añadir validaciones con `@Valid`, `@NotNull`, `@NotBlank`, `@Min`, `@Max`, etc.
  - [ ] Documentar con Javadoc

- [ ] **IMPLEMENTAR Interceptor de Multi-tenancy**
  - [ ] Extraer `ecommerceId` del JWT en cada request
  - [ ] Validar que usuario pertenece al ecommerce (check en `app_user`)
  - [ ] Rechazar con 403 si acceso cruzado

- [ ] **IMPLEMENTAR Auditoría (Interceptor/AOP)**
  - [ ] Capturar cambios en entidades (oldValue → newValue)
  - [ ] Grabar en tabla `audit_log` automáticamente
  - [ ] Capturar usuario del JWT, acción (CREATE/UPDATE/DELETE), timestamp

---

### 3.2 Frontend Tasks (Opcional para esta fase)

- [ ] Identificar si existen pantallas que necesiten actualizarse (no prioritario en esta sprint)

---

### 3.3 Testing Tasks

- [ ] **Unit Tests** para cada controller (validaciones, edge cases)
- [ ] **Integration Tests** para endpoints E2E
- [ ] **Security Tests** para verificar autorización y multi-tenancy

---

### 3.4 QA & Review Tasks

- [ ] Code review de todos los cambios
- [ ] Verificación de cumplimiento con instrucciones `.github/instructions/backend.instructions.md`
- [ ] Validación de que endpoints siguen patrones REST
- [ ] Prueba manual en Postman/Insomnia

---

## 4. NOTAS ADICIONALES

### Decisiones de Diseño

1. **Soft Deletes vs. Hard Deletes**: Se implementan soft deletes (is_active = false) para preservar auditoría e integridad referencial.
2. **Multi-tenancy**: Cada ecommerce es un tenant aislado. Los datos se filtran por `ecommerce_id` en todos los endpoints.
3. **Timestamps UTC**: Todos los registros usan `TIMESTAMP WITH TIME ZONE` en BD → se mapean a `OffsetDateTime` o `ZonedDateTime` en Java.
4. **API Key Rotation**: No incluido en esta spec pero recomendado en futuro (endpoint de renovación de keys).
5. **Roles Hardcoded**: Los roles (SUPER_ADMIN, STORE_ADMIN, STORE_USER) son predefinidos en la BD y seededeados. No se crean vía API.

### Riesgos Identificados (Fase 2: QA)

- **ALTO**: Multi-tenancy —validar que no haya fuga de datos entre ecommerces
- **ALTO**: Auditoría automática — serialización completa de entidades en JSON
- **MEDIO**: Validaciones de fechas — rango de seasonal rules no debe solaparse
- **MEDIO**: Constraints únicos — (ecommerce_id, product_type, is_active) en product_rules

### Dependencias Externas

- Spring Security (JWT parsing)
- Flyway (DDL migrations)
- PostgreSQL (BD)
- Lombok (DTOs)

---

## Aprobación

- **Spec Status**: `DRAFT`
- **Aprobado por:** (pendiente)
- **Fecha de aprobación:** (pendiente)

**Próximo paso**: Revisar esta spec, hacer ajustes si es necesario y cambiar status a `APPROVED` antes de iniciar implementación en paralelo (Backend Dev + Frontend Dev si aplica) → Tests → QA.
