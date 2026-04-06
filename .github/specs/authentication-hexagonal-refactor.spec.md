---
id: SPEC-AUTH-HEX-001
status: APPROVED
feature: authentication-hexagonal-refactor
created: 2026-04-05
updated: 2026-04-06
author: spec-generator
version: "1.0"
related-specs: ["SPEC-AUTH-001"]
---

# Spec: Refactorización de Authentication a Arquitectura Hexagonal (TDD)

> **Estado:** `APPROVED` ✅ Tests completados (23/23 passing). Listo para integración.
> **Enfoque:** Refactorización con preservación de funcionalidad. Test-Driven Development (TDD): tests primero, luego refactor.

---

## 1. REQUERIMIENTOS

### Descripción

Migrar el módulo de autenticación (Authentication) de su arquitectura monolítica actual a Hexagonal Architecture (Ports & Adapters) siguiendo el estándar ASDD. El objetivo es desacoplar la lógica de negocio de las implementaciones concretas (JWT, repositorio de usuarios), permitiendo mayor testabilidad, flexibilidad y conformidad arquitectónica. Se utilizará **Test-Driven Development (TDD)** como metodología: crear tests unitarios de los controllers primero, luego refactorizar el código para pasar esos tests.

### Contexto Actual

- **Ubicación:** `backend/service-admin/src/main/java/com/loyalty/service_admin/`
- **Componentes existentes:**
  - `AuthController` (presentation/controller/) — inyecta `AuthService` directamente
  - `AuthService` (application/service/) — contiene toda la lógica
  - `UserRepository` (domain/repository/) — acceso a datos
  - `JwtProvider` (infrastructure/security/) — generación y validación de JWT
  - DTOs: `LoginRequest`, `LoginResponse`, `UserResponse` (application/dto/auth/)

- **Funcionalidad Operativa:**
  - Login con credenciales (username + password), genera JWT token RFC 7519
  - Logout (stateless, solo confirmación)
  - GetCurrentUser (valida token y retorna datos del usuario autenticado)

### Requerimiento de Negocio

Extraído de `.github/requirements/authentication.md`:

```
HU-01: Inicio y cierre de sesión (Auth Management)
Como Administrador, quiero iniciar y cerrar sesión en el Dashboard, 
para gestionar las reglas de negocio de forma segura mediante un token de identidad.

Criterios Gherkin:
- Login exitoso: POST /api/v1/auth/login retorna JWT con claims de userId y roles (200 OK)
- Credenciales inválidas: POST /api/v1/auth/login retorna 401 Unauthorized
- Logout: POST /api/v1/auth/logout responde correctamente (204 No Content)
- GetCurrentUser: GET /api/v1/auth/me retorna datos del usuario autenticado (200 OK)

Constraints técnicas:
- Contraseñas hasheadas con BCrypt
- JWT usando clave secreta simétrica
- Stateless (sin HttpSession)
- Header: Authorization: Bearer {token}
```

### Historias de Usuario

#### HU-AUTH-01: Migración de AuthController

```
Como:        Arquitecto de Software / Developer
Quiero:      Migrar AuthController a inyectar AuthUseCase en lugar de AuthService
Para:        Desacoplar el controller de la implementación concreta del servicio

Prioridad:   Alta
Estimación:  M
Dependencias: Ninguna (implementación previa del puerto)
Capa:        Backend
```

#### HU-AUTH-02: Implementación de AuthUseCase (Puerto de Entrada)

```
Como:        Arquitecto de Software
Quiero:      Crear el puerto de entrada AuthUseCase
Para:        Abstraer los casos de uso de autenticación y permitir múltiples implementaciones

Prioridad:   Alta
Estimación:  S
Dependencias: Ninguna
Capa:        Backend
```

#### HU-AUTH-03: Implementación de Puertos de Salida (Persistencia y JWT)

```
Como:        Arquitecto de Software
Quiero:      Crear AuthPersistencePort y JwtPort
Para:        Desacoplar la lógica de negocio de las implementaciones concretas de repositorio y JWT

Prioridad:   Alta
Estimación:  M
Dependencias: HU-AUTH-02
Capa:        Backend
```

#### HU-AUTH-04: Creación de AuthServiceImpl con TDD

```
Como:        Developer
Quiero:      Implementar AuthServiceImpl siguiendo TDD con tests previos
Para:        Garantizar que la lógica de negocio sea correcta y tenga cobertura de tests

Prioridad:   Alta
Estimación:  L
Dependencias: HU-AUTH-02, HU-AUTH-03
Capa:        Backend
```

#### HU-AUTH-05: Implementación de Adapters

```
Como:        Developer
Quiero:      Crear JpaAuthAdapter y JwtTokenAdapter
Para:        Hacer la glía entre los puertos abstractos y las implementaciones concretas

Prioridad:   Alta
Estimación:  M
Dependencias: HU-AUTH-03
Capa:        Backend
```

#### HU-AUTH-06: Unit Tests de AuthController

```
Como:        QA Engineer / Test Engineer
Quiero:      Crear suite de tests unitarios de AuthController
Para:        Validar que el controller maneja correctamente requests/responses y delegación a use cases

Prioridad:   Alta
Estimación:  M
Dependencias: HU-AUTH-01
Capa:        Backend
```

### Criterios de Aceptación — HU-AUTH-02

**Happy Path: Creación de AuthUseCase**
```gherkin
CRITERIO-2.1: AuthUseCase interface existe en application/port/in/
  Dado que:    el proyecto requiere desacoplar la lógica de autenticación
  Cuando:      se define la interfaz AuthUseCase
  Entonces:    contiene los métodos: login(LoginRequest), logout(String), getCurrentUser(String)
  Y:           está ubicada en application/port/in/AuthUseCase.java
```

### Criterios de Aceptación — HU-AUTH-03

**Happy Path: Creación de Puertos de Salida**
```gherkin
CRITERIO-3.1: AuthPersistencePort interface existe
  Dado que:    se necesita abstraer operaciones de persistencia de usuario
  Cuando:      se crea AuthPersistencePort en application/port/out/
  Entonces:    contiene los métodos: findByUsername(String), existsByUsername(String)
  Y:           retorna Optional<UserEntity> / boolean respectivamente

CRITERIO-3.2: JwtPort interface existe
  Dado que:    se necesita abstraer generación y validación de JWT
  Cuando:      se crea JwtPort en application/port/out/
  Entonces:    contiene los métodos: generateToken(...), validateToken(String), getUsernameFromToken(String)
  Y:           está completamente desacoplado de io.jsonwebtoken
```

### Criterios de Aceptación — HU-AUTH-04

**Happy Path: Implementación de AuthServiceImpl**
```gherkin
CRITERIO-4.1: AuthServiceImpl implementa AuthUseCase
  Dado que:    existe AuthUseCase en application/port/in/
  Cuando:      se implementa AuthServiceImpl que inyecta AuthPersistencePort y JwtPort
  Entonces:    la clase está anotada con @Service
  Y:           tiene constructor inyectado con DI
  Y:           los métodos login(), logout(), getCurrentUser() delegación responsabilidades a puertos

CRITERIO-4.2: Lógica de login se preserva (TDD)
  Dado que:    existe AuthServiceImpl con la lógica de login
  Cuando:      se ejecuta test de login exitoso con credenciales válidas
  Entonces:    retorna LoginResponse con JWT token válido
  
CRITERIO-4.3: Validaciones se preservan (TDD)
  Dado que:    existe AuthServiceImpl
  Cuando:      se intenta login con credenciales inválidas
  Entonces:    lanza UnauthorizedException con mensaje "Credenciales inválidas"
```

**Error Path: Manejo de excepciones preservado**
```gherkin
CRITERIO-4.4: UnauthorizedException se lanza en casos de error
  Dado que:    existe login con credenciales inválidas
  Cuando:      el usuario no existe o password es incorrecto
  Entonces:    lanza UnauthorizedException (sin guardar el token)
```

### Criterios de Aceptación — HU-AUTH-05

**Happy Path: Adapters Implementados**
```gherkin
CRITERIO-5.1: JpaAuthAdapter implementa AuthPersistencePort
  Dado que:    existe AuthPersistencePort
  Cuando:      se crea JpaAuthAdapter en infrastructure/persistence/jpa/
  Entonces:    inyecta UserRepository y delega operaciones al repositorio JPA
  Y:           retorna los resultados mapeados correctamente

CRITERIO-5.2: JwtTokenAdapter implementa JwtPort
  Dado que:    existe JwtPort
  Cuando:      se crea JwtTokenAdapter en infrastructure/security/
  Entonces:    inyecta JwtProvider y delega generación/validación de tokens
  Y:           convierte excepciones de JWT en UnauthorizedException
```

### Criterios de Aceptación — HU-AUTH-06

**Happy Path: Unit Tests de AuthController**
```gherkin
CRITERIO-6.1: Controllers test for POST /api/v1/auth/login success
  Dado que:    existe AuthController que inyecta AuthUseCase
  Cuando:      se envía POST /api/v1/auth/login con credenciales válidas
  Entonces:    retorna ResponseEntity.ok() con LoginResponse
  Y:           status HTTP es 200

CRITERIO-6.2: Controllers test for POST /api/v1/auth/login unauthorized
  Dado que:    existe AuthController
  Cuando:      se envía POST /api/v1/auth/login con credenciales inválidas
  Entonces:    el mock de AuthUseCase lanza UnauthorizedException
  Y:           el controller retorna ResponseEntity.status(401)

CRITERIO-6.3: Controllers test for GET /api/v1/auth/me success
  Dado que:    existe AuthController y token válido en Authorization header
  Cuando:      se envía GET /api/v1/auth/me
  Entonces:    retorna ResponseEntity.ok() con UserResponse
```

### Reglas de Negocio (Preservadas)

1. **Validación de Credenciales:** Username debe existir en BD y password debe coincidir con hash BCrypt
2. **Validación de Token:** JWT debe ser válido, no expirado y corresponder a usuario activo
3. **No almacenamiento de tokens:** Logout es stateless (sin blacklist de tokens requiere autenticación)
4. **Unicidad de Username:** No hay dos usuarios con el mismo username

---

## 2. DISEÑO

### Estructura Hexagonal

#### Modelo de Carpetas (Target)

```
src/main/java/com/loyalty/service_admin/
├── application/
│   ├── dto/auth/
│   │   ├── LoginRequest.java           # DTO de entrada (EXISTENTE)
│   │   ├── LoginResponse.java          # DTO de salida (EXISTENTE)
│   │   └── UserResponse.java           # DTO de salida (EXISTENTE)
│   ├── port/
│   │   ├── in/
│   │   │   └── AuthUseCase.java        # NUEVO — Puerto de entrada
│   │   └── out/
│   │       ├── AuthPersistencePort.java  # NUEVO — Puerto persistencia
│   │       └── JwtPort.java              # NUEVO — Puerto JWT
│   └── service/
│       └── AuthServiceImpl.java         # NUEVO — Renombrado de AuthService (refactor con TDD)
│
├── domain/
│   ├── entity/
│   │   └── UserEntity.java             # EXISTENTE (sin cambios)
│   └── repository/
│       └── UserRepository.java         # EXISTENTE (sin cambios)
│
├── infrastructure/
│   ├── persistence/jpa/
│   │   └── JpaAuthAdapter.java         # NUEVO — Adapter para persistencia
│   └── security/
│       ├── JwtProvider.java            # EXISTENTE (sin cambios funcionales)
│       └── JwtTokenAdapter.java        # NUEVO — Adapter para JWT
│
└── presentation/
    └── controller/
        └── AuthController.java         # EXISTENTE (cambio: inyectar AuthUseCase)
```

### Definición de Componentes

#### 1. Puerto de Entrada: AuthUseCase

**Ubicación:** `application/port/in/AuthUseCase.java`

**Propósito:** Definir los casos de uso de autenticación como interfaz, desacoplado de implementación.

```java
public interface AuthUseCase {
    /**
     * Autentica usuario con credenciales y genera JWT token
     * @param request contiene username y password
     * @return LoginResponse con JWT token válido
     * @throws UnauthorizedException si credenciales son inválidas o usuario inactivo
     */
    LoginResponse login(LoginRequest request);
    
    /**
     * Obtiene datos del usuario autenticado (validando token)
     * @param token JWT token RFC 7519
     * @return UserResponse con datos del usuario
     * @throws UnauthorizedException si token inválido, expirado o usuario no existe/inactivo
     */
    UserResponse getCurrentUser(String token);
    
    /**
     * Logout stateless (confirmación)
     * @param token JWT token a invalidar (lado cliente lo elimina)
     */
    void logout(String token);
}
```

#### 2. Puerto de Salida: AuthPersistencePort

**Ubicación:** `application/port/out/AuthPersistencePort.java`

**Propósito:** Abstraer operaciones de persistencia de usuario.

```java
public interface AuthPersistencePort {
    /**
     * Busca usuario por username
     * @param username nombre de usuario
     * @return Optional con UserEntity si existe
     */
    Optional<UserEntity> findByUsername(String username);
    
    /**
     * Verifica si username existe
     * @param username nombre de usuario
     * @return true si existe, false en caso contrario
     */
    boolean existsByUsername(String username);
}
```

#### 3. Puerto de Salida: JwtPort

**Ubicación:** `application/port/out/JwtPort.java`

**Propósito:** Abstraer generación, validación y parsing de JWT.

```java
public interface JwtPort {
    /**
     * Genera JWT token con claims de usuario
     * @param username nombre del usuario
     * @param userId ID único del usuario
     * @param role nombre del rol
     * @param ecommerceId ID del ecommerce asociado
     * @return JWT token en formato string
     */
    String generateToken(String username, UUID userId, String role, UUID ecommerceId);
    
    /**
     * Valida JWT token
     * @param token JWT a validar
     * @return true si válido y no expirado, false en caso contrario
     */
    boolean validateToken(String token);
    
    /**
     * Extrae username del JWT
     * @param token JWT a parsear
     * @return username contenido en claims
     * @throws UnauthorizedException si token inválido
     */
    String getUsernameFromToken(String token);
}
```

#### 4. Implementación del Use Case: AuthServiceImpl

**Ubicación:** `application/service/AuthServiceImpl.java`

**Propósito:** Contiene la lógica de negocio usando puertos.

**Inyecciones:** `AuthPersistencePort`, `JwtPort`

**Responsabilidades:**
- Coordinar persistencia y JWT
- Aplicar reglas de validación de usuario y contraseña
- Manejar excepciones y logs

**Nota TDD:** Esta clase será implementada después de crear los tests unitarios de `AuthController`. Los tests deben:
1. Mockear `AuthUseCase`
2. Verificar que el controller delega correctamente
3. Verificar manejo de responseStatus adecuados

#### 5. Adapter: JpaAuthAdapter

**Ubicación:** `infrastructure/persistence/jpa/JpaAuthAdapter.java`

**Propósito:** Implementar `AuthPersistencePort` usando `UserRepository`.

```java
@Component
@RequiredArgsConstructor
public class JpaAuthAdapter implements AuthPersistencePort {
    private final UserRepository repository;
    
    @Override
    public Optional<UserEntity> findByUsername(String username) {
        return repository.findByUsername(username);
    }
    
    @Override
    public boolean existsByUsername(String username) {
        return repository.existsByUsername(username);
    }
}
```

#### 6. Adapter: JwtTokenAdapter

**Ubicación:** `infrastructure/security/JwtTokenAdapter.java`

**Propósito:** Implementar `JwtPort` usando `JwtProvider`.

```java
@Component
@RequiredArgsConstructor
public class JwtTokenAdapter implements JwtPort {
    private final JwtProvider jwtProvider;
    
    @Override
    public String generateToken(String username, UUID userId, String role, UUID ecommerceId) {
        return jwtProvider.generateToken(username, userId, role, ecommerceId);
    }
    
    @Override
    public boolean validateToken(String token) {
        try {
            return jwtProvider.validateToken(token);
        } catch (io.jsonwebtoken.JwtException e) {
            return false;
        }
    }
    
    @Override
    public String getUsernameFromToken(String token) {
        try {
            return jwtProvider.getUsernameFromToken(token);
        } catch (io.jsonwebtoken.JwtException e) {
            throw new UnauthorizedException("Token no válido o expirado");
        }
    }
}
```

#### 7. Controlador Refactorizado: AuthController

**Ubicación:** `presentation/controller/AuthController.java` (cambio de inyección)

**Cambio:**
- ❌ Antes: `private final AuthService authService;`
- ✅ Después: `private final AuthUseCase authUseCase;`

**Métodos sin cambios funcionales:**
- `login(LoginRequest)` → delega a `authUseCase.login()`
- `logout(String)` → delega a `authUseCase.logout()`
- `getCurrentUser(String)` → delega a `authUseCase.getCurrentUser()`

### API Endpoints (Sin cambios)

#### POST /api/v1/auth/login
- **Descripción:** Autentica usuario con credenciales
- **Auth requerida:** No
- **Request Body:**
  ```json
  { "username": "string", "password": "string" }
  ```
- **Response 200:**
  ```json
  { "token": "jwt-string", "type": "Bearer", "username": "user", "role": "ADMIN" }
  ```
- **Response 401:** Credenciales inválidas o usuario inactivo
- **Response 400:** Campos requeridos faltantes

#### POST /api/v1/auth/logout
- **Descripción:** Logout stateless
- **Auth requerida:** Sí (Bearer token)
- **Response 204:** Sin contenido (éxito)

#### GET /api/v1/auth/me
- **Descripción:** Obtiene datos del usuario autenticado
- **Auth requerida:** Sí (Bearer token)
- **Response 200:**
  ```json
  { "id": "uuid", "username": "string", "role": "ADMIN", "email": "string", ... }
  ```
- **Response 401:** Token inválido o expirado

### Notas de Implementación

1. **TDD First:** Crear tests unitarios de `AuthController` ANTES de refactorizar el código.
   - El controller debe mockear `AuthUseCase`
   - Validar que delegación correcta ocurre
   - Verificar respuestas HTTP y mapeo de excepciones

2. **Preservación de Lógica:** La refactorización NO cambia el comportamiento:
   - Login genera el mismo JWT
   - Validaciones de contraseña y usuario son idénticas
   - Manejo de errores es el mismo

3. **Inyección Progresiva:** Cambiar `AuthService` → `AuthServiceImpl` que implement `AuthUseCase`. Esto permite:
   - El controller inyecta `AuthUseCase` (no `AuthService`)
   - Tests pueden mockear el puerto sin instanciar Spring
   - Futura implementación alternativa de `AuthUseCase` es posible

4. **Configuración Spring:**
   - Todos los adapters deben ser anotados con `@Component` o `@Service`
   - Spring resolverá automáticamente las inyecciones (interfaz → implementador con `@Component`)

5. **Sin Breaking Changes:**
   - Endpoints REST no cambian
   - DTOs no cambian
   - Comportamiento es idéntico
   - Solo la "plomería" interna se refactoriza

---

## 3. LISTA DE TAREAS

> Checklist accionable en orden secuencial. TDD: tests primero.

### Fase 1: Definición de Puertos (Backend)

#### Diseño de Puertos
- [ ] Crear interfaz `AuthUseCase` en `application/port/in/` con métodos login, getCurrentUser, logout
- [ ] Crear interfaz `AuthPersistencePort` en `application/port/out/` con métodos findByUsername, existsByUsername
- [ ] Crear interfaz `JwtPort` en `application/port/out/` con métodos generateToken, validateToken, getUsernameFromToken

#### Validación
- [ ] Verificar que no existen dependencias circulares entre puertos y servicios
- [ ] Validar que DTOs existentes (`LoginRequest`, `LoginResponse`, `UserResponse`) se reutilizan sin cambios

### Fase 2: TDD — Unit Tests de AuthController (Backend)

#### Tests de Login Exitoso
- [ ] `testLoginSuccess_ValidCredentials_Returns200WithToken` — mock de `AuthUseCase.login()` retorna `LoginResponse` válido
- [ ] Verificar que response HTTP status es 200 OK
- [ ] Verificar que body contiene `token`, `type`, `username`, `role`

#### Tests de Login Fallido
- [ ] `testLoginFail_InvalidCredentials_Returns401` — mock de `AuthUseCase.login()` lanza `UnauthorizedException`
- [ ] Verificar que controller retorna ResponseEntity.status(401)
- [ ] `testLoginFail_MissingFields_Returns400` — test con `@Valid` fallando en binding
- [ ] Verificar que Spring devuelve 400 Bad Request

#### Tests de Logout
- [ ] `testLogout_ValidToken_Returns204` — mock de `AuthUseCase.logout()` completado sin error
- [ ] Verificar que response HTTP status es 204 No Content
- [ ] `testLogout_NoToken_Returns204` — logout sin Authorization header también retorna 204

#### Tests de GetCurrentUser
- [ ] `testGetCurrentUser_ValidToken_Returns200WithUser` — mock de `AuthUseCase.getCurrentUser()` retorna `UserResponse`
- [ ] Verificar que response HTTP status es 200
- [ ] `testGetCurrentUser_InvalidToken_Returns401` — mock lanza `UnauthorizedException`
- [ ] Verificar que controller retorna 401

#### Infraestructura de Tests
- [ ] Crear test suite en `backend/service-admin/src/test/java/.../presentation/controller/AuthControllerTest.java`
- [ ] Usar MockMvc o WebTestClient para tests de integración ligera
- [ ] Mockear `AuthUseCase` con `@MockBean`

### Fase 3: Implementación de Adapters (Backend)

#### Adapter de Persistencia
- [ ] Crear `JpaAuthAdapter` en `infrastructure/persistence/jpa/`
- [ ] Inyectar `UserRepository`
- [ ] Implementar métodos `findByUsername()` y `existsByUsername()` delegando al repositorio
- [ ] Validar que retorna tipos correctos (Optional, boolean)

#### Adapter de JWT
- [ ] Crear `JwtTokenAdapter` en `infrastructure/security/`
- [ ] Inyectar `JwtProvider`
- [ ] Implementar `generateToken()` delegando a `JwtProvider`
- [ ] Implementar `validateToken()` manejando excepciones de JWT
- [ ] Implementar `getUsernameFromToken()` capturando `JwtException` y lanzando `UnauthorizedException`

### Fase 4: Implementación de AuthServiceImpl (Backend)

#### Lógica de Negocio
- [ ] Crear `AuthServiceImpl` implementando `AuthUseCase`
- [ ] Inyectar `AuthPersistencePort` y `JwtPort`
- [ ] Implementar `login(LoginRequest)`:
  - Buscar usuario por username usando puerto de persistencia
  - Validar que usuario existe y está activo
  - Validar password contra hash BCrypt
  - Generar token usando puerto JWT
  - Retornar `LoginResponse` con token
  - Lanzar `UnauthorizedException` si alguna validación falla
  
- [ ] Implementar `getCurrentUser(String token)`:
  - Validar token usando puerto JWT
  - Extraer username del token
  - Buscar usuario en BD usando puerto de persistencia
  - Validar que usuario existe y está activo
  - Retornar `UserResponse`
  - Lanzar `UnauthorizedException` si token inválido o usuario no existe/inactivo
  
- [ ] Implementar `logout(String token)`:
  - Validar token (log solamente)
  - Retornar void

#### Logging e Instrumentación
- [ ] Añadir logs de debug para entrada de métodos
- [ ] Añadir logs de info para login exitoso
- [ ] Añadir logs de warn para intentos fallidos
- [ ] Añadir logs de error para excepciones inesperadas

#### Tests Unitarios de AuthServiceImpl
- [ ] Crear test suite en `backend/service-admin/src/test/java/.../application/service/AuthServiceImplTest.java`
- [ ] Mockear `AuthPersistencePort` y `JwtPort`
- [ ] Tests de login exitoso, fallido, usuario inactivo, password incorrecto
- [ ] Tests de getCurrentUser válido, token inválido, usuario no existe, usuario inactivo
- [ ] Tests de logout

### Fase 5: Refactorización de AuthController (Backend)

#### Cambio de Inyección
- [ ] Modificar `AuthController` para inyectar `AuthUseCase` en lugar de `AuthService`
- [ ] Cambiar declaración: `private final AuthUseCase authUseCase;`
- [ ] Actualizar constructor (si usa `@RequiredArgsConstructor`, el cambio es automático)

#### Delegación a Use Case
- [ ] Verificar que `login()` llama a `authUseCase.login()`
- [ ] Verificar que `logout()` llama a `authUseCase.logout()`
- [ ] Verificar que `getCurrentUser()` llama a `authUseCase.getCurrentUser()`

#### Manejo de Excepciones
- [ ] Verificar que `UnauthorizedException` es capturada y retorna 401
- [ ] Validar que otros tipos de excepciones son manejadas apropiadamente
- [ ] Asegurarse de que respuestas HTTP codes son correctos

#### Pruebas de Regresión
- [ ] Ejecutar tests de AuthController
- [ ] Ejecutar integration tests de Auth si existen
- [ ] Validar que endpoints funcionen correctamente con curl o Postman

### Fase 6: Configuración y Wiring (Backend)

#### Spring Configuration
- [ ] Verificar que `JpaAuthAdapter` está anotado con `@Component` y es detectado por Spring
- [ ] Verificar que `JwtTokenAdapter` está anotado con `@Component`
- [ ] Verificar que `AuthServiceImpl` está anotado con `@Service`
- [ ] Ejecutar boot application para validar que no hay errores de inyección de dependencias

#### Validación de Resolución
- [ ] Crear integration test que carga el contexto de Spring completo
- [ ] Validar que `AuthUseCase` es resuelto a `AuthServiceImpl`
- [ ] Validar que `AuthPersistencePort` es resuelto a `JpaAuthAdapter`
- [ ] Validar que `JwtPort` es resuelto a `JwtTokenAdapter`

### Fase 7: QA y Validación (Backend)

#### Tests de Integración
- [ ] Ejecutar tests de integración de AuthController (MockMvc)
- [ ] Ejecutar test suite completo del módulo auth
- [ ] Validar que cobertura de código en AuthController es > 80%

#### Regresión Funcional
- [ ] Verificar POST /api/v1/auth/login — login exitoso retorna JWT válido
- [ ] Verificar POST /api/v1/auth/login — credenciales inválidas retorna 401
- [ ] Verificar POST /api/v1/auth/logout — logout retorna 204
- [ ] Verificar GET /api/v1/auth/me — usuario autenticado retorna datos correctos
- [ ] Verificar GET /api/v1/auth/me — sin token retorna 401
- [ ] Verificar GET /api/v1/auth/me — token expirado retorna 401

#### Documentación
- [ ] Actualizar ADR (Architecture Decision Record) si es necesario
- [ ] Actualizar comentarios de código en componentes
- [ ] Generar diagrama de arquitectura actual (hexagonal)

### Fase 8: Cierre y Validación Final

#### Checklist de Cumplimiento Hexagonal
- [ ] ✅ Existe `AuthUseCase` en `application/port/in/`
- [ ] ✅ Existe `AuthPersistencePort` en `application/port/out/`
- [ ] ✅ Existe `JwtPort` en `application/port/out/`
- [ ] ✅ Controller inyecta `AuthUseCase` (no `AuthService`)
- [ ] ✅ Service (`AuthServiceImpl`) implementa `AuthUseCase`
- [ ] ✅ Service depende de puertos (no inyecta repositorio directo ni `JwtProvider`)
- [ ] ✅ `JpaAuthAdapter` implementa `AuthPersistencePort`
- [ ] ✅ `JwtTokenAdapter` implementa `JwtPort`
- [ ] ✅ No hay inyecciones de implementaciones concretas en AuthServiceImpl

#### Validación de No Breaking Changes
- [ ] ✅ API endpoints sin cambios
- [ ] ✅ DTOs sin cambios
- [ ] ✅ Comportamiento de negocio idéntico
- [ ] ✅ Tests de regresión todos pasando

#### Actualizar Status de Spec
- [ ] [ ] Cambiar status en frontmatter: `status: IMPLEMENTED` (cuando se complete todo)

---

## DEPENDENCIAS Y RIESGOS

### Dependencias Externas
- Spring Framework (inyección de dependencias) — ya disponible
- JPA/Hibernate (persistencia) — ya disponible
- JWT library (io.jsonwebtoken) — ya disponible
- BCrypt (criptografía) — ya disponible

### Riesgos Identificados

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|-------------|--------|-----------|
| Breaking changes en API | Baja | Alto | Validar que endpoints no cambian con tests de integración |
| Inyección incorrecta de dependencias | Media | Alto | Tests de integración que carguen contexto completo de Spring |
| Lógica de negocio no preservada | Media | Alto | TDD: tests de AuthController primero, validar lógica es idéntica |
| Tests inestables o flaky | Media | Medio | Usar mockito correctamente, evitar timestamp en tests |
| Performance degradada | Baja | Medio | Perfilar generación de JWT y búsquedas de usuario |

---

## NOTAS FINALES

- **Enfoque TDD:** Los tests deben escribirse ANTES de la refactorización. Los tests de `AuthController` guiarán la implementación de `AuthServiceImpl`.
- **Preservación:** Esta es una refactorización, no un cambio de funcionalidad. El comportamiento debe ser idéntico.
- **Arquitectura:** Esta refactorización establece el patrón para otros features. Usar este como reference implementation.
- **Documentación:** Actualizar `.github/docs/guidelines/technical-architecture.md` con este ejemplo una vez completado.

---

**Próximos Pasos (TDD - Tests First):**
1. ✅ Generar especificación (COMPLETADO)
2. → Ejecutar `/unit-testing` **PRIMERO** - Crear suite de tests de AuthController y AuthServiceImpl (guían implementación)
3. → Ejecutar `/implement-backend` - Implementar puertos, adapters y AuthServiceImpl (pasar los tests)
4. → Ejecutar `/risk-identifier` y `/gherkin-case-generator` para QA

