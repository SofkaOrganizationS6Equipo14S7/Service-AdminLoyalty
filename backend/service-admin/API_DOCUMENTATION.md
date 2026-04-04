# Service Admin API Documentation

Base URL: `http://localhost:8081/api/v1`

---

## AUTH

### POST /auth/login
Iniciar sesión y obtener token JWT.

**Request Body:**
```json
{
  "username": "string (required)",
  "password": "string (required)"
}
```

**Response (200):**
```json
{
  "token": "string",
  "type": "Bearer"
}
```

---

### POST /auth/logout
Cerrar sesión (invalidar token).

**Headers:**
- `Authorization: Bearer <token>`

**Response (204):** No Content

---

### GET /auth/me
Obtener información del usuario actual.

**Headers:**
- `Authorization: Bearer <token>`

**Response (200):**
```json
{
  "uid": "uuid",
  "username": "string",
  "email": "string",
  "name": "string",
  "role": "string",
  "ecommerceId": "uuid",
  "active": boolean,
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

**Response (401):** Unauthorized (si no hay token o es inválido)

---

## USERS

### POST /users
Crear un nuevo usuario.

**Request Body:**
```json
{
  "username": "string (required, 3-50 chars)",
  "email": "string (required, valid email)",
  "password": "string (required, min 12 chars)",
  "roleId": "uuid (required)",
  "ecommerceId": "uuid (optional)"
}
```

**Response (201):**
```json
{
  "uid": "uuid",
  "username": "string",
  "email": "string",
  "name": "string",
  "role": "string",
  "ecommerceId": "uuid",
  "active": true,
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

---

### GET /users
Listar usuarios (filtro opcional por ecommerceId).

**Query Parameters:**
- `ecommerceId` (optional, uuid)

**Headers:** `Authorization: Bearer <token>`

**Response (200):**
```json
[
  {
    "uid": "uuid",
    "username": "string",
    "email": "string",
    "name": "string",
    "role": "string",
    "ecommerceId": "uuid",
    "active": true,
    "createdAt": "timestamp",
    "updatedAt": "timestamp"
  }
]
```

---

### GET /users/{uid}
Obtener usuario por ID.

**Path Parameters:**
- `uid` (uuid)

**Headers:** `Authorization: Bearer <token>`

**Response (200):** UserResponse (mismo formato que arriba)

---

### PUT /users/{uid}
Actualizar usuario.

**Path Parameters:**
- `uid` (uuid)

**Request Body (todos los campos opcionales):**
```json
{
  "username": "string (3-50 chars)",
  "email": "string (valid email)",
  "password": "string (min 12 chars)",
  "ecommerceId": "uuid (solo SUPER_ADMIN)",
  "active": "boolean (solo SUPER_ADMIN)"
}
```

**Response (200):** UserResponse actualizado

---

### DELETE /users/{uid}
Eliminar usuario (soft delete).

**Path Parameters:**
- `uid` (uuid)

**Headers:** `Authorization: Bearer <token>`

**Response (204):** No Content

---

### PUT /users/me
Actualizar perfil del usuario autenticado.

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "name": "string (required, 1-100 chars)",
  "email": "string (required, valid email)"
}
```

**Response (200):** UserResponse actualizado

---

### PUT /users/me/password
Cambiar contraseña del usuario autenticado.

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "currentPassword": "string (required)",
  "newPassword": "string (required, min 12 chars)",
  "confirmPassword": "string (required)"
}
```

**Response (200):**
```json
{
  "token": "string",
  "type": "Bearer"
}
```

---

## ECOMMERCE

### POST /ecommerces
Crear un nuevo ecommerce. Requiere rol SUPER_ADMIN.

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "name": "string (required, 3-100 chars)",
  "slug": "string (required, lowercase, numbers, hyphens, 3-255 chars)"
}
```

**Response (201):**
```json
{
  "uid": "uuid",
  "name": "string",
  "slug": "string",
  "status": "ACTIVE | INACTIVE",
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

---

### GET /ecommerces
Listar ecommerces con paginación y filtro opcional por status. Requiere rol SUPER_ADMIN.

**Query Parameters:**
- `status` (optional): "ACTIVE" | "INACTIVE"
- `page` (default: 0)
- `size` (default: 50)

**Headers:** `Authorization: Bearer <token>`

**Response (200):** Page<EcommerceResponse>

---

### GET /ecommerces/{uid}
Obtener ecommerce por ID.

**Path Parameters:**
- `uid` (uuid)

**Headers:** `Authorization: Bearer <token>`

**Response (200):** EcommerceResponse (mismo formato que arriba)

---

### PUT /ecommerces/{uid}
Actualizar status del ecommerce. Requiere rol SUPER_ADMIN.

**Path Parameters:**
- `uid` (uuid)

**Request Body:**
```json
{
  "status": "string (required, ACTIVE | INACTIVE)"
}
```

**Response (200):** EcommerceResponse actualizado

---

## API KEYS

### POST /ecommerces/{ecommerceId}/api-keys
Crear una nueva API Key para un ecommerce.

**Path Parameters:**
- `ecommerceId` (uuid)

**Headers:** `Authorization: Bearer <token>`

**Request Body:** vacío (objeto vacío `{}` o no enviar body)

**Response (201):**
```json
{
  "keyId": "uuid",
  "key": "string (API key sin enmascarar - mostrar solo una vez)",
  "prefix": "string",
  "createdAt": "timestamp"
}
```

---

### GET /ecommerces/{ecommerceId}/api-keys
Listar todas las API Keys de un ecommerce.

**Path Parameters:**
- `ecommerceId` (uuid)

**Headers:** `Authorization: Bearer <token>`

**Response (200):**
```json
[
  {
    "keyId": "uuid",
    "prefix": "string",
    "createdAt": "timestamp",
    "lastUsedAt": "timestamp"
  }
]
```

---

### DELETE /ecommerces/{ecommerceId}/api-keys/{keyId}
Eliminar una API Key.

**Path Parameters:**
- `ecommerceId` (uuid)
- `keyId` (uuid)

**Headers:** `Authorization: Bearer <token>`

**Response (204):** No Content

---

## CONFIGURATION

### POST /configurations
Crear una configuración de descuentos. Requiere rol ADMIN.

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "ecommerceId": "uuid (required)",
  "currency": "string (required, 3 chars, ej: CLP)",
  "roundingRule": "string (required)",
  "cap": {
    "type": "PERCENTAGE | FIXED",
    "value": "decimal (required, > 0)",
    "appliesTo": "SUBTOTAL | TOTAL"
  },
  "priority": [
    {
      "type": "string (required)",
      "order": "integer (required, >= 1)"
    }
  ]
}
```

**Response (201):**
```json
{
  "success": true,
  "data": {
    "ecommerceId": "uuid",
    "currency": "string",
    "roundingRule": "string",
    "cap": { ... },
    "priority": [ ... ]
  }
}
```

---

### PATCH /configurations/{ecommerceId}
Actualizar parcialmente una configuración. Requiere rol ADMIN.

**Path Parameters:**
- `ecommerceId` (uuid)

**Request Body (todos los campos opcionales):**
```json
{
  "currency": "string (3 chars)",
  "roundingRule": "string",
  "cap": {
    "type": "PERCENTAGE | FIXED",
    "value": "decimal (> 0)",
    "appliesTo": "SUBTOTAL | TOTAL"
  },
  "priority": [
    {
      "type": "string",
      "order": "integer (>= 1)"
    }
  ]
}
```

**Response (200):** ApiResponse con ConfigurationWriteData actualizado

---

## DISCOUNT CONFIG

### POST /discount-config
Crear o actualizar configuración de límites de descuentos.

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "ecommerceId": "uuid (required)",
  "maxDiscountLimit": "decimal (required)",
  "currencyCode": "string (required)",
  "allowStacking": "boolean (required)",
  "roundingRule": "string (required)",
  "capType": "string (required)",
  "capValue": "decimal (required)",
  "capAppliesTo": "string (required)"
}
```

**Response (201):**
```json
{
  "id": "uuid",
  "ecommerceId": "uuid",
  "maxDiscountLimit": "decimal",
  "currencyCode": "string",
  "allowStacking": true,
  "roundingRule": "string",
  "capType": "string",
  "capValue": "decimal",
  "capAppliesTo": "string",
  "active": true,
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

---

### GET /discount-config
Obtener configuración activa de descuentos.

**Query Parameters:**
- `ecommerceId` (required, uuid)

**Headers:** `Authorization: Bearer <token>`

**Response (200):** DiscountConfigResponse (mismo formato que arriba)

---

### POST /discount-priority
Guardar prioridades de tipos de descuento.

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "discountConfigId": "string (required)",
  "priorities": [
    {
      "discountType": "string (required)",
      "priorityLevel": "integer (required)"
    }
  ]
}
```

**Response (201):**
```json
{
  "discountConfigId": "string",
  "priorities": [
    {
      "discountType": "string",
      "priorityLevel": "integer"
    }
  ]
}
```

---

### GET /discount-priority
Obtener prioridades de descuento.

**Query Parameters:**
- `configId` (required, string)

**Headers:** `Authorization: Bearer <token>`

**Response (200):** DiscountLimitPriorityResponse

---

## RULES

### POST /rules
Crear una nueva regla. Requiere rol STORE_ADMIN.

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "name": "string (required, 3-255 chars)",
  "description": "string (optional, max 1000 chars)",
  "discountPercentage": "decimal (required, 0-100)",
  "discountPriorityId": "string (required, ej: FIDELITY, SEASONAL, PRODUCT, CLASSIFICATION)",
  "attributes": {
    "key": "value"
  }
}
```

**Response (201):**
```json
{
  "id": "uuid",
  "ecommerceId": "uuid",
  "discountPriorityId": "string",
  "name": "string",
  "description": "string",
  "discountPercentage": "decimal",
  "isActive": true,
  "attributes": { },
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

---

### GET /rules
Listar reglas con paginación y filtro opcional por status.

**Query Parameters:**
- `page` (default: 0)
- `size` (default: 20)
- `active` (optional, boolean)

**Headers:** `Authorization: Bearer <token>`

**Response (200):** Page<RuleResponse>

---

### GET /rules/{ruleId}
Obtener detalles de una regla con sus tiers asignados.

**Path Parameters:**
- `ruleId` (uuid)

**Headers:** `Authorization: Bearer <token>`

**Response (200):**
```json
{
  "id": "uuid",
  "ecommerceId": "uuid",
  "discountPriorityId": "string",
  "name": "string",
  "description": "string",
  "discountPercentage": "decimal",
  "isActive": true,
  "attributes": { },
  "customerTiers": [
    {
      "id": "uuid",
      "name": "string",
      "minPurchaseAmount": "decimal",
      "discountPercentage": "decimal",
      "active": true
    }
  ],
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

---

### PUT /rules/{ruleId}
Actualizar una regla.

**Path Parameters:**
- `ruleId` (uuid)

**Headers:** `Authorization: Bearer <token>`

**Request Body:** Mismo que POST /rules

**Response (200):** RuleResponse actualizado

---

### DELETE /rules/{ruleId}
Eliminar una regla (soft delete - isActive = false).

**Path Parameters:**
- `ruleId` (uuid)

**Headers:** `Authorization: Bearer <token>`

**Response (204):** No Content

---

### POST /rules/{ruleId}/tiers
Asignar customer tiers a una regla.

**Path Parameters:**
- `ruleId` (uuid)

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "customerTierIds": ["uuid1", "uuid2", ...] (required, no vacío)
}
```

**Response (201):** RuleResponseWithTiers

---

### GET /rules/{ruleId}/tiers
Obtener los customer tiers asignados a una regla.

**Path Parameters:**
- `ruleId` (uuid)

**Headers:** `Authorization: Bearer <token>`

**Response (200):**
```json
[
  {
    "id": "uuid",
    "name": "string",
    "minPurchaseAmount": "decimal",
    "discountPercentage": "decimal",
    "active": true
  }
]
```

---

## AUDIT LOGS (Solo lectura)

### GET /audit-logs
Listar logs de auditoría con filtros. Requiere rol SUPER_ADMIN.

**Query Parameters:**
- `entityName` (optional, string, ej: "app_user", "ecommerce")
- `ecommerceId` (optional, uuid)
- `page` (default: 0)
- `size` (default: 50)

**Headers:** `Authorization: Bearer <token>`

**Response (200):** Page<AuditLogResponse>

---

### GET /audit-logs/{logId}
Obtener detalles de un log de auditoría específico.

**Path Parameters:**
- `logId` (uuid)

**Headers:** `Authorization: Bearer <token>`

**Response (200):**
```json
{
  "id": "uuid",
  "entityName": "string",
  "entityId": "uuid",
  "ecommerceId": "uuid",
  "action": "string",
  "userId": "uuid",
  "oldValue": "json string",
  "newValue": "json string",
  "timestamp": "timestamp"
}
```

**PUT/DELETE:** Retorna 405 Method Not Allowed (logs son inmutables)

---

## DISCOUNT LOGS (Solo lectura)

### GET /discount-logs
Listar logs de descuentos aplicados con filtros.

**Query Parameters:**
- `ecommerceId` (optional, uuid)
- `externalOrderId` (optional, string, ej: #ORDER-12345)
- `page` (default: 0)
- `size` (default: 50)

**Headers:** `Authorization: Bearer <token>`

**Response (200):** Page<DiscountApplicationLogResponse>

---

### GET /discount-logs/{logId}
Obtener detalles de un log de descuento específico.

**Path Parameters:**
- `logId` (uuid)

**Headers:** `Authorization: Bearer <token>`

**Response (200):**
```json
{
  "id": "uuid",
  "ecommerceId": "uuid",
  "externalOrderId": "string",
  "appliedDiscounts": [
    {
      "discountType": "string",
      "percentage": "decimal",
      "amount": "decimal"
    }
  ],
  "subtotal": "decimal",
  "totalDiscount": "decimal",
  "finalTotal": "decimal",
  "currency": "string",
  "appliedAt": "timestamp"
}
```

**PUT/DELETE:** Retorna 405 Method Not Allowed (logs son inmutables)

---

## ROLES Y PERMISOS

| Rol | Permisos |
|-----|----------|
| SUPER_ADMIN | Acceso completo a todos los endpoints |
| STORE_ADMIN | Acceso a su propio ecommerce (no puede crear ecommerces) |
| STORE_USER | Solo lectura de su perfil y cambio de contraseña |
| ADMIN | Configuración de descuentos |

---

## AUTENTICACIÓN

Todos los endpoints (excepto `/auth/login`) requieren:
- Header: `Authorization: Bearer <JWT_TOKEN>`

El token se obtiene del endpoint `POST /auth/login`.

---

## CÓDIGOS DE RESPUESTA

| Código | Descripción |
|--------|-------------|
| 200 | OK - Solicitud exitosa |
| 201 | Created - Recurso creado exitosamente |
| 204 | No Content - Respuesta vacía exitosa |
| 400 | Bad Request - Error en los datos enviados |
| 401 | Unauthorized - Token inválido o ausente |
| 403 | Forbidden - No tiene permisos |
| 404 | Not Found - Recurso no encontrado |
| 405 | Method Not Allowed - Método no soportado |
| 500 | Internal Server Error - Error en el servidor |

---

## EJEMPLO DE USO EN POSTMAN

### 1. Login
```
POST http://localhost:8081/api/v1/auth/login
Body (JSON):
{
  "username": "admin",
  "password": "password123"
}
```

### 2. Usar token en siguientes requests
Agregar header:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 3. Listar usuarios
```
GET http://localhost:8081/api/v1/users
```

### 4. Crear ecommerce (SUPER_ADMIN)
```
POST http://localhost:8081/api/v1/ecommerces
Body (JSON):
{
  "name": "Mi Tienda",
  "slug": "mi-tienda"
}
```