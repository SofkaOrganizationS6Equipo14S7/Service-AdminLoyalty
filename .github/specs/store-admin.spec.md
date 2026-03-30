---
id: SPEC-003
status: APPROVED
feature: store-admin-management
created: 2026-03-29
updated: 2026-03-29
author: spec-generator
version: "1.0"
related-specs: ["SPEC-001", "SPEC-002"]
---

# Spec: Administración de Ecommerce por STORE_ADMIN

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de iniciar implementación.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPRECATED
> 
> **Contexto:** Implementa la Historia de Usuario HU-03 del requerimiento `.github/requirements/store-admin.md`. Extiende SPEC-001 y SPEC-002 con nuevo rol STORE_ADMIN y permisos restrictivos por ecommerce.

---

## 1. REQUERIMIENTOS

### Descripción

El STORE_ADMIN es un administrador de su ecommerce con permiso para crear, modificar y eliminar usuarios estándar dentro de su organización. A diferencia del SUPER_ADMIN (sin restricciones), el STORE_ADMIN está vinculado a exactamente un ecommerce y solo accede a datos de su propia tienda, asegurando aislamiento multi-tenant.

### Requerimiento de Negocio

```
Como:        STORE_ADMIN
Quiero:      ser administrador de mi ecommerce  
Para:        crear, modificar y eliminar usuarios estándar de mi ecommerce, gestionar reglas de descuento, 
             productos y clientes, y acceder exclusivamente a la información y métricas de mi ecommerce 
             sin poder ver datos de otros ecommerce.
```

**Notas de negocio:**
- STORE_ADMIN es propietario/gerente de su ecommerce
- Puede gestionar su propio equipo (usuarios estándar: STORE_USER)
- Acceso completamente aislado por ecommerce_id
- No puede crear otros STORE_ADMIN (requiere SUPER_ADMIN)
- Sus cambios se auditan con timestamps

### Historias de Usuario

#### HU-03.1: Crear usuario estándar por STORE_ADMIN

```
Como:        STORE_ADMIN
Quiero:      crear un usuario estándar (STORE_USER) para mi ecommerce
Para:        que mi equipo pueda acceder al sistema con credenciales únicas

Prioridad:   Alta
Estimación:  M
Dependencias: HU-03 (STORE_ADMIN debe existir), SPEC-001, SPEC-002
Capa:        Backend + Frontend
```

#### Criterios de Aceptación — HU-03.1

**Happy Path**
```gherkin
CRITERIO-3.1.1: Crear usuario estándar exitosamente
  Dado que:  existe un usuario con rol STORE_ADMIN vinculado a un ecommerce
  Cuando:    el STORE_ADMIN crea un usuario estándar con username, password y email válidos
  Entonces:  el sistema crea el usuario y lo vincula al mismo ecommerce del STORE_ADMIN
  Y:        retorna 201 Created con UserResponse (uid, username, email, role=STORE_USER, ecommerce_id, created_at)
```

**Error Paths**
```gherkin
CRITERIO-3.1.2: Intentar crear usuario con username duplicado globalmente
  Dado que:  el STORE_ADMIN intenta crear usuario con username que ya existe en otra tienda
  Cuando:    envía POST /api/v1/users con username duplicado
  Entonces:  el sistema retorna 409 Conflict con mensaje "Username ya existe en el sistema"

CRITERIO-3.1.3: Intentar crear usuario sin autenticación
  Dado que:  no existe token JWT válido
  Cuando:    envía POST /api/v1/users sin header Authorization
  Entonces:  el sistema retorna 401 Unauthorized

CRITERIO-3.1.4: Intentar crear usuario con email inválido
  Dado que:  el STORE_ADMIN intenta crear usuario con email malformado
  Cuando:    envía POST /api/v1/users con email="invalid"
  Entonces:  el sistema retorna 400 Bad Request con mensaje "Email inválido"

CRITERIO-3.1.5: Intentar crear usuario en ecommerce que no existe
  Dado que:  la solicitud de creación contiene ecommerce_id no válido
  Cuando:    envía POST /api/v1/users con ecommerce_id que no existe en BD
  Entonces:  el sistema retorna 400 Bad Request con mensaje "Ecommerce no encontrado"
```

**Edge Cases**
```gherkin
CRITERIO-3.1.6: Validar que usuario solo se crea en ecommerce del STORE_ADMIN
  Dado que:  STORE_ADMIN está vinculado a ecommerce_id=uuid-123
  Cuando:    intenta crear usuario especificando ecommerce_id=uuid-456 (otro ecommerce)
  Entonces:  el sistema rechaza la solicitud con 403 Forbidden "No puede crear usuarios fuera de su ecommerce"
```

---

#### HU-03.2: Listar usuarios por ecommerce del STORE_ADMIN

```
Como:        STORE_ADMIN
Quiero:      consultar la lista de usuarios de mi ecommerce
Para:        ver el equipo actual, buscar usuarios, auditar accesos

Prioridad:   Alta
Estimación:  S
Dependencias: HU-03.1
Capa:        Backend + Frontend
```

#### Criterios de Aceptación — HU-03.2

**Happy Path**
```gherkin
CRITERIO-3.2.1: Listar usuarios exitosamente con filtros
  Dado que:  STORE_ADMIN está autenticado y vinculado a ecommerce_id=uuid-123
  Cuando:    envía GET /api/v1/users
  Entonces:  el sistema retorna 200 OK con lista de usuarios SOLO del ecommerce_id=uuid-123
  Y:        cada usuario muestra: uid, username, email, role=STORE_USER, created_at, updated_at
  Y:        la lista NO contiene usuarios de otros ecommerce
```

```gherkin
CRITERIO-3.2.2: Listar usuarios con búsqueda por username
  Dado que:  STORE_ADMIN busca usuarios
  Cuando:    envía GET /api/v1/users?search=john
  Entonces:  el sistema retorna 200 OK con usuarios cuyo username contiene "john" en su ecommerce
  Y:        la búsqueda es case-insensitive
```

**Error Paths**
```gherkin
CRITERIO-3.2.3: Intentar listar usuarios de otro ecommerce
  Dado que:  STORE_ADMIN está vinculado a ecommerce_id=uuid-123
  Cuando:    intenta enviar GET /api/v1/users?ecommerceId=uuid-456
  Entonces:  el sistema rechaza con 403 Forbidden "No puede listar usuarios de otro ecommerce"
```

---

#### HU-03.3: Actualizar datos de usuario estándar

```
Como:        STORE_ADMIN
Quiero:      modificar el email o nombre de un usuario
Para:        mantener datos actualizados sin eliminar el usuario

Prioridad:   Media
Estimación:  S
Dependencias: HU-03.1, HU-03.2
Capa:        Backend + Frontend
```

#### Criterios de Aceptación — HU-03.3

**Happy Path**
```gherkin
CRITERIO-3.3.1: Actualizar email y nombre de usuario exitosamente
  Dado que:  existe un usuario STORE_USER en ecommerce_id=uuid-123
  Cuando:    STORE_ADMIN envía PUT /api/v1/users/{uid} con campos actualizados (email, nombre)
  Entonces:  el sistema actualiza los campos
  Y:        retorna 200 OK con UserResponse con datos nuevos
  Y:        timestamps updated_at se actualiza automáticamente
  Y:        el rol y ecommerce_id NO se modifican
```

**Error Paths**
```gherkin
CRITERIO-3.3.2: Intentar actualizar usuario de otro ecommerce
  Dado que:  STORE_ADMIN intenta modificar usuario de otro ecommerce
  Cuando:    envía PUT /api/v1/users/{uid-de-otro-ecommerce}
  Entonces:  el sistema rechaza con 403 Forbidden "No puede editar usuarios de otro ecommerce"

CRITERIO-3.3.3: Intentar cambiar el rol del usuario
  Dado que:  STORE_ADMIN intenta cambiar role de STORE_USER a SUPER_ADMIN
  Cuando:    envía PUT /api/v1/users/{uid} con role="SUPER_ADMIN"
  Entonces:  el sistema rechaza con 400 Bad Request "No puede cambiar el rol de usuarios"
```

---

#### HU-03.4: Eliminar usuario estándar

```
Como:        STORE_ADMIN
Quiero:      eliminar un usuario de mi ecommerce
Para:        revocar acceso cuando el usuario abandona el equipo

Prioridad:   Media
Estimación:  S
Dependencias: HU-03.1, HU-03.2
Capa:        Backend + Frontend
```

#### Criterios de Aceptación — HU-03.4

**Happy Path**
```gherkin
CRITERIO-3.4.1: Eliminar usuario exitosamente
  Dado que:  existe un usuario STORE_USER en ecommerce_id=uuid-123
  Cuando:    STORE_ADMIN envía DELETE /api/v1/users/{uid}
  Entonces:  el sistema elimina el usuario de forma permanente
  Y:        retorna 204 No Content
  Y:        el usuario ya no puede autenticarse
```

**Error Paths**
```gherkin
CRITERIO-3.4.2: Intentar eliminar usuario de otro ecommerce
  Dado que:  STORE_ADMIN intenta eliminar usuario fuera de su ecommerce
  Cuando:    envía DELETE /api/v1/users/{uid-de-otro-ecommerce}
  Entonces:  el sistema rechaza con 403 Forbidden "No puede eliminar usuarios de otro ecommerce"

CRITERIO-3.4.3: Validar que STORE_ADMIN no se elimine a sí mismo
  Dado que:  STORE_ADMIN intenta auto-eliminarse
  Cuando:    envía DELETE /api/v1/users/{su-propio-uid}
  Entonces:  el sistema rechaza con 400 Bad Request "No puede eliminarse a sí mismo"
```

---

### Reglas de Negocio

1. **RN-01: Exclusividad de rol STORE_ADMIN**
   - STORE_ADMIN está vinculado a EXACTAMENTE un ecommerce
   - ecommerce_id NOT NULL en campo de usuario con role='STORE_ADMIN'
   
2. **RN-02: Aislamiento de datos**
   - Consultas en BD siempre filtran por ecommerce_id del contexto de seguridad
   - STORE_ADMIN NUNCA ve datos de otro ecommerce (ni mediante API ni BD queries)

3. **RN-03: Unicidad de username GLOBAL**
   - Username es único GLOBALMENTE (no por ecommerce)
   - **Justificación:** Mantiene consistencia con SPEC-002 y simplifica autenticación
   - El endpoint `/api/v1/auth/login` usa username (sin necesidad de slug de tienda) para identificar al usuario
   - Dos ecommerce NO pueden tener usuario con mismo username

4. **RN-04: Validación de email**
   - Email es obligatorio y debe cumplir RFC 5322 básico (formato válido)
   - Email es único GLOBALMENTE (consistente con username para evitar colisiones de identidad)

5. **RN-05: Permisos de usuario**
   - STORE_ADMIN puede CREATE/READ/UPDATE/DELETE usuarios STORE_USER en su ecommerce
   - STORE_ADMIN NUNCA puede crear otro STORE_ADMIN (solo SUPER_ADMIN)
   - STORE_ADMIN NUNCA puede cambiar role de STORE_USER a STORE_ADMIN

6. **RN-06: Auditoría**
   - Todos los usuarios tienen timestamps: created_at (no editable), updated_at (auto-actualizado en cada cambio)
   - created_at registra quién creó el usuario (via SecurityContextHelper)

7. **RN-07: Validación de contraseña**
   - Password mínimo 12 caracteres (requisito de seguridad)
   - Password se hashea con BCrypt antes de guardar (nunca texto plano)

---

## 2. DISEÑO

### Modelos de Datos

#### Entidades afectadas

| Entidad | Almacén | Cambios | Descripción |
|---------|---------|---------|-------------|
| `UserEntity` | tabla `users` | EXTENSIÓN | Nuevo rol `STORE_ADMIN` + campo `email` obligatorio |
| `EcommerceEntity` | tabla `ecommerces` | No cambia | Mantiene estructura actual |

#### Cambios en UserEntity

| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `uid` | UUID | sí | auto-generado | Identificador único global |
| `username` | string | sí | 3-50 chars, UNIQUE GLOBAL | Único globalmente (para Login simple) |
| `password` | string | sí | BCrypt hash, min 12 original | Contraseña hasheada |
| `email` | string | sí | RFC 5322, UNIQUE GLOBAL | **NUEVO:** Email único globalmente |
| `role` | string | sí | enum: SUPER_ADMIN, STORE_ADMIN, STORE_USER | Rol del usuario |
| `ecommerce_id` | UUID | condicional | NOT NULL si role IN ('STORE_ADMIN', 'STORE_USER') | Vínculo a ecommerce |
| `active` | boolean | sí | default true | Flag de estado |
| `created_at` | datetime (UTC) | sí | auto-generado | Timestamp creación |
| `updated_at` | datetime (UTC) | sí | auto-generado | Timestamp actualización |

#### CHECK Constraints

```sql
-- Validación: SUPER_ADMIN sin ecommerce, STORE_ADMIN y STORE_USER siempre con ecommerce
CHECK (
  (role = 'SUPER_ADMIN' AND ecommerce_id IS NULL) OR
  (role IN ('STORE_ADMIN', 'STORE_USER') AND ecommerce_id IS NOT NULL)
)

-- Unicidad de username GLOBAL (no por ecommerce)
UNIQUE (username)

-- Unicidad de email GLOBAL (no por ecommerce)
UNIQUE (email)
```

#### Índices / Constraints

| Índice | Columnas | Propósito |
|--------|----------|----------|
| `idx_username` | `username` (UNIQUE) | Búsqueda y validación unicidad global |
| `idx_email` | `email` (UNIQUE) | Búsqueda y validación unicidad global |
| `idx_ecommerce_id` | `ecommerce_id` | Filtrado por ecommerce (queries frecuentes) |
| `idx_role` | `role` | Búsqueda de usuarios por rol |
| `idx_active_ecommerce` | `active, ecommerce_id` | Filtrado de usuarios activos por ecommerce |

---

### API Endpoints

#### POST /api/v1/users
- **Descripción**: Crea un nuevo usuario estándar (STORE_USER) en el ecommerce del STORE_ADMIN
- **Auth requerida**: sí (rol STORE_ADMIN)
- **Request Body**:
  ```json
  {
    "username": "string (3-50 chars, único en ecommerce)",
    "email": "string (RFC 5322, único en ecommerce)",
    "password": "string (mín 12 chars)",
    "role": "STORE_USER"
  }
  ```
- **Response 201 Created**:
  ```json
  {
    "uid": "uuid",
    "username": "string",
    "email": "string",
    "role": "STORE_USER",
    "ecommerce_id": "uuid",
    "active": true,
    "created_at": "2026-03-29T10:30:00Z",
    "updated_at": "2026-03-29T10:30:00Z"
  }
  ```
- **Response 400 Bad Request**: Email inválido, password débil, role != STORE_USER, ecommerce no existe, validación fallida
- **Response 401 Unauthorized**: Token ausente o expirado, no autenticado
- **Response 403 Forbidden**: No es STORE_ADMIN (TenantInterceptor rechaza si intenta crear en otro ecommerce)
- **Response 409 Conflict**: Username o email duplicado globalmente (colisión en el sistema)

#### GET /api/v1/users
- **Descripción**: Lista usuarios del ecommerce del STORE_ADMIN (solo su propio ecommerce)
- **Auth requerida**: sí (rol STORE_ADMIN)
- **Query Parameters**:
  - `search` (opcional): string para búsqueda en username/email (case-insensitive)
  - `active` (opcional): boolean para filtrar por estado
  - `role` (opcional): filtrar por rol (debe ser STORE_USER para STORE_ADMIN)
- **Response 200 OK**:
  ```json
  [
    {
      "uid": "uuid",
      "username": "string",
      "email": "string",
      "role": "STORE_USER",
      "ecommerce_id": "uuid",
      "active": true,
      "created_at": "2026-03-29T10:30:00Z",
      "updated_at": "2026-03-29T10:30:00Z"
    }
  ]
  ```
- **Response 401 Unauthorized**: sin token
- **Response 403 Forbidden**: no es STORE_ADMIN

#### GET /api/v1/users/{uid}
- **Descripción**: Obtiene detalles de un usuario específico
- **Auth requerida**: sí (rol STORE_ADMIN)
- **Validación**: usuario DEBE pertenecer al ecommerce del STORE_ADMIN
- **Response 200 OK**: UserResponse completo
- **Response 401 Unauthorized**: sin token
- **Response 403 Forbidden**: usuario pertenece a otro ecommerce
- **Response 404 Not Found**: usuario no existe

#### PUT /api/v1/users/{uid}
- **Descripción**: Actualiza email y/o nombre de un usuario (NO role, NO ecommerce_id)
- **Auth requerida**: sí (rol STORE_ADMIN)
- **Validación**: TenantInterceptor valida que usuario DEBE pertenecer al ecommerce del STORE_ADMIN (403 si no)
- **Request Body** (campos opcionales):
  ```json
  {
    "email": "string (opcional, único globalmente)",
    "username": "string (opcional, único globalmente)"
  }
  ```
- **Response 200 OK**: UserResponse actualizado
- **Response 400 Bad Request**: email inválido, username/email duplicado globalmente, intento de cambiar role
- **Response 401 Unauthorized**: sin token
- **Response 403 Forbidden**: usuario pertenece a otro ecommerce (TenantInterceptor)
- **Response 404 Not Found**: usuario no existe

#### DELETE /api/v1/users/{uid}
- **Descripción**: Elimina un usuario permanentemente
- **Auth requerida**: sí (rol STORE_ADMIN)
- **Validación**: 
  - TenantInterceptor valida que usuario DEBE pertenecer al ecommerce del STORE_ADMIN (403 si no)
  - STORE_ADMIN NO puede eliminarse a sí mismo
- **Response 204 No Content**: eliminación exitosa
- **Response 400 Bad Request**: intento de auto-eliminación
- **Response 401 Unauthorized**: sin token
- **Response 403 Forbidden**: usuario de otro ecommerce (TenantInterceptor)
- **Response 404 Not Found**: usuario no existe

---

### Diseño Frontend

#### Componentes nuevos

| Componente | Archivo | Props principales | Descripción |
|------------|---------|------------------|-------------|
| `UserCard` | `components/UserCard.jsx` | `user, onEdit, onDelete, isCurrentUser` | Tarjeta individual de usuario |
| `UserFormModal` | `components/UserFormModal.jsx` | `isOpen, onSubmit, onClose, initialData, mode='create'\|'edit'` | Modal crear/editar usuario |
| `UsersList` | `components/UsersList.jsx` | `users, loading, onEdit, onDelete, searchQuery` | Lista completa con búsqueda |

#### Páginas nuevas

| Página | Archivo | Ruta | Protegida | Acceso |
|--------|---------|------|-----------|--------|
| `UsersPage` | `pages/UsersPage.jsx` | `/admin/users` | sí | STORE_ADMIN |

#### Hooks y State

| Hook | Archivo | Retorna | Descripción |
|------|---------|---------|-------------|
| `useUsers` | `hooks/useUsers.js` | `{ users, loading, error, create, update, remove, search }` | CRUD + búsqueda de usuarios |

#### Services (llamadas API)

| Función | Archivo | Endpoint | HTTP |
|---------|---------|---------|------|
| `listUsers(token, search?, active?, role?)` | `services/userService.js` | `GET /api/v1/users` | GET |
| `createUser(data, token)` | `services/userService.js` | `POST /api/v1/users` | POST |
| `updateUser(uid, data, token)` | `services/userService.js` | `PUT /api/v1/users/{uid}` | PUT |
| `deleteUser(uid, token)` | `services/userService.js` | `DELETE /api/v1/users/{uid}` | DELETE |
| `getUser(uid, token)` | `services/userService.js` | `GET /api/v1/users/{uid}` | GET |

#### Flujo de Componentes

```
UsersPage (main container)
├── UsersList (lista + búsqueda)
│   └── UserCard[] (items individuales)
│       ├── [Edit Button] → abre UserFormModal
│       └── [Delete Button] → confirmación → API delete
├── UserFormModal (crear/editar)
│   └── form con validación
├── useUsers hook
└── userService (API calls)
```

#### Validaciones Frontend

- **Email**: validación RFC 5322 básica (`/^[^\s@]+@[^\s@]+\.[^\s@]+$/`), único globalmente
- **Username**: 3-50 caracteres, alfanuméricos + guiones/puntos, único globalmente
- **Password**: mínimo 12 caracteres, indicador de fortaleza
- **Required fields**: username, email, password (create), email/username (update)
- **Error handling**: Capturar y mostrar 409 Conflict cuando username/email duplicado globalmente

---

### Migración Base de Datos

#### V7__Add_email_and_update_users_table.sql

```sql
-- Agregar columna email a usuarios (UNIQUE GLOBAL para mantener consistencia con username)
ALTER TABLE users ADD COLUMN email VARCHAR(255) NOT NULL UNIQUE;

-- Crear índices globales para búsqueda rápida
CREATE UNIQUE INDEX idx_users_username ON users(username);
CREATE UNIQUE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_ecommerce_id ON users(ecommerce_id);

-- Check constraint: validar roles y ecommerce_id
ALTER TABLE users ADD CONSTRAINT ck_users_role_ecommerce
  CHECK (
    (role = 'SUPER_ADMIN' AND ecommerce_id IS NULL) OR
    (role IN ('STORE_ADMIN', 'STORE_USER') AND ecommerce_id IS NOT NULL)
  );
```

---

### Arquitectura y Dependencias

- **Paquetes nuevos**: Ninguno (reutilizar jakarta.validation, Spring Security existentes)
- **Servicios externos**: Ninguno
- **Cambios en arquitectura**: NONE — mantiene Clean Architecture (domain → application → infrastructure → presentation)
- **Impacto en punto de entrada**: Registrar SecurityContextHelper en SecurityConfiguration si no está

---

### Notas de Implementación

> **Autorización multi-tenant (CRÍTICO):**
> - `SecurityContextHelper.getCurrentUserEcommerceId()` debe retornar ecommerce_id del JWT claim
> - `TenantInterceptor` DEBE validar autorización en TODAS las operaciones (GET, POST, PUT, DELETE), no solo filtrar
>   - En GET: retorna solo recursos del ecommerce_id del usuario
>   - En POST: valida que el ecommerce_id del recurso sea igual al del usuario
>   - En PUT/DELETE: valida que el recurso destino pertenezca al ecommerce_id del usuario (ANTES de permitir modificación)
>   - **Riesgo evitado:** Un STORE_ADMIN de Nike intentando DELETE a un usuario de Adidas (conociendo su UUID) será rechazado con 403 Forbidden
>
> **Cambios en UniConstraints:**
> - Username y email son únicos GLOBALMENTE (no por ecommerce) para mantener consistencia con SPEC-002 y simplificar Login
> - **Migración requerida:** V7__Add_email_and_update_users_table.sql (actualizar índices como UNIQUE globales, no compuestos por ecommerce_id)
>
> **Validación de email:**
> - Usar `@Email` de jakarta.validation
> - Frontend: validación RFC 5322 básica
>
> **Permisos:**
> - Solo STORE_ADMIN con role='STORE_ADMIN' puede acceder a this endpoints
> - Todas las operaciones son aisladas por ecommerce_id del usuario actual
>
> **Passwords:**
> - BCryptPasswordEncoder con salt = 12 (ya implementado en SecurityConfiguration)
> - Min 12 caracteres (política de seguridad del cliente)

---

## 3. LISTA DE TAREAS

> Checklist accionable para todos los agentes. Marcar cada ítem (`[x]`) al completarlo.
> El Orchestrator monitorea este checklist para determinar el progreso.

### Backend

#### Migración Base de Datos
- [ ] Crear `V7__Add_email_and_update_users_table.sql`
  - Agregar columna `email` (NOT NULL, UNIQUE GLOBAL)
  - Crear índices UNIQUE globales: `idx_users_username`, `idx_users_email`
  - Crear índice compuesto para filtrado eficiente: `idx_users_ecommerce_id`
  - Agregar CHECK constraint para validar role + ecommerce_id
- [ ] Ejecutar migración e validar esquema en PostgreSQL

#### Actualización de Modelos
- [ ] Actualizar `UserEntity` — agregar campo `email` con @Column validations
- [ ] Actualizar `UserCreateRequest` — agregar campo `email` con @Email
- [ ] Actualizar `UserUpdateRequest` — agregar campo `email` (opcional)
- [ ] Actualizar `UserResponse` — exponer campo `email`

#### Implementación de Lógica
- [ ] Implementar `UserRepository.findByEcommerceIdAndUsername()` — query por ecommerce (username global, validado en DB)
- [ ] Implementar `UserRepository.findByEcommerceIdAndEmail()` — query por ecommerce (email global, validado en DB)
- [ ] Implementar `UserRepository.findByEcommerceIdAndUsernameLike()` — búsqueda case-insensitive by username dentro del ecommerce
- [ ] Actualizar `UserService.createUser()` — validar email, username único globalmente, role=STORE_USER, autorización STORE_ADMIN
- [ ] Actualizar `UserService.listUsers()` — agregar filtro search (username + email), role filter, TenantInterceptor valida ecommerce_id
- [ ] Actualizar `UserService.updateUser()` — validar email/username no duplicados globalmente, prevenir cambios role, TenantInterceptor valida pertenencia
- [ ] Actualizar `UserService.deleteUser()` — agregar validación auto-eliminación, TenantInterceptor valida pertenencia
- [ ] Implementar `SecurityContextHelper.getCurrentUserRole()` si no existe — retornar role del JWT
- [ ] **CRÍTICO:** Implementar/validar `TenantInterceptor` — DEBE validar autorización en TODAS las operaciones (GET, POST, PUT, DELETE) rechazando con 403 si ecommerce_id no coincide

#### Tests Backend
- [ ] `test_createUser_success_with_email` — happy path con email válido
- [ ] `test_createUser_duplicate_email_globally_conflict` — 409 Conflict email duplicado globalmente
- [ ] `test_createUser_duplicate_username_globally_conflict` — 409 Conflict username duplicado globalmente
- [ ] `test_createUser_invalid_email_bad_request` — 400 email malformado
- [ ] `test_createUser_role_not_user_bad_request` — 400 si role != STORE_USER
- [ ] `test_createUser_forbidden_different_ecommerce` — 403 TenantInterceptor rechaza crear en otro ecommerce
- [ ] `test_createUser_unauthorized_no_token` — 401 sin autenticación
- [ ] `test_listUsers_success_filtered_by_ecommerce` — 200 OK solo usuarios del ecommerce
- [ ] `test_listUsers_search_by_username` — búsqueda case-insensitive
- [ ] `test_listUsers_forbidden_different_ecommerce` — 403 TenantInterceptor rechaza listar otro ecommerce
- [ ] `test_getUser_forbidden_different_ecommerce` — 403 TenantInterceptor rechaza GET de usuario ajeno
- [ ] `test_updateUser_email_success` — 200 actualizar email
- [ ] `test_updateUser_duplicate_email_globally_conflict` — 409 email duplicado globalmente
- [ ] `test_updateUser_role_not_allowed` — 400 si intenta cambiar role
- [ ] `test_updateUser_forbidden_different_ecommerce` — 403 TenantInterceptor rechaza PUT de usuario ajeno
- [ ] `test_deleteUser_success` — 204 eliminar exitosamente
- [ ] `test_deleteUser_self_elimination_bad_request` — 400 intento auto-eliminarse
- [ ] `test_deleteUser_forbidden_different_ecommerce` — 403 TenantInterceptor rechaza DELETE de usuario ajeno

### Frontend

#### Implementación
- [ ] Crear `services/userService.js` — funciones `listUsers`, `createUser`, `updateUser`, `deleteUser`
- [ ] Crear `hooks/useUsers.js` — hook con estado, CRUD actions y búsqueda
- [ ] Crear `components/UserCard.jsx` + `UserCard.module.css` — tarjeta individual
- [ ] Crear `components/UsersList.jsx` + `UsersList.module.css` — lista con búsqueda
- [ ] Crear `components/UserFormModal.jsx` + `UserFormModal.module.css` — modal crear/editar
- [ ] Crear `pages/UsersPage.jsx` + `UsersPage.module.css` — página principal
- [ ] Registrar ruta `/admin/users` en `src/App.jsx` con ProtectedRoute
- [ ] Implementar validación de email (RFC 5322 básico)
- [ ] Implementar validación de password (min 12 chars)
- [ ] Agregar confirmación antes de eliminar usuario
- [ ] Implementar loading/error states en componentes

#### Tests Frontend
- [ ] `[UserCard] renders user name correctly`
- [ ] `[UserCard] calls onEdit when edit button clicked`
- [ ] `[UserCard] calls onDelete when delete button clicked`
- [ ] `[UserFormModal] submits form with correct data in create mode`
- [ ] `[UserFormModal] validates email format`
- [ ] `[UserFormModal] validates password min 12 chars`
- [ ] `[UserFormModal] shows validation errors`
- [ ] `useUsers() loads items on mount`
- [ ] `useUsers.create() calls API and updates state`
- [ ] `useUsers.update() calls API and updates state`
- [ ] `useUsers.remove() calls API and updates state`
- [ ] `useUsers search filters by username`
- [ ] `[UsersPage] renders list of users`
- [ ] `[UsersPage] opens create modal on button click`
- [ ] `[UsersPage] handles API errors gracefully`

### QA
- [ ] Ejecutar skill `/gherkin-case-generator` → criterios CRITERIO-3.1.1 a 3.4.3
- [ ] Ejecutar skill `/risk-identifier` → clasificación ASD de riesgos
- [ ] Revisar cobertura de tests contra criterios de aceptación
- [ ] **CRÍTICO:** Validar aislamiento multi-tenant EN TODAS LAS OPERACIONES (GET, POST, PUT, DELETE)
  - [ ] TenantInterceptor rechaza con 403 al intentar acceder usuario de otro ecommerce
  - [ ] STORE_ADMIN de Nike NO puede ver/editar/borrar usuario de Adidas incluso conociendo UUID
- [ ] Validar que STORE_ADMIN NO puede crear otro STORE_ADMIN
- [ ] Validar que email y username son únicos GLOBALMENTE (no por ecommerce)
- [ ] Validar que Login sigue siendo simple (POST /api/v1/auth/login con username sin slug de tienda)
- [ ] Actualizar estado spec: `status: IMPLEMENTED` cuando todas las tareas están ✅

---

## 4. ANÁLISIS DE IMPACTO

### Cambios de Base de Datos
- **Migración V7**: Agregar columna `email`, índices compuestos, constrainsts CHECK
- **Compatibilidad**: PostgreSQL 12+ (sintaxis estándar)
- **Backward Compatibility**: NO rompe esquema actual (solo extensión)

### Cambios de Seguridad
- **Nuevos requisitos**: email válido, password 12+ caracteres
- **Multi-tenant**: aislamiento por ecommerce_id en TODAS las queries
- **Autorización**: solo STORE_ADMIN puede gestionar usuarios de su ecommerce

### Cambios de API
- Endpoints existentes en `/api/v1/users` se extienden con nuevo rol STORE_ADMIN
- Request/Response DTOs se extienden con campo `email`
- Códigos HTTP nuevos: 403 Forbidden para aislamiento multi-tenant

### Performance
- **Índices nuevos**: `(username)` UNIQUE, `(email)` UNIQUE, `(ecommerce_id)` para filtrado — búsquedas rápidas
- **Queries afectadas**: listUsers, búsqueda — filtrando por ecommerce_id (índice disponible), validación unicidad global en O(1)
- **Expected**: < 100ms para listado de 1000 usuarios (con índices)

### Riesgos Identificados
- **ALTO**: Fallo en aislamiento multi-tenant (TenantInterceptor no valida en DELETE/PUT) → STORE_ADMIN borra usuario de otro ecommerce
  - **Mitigación**: TenantInterceptor valida TODAS las operaciones (GET, POST, PUT, DELETE) con 403 Forbidden, tests de autorización en cada endpoint
  
- **MEDIO**: Username/email duplicado globalmente entre ecommerces → colisión en autenticación
  - **Mitigación**: índices UNIQUE globales, validations en UserRepository con CHECK constraints DB
  
- **BAJO**: Email format validation — diferentes RFC standards
  - **Mitigación**: jakarta.validation @Email + regex básico en frontend

---

