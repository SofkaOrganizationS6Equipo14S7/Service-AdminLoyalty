---
id: SPEC-002
status: DRAFT
feature: ecommerce-users
created: 2026-03-26
updated: 2026-03-26
author: spec-generator
version: "1.0"
related-specs: []
---

# Spec: Gestión de Usuarios por Ecommerce

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de iniciar implementación.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPRECATED

---

## 1. REQUERIMIENTOS

### Descripción

Permite que Super Administrador cree y gestione usuarios vinculados a ecommerce específicos, asegurando que cada usuario solo acceda a su propio ecommerce. Cada usuario pertenece exclusivamente a un ecommerce, y sus permisos se restringen al contexto de ese ecommerce en el contexto de seguridad JWT.

### Requerimiento de Negocio

```
Como Super Admin, quiero crear usuarios vinculados a un ecommerce, 
para garantizar que cada uno gestione únicamente sus propias reglas 
de descuento sin afectar a otros ecommerce.
```

### Historias de Usuario

#### HU-01: Crear perfil de usuario asociado a ecommerce

```
Como:        Super Admin
Quiero:      crear un perfil de usuario vinculado a un ecommerce específico
Para:        garantizar que cada usuario solo gestiona su ecommerce

Prioridad:   Alta
Estimación:  M
Dependencias: Ninguna
Capa:        Backend + Frontend
```

#### Criterios de Aceptación — HU-01

**Happy Path**
```gherkin
CRITERIO-1.1: Crear usuario exitosamente con ecommerce válido
  Dado que:     existe un Super Admin autenticado
  Y:            existe un ecommerce registrado (validado por EcommerceService)
  Cuando:       realiza POST /api/v1/users con username, password, role e ecommerceId
  Entonces:     el sistema crea el usuario con status 201
  Y:            retorna { uid (UUID), username, role, ecommerceId, createdAt, updatedAt }
  Y:            la contraseña se guarda hasheada
  Y:            el usuario queda asociado exclusivamente a ese ecommerce
```

**Error Path**
```gherkin
CRITERIO-1.2: Rechazar creación si ecommerce no existe
  Dado que:     existe un Super Admin autenticado
  Cuando:       intenta crear usuario con ecommerceId inválido
  Entonces:     retorna 400 Bad Request
  Y:            mensaje: "El ecommerce no existe"

CRITERIO-1.3: Rechazar creación si username duplicado en mismo ecommerce
  Dado que:     existe usuario "admin@shop1" asociado a ecommerce-1
  Cuando:       intenta crear otro usuario "admin@shop1" en ecommerce-1
  Entonces:     retorna 409 Conflict
  Y:            mensaje: "El username ya existe en este ecommerce"

CRITERIO-1.4: Permitir mismo username en ecommerce diferentes
  Dado que:     existe "admin@shop1" en ecommerce-1
  Y:            existe ecommerce-2
  Cuando:       crea "admin@shop1" en ecommerce-2
  Entonces:     retorna 201 Created
  Y:            ambos usuarios coexisten sin conflicto
```

---

#### HU-02: Validar acceso según ecommerce del usuario

```
Como:        Usuario de ecommerce
Quiero:      que el sistema solo me permita acceder a datos del mi ecommerce
Para:        evitar exponer datos de otros ecommerce

Prioridad:   Alta
Estimación:  M
Dependencias: HU-01
Capa:        Backend
```

#### Criterios de Aceptación — HU-02

**Happy Path**
```gherkin
CRITERIO-2.1: Usuario no-super-admin ve solo su ecommerce
  Dado que:     usuario "admin@shop1" está autenticado con ecommerceId=ecommerce-1
  Y:            el JWT contiene claim: { "ecommerce_id": "ecommerce-1" }
  Cuando:       realiza GET /api/v1/users (listar usuarios)
  Entonces:     retorna solo usuarios asociados a ecommerce-1
  Y:            no expone usuarios de ecommerce-2 o ecommerce-3

CRITERIO-2.2: Super Admin ve todos los ecommerce (sin restricción)
  Dado que:     usuario con rol SUPER_ADMIN está autenticado
  Y:            el JWT NO contiene claim "ecommerce_id"
  Cuando:       realiza GET /api/v1/users
  Entonces:     retorna usuarios de TODOS los ecommerce (sin filtro)
  Y:            puede listar por ecommerceId opcional: GET /api/v1/users?ecommerceId=X
```

**Error Path**
```gherkin
CRITERIO-2.3: Bloquear acceso cruzado entre ecommerce
  Dado que:     usuario "admin@shop1" está asociado a ecommerce-1
  Cuando:       intenta acceder a datos de ecommerce-2 (ej: GET /api/v1/users?ecommerceId=ecommerce-2)
  Entonces:     retorna 403 Forbidden
  Y:            mensaje: "No tiene permiso para acceder a este ecommerce"
```

---

#### HU-03: Listar usuarios por ecommerce

```
Como:        Super Admin
Quiero:      listar todos los usuarios asociados a un ecommerce
Para:        gestionar y monitorear qué usuarios acceden a cada ecommerce

Prioridad:   Alta
Estimación:  S
Dependencias: HU-01
Capa:        Backend + Frontend
```

#### Criterios de Aceptación — HU-03

**Happy Path**
```gherkin
CRITERIO-3.1: Listar usuarios de un ecommerce exitosamente
  Dado que:     existe Super Admin autenticado
  Y:            existen usuarios vinculados a ecommerce-1
  Cuando:       realiza GET /api/v1/users?ecommerceId=ecommerce-1
  Entonces:     retorna 200 OK con array de usuarios
  Y:            cada usuario contiene: { uid, username, role, ecommerceId, createdAt, updatedAt }
  Y:            solo usuarios de ecommerce-1 se retornan

CRITERIO-3.2: Listar sin parámetro ecommerceId (Super Admin)
  Dado que:     existe Super Admin autenticado
  Cuando:       realiza GET /api/v1/users (sin parámetro)
  Entonces:     retorna 200 OK con TODOS los usuarios de TODOS los ecommerce
```

---

#### HU-04: Actualizar usuario (cambio de ecommerce)

```
Como:        Super Admin
Quiero:      cambiar el ecommerce asociado a un usuario existente
Para:        reasignar usuarios cuando hay cambios organizacionales

Prioridad:   Media
Estimación:  M
Dependencias: HU-01
Capa:        Backend + Frontend
```

#### Criterios de Aceptación — HU-04

**Happy Path**
```gherkin
CRITERIO-4.1: Cambiar ecommerce de usuario exitosamente
  Dado que:     existe usuario "admin1" asociado a ecommerce-1
  Y:            existe ecommerce-2 válido
  Cuando:       realiza PUT /api/v1/users/{uid} con { "ecommerceId": "ecommerce-2" }
  Entonces:     retorna 200 OK
  Y:            el usuario ahora pertenece a ecommerce-2
  Y:            puede acceder solo a ecommerce-2

CRITERIO-4.2: Actualizar solo campos permitidos
  Dado que:     existe usuario "admin1"
  Cuando:       realiza PUT /api/v1/users/{uid} con { "username": "admin2", "ecommerceId": "ecommerce-2" }
  Entonces:     retorna 200 OK
  Y:            username se actualiza
  Y:            ecommerceId se actualiza
  Y:            role y password NO se modifican (requieren endpoint separado)
```

**Error Path**
```gherkin
CRITERIO-4.3: Rechazar cambio a ecommerce inválido
  Dado que:     existe usuario "admin1"
  Cuando:       intenta PUT /api/v1/users/{uid} con ecommerceId inválido
  Entonces:     retorna 400 Bad Request
  Y:            mensaje: "El ecommerce no existe"

CRITERIO-4.4: Rechazar actualización de username duplicado en mismo ecommerce
  Dado que:     "admin1" en ecommerce-1
  Y:            "admin2" en ecommerce-1
  Cuando:       intenta renombrar "admin1" a "admin2"
  Entonces:     retorna 409 Conflict
  Y:            mensaje: "El username ya existe en este ecommerce"
```

---

#### HU-05: Eliminar usuario

```
Como:        Super Admin
Quiero:      eliminar un usuario del sistema
Para:        revocar acceso cuando un usuario se va o cambia de rol

Prioridad:   Alta
Estimación:  S
Dependencias: HU-01
Capa:        Backend + Frontend
```

#### Criterios de Aceptación — HU-05

**Happy Path**
```gherkin
CRITERIO-5.1: Eliminar usuario exitosamente
  Dado que:     existe usuario "admin1" en ecommerce-1
  Y:            existe Super Admin autenticado
  Cuando:       realiza DELETE /api/v1/users/{uid}
  Entonces:     retorna 204 No Content (éxito silencioso)
  Y:            el usuario se elimina permanentemente
  Y:            el usuario ya no puede iniciar sesión

CRITERIO-5.2: No permitir que usuario se elimine a sí mismo
  Dado que:     usuario "admin1" está autenticado
  Cuando:       intenta DELETE /api/v1/users/{uid-de-admin1}
  Entonces:     retorna 400 Bad Request
  Y:            mensaje: "No puede eliminarse a sí mismo"
```

**Error Path**
```gherkin
CRITERIO-5.3: Retornar 404 si usuario no existe
  Dado que:     existe Super Admin autenticado
  Cuando:       intenta DELETE /api/v1/users/{uid-inexistente}
  Entonces:     retorna 404 Not Found
```

---

### Reglas de Negocio

1. **Vinculación obligatoria**: Cada usuario debe estar asociado exactamente a un ecommerce. No se permite crear usuario sin ecommerceId válido.

2. **Validación de ecommerce**: La existencia del ecommerce se valida contra `EcommerceService.validateEcommerceExists(ecommerceId)` antes de crear/actualizar usuario.

3. **Unicidad de username por ecommerce**: Username debe ser único dentro del ecommerce, pero puede repetirse entre ecommerce diferentes.

4. **Aislamiento en JWT**: 
   - Usuarios no-super-admin: JWT incluye claim `"ecommerce_id"` con su ecommerce
   - Super Admin: JWT NO tiene restricción de `ecommerce_id`

5. **Filtrado automático en consultas**: 
   - Usuarios no-super-admin solo pueden consultar/actuar sobre usuarios del mismo ecommerce
   - Super Admin puede filtrar por ecommerce o listar todos

6. **Eliminación permanente**: DELETE es irreversible (sin soft delete en esta versión).

7. **Password nunca visible**: La contraseña hasheada nunca se retorna en respuestas (ni en GET ni en POST).

8. **Role no cambiable por usuario**: Solo Super Admin puede cambiar role (no incluido en PUT /api/v1/users/{uid}, requiere endpoint separado).

---

## 2. DISEÑO

### Modelos de Datos

#### Entidades afectadas

| Entidad | Almacén | Cambios | Descripción |
|---------|---------|---------|-------------|
| `UserEntity` | tabla `users` | **modificada** | Agregar columna `ecommerce_id` (UUID, NOT NULL, FK) |

#### Cambios a UserEntity

```sql
ALTER TABLE users ADD COLUMN ecommerce_id UUID NOT NULL;
ALTER TABLE users ADD CONSTRAINT fk_users_ecommerce FOREIGN KEY (ecommerce_id) REFERENCES ecommerce(id);
ALTER TABLE users ADD INDEX idx_ecommerce_id (ecommerce_id);
```

#### Nuevos índices

- `idx_ecommerce_id (ecommerce_id)` — búsqueda rápida de usuarios por ecommerce
- `UNIQUE (username, ecommerce_id)` — unicidad de username dentro de cada ecommerce (reemplaza UK anterior en username solo)

#### Campos del modelo UserEntity (actualizado)

| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `id` | Long | sí | auto-incremento | PK en BD (legacy) |
| `uid` | UUID | sí | auto-generado | UUID para cliente (nuevo campo en DTO) |
| `username` | string | sí | max 50 chars, unique per ecommerce | Nombre de usuario |
| `password` | string | sí | 255 chars (hasheada) | Contraseña hasheada |
| `email` | string | no | format email | Email (optional) |
| `role` | string | sí | (ADMIN, SUPER_ADMIN, etc.) | Rol del usuario |
| `ecommerce_id` | UUID | sí | FK a ecommerce | Ecommerce al que pertenece |
| `active` | boolean | sí | default true | Marca el usuario como activo |
| `created_at` | Instant (UTC) | sí | auto-generado | Timestamp creación |
| `updated_at` | Instant (UTC) | sí | auto-generado | Timestamp actualización |

---

### API Endpoints

#### POST /api/v1/users
- **Descripción**: Crea un nuevo usuario vinculado a un ecommerce
- **Auth requerida**: JWT con rol SUPER_ADMIN
- **Request Body**:
  ```json
  {
    "username": "string (50 chars max)",
    "password": "string (8+ chars, strong)",
    "role": "string (ADMIN, USER, etc.)",
    "email": "string (opcional)",
    "ecommerceId": "string (UUID válido)"
  }
  ```
- **Response 201 Created**:
  ```json
  {
    "uid": "uuid",
    "username": "string",
    "role": "string",
    "email": "string",
    "ecommerceId": "uuid",
    "active": true,
    "createdAt": "2026-03-26T10:30:00Z",
    "updatedAt": "2026-03-26T10:30:00Z"
  }
  ```
- **Response 400 Bad Request**:
  ```json
  { "error": "El ecommerce no existe" }
  ```
  o
  ```json
  { "error": "El username es obligatorio" }
  ```
- **Response 401 Unauthorized**: Token ausente o expirado
- **Response 403 Forbidden**: Usuario no es SUPER_ADMIN
- **Response 409 Conflict**:
  ```json
  { "error": "El username ya existe en este ecommerce" }
  ```

---

#### GET /api/v1/users
- **Descripción**: Lista usuarios (filtra por ecommerce del contexto si no es super admin)
- **Auth requerida**: sí (cualquier rol)
- **Query params**:
  - `ecommerceId` (opcional, UUID) — filtro by ecommerce (solo super admin puede filtrar otro ecommerce)
- **Response 200 OK**:
  ```json
  [
    {
      "uid": "uuid",
      "username": "string",
      "role": "string",
      "email": "string",
      "ecommerceId": "uuid",
      "active": true,
      "createdAt": "2026-03-26T10:30:00Z",
      "updatedAt": "2026-03-26T10:30:00Z"
    }
  ]
  ```
- **Response 401 Unauthorized**: Token ausente o expirado
- **Response 403 Forbidden**: Usuario intenta filtrar por ecommerce diferente al suyo

---

#### GET /api/v1/users/{uid}
- **Descripción**: Obtiene un usuario por uid
- **Auth requerida**: sí
- **Path param**: `uid` (UUID)
- **Response 200 OK**: recurso completo (igual a POST response)
- **Response 401 Unauthorized**: Token ausente o expirado
- **Response 403 Forbidden**: Usuario intenta ver usuario de otro ecommerce
- **Response 404 Not Found**: Usuario no existe

---

#### PUT /api/v1/users/{uid}
- **Descripción**: Actualiza un usuario (username y/o ecommerceId)
- **Auth requerida**: sí (SUPER_ADMIN)
- **Request Body** (todos opcionales):
  ```json
  {
    "username": "string (opcional)",
    "ecommerceId": "string (opcional, UUID válido)"
  }
  ```
- **Response 200 OK**: recurso actualizado (igual estructura POST)
- **Response 400 Bad Request**: ecommerce inválido, username inválido, etc.
- **Response 401 Unauthorized**: Token ausente o expirado
- **Response 403 Forbidden**: No es SUPER_ADMIN
- **Response 404 Not Found**: Usuario no existe
- **Response 409 Conflict**: Username duplicado en nuevo ecommerce

---

#### DELETE /api/v1/users/{uid}
- **Descripción**: Elimina un usuario permanentemente
- **Auth requerida**: sí (SUPER_ADMIN)
- **Path param**: `uid` (UUID)
- **Response 204 No Content**: eliminado exitosamente
- **Response 400 Bad Request**: Usuario intenta eliminarse a sí mismo
- **Response 401 Unauthorized**: Token ausente o expirado
- **Response 403 Forbidden**: No es SUPER_ADMIN
- **Response 404 Not Found**: Usuario no existe

---

### Diseño Frontend

#### Componentes nuevos

| Componente | Archivo | Props principales | Descripción |
|------------|---------|------------------|-------------|
| `UserCard` | `components/UserCard` | `user, onDelete, onEdit, currentUser` | Tarjeta que muestra usuario con acciones |
| `UserFormModal` | `components/UserFormModal` | `isOpen, onSubmit, onClose, ecommerceOptions, initialData` | Modal CRUD (crear/editar usuario) |
| `UsersList` | `components/UsersList` | `users, loading, onDelete, onEdit, currentUser` | Lista completa con paginación |
| `EcommerceFilter` | `components/EcommerceFilter` | `ecommerces, selectedId, onChange, isSuperAdmin` | Dropdown filtro por ecommerce |

#### Páginas nuevas

| Página | Archivo | Ruta | Protegida | Rol requerido |
|--------|---------|------|-----------|---------------|
| `UsersPage` | `pages/UsersPage` | `/users` | sí | SUPER_ADMIN (lectura+gestión) / ADMIN (solo lectura del suyo) |

#### Hooks y State

| Hook | Archivo | Retorna | Descripción |
|------|---------|---------|-------------|
| `useUsers` | `hooks/useUsers` | `{ users, loading, error, create, update, delete, refreshUsers }` | CRUD del usuario + refresh |
| `useEcommerce` | `hooks/useEcommerce` | `{ ecommerce, isSuperAdmin, isLoading }` | Contexto del ecommerce actual |

#### Services (llamadas API)

| Función | Archivo | Endpoint | Method |
|---------|---------|---------|--------|
| `listUsers(token, ecommerceId?)` | `services/userService` | `GET /api/v1/users` | GET |
| `getUser(uid, token)` | `services/userService` | `GET /api/v1/users/{uid}` | GET |
| `createUser(data, token)` | `services/userService` | `POST /api/v1/users` | POST |
| `updateUser(uid, data, token)` | `services/userService` | `PUT /api/v1/users/{uid}` | PUT |
| `deleteUser(uid, token)` | `services/userService` | `DELETE /api/v1/users/{uid}` | DELETE |

---

### Arquitectura y Dependencias

#### Backend — Spring Boot

**Paquetes nuevos**: Ninguno (reutilizar estructura existente)

**Modificaciones en paquetes existentes**:
- `domain/entity/UserEntity.java` — agregar campo `ecommerce_id`
- `domain/repository/UserRepository.java` — agregar métodos `findByEcommerceId(UUID)`, `findByUsernameAndEcommerceId(String, UUID)`
- `application/dto/` — crear DTOs: `UserCreateRequest`, `UserUpdateRequest`, `UserResponse`
- `application/service/UserService.java` — lógica CRUD con validación de ecommerce
- `presentation/controller/UserController.java` — endpoints REST

**Interceptor de seguridad**: Modificar `AuthenticationFilter` existente para:
- Extraer `ecommerce_id` del JWT y guardarlo en contexto de seguridad (ej: `SecurityContextHolder`)
- Validar que usuario solo accede a su ecommerce (excepto super admin)

**Servicios reutilizados**:
- `EcommerceService.validateEcommerceExists(UUID)` — ya existe
- `PasswordEncoder` — para hash de password

**Migraciones Flyway**:
- `V5__Add_ecommerce_id_to_users.sql` — agregar columna + índices

#### Frontend — React

**Paquetes nuevos**: Ninguno

**Servicios reutilizados**:
- `authService` (login, isAuthenticated, getRole)
- `apiClient` (interceptor con token JWT)

**Contexto reutilizado**:
- `AuthContext` — para obtener rol actual y ecommerce_id del JWT

**Store/State**:
- Si usa Context API: `UserContext` (crear)
- Si usa Redux: `userSlice` (crear)

---

### Notas de Implementación

1. **Migración de datos**: Si UserEntity ya existe en producción, crear script Flyway que:
   - Agrega columna `ecommerce_id` (nullable inicialmente)
   - Asigna default ecommerce para usuarios existentes (ej: via parámetro en migración)
   - Cambia a NOT NULL

2. **Backward compatibility**: El campo `ecommerce_id` debe ser obligatorio en la API pero tolerante en migración.

3. **Password strength**: Validar mínimo 8 caracteres, mezcla de mayúsculas/minúsculas/números en frontend y backend.

4. **Contexto de seguridad**: El `ecommerce_id` debe extraerse de JWT (claim `ecommerce_id`) y usado para filtrado automático en queries.

5. **UID vs ID**: Internamente usamos `Long` en BD, pero exponemos `UUID` en API como `uid` para cliente.

6. **Super Admin**: No tiene `ecommerce_id` en JWT, por lo que puede listar/filtrar todos los ecommerce.

7. **Transaccionalidad**: Endpoints POST/PUT/DELETE deben ser transaccionales (`@Transactional`).

---

## 3. LISTA DE TAREAS

### Backend

#### Implementación
- [ ] Crear migración `V5__Add_ecommerce_id_to_users.sql` — agregar columna + constraints
- [ ] Modificar `UserEntity` — agregar campo `ecommerceId` (UUID, NOT NULL)
- [ ] Crear DTOs: `UserCreateRequest`, `UserUpdateRequest`, `UserResponse` (Java Records)
- [ ] Extender `UserRepository` — métodos `findByEcommerceId(UUID)`, `findByUsernameAndEcommerceId(...)`
- [ ] Implementar `UserService` — métodos CRUD con validación de ecommerce
  - `createUser(UserCreateRequest)`
  - `listUsersByEcommerce(UUID)`
  - `getUserById(UUID)`
  - `updateUser(UUID, UserUpdateRequest)`
  - `deleteUser(UUID)`
- [ ] Crear `UserController` — endpoints POST, GET (list), GET (detail), PUT, DELETE
- [ ] Modificar `AuthenticationFilter` — extraer y validar `ecommerce_id` del JWT
- [ ] Registrar `UserController` en punto de entrada

#### Tests Backend
- [ ] `test_userService_createUser_success` — happy path creación
- [ ] `test_userService_createUser_ecommerceNotFound` — error ecommerce inválido
- [ ] `test_userService_createUser_usernameDuplicate` — error username duplicado en same ecommerce
- [ ] `test_userService_createUser_allowDuplicateUsernameOtherEcommerce` — same username OK en otro ecommerce
- [ ] `test_userRepository_findByEcommerceId_returns_list` — repositorio query
- [ ] `test_userController_post_returns_201` — endpoint creación
- [ ] `test_userController_get_returns_200` — listado
- [ ] `test_userController_get_filtering_by_ecommerce` — filtrado por ecommerce
- [ ] `test_userController_put_returns_200` — actualización
- [ ] `test_userController_delete_returns_204` — eliminación
- [ ] `test_userController_delete_self_returns_400` — validación auto-eliminación

### Frontend

#### Implementación
- [ ] Crear `services/userService.js` — funciones listUsers, createUser, updateUser, deleteUser
- [ ] Crear `hooks/useUsers.js` — hook/store con estado, loading, error y acciones CRUD
- [ ] Crear `components/UserCard.js` — tarjeta de usuario con botones editar/eliminar
- [ ] Crear `components/UserFormModal.js` — modal CRUD con campos username, password, role, ecommerceId
- [ ] Crear `components/UsersList.js` — lista con paginación y acciones
- [ ] Crear `components/EcommerceFilter.js` — dropdown filtro (solo visible si super admin)
- [ ] Crear `pages/UsersPage.js` — layout completo con lista + modal
- [ ] Registrar ruta `/users` en router

#### Tests Frontend
- [ ] `[UserCard] renders user name correctly`
- [ ] `[UserCard] calls onDelete when delete clicked`
- [ ] `[UserCard] calls onEdit when edit clicked`
- [ ] `[UserFormModal] submits POST on create`
- [ ] `[UserFormModal] submits PUT on edit`
- [ ] `[UserFormModal] validates required fields`
- [ ] `useUsers hook loads users on mount`
- [ ] `useUsers hook handles create error`
- [ ] `useUsers hook respects ecommerce filter`
- [ ] `[UsersPage] renders list of users`
- [ ] `[EcommerceFilter] only visible to super admin`

### QA
- [ ] Ejecutar skill `/gherkin-case-generator` → criterios CRITERIO-1.1 a 5.3
- [ ] Ejecutar skill `/risk-identifier` → clasificación ASD de riesgos
- [ ] Revisar cobertura de tests contra criterios de aceptación
- [ ] Validar aislamiento de ecommerce en queries
- [ ] Validar que super admin puede listar todos los ecommerce
- [ ] Validar que usuario no-super-admin solo ve su ecommerce
- [ ] Actualizar estado spec: `status: IMPLEMENTED` después de completar

---

### Estimación Total
- **Backend**: 2 sprints (implementación + tests)
- **Frontend**: 2 sprints (implementación + tests)
- **QA**: 1 sprint

**Riesgo**: Migración de datos existentes en `users` table (requiere estrategia de backward compatibility)
