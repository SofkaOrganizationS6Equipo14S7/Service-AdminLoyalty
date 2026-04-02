---
id: SPEC-004
status: DRAFT
feature: store-user
created: 2026-03-29
updated: 2026-03-29
author: spec-generator
version: "1.0"
related-specs:
  - SPEC-006
  - SPEC-003
---

# Spec: Usuario Estándar (STORE_USER)

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de iniciar implementación.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPRECATED

---

## 1. REQUERIMIENTOS

### Descripción
Implementar el rol de Usuario Estándar (STORE_USER) que proporciona acceso limitado a un único ecommerce. Los STORE_USER pueden iniciar sesión, visualizar información de su ecommerce integrado y actualizar su perfil personal, siempre dentro del contexto de los permisos asignados por el STORE_ADMIN.

### Requerimiento de Negocio
El requerimiento original de `.github/requirements/store-user.md`:

**Como** STORE_USER  
**Quiero** tener acceso limitado según los permisos que me asigne el STORE_ADMIN  
**Para** visualizar y gestionar únicamente la información de mi ecommerce, sin poder crear otros usuarios y estando vinculado a un ecommerce específico.

### Historias de Usuario

#### HU-01: Iniciar sesión como STORE_USER

```
Como:        Usuario con rol STORE_USER
Quiero:      autenticarme con username y contraseña
Para:        acceder al sistema y visualizar mi ecommerce

Prioridad:   Alta
Estimación:  S
Dependencias: Requiere tablas de usuarios y JWT implementados
Capa:        Backend + Frontend
```

#### Criterios de Aceptación — HU-01

**Happy Path**
```gherkin
CRITERIO-1.1: Autenticación exitosa de STORE_USER
  Dado que:  existe un usuario con rol STORE_USER vinculado a un ecommerce
  Cuando:    proporciono username y contraseña válidos
  Entonces:  el sistema valida las credenciales
             y retorna un JWT con claims: role=STORE_USER, ecommerce_id=<UID>, uid=<UID>
             y redirige al dashboard del ecommerce
```

**Error Path**
```gherkin
CRITERIO-1.2: Rechazo de credenciales inválidas
  Dado que:  intento autenticarme como STORE_USER
  Cuando:    proporciono email o contraseña incorrectos
  Entonces:  el sistema retorna 401 Unauthorized
             y muestra mensaje de error: "Credenciales inválidas"

CRITERIO-1.3: Usuario no activo
  Dado que:  existe un STORE_USER pero no está activo
  Cuando:    intento autenticarme
  Entonces:  el sistema retorna 403 Forbidden
             y muestra mensaje: "Usuario inactivo. Contacte al administrador"
```

---

#### HU-02: Acceder solo a información de mi ecommerce

```
Como:        STORE_USER autenticado
Quiero:      visualizar solo los datos pertenecientes a mi ecommerce
Para:        garantizar aislamiento de datos entre diferentes negocios

Prioridad:   Alta
Estimación:  M
Dependencias: HU-01 (login)
Capa:        Backend
```

#### Criterios de Aceptación — HU-02

**Happy Path**
```gherkin
CRITERIO-2.1: Acceso a datos del ecommerce autorizado
  Dado que:  soy un STORE_USER autenticado vinculado al ecommerce XYZ
  Cuando:    consulto endpoints que retornan información de mi ecommerce
  Entonces:  el sistema filtra automáticamente por ecommerce_id del JWT
             y retorna solo los datos del ecommerce XYZ

CRITERIO-2.2: Dashboard del ecommerce cargado correctamente
  Dado que:  soy un STORE_USER autenticado
  Cuando:    accedo al dashboard
  Entonces:  se cargan los datos agregados de mi ecommerce (métricas básicas)
             dentro del contexto de aislamiento por ecommerce_id
```

**Error Path**
```gherkin
CRITERIO-2.3: Intento de acceso a otro ecommerce
  Dado que:  soy un STORE_USER vinculado al ecommerce A
  Cuando:    intento acceder a datos del ecommerce B (ej. manipulando query params)
  Entonces:  el sistema deniega el acceso
             y retorna 403 Forbidden
             y registra el intento fallido en logs de seguridad

CRITERIO-2.4: Token ausente o expirado
  Dado que:  no tengo un JWT válido
  Cuando:    intento acceder a endpoints protegidos
  Entonces:  el sistema retorna 401 Unauthorized
             y redirige al login en el frontend
```

---

#### HU-03: Actualizar mi información de perfil

```
Como:        STORE_USER autenticado
Quiero:      actualizar mis datos personales (nombre, email, contraseña)
Para:        mantener mi perfil actualizado sin afectar la vinculación con mi ecommerce

Prioridad:   Media
Estimación:  S
Dependencias: HU-01 (debe estar autenticado)
Capa:        Backend + Frontend
```

#### Criterios de Aceptación — HU-03

**Happy Path**
```gherkin
CRITERIO-3.1: Actualización de nombre y email
  Dado que:  soy un STORE_USER autenticado
  Cuando:    actualizo mi nombre y/o email en la pantalla de perfil
  Entonces:  el sistema valida los campos
             y persiste los cambios en la BD
             y retorna 200 OK con el perfil actualizado
             y mantiene mi vinculación con el ecommerce intacta

CRITERIO-3.2: Cambio seguro de contraseña
  Dado que:  soy un STORE_USER autenticado
  Cuando:    solicito cambiar mi contraseña y proporciono: contraseña actual + nueva contraseña (2x)
  Entonces:  el sistema verifica que la contraseña actual sea correcta
             y valida que las nuevas contraseñas coincidan
             y persiste el hash de la nueva contraseña
             y retorna 200 OK
             y genera un nuevo JWT automáticamente
```

**Error Path**
```gherkin
CRITERIO-3.3: Validación de email duplicado a nivel global
  Dado que:  intento cambiar mi email a uno que ya existe en cualquier otro usuario del sistema
  Cuando:    envío la solicitud de actualización (ej. un STORE_USER de Nike intenta usar email de admin de Adidas)
  Entonces:  el sistema retorna 409 Conflict
             y muestra mensaje: "El email ya está en uso"
             (validación global, no limitada al ecommerce)

CRITERIO-3.4: Cambio de contraseña con contraseña actual incorrecta
  Dado que:  intento cambiar mi contraseña
  Cuando:    proporciono una contraseña actual inválida
  Entonces:  el sistema retorna 401 Unauthorized
             y muestra mensaje: "Contraseña actual incorrecta"

CRITERIO-3.5: Email inválido
  Dado que:  intento cambiar a un email mal formado
  Cuando:    envío la solicitud
  Entonces:  el sistema retorna 400 Bad Request
             y muestra mensaje: "Formato de email inválido"
```

---

### Reglas de Negocio

1. **Vinculación única:** Todo STORE_USER debe estar vinculado a exactamente un ecommerce. No puede cambiar de ecommerce.
2. **Aislamiento de datos:** Todas las consultas en BD deben filtrar automáticamente por `ecommerce_id` del contexto de seguridad (JWT).
3. **Sin creación de usuarios:** Los STORE_USER no pueden crear otros usuarios. Solo el STORE_ADMIN puede hacerlo.
4. **Permisos granulares:** El STORE_ADMIN asigna qué acciones específicas puede realizar el STORE_USER (lectura/escritura por módulo). El sistema soporta permisos dinámicos configurables.
5. **Autenticación JWT:** Toda solicitud debe incluir un JWT válido con claims `role`, `ecommerce_id` y `uid`.
6. **Validación de contraseña:** Mínimo 12 caracteres, debe incluir mayúscula, minúscula y número.
7. **Email único global:** No puede haber dos usuarios con el mismo email en el sistema, sin importar a qué ecommerce estén vinculados. La validación de unicidad es a nivel global.
8. **Auditoría:** Los cambios de perfil deben registrarse con timestamp y usuario que realizó el cambio.

---

## 2. DISEÑO

### Modelos de Datos

#### Entidades afectadas
| Entidad | Almacén | Cambios | Descripción |
|---------|---------|---------|-------------|
| `User` | tabla `app_user` | **existente** | Tabla de usuarios con soporte para roles |
| `Role` | tabla `roles` | **existente** | Roles: SUPER_ADMIN, STORE_ADMIN, STORE_USER |
| `Ecommerce` | tabla `ecommerce` | **existente** | Comercios electrónicos |

#### Validación — Constraints existentes obligatorios
| Constraint | Tabla | Campo(s) | Descripción |
|-----------|-------|----------|-------------|
| `email_unique` | `app_user` | `email` | Email único a nivel global |
| `user_ecommerce_fk` | `app_user` | `ecommerce_id` | Relación con tabla ecommerce |
| `role_fk` | `app_user` | `role_id` | FK a tabla roles |

### API Endpoints

#### POST /api/v1/auth/login
- **Descripción**: Autentica un usuario (STORE_USER u otros roles) con username y contraseña
- **Auth requerida**: no
- **Request Body**:
  ```json
  {
    "username": "usuario",
    "password": "SecurePassword123"
  }
  ```
- **Response 200**:
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "uid": "550e8400-e29b-41d4-a716-446655440000",
      "email": "usuario@ecommerce.com",
      "name": "Juan Pérez",
      "role": "STORE_USER",
      "ecommerce_id": "660e8400-e29b-41d4-a716-446655440001"
    }
  }
  ```
- **Response 401**: Credenciales inválidas
- **Response 403**: Usuario inactivo

---

#### GET /api/v1/users/me
- **Descripción**: Obtiene el perfil del usuario autenticado
- **Auth requerida**: sí (JWT)
- **Response 200**:
  ```json
  {
    "uid": "550e8400-e29b-41d4-a716-446655440000",
    "email": "usuario@ecommerce.com",
    "name": "Juan Pérez",
    "role": "STORE_USER",
    "ecommerce_id": "660e8400-e29b-41d4-a716-446655440001",
    "created_at": "2026-03-20T10:00:00Z",
    "updated_at": "2026-03-28T15:30:00Z"
  }
  ```
- **Response 401**: Token ausente o expirado
- **Response 403**: Token inválido

---

#### PUT /api/v1/users/me
- **Descripción**: Actualiza el perfil del usuario autenticado (nombre y email)
- **Auth requerida**: sí (JWT)
- **Request Body**:
  ```json
  {
    "name": "Juan Carlos Pérez",
    "email": "nuevo.email@ecommerce.com"
  }
  ```
- **Response 200**: Perfil actualizado
- **Response 400**: Validación fallida (email inválido, nombre vacío, etc.)
- **Response 409**: Email ya existe en otro usuario
- **Response 401**: Token inválido

---

#### PUT /api/v1/users/me/password
- **Descripción**: Cambia la contraseña del usuario autenticado
- **Auth requerida**: sí (JWT)
- **Request Body**:
  ```json
  {
    "currentPassword": "OldPassword123",
    "newPassword": "NewPassword456",
    "confirmPassword": "NewPassword456"
  }
  ```
- **Response 200**:
  ```json
  {
    "message": "Contraseña actualizada exitosamente",
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
  ```
- **Response 400**: Contraseñas no coinciden / formato inválido
- **Response 401**: Contraseña actual incorrecta

---

#### GET /api/v1/ecommerces/{ecommerce_id}
- **Descripción**: Obtiene datos del ecommerce (solo accesible si el user está vinculado)
- **Auth requerida**: sí (JWT con ecommerce_id coincidente)
- **Response 200**: Datos del ecommerce
- **Response 403**: Usuario no vinculado a este ecommerce
- **Response 404**: Ecommerce no encontrado

---

### Diseño Frontend

#### Páginas nuevas
| Página | Archivo | Ruta | Protegida | Descripción |
|--------|---------|------|-----------|-------------|
| `LoginPage` | `pages/LoginPage` | `/login` | no | Formulario de login para STORE_USER |
| `ProfilePage` | `pages/ProfilePage` | `/profile` | sí | Edición de perfil personal |
| `DashboardPage` | `pages/DashboardPage` | `/dashboard` | sí | Panel principal con datos del ecommerce |

#### Componentes nuevos
| Componente | Archivo | Props principales | Descripción |
|------------|---------|------------------|-------------|
| `LoginForm` | `components/auth/LoginForm` | `onSubmit, isLoading, error` | Formulario de login |
| `ProfileForm` | `components/profile/ProfileForm` | `user, onSubmit, isLoading` | Edición de nombre y email |
| `PasswordChangeForm` | `components/profile/PasswordChangeForm` | `onSubmit, isLoading, error` | Cambio de contraseña |
| `EcommerceDashboard` | `components/dashboard/EcommerceDashboard` | `ecommerce, user` | Panel del ecommerce con métricas básicas |

#### Hooks y State
| Hook | Archivo | Retorna | Descripción |
|------|---------|---------|-------------|
| `useAuth` | `hooks/useAuth` | `{ user, token, login, logout, loading, error }` | Gestión de autenticación |
| `useProfile` | `hooks/useProfile` | `{ user, updateProfile, changePassword, loading, error }` | Operaciones de perfil |
| `useEcommerce` | `hooks/useEcommerce` | `{ data, loading, error }` | Obtiene datos del ecommerce del usuario |

#### Services (llamadas API)
| Función | Archivo | Endpoint |
|---------|---------|---------|
| `login(username, password)` | `services/authService` | `POST /api/v1/auth/login` |
| `logout()` | `services/authService` | Local token clear |
| `getProfile(token)` | `services/userService` | `GET /api/v1/users/me` |
| `updateProfile(data, token)` | `services/userService` | `PUT /api/v1/users/me` |
| `changePassword(data, token)` | `services/userService` | `PUT /api/v1/users/me/password` |
| `getEcommerce(ecommerceId, token)` | `services/ecommerceService` | `GET /api/v1/ecommerces/{ecommerce_id}` |

#### Flujo de navegación
- Usuario no autenticado → redirige a `/login`
- Login exitoso → redirige a `/dashboard`
- Acceso a `/profile` dentro de dashboard
- Logout → limpia token, redirige a `/login`

### Arquitectura y Dependencias

#### Backend
- **Nuevas dependencias**: Ninguna (reutiliza JWT, Spring Security, Flyway existentes)
- **Paquetes existentes a reutilizar**:
  - `com.loyalty.service_admin.domain.entities.User` (model existente)
  - `com.loyalty.service_admin.infrastructure.security.JwtTokenProvider` (JWT existente)
  - `com.loyalty.service_admin.presentation.controllers.AuthController` (probablemente existe)
  
#### Frontend
- **Nuevas dependencias**: Ninguna (reutiliza react-router, axios, zustand u otro state manager existente)
- **Integración con existente**: Router protegido, interceptor de authorization header, token storage

### Notas de Implementación

1. **Contexto de seguridad:** El valor `ecommerce_id` del JWT debe usarse para filtrar automáticamente en todas las queries. Implementar esto en un servicio reutilizable.

2. **Permisos granulares:** Implementar sistema de permisos mediante tabla `permissions` y relación `user_permissions`. Los permisos son configurables por el STORE_ADMIN y se validan en cada request (ej. `@RequirePermission("promotion:write")`). Considerar cache en memoria (Caffeine) para permisos frecuentes.

3. **Validación de email global:** La unicidad de email debe validarse contra TODOS los usuarios del sistema (constraint `UNIQUE` en BD), sin filtrar por ecommerce. Un usuario de Nike NO puede usar el email del admin de Adidas.

4. **Validación de contraseña:** Reutilizar validador existente. Mínimo 12 caracteres, debe incluir mayúscula, minúscula, número.

5. **Hash de contraseña:** Usar BCrypt (Spring Security) con salt automático. Nunca almacenar contraseña en texto plano.

6. **Token expiration:** Definir en `application.yml`. Recomendado: 1 hora de acceso, refresh token de 7 días (si se implementa).

7. **CORS:** Asegurar que `/auth/login` permite requests desde frontend sin autenticación.

8. **Auditoría:** Los endpoints de actualización (`PUT /api/v1/users/me`, `PUT /api/v1/users/me/password`) deben registrar el cambio con timestamp y user UID en tabla de auditoría.

9. **Aislamiento:** Validar que el filtro por `ecommerce_id` se aplica consistentemente. Usar testing de seguridad para verificar que un STORE_USER A no puede acceder a datos de ecommerce B.

---

## 3. LISTA DE TAREAS

> Checklist accionable para Backend Developer, Frontend Developer y Test Engineer. Marcar cada ítem (`[x]`) al completarlo.
> El Orchestrator monitorea este checklist para determinar el progreso.

### Backend

#### Implementación
- [ ] Validar que modelo `User` tiene campos: `uid`, `email`, `name`, `password_hash`, `role`, `ecommerce_id`, `active`, `created_at`, `updated_at`
- [ ] Implementar/completar `AuthService` — método `login(username, password)` que valida credenciales y genera JWT con claims `role`, `ecommerce_id`, `uid`
- [ ] Implementar `UserService` — métodos: `getProfile(uid)`, `updateProfile(uid, profileData)`, `changePassword(uid, currentPassword, newPassword)`
- [ ] Implementar controlador `AuthController` — endpoint `POST /api/v1/auth/login`
- [ ] Implementar controlador `UserController` — endpoints: `GET /api/v1/users/me`, `PUT /api/v1/users/me`, `PUT /api/v1/users/me/password`
- [ ] Implementar `EcommerceController` — endpoint `GET /api/v1/ecommerces/{ecommerce_id}` con validación de ownership
- [ ] Crear o completar `SecurityContextFilter` — extrae `ecommerce_id` del JWT y lo disponibiliza en contexto
- [ ] Implementar validador de contraseña reutilizable — mínimo 12 caracteres, mayúscula, minúscula, número
- [ ] Agregar constraint de unicidad en email si no existe
- [ ] Asegurar que todas las queries de datos de ecommerce incluyen filtro automático por `ecommerce_id` del contexto

#### Tests Backend
- [ ] `test_login_success_returns_jwt_with_ecommerce_id` — login válido genera JWT correcto
- [ ] `test_login_invalid_password_returns_401` — credenciales incorrectas
- [ ] `test_login_inactive_user_returns_403` — usuario inactivo
- [ ] `test_get_profile_returns_user_data` — obtener perfil autenticado
- [ ] `test_get_profile_no_token_returns_401` — sin token
- [ ] `test_update_profile_name_success` — actualizar nombre
- [ ] `test_update_profile_email_duplicate_returns_409` — email duplicado
- [ ] `test_change_password_success_returns_new_token` — cambio de contraseña exitoso
- [ ] `test_change_password_wrong_current_returns_401` — contraseña actual incorrecta
- [ ] `test_change_password_mismatch_returns_400` — nuevas contraseñas no coinciden
- [ ] `test_get_ecommerce_success_if_user_linked` — acceso a ecommerce propio
- [ ] `test_get_ecommerce_forbidden_if_user_not_linked` — acceso denegado a otro ecommerce
- [ ] `test_ecommerce_queries_filtered_by_ecommerce_id` — validar aislamiento de datos

### Frontend

#### Implementación
- [ ] Crear `services/authService` — funciones `login()`, `logout()`, `getToken()`, `setToken()`
- [ ] Crear `services/userService` — funciones `getProfile()`, `updateProfile()`, `changePassword()`
- [ ] Crear `services/ecommerceService` — función `getEcommerce()`
- [ ] Crear `hooks/useAuth` — gestiona token, user, login/logout, persistencia en localStorage
- [ ] Crear `hooks/useProfile` — gestiona datos del perfil, actualización, error handling
- [ ] Crear `hooks/useEcommerce` — carga datos del ecommerce del usuario
- [ ] Crear `components/auth/LoginForm` — form con username, password, validación básica
- [ ] Crear `components/profile/ProfileForm` — form edición nombre y email
- [ ] Crear `components/profile/PasswordChangeForm` — form cambio de contraseña
- [ ] Crear `components/dashboard/EcommerceDashboard` — panel con datos e info del ecommerce
- [ ] Crear `pages/LoginPage` — layout con LoginForm
- [ ] Crear `pages/ProfilePage` — layout con ProfileForm y PasswordChangeForm
- [ ] Crear `pages/DashboardPage` — layout con EcommerceDashboard
- [ ] Registrar rutas `/login`, `/profile`, `/dashboard` en router principal
- [ ] Implementar `ProtectedRoute` — redirige a `/login` si no hay token válido
- [ ] Implementar interceptor de Axios — agrega `Authorization: Bearer` headers en requests
- [ ] Implementar persistencia de token en localStorage / sessionStorage

#### Tests Frontend
- [ ] `[LoginForm] renders username and password inputs`
- [ ] `[LoginForm] submits form with correct data on submit`
- [ ] `[LoginForm] Shows error message on failed login`
- [ ] `useAuth returns login function`
- [ ] `useAuth persists token to localStorage on login`
- [ ] `useAuth clears token on logout`
- [ ] `[ProtectedRoute] redirects to login if no token`
- [ ] `[ProfileForm] renders form with user data`
- [ ] `[ProfileForm] submits update on form submit`
- [ ] `[PasswordChangeForm] validates password match`
- [ ] `[EcommerceDashboard] renders ecommerce data`
- [ ] `[DashboardPage] loads on mount`
- [ ] `useEcommerce returns ecommerce data filtered by user`

### QA
- [ ] Ejecutar skill `/gherkin-case-generator` → escenarios CRITERIO-1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4, 3.5
- [ ] Ejecutar skill `/risk-identifier` → clasificación ASD de riesgos (autenticación, aislamiento de datos, validación)
- [ ] Revisar cobertura de tests contra todos los criterios
- [ ] Validar que el aislamiento por `ecommerce_id` funciona correctamente (test manual de seguridad)
- [ ] Validar que STORE_USER no puede crear usuarios
- [ ] Validar que STORE_USER no puede cambiar su role
- [ ] Validar que STORE_USER solo ve su ecommerce
- [ ] Actualizar estado spec: `status: IMPLEMENTED`
