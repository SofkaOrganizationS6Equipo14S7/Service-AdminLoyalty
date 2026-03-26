---
id: SPEC-001
status: APPROVED
feature: login-logout
created: 2026-03-26
updated: 2026-03-26
author: spec-generator
version: "1.1"
related-specs: []
---

# Spec: Auth Management (Login & Logout)

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de iniciar implementación.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPRECATED

---

## 1. REQUERIMIENTOS

### Descripción
Implementación de un sistema de autenticación seguro basado en JWT que permita a Administradores iniciar sesión en el Dashboard con credenciales válidas y cerrar sesión de forma segura. Las contraseñas se almacenan hasheadas con BCrypt y los tokens se generan con clave simétrica (stateless, sin sesión HTTP).

### Requerimiento de Negocio
Como Administrador, quiero iniciar y cerrar sesión en el Dashboard, para gestionar las reglas de negocio de forma segura mediante un token de identidad.

**Constraints técnicos:**
- Service: loyalty-admin (Puerto 8081)
- Security: Contraseñas hasheadas con BCrypt
- JWT: Generación usando clave secreta compartida (Symmetric)
- Stateless: No usar HttpSession. Todo se maneja vía Header `Authorization: Bearer {token}`
- Output: El login retorna JSON con token y fecha de expiración

### Historias de Usuario

#### HU-01: Inicio de sesión exitoso

```
Como:        Administrador
Quiero:      iniciar sesión con mis credenciales (username + password)
Para:        acceder al Dashboard y gestionar la configuración de forma segura

Prioridad:   Alta
Estimación:  M
Dependencias: Ninguna
Capa:        Backend + Frontend
```

##### Criterios de Aceptación — HU-01

**Happy Path**
```gherkin
CRITERIO-1.1: Login exitoso con credenciales válidas
  Dado que:    un Usuario está registrado en el sistema con username y password válidos
  Cuando:      envía una solicitud POST a /api/v1/auth/login con username y password válidos
  Entonces:    el sistema retorna status code 200 OK
  Y:           la respuesta contiene un token JWT válido (formato Bearer)
  Y:           la respuesta incluye el username y rol del Usuario
  Y:           el token es válido por 24 horas desde su emisión
  Y:           el log registra el login exitoso con timestamp
```

**Error Path — Credenciales inválidas**
```gherkin
CRITERIO-1.2: Login rechazado con credenciales inválidas
  Dado que:    un Usuario intenta autenticarse
  Cuando:      envía credenciales incorrectas (password erróneo o username no existe)
  Entonces:    el sistema retorna status code 401 Unauthorized
  Y:           la respuesta contiene el mensaje "Credenciales inválidas"
  Y:           no se genera token
  Y:           el log registra el intento fallido (warning)
```

**Error Path — Campos obligatorios**
```gherkin
CRITERIO-1.3: Login rechazado por validación de campos
  Dado que:    un Usuario intenta autenticarse
  Cuando:      envía una solicitud sin username, sin password o ambos vacíos
  Entonces:    el sistema retorna status code 400 Bad Request
  Y:           la respuesta detalla qué campos son obligatorios (validación Bean Validation)
  Y:           no se consulta la base de datos
```

**Error Path — Usuario inactivo**
```gherkin
CRITERIO-1.4: Login rechazado para Usuario desactivado
  Dado que:    un Usuario está registrado pero su estado es `active = false`
  Cuando:      intenta hacer login con credenciales válidas
  Entonces:    el sistema retorna status code 401 Unauthorized
  Y:           la respuesta indica"Usuario no válido"
  Y:           no se genera token
```

---

#### HU-02: Cierre de sesión (Logout)

```
Como:        Administrador
Quiero:      cerrar sesión desde el Dashboard
Para:        garantizar que mi token sea invalidado y no pueda usarse para acceso posterior

Prioridad:   Alta
Estimación:  S
Dependencias: HU-01
Capa:        Backend + Frontend
```

##### Criterios de Aceptación — HU-02

**Happy Path**
```gherkin
CRITERIO-2.1: Logout exitoso con token válido
  Dado que:    un Usuario está autenticado (posee un token JWT válido)
  Cuando:      envía una solicitud POST a /api/v1/auth/logout con su token en header Authorization
  Entonces:    el sistema retorna status code 204 No Content
  Y:           el token es invalidado (v1: registro en log; v2: en blacklist)
  Y:           el log registra el logout exitoso
```

**Edge Case**
```gherkin
CRITERIO-2.2: Logout sin token (siempre exitoso)
  Dado que:    un Usuario intenta hacer logout sin token o con token inválido
  Cuando:      envía una solicitud POST a /api/v1/auth/logout sin Authorization header
  Entonces:    el sistema retorna status code 204 No Content (no error)
  Y:           el log registra el intento de logout sin token (info)
  Y:           no se genera excepción
```

---

#### HU-03: Obtención de datos del Usuario autenticado

```
Como:        Administrador
Quiero:      consultar mis datos (username, rol, timestamps) a partir de mi token
Para:        validar que mi sesión es activa y obtener información de mi perfil

Prioridad:   Media
Estimación:  S
Dependencias: HU-01
Capa:        Backend
```

##### Criterios de Aceptación — HU-03

**Happy Path**
```gherkin
CRITERIO-3.1: Obtener Usuario autenticado exitosamente
  Dado que:    un Usuario posee un token JWT válido y no expirado
  Cuando:      envía una solicitud GET a /api/v1/auth/me con su token en header Authorization
  Entonces:    el sistema retorna status code 200 OK
  Y:           la respuesta contiene: id, username, rol, active, created_at, updated_at del Usuario
  Y:           el log registra la consulta exitosa
```

**Error Path — Token inválido**
```gherkin
CRITERIO-3.2: Rechazar acceso sin token válido
  Dado que:    un Usuario intenta acceder sin token, con token malformado o expirado
  Cuando:      envía GET a /api/v1/auth/me sin Authorization header o con token inválido
  Entonces:    el sistema retorna status code 401 Unauthorized
  Y:           la respuesta indica "Token no válido o expirado"
```

**Error Path — Usuario desactivado**
```gherkin
CRITERIO-3.3: Rechazar acceso si el Usuario fue desactivado después de emitir token
  Dado que:    un Usuario tenía un token válido pero su estado fue cambiado a `active = false`
  Cuando:      envía GET a /api/v1/auth/me con su token anterior
  Entonces:    el sistema retorna status code 401 Unauthorized
  Y:           la respuesta indica "Usuario desactivado"
```

---

### Reglas de Negocio

1. **RN-01 Validación de campos obligatorios:** username y password son obligatorios. Si falta alguno, retornar 400 Bad Request con detalle de validación.

2. **RN-02 Unicidad de username:** No se pueden crear dos Usuarios con el mismo username. Este constraint está en la BD (índice única).

3. **RN-03 Hash de password:** Toda contraseña se almacena hasheada usando BCrypt (con salt aleatorio). Nunca se almacenan en texto plano.

4. **RN-04 Validación de password:** La comparación se realiza usando BCrypt.checkpw() para evitar ataques de timing.

5. **RN-05 JWT real con librería jjwt (CRÍTICA):** El token se genera usando la librería `io.jsonwebtoken:jjwt` (RFC 7519) con clave simétrica (HMAC-SHA256) almacenada en `app.jwt.secret` en application.yml. Estructura: `Header.Payload.Signature`. El Payload contiene claims: `username`, `userId`, `role`, `iat` (issued_at), `exp` (expires_at). NUNCA usar home-made tokens (ej. Base64 casero). La firma garantiza integridad.

6. **RN-06 Expiración de token:** El token expira en 24 horas (86,400,000 ms) desde su emisión (claim `exp`). Después de que vence, no es válido. Validar con `jjwt.parseClaimsJws(token).getBody().getExpiration()`.

7. **RN-07 Usuario activo requerido:** Solo un Usuario con `active = true` puede iniciar sesión. Si está desactivado, retornar 401.

8. **RN-08 Logout stateless con validación (v1):** En v1, el POST /api/v1/auth/logout valida el token recibido en el header Authorization ANTES de disparar el log. Si el token es válido, extrae el username y registra el logout exitoso con el usuario identificado. Si el token es inválido o falta, responde con 204 igual (no error) pero no registra usuario. En v2 (future), el token será añadido a una blacklist en caché (Redis/Caffeine) para bloquear tokens antes de expiración.

9. **RN-09 Header Authorization requerido:** Endpoints que requieren auth (`GET /api/v1/auth/me`, `POST /api/v1/auth/logout`) validan header `Authorization: Bearer <token>`.

10. **RN-10 Logs de eventos de seguridad:** Toda acción de login/logout genera un log (exitosa o fallida) para auditoría.

11. **RN-11 BCrypt Strength (CRÍTICA):** El PasswordEncoder debe usar `new BCryptPasswordEncoder(strength)` con strength entre 10 y 12 (inclusive). Strength < 10 es vulnerable a fuerza bruta; strength > 12 puede causar DoS involuntario bajo carga al consumir excesiva CPU durante login. Recomendado: strength = 10 o 11.

---

## 2. DISEÑO

### Modelos de Datos

#### Entidades afectadas
| Entidad | Almacén | Cambios | Descripción |
|---------|---------|---------|-------------|
| `UserEntity` | tabla `users` | ya existe | Usuario con id, username, password (hash BCrypt), role, active, created_at, updated_at |

#### Campos del modelo UserEntity (ya existe)
| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `id` | Long (auto-increment) | sí | auto-generado | Identificador único de BD |
| `username` | String (50 chars) | sí | unique, not null | Nombre de usuario único |
| `password` | String (255 chars) | sí | not null | Contraseña hasheada con BCrypt |
| `role` | String (50 chars) | sí | not null | Rol del usuario (ej. "ADMIN") |
| `active` | Boolean | sí | default true | Estado del usuario |
| `created_at` | Instant (UTC) | sí | auto-generado | Timestamp creación |
| `updated_at` | Instant (UTC) | sí | auto-generado | Timestamp actualización |

#### Índices / Constraints
- **idx_username**: Índice único en columna `username` (búsqueda rápida y garantía de unicidad)
- **idx_active**: Índice en columna `active` (filtrado de usuarios activos)
- **PRIMARY KEY**: `id`

---

### API Endpoints

#### POST /api/v1/auth/login
- **Descripción**: Autentica un Usuario con credenciales y genera token JWT
- **Auth requerida**: no
- **Request Body**:
  ```json
  {
    "username": "string (obligatorio, max 50 caracteres)",
    "password": "string (obligatorio)"
  }
  ```
- **Response 200 OK**:
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tipo": "Bearer",
    "username": "string",
    "role": "string (ej. ADMIN)"
  }
  ```
- **Response 400 Bad Request** (campos faltantes o inválidos):
  ```json
  {
    "timestamp": "2026-03-26T15:30:45Z",
    "status": 400,
    "error": "Bad Request",
    "message": "username es obligatorio",
    "path": "/api/v1/auth/login"
  }
  ```
- **Response 401 Unauthorized** (credenciales inválidas o usuario inactivo):
  ```json
  {
    "timestamp": "2026-03-26T15:30:45Z",
    "status": 401,
    "error": "Unauthorized",
    "message": "Credenciales inválidas",
    "path": "/api/v1/auth/login"
  }
  ```
- **Validaciones en Controller**:
  - Usar `@Valid @RequestBody LoginRequest` para validación automática (Bean Validation)
  - LoginRequest contiene anotaciones `@NotBlank` en cada campo
- **Logs generados**:
  - `log.debug("Intento de login para usuario: {}", username)` al recibir request
  - `log.warn("Usuario no encontrado")` si username no existe
  - `log.warn("Password incorrecto")` si BCrypt.checkpw() retorna false
  - `log.info("Login exitoso para usuario: {}", username)` si todo es válido

#### POST /api/v1/auth/logout
- **Descripción**: Invalida la sesión del Usuario (logout)
- **Auth requerida**: sí (header `Authorization: Bearer <token>`)
- **Request Header**:
  ```
  Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
  ```
- **Request Body**: vacío
- **Response 204 No Content**: siempre exitoso (incluso si no hay token o es inválido)
- **Comportamiento**:
  - Si Authorization header presente y token válido: log info "Logout exitoso para usuario: X"
  - Si Authorization header ausente o token inválido: log warning "Logout con token inválido" (sin excepción)
  - En ambos casos retorna 204 (stateless)
- **Logs generados**:
  - `log.debug("Logout realizado...")` al recibir request
  - `log.info("Logout exitoso para usuario: {}", username)` si token es válido
  - `log.warn("Logout con token inválido")` si hay error

#### GET /api/v1/auth/me
- **Descripción**: Obtiene datos del Usuario autenticado
- **Auth requerida**: sí (header `Authorization: Bearer <token>`)
- **Request Header**:
  ```
  Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
  ```
- **Response 200 OK**:
  ```json
  {
    "id": 1,
    "username": "admin",
    "role": "ADMIN",
    "active": true,
    "createdAt": "2026-01-15T10:30:00Z",
    "updatedAt": "2026-03-26T12:00:00Z"
  }
  ```
- **Response 401 Unauthorized** (token faltante, inválido o expirado):
  ```json
  {
    "timestamp": "2026-03-26T15:30:45Z",
    "status": 401,
    "error": "Unauthorized",
    "message": "Token no válido o expirado",
    "path": "/api/v1/auth/me"
  }
  ```
- **Validaciones**:
  - Extraer token del header Authorization (eliminar prefijo "Bearer ")
  - Decodificar Base64
  - Validar formato y secret
  - Validar no-expiración (< 24 horas)
  - Buscar Usuario por username en BD
  - Validar que Usuario siga activo
- **Logs generados**:
  - `log.debug("Solicitando datos del usuario...")` al recibir request
  - `log.warn("Token expirado para usuario: X")` si edad > 24h
  - `log.info("Usuario actual retornado: {}", username)` si exitoso

---

### DTOs (Java Records)

#### LoginRequest (ya existe)
```java
public record LoginRequest(
    @NotBlank(message = "username es obligatorio")
    String username,
    
    @NotBlank(message = "password es obligatorio")
    String password
) {}
```

#### LoginResponse (ya existe)
```java
public record LoginResponse(
    String token,
    String tipo,
    String username,
    String role
) {}
```

#### UserResponse (ya existe)
```java
public record UserResponse(
    Long id,
    String username,
    String role,
    Boolean active,
    Instant createdAt,
    Instant updatedAt
) {}
```

---

### Componentes Frontend (esbozo)

**NOTA:** El frontend no está incluido en este scope actual. Mención de componentes esperados para FE:

| Componente | Pathname | Props/Hooks | Descripción |
|------------|----------|------------|-------------|
| `LoginForm` | `components/LoginForm.jsx` | `onSubmit, loading, error` | Formulario con campos username, password |
| `useAuth` | `hooks/useAuth.js` | `{ user, token, login, logout, loading, error }` | Hook personalizado de auth |
| `authService` | `services/authService.js` | `login(username, password), logout(), getCurrentUser(token)` | Llamadas AJAX al backend |
| `ProtectedRoute` | `components/ProtectedRoute.jsx` | `children, isAuthenticated` | Componente wrapper para rutas protegidas |
| `Dashboard` | `pages/Dashboard.jsx` | ninguno | Página principal protegida |

---

### Arquitectura y Dependencias

#### Paquetes ya existentes
- `com.loyalty.service_admin.domain.entity` → UserEntity
- `com.loyalty.service_admin.domain.repository` → UserRepository
- `com.loyalty.service_admin.application.service` → AuthService
- `com.loyalty.service_admin.application.dto` → LoginRequest, LoginResponse, UserResponse
- `com.loyalty.service_admin.presentation.controller` → AuthController

#### Servicios externos / Dependencias
- **PostgreSQL**: Almacena tabla `users`
- **BCrypt (jbcrypt library)**: Hash y validación de passwords
- **Spring Security / Spring Boot**: Framework base (validación, exception handling, etc.)
- **JWT (Base64 encoding)**: Generación de tokens simétricos

#### Configuración necesaria
- **application.yml** debe contener:
  ```yaml
  app:
    jwt:
      secret: "loyalty-secret-key-v1-please-change-in-production"
  ```
  (ya existe en application.properties)

---

### Notas de Implementación

1. **Dependencia jjwt (REQUERIDA)**: Usar la librería `io.jsonwebtoken:jjwt` (versión estable, ej. 0.11.5 o superior) en pom.xml. NO inventar tokens caseros.
   ```xml
   <dependency>
       <groupId>io.jsonwebtoken</groupId>
       <artifactId>jjwt</artifactId>
       <version>0.11.5</version>
   </dependency>
   ```

2. **Seguridad de la clave JWT**: La clave `app.jwt.secret` debe tener al menos 32 caracteres (256 bits) y almacenarse en variables de entorno en producción. Nunca commitear claves reales en Git. Cambiar en cada deploy.

3. **HTTPS obligatorio en producción**: Todos los endpoints deben servirse sobre HTTPS para proteger tokens en tránsito. Sin HTTPS, los Bearer tokens son vulnerables a man-in-the-middle.

4. **CORS**: Si el frontend está en dominio diferente, configurar CORS permitiendo request OPTIONS a /api/v1/auth/login y credenciales (Allow-Credentials: true).

5. **Token en localStorage (Frontend)**: El token JWT debe almacenarse en localStorage del navegador y enviarse en header `Authorization: Bearer <token>` en cada request. Considerar HttpOnly cookies en v2 para mayor seguridad.

6. **Refresh tokens (future)**: v1 no incluye refresh tokens. Se considerará agregarlo en v2 si se requieren sesiones más largas (ej. tokens más cortos de 5 minutos + refresh de 7 días).

7. **Blacklist de tokens (future)**: En v2, implementar Redis/Caffeine para invalidar tokens en logout sin esperar expiración (actualmente solo se logguea).

8. **Rate limiting (RECOMENDADO)**: Agregar rate limiting a `/api/v1/auth/login` con máximo 5 intentos fallidos por IP/minuto para evitar ataques de fuerza bruta (considerar Spring Cloud Gateway o AspectJ).

9. **Auditoría**: Todos los logins/logouts se registran en logs. Considerar agregar tabla de auditoría en DB para análisis histórico (future feature).

10. **OncePerRequestFilter para /api/v1/auth/me**: El endpoint debe estar protegido por un filtro que valida el token JWT antes de permitir acceso. Usar Spring Security o implementar `OncePerRequestFilter` personalizado.

---

## 2.5 Architecture Blueprint — Clean Layers

El Agente Backend debe respetar la arquitectura de capas (domain, application, infrastructure) para mantener desacoplamiento. Estructura obligatoria:

```
com.loyalty.service_admin/
├── domain/
│   ├── model/
│   │   └── UserEntity.java              # Pure Domain Entity + JPA annotations
│   └── repository/
│       └── UserRepository.java          # JpaRepository interface (domain contract)
├── application/
│   ├── service/
│   │   └── AuthService.java             # Login/Logout/GetMe business logic (sin HTTP)
│   └── dto/
│       ├── LoginRequest.java (Record)   # HTTP input contract
│       ├── LoginResponse.java (Record)  # HTTP output contract
│       └── UserResponse.java (Record)   # User data output contract
├── infrastructure/
│   ├── security/
│   │   ├── JwtProvider.java             # JWT creation/validation (jjwt library)
│   │   ├── AuthenticationFilter.java    # OncePerRequestFilter para /api/v1/auth/me
│   │   └── SecurityConfiguration.java   # Spring Security setup
│   ├── persistence/                     # JPA entity mapping (already under domain)
│   └── web/
│       ├── AuthController.java          # REST endpoints (login, logout, me)
│       └── GlobalExceptionHandler.java  # @RestControllerAdvice para manejar excepciones globales
└── config/
    └── ApplicationProperties.java       # Inyección de properties (app.jwt.secret)
```

**Principios:**
- `domain/`: modelos puros sin dependencias de framework (UserEntity sí tiene @Entity porque requiere JPA)
- `application/`: lógica de negocio; ServiceComponents inyectan Repository y JwtProvider; NO saben de HTTP
- `infrastructure/`: detalles técnicos (Spring, HTTP, BD); Controllers llaman Services
- `AuthService` nunca conoce `HttpServletRequest`; `AuthController` convierte HTTP ↔ DTO ↔ Service

---

## 3. LISTA DE TAREAS

> Checklist accionable para todos los agentes. Marcar cada ítem (`[x]`) al completarlo.
> El Orchestrator monitorea este checklist para determinar el progreso.

### Backend (/implement-backend skill)

#### Implementación
- [ ] **CRÍTICA**: Agregar dependencia jjwt en pom.xml: `<groupId>io.jsonwebtoken</groupId> <artifactId>jjwt</artifactId> <version>0.11.5</version>`
- [ ] Validar que `UserEntity` existe con todos los campos requeridos (id, username, password, role, active, created_at, updated_at)
- [ ] Validar que `UserRepository` existe y expone métodos: `findByUsername(String)`, CRUD básico
- [ ] Crear/validar DTO `LoginRequest` con anotaciones `@NotBlank` en campos (nunca retornar password en response)
- [ ] Crear/validar DTO `LoginResponse` con campos: token, tipo, username, role (SIN password)
- [ ] Crear/validar DTO `UserResponse` con todos los campos del Usuario (SIN password)
- [ ] Implementar `JwtProvider.java` con métodos:
  - [ ] `generateToken(String username, String role, Long userId): String` — crea JWT con jjwt, claims: username, userId, role, iat, exp
  - [ ] `validateToken(String token): boolean` — parsea y valida firma + expiración con jjwt.parseClaimsJws()
  - [ ] `getUsernameFromToken(String token): String` — extrae username del Payload
  - [ ] `getUserIdFromToken(String token): Long` — extrae userId del Payload
  - [ ] `getRoleFromToken(String token): String` — extrae role del Payload
- [ ] Implementar `BCryptPasswordEncoder` con strength entre 10 y 12 (ej. `new BCryptPasswordEncoder(11)`) en SecurityConfiguration
- [ ] Implementar `AuthService.login(String username, String password): LoginResponse`
  - [ ] Validar que username existe
  - [ ] Validar con BCrypt.matches() que password es correcto
  - [ ] Validar que Usuario.active == true
  - [ ] Generar token via JwtProvider.generateToken()
  - [ ] Retornar LoginResponse (token, tipo="Bearer", username, role) — SIN password
- [ ] Implementar `AuthService.getCurrentUser(String token): UserResponse`
  - [ ] Validar token con JwtProvider.validateToken()
  - [ ] Extraer username con JwtProvider.getUsernameFromToken()
  - [ ] Buscar Usuario en BD por username
  - [ ] Validar que Usuario.active == true
  - [ ] Retornar UserResponse completo (SIN password)
- [ ] Implementar `AuthService.logout(String token): void`
  - [ ] Si token válido: extraer username, registrar log info "Logout exitoso para usuario: X"
  - [ ] Si token inválido: registrar log warning "Logout con token inválido"
  - [ ] No lanzar excepciones (manejo graceful de errores)
- [ ] Implementar `AuthController`:
  - [ ] POST `/api/v1/auth/login` con `@Valid @RequestBody LoginRequest` → retorna 200 + LoginResponse
  - [ ] POST `/api/v1/auth/logout` con header `Authorization: Bearer <token>` → retorna 204 (siempre)
  - [ ] GET `/api/v1/auth/me` con header `Authorization: Bearer <token>` → retorna 200 + UserResponse
- [ ] Implementar `AuthenticationFilter extends OncePerRequestFilter`:
  - [ ] Intercepta requests a `/api/v1/auth/me`
  - [ ] Extrae token del header Authorization
  - [ ] Valida con JwtProvider.validateToken()
  - [ ] Si válido: establece SecurityContext + next
  - [ ] Si inválido: retorna 401 Unauthorized
- [ ] Implementar `GlobalExceptionHandler extends RestControllerAdvice`:
  - [ ] Catch `BadCredentialsException` → retorna 401 Unauthorized + mensaje "Credenciales inválidas"
  - [ ] Catch `MethodArgumentNotValidException` (validación Bean) → retorna 400 Bad Request + detalles de campos
  - [ ] Catch `JwtException` → retorna 401 Unauthorized + mensaje "Token no válido o expirado"
  - [ ] Catch excepciones genéricas → retorna 500 Internal Server Error
- [ ] Configurar `application.yml` con:
  - [ ] `app.jwt.secret` (mínimo 32 caracteres)
  - [ ] `app.jwt.expiration` (86400000 ms = 24 horas)
- [ ] Registrar `AuthenticationFilter` en `SecurityConfiguration` antes del filtro estándar de autenticación

#### Tests Backend
- [ ] `test_auth_login_success` → POST /api/v1/auth/login con credenciales válidas retorna 200 + token
- [ ] `test_auth_login_invalid_credentials` → credenciales erróneas retorna 401
- [ ] `test_auth_login_missing_username` → falta username retorna 400
- [ ] `test_auth_login_missing_password` → falta password retorna 400
- [ ] `test_auth_login_inactive_user` → Usuario inactivo retorna 401
- [ ] `test_auth_logout_success` → POST /api/v1/auth/logout con token válido retorna 204
- [ ] `test_auth_logout_no_token` → POST /api/v1/auth/logout sin token retorna 204 (no error)
- [ ] `test_auth_me_success` → GET /api/v1/auth/me con token válido retorna 200 + datos Usuario
- [ ] `test_auth_me_invalid_token` → GET /api/v1/auth/me con token inválido retorna 401
- [ ] `test_auth_me_expired_token` → GET /api/v1/auth/me con token expirado (> 24h) retorna 401
- [ ] `test_auth_me_inactive_user` → GET /api/v1/auth/me si Usuario fue desactivado retorna 401
- [ ] `test_bcrypt_password_hashing` → password se almacena hasheado y se valida correctamente

### Frontend (esbozo — `/implement-frontend` skill)

#### Implementación (Fuera de scope actual, pero incluido por completitud)
- [ ] Crear componente `LoginForm.jsx` con campos username, password, button submit
- [ ] Crear hook `useAuth.js` que maneja estado de login/logout
- [ ] Crear `authService.js` con funciones: `login(username, password)`, `logout()`, `getCurrentUser(token)`
- [ ] Implementar llamada POST /api/v1/auth/login en authService
- [ ] Implementar llamada POST /api/v1/auth/logout en authService
- [ ] Almacenar token en localStorage tras login exitoso
- [ ] Crear componente `ProtectedRoute.jsx` que valida token antes de renderizar
- [ ] Crear página `Dashboard.jsx` solo accesible si autenticado
- [ ] Implementar error handling (mostrar mensajes 401, 400, red errors)
- [ ] Implementar loading state en LoginForm

#### Tests Frontend
- [ ] `LoginForm renders correctly` → existe formulario con username, password
- [ ] `LoginForm submits data on button click` → click en submit envía datos
- [ ] `authService.login makes POST to /api/v1/auth/login` → llamada correcta al endpoint
- [ ] `authService.login stores token in localStorage` → token se guarda tras éxito
- [ ] `ProtectedRoute renders children if authenticated` → si token válido muestra contenido
- [ ] `ProtectedRoute redirects if not authenticated` → si sin token redirige a login
- [ ] `useAuth hook handles login error` → muestra error si credenciales inválidas
- [ ] `authService.logout removes token from localStorage` → logout limpia almacenamiento

### QA

- [ ] Ejecutar skill `/gherkin-case-generator` → genera escenarios formales Gherkin para CRITERIO-1.1, 1.2, 1.3, 1.4, 2.1, 2.2, 3.1, 3.2, 3.3
- [ ] Ejecutar skill `/risk-identifier` → clasifica riesgos ASD (Alto/Medio/Bajo) identificando:
  - alto: gestión de credenciales, JWT, bcrypt, validación de token
  - medio: expiración de tokens, usuario inactivo, CORS
  - bajo: logging, mensajes de error
- [ ] Ejecutar skill `/automation-flow-proposer` → propone flujos a automatizar (login exitoso, logout, error handling)
- [ ] Revisar cobertura de tests contra criterios de aceptación (CRITERIO-1.1 a 3.3)
- [ ] Validar que todas las reglas de negocio (RN-01 a RN-10) están cubiertas
- [ ] Ejecutar tests en CI/CD pipeline (`mvn clean test` backend, tests frontend)
- [ ] Validar que logs se generan correctamente (debug, info, warn, error)
- [ ] Pruebas manuales: login exitoso → logout → acceso denegado

### Orchestrator / Status Tracking

- [ ] Estado de spec: `DRAFT` → `APPROVED` (requiere revisión humana)
- [ ] Fase Backend completada (todos los ítems marcados `[x]`)
- [ ] Fase Frontend completada (todos los ítems marcados `[x]`)
- [ ] Fase Tests completada (todos los ítems marcados `[x]`)
- [ ] Fase QA completada (todos los ítems marcados `[x]`)
- [ ] Actualizar estado spec: `status: IN_PROGRESS` (cuando inicia implementación)
- [ ] Actualizar estado spec: `status: IMPLEMENTED` (cuando todo está hecho y aprobado)

---

## 4. CHECKLIST FINAL PARA EL ORCHESTRATOR

Antes de pasar el spec a status `IN_PROGRESS`, verificar que el Agente Backend confirmó:

- [ ] **Dependencia jjwt**: Agregada en pom.xml con versión >= 0.11.5
- [ ] **JwtProvider implementado**: Con métodos `generateToken()`, `validateToken()`, `getUsernameFromToken()`, `getUserIdFromToken()`, `getRoleFromToken()`
- [ ] **OncePerRequestFilter**: Implementado `AuthenticationFilter` protegiendo `/api/v1/auth/me`
- [ ] **SIN password en responses**: LoginResponse y UserResponse NO contienen campo `password`
- [ ] **GlobalExceptionHandler**: Implementado `@RestControllerAdvice` capturando:
  - [ ] `BadCredentialsException` → 401 Unauthorized
  - [ ] `MethodArgumentNotValidException` → 400 Bad Request (validación Bean)
  - [ ] `JwtException` → 401 Unauthorized
- [ ] **BCrypt strength 10-12**: `new BCryptPasswordEncoder(11)` o similar en SecurityConfiguration
- [ ] **Login retorna token, NO password**: POST `/api/v1/auth/login` response incluye `token`, `tipo`, `username`, `role`
- [ ] **Logout valida token**: POST `/api/v1/auth/logout` valida el header Authorization antes de loguear el logout
- [ ] **RFC 7519 JWT**: Tokens generados con estructura Header.Payload.Signature via jjwt, NO home-made Base64

Una vez confirmados estos ítems, el Orchestrator puede proceder a marcar spec como `IN_PROGRESS` e iniciar implementación con `/implement-backend`.

---

## Diccionario de Dominio Aplicado en Esta Spec

| Término Canónico | Uso en Esta Spec | Sinónimos rechazados |
|---|---|---|
| **Usuario** | `UserEntity`, "un Usuario requiere..." | Persona, cliente, usuario final |
| **uid** | No aplica (usamos `id` Long de BD) | _id, user_id técnico |
| **token** | "token JWT", "Authorization: Bearer <token>" | sesión, credencial, key |
| **role** | Rol del Usuario (ej. "ADMIN") | perfil, permiso, grupo |
| **created_at** | Timestamp de creación en UTC | fecha_alta, date_created |
| **updated_at** | Timestamp de actualización en UTC | modified_at, fecha_modificacion |

---

## Histórico de Cambios

| Versión | Fecha | Autor | Cambios |
|---------|-------|-------|---------|
| 1.0 | 2026-03-26 | spec-generator | Creación inicial: requerimientos, diseño, lista de tareas |
| 1.1 | 2026-03-26 | tech-lead | **CRITICAL SECURITY REVIEW**: RN-05 corregida para usar jjwt (RFC 7519) en lugar de home-made tokens; RN-08 reforzada para validar token antes de loguear logout; agregada RN-11 con BCrypt strength 10-12; agregado Architecture Blueprint (clean layers); checklist Orchestrator actualizado con validaciones de seguridad |
| 1.2 | 2026-03-26 | backend-developer | **IMPLEMENTACIÓN COMPLETADA**: Agregada dependencia jjwt 0.11.5; Creado JwtProvider con generación y validación RFC 7519; Refactorizado AuthService para usar JwtProvider; Creada SecurityConfiguration con BCryptPasswordEncoder(11); Agregado manejo de JwtException en GlobalExceptionHandler; Agregada propiedad app.jwt.expiration en application.properties. Status: APPROVED |
