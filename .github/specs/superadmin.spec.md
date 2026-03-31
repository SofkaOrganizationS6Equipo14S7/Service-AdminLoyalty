---
id: SPEC-002
status: APPROVED
feature: superadmin-access-control
created: 2026-03-29
updated: 2026-03-29
author: spec-generator
version: "1.0"
related-specs: ["SPEC-001", "SPEC-002"]
---

# Spec: SUPER_ADMIN — Acceso Total a la Plataforma

> **Estado:** `APPROVED` — listo para implementación fase backend/frontend.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPRECATED

---

## 1. REQUERIMIENTOS

### Descripción

SUPER_ADMIN es un rol con acceso total a la plataforma sin vinculación a ecommerce específico. Puede gestionar todos los ecommerces, crear/editar/eliminar STORE_ADMIN de cada marca, cambiar la vinculación de usuarios entre ecommerces y configurar el sistema global. Esta funcionalidad es crítica para la gobernanza multi-tenant.

### Requerimiento de Negocio

El usuario SUPER_ADMIN necesita:
- **Acceso global:** No está vinculado a ningún ecommerce. Puede gestionar todos.
- **Gestión de administradores:** Crear, editar y eliminar STORE_ADMIN para cada ecommerce.
- **Listado de ecommerces:** Ver todos los ecommerces registrados en el sistema.
- **Cambio de vinculación:** Reasignar usuarios (STORE_ADMIN o STORE_USER) a diferentes ecommerces.
- **Aislamiento en datos:** Las consultas de SUPER_ADMIN NO filtran por `ecommerce_id` (acceso global).

### Historias de Usuario

#### HU-02.1: Crear STORE_ADMIN para ecommerce

```
Como:        SUPER_ADMIN (rol con acceso total)
Quiero:      crear un usuario con rol STORE_ADMIN para un ecommerce
Para:        que ese ecommerce tenga administrador dedicado y pueda gestionar usuarios estándar

Prioridad:   Alta
Estimación:  M
Dependencias: HU-01 (requerimiento SPEC-002)
Capa:        Backend
```

#### Criterios de Aceptación — HU-02.1

**Happy Path — Crear STORE_ADMIN exitosamente**
```gherkin
CRITERIO-2.1.1: SUPER_ADMIN crea STORE_ADMIN para ecommerce válido
  Dado que:    soy un usuario con rol SUPER_ADMIN autenticado
  Y que:       el ecommerce "Nike" existe en el sistema
  Cuando:      hago POST /api/v1/users con:
               { "username": "nike-admin", "password": "SecurePass123!", "role": "STORE_ADMIN", "ecommerceId": "<nike-uid>" }
  Entonces:    retorna 201 Created
  Y que:       el usuario "nike-admin" queda vinculado al ecommerce Nike
  Y que:       el usuario puede autenticarse con su contraseña
  Y que:       el JWT incluye el claim "ecommerce_id" con el UUID de Nike
```

**Error Path — Credenciales inválidas**
```gherkin
CRITERIO-2.1.2: SUPER_ADMIN intenta crear STORE_ADMIN con username duplicado
  Dado que:    soy SUPER_ADMIN
  Y que:       "nike-admin" ya existe en el sistema
  Cuando:      intento crear otro usuario con username "nike-admin"
  Entonces:    retorna 409 Conflict
  Y que:       respuesta es { "error": "El username ya existe. Use otro." }
  Y que:       el usuario NO se crea
```

**Error Path — Permiso insuficiente (usuario no autorizado)**
```gherkin
CRITERIO-2.1.3: Usuario STORE_USER intenta crear usuario
  Dado que:    soy un usuario con rol STORE_USER
  Cuando:      intento crear un nuevo usuario
  Entonces:    retorna 403 Forbidden
  Y que:       respuesta es { "error": "Solo SUPER_ADMIN o STORE_ADMIN pueden crear usuarios" }
```

**Happy Path — STORE_ADMIN crea usuario en su ecommerce**
```gherkin
CRITERIO-2.1.4: STORE_ADMIN crea STORE_USER dentro de su ecommerce
  Dado que:    soy un usuario con rol STORE_ADMIN vinculado a ecommerce Nike
  Y que:       el ecommerce Nike existe en el sistema
  Cuando:      hago POST /api/v1/users con:
               { "username": "nike-staff", "password": "SecurePass123!", "role": "STORE_USER", "ecommerceId": "<nike-uid>" }
  Entonces:    retorna 201 Created
  Y que:       el usuario "nike-staff" queda vinculado al ecommerce Nike
  Y que:       el usuario puede autenticarse
```

**Error Path — STORE_ADMIN intenta crear usuario en otro ecommerce**
```gherkin
CRITERIO-2.1.5: STORE_ADMIN intenta crear usuario en ecommerce ajeno
  Dado que:    soy STORE_ADMIN vinculado a Nike
  Y que:       el ecommerce Adidas existe
  Cuando:      intento crear usuario con ecommerceId = Adidas
  Entonces:    retorna 403 Forbidden
  Y que:       respuesta es { "error": "Solo puede crear usuarios dentro de su ecommerce" }
  Y que:       el usuario NO se crea
```

**Edge Case — Rol no permitido**
```gherkin
CRITERIO-2.1.6: SUPER_ADMIN intenta crear usuario con rol inválido
  Dado que:    soy SUPER_ADMIN
  Cuando:      intento crear usuario con role="CUSTOM_ROLE"
  Entonces:    retorna 400 Bad Request
  Y que:       respuesta es { "error": "Solo se permiten roles: SUPER_ADMIN, STORE_ADMIN, STORE_USER." }
```

---

#### HU-02.2: Listar todos los ecommerces

```
Como:        SUPER_ADMIN
Quiero:      ver una lista de todos los ecommerces registrados
Para:        gestionar y asignar administradores a cada marca

Prioridad:   Alta
Estimación:  S
Dependencias: SPEC-001 (ecommerces ya existen)
Capa:        Backend
```

#### Criterios de Aceptación — HU-02.2

**Happy Path — Listar ecommerces sin filtros**
```gherkin
CRITERIO-2.2.1: SUPER_ADMIN lista todos los ecommerces
  Dado que:    soy SUPER_ADMIN
  Y que:       existen 3 ecommerces: Nike, Adidas, Puma
  Cuando:      hago GET /api/v1/ecommerces
  Entonces:    retorna 200 OK
  Y que:       respuesta incluye todos los 3 ecommerces
  Y que:       cada item incluye: uid, name, slug, status, createdAt, updatedAt
  Y que:       NO hay filtro por ecommerce_id (acceso global sin restricción)
```

**Error Path — Usuario no SUPER_ADMIN**
```gherkin
CRITERIO-2.2.2: Usuario STORE_ADMIN intenta listar todos los ecommerces
  Dado que:    soy un STORE_ADMIN vinculado a Nike
  Cuando:      intento GET /api/v1/ecommerces
  Entonces:    retorna 403 Forbidden
  Y que:       respuesta es { "error": "Solo SUPER_ADMIN puede ver lista global de ecommerces" }
  Y que:       no veo la lista global de ecommerces
```

---

#### HU-02.3: Modificar STORE_ADMIN de un ecommerce

```
Como:        SUPER_ADMIN
Quiero:      modificar los datos de un STORE_ADMIN (username, contraseña, estado)
Para:        mantener actualizada la información y rotar credenciales si es necesario

Prioridad:   Media
Estimación:  M
Dependencias: HU-02.1 (STORE_ADMIN creado)
Capa:        Backend
```

#### Criterios de Aceptación — HU-02.3

**Happy Path — SUPER_ADMIN actualiza credenciales de usuario**
```gherkin
CRITERIO-2.3.1: SUPER_ADMIN actualiza credenciales de STORE_ADMIN
  Dado que:    soy SUPER_ADMIN
  Y que:       existe STORE_ADMIN "nike-admin" vinculado a Nike
  Cuando:      hago PUT /api/v1/users/<uid> con:
               { "username": "nike-admin-v2", "password": "NewSecurePass456!" }
  Entonces:    retorna 200 OK
  Y que:       el usuario ahora usa el nuevo username y contraseña
  Y que:       la vinculación a Nike se mantiene intacta
```

**Happy Path — Usuario actualiza su propio perfil**
```gherkin
CRITERIO-2.3.1B: STORE_USER cambia su propia contraseña
  Dado que:    soy un usuario STORE_USER autenticado (uid=<su-uid>)
  Cuando:      hago PUT /api/v1/users/<su-uid> con:
               { "password": "NewSecurePass456!" }
  Entonces:    retorna 200 OK
  Y que:       puedo autenticarme con la nueva contraseña
```

**Error Path — Usuario NO puede cambiar su propio ecommerce_id**
```gherkin
CRITERIO-2.3.1C: STORE_USER intenta cambiar su ecommerce asignado
  Dado que:    soy STORE_USER vinculado a Nike
  Cuando:      intento PUT /api/v1/users/<su-uid> con: { "ecommerceId": "<adidas-uid>" }
  Entonces:    retorna 403 Forbidden
  Y que:       respuesta es { "error": "No puede cambiar su ecommerce_id" }
  Y que:       mi vinculación a Nike se mantiene
```

**Error Path — Username en uso**
```gherkin
CRITERIO-2.3.2: SUPER_ADMIN intenta cambiar username a uno duplicado
  Dado que:    soy SUPER_ADMIN
  Y que:       "adidas-admin" ya existe
  Cuando:      intento cambiar "nike-admin" a "adidas-admin"
  Entonces:    retorna 409 Conflict
  Y que:       la actualización NO ocurre
```

---

#### HU-02.4: Cambiar vinculación de usuario entre ecommerces

```
Como:        SUPER_ADMIN
Quiero:      reasignar un usuario de un ecommerce a otro
Para:        adaptarme a cambios organizacionales sin eliminar el perfil

Prioridad:   Alta
Estimación:  M
Dependencias: HU-02.1, HU-02.2
Capa:        Backend
```

#### Criterios de Aceptación — HU-02.4

**Happy Path — Cambiar ecommerce de usuario**
```gherkin
CRITERIO-2.4.1: SUPER_ADMIN reasigna STORE_ADMIN de Nike a Adidas
  Dado que:    soy SUPER_ADMIN
  Y que:       "nike-admin" está vinculado a ecommerce Nike (uid=<nike-uid>)
  Y que:       ecommerce Adidas existe (uid=<adidas-uid>)
  Cuando:      hago PUT /api/v1/users/<nike-admin-uid> con:
               { "ecommerceId": "<adidas-uid>" }
  Entonces:    retorna 200 OK
  Y que:       el usuario ahora pertenece a Adidas
  Y que:       el usuario mantiene su username y contraseña
  Y que:       el JWT al login ahora incluye ecommerce_id=<adidas-uid>
```

**Error Path — Ecommerce no existe**
```gherkin
CRITERIO-2.4.2: SUPER_ADMIN intenta asignar usuario a ecommerce inexistente
  Dado que:    soy SUPER_ADMIN
  Y que:       un ecommerce con uid="<invalid-uuid>" no existe
  Cuando:      intento reasignar un usuario a ese ecommerce
  Entonces:    retorna 404 Not Found
  Y que:       respuesta es { "error": "Ecommerce no encontrado" }
  Y que:       el usuario mantiene su vinculación anterior
```

---

#### HU-02.5: Eliminar STORE_ADMIN de un ecommerce

```
Como:        SUPER_ADMIN
Quiero:      eliminar un usuario (STORE_ADMIN o STORE_USER) permanentemente
Para:        remover acceso cuando alguien deja de pertenecer a la plataforma

Prioridad:   Alta
Estimación:  S
Dependencias: HU-02.1
Capa:        Backend
```

#### Criterios de Aceptación — HU-02.5

**Happy Path — Eliminar usuario exitosamente**
```gherkin
CRITERIO-2.5.1: SUPER_ADMIN elimina un STORE_ADMIN
  Dado que:    soy SUPER_ADMIN
  Y que:       "nike-admin" existe y está vinculado a Nike
  Cuando:      hago DELETE /api/v1/users/<nike-admin-uid>
  Entonces:    retorna 204 No Content
  Y que:       el usuario es eliminado permanentemente
  Y que:       el usuario YA NO puede autenticarse
  Y que:       su perfil no aparece en listados
```

**Happy Path — STORE_ADMIN elimina usuario de su ecommerce**
```gherkin
CRITERIO-2.5.1B: STORE_ADMIN elimina usuario de su STORE_USER
  Dado que:    soy STORE_ADMIN vinculado a Nike
  Y que:       "nike-staff" es un STORE_USER vinculado a Nike
  Cuando:      hago DELETE /api/v1/users/<nike-staff-uid>
  Entonces:    retorna 204 No Content
  Y que:       el usuario es eliminado
```

**Error Path — STORE_ADMIN intenta eliminar usuario de otro ecommerce**
```gherkin
CRITERIO-2.5.1C: STORE_ADMIN intenta eliminar usuario de otro ecommerce
  Dado que:    soy STORE_ADMIN vinculado a Nike
  Y que:       "adidas-staff" existe en Adidas
  Cuando:      intento DELETE /api/v1/users/<adidas-staff-uid>
  Entonces:    retorna 403 Forbidden
  Y que:       respuesta es { "error": "No puede eliminar usuarios de otro ecommerce" }
```

**Error Path — Auto-eliminación**
```gherkin
CRITERIO-2.5.2: SUPER_ADMIN intenta eliminarse a sí mismo
  Dado que:    soy SUPER_ADMIN con uid=<su-uid>
  Cuando:      intento DELETE /api/v1/users/<su-uid>
  Entonces:    retorna 400 Bad Request
  Y que:       respuesta es { "error": "No puede eliminar su propio usuario" }
  Y que:       el usuario NO se elimina
```

**Error Path — Usuario no encontrado**
```gherkin
CRITERIO-2.5.3: SUPER_ADMIN intenta eliminar usuario inexistente
  Dado que:    soy SUPER_ADMIN
  Cuando:      intento DELETE /api/v1/users/<invalid-uuid>
  Entonces:    retorna 404 Not Found
```

---

### Reglas de Negocio

1. **RN-01: Rol SUPER_ADMIN sin vinculación a ecommerce**
   - Un SUPER_ADMIN SIEMPRE tiene `ecommerce_id = NULL` en la BD.
   - Un SUPER_ADMIN NUNCA ve filtros por `ecommerce_id` en sus consultas.
   - Constraint DB: `(role='SUPER_ADMIN' AND ecommerce_id IS NULL)`

2. **RN-02: Roles permitidos en la plataforma**
   - `SUPER_ADMIN`: Acceso global sin restricciones.
   - `STORE_ADMIN`: Vinculado obligatoriamente a un ecommerce. Puede crear/editar usuarios estándar de su marca.
   - `STORE_USER`: Vinculado obligatoriamente a un ecommerce. Usuario final con permisos limitados.

3. **RN-03: Username es único globalmente**
   - No dos usuarios pueden tener el mismo username en todo el sistema.
   - Validación en creación y actualización.

4. **RN-04: Contraseña fuerte**
   - Mínimo 12 caracteres.
   - Se encripta con BCrypt (costo mínimo 10).

5. **RN-05: Gestión de usuarios por contexto de autorización**
   - **SUPER_ADMIN**: Puede CREATE, UPDATE, DELETE cualquier usuario en cualquier ecommerce.
   - **STORE_ADMIN**: Puede CREATE, UPDATE, DELETE usuarios dentro de su propio ecommerce_id únicamente.
   - **STORE_USER**: Puede actualizar su propio perfil (contraseña, datos personales). No puede crear ni eliminar usuarios.
   - Validación: Si STORE_ADMIN intenta operar sobre usuario de otro ecommerce → 403 Forbidden.

6. **RN-06: Acceso restringido a listado de ecommerces**
   - **SUPER_ADMIN**: Ve todos los ecommerces sin restricción.
   - **STORE_ADMIN**: Ve solo su propio ecommerce (no tiene acceso a GET /api/v1/ecommerces?all=true).
   - **STORE_USER**: Acceso limitado a datos de su ecommerce únicamente.

7. **RN-07: Cambio de ecommerce válido**
   - No se puede asignar un usuario a un ecommerce que no existe.
   - Al cambiar ecommerce, el usuario mantiene credenciales y uid.

8. **RN-08: Auditoría de cambios**
   - Todo CREATE, UPDATE, DELETE de usuario se registra en logs (username, timestamp, actor).

---

## 2. DISEÑO

### Modelos de Datos

#### Entidades afectadas

| Entidad | Almacén | Cambios | Descripción |
|---------|---------|---------|-------------|
| `UserEntity` | tabla `users` | modificada | Añadir validaciones para SUPER_ADMIN role |
| `EcommerceEntity` | tabla `ecommerces` | sin cambios | Ya existe (SPEC-001) |

#### Campos del modelo `UserEntity` (estado actual + validaciones)

| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `id` | BIGINT (PK) | sí | auto-generado | ID secuencial interno |
| `uid` | UUID | sí | UNIQUE | Identificador único público |
| `username` | VARCHAR(50) | sí | UNIQUE, NOT NULL | Nombre de usuario global |
| `password` | VARCHAR(255) | sí | NOT NULL, 12+ chars → BCrypt | Contraseña hasheada |
| `role` | VARCHAR(50) | sí | Enum: SUPER_ADMIN, STORE_ADMIN, STORE_USER | Rol del usuario |
| `ecommerce_id` | UUID | condicional | FK → ecommerces.uid, NULL si SUPER_ADMIN | UUID del ecommerce |
| `active` | BOOLEAN | sí | DEFAULT true | Flag de soft delete (true=activo) |
| `created_at` | TIMESTAMP (UTC) | sí | NOT NULL, DEFAULT NOW() | Timestamp creación |
| `updated_at` | TIMESTAMP (UTC) | sí | NOT NULL, DEFAULT NOW() | Timestamp última actualización |

#### Índices / Constraints

| Índice | Columnas | Tipo | Justificación |
|--------|----------|------|--------------|
| `idx_username` | `(username)` | UNIQUE | Búsqueda por username (login) |
| `idx_ecommerce_id` | `(ecommerce_id)` | normal | Filtrado rápido usuarios por ecommerce |
| `idx_active` | `(active)` | normal | Filtrado por estado activo |
| `idx_role` | `(role)` | normal | Filtrado por rol (e.g., contar SUPER_ADMIN) |
| `idx_uid` | `(uid)` | UNIQUE | Búsqueda por identificador público |
| **CHECK constraint** | `(role='SUPER_ADMIN' AND ecommerce_id IS NULL) OR (role!='SUPER_ADMIN' AND ecommerce_id IS NOT NULL)` | – | Garantiza que SUPER_ADMIN NO tiene ecommerce_id |

---

### API Endpoints

#### POST /api/v1/users
- **Descripción**: Crea un nuevo usuario (SUPER_ADMIN, STORE_ADMIN o STORE_USER)
- **Auth requerida**: Sí (JWT token en header `Authorization: Bearer <token>`)
- **Autorización**: Acceso permitido si:
  1. El actor es **SUPER_ADMIN** → puede crear usuario en cualquier ecommerce con cualquier rol.
  2. El actor es **STORE_ADMIN** → puede crear usuario dentro de su ecommerce únicamente, con rol STORE_USER o STORE_ADMIN (para gestionar su staff).
- **Request Body**:
  ```json
  {
    "username": "string (3-50 chars, unique)",
    "password": "string (mínimo 12 chars)",
    "role": "enum (SUPER_ADMIN|STORE_ADMIN|STORE_USER)",
    "ecommerceId": "uuid (obligatorio si role != SUPER_ADMIN; si actor es STORE_ADMIN, debe coincidir con su ecommerce_id)"
  }
  ```
- **Response 201 Created**:
  ```json
  {
    "uid": "550e8400-e29b-41d4-a716-446655440000",
    "username": "nike-admin",
    "role": "STORE_ADMIN",
    "ecommerceId": "550e8400-e29b-41d4-a716-446655440001",
    "active": true,
    "createdAt": "2026-03-29T10:30:00Z",
    "updatedAt": "2026-03-29T10:30:00Z"
  }
  ```
- **Response 400**: Validación fallida (username duplicado, ecommerce no existe, contraseña débil, etc.)
- **Response 401**: Token ausente o expirado
- **Response 403**: Permiso insuficiente (ej: STORE_ADMIN intenta crear usuario en otro ecommerce, o STORE_USER intenta crear usuario)
- **Response 409**: Username duplicado

---

#### GET /api/v1/users
- **Descripción**: Lista usuarios
  - Si caller es **SUPER_ADMIN**: retorna todos los usuarios (con filtro opcional por `ecommerceId`)
  - Si caller es **STORE_ADMIN**: retorna solo usuarios de su ecommerce (filtro auto-aplicado, no se puede request otro ecommerce)
  - Si caller es **STORE_USER**: no tiene acceso (retorna 403 Forbidden)
- **Auth requerida**: Sí
- **Query params (opcionales)**:
  - `ecommerceId`: UUID (solo SUPER_ADMIN puede usar para filtrar; STORE_ADMIN intento de acceso cruzado retorna 403)
  - `role`: enum para filtrar por rol (SUPER_ADMIN puede filtrar cualquier rol; STORE_ADMIN solo ve roles de su ecommerce)
  - `active`: boolean (true/false)
- **Response 200 OK**:
  ```json
  [
    {
      "uid": "550e8400-e29b-41d4-a716-446655440000",
      "username": "nike-admin",
      "role": "STORE_ADMIN",
      "ecommerceId": "550e8400-e29b-41d4-a716-446655440001",
      "active": true,
      "createdAt": "2026-03-29T10:30:00Z",
      "updatedAt": "2026-03-29T10:30:00Z"
    }
  ]
  ```
- **Response 403**: Intento de acceso prohibido (ej: STORE_USER intenta acceder, o STORE_ADMIN intenta filtrar otro ecommerce)

---

#### GET /api/v1/users/{uid}
- **Descripción**: Obtiene un usuario por uid
- **Auth requerida**: Sí
- **Autorización**: Acceso permitido si:
  1. El actor es **SUPER_ADMIN** → puede obtener cualquier usuario.
  2. El actor es **STORE_ADMIN** → puede obtener solo usuarios de su ecommerce_id.
  3. El actor es el **dueño del perfil** (uid del token == uid del endpoint) → puede obtener su propio perfil.
- **Response 200 OK**: User completo (igual estructura que POST response)
- **Response 404**: Usuario no encontrado
- **Response 403**: Permiso insuficiente (ej: STORE_ADMIN intenta ver usuario de otro ecommerce, o STORE_USER intenta ver perfil ajeno)

---

#### PUT /api/v1/users/{uid}
- **Descripción**: Actualiza un usuario (username, password, ecommerceId, active)
- **Auth requerida**: Sí (JWT token)
- **Autorización**: Acceso permitido si:
  1. El actor es **SUPER_ADMIN** (acceso global a cualquier usuario).
  2. El actor es **STORE_ADMIN** y el usuario a actualizar pertenece al mismo ecommerce_id.
  3. El actor es el **dueño del perfil** (uid del token == uid del endpoint), permitiendo actualizar propio username, password y datos personales. Los STORE_ADMIN/STORE_USER NO pueden alterar su ecommerce_id ni el campo `active`.
- **Request Body** (todos los campos opcionales):
  ```json
  {
    "username": "string (opcional, 3-50 chars)",
    "password": "string (opcional, 12+ chars)",
    "ecommerceId": "uuid (opcional si es SUPER_ADMIN, forbidden si actor es dueño del perfil)",
    "active": "boolean (opcional, solo SUPER_ADMIN puede modificar)"
  }
  ```
- **Response 200 OK**: Usuario actualizado (misma estructura que POST response)
- **Response 400**: Validación fallida (username duplicado, ecommerce no existe, intento de cambiar campo prohibido)
- **Response 401**: Token ausente o expirado
- **Response 403**: Permiso insuficiente (ej: STORE_ADMIN intenta editar usuario de otro ecommerce, o usuario no-SUPER_ADMIN intenta cambiar su propio ecommerce_id)
- **Response 404**: Usuario no encontrado
- **Response 409**: Username duplicado

---

#### DELETE /api/v1/users/{uid}
- **Descripción**: Elimina un usuario permanentemente
- **Auth requerida**: Sí
- **Autorización**: Acceso permitido si:
  1. El actor es **SUPER_ADMIN** → puede eliminar cualquier usuario.
  2. El actor es **STORE_ADMIN** → puede eliminar solo usuarios de su ecommerce_id.
- **Validaciones**:
  - Usuario no puede eliminar su propio perfil (uid actual) → retorna 400 Bad Request
  - Usuario debe existir
  - STORE_ADMIN intenta eliminar usuario de otro ecommerce → retorna 403 Forbidden
- **Response 204 No Content**: Eliminado exitosamente
- **Response 400**: Intento de auto-eliminación
- **Response 401**: Token ausente o expirado
- **Response 403**: Permiso insuficiente (ej: STORE_ADMIN intenta eliminar usuario de otro ecommerce, o STORE_USER intenta eliminar)
- **Response 404**: Usuario no encontrado

---

#### GET /api/v1/ecommerces (modificado para SUPER_ADMIN context)
- **Descripción**: Lista ecommerces
  - **Si caller es SUPER_ADMIN**: retorna TODOS los ecommerces sin filtro
  - **Si caller es STORE_ADMIN/STORE_USER**: retorna solo su ecommerce o error 403
- **Auth requerida**: Sí
- **Response 200 OK**:
  ```json
  [
    {
      "uid": "550e8400-e29b-41d4-a716-446655440001",
      "name": "Nike Store",
      "slug": "nike-store",
      "status": "ACTIVE",
      "createdAt": "2026-03-29T10:30:00Z",
      "updatedAt": "2026-03-29T10:30:00Z"
    }
  ]
  ```
- **Response 403**: No es SUPER_ADMIN (para usuarios STORE_ADMIN que intentan acceso global)

---

### Diseño Frontend

#### Componentes nuevos

| Componente | Archivo | Props principales | Descripción |
|------------|---------|------------------|-------------|
| `SuperAdminPanel` | `components/SuperAdminPanel` | `onUserCreated, onUserDeleted` | Panel principal de SUPER_ADMIN |
| `UserManagementTable` | `components/UserManagementTable` | `users, onEdit, onDelete, isLoading` | Tabla de usuarios con CRUD |
| `UserFormModal` | `components/UserFormModal` | `isOpen, isEdit, user, onSubmit, onClose` | Modal crear/editar usuario |
| `EcommerceListDropdown` | `components/EcommerceListDropdown` | `selectedId, onChange, isLoading` | Dropdown para seleccionar ecommerce |
| `AllEcommercesPage` | `pages/AllEcommercesPage` | – | Página listado global de ecommerce (solo SUPER_ADMIN) |

#### Páginas nuevas

| Página | Archivo | Ruta | Protegida | Visible |
|--------|---------|------|-----------|---------|
| `SuperAdminDashboard` | `pages/SuperAdminDashboard` | `/superadmin` | Sí (JWT) | Solo SUPER_ADMIN |
| `AllEcommercesPage` | `pages/AllEcommercesPage` | `/superadmin/ecommerces` | Sí | Solo SUPER_ADMIN |
| `UserManagementPage` | `pages/UserManagementPage` | `/superadmin/users` | Sí | Solo SUPER_ADMIN |

#### Hooks y State Management

| Hook | Archivo | Retorna | Descripción |
|------|---------|---------|-------------|
| `useSuperAdminUsers` | `hooks/useSuperAdminUsers` | `{ users, loading, error, create, update, delete, listByEcommerce }` | CRUD usuarios con contexto SUPER_ADMIN |
| `useAllEcommerces` | `hooks/useAllEcommerces` | `{ ecommerces, loading, error, refresh }` | Acceso global a todos los ecommerces |
| `useUserAssignment` | `hooks/useUserAssignment` | `{ assignUser, loading, error }` | Reasignar usuario a ecommerce |

#### Services (llamadas API)

| Función | Archivo | Endpoint | Descripción |
|---------|---------|---------|-------------|
| `getAllUsers(token, filters)` | `services/superAdminService` | `GET /api/v1/users` | Listar todos usuarios (SUPER_ADMIN context) |
| `createUser(data, token)` | `services/superAdminService` | `POST /api/v1/users` | Crear nuevo usuario |
| `updateUser(uid, data, token)` | `services/superAdminService` | `PUT /api/v1/users/{uid}` | Actualizar usuario |
| `deleteUser(uid, token)` | `services/superAdminService` | `DELETE /api/v1/users/{uid}` | Eliminar usuario |
| `getAllEcommerces(token)` | `services/superAdminService` | `GET /api/v1/ecommerces` | Listar todos los ecommerces |
| `assignUserToEcommerce(uid, ecommerceId, token)` | `services/superAdminService` | `PUT /api/v1/users/{uid}` | Reasignar usuario a ecommerce |

#### UI/UX Considerations

- **Access Control:** El frontend debe verificar JWT claim `role: SUPER_ADMIN` antes de mostrar rutas `/superadmin/*`.
- **User Confirmation:** Modal de confirmación antes de eliminar usuarios.
- **Ecommerce Filter:** Dropdown para filtrar usuarios por ecommerce (antes de listar).
- **Password Reset:** Campo de contraseña en edición debe ser opcional (solo si se quiere resetear).
- **Error Handling:** Mensajes claros para conflictos (username duplicado, ecommerce no existe, permiso insuficiente).

---

### Arquitectura y Dependencias

#### Paquetes sin cambios

- `com.loyalty.service_admin.domain.entity.UserEntity` — Ya implementado (SPEC-002)
- `com.loyalty.service_admin.domain.repository.UserRepository` — Ya implementado
- `com.loyalty.service_admin.application.service.UserService` — Se mejora para SUPER_ADMIN context
- `com.loyalty.service_admin.presentation.controller.UserController` — Se mejora con validaciones
- `com.loyalty.service_admin.infrastructure.security.SecurityContextHelper` — Ya soporta rol SUPER_ADMIN

#### Cambios incrementales

**Backend:**
1. Actualizar `UserEntity` con CHECK constraint (SUPER_ADMIN + ecommerce_id=NULL).
2. Mejorar `UserService.listUsers()` para manejo SUPER_ADMIN (sin filtro de ecommerce).
3. Mejorar `UserService.updateUser()` validación de cambio de ecommerce.
4. Mejorar `EcommerceService.getAll()` para que SUPER_ADMIN vea todos sin filtro.
5. Añadir validaciones en `SecurityContextHelper` para detectar rol SUPER_ADMIN.

**Frontend:**
1. Crear componente `SuperAdminPanel` como entry point.
2. Crear página `/superadmin` con dashboard y acciones rápidas.
3. Crear tabla de usuarios con columna "ecommerce" (visible para SUPER_ADMIN).
4. Crear página `/superadmin/ecommerces` listado global.
5. Proteger rutas `/superadmin/*` con verificación de JWT role.

#### Integraciones y servicios externos

- **RabbitMQ:** No afecta. Los eventos de usuario cambios siguen mismo flujo.
- **PostgreSQL:** CHECK constraint añadido en migration.
- **JWT (JwtProvider):** Sin cambios. El claim `role: SUPER_ADMIN` ya fomata correctamente.

#### Impacto en punto de entrada de la app

- **Backend (`ServiceAdminApplication`):** Sin cambios. Configuración existente soporta SUPER_ADMIN.
- **Frontend:** Añadir ruta `/superadmin` en router principal si aún no existe.

---

### Notas de Implementación

- **CHECK Constraint:** Migracion Flyway nuevamás debe añadir constraint `(role='SUPER_ADMIN' AND ecommerce_id IS NULL) OR (role!='SUPER_ADMIN' AND ecommerce_id IS NOT NULL)` en tabla `users`.
- **Validación de Ecommerce:** Toda operación que refiera a `ecommerce_id` debe validar que existe en tabla ecommerces.
- **Logging de Auditoría:** Toda operación CRUD debe loguear `[actor=SUPER_ADMIN uid=X, action=CREATE|UPDATE|DELETE, target=user uid=Y, timestamp=Z]`.
- **Frontend Role Guard:** Usar helper `isUserSuperAdmin()` (extraído del JWT `role` claim) antes de renderizar componentyes `/superadmin/*`.
- **Backwards Compatibility:** Todas las operaciones STORE_ADMIN/STORE_USER existentes deben funcionar igual (filtro por ecommerce_id se aplica solo si user NO es SUPER_ADMIN).

---

## 3. LISTA DE TAREAS

> Checklist accionable para backend, frontend y QA. El Orchestrator valida paso a paso.

### Backend

#### Implementación — BD & ORM

- [ ] Crear migration Flyway `V7__Add_superadmin_check_constraint.sql`
  - [ ] Añadir CHECK constraint en tabla `users`
  - [ ] Validar que no quedan usuarios en estado inválido (SUPER_ADMIN con ecommerce_id != NULL)
- [ ] Actualizar `UserEntity` documentacion para reflejar CHECK constraint
- [ ] Generar y verificar script DDL actualizado

#### Implementación — Servicios & Lógica

- [ ] Mejorar `UserService.createUser()` para permitir creación por SUPER_ADMIN y STORE_ADMIN
  - [ ] Si caller es SUPER_ADMIN: permite crear cualquier rol en cualquier ecommerce
  - [ ] Si caller es STORE_ADMIN: permite crear STORE_USER o STORE_ADMIN SOLO en su ecommerce
  - [ ] Si caller es STORE_USER: retorna 403 Forbidden
  - [ ] Validar `role == SUPER_ADMIN` → `ecommerceId` debe ser NULL
  - [ ] Validar `role != SUPER_ADMIN` → `ecommerceId` obligatorio y debe existir
- [ ] Mejorar `UserService.updateUser()` con autorización multi-contexto
  - [ ] Si caller es SUPER_ADMIN: permite actualizar cualquier usuario
  - [ ] Si caller es STORE_ADMIN: solo actualiza usuarios de su ecommerce_id
  - [ ] Si caller es dueño del perfil (uid==uid): permite cambiar password y username (NO ecommerce_id ni active)
  - [ ] Validar que usuario no intenta cambiar `ecommerce_id` si no es SUPER_ADMIN
  - [ ] Validar que `role` no cambia a SUPER_ADMIN (protección adicional)
  - [ ] Loguear cambios (especialmente cambio de ecommerce)
- [ ] Mejorar `UserService.deleteUser()` con autorización multi-contexto
  - [ ] Si caller es SUPER_ADMIN: permite eliminar cualquier usuario
  - [ ] Si caller es STORE_ADMIN: solo elimina usuarios de su ecommerce_id
  - [ ] Validar auto-eliminación (user no puede eliminar su propio perfil)
  - [ ] Loguear eliminación con contexto (quién eliminó a quién)
- [ ] Mejorar `UserService.listUsers(UUID ecommerceIdParam)` para contextos múltiples
  - [ ] Si caller es SUPER_ADMIN: retorna todos (o filtra por param sin restricción)
  - [ ] Si caller es STORE_ADMIN: retorna solo usuarios de su ecommerce (ignora/rechaza param si difiere)
  - [ ] Si caller es STORE_USER: retorna 403 Forbidden
- [ ] Actualizar `EcommerceService.getAll()`
  - [ ] Si caller es SUPER_ADMIN: sin filtro, retorna todos
  - [ ] Si caller es STORE_ADMIN/STORE_USER: retorna solo su ecommerce o error 403
- [ ] Mejorar `SecurityContextHelper`
  - [ ] Añadir método `isCurrentUserSuperAdmin()` basado en JWT role claim
  - [ ] Añadir método `getCurrentUserEcommerceId()` extrae ecommerce_id del JWT
  - [ ] Añadir método `canActOnUser(targetUser)` que valida permite actuar sobre Usuario X
  - [ ] Validar que funciona correctamente después de login

#### Implementación — Controllers

- [ ] Validar `UserController` endpoints respetan autorización multi-contexto
  - [ ] POST /api/v1/users: SUPER_ADMIN acceso global + STORE_ADMIN limitado a su ecommerce
  - [ ] GET /api/v1/users: SUPER_ADMIN acceso global + STORE_ADMIN limitado a su ecommerce + STORE_USER rechazado (403)
  - [ ] GET /api/v1/users/{uid}: SUPER_ADMIN acceso global + STORE_ADMIN limitado a su ecommerce + dueño del perfil acceso propio
  - [ ] PUT /api/v1/users/{uid}: SUPER_ADMIN acceso global + STORE_ADMIN limitado a su ecommerce + dueño del perfil actualiza limitado
  - [ ] DELETE /api/v1/users/{uid}: SUPER_ADMIN acceso global + STORE_ADMIN limitado a su ecommerce
- [ ] Mejorar respuestas de error con mensajes de contexto (ej: "No puede editar usuarios de otro ecommerce")
- [ ] Validar que endpoint GET /api/v1/ecommerces respeta SUPER_ADMIN context (403 para no-SUPER_ADMIN)

#### Tests Backend

- [ ] `testCreateUser_SuperAdminRole_Success` — Crear SUPER_ADMIN con ecommerce_id = NULL
- [ ] `testCreateUser_SuperAdminRole_WithEcommerceId_Fails` — Crear SUPER_ADMIN con ecommerce_id != NULL debe fallar
- [ ] `testCreateUser_StoreAdminRole_Success` — Crear STORE_ADMIN con ecommerce_id válida
- [ ] `testCreateUser_StoreAdminRole_WithoutEcommerceId_Fails` — STORE_ADMIN sin ecommerce_id debe fallar
- [ ] `testCreateUser_DuplicateUsername_Returns409` — Username duplicado retorna 409
- [ ] `testCreateUser_NonSuperAdminCaller_Returns403` — STORE_USER intenta crear → 403 Forbidden
- [ ] `testCreateUser_StoreAdminCreatesInOwnEcommerce_Success` — STORE_ADMIN crea STORE_USER en su ecommerce
- [ ] `testCreateUser_StoreAdminCreatesInForeignEcommerce_Returns403` — STORE_ADMIN intenta crear en otro ecommerce → 403
- [ ] `testListUsers_SuperAdminSeeAll_Success` — SUPER_ADMIN ve todos sin filtro
- [ ] `testListUsers_StoreAdminSeesOnly_OwnEcommerce_Success` — STORE_ADMIN ve solo su ecommerce
- [ ] `testListUsers_StoreUserCaller_Returns403` — STORE_USER intenta listar → 403 Forbidden
- [ ] `testListUsers_CrossEcommerceAccess_Returns403` — STORE_ADMIN intenta acceder otro ecommerce → 403
- [ ] `testGetUser_SelfAccess_Success` — Usuario accede a su propio perfil
- [ ] `testGetUser_SelfAccess_CrossEcommerce_Returns403` — Usuario intenta acceder perfil ajeno → 403
- [ ] `testUpdateUser_SuperAdminChangesEcommerce_Success` — SUPER_ADMIN puede cambiar ecommerce
- [ ] `testUpdateUser_StoreAdminUpdatesOwnEcommerce_Success` — STORE_ADMIN actualiza usuario de su ecommerce
- [ ] `testUpdateUser_StoreAdminUpdatesForeignEcommerce_Returns403` — STORE_ADMIN intenta actualizar otro ecommerce → 403
- [ ] `testUpdateUser_UserUpdatesOwnPassword_Success` — Usuario cambia su propia contraseña
- [ ] `testUpdateUser_UserTriesToChangeEcommerce_Returns403` — Usuario intenta cambiar su ecommerce_id → 403
- [ ] `testUpdateUser_UserTriesToChangeActive_Returns403` — Usuario intenta cambiar su active status → 403
- [ ] `testUpdateUser_InvalidEcommerce_Returns404` — Ecommerce que no existe retorna 404
- [ ] `testDeleteUser_SuperAdminDeletesAnyone_Success` — SUPER_ADMIN puede eliminar cualquiera
- [ ] `testDeleteUser_StoreAdminDeletesOwnEcommerce_Success` — STORE_ADMIN elimina usuario de su ecommerce
- [ ] `testDeleteUser_StoreAdminDeletesForeignEcommerce_Returns403` — STORE_ADMIN intenta eliminar otro ecommerce → 403
- [ ] `testDeleteUser_SelfElimination_Returns400` — Intentar eliminarse a sí mismo retorna 400
- [ ] `testDeleteUser_StoreUserCaller_Returns403` — STORE_USER intenta eliminar → 403 Forbidden
- [ ] `testCheckConstraint_SuperAdminWithoutEcommerce_Enforced` — BD enforce constraint
- [ ] `testSecurityContextHelper_IsCurrentUserSuperAdmin_Works` — Helper detecta correctamente SUPER_ADMIN role
- [ ] `testSecurityContextHelper_GetCurrentUserEcommerceId_Works` — Helper extrae ecommerce_id del JWT
- [ ] `testSecurityContextHelper_CanActOnUser_Works` — Helper valida permisos para actuar sobre usuario

### Frontend

#### Implementación — Pages & Components

- [ ] Crear página `SuperAdminDashboard` en `/superadmin`
  - [ ] Header con Welcome "Bienvenido SUPER_ADMIN"
  - [ ] Tarjetas de acciones rápidas (Create User, List Users, All Ecommerces)
  - [ ] Stats panel (# total users, # ecommerces, # SUPER_ADMIN)
- [ ] Crear componente `UserManagementTable`
  - [ ] Columnas: username, role, ecommerce, active, actions
  - [ ] Botones edit/delete por fila
  - [ ] Sorting por columna
  - [ ] Pagination (10 items por página)
- [ ] Crear componente `UserFormModal`
  - [ ] Campos: username (required), password (required en create, opcional en edit), role (dropdown), ecommerceId (dropdown, required si role != SUPER_ADMIN)
  - [ ] Validaciones en submit (username min 3 chars, password min 12 chars)
  - [ ] Loading state durante POST/PUT
- [ ] Crear componente `EcommerceListDropdown`
  - [ ] Fetch `/api/v1/ecommerces` al abrir
  - [ ] Mostrar nombre + slug de cada ecommerce
  - [ ] Soportar search/filter por nombre
- [ ] Crear página `AllEcommercesPage` en `/superadmin/ecommerces`
  - [ ] Tabla listado de todos los ecommerces
  - [ ] Columnas: name, slug, status, createdAt, actions
  - [ ] Botón para cada ecommerce que abre modal con usuarios asignados
- [ ] Crear página `UserManagementPage` en `/superadmin/users`
  - [ ] Dropdown filtro por ecommerce (con opción "Todos")
  - [ ] Tabla completa de usuarios con acciones
  - [ ] Modal crear usuario
  - [ ] Modal edit usuario (vuelve a cargar datos actuales)

#### Implementación — Hooks & Services

- [ ] Crear `services/superAdminService.ts`
  - [ ] `getAllUsers(token, filters?)` → GET /api/v1/users
  - [ ] `createUser(data, token)` → POST /api/v1/users
  - [ ] `updateUser(uid, data, token)` → PUT /api/v1/users/{uid}
  - [ ] `deleteUser(uid, token)` → DELETE /api/v1/users/{uid}
  - [ ] `getAllEcommerces(token)` → GET /api/v1/ecommerces
  - [ ] Manejo de errores (401, 403, 404, 409, 400)
- [ ] Crear hook `useSuperAdminUsers`
  - [ ] Estado: users[], loading, error
  - [ ] Acciones: listUsers(filters?), createUser(data), updateUser(uid, data), deleteUser(uid), refresh()
  - [ ] Manejo de error states
- [ ] Crear hook `useAllEcommerces`
  - [ ] Estado: ecommerces[], loading, error
  - [ ] Acciones: refresh()
  - [ ] Auto-carga al montar
- [ ] Crear hook `useUserAssignment`
  - [ ] Acción: assignUserToEcommerce(uid, ecommerceId)
  - [ ] Estado: loading, error
  - [ ] Retorna success/error

#### Implementación — Auth & Routing

- [ ] Crear helper `utils/authUtils.ts`
  - [ ] `isUserSuperAdmin()` — parsea JWT, retorna `role === 'SUPER_ADMIN'`
  - [ ] `getSuperAdminClaimsFromJWT()` — extrae claims relevantes
- [ ] Proteger ruta `/superadmin` y sub-rutas
  - [ ] Guard: si no es SUPER_ADMIN, redirige a `/dashboard` o `/login`
  - [ ] Usar helper `isUserSuperAdmin()`
- [ ] Actualizar router principal (`App.jsx` o `Router.jsx`)
  - [ ] Añadir ruta `<Route path="/superadmin" element={<SuperAdminDashboard />} />`
  - [ ] Añadir ruta `/superadmin/users` → `UserManagementPage`
  - [ ] Añadir ruta `/superadmin/ecommerces` → `AllEcommercesPage`

#### Tests Frontend

- [ ] `[SuperAdminDashboard] renders welcome message for SUPER_ADMIN`
- [ ] `[SuperAdminDashboard] shows stats (total users, ecommerces)`
- [ ] `[UserManagementTable] renders users correctly`
- [ ] `[UserManagementTable] delete button calls onDelete with correct uid`
- [ ] `[UserManagementTable] edit button opens modal`
- [ ] `[UserFormModal] submits with correct data on create`
- [ ] `[UserFormModal] submits with correct data on edit`
- [ ] `[UserFormModal] validates username min length`
- [ ] `[UserFormModal] validates password min length`
- [ ] `[UserFormModal] requires ecommerceId if role != SUPER_ADMIN`
- [ ] `[EcommerceListDropdown] loads and shows ecommerces`
- [ ] `[EcommerceListDropdown] filters on search input`
- [ ] `[AllEcommercesPage] lists all ecommerces (SUPER_ADMIN context)`
- [ ] `[AllEcommercesPage]404 if user is not SUPER_ADMIN`
- [ ] `useSuperAdminUsers` hook loads users without ecommerce filter
- [ ] `useSuperAdminUsers` hook creates user and refreshes list
- [ ] `useSuperAdminUsers` hook updates user and refreshes list
- [ ] `useSuperAdminUsers` hook deletes user and refreshes list
- [ ] `useAllEcommerces` hook loads all ecommerces on mount
- [ ] `isUserSuperAdmin()` returns true for SUPER_ADMIN role
- [ ] `isUserSuperAdmin()` returns false for STORE_ADMIN role
- [ ] `Route /superadmin redirects to /login if not authenticated`
- [ ] `Route /superadmin redirects to /dashboard if not SUPER_ADMIN`

### QA

- [ ] Ejecutar skill `/gherkin-case-generator`
  - [ ] Generar escenarios Gherkin para HU-02.1, 02.2, 02.3, 02.4, 02.5
  - [ ] Incluir datos de prueba (usuarios test, ecommerces test)
  - [ ] Salida: `docs/output/qa/superadmin-gherkin-cases.md`
- [ ] Ejecutar skill `/risk-identifier`
  - [ ] Analizar riesgos: autenticación, aislamiento multi-tenant, integridad de datos
  - [ ] Clasificación ASD (Alto/Medio/Bajo)
  - [ ] Recomendaciones de testing
- [ ] Revisar cobertura de tests
  - [ ] Backend: 14 tests funcionales + 5 tests BD constraint
  - [ ] Frontend: 24 tests componentes + 8 tests routing
  - [ ] Meta: 100% cobertura en lógica SUPER_ADMIN, 80%+ en componentes UI
- [ ] Validar reglas de negocio
  - [ ] RN-01: SUPER_ADMIN tiene ecommerce_id = NULL y acceso sin filtro
  - [ ] RN-02: Roles permitidos (SUPER_ADMIN, STORE_ADMIN, STORE_USER)
  - [ ] RN-03: Username único globalmente
  - [ ] RN-04: Contraseña fuerte (12+ chars, BCrypt)
  - [ ] RN-05: Solo SUPER_ADMIN gestiona usuarios
  - [ ] RN-06: Solo SUPER_ADMIN ve lista global ecommerces
  - [ ] RN-07: Cambio de ecommerce válido si existe
  - [ ] RN-08: Logging de auditoría en cada CRUD
- [ ] Testing manual
  - [ ] Scenario: SUPER_ADMIN crea STORE_ADMIN, accede al sistema, crea usuarios estándar
  - [ ] Scenario: SUPER_ADMIN cambia usuario de Nike a Adidas, verifica JWT actualizado
  - [ ] Scenario: SUPER_ADMIN ve 5 ecommerces en listado global; STORE_ADMIN solo ve el suyo
  - [ ] Scenario: Usuario STORE_ADMIN intenta DELETE → 403 Forbidden
  - [ ] Scenario: Intento de eliminar SUPER_ADMIN a sí mismo → 400 Bad Request
- [ ] Testing de integración
  - [ ] JWT incluye claim `role: SUPER_ADMIN` para usuarios con ese rol
  - [ ] RabbitMQ propaga eventos de creación/edición usuarios sin cambios
  - [ ] Caché de Auth (si aplica) incluye SUPER_ADMIN sin filtro ecommerce_id
- [ ] Performance
  - [ ] GET /api/v1/users sin ecommerce_id (SUPER_ADMIN) < 500ms con 10k users
  - [ ] GET /api/v1/ecommerces (SUPER_ADMIN) < 200ms con 100 ecommerces
- [ ] Seguridad
  - [ ] Contraseña NO se retorna en responses (solo uid/username/role)
  - [ ] Token JWT válida para 24h (valor por defecto en config)
  - [ ] DELETE usuario no falla silenciosamente; retorna 204 o error explícito
- [ ] Actualizar estado spec: `status: IMPLEMENTED` cuando todos los checks pasen
