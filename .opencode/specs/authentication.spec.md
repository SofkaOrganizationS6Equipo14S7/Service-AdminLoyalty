---
id: SPEC-001
status: DRAFT
feature: authentication-management
created: 2026-04-05
updated: 2026-04-05
author: spec-generator
version: "1.0"
related-specs: []
---

# Spec: Authentication Management (HU-01)

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de iniciar implementación.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPRECATED

> **Nota Importante:** La mayoría de esta HU ya está implementada en el backend. Solo falta completar el **Error Response Estándar** con el campo `details` para validaciones detalladas.

---

## 1. REQUERIMIENTOS

### Descripción
Implementar un sistema seguro de autenticación stateless basado en JWT (RFC 7519) con hasheado de contraseñas mediante BCrypt. Los administradores podrán iniciar y cerrar sesión en el dashboard para gestionar las reglas de negocio mediante tokens de identidad. El sistema debe proporcionar respuestas de error consistentes y detalladas en todos los endpoints.

### Requerimiento de Negocio
Como Administrador, quiero iniciar y cerrar sesión en el Dashboard, para gestionar las reglas de negocio de forma segura mediante un token de identidad. El sistema debe validar credenciales, manejar errores consistentemente y proporcionar mensajes de error específicos y detallados.

### Historias de Usuario

#### HU-01: Autenticación Segura (Login/Logout)

```
Como:        Administrador del sistema
Quiero:      Iniciar sesión con credenciales y recibir un JWT válido; también cerrar sesión
Para:        Gestionar las reglas de negocio de forma segura mediante tokens de identidad

Prioridad:   Alta
Estimación:  M
Dependencias: Ninguna
Capa:        Backend
```

#### Criterios de Aceptación — HU-01

**CRITERIO-1.1: Login exitoso con credenciales válidas**
```gherkin
Dado que:      usuario registrado con username "admin@loyalty.com" y contraseña válida en base de datos
Cuando:        envía POST /api/v1/auth/login con username y password correcto
Entonces:      el sistema retorna 200 OK
Y:             la respuesta contiene un JWT válido (token con claims userId y roles)
Y:             el JWT puede ser decodificado y contiene los claims: sub, userId, role, uid, iat, exp
```

**CRITERIO-1.2: Login rechazado por credenciales inválidas**
```gherkin
Dado que:      usuario intenta login con contraseña incorrecta o username no existe
Cuando:        envía POST /api/v1/auth/login
Entonces:      el sistema retorna 401 Unauthorized
Y:             la respuesta contiene mensaje "Invalid credentials"
Y:             la respuesta NO expone detalles sobre cuál campo es incorrecto (por seguridad)
```

**CRITERIO-1.3: Login rechazado por campos obligatorios faltantes**
```gherkin
Dado que:      usuario intenta login sin username y/o sin password
Cuando:        envía POST /api/v1/auth/login con campos vacíos, nulos o incompletos
Entonces:      el sistema retorna 400 Bad Request
Y:             la respuesta contiene un error de validación con detalles: field, message
Y:             el campo details[] lista cada validación fallida:
                - {"field": "username", "message": "username es obligatorio"}
                - {"field": "password", "message": "password es obligatorio"}
```

**CRITERIO-1.4: Logout exitoso**
```gherkin
Dado que:      usuario autenticado con JWT válido activo
Cuando:        envía POST /api/v1/auth/logout
Entonces:      el sistema retorna 204 No Content
Y:             el cliente debe remover el JWT del storage local
```

**CRITERIO-1.5: Acceso sin token**
```gherkin
Dado que:      endpoint protegido requiere autenticación
Cuando:        usuario envía request sin token en header Authorization
Entonces:      el sistema retorna 401 Unauthorized
Y:             la respuesta contiene mensaje "Missing or invalid token"
```

**CRITERIO-1.6: Token expirado o inválido**
```gherkin
Dado que:      usuario tiene un JWT expirado o manipulado
Cuando:        envía request a endpoint protegido
Entonces:      el sistema retorna 401 Unauthorized
Y:             la respuesta contiene mensaje "Invalid or expired token"
Y:             el cliente debe remover el JWT y redirigir a login (frontend)
```

### Reglas de Negocio
1. **RN-001:** Las contraseñas deben hashearse con BCrypt (strength = 11, balance óptimo seguridad/performance)
2. **RN-002:** Los tokens JWT deben tener expiración de 1 hora (configurable en application.properties)
3. **RN-003:** Solo usuarios con `is_active = true` pueden autenticarse
4. **RN-004:** Los JWT deben incluir claims: `sub` (username), `userId`, `role`, `uid`, `ecommerce_id`, `iat`, `exp`
5. **RN-005:** Todas las respuestas de error deben seguir el formato estándar: `{ timestamp, status, error, code, message, path, details (opcional) }`
6. **RN-006:** Las validaciones de entrada deben retornar detalles específicos en `details[]` para ayudar al cliente a identificar qué campos son incorrectos
7. **RN-007:** El sistema debe ser stateless; no usar HttpSession. Todo se maneja vía header Authorization: Bearer {token}
8. **RN-008:** La contraseña no debe exponerse en logs ni en respuestas de error

---

## 2. DISEÑO

### Modelos de Datos

#### Entidades afectadas
| Entidad | Almacén | Cambios | Descripción |
|---------|---------|---------|-------------|
| `UserEntity` | tabla `users` | ya existe | Usuario del sistema con credenciales y estado activo |
| `RoleEntity` | tabla `roles` | ya existe | Rol asociado al usuario (ADMIN, USER, etc.) |

#### Campos del modelo UserEntity (existente)
| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `id` | UUID | sí | auto-generado | Identificador único |
| `username` | string | sí | unique, email format | Email del usuario |
| `password_hash` | string | sí | BCrypt hashed | Contraseña hasheada |
| `is_active` | boolean | sí | default true | Usuario activo o desactivado |
| `role_id` | UUID (FK) | sí | referencia a roles | Rol del usuario |
| `ecommerce_id` | UUID | sí | multi-tenant | ID de la tienda/empresa |
| `created_at` | timestamp | sí | auto-generado UTC | Timestamp creación |
| `updated_at` | timestamp | sí | auto-generado UTC | Timestamp actualización |

#### Índices / Constraints
- Índice en `username` — búsqueda frecuente en login
- Índice en `ecommerce_id` — filtrado multi-tenant
- Índice en `is_active` — validación de usuarios activos
- FK en `role_id` → tabla `roles`

#### DTOs y Estructuras de Respuesta (existentes y nuevas)

**LoginRequest:**
```java
record LoginRequest(
    @NotBlank(message = "username es obligatorio")
    String username,
    
    @NotBlank(message = "password es obligatorio")
    String password
) {}
```

**LoginResponse:**
```java
record LoginResponse(
    String token,
    String tipo,      // siempre "Bearer"
    String username,
    String role
) {}
```

**UserResponse:**
```java
record UserResponse(
    String uid,
    String username,
    String roleId,
    String roleName,
    String email,
    String ecommerceId,
    Boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

**ApiErrorResponse (MODIFICADA - agregar `details`):**
```java
record ApiErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String code,
    String message,
    String path,
    @Nullable
    List<ErrorDetail> details  // ← NUEVO: detalles de validación
) {}

record ErrorDetail(
    String field,
    String message
) {}
```

**Ejemplos de Respuestas:**

- **Login exitoso (200 OK):**
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tipo": "Bearer",
    "username": "admin@loyalty.com",
    "role": "ADMIN"
  }
  ```

- **Credenciales inválidas (401 Unauthorized):**
  ```json
  {
    "timestamp": "2026-04-05T10:30:00Z",
    "status": 401,
    "error": "Unauthorized",
    "code": "INVALID_CREDENTIALS",
    "message": "Invalid credentials",
    "path": "/api/v1/auth/login",
    "details": null
  }
  ```

- **Validación fallida (400 Bad Request):**
  ```json
  {
    "timestamp": "2026-04-05T10:30:00Z",
    "status": 400,
    "error": "Bad Request",
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "path": "/api/v1/auth/login",
    "details": [
      {"field": "username", "message": "username es obligatorio"},
      {"field": "password", "message": "password es obligatorio"}
    ]
  }
  ```

- **Token inválido/expirado (401 Unauthorized):**
  ```json
  {
    "timestamp": "2026-04-05T10:30:00Z",
    "status": 401,
    "error": "Unauthorized",
    "code": "INVALID_TOKEN",
    "message": "Invalid or expired token",
    "path": "/api/v1/admin/rules",
    "details": null
  }
  ```

### API Endpoints

#### POST /api/v1/auth/login
- **Descripción**: Autentica un usuario y retorna un JWT válido
- **Auth requerida**: no
- **Request Body**:
  ```json
  { "username": "admin@loyalty.com", "password": "securePassword123" }
  ```
- **Response 200 OK**:
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tipo": "Bearer",
    "username": "admin@loyalty.com",
    "role": "ADMIN"
  }
  ```
- **Response 400 Bad Request**: Validación fallida (campos obligatorios faltantes)
  - Code: `VALIDATION_ERROR`
  - Incluye array `details[]` con cada campo inválido
- **Response 401 Unauthorized**: Credenciales inválidas o usuario inactivo
  - Code: `INVALID_CREDENTIALS`
  - Mensaje: "Invalid credentials" (sin detalles por seguridad)

#### POST /api/v1/auth/logout
- **Descripción**: Finaliza la sesión del usuario (client-side token removal)
- **Auth requerida**: sí (header Authorization: Bearer {token})
- **Request Body**: vacío
- **Response 204 No Content**: Logout exitoso
- **Response 401 Unauthorized**: Token inválido o expirado

#### GET /api/v1/auth/me
- **Descripción**: Retorna datos del usuario autenticado actual
- **Auth requerida**: sí
- **Response 200 OK**: UserResponse con datos del usuario actual
- **Response 401 Unauthorized**: Token faltante, expirado o inválido

### Diseño Frontend

#### Componentes nuevos

| Componente | Archivo | Props principales | Descripción |
|------------|---------|------------------|-------------|
| `LoginForm` | `components/LoginForm.jsx` | `onSubmit, isLoading, error` | Formulario con campos username, password y botón de envío |
| `LogoutButton` | `components/LogoutButton.jsx` | `onClick, isLoading` | Botón para cerrar sesión |
| `AuthGuard` | `components/AuthGuard.jsx` | `children, redirectTo` | Componente de protección de rutas (redirige a login si no está autenticado) |
| `ErrorAlert` | `components/ErrorAlert.jsx` | `error, details, onDismiss` | Mostrar errores con detalles de validación |

#### Páginas nuevas

| Página | Archivo | Ruta | Protegida |
|--------|---------|------|-----------|
| `LoginPage` | `pages/LoginPage.jsx` | `/login` | no |
| `DashboardPage` | `pages/DashboardPage.jsx` | `/dashboard` | sí |

#### Hooks y State (Context o libería de estado)

| Hook | Archivo | Retorna | Descripción |
|------|---------|---------|-------------|
| `useAuth()` | `hooks/useAuth.js` | `{ user, token, isLoading, error, login, logout }` | Manejo global de autenticación (Context o Zustand) |
| `useAuthForm()` | `hooks/useAuthForm.js` | `{ formData, errors, handleChange, handleSubmit, isLoading }` | Estado local del formulario de login |

#### Services (llamadas API)

| Función | Archivo | Endpoint | Descripción |
|---------|---------|---------|-------------|
| `loginUser(username, password)` | `services/authService.js` | `POST /api/v1/auth/login` | Envía credenciales y obtiene JWT |
| `logoutUser(token)` | `services/authService.js` | `POST /api/v1/auth/logout` | Cierra sesión |
| `getCurrentUser(token)` | `services/authService.js` | `GET /api/v1/auth/me` | Obtiene datos del usuario actual |
| `isTokenValid(token)` | `services/authService.js` | (local) | Valida token en cliente (decode sin validar firma) |

#### Flujo de Autenticación (Frontend)

1. Usuario llega a `/login`
2. LoginForm captura `username` y `password`
3. Al hacer submit:
   - Envía `POST /api/v1/auth/login`
   - Si 200: guarda token en localStorage y redirige a `/dashboard`
   - Si 400: muestra errores de validación con detalles en ErrorAlert
   - Si 401: muestra "Credenciales inválidas"
4. AuthGuard protege rutas privadas (ej. `/dashboard`):
   - Verifica token en localStorage
   - Si válido: renderiza componente protegido
   - Si inválido/expirado: redirige a `/login`
5. Logout: limpia localStorage, token context y redirige a `/login`

### Arquitectura y Dependencias

#### Backend

**Paquetes/Clases sobre los que trabaja:**
- `com.loyalty.service_admin.infrastructure.security.JwtProvider` — generación/validación JWT (ya existe)
- `com.loyalty.service_admin.infrastructure.security.SecurityConfiguration` — config de seguridad (ya existe)
- `com.loyalty.service_admin.infrastructure.config.AuthenticationFilter` — filtro de JWT (ya existe)
- `com.loyalty.service_admin.infrastructure.exception.GlobalExceptionHandler` — manejo global de excepciones (ya existe, **requiere mejora**)
- `com.loyalty.service_admin.domain.entity.UserEntity` — modelo de usuario (ya existe)
- `com.loyalty.service_admin.domain.repository.UserRepository` — acceso a datos (ya existe)
- `com.loyalty.service_admin.application.service.AuthService` — lógica de autenticación (ya existe)
- `com.loyalty.service_admin.presentation.controller.AuthController` — endpoints de auth (ya existe)

**Cambios requeridos en Backend:**
1. **Modificar `ApiErrorResponse`** — agregar campo `details: List<ErrorDetail>` (nullable)
2. **Mejorar `GlobalExceptionHandler.handleMethodArgumentNotValid()`** — extraer errores de validación a array `details[]` en lugar de concatenar en string
3. **Mantener intacto** — resto de components (no requieren cambios)

#### Frontend

**Paquetes/Librerías:**
- `react` — framework UI
- `react-router-dom` — routing y protección de rutas (AuthGuard)
- `axios` — cliente HTTP para llamadas a API
- `zustand` o `useContext` — state management para autenticación global
- `react-hook-form` — gestión de formularios (opcional, pero recomendado)

**Nuevos componentes/hooks/services** — ver sección "Diseño Frontend" arriba

#### Servicios externos
- Ninguno (autenticación completamente self-contained)

#### Dependencias de Base de Datos
- Tabla `users` ya existe con estructura correcta
- Tabla `roles` ya existe
- Índices ya están creados

### Notas de Implementación

> ⚠️ **IMPORTANTE:** Esta HU está 95% implementada. El único trabajo pendiente es:
> 1. Agregar campo `details` a `ApiErrorResponse` en backend
> 2. Mejorar `GlobalExceptionHandler.handleMethodArgumentNotValid()` para extraer detalles

> **Frontend:** Aunque el backend está listo, el frontend debe ser completamente implementado desde cero. No existe ningún componente de login.

> **Security Notes:**
> - Los JWT se validan con HMAC-SHA256
> - Las contraseñas están hasheadas con BCrypt (strength 11)
> - No se implementa token blacklist en esta fase (recomendación futura: Redis + TTL)
> - No se implementa rate limiting en /login en esta fase (recomendación futura: agregar filtro de rate limit)
> - El header Authorization debe estar presente en todas las request a endpoints protegidos

> **Testing:** Priorizar tests sobre validación de credenciales, manejo de errores y protección de rutas.

---

## 3. LISTA DE TAREAS

> Checklist accionable para todos los agentes. Marcar cada ítem (`[x]`) al completarlo.
> El Orchestrator monitorea este checklist para determinar el progreso.

### Backend

#### Implementación
- [x] Crear registros de usuario en base de datos con credenciales hasheadas
- [x] Implementar `JwtProvider` — generación/validación de JWT con HS256
- [x] Implementar `SecurityConfiguration` — configuración de seguridad
- [x] Implementar `AuthenticationFilter` — extrae token y crea UserPrincipal
- [x] Implementar `AuthService` — lógica de login, logout, getCurrentUser
- [x] Implementar `AuthController` — endpoints /api/v1/auth/{login, logout, me}
- [x] Implementar `UserEntity` y `UserRepository` — persistencia
- [x] BCrypt hasheado para contraseñas (strength 11)
- [ ] **PENDIENTE:** Agregar campo `details` a `ApiErrorResponse` — para errores de validación
- [ ] **PENDIENTE:** Mejorar `GlobalExceptionHandler.handleMethodArgumentNotValid()` — extraer detalles a array
- [x] Existencia de `GlobalExceptionHandler` con 10+ excepciones mapeadas

#### Tests Backend — Esencial y Crítico
- [ ] `test_auth_login_success_returns_jwt_token` — login exitoso retorna 200 + token válido
- [ ] `test_auth_login_invalid_credentials_returns_401` — credenciales inválidas retorna 401
- [ ] `test_auth_login_missing_fields_returns_400` — campos vacíos retorna 400 con detalles
- [ ] `test_auth_login_missing_fields_includes_validation_details` — error 400 incluye array `details[]`
- [ ] `test_auth_logout_success_returns_204` — logout retorna 204
- [ ] `test_auth_me_returns_current_user` — /me retorna datos del usuario autenticado
- [ ] `test_auth_me_no_token_returns_401` — sin token retorna 401
- [ ] `test_jwt_generation_includes_required_claims` — JWT incluye sub, userId, role, uid
- [ ] `test_jwt_validation_rejects_invalid_token` — token corrompido es rechazado
- [ ] `test_bcrypt_password_validation_success` — contraseña correcta valida exitosamente
- [ ] `test_bcrypt_password_validation_failure` — contraseña incorrecta falla

### Frontend

#### Implementación — Nuevos componentes
- [ ] Crear `services/authService.js` — `loginUser()`, `logoutUser()`, `getCurrentUser()`
- [ ] Crear `hooks/useAuth.js` — Context/Hook global con estado de autenticación
- [ ] Crear `components/LoginForm.jsx` — formulario con validación local
- [ ] Crear `components/ErrorAlert.jsx` — mostrar errores con detalles de validación
- [ ] Crear `components/AuthGuard.jsx` — componente para proteger rutas privadas
- [ ] Crear `components/LogoutButton.jsx` — botón para logout
- [ ] Crear `pages/LoginPage.jsx` — página de login (ruta `/login`)
- [ ] Crear `pages/DashboardPage.jsx` — página protegida (ruta `/dashboard`)
- [ ] Registrar rutas en router:
  - `/login` → LoginPage (pública)
  - `/dashboard` → DashboardPage (protegida con AuthGuard)
- [ ] Implementar localStorage.setItem('token') en login exitoso
- [ ] Implementar localStorage.removeItem('token') en logout
- [ ] Redirigir a `/dashboard` después de login exitoso
- [ ] Redirigir a `/login` si token expirado en cualquier página protegida

#### Tests Frontend — Esencial y Crítico
- [ ] `test_LoginForm_submits_with_username_and_password` — formulario envía credenciales
- [ ] `test_LoginForm_shows_validation_errors_from_server` — muestra errores con detalles
- [ ] `test_LoginForm_displays_error_alert_with_details` — ErrorAlert renderiza array `details[]`
- [ ] `test_authService_loginUser_success_returns_token` — loginUser() retorna token
- [ ] `test_authService_loginUser_error_returns_error_object` — error incluye `details[]`
- [ ] `test_useAuth_stores_token_in_localStorage` — token guardado en localStorage
- [ ] `test_useAuth_logout_removes_token` — logout limpia localStorage
- [ ] `test_AuthGuard_redirects_to_login_if_no_token` — sin token redirige a /login
- [ ] `test_AuthGuard_renders_children_if_token_valid` — token válido renderiza componente
- [ ] `test_LoginPage_renders_LoginForm` — página de login carga el formulario

### QA/Testing Integration

#### Estrategia de Testing
- [ ] Ejecutar skill `/risk-identifier` → clasificación ASD de riesgos (esperar que termine backend)
- [ ] Ejecutar skill `/gherkin-case-generator` → criterios CRITERIO-1.1 a 1.6 + datos de prueba
- [ ] Revisar cobertura de tests contra todos los criterios de aceptación
- [ ] Validar que todos los escenarios felices, errores y edge cases están cubiertos
- [ ] Prueba manual de login/logout en navegador (token persistencia)
- [ ] Prueba manual de redirecciones de AuthGuard
- [ ] Validar mensajes de error detallados en frontend

#### Actualización de Status
- [ ] Backend: marcar como IMPLEMENTED una vez completados los 2 ítems pendientes
- [ ] Frontend: marcar como IMPLEMENTED una vez completados todos los componentes y tests
- [ ] Spec: actualizar a `status: IMPLEMENTED` cuando TODO está completo

---

## Resumen de Estado

### Completitud Actual
- **Backend:** ~95% (FALTA: campo `details` en ApiErrorResponse)
- **Frontend:** 0% (TODO POR IMPLEMENTAR)
- **Spec:** DRAFT (requiere aprobación antes de iniciar trabajo)

### Próximos Pasos en Orden
1. ✅ Revisar y aprobar esta spec (`status: APPROVED`)
2. 📝 Backend Developer: Agregar campo `details` a `ApiErrorResponse` + tests
3. 🎨 Frontend Developer: Implementar todos los componentes de login
4. 🧪 Test Engineers: Generar suite de tests (unittest + gherkin)
5. 🔍 QA Agent: Ejecutar `/gherkin-case-generator` y `/risk-identifier`
6. 📚 (Opcional) Documentation Agent: Actualizar README con instrucciones de autenticación
