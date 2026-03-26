---
id: SPEC-001
status: IN_PROGRESS
feature: login-logout
created: 2026-03-26
updated: 2026-03-26
author: spec-generator
version: "1.0"
related-specs: []
---

# Spec: Inicio y Cierre de Sesión (Login/Logout)

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de iniciar implementación.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPRECATED

---

## 1. REQUERIMIENTOS

### Descripción
Sistema de autenticación seguro que permite a los usuarios iniciar sesión con credenciales válidas (email/username + password) y cerrar sesión. Tras el login exitoso, se genera un token JWT para acceso al dashboard. El logout invalida la sesión del usuario.

### Requerimiento de Negocio
```
HU-01: Inicio y cierre de sesión

Como usuario de LOYALTY,
quiero iniciar sesión y cerrar sesión,
para acceder de forma segura al dashboard.

Criterios de Aceptación:
- Login exitoso con credenciales válidas otorga acceso
- Sistema rechaza credenciales inválidas
- Sistema valida campos obligatorios en login
- Usuario puede cerrar sesión y ser redirigido a login
```

### Historias de Usuario

#### HU-01.1: Inicio de sesión exitoso

```
Como:        Usuario no autenticado
Quiero:      Iniciar sesión con mis credenciales (username + password)
Para:        Acceder al dashboard y funciones del sistema de forma segura

Prioridad:   Alta
Estimación:  M
Dependencias: Ninguna (User entity ya existe)
Capa:        Backend + Frontend
```

#### Criterios de Aceptación — HU-01.1

**Happy Path**
```gherkin
CRITERIO-1.1: Inicio de sesión exitoso
  Dado que:    existe un usuario registrado con credenciales válidas (username="admin", password="admin123")
  Cuando:      envía POST /api/v1/auth/login con username y password válidos
  Entonces:    recibe status 200 con token JWT, tipo "Bearer" y username del usuario
         AND   token es válido por 24 horas
         AND   frontend redirige al dashboard según el rol del usuario
```

**Error Paths**
```gherkin
CRITERIO-1.2: Rechazo de credenciales erróneas (password inválido)
  Dado que:    existe un usuario registrado con username válido
  Cuando:      envía POST /api/v1/auth/login con password incorrecto
  Entonces:    recibe status 401 (Unauthorized)
         AND   mensaje de error: "Credenciales inválidas"
         AND   no genera token alguno

CRITERIO-1.3: Usuario no encontrado
  Dado que:    no existe usuario con el username proporcionado
  Cuando:      envía POST /api/v1/auth/login con username inexistente
  Entonces:    recibe status 401 (Unauthorized)
         AND   mensaje de error: "Usuario no válido"

CRITERIO-1.4: Usuario desactivado
  Dado que:    existe un usuario con credenciales válidas pero active=false
  Cuando:      intenta iniciar sesión
  Entonces:    recibe status 401 (Unauthorized)
         AND   mensaje de error: "Usuario no válido"
```

**Validación de Campos**
```gherkin
CRITERIO-1.5: Username obligatorio
  Dado que:    está en la pantalla de login
  Cuando:      intenta enviar formulario sin username
  Entonces:    recibe error de validación frontend
         AND   mensaje: "username es obligatorio"
         AND   no envía request al backend

CRITERIO-1.6: Password obligatorio
  Dado que:    está en la pantalla de login
  Cuando:      intenta enviar formulario sin password
  Entonces:    recibe error de validación frontend
         AND   mensaje: "password es obligatorio"
         AND   no envía request al backend
```

---

#### HU-01.2: Cierre de sesión (Logout)

```
Como:        Usuario autenticado
Quiero:      Cerrar sesión
Para:        Salir de forma segura del sistema y evitar acceso no autorizado

Prioridad:   Alta
Estimación:  S
Dependencias: HU-01.1 (login previo)
Capa:        Backend + Frontend
```

#### Criterios de Aceptación — HU-01.2

**Happy Path**
```gherkin
CRITERIO-2.1: Cierre de sesión exitoso
  Dado que:    el usuario está autenticado con token válido
  Cuando:      ejecuta logout (click botón logout o POST /api/v1/auth/logout)
  Entonces:    el token se invalida en backend
         AND   frontend limpia el localStorage (token, user data)
         AND   frontend redirige a página de login
         AND   intentos futuros al dashboard con ese token se rechazan

CRITERIO-2.2: Logout sin afectar otros usuarios
  Dado que:    múltiples usuarios están autenticados simultáneamente
  Cuando:      un usuario ejecuta logout
  Entonces:    solo su token se invalida
         AND   otros usuarios permanecen autenticados
```

**Error Paths**
```gherkin
CRITERIO-2.3: Logout sin token válido
  Dado que:    el usuario no está autenticado o su token expiró
  Cuando:      intenta ejecutar logout (token ausente o inválido)
  Entonces:    frontend redirige a login
         AND   se limpian datos locales

CRITERIO-2.4: Dashboard protegido sin token
  Dado que:    el usuario cerró sesión
  Cuando:      intenta acceder a /dashboard o cualquier ruta protegida
  Entonces:    frontend redirige a /login
         AND   recibe mensaje: "Sesión expirada, por favor inicie sesión nuevamente"
```

---

### Reglas de Negocio
1. **Validación de credenciales**: El sistema realiza comparación directa de password (campo plano). En futuras versiones se debe implementar hashing (bcrypt).
2. **Token JWT**: Se genera en Base64 con formato `username:role:timestamp:secret`. Válido por 24 horas.
3. **Autenticación**: Token se envía en header `Authorization: Bearer <token>`.
4. **Usuario activo**: Solo usuarios con `active=true` pueden iniciar sesión.
5. **Unicidad de username**: El username del usuario debe ser único en la tabla `users`.
6. **Logout**: Invalida el token del lado del cliente (frontend limpia localStorage); backend no requiere "lista negra" de tokens en esta versión.
7. **Roles**: Los usuarios tienen roles (ej. ADMIN, USER) que determinan permisos en el dashboard.

---

## 2. DISEÑO

### Modelos de Datos

#### Entidades Afectadas
| Entidad | Almacén | Cambios | Descripción |
|---------|---------|---------|-------------|
| `User` | tabla `users` | existente, sin cambios | Usuario con credenciales para login |
| `LoginEvent` (opcional) | tabla `login_events` | nueva, opcional | Auditoría de intentos de login |

#### Campos del modelo User (existente)
| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `id` | Long | sí | auto-generado (PK) | Identificador único |
| `username` | string | sí | max 50 chars, único | Nombre de usuario |
| `password` | string | sí | min 6 chars | Contraseña (plain text, sin hashing aún) |
| `role` | string | sí | enum: ADMIN, USER | Rol del usuario |
| `active` | boolean | sí | default=true | Usuario activo o desactivado |
| `created_at` | datetime (UTC) | sí | auto-generado | Timestamp de creación |
| `updated_at` | datetime (UTC) | sí | auto-generado | Timestamp de última actualización |

#### Índices / Constraints
- `users.username` → índice único (búsqueda frecuente en login)
- `users.active` → índice estándar (filtro en validación de sesión)

---

### API Endpoints

#### POST /api/v1/auth/login
- **Descripción**: Valida credenciales y retorna token JWT
- **Auth requerida**: no
- **Request Body**:
  ```json
  {
    "username": "string (obligatorio, max 50 chars)",
    "password": "string (obligatorio, min 6 chars)"
  }
  ```
- **Response 200 - Success**:
  ```json
  {
    "token": "Base64-encoded-JWT-token",
    "tipo": "Bearer",
    "username": "admin",
    "role": "ADMIN"
  }
  ```
- **Response 400 - Validation Error**:
  ```json
  {
    "error": "Validation failed",
    "messages": ["username es obligatorio", "password es obligatorio"]
  }
  ```
- **Response 401 - Unauthorized**:
  ```json
  {
    "error": "Unauthorized",
    "message": "Credenciales inválidas" | "Usuario no válido"
  }
  ```

#### POST /api/v1/auth/logout
- **Descripción**: Cierra la sesión del usuario (notifica al backend; limpieza real en frontend)
- **Auth requerida**: sí (header `Authorization: Bearer <token>`)
- **Request Body**: vacío `{}`
- **Response 204 - No Content**: Logout exitoso, body vacío
- **Response 401 - Unauthorized**: Token ausente, inválido o expirado
  ```json
  {
    "error": "Unauthorized",
    "message": "Token no válido o expirado"
  }
  ```

#### GET /api/v1/auth/me (opcional, para verificar token)
- **Descripción**: Retorna datos del usuario autenticado (útil para frontend)
- **Auth requerida**: sí
- **Response 200**:
  ```json
  {
    "id": 1,
    "username": "admin",
    "role": "ADMIN",
    "active": true
  }
  ```
- **Response 401**: Token inválido o expirado

---

### Diseño Frontend

#### Páginas nuevas
| Página | Archivo | Ruta | Protegida | Descripción |
|--------|---------|------|-----------|-------------|
| `LoginPage` | `pages/LoginPage.jsx` | `/login` | no | Formulario de inicio de sesión |
| `Dashboard` | `pages/Dashboard.jsx` | `/dashboard` | sí | Página principal tras login |

#### Componentes nuevos
| Componente | Archivo | Props | Descripción |
|------------|---------|-------|-------------|
| `LoginForm` | `components/LoginForm.jsx` | `onSubmit, isLoading, error` | Formulario username + password |
| `ProtectedRoute` | `components/ProtectedRoute.jsx` | `children` | Wrapper que redirige a /login si no hay token |

#### Hooks y State
| Hook | Archivo | Retorna | Descripción |
|------|---------|---------|-------------|
| `useAuth` | `hooks/useAuth.js` | `{ token, user, loading, login, logout, isAuthenticated }` | Estado centralizado de autenticación |

#### Services (llamadas API)
| Función | Archivo | Endpoint | Auth |
|---------|---------|---------|------|
| `login(username, password)` | `services/authService.js` | `POST /api/v1/auth/login` | no |
| `logout(token)` | `services/authService.js` | `POST /api/v1/auth/logout` | sí |
| `getCurrentUser(token)` | `services/authService.js` | `GET /api/v1/auth/me` | sí |

#### Flow de Autenticación Frontend
```
1. Usuario navega a /login (no autenticado) → LoginPage se renderiza
2. Usuario completa formulario y hace submit
3. LoginForm llama authService.login(username, password)
4. Backend retorna token
5. LoginForm limpia campos y usa useAuth para guardar token en Context
6. App redirige a /dashboard (ProtectedRoute valida token)
7. En Dashboard, botón "Logout" → useAuth.logout()
8. logout() limpia localStorage y redirige a /login
```

#### Storage (localStorage)
```js
// Guardar tras login exitoso
localStorage.setItem('token', loginResponse.token);
localStorage.setItem('user', JSON.stringify({ username, role }));

// Limpiar en logout
localStorage.removeItem('token');
localStorage.removeItem('user');

// Recuperar en refresh de página (validación de sesión persistente)
const token = localStorage.getItem('token');
if (token) {
  // Validar token en backend con GET /api/v1/auth/me
}
```

---

### Arquitectura y Dependencias

#### Backend
- **Dependencias nuevas**: Ninguna (Spring Security, JWT ya disponibles en Spring Boot)
- **Servicios externos**: Ninguno
- **Paquetes a crear**:
  - Verificar que exista `com.loyalty.admin.service.AuthService` (ya existe)
  - Crear LoginEvent (auditoría) si aplica
- **Impacto en punto de entrada**: Registrar endpoint `/api/v1/auth/logout` si no existe

#### Frontend
- **Dependencias nuevas**: Ninguna (Axios ya en package.json)
- **Context API**: Usar React Context para `AuthContext` (centralizar token + user data)
- **Rutas protegidas**: Componente `ProtectedRoute` debe wrappear rutas protegidas en `App.jsx`

### Notas de Implementación
> 1. **Security**: El hashing de password (bcrypt) NO se implementa en esta versión. Se usa comparación plana de strings.
> 2. **Token expiry**: Implementado en Backend (24 horas); Frontend debe manejar 401 y redirigir a login.
> 3. **Logout**: En esta versión, logout es principalmente cleanup en frontend. Backend invalida el token en futuras versiones (token blacklist).
> 4. **CORS**: Si frontend y backend están en dominios diferentes, configurar CORS en Spring Boot.
> 5. **AuthService.login()**: Ya implementado. Verificar que cumple con spec de response.
> 6. **AuthService.logout()**: Crear si no existe (puede ser simple, o registrar logout event).

---

## 3. LISTA DE TAREAS

### Backend

#### Implementación
- [x] Verificar que `AuthService.login()` valida credenciales y genera token JWT
- [x] Verificar que `AuthController.login()` mapea request/response correctamente
- [x] Implementar `AuthController.logout()` — endpoint POST /api/v1/auth/logout
- [x] Crear filtro `AuthenticationFilter` — valida token en header Authorization
- [x] Implementar `UnauthorizedException` y manejo de error 401
- [x] Crear `AuthService.getCurrentUser()` — valida token y retorna usuario (GET /api/v1/auth/me)
- [x] Arreglar endpoint path a `/api/v1/auth/login` si actualmente es `/auth/login`
- [x] Registrar endpoints en Documentation (Swagger/OpenAPI si aplica)

#### Tests Backend
- [x] `test_authService_login_success` — happy path login
- [x] `test_authService_login_invalid_password_throws_401` — password incorrecto
- [x] `test_authService_login_user_not_found_throws_401` — usuario no existe
- [x] `test_authService_login_inactive_user_throws_401` — usuario desactivado
- [x] `test_authController_login_returns_200_with_token` — endpoint HTTP
- [x] `test_authController_login_missing_username_returns_400` — validación
- [x] `test_authController_login_missing_password_returns_400` — validación
- [x] `test_authController_logout_returns_204` — logout
- [x] `test_authController_logout_without_token_returns_401` — sin auth
- [x] `test_authController_getCurrentUser_returns_200` — GET /auth/me
- [ ] `test_authFilter_valid_token_continues_request` — filtro auth
- [ ] `test_authFilter_missing_token_returns_401` — sin token en header

### Frontend

#### Implementación
- [ ] Crear `hooks/useAuth.js` — Context para autenticación (token, user, login, logout)
- [ ] Crear `services/authService.js` — funciones login, logout, getCurrentUser
- [ ] Crear `pages/LoginPage.jsx` — página con form login
- [ ] Crear `components/LoginForm.jsx` — form username + password
- [ ] Crear `components/ProtectedRoute.jsx` — wrapper para rutas protegidas
- [ ] Crear `AuthProvider` en `pages/AppProvider.jsx` — wrapper de Context
- [ ] Implementar App.jsx routing: `/login`, `/dashboard` (protegida), redirección default
- [ ] Implementar logout en header/navbar — botón que limpia sesión
- [ ] Implementar persistencia de token en localStorage
- [ ] Implementar validación de token en refresh (GET /auth/me al cargar app)
- [ ] Implementar redireccionamiento a login si token expirado (response 401)
- [ ] Estilos CSS Modules para LoginPage.module.css, LoginForm.module.css

#### Tests Frontend
- [ ] `test_LoginForm_renders_username_and_password_inputs` — render UI
- [ ] `test_LoginForm_submit_calls_login_service` — submit form
- [ ] `test_LoginForm_displays_error_message_on_401` — error handling
- [ ] `test_LoginForm_clears_form_on_success` — clear inputs
- [ ] `test_useAuth_stores_token_in_localStorage` — localStorage
- [ ] `test_useAuth_logout_removes_token_from_storage` — cleanup
- [ ] `test_ProtectedRoute_redirects_to_login_if_no_token` — protección
- [ ] `test_ProtectedRoute_renders_children_if_token_valid` — acceso
- [ ] `test_authService_login_returns_token_and_user` — service
- [ ] `test_authService_logout_calls_backend` — service

### QA (Integración)

#### Test Cases Gherkin
- [ ] Scenario: Happy path login → token generado, redirect dashboard
- [ ] Scenario: Invalid password → 401, error message, redirect login
- [ ] Scenario: Missing fields → validation error, form not submitted
- [ ] Scenario: Logout button → token elimina, redirect login
- [ ] Scenario: Stale token → 401 en cualquier request, force login
- [ ] Scenario: Simultaneous logins — múltiples usuarios

#### Performance
- [ ] <=200ms en login endpoint (sin caídas de DB)
- [ ] <=100ms en logout endpoint
- [ ] <=50ms en validación de token (GET /auth/me)

#### Seguridad
- [ ] Token no se expone en logs o console (XSS risk)
- [ ] localStorage usado (no sessionStorage) para persistencia
- [ ] CORS configurado correctamente (no acepta * en producción)
- [ ] Password no viaja en plaintext en respuesta (nunca retornar password)
- [ ] Password en DB se compara como plaintext (será hashing en v2)

---

## Anexo: Dependencias Existentes

| Componente | Estado | Observaciones |
|------------|--------|---------------|
| `User` entity | ✅ Existe | Tabla `users` con id, username, password, role, active |
| `UserRepository` | ✅ Existe | JPA repository con método `findByUsername()` |
| `AuthService.login()` | ✅ Existe | Genera token Base64 (24h validity) |
| `AuthController` | ✅ Existe | Endpoint POST /auth/login mapeado |
| `AuthService.logout()` | ✅ Implementado | Endpoint POST /auth/logout funcional |
| `Frontend LoginPage` | ❌ No existe | Implementar |
| `Frontend useAuth hook` | ❌ No existe | Crear Context API |
| `ProtectedRoute` | ❌ No existe | Crear componente protección |

---

## Ciclo de Vida de la Spec

```
[DRAFT] ← Revisi crítica y aprobación
   ↓
[APPROVED] ← Listo para implementación
   ↓
[IN_PROGRESS] ← Backend + Frontend en desarrollo (paralelo)
   ↓
[IMPLEMENTED] ← Código completo, tests pasados
   ↓
[DEPRECATED] ← (futuro, si se reemplaza por nueva versión)
```
