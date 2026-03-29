---
id: SPEC-002
status: APPROVED
feature: ecommerce-users
created: 2026-03-27
updated: 2026-03-27
author: spec-generator
version: "2.0"
related-specs: []
---

# Spec: Gestión de Usuarios por Ecommerce

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de iniciar implementación.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPRECATED
> **Versión 2.0:** Simplificación a 2 roles (SUPER_ADMIN, USER) con aislamiento multi-tenant centralizado

---

## 1. REQUERIMIENTOS

### Descripción
Implementar gestión de usuarios vinculados a ecommerce en el sistema LOYALTY con **poder centralizado**. Solo SUPER_ADMIN puede crear, actualizar y eliminar usuarios. Usuarios con rol USER operan sus dashboards con aislamiento mandatorio: viven en una "burbuja" donde solo ven datos de su ecommerce asignado. El sistema filtra automáticamente todas las consultas mediante un TenantInterceptor basado en `ecommerce_id` del JWT.

### Requerimiento de Negocio
**Escenario 1 (HU-01)**: Como SUPER_ADMIN, quiero crear usuarios vinculados a un ecommerce, para garantizar que cada cliente gestione únicamente sus propias reglas de descuento sin afectar a otros ecommerce.

**Escenario 2 (HU-02)**: Como USER (vinculado a un ecommerce), quiero que el sistema me permita acceder solo a datos de mi tienda, para garantizar aislamiento de datos entre ecommerce.

### Historias de Usuario

#### HU-01: Creación de perfil asociado a un ecommerce específico (SUPER_ADMIN)

```
Como:        SUPER_ADMIN del sistema
Quiero:      Crear usuarios con rol USER vinculados a un ecommerce específico
Para:        Dar de alta clientes en el sistema LOYALTY con acceso aislado a su tienda

Prioridad:   Alta
Estimación:  M
Dependencias: Ninguna
Capa:        Backend + Frontend
```

#### Criterios de Aceptación — HU-01

**Happy Path**
```gherkin
CRITERIO-1.1: Creación de perfil asociado a un ecommerce específico
  Dado que:  Existe un usuario con rol SUPER_ADMIN autenticado
  Y         Un ecommerce contrató los servicios de LOYALTY
  Cuando:    El SUPER_ADMIN crea un perfil de usuario asociado a ese ecommerce
  Entonces:  El perfil queda vinculado exclusivamente a dicho ecommerce
  Y         El sistema retorna 201 Created con los datos del usuario
```

**Error Path**
```gherkin
CRITERIO-1.2: Validar que ecommerce existe
  Dado que:  Un SUPER_ADMIN intenta crear un usuario
  Cuando:    Proporciona un ecommerce_id que no existe en el sistema
  Entonces:  El sistema retorna 400 Bad Request
  Y         El mensaje indica: "El ecommerce especificado no existe"

CRITERIO-1.3: Validar que username es único globalmente
  Dado que:  Ya existe un usuario con username "tienda1-admin"
  Cuando:    Un SUPER_ADMIN intenta crear otro usuario con el mismo username
  Entonces:  El sistema retorna 409 Conflict
  Y         El mensaje indica: "El username ya existe globalmente en el sistema"

CRITERIO-1.4: Solo SUPER_ADMIN puede crear usuarios
  Dado que:  Un usuario con rol USER intenta crear otro usuario
  Cuando:    Ejecuta POST /api/v1/users
  Entonces:  El sistema retorna 403 Forbidden
  Y         El mensaje indica: "Solo SUPER_ADMIN puede crear usuarios"
```

---

#### HU-02: Validar que el usuario solo accede a su ecommerce

```
Como:        Usuario con rol USER
Quiero:      Que el sistema me aisle en una "burbuja" de mi ecommerce
Para:        Garantizar que no puedo visualizar ni gestionar información de otras tiendas

Prioridad:   Alta
Estimación:  M
Dependencias: HU-01
Capa:        Backend (TenantInterceptor)
```

#### Criterios de Aceptación — HU-02

**Happy Path**
```gherkin
CRITERIO-2.1: Usuario inicia sesión y obtiene token con ecommerce_id
  Dado que:  Existe un usuario "user1" con rol USER vinculado a ecommerce "4a1c8d4f-..."
  Cuando:    El usuario inicia sesión con credenciales válidas
  Entonces:  El sistema retorna token JWT
  Y         El token contiene claim "ecommerce_id": "4a1c8d4f-..."
  Y         Todas las consultas posteriores se filtran automáticamente por ese ecommerce_id

CRITERIO-2.2: TenantInterceptor filtra automáticamente consultas por ecommerce_id
  Dado que:  Un usuario USER con ecommerce_id="store1-uuid" está autenticado
  Cuando:    Ejecuta cualquier request (GET /api/v1/users, GET /api/v1/discounts, etc.)
  Entonces:  El TenantInterceptor extrae ecommerce_id del JWT
  Y         El interceptor añade automáticamente WHERE ecommerce_id = "store1-uuid" a cualquier consulta
  Y         El usuario solo ve datos de su ecommerce, sin excepciones
```

**Error Path**
```gherkin
CRITERIO-2.3: Usuario intenta acceder a usuario de otro ecommerce
  Dado que:  Usuario1 (USER) está vinculado a ecommerce A
  Y         Usuario2 (USER) está vinculado a ecommerce B
  Y         Usuario1 está autenticado
  Cuando:    Usuario1 intenta obtener datos de Usuario2 (GET /api/v1/users/{uid})
  Entonces:  El TenantInterceptor detecta la violación
  Y         El sistema retorna 403 Forbidden
  Y         El mensaje indica: "No tienes permisos para acceder a este recurso"

CRITERIO-2.4: SUPER_ADMIN sin restricción de ecommerce_id
  Dado que:  Un SUPER_ADMIN inicia sesión
  Entonces:  El JWT NO contiene ecommerce_id (NULL en BD)
  Y         El TenantInterceptor NO aplica filtro a sus consultas
  Y         El SUPER_ADMIN puede acceder a datos de cualquier ecommerce
```

---

#### HU-03: Crear usuario para ecommerce

```
Como:        Administrador del sistema (SUPER_ADMIN)
Quiero:      Crear un nuevo usuario asociado a un ecommerce
Para:        Dar de alta operadores en tiendas específicas

Prioridad:   Alta
Estimación:  S
Dependencias: HU-01
Capa:        Backend + Frontend
```

#### Criterios de Aceptación — HU-03

**Happy Path**
```gherkin
CRITERIO-3.1: Crear usuario para ecommerce
  Dado que:  Soy un SUPER_ADMIN con acceso al sistema
  Y         Existe un ecommerce registrado
  Cuando:    Creo un nuevo usuario asociado a ese ecommerce
  Entonces:  El sistema genera las credenciales del usuario
  Y         El usuario queda vinculado al ecommerce especificado
  Y         El sistema retorna 201 Created
```

---

#### HU-04: Listar usuarios por ecommerce

```
Como:        Usuario del sistema
Quiero:      Consultar la lista de usuarios de mi ecommerce
Para:        Ver quién más tiene acceso

Prioridad:   Alta
Estimación:  S
Dependencias: HU-01
Capa:        Backend + Frontend
```

#### Criterios de Aceptación — HU-04

**Happy Path**
```gherkin
CRITERIO-4.1: Listar usuarios por ecommerce
  Dado que:  Soy un SUPER_ADMIN o USER con acceso al sistema
  Y         Existen usuarios asociados a un ecommerce
  Cuando:    Consulto los usuarios de ese ecommerce
  Entonces:  El sistema muestra la lista de usuarios vinculados
  Y         Cada usuario muestra: username, rol, email, fecha de creación
  Y         Si soy USER: solo veo usuarios de mi ecommerce (filtrado automático por TenantInterceptor)
  Y         Si soy SUPER_ADMIN: veo todos o puedo filtrar por ecommerce mediante query param

CRITERIO-4.2: Ecommerce sin usuarios retorna lista vacía
  Dado que:  Existe un ecommerce sin usuarios
  Cuando:    Consulto GET /api/v1/users
  Entonces:  El sistema retorna 200 OK con array vacío []
```

---

#### HU-05: Actualizar usuario (cambio de ecommerce)

```
Como:        SUPER_ADMIN
Quiero:      Cambiar el ecommerce asociado a un usuario
Para:        Reasignar clientes según cambios organizacionales

Prioridad:   Media
Estimación:  M
Dependencias: HU-01
Capa:        Backend + Frontend
```

#### Criterios de Aceptación — HU-05

**Happy Path**
```gherkin
CRITERIO-5.1: Actualizar usuario (cambio de ecommerce)
  Dado que:  Soy un SUPER_ADMIN
  Y         Existe un usuario asociado a un ecommerce
  Cuando:    Intento cambiar el ecommerce asociado al usuario
  Entonces:  El sistema actualiza la vinculación
  Y         El usuario ahora pertenece al nuevo ecommerce
  Y         El sistema retorna 200 OK
```

**Error Path**
```gherkin
CRITERIO-5.2: Validar que nuevo ecommerce existe
  Dado que:  Un SUPER_ADMIN intenta actualizar ecommerce_id
  Cuando:    Proporciona un ecommerce_id que no existe
  Entonces:  El sistema retorna 400 Bad Request

CRITERIO-5.3: Solo SUPER_ADMIN puede actualizar
  Dado que:  Un usuario USER intenta actualizar otro usuario
  Cuando:    Ejecuta PUT /api/v1/users/{uid}
  Entonces:  El sistema retorna 403 Forbidden
```

---

#### HU-06: Eliminar usuario

```
Como:        SUPER_ADMIN
Quiero:      Eliminar un usuario del sistema
Para:        Revocar acceso cuando el usuario ya no necesita el servicio

Prioridad:   Alta
Estimación:  S
Dependencias: HU-01
Capa:        Backend + Frontend
```

#### Criterios de Aceptación — HU-06

**Happy Path**
```gherkin
CRITERIO-6.1: Eliminar usuario
  Dado que:  Soy un SUPER_ADMIN
  Y         Existe un usuario asociado a un ecommerce
  Cuando:    Elimino el usuario
  Entonces:  El sistema elimina el perfil de forma permanente
  Y         El usuario ya no puede acceder al sistema
  Y         El sistema retorna 204 No Content
```

**Error Path**
```gherkin
CRITERIO-6.2: Usuario no existe
  Dado que:  Un SUPER_ADMIN intenta eliminar un usuario
  Cuando:    Proporciona un uid que no existe
  Entonces:  El sistema retorna 404 Not Found

CRITERIO-6.3: Solo SUPER_ADMIN puede eliminar
  Dado que:  Un usuario USER intenta eliminar otro usuario
  Cuando:    Ejecuta DELETE /api/v1/users/{uid}
  Entonces:  El sistema retorna 403 Forbidden
```

### Reglas de Negocio

1. **RN-01: Dos roles únicos**
   - `SUPER_ADMIN`: Administrador central del sistema. Sin restricción de ecommerce_id (NULL en BD). Acceso total a CRUD usuarios y a todos los ecommerce.
   - `USER`: Operador de tienda. OBLIGATORIAMENTE vinculado a un ecommerce_id (NOT NULL en BD). Solo ve datos de su ecommerce.

2. **RN-02: Unicidad global de username** — El campo `username` DEBE ser único en todo el sistema. Dos usuarios de diferentes ecommerce NO pueden compartir el mismo username.

3. **RN-03: Vinculación obligatoria para USER** — Cualquier usuario con rol USER DEBE estar vinculado a exactamente un ecommerce_id. El ecommerce_id es NOT NULL para USER.

4. **RN-04: ecommerce_id nullable solo para SUPER_ADMIN** — Solo usuarios SUPER_ADMIN pueden tener ecommerce_id = NULL. Para USER, ecommerce_id es siempre obligatorio.

5. **RN-05: TenantInterceptor filtra todas las consultas** — Un interceptor de seguridad extrae ecommerce_id del JWT y aplica automáticamente `WHERE ecommerce_id = ?` a TODAS las consultas de usuarios con rol USER. SUPER_ADMIN no es filtrado.

6. **RN-06: JWT con ecommerce_id para USER** — El token JWT de usuarios USER DEBE incluir el claim `ecommerce_id`. El token de SUPER_ADMIN NO incluye este claim (o puede incluirlo con valor NULL).

7. **RN-07: Solo SUPER_ADMIN puede hacer CRUD de usuarios** — Endpoints POST, PUT, DELETE `/api/v1/users` son exclusivos para SUPER_ADMIN. Intentos de USER retornan 403 Forbidden.

8. **RN-08: Contraseña hasheada con BCrypt** — Las contraseñas DEBEN ser almacenadas hasheadas usando BCrypt. Nunca plaintext o MD5.

9. **RN-09: Timestamps en UTC ISO 8601** — Todos los timestamps (`created_at`, `updated_at`) se almacenan en UTC (Instant en Java).

---

## 2. DISEÑO

### Modelos de Datos

#### Entidades afectadas

| Entidad | Almacén | Cambios | Descripción |
|---------|---------|---------|-------------|
| `UserEntity` | tabla `users` (PostgreSQL) | modificada | Usuario con roles SUPER_ADMIN (ecommerce_id NULL) o USER (ecommerce_id NOT NULL) |
| `Ecommerce` | tabla `ecommerce` | no modificada | Entidad de ecommerce (referenciada por FK) |

#### Campos del modelo UserEntity

| Campo | Tipo | Nullable | Validación | Descripción |
|-------|------|----------|------------|-------------|
| `id` | Long (BigSerial) | no | auto-generado | ID de base de datos (interno) |
| `uid` | UUID | no | generado | UUID público del usuario (para requests/responses) |
| `username` | String(50) | no | UNIQUE, alphanumeric + underscore | Nombre único globalmente |
| `password` | String(255) | no | BCrypt hash, min 12 chars | Contraseña hasheada |
| `role` | String(50) | no | enum: SUPER_ADMIN, USER | Rol del usuario |
| `ecommerceId` | UUID | **SÍ** | FK a ecommerce (solo si role=USER) | NULL para SUPER_ADMIN, NOT NULL para USER |
| `active` | Boolean | no | default: true | Estado del usuario (activo/inactivo) |
| `created_at` | Instant (UTC) | no | auto-generado | Timestamp de creación |
| `updated_at` | Instant (UTC) | no | auto-generado | Timestamp de última actualización |

#### Índices / Constraints

| Índice | Tabla | Columnas | Tipo | Razón |
|--------|-------|----------|------|-------|
| `idx_username` | users | username | UNIQUE | Búsqueda y validación de unicidad global |
| `idx_ecommerce_id` | users | ecommerce_id | NORMAL | Búsqueda rápida de usuarios por ecommerce |
| `idx_active` | users | active | NORMAL | Filtrado de usuarios activos |
| `idx_role` | users | role | NORMAL | Búsqueda de usuarios por rol |
| `uk_username_global` | users | username | UNIQUE CONSTRAINT | Garantizar unicidad global de username |

#### Validación de Integridad (CHECK Constraint)

```sql
ALTER TABLE users ADD CONSTRAINT chk_role_ecommerce_mapping
CHECK (
  (role = 'SUPER_ADMIN' AND ecommerce_id IS NULL) OR
  (role = 'USER' AND ecommerce_id IS NOT NULL)
);
```

Esto garantiza que SUPER_ADMIN siempre tiene ecommerce_id NULL y USER siempre tiene ecommerce_id NOT NULL.

#### Migraciones Flyway

- **V2__Create_users_table.sql** — Tabla base de usuarios (sin ecommerce_id)
- **V3__Add_ecommerce_id_to_users.sql** — Agregar columna ecommerce_id NULLABLE y migración de datos

---

### API Endpoints

#### POST /api/v1/users
- **Descripción**: Crea un nuevo usuario con rol USER vinculado a un ecommerce
- **Auth requerida**: sí (JWT token con rol SUPER_ADMIN)
- **Request Body**:
  ```json
  {
    "username": "string, 5-50 chars, alphanumeric + underscore",
    "password": "string, min 12 chars",
    "role": "USER",
    "ecommerceId": "UUID, debe existir en sistema"
  }
  ```
- **Response 201 Created**:
  ```json
  {
    "uid": "550e8400-e29b-41d4-a716-446655440000",
    "username": "tienda1-admin",
    "role": "USER",
    "ecommerceId": "4a1c8d4f-a1b2-c3d4-e5f6-a7b8c9d0e1f2",
    "active": true,
    "createdAt": "2026-03-27T10:15:30Z",
    "updatedAt": "2026-03-27T10:15:30Z"
  }
  ```
- **Response 400 Bad Request**: ecommerce_id no existe, password débil, validación falla
- **Response 401 Unauthorized**: token ausente o expirado
- **Response 403 Forbidden**: solo SUPER_ADMIN puede crear usuarios
- **Response 409 Conflict**: username ya existe globalmente

#### GET /api/v1/users
- **Descripción**: Lista usuarios según contexto del usuario autenticado
- **Auth requerida**: sí
- **Query Parameters** (opcional):
  - `ecommerceId`: UUID (solo SUPER_ADMIN puede usar para filtrar específicamente)
  - `active`: boolean (filtrar por estado)
- **Response 200 OK**: array de UserResponse
- **Comportamiento de filtrado**:
  - Si usuario es USER: TenantInterceptor filtra automáticamente por su ecommerce_id
  - Si usuario es SUPER_ADMIN: retorna todos los usuarios (sin filtro automático)
  - Si SUPER_ADMIN usa query param `?ecommerceId=uuid`: filtra específicamente por ese ecommerce
- **Response 403 Forbidden**: usuario USER intenta usar parámetro ecommerceId para otro ecommerce

#### GET /api/v1/users/{uid}
- **Descripción**: Obtiene un usuario por su UUID
- **Auth requerida**: sí
- **Response 200 OK**: objeto UserResponse completo
- **Response 403 Forbidden**: usuario USER intenta acceder a usuario de otro ecommerce
- **Response 404 Not Found**: usuario no existe

#### PUT /api/v1/users/{uid}
- **Descripción**: Actualiza datos de un usuario (solo SUPER_ADMIN)
- **Auth requerida**: sí (JWT con rol SUPER_ADMIN)
- **Request Body** (todos los campos opcionales):
  ```json
  {
    "password": "string, min 12 chars (opcional)",
    "ecommerceId": "UUID (opcional, reasignar a otro ecommerce)",
    "active": "boolean (opcional)"
  }
  ```
- **Response 200 OK**: objeto UserResponse actualizado
- **Response 400 Bad Request**: ecommerce_id no existe, validación falla
- **Response 403 Forbidden**: solo SUPER_ADMIN puede actualizar
- **Response 404 Not Found**: usuario no existe

#### DELETE /api/v1/users/{uid}
- **Descripción**: Elimina un usuario (solo SUPER_ADMIN)
- **Auth requerida**: sí (JWT con rol SUPER_ADMIN)
- **Response 204 No Content**: eliminado exitosamente
- **Response 403 Forbidden**: solo SUPER_ADMIN puede eliminar
- **Response 404 Not Found**: usuario no existe

---

### TenantInterceptor (Componente Crítico)

#### Propósito
Filtrar automáticamente TODAS las consultas de usuarios USER por su `ecommerce_id`, garantizando que no puedan acceder a datos de otros ecommerce.

#### Flujo
1. **Request entra**: usuario autenticado (JWT con ecommerce_id)
2. **Interceptor extrae ecommerce_id** del token
3. **Si el usuario es USER** (tiene ecommerce_id en token):
   - Antes de ejecutar cualquier consulta: agregar `WHERE ecommerce_id = ?`
   - Esta restricción se aplica a TODOS los endpoints
4. **Si el usuario es SUPER_ADMIN**:
   - NO se aplica filtro automático (ecommerce_id es NULL en token)
5. **Validación post-filtro**: Si la consulta retorna un recurso de otro ecommerce, retornar 403 Forbidden

#### Implementación
- Crear `TenantInterceptor implements HandlerInterceptor`
- Registrar en `WebMvcConfigurer.addInterceptors()`
- El interceptor trabaja en conjunto con `SecurityContextHelper` para extraer claim del JWT

---

### Diseño Frontend

#### Componentes nuevos

| Componente | Archivo | Props principales | Descripción |
|------------|---------|------------------|-------------|
| `UserCard` | `components/UserCard.jsx` | `user, onEdit, onDelete, isSuperAdmin` | Tarjeta con username, role, createdAt. Botones edit/delete solo para SUPER_ADMIN |
| `UserFormModal` | `components/UserFormModal.jsx` | `isOpen, onSubmit, onClose, initialData, ecommerce, editMode` | Modal crear/editar usuario |
| `ConfirmDeleteModal` | `components/ConfirmDeleteModal.jsx` | `isOpen, onConfirm, onCancel, itemName` | Confirmación de eliminación |

#### Páginas nuevas

| Página | Archivo | Ruta | Protegida | Permisos requeridos |
|--------|---------|------|-----------|-------------------|
| `UsersPage` | `pages/UsersPage.jsx` | `/users` | sí | SUPER_ADMIN |

#### Hooks y State

| Hook | Archivo | Retorna | Descripción |
|------|---------|---------|-------------|
| `useUsers` | `hooks/useUsers.js` | `{ users, loading, error, create, update, delete, refetch }` | CRUD de usuarios |
| `useAuth` | `hooks/useAuth.js` | `{ user, role, ecommerceId, token }` | Info del usuario autenticado |

#### Services

| Función | Archivo | Endpoint |
|---------|---------|---------|
| `createUser(data, token)` | `services/userService.js` | `POST /api/v1/users` |
| `getUsers(ecommerceId?, token)` | `services/userService.js` | `GET /api/v1/users` |
| `getUserByUid(uid, token)` | `services/userService.js` | `GET /api/v1/users/{uid}` |
| `updateUser(uid, data, token)` | `services/userService.js` | `PUT /api/v1/users/{uid}` |
| `deleteUser(uid, token)` | `services/userService.js` | `DELETE /api/v1/users/{uid}` |

---

### Notas de Implementación

- **UUID para respuestas públicas**: El endpoint expone `uid` (UUID), no `id` (Long), para evitar exposición de IDs internos.
- **Contraseñas en creación**: El frontend NO debe mostrar la contraseña después de creación, por seguridad.
- **Validación de ecommerce**: Usar `EcommerceService.validateEcommerceExists()` en el backend antes de crear/actualizar.
- **Aislamiento automático**: `TenantInterceptor` debe extraer `ecommerce_id` del JWT y pasarlo a repositorio para filtrado automático.
- **SUPER_ADMIN sin filtro**: Solo usuarios SUPER_ADMIN pueden listar/acceder a todos los ecommerce. Validar en controller.
- **Hard delete**: Esta spec implementa hard delete (eliminación permanente). No hay soft delete con `deleted_at`.

---

## 3. LISTA DE TAREAS

### Backend

#### Implementación

- [ ] **Modelo/DTO**: Crear/actualizar `UserCreateRequest`, `UserUpdateRequest`, `UserResponse` como Java Records
- [ ] **Entity**: Actualizar `UserEntity` con ecommerceId NULLABLE
- [ ] **Repository**: Implementar métodos:
  - `findByUsername(username): Optional<UserEntity>`
  - `findByEcommerceId(ecommerceId): List<UserEntity>`
  - `findByUid(uid): Optional<UserEntity>`
- [ ] **Service**: Implementar `UserService` con métodos CRUD
- [ ] **Controller**: Implementar `UserController` con endpoints POST, GET, GET/{uid}, PUT, DELETE
- [ ] **TenantInterceptor**: Crear interceptor de seguridad
- [ ] **SecurityContextHelper**: Actualizar para extraer `ecommerce_id` del JWT
- [ ] **AuthenticationFilter**: Actualizar para inyectar ecommerce_id en JWT
- [ ] **Migration V3**: Asegurar que ecommerce_id es NULLABLE
- [ ] **Excepciones**: Usar existentes (BadRequestException, ConflictException, ResourceNotFoundException, AuthorizationException)

#### Tests Backend

- [ ] `test_createUser_success_super_admin` — Happy path
- [ ] `test_createUser_user_forbidden` — USER no puede crear (403)
- [ ] `test_createUser_ecommerce_not_found` — Error 400
- [ ] `test_createUser_username_duplicate` — Error 409
- [ ] `test_listUsers_user_filtered` — USER ve solo su ecommerce
- [ ] `test_listUsers_super_admin_unfiltered` — SUPER_ADMIN ve todos
- [ ] `test_getUserByUid_different_ecommerce_forbidden` — USER 403 al otro ecommerce
- [ ] `test_updateUser_user_forbidden` — USER no puede actualizar (403)
- [ ] `test_deleteUser_user_forbidden` — USER no puede eliminar (403)
- [ ] `test_TenantInterceptor_filters_user_queries` — Interceptor filtra
- [ ] `test_TenantInterceptor_super_admin_no_filter` — Interceptor NO filtra SUPER_ADMIN
- [ ] `test_JWT_contains_ecommerce_id_for_user` — Token USER tiene ecommerce_id
- [ ] `test_JWT_super_admin_no_ecommerce_id` — Token SUPER_ADMIN no tiene ecommerce_id

### Frontend

#### Implementación

- [ ] **Service**: `userService.js` con funciones API
- [ ] **Hook**: `useUsers.js` con estado y acciones
- [ ] **Componente**: `UserCard.jsx`
- [ ] **Componente**: `UserFormModal.jsx`
- [ ] **Componente**: `ConfirmDeleteModal.jsx`
- [ ] **Página**: `UsersPage.jsx`
- [ ] **Rutas**: Registrar `/users` en router (solo SUPER_ADMIN)
- [ ] **Estilos**: CSS/Tailwind consistente
- [ ] **Validaciones**: username, password, ecommerce

#### Tests Frontend

- [ ] `UserCard.test.jsx` — Renderiza datos
- [ ] `UserCard.test.jsx` — Botones solo si SUPER_ADMIN
- [ ] `UserFormModal.test.jsx` — Validaciones
- [ ] `useUsers.test.js` — CRUD operations
- [ ] `UsersPage.test.jsx` — Layout completo

### QA

- [ ] Ejecutar skill `/gherkin-case-generator` → mapear CRITERIO-1.1 a 6.3
- [ ] Ejecutar skill `/risk-identifier` → evaluar riesgos
- [ ] Revisar cobertura de tests contra todos los CRITERIOS
- [ ] Validar que Reglas de Negocio RN-01 a RN-09 están testeadas
- [ ] Pruebas de integración E2E
- [ ] Validar aislamiento multi-tenant
- [ ] Validar JWT contiene ecommerce_id correcto
- [ ] Actualizar estado spec: `status: IMPLEMENTED`
