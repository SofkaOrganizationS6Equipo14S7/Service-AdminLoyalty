---
id: SPEC-HU02-HARD-DELETE
status: APPROVED
feature: hu-02-hard-delete-usuarios
created: 2026-04-05
updated: 2026-04-05
author: Backend Developer Team
version: "2.0"
related-specs:
  - SPEC-HU02-ECOMMERCE-USERS (implementación base)
  - SPEC-HEXAGONAL-REFACTOR (migración posterior)
tdd-approach: true
testing-first: true
---

# Spec: HU-02.5 Gestión de Usuarios - Hard Delete

> **Estado:** `APPROVED` ✅
> **Enfoque:** TDD (Test-Driven Development) — **Tests PRIMERO, luego implementación**
> **Ficción técnica:** Cambiar el mecanismo de eliminación de usuarios de soft delete (isActive=false) a hard delete (eliminación física de la base de datos).
> **Roadmap futuro:** Preparar para migración a Hexagonal Architecture (posterior a validación de tests)

---

## 1. REQUERIMIENTOS

### Descripción
La HU-02 "Gestión de Usuarios por Ecommerce" requiere que cuando un ADMIN o SUPER_ADMIN elimine un usuario, el registro se elimine de forma **permanente e irrecuperable** de la base de datos. Actualmente, el sistema implementa soft delete (marca isActive=false). Este cambio garantiza cumplimiento con políticas de privacidad, derecho al olvido (GDPR), y gestión adecuada del ciclo de vida del usuario.

### Requerimiento de Negocio
**De HU-02 original:**
- Eliminar usuario estándar → el sistema **elimina el perfil de forma permanente**
- El usuario ya no puede acceder al sistema (implícito en hard delete)

**Nuevas implicaciones técnicas:**
- Hard delete requiere auditoría previa (registrar datos antes de eliminar)
- Cascada controlada de relaciones (audit_log: ON DELETE SET NULL, otras FKs: ON DELETE RESTRICT)
- No poder recuperar el usuario después de hard delete

### Historias de Usuario

#### HU-02.5: Hard Delete de Usuarios

```
Como:        SUPER_ADMIN o STORE_ADMIN
Quiero:      eliminar permanentemente un usuario de la base de datos
Para:        cumplir con políticas de privacidad, GDPR (derecho al olvido) 
             y garantizar que el usuario no pueda recuperarse ni acceder al sistema

Prioridad:   Alta
Estimación:  M
Dependencias: HU-02 (creación, lectura, actualización de usuarios) -- YA IMPLEMENTADA
Capa:        Backend (lógica de negocio) + Tests
```

#### Criterios de Aceptación — HU-02.5

**Happy Path: SUPER_ADMIN elimina usuario existente**
```gherkin
CRITERIO-2.5.1: Hard delete exitoso de usuario estándar por SUPER_ADMIN
  Dado que: soy un SUPER_ADMIN autenticado
  Y existe un usuario estándar (STANDARD) con uid=user-123
  Y el usuario está vinculado a un ecommerce válido
  Cuando: ejecuto DELETE /api/v1/users/user-123
  Entonces: 
    - La BD registra la eliminación en audit_log antes de delete (user_id, username, email, role, ecommerce_id, timestamp)
    - El usuario se elimina físicamente de app_user
    - El endpoint retorna HTTP 204 No Content (sin body)
    - Los logs muestran: "Hard delete ejecutado: uid=user-123, actor=super-admin-uid"
```

**Happy Path: STORE_ADMIN elimina usuario de su propio ecommerce**
```gherkin
CRITERIO-2.5.2: Hard delete exitoso por STORE_ADMIN en su ecommerce
  Dado que: soy un STORE_ADMIN autenticado con ecommerce_id=ecom-456
  Y existe un usuario estándar en mi ecommerce (uid=user-789)
  Cuando: ejecuto DELETE /api/v1/users/user-789
  Entonces:
    - El usuario se registra en audit_log
    - El usuario se elimina de app_user
    - El endpoint retorna HTTP 204 No Content
    - Los logs muestran: "Hard delete ejecutado: uid=user-789, actor=store-admin-uid, ecommerce=ecom-456"
```

**Error Path: Intento de auto-eliminación (usuario intenta su propio hard delete)**
```gherkin
CRITERIO-2.5.3: Prevenir auto-eliminación (hard delete prohibido de sí mismo)
  Dado que: soy un STORE_ADMIN con uid=admin-111 autenticado
  Cuando: intento ejecutar DELETE /api/v1/users/admin-111 (su propio uid)
  Entonces:
    - El endpoint retorna HTTP 400 Bad Request
    - El body del error es: { "error": "No puede eliminarse a sí mismo" }
    - El usuario NO se registra en audit_log
    - El usuario permanece activo en app_user
```

**Error Path: Usuario no encontrado**
```gherkin
CRITERIO-2.5.4: Hard delete de usuario inexistente
  Dado que: soy un SUPER_ADMIN autenticado
  Cuando: intento ejecutar DELETE /api/v1/users/uid-invalido-999
  Entonces:
    - El endpoint retorna HTTP 404 Not Found
    - El body del error es: { "error": "Usuario no encontrado" }
    - Nada se registra en audit_log
```

**Error Path: Acceso denegado (STORE_ADMIN intenta eliminar usuario de otro ecommerce)**
```gherkin
CRITERIO-2.5.5: Prevenir acceso cruzado entre ecommerce
  Dado que: soy un STORE_ADMIN con ecommerce_id=ecom-aaa
  Y existe un usuario en ecommerce_id=ecom-bbb con uid=user-xyz
  Cuando: intento ejecutar DELETE /api/v1/users/user-xyz
  Entonces:
    - El endpoint retorna HTTP 403 Forbidden
    - El body del error es: { "error": "No tiene permiso para eliminar este usuario" }
    - El usuario NO se elimina
    - Los logs registran el intento fallido
```

**Error Path: Usuario estándar intenta eliminar otro usuario**
```gherkin
CRITERIO-2.5.6: Validar que solo ADMIN roles pueden eliminar
  Dado que: soy un usuario STANDARD autenticado
  Y existe otro usuario en mi ecommerce con uid=other-user-456
  Cuando: intento ejecutar DELETE /api/v1/users/other-user-456
  Entonces:
    - El endpoint retorna HTTP 403 Forbidden
    - El body del error es: { "error": "No tiene permiso para eliminar este usuario" }
    - El usuario NO se elimina
```

**Edge Case: Cascada de eliminación - audit_log.user_id**
```gherkin
CRITERIO-2.5.7: Cascada controlada - audit_log sobrevive al hard delete
  Dado que: soy un SUPER_ADMIN
  Y existe un usuario (uid=user-111) con múltiples registros en audit_log
  Cuando: ejecuto DELETE /api/v1/users/user-111
  Entonces:
    - Los registros de audit_log.user_id se ponen a NULL (ON DELETE SET NULL)
    - El histórico de auditoría se preserva, pero user_id es NULL
    - El usuario se elimina completamente de app_user
    - Los logs muestran que la cascada se ejecutó correctamente
```

### Reglas de Negocio

1. **Autorización RBA (Role-Based Access Control)**
   - Solo SUPER_ADMIN puede eliminar cualquier usuario
   - STORE_ADMIN solo puede eliminar usuarios en su propio ecommerce
   - STANDARD user NO puede eliminar usuarios

2. **Prohibición de auto-eliminación**
   - Un usuario NO puede usar DELETE en su propio uid
   - Validar: `currentUserUid != targetUserUid`

3. **Aislamiento por ecommerce**
   - STORE_ADMIN solo ve y actúa sobre usuarios de su ecommerce_id
   - SUPER_ADMIN tiene acceso global

4. **Auditoría previa a eliminación**
   - Antes de hard delete, registrar en audit_log los datos: user_id, username, email, role_name, ecommerce_id
   - Acción = "USER_DELETE"
   - Timestamp en UTC

5. **Cascada controlada**
   - audit_log.user_id → ON DELETE SET NULL (se preserva el histórico sin user_id)
   - Otras tablas que referencien app_user → ON DELETE RESTRICT (prevenir eliminación accidental)

6. **Validación de existencia**
   - Si uid no existe → HTTP 404

7. **Respuesta HTTP**
   - DELETE exitoso → HTTP 204 No Content (sin body)
   - Errores → JSON con campo "error" y HTTP 400/403/404

---

## 2. DISEÑO

### 2.1 Arquitectura Hexagonal (Ports & Adapters)

> **Referencia:** [.github/requirements/hexagonal-architecture.md](../.github/requirements/hexagonal-architecture.md)

#### Estructura de Carpetas para UserDelete

El feature debe seguir el modelo estándar Hexagonal Architecture:

```
src/main/java/com/loyalty/service_admin/
├── application/
│   ├── port/
│   │   ├── in/
│   │   │   └── UserDeleteUseCase.java          # Interface casos de uso DELETE
│   │   └── out/
│   │       ├── UserDeletePersistencePort.java  # Puerto persistencia (BD)
│   │       └── AuditLogPort.java               # Puerto auditoría
│   ├── service/
│   │   └── UserDeleteService.java           # Implementación del use case
│   └── dto/
│       └── user/
│           └── UserDeleteRequest.java          # DTO entrada (si aplica)
│
├── domain/
│   └── repository/
│       └── UserRepository.java                 # Interface JPA Repository
│
├── infrastructure/
│   ├── persistence/
│   │   └── jpa/
│   │       ├── JpaUserDeleteAdapter.java       # Adapter persistencia
│   │       └── JpaAuditLogAdapter.java         # Adapter auditoría
│   └── exception/
│       ├── GlobalExceptionHandler.java         # Manejo global de excepciones ✅
│       ├── ApiErrorResponse.java               # DTO respuesta error ✅
│       ├── BadRequestException.java            # Excepciones custom
│       ├── AuthorizationException.java
│       └── ResourceNotFoundException.java
│
└── presentation/
    └── controller/
        └── UserController.java                 # REST endpoint DELETE
```

#### Explicación de Componentes

| Componente | Responsabilidad | Implementación |
|-----------|-----------------|-----------------|
| `UserDeleteUseCase` | Define contrato de DELETE | Interface con método `void hardDeleteUser(UUID uid)` |
| `UserDeleteServiceImpl` | Implementa lógica de negocio | Valida autoz., audita, ejecuta hard delete |
| `UserDeletePersistencePort` | Abstrae acceso a BD | Interface `void deleteUser(UserEntity user)` |
| `JpaUserDeleteAdapter` | Implementa puerto persistencia | Inyecta `UserRepository` y delega |
| `AuditLogPort` | Abstrae logs de auditoría | Interface para registrar pre-delete |
| `JpaAuditLogAdapter` | Implementa auditoría | Inserta registros en `audit_log` |
| `UserController` | REST API | Inyecta `UserDeleteUseCase` (NO `ServiceImpl`) |
| `GlobalExceptionHandler` | Maneja excepciones globalmente | ✅ YA EXISTE — convierte excepciones a `ApiErrorResponse` |

#### Inyección de Dependencias

**En Controller (malo ❌):**
```java
private final UserDeleteServiceImpl service; // ❌ NUNCA
private final UserRepository repository;    // ❌ NUNCA
```

**En Controller (correcto ✅):**
```java
private final UserDeleteUseCase userDeleteUseCase; // ✅ SIEMPRE puerto entrada
```

**En Service (correcto ✅):**
```java
private final UserDeletePersistencePort persistencePort;  // ✅ Puerto salida
private final AuditLogPort auditLogPort;                  // ✅ Puerto salida
private final AuthorizationService authService;           // ✅ Servicio si necesaria
```

---

### 2.2 Modelos de Datos

#### Entidades afectadas

| Entidad | Almacén | Cambios | Descripción |
|---------|---------|---------|-------------|
| `UserEntity` | tabla `app_user` | ninguno (eliminación física) | Entidad se elimina completamente de BD |
| `AuditLog` (indirecta) | tabla `audit_log` | user_id → NULL (cascada ON DELETE SET NULL) | Registros históricos preservados sin referencia a usuario |

#### Campos del modelo
No hay cambios en la estructura de UserEntity. La eliminación es física (DELETE FROM).

**Campos registrados en audit_log previa a hard delete:**

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `user_id` | UUID | Identificador del usuario siendo eliminado |
| `username` | string | Nombre de usuario (para auditoría sin poder recuperarse) |
| `email` | string | Email del usuario (para auditoría) |
| `role_name` | string | Nombre del rol (STANDARD, STORE_ADMIN, SUPER_ADMIN) |
| `ecommerce_id` | UUID | Ecommerce al que pertenecía (para auditoría) |
| `action` | string | "USER_DELETE" |
| `actor_uid` | UUID | UID de quien realizó la eliminación |
| `timestamp` | datetime (UTC) | Cuando se ejecutó |

#### Índices / Constraints
- `idx_user_ecommerce`: Buscar usuarios por ecommerce (USED: hard delete by ecommerce)
- `idx_user_role`: Buscar por rol (USED: filtrado por autorización)
- Constraint: STORE_ADMIN solo puede eliminar usuarios de su ecommerce
- Constraint: No permitir auto-eliminación
- Constraint: audit_log.user_id ON DELETE SET NULL (permite hard delete sin orfandad)

### 2.3 API Endpoints

#### DELETE /api/v1/users/{uid}
- **Descripción**: Elimina un usuario de forma permanente e irrecuperable
- **Auth requerida**: sí (Bearer token OAuth/JWT con claims: `ecommerce_id`, `role`)
- **Parámetros**: 
  - Path: `uid` (string/UUID) — UID del usuario a eliminar
- **Request Body**: vacío (sin body)
- **Validaciones previas**:
  1. Token válido y no expirado → lanzar `JwtException` → **GlobalExceptionHandler** → 401 UNAUTHORIZED
  2. Usuario autenticado existe → lanzar `UnauthorizedException` → 401 UNAUTHORIZED
  3. Usuario target existe (uid) → lanzar `ResourceNotFoundException` → 404 NOT_FOUND
  4. Usuario autenticado != usuario target → lanzar `BadRequestException` → 400 BAD_REQUEST
  5. Si role=STORE_ADMIN: validar ecommerce_id coincida → lanzar `AuthorizationException` → 403 FORBIDDEN
  6. Si role no es SUPER_ADMIN o STORE_ADMIN → lanzar `AuthorizationException` → 403 FORBIDDEN

#### Response 204 No Content (exitoso)
```
(sin body)
HTTP 204 No Content
```

#### Response 400 Bad Request (auto-eliminación)
```json
{
  "timestamp": "2026-04-06T03:51:57.496130182Z",
  "status": 400,
  "error": "Bad Request",
  "code": "BAD_REQUEST",
  "message": "No puede eliminarse a sí mismo",
  "path": "/api/v1/users/user-abc-123"
}
```
**Excepción lanzada:** `BadRequestException("No puede eliminarse a sí mismo")`

#### Response 403 Forbidden (autorización denegada - rol insuficiente)
```json
{
  "timestamp": "2026-04-06T03:51:57.496130182Z",
  "status": 403,
  "error": "Forbidden",
  "code": "FORBIDDEN",
  "message": "No tiene permiso para eliminar este usuario",
  "path": "/api/v1/users/user-xyz-789"
}
```
**Excepción lanzada:** `AuthorizationException("No tiene permiso para eliminar este usuario")`

#### Response 403 Forbidden (autorización denegada - ecommerce cruzado)
```json
{
  "timestamp": "2026-04-06T03:51:57.496130182Z",
  "status": 403,
  "error": "Forbidden",
  "code": "FORBIDDEN",
  "message": "No puede eliminar usuarios de otro ecommerce",
  "path": "/api/v1/users/user-other-ecom-456"
}
```
**Excepción lanzada:** `AuthorizationException("No puede eliminar usuarios de otro ecommerce")`

#### Response 404 Not Found (usuario no existe)
```json
{
  "timestamp": "2026-04-06T03:51:57.496130182Z",
  "status": 404,
  "error": "Not Found",
  "code": "NOT_FOUND",
  "message": "Usuario no encontrado",
  "path": "/api/v1/users/uid-invalido-999"
}
```
**Excepción lanzada:** `ResourceNotFoundException("Usuario no encontrado")`

#### Response 401 Unauthorized (token faltante/expirado)
```json
{
  "timestamp": "2026-04-06T03:51:57.496130182Z",
  "status": 401,
  "error": "Unauthorized",
  "code": "UNAUTHORIZED",
  "message": "Token no válido o expirado",
  "path": "/api/v1/users/user-123"
}
```
**Excepción lanzada:** `JwtException` → capturada por **GlobalExceptionHandler**

**Nota:** El `GlobalExceptionHandler` existente convierte automáticamente las excepciones lanzadas al formato `ApiErrorResponse`. No es necesario capturar y convertir manualmente en el controller.

#### Workflow de Ejecución (en `UserDeleteServiceImpl`)

**En el Controller:**
```java
@DeleteMapping("/{uid}")
public ResponseEntity<Void> deleteUser(@PathVariable UUID uid) {
    userDeleteUseCase.hardDeleteUser(uid);  // Lanza excepciones → manejadas por GlobalExceptionHandler
    return ResponseEntity.noContent().build();
}
```

**En el Service (`UserDeleteServiceImpl`):**
```java
@Transactional
@Override
public void hardDeleteUser(UUID uid) {
    // 1. Obtener usuario autenticado del SecurityContext
    User currentUser = getCurrentUser(); // Si no existe → UnauthorizedException
    UUID currentUserUid = currentUser.getUid();
    
    // 2. Recuperar usuario a eliminar
    UserEntity targetUser = userDeletePersistencePort.findById(uid)
        .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    
    // 3. Validar: No auto-eliminación
    if (currentUserUid.equals(targetUser.getUid())) {
        throw new BadRequestException("No puede eliminarse a sí mismo");
    }
    
    // 4. Validar: Acceso por rol
    if (!isAdminRole(currentUser.getRole())) {
        throw new AuthorizationException("No tiene permiso para eliminar este usuario");
    }
    
    // 5. Validar: Aislamiento por ecommerce (si es STORE_ADMIN)
    if (isStoreAdmin(currentUser.getRole())) {
        if (!currentUser.getEcommerceId().equals(targetUser.getEcommerceId())) {
            throw new AuthorizationException("No puede eliminar usuarios de otro ecommerce");
        }
    }
    
    // 6. Registrar en audit_log ANTES de eliminar
    auditLogPort.logUserDeletion(
        targetUser.getUid(),
        targetUser.getUsername(),
        targetUser.getEmail(),
        targetUser.getRole().getName(),
        targetUser.getEcommerceId(),
        currentUserUid,  // actor
        Instant.now()
    );
    
    // 7. Ejecutar hard delete (eliminación física)
    userDeletePersistencePort.deleteUser(targetUser);
    
    // 8. Log info
    logger.info("Hard delete ejecutado: uid={}, username={}, ecommerce={}, actor={}",
        targetUser.getUid(), targetUser.getUsername(), targetUser.getEcommerceId(), currentUserUid);
    
    // 9. GlobalExceptionHandler convierte excepciones a ApiErrorResponse automáticamente
    // Si llegamos aquí (sin excepciones), controller retorna 204 No Content
}
```

**Excepciones lanzadas y su mapeo:**

| Excepción | HTTP | Code | Manejada por |
|-----------|------|------|--------------|
| `ResourceNotFoundException` | 404 | NOT_FOUND | GlobalExceptionHandler |
| `BadRequestException` | 400 | BAD_REQUEST | GlobalExceptionHandler |
| `AuthorizationException` | 403 | FORBIDDEN | GlobalExceptionHandler |
| `UnauthorizedException` | 401 | UNAUTHORIZED | GlobalExceptionHandler |
| `JwtException` | 401 | UNAUTHORIZED | GlobalExceptionHandler |

### 2.4 Diseño Frontend

#### Cambios de UI (si aplica)
- **Modal de confirmación**: Ya debe existir un modal de "¿Estás seguro de eliminar?". Verificar que:
  - Muestra nombre del usuario a eliminar
  - Advierte que la eliminación es permanente e irrecuperable
  - Botón rojo "Eliminar" + "Cancelar"
- **Handling de 204**: Frontend debe refrescar la tabla y mostrar notificación "Usuario eliminado exitosamente"
- **Handling de errores**: 
  - 400/403/404 → mostrar mensaje de error en toast (leer campo `message` del `ApiErrorResponse`)
  - 401 → redirigir a login

#### Servicios (llamadas API)

**Función deleteUser con manejo correcto de ApiErrorResponse:**

```javascript
export const deleteUser = async (uid, token) => {
  const response = await fetch(`${API_BASE_URL}/api/v1/users/${uid}`, {
    method: 'DELETE',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  });

  // Success: 204 No Content
  if (response.status === 204) {
    return { success: true, message: 'Usuario eliminado exitosamente' };
  }

  // Error: parsear ApiErrorResponse
  const errorData = await response.json(); // { timestamp, status, error, code, message, path }
  
  // Errores específicos con manejo diferente
  switch (response.status) {
    case 400:
      // Bad Request: auto-eliminación intentada
      throw new Error(errorData.message || 'Solicitud inválida');
    
    case 401:
      // Unauthorized: token expirado o ausente
      console.warn('Token expirado, redirigiendo a login...');
      window.location.href = '/login'; // Redirigir fuera de promise
      return;
    
    case 403:
      // Forbidden: sin permiso (rol o ecommerce)
      throw new Error(errorData.message || 'No tiene permiso para realizar esta acción');
    
    case 404:
      // Not Found: usuario no existe
      throw new Error(errorData.message || 'Usuario no encontrado');
    
    default:
      throw new Error(errorData.message || 'Error desconocido al eliminar usuario');
  }
};
```

**Uso en componente con manejo de errores:**

```javascript
const handleDeleteUser = async (uid) => {
  setLoading(true);
  try {
    await deleteUser(uid, token);
    
    // Éxito: refrescar tabla y mostrar toast
    await refetchUsers();
    showToast('Usuario eliminado exitosamente', 'success');
    closeDeleteModal();
    
  } catch (error) {
    // Error: mostrar toast con mensaje del ApiErrorResponse
    showToast(error.message, 'error');
  } finally {
    setLoading(false);
  }
};
```

---

## 3. LISTA DE TAREAS

> Checklist accionable para Backend, Frontend y QA. 
> Estado actual: HU-02 parcialmente implementada (Create/Read/Update OK, Delete en SOFT DELETE).

### Backend

#### Implementación de Hard Delete

- [x] **HU-02 Create User** — endpoint POST /api/v1/users ✅
- [x] **HU-02 Read Users** — endpoint GET /api/v1/users, GET /api/v1/users/{uid} ✅
- [x] **HU-02 Update User** — endpoint PUT /api/v1/users/{uid} ✅
- [x] **UserService.createUser()** — implementado ✅
- [x] **UserService.listUsers()** — con aislamiento por ecommerce ✅
- [x] **UserService.getUserByUid()** — con autorización ✅
- [x] **UserService.updateUser()** — con autorización ✅
- [ ] **UserService.deleteUser()** — cambiar de soft delete a hard delete ❌
  - [ ] Validar que usuario no intente auto-eliminarse
  - [ ] Validar autorización (SUPER_ADMIN o STORE_ADMIN)
  - [ ] Validar aislamiento por ecommerce (STORE_ADMIN)
  - [ ] Crear registro en audit_log antes de eliminar (action="USER_DELETE", capturar username, email, role_name, ecommerce_id)
  - [ ] Ejecutar userRepository.delete(user) en vez de setIsActive(false)
  - [ ] Log info: "Hard delete ejecutado: uid={}, username={}, ecommerce={}, actor={}"
- [ ] **Quitar campo isActive de consultas** (opcional, pero limpiar lógica soft-delete antigua)
  - [ ] Revisar si hay `findByIsActiveTrue()` que deba ajustarse
  - [ ] Revisar filtros en listUsers() que dependan de isActive
- [ ] **UserController.deleteUser()** — endpoint DELETE /api/v1/users/{uid} ❌
  - [ ] Recibir @PathVariable UUID uid
  - [ ] Validar token JWT
  - [ ] Llamar userService.deleteUser(uid)
  - [ ] Retornar ResponseEntity.noContent() (HTTP 204)
  - [ ] Manejo de excepciones (BadRequestException, AuthorizationException, ResourceNotFoundException)

#### Tests Backend — Hard Delete

- [ ] **test_userService_hardDelete_success** — happy path ❌
  - [ ] GIVEN: usuario autenticado SUPER_ADMIN
  - [ ] WHEN: deleteUser(userUid)
  - [ ] THEN: usuario eliminado de BD, audit_log registrado
- [ ] **test_userService_hardDelete_storeAdminOwnEcommerce** — scope de ecommerce ❌
  - [ ] GIVEN: STORE_ADMIN en ecom-123, usuario en ecom-123
  - [ ] WHEN: deleteUser(userUid)
  - [ ] THEN: usuario eliminado
- [ ] **test_userService_hardDelete_storeAdminCrossEcommerce_throws403** — prevención ❌
  - [ ] GIVEN: STORE_ADMIN en ecom-123, usuario en ecom-456
  - [ ] WHEN: deleteUser(userUid)
  - [ ] THEN: AuthorizationException lanzada
- [ ] **test_userService_hardDelete_selfDelete_throws400** — auto-eliminación ❌
  - [ ] GIVEN: usuario (uid=123) intenta DeleteUser(123)
  - [ ] WHEN: deleteUser(uid=123)
  - [ ] THEN: BadRequestException "No puede eliminarse a sí mismo"
- [ ] **test_userService_hardDelete_notFound_throws404** — usuario no existe ❌
  - [ ] GIVEN: uid=invalid-999
  - [ ] WHEN: deleteUser(uid=invalid-999)
  - [ ] THEN: ResourceNotFoundException lanzada
- [ ] **test_userService_hardDelete_auditLogCreated** — auditoría previa ❌
  - [ ] GIVEN: usuario con datos completos
  - [ ] WHEN: deleteUser()
  - [ ] THEN: audit_log registra: user_id, username, email, role_name, ecommerce_id, action="USER_DELETE", actor_uid, timestamp
- [ ] **test_userService_hardDelete_cascadeAuditLog** — cascada ON DELETE SET NULL ❌
  - [ ] GIVEN: usuario con múltiples registros en audit_log
  - [ ] WHEN: deleteUser()
  - [ ] THEN: audit_log.user_id = NULL, histórico preservado
- [ ] **test_userService_hardDelete_standardUserCannotDelete_throws403** — validar rol ❌
  - [ ] GIVEN: usuario STANDARD intenta eliminar otro usuario
  - [ ] WHEN: deleteUser()
  - [ ] THEN: AuthorizationException
- [ ] **test_userController_deleteUser_returns204** — endpoint ❌
  - [ ] WHEN: DELETE /api/v1/users/{uid} con token SUPER_ADMIN válido
  - [ ] THEN: ResponseEntity status 204 No Content

### Frontend

#### Implementación de UI Hard Delete

- [ ] **UserTable / UserList component** — botón DELETE ❌
  - [ ] Verificar que existe botón "Eliminar" en cada fila
  - [ ] Click abre ConfirmDeleteModal
- [ ] **ConfirmDeleteModal component** — modal de confirmación ❌
  - [ ] Mostrar nombre del usuario a eliminar
  - [ ] Texto: "Esta acción es permanente e irrecuperable"
  - [ ] Campos: nombre usuario, email (opcional)
  - [ ] Botón "Eliminar" (rojo, deshabilitado mientras procesa)
  - [ ] Botón "Cancelar"
- [ ] **useUser hook** — función deleteUser ❌
  - [ ] Llamar a deleteUser(uid) del service
  - [ ] Manejo de loading state
  - [ ] Manejo de error (mostrar toast)
  - [ ] Éxito: cerrar modal, refrescar tabla, mostrar toast "Usuario eliminado"
- [ ] **deleteUserService function** — DELETE endpoint ❌
  - [ ] Hacer fetch DELETE /api/v1/users/{uid}
  - [ ] Header Authorization: Bearer {token}
  - [ ] Manejar 204 (éxito)
  - [ ] Manejar 400/403/404 (errores con mensaje)
  - [ ] Manejar 401 (redirigir a login)

#### Tests Frontend — Hard Delete

- [ ] **test_UserListComponent_deleteButton_opensModal** ❌
  - [ ] WHEN: click "Eliminar" en una fila
  - [ ] THEN: ConfirmDeleteModal abre
- [ ] **test_ConfirmDeleteModal_cancel_closeModal** ❌
  - [ ] WHEN: click "Cancelar"
  - [ ] THEN: modal cierra, tabla NO cambia
- [ ] **test_ConfirmDeleteModal_delete_calls_service** ❌
  - [ ] WHEN: click "Eliminar"
  - [ ] THEN: deleteUserService(uid) llamado
- [ ] **test_deleteUserService_success_204** ❌
  - [ ] WHEN: DELETE /api/v1/users/{uid} retorna 204
  - [ ] THEN: resolve con { success: true }
- [ ] **test_deleteUserService_error_400_throws** ❌
  - [ ] WHEN: DELETE retorna 400 + { error: "..." }
  - [ ] THEN: throw Error con mensaje
- [ ] **test_UserListComponent_afterDelete_refreshTable** ❌
  - [ ] WHEN: user eliminado exitosamente
  - [ ] THEN: tabla refrescada, usuario ya no aparece
- [ ] **test_UserListComponent_afterDelete_showToast** ❌
  - [ ] WHEN: user eliminado exitosamente
  - [ ] THEN: toast muestra "Usuario eliminado"

### QA / Integración

#### Test Cases Gherkin y Integración

- [ ] **test_hardDelete_full_flow_SUPER_ADMIN** ❌
  - Crear usuario → List → Delete → Verificar 204 → Verificar desaparece de BD
- [ ] **test_hardDelete_full_flow_STORE_ADMIN** ❌
  - STORE_ADMIN crea usuario en su ecommerce → Delete → Verificar 204
- [ ] **test_hardDelete_auditLog_preserved** ❌
  - Crear usuario → generar audit_log entries → Delete → Verificar audit_log.user_id=NULL, histórico intacto
- [ ] **test_hardDelete_cascadeConstraints** ❌
  - Intentar eliminar usuario con relaciones restringidas (si las hay) → verificar comportamiento
- [ ] **Performance test: hard delete time < 500ms** ❌
  - Medir tiempo de ejecución DELETE en usuarios sin relaciones complejas

---

## 4. RIESGOS IDENTIFICADOS

### Riesgo 1: Pérdida Permanente de Datos (CRÍTICO)
- **Severidad**: CRÍTICO (imposible recuperar post-hard delete)
- **Probabilidad**: Media (error humano, bugs, malicia)
- **Impacto**: Pérdida total del usuario, accionable legalmente
- **Mitigación**:
  - ✅ Auditoría previa: registrar todos los datos en AuditLog antes de eliminar
  - ✅ Validaciones estrictas: no permitir auto-eliminación
  - ✅ Logs detallados: registrar actor, timestamp, uid
  - ✅ Modal de confirmación con advertencia: "Esta acción es permanente"
  - ✅ Backups regulares de BD (fuera de scope, pero recomendado)

### Riesgo 2: Cascada Incorrecta en Relaciones (ALTO)
- **Severidad**: ALTO (integridad referencial comprometida)
- **Probabilidad**: Media (schema no validado)
- **Impacto**: Orfandad de audit_log, restricciones de FK que impiden delete
- **Mitigación**:
  - ✅ Validar ON DELETE actions en schema:
    - audit_log.user_id → ON DELETE SET NULL ✅
    - Otras FKs → ON DELETE RESTRICT (prevenir accidental delete)
  - ✅ Test: test_hardDelete_cascadeAuditLog verifica que user_id=NULL post-delete
  - ✅ Test: test_hardDelete_cascadeConstraints verifica que restricciones funcionan

### Riesgo 3: Vulnerabilidad de Autorización (ALTO)
- **Severidad**: ALTO (acceso no autorizado a recursos)
- **Probabilidad**: Media (lógica compleja de RBAC)
- **Impacto**: STORE_ADMIN podría eliminar usuarios de otros ecommerce
- **Mitigación**:
  - ✅ Validación exhaustiva en UserService.deleteUser():
    - currentUserUid != uid
    - canActOnUser(ecommerceId, uid) check
    - Role validation (solo SUPER_ADMIN, STORE_ADMIN)
  - ✅ Test: test_storeAdminCrossEcommerce_throws403
  - ✅ Log: registrar intentos fallidos

### Riesgo 4: Auto-Eliminación (MEDIO)
- **Severidad**: MEDIO (usuario se auto-elimina, no puede recuperarse)
- **Probabilidad**: Baja (pero evitable)
- **Impacto**: Pérdida de acceso permanente
- **Mitigación**:
  - ✅ Validación: currentUserUid != uid (línea `if (currentUserUid.equals(uid))`)
  - ✅ Test: test_selfDelete_throws400

### Riesgo 5: Inconsistencia en Frontend + Backend (MEDIO)
- **Severidad**: MEDIO (UI desincronizada con BD)
- **Probabilidad**: Media (race conditions)
- **Impacto**: Usuario ve usuario "fantasma" en tabla
- **Mitigación**:
  - ✅ Frontend refrescar tabla post-delete
  - ✅ Test: test_afterDelete_refreshTable
  - ✅ Optimismo: eliminar de UI antes de confirmar (con rollback en error)

### Riesgo 6: Auditoría Insuficiente (BAJO)
- **Severidad**: BAJO (compliance, GDPR)
- **Probabilidad**: Baja (se está implementando auditoría previa)
- **Impacto**: Incumplimiento normativo
- **Mitigación**:
  - ✅ AuditLog captura: user_id, username, email, role_name, ecommerce_id, actor_uid, timestamp, action="USER_DELETE"
  - ✅ Test: test_auditLogCreated verifica todos los campos

---

## 5. CONSIDERACIONES DE AUDITORÍA

### Datos Capturados Pre-Hard Delete
Antes de ejecutar `DELETE FROM app_user WHERE id=uid`, se DEBE crear un registro en `audit_log` con:

| Campo | Valor | Descripción |
|-------|-------|-------------|
| `id` | UUID (auto) | PK del audit_log |
| `user_id` | {uuid del usuario} | UID del usuario siendo eliminado |
| `username` | {nombre de usuario} | Para auditoría sin poder recuperar |
| `email` | {email del usuario} | Para auditoría |
| `role_name` | "STANDARD" \| "STORE_ADMIN" \| "SUPER_ADMIN" | Rol del usuario eliminado |
| `ecommerce_id` | {uuid} | Ecommerce al que pertenecía |
| `action` | "USER_DELETE" | Tipo de operación |
| `actor_uid` | {uuid del actual user} | UID de quién ejecutó el delete |
| `timestamp` | Instant.now() UTC | Cuándo se ejecutó |

### Flujo de Eliminación (Orden crítico)
1. **ANTES** de delete: `auditService.logUserDeletion(user, currentUserUid)`
   - Captura todos los campos de UserEntity
   - Inserta fila en audit_log con action="USER_DELETE"
2. **DESPUÉS** de auditoría exitosa: `userRepository.delete(user)` (hard delete)
3. **CASCADA**: audit_log.user_id para los registros antiguos → ON DELETE SET NULL (preserva histórico)
4. **LOG**: "Hard delete ejecutado: uid={}, username={}, ecommerce={}, actor={}, timestamp={}"

### Implicaciones de Hard Delete en Auditoría
- ✅ Los registros `audit_log` con `action != USER_DELETE` tendrán `user_id=NULL` post-hard delete
- ✅ Los registros `audit_log` con `action=USER_DELETE` tendrán el `user_id` capturado (momentáneamente, luego NULL por cascada)
- ✅ El histórico se preserva: "El usuario fue eliminado en {timestamp} por {actor_uid}"

### Compliance y Políticas
- **GDPR Derecho al Olvido**: Hard delete cumple, auditLog preserva histórico sin datos personales identificables (user_id=NULL)
- **Trazabilidad**: actor_uid + timestamp permiten auditar quién eliminó qué y cuándo
- **Irrecuperabilidad**: Hard delete es permanente, no hay "undelete"

---

## 6. NOTAS DE IMPLEMENTACIÓN

### 6.1 Estructura Hexagonal Requerida

**OBLIGATORIO:** Seguir el modelo de [.github/requirements/hexagonal-architecture.md](../.github/requirements/hexagonal-architecture.md)

#### Interfaces/Puertos a Crear

1. **`UserDeleteUseCase` (port/in/)**
```java
public interface UserDeleteUseCase {
    void hardDeleteUser(UUID uid);  // Lanza excepciones (capturadas por GlobalExceptionHandler)
}
```

2. **`UserDeletePersistencePort` (port/out/)**
```java
public interface UserDeletePersistencePort {
    Optional<UserEntity> findById(UUID uid);
    void deleteUser(UserEntity user);
}
```

3. **`AuditLogPort` (port/out/)**
```java
public interface AuditLogPort {
    void logUserDeletion(UUID userId, String username, String email, 
                         String roleName, UUID ecommerceId, UUID actorUid, Instant timestamp);
}
```

#### Implementaciones a Crear

1. **`UserDeleteServiceImpl` (service/impl/)** - implementa `UserDeleteUseCase`
   - Inyecta `UserDeletePersistencePort` (NO `UserRepository` directo)
   - Inyecta `AuditLogPort` para logging
   - Contiene toda la lógica de validación y hard delete
   - **Lanza excepciones custom** (BadRequestException, AuthorizationException, ResourceNotFoundException)

2. **`JpaUserDeleteAdapter` (infrastructure/persistence/jpa/)** - implementa `UserDeletePersistencePort`
   - Inyecta `UserRepository` (aquí SÍ es permitido, es un adapter)
   - Delega operaciones CRUD a repository

3. **`JpaAuditLogAdapter` (infrastructure/persistence/jpa/)** - implementa `AuditLogPort`
   - Inyecta `AuditLogRepository`
   - Crea registros en `audit_log` previa a hard delete

#### Controller (@RestController)
```java
@DeleteMapping("/{uid}")
public ResponseEntity<Void> deleteUser(@PathVariable UUID uid) {
    // ✅ CORRECTO: inyecta UserDeleteUseCase (puerto entrada)
    userDeleteUseCase.hardDeleteUser(uid);
    // GlobalExceptionHandler convierte excepciones a ApiErrorResponse automáticamente
    return ResponseEntity.noContent().build();
}
```

### 6.2 Manejo de Excepciones

**GlobalExceptionHandler YA EXISTE** — Solo lanzar las excepciones correctas, el handler convierte a ApiErrorResponse.

| Caso | Excepción a Lanzar | HTTP | Code |
|------|-------------------|------|------|
| Usuario no existe | `ResourceNotFoundException("Usuario no encontrado")` | 404 | NOT_FOUND |
| Auto-eliminación | `BadRequestException("No puede eliminarse a sí mismo")` | 400 | BAD_REQUEST |
| Rol insuficiente | `AuthorizationException("No tiene permiso para eliminar este usuario")` | 403 | FORBIDDEN |
| Ecommerce cruzado | `AuthorizationException("No puede eliminar usuarios de otro ecommerce")` | 403 | FORBIDDEN |
| Token inválido | Lanzado por security filter (JwtException) | 401 | UNAUTHORIZED |

### 6.3 Patrón Transaccional

```java
@Service
public class UserDeleteServiceImpl implements UserDeleteUseCase {
    
    private final UserDeletePersistencePort persistencePort;
    private final AuditLogPort auditLogPort;
    private final SecurityContextService securityService;
    private static final Logger logger = LoggerFactory.getLogger(UserDeleteServiceImpl.class);
    
    @Transactional  // ✅ IMPORTANTE: wrapping la lógica en transacción
    @Override
    public void hardDeleteUser(UUID uid) {
        
        // 1. Usuario autenticado (extraído del SecurityContext)
        User currentUser = securityService.getCurrentUser();  // Lanza UnauthorizedException si no existe
        
        // 2. Validar que usuario target existe
        UserEntity targetUser = persistencePort.findById(uid)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
        // 3. Self-delete check
        if (currentUser.getUid().equals(targetUser.getUid())) {
            throw new BadRequestException("No puede eliminarse a sí mismo");
        }
        
        // 4. Role validation
        if (!isAdminRole(currentUser.getRole())) {
            throw new AuthorizationException("No tiene permiso para eliminar este usuario");
        }
        
        // 5. Ecommerce validation (STORE_ADMIN only)
        if (isStoreAdmin(currentUser)) {
            if (!currentUser.getEcommerceId().equals(targetUser.getEcommerceId())) {
                throw new AuthorizationException("No puede eliminar usuarios de otro ecommerce");
            }
        }
        
        // 6. LOG EN AUDIT_LOG ANTES DE ELIMINAR (crítico)
        auditLogPort.logUserDeletion(
            targetUser.getUid(),
            targetUser.getUsername(),
            targetUser.getEmail(),
            targetUser.getRole().getName(),
            targetUser.getEcommerceId(),
            currentUser.getUid(),
            Instant.now()
        );
        
        // 7. HARD DELETE
        persistencePort.deleteUser(targetUser);
        
        // 8. LOG INFO
        logger.info("Hard delete ejecutado: uid={}, username={}, ecommerce={}, actor={}",
            targetUser.getUid(), targetUser.getUsername(), targetUser.getEcommerceId(), currentUser.getUid());
        
        // 9. Si no lanza excepción, controller retorna 204 No Content
    }
    
    private boolean isAdminRole(Role role) {
        return role == Role.SUPER_ADMIN || role == Role.STORE_ADMIN;
    }
    
    private boolean isStoreAdmin(User user) {
        return user.getRole() == Role.STORE_ADMIN;
    }
}
```

### 6.4 Schema BD a Verificar

```sql
-- Verificar cascada en audit_log
ALTER TABLE audit_log 
ADD CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES app_user(id)
ON DELETE SET NULL;  -- ✅ DEBE SER SET NULL

-- Otras FK a app_user: RESTRICT (previene accidental delete)
ALTER TABLE user_role
ADD CONSTRAINT fk_user_role FOREIGN KEY (user_id) REFERENCES app_user(id)
ON DELETE RESTRICT;  -- Protege integridad
```

### 6.5 Tests a Ejecutar

```bash
# Backend
mvn clean test -Dtest="UserDeleteServiceTest"
mvn clean test -Dtest="UserControllerTest#*delete*"

# Con coverage
mvn clean test jacoco:report  # Verificar >95%

# Frontend
npm test -- UserList.test.js
npm test -- deleteUserService.test.js
```

### 6.6 Checklist Previo a Merge

- [ ] Estructura de carpetas sigue Hexagonal Architecture
- [ ] Controller inyecta `UserDeleteUseCase` (NO `ServiceImpl`)
- [ ] Service inyecta puertos (NO repositories directo)
- [ ] GlobalExceptionHandler maneja todas las excepciones
- [ ] Transacción en `@Transactional` cubre audit + delete
- [ ] `audit_log.user_id` tiene `ON DELETE SET NULL`
- [ ] Tests unitarios pasan (>95% coverage)
- [ ] Frontend maneja `ApiErrorResponse` correctamente

---

## 7. ESTRATEGIA DE TESTING — TDD

> **Enfoque Core:** Test-Driven Development. **Los tests se escriben PRIMERO**, luego implementación.
> **Prioridad:** Controllers + Services (unitarios) > Integración > E2E

### 7.1 Test Strategy: Backend

#### Fase 1: Unit Tests — Controllers (PRIMERO)

**Archivo objetivo:** `UserControllerTest.java`

```java
// Test 1: DELETE endpoint retorna 204 exitosamente
@Test
public void testDeleteUser_Success_Returns204() {
    // GIVEN: SUPER_ADMIN token válido, usuario existente
    // WHEN: DELETE /api/v1/users/{uid}
    // THEN: status 204 No Content
}

// Test 2: DELETE endpoint retorna 400 si self-delete
@Test
public void testDeleteUser_SelfDelete_Returns400() {
    // GIVEN: usuario con uid=X intenta DELETE /api/v1/users/X
    // WHEN: DELETE request
    // THEN: status 400, error="No puede eliminarse a sí mismo"
}

// Test 3: DELETE endpoint retorna 403 si no es ADMIN
@Test
public void testDeleteUser_StandardUserCannotDelete_Returns403() {
    // GIVEN: STANDARD user token
    // WHEN: DELETE /api/v1/users/{otherUserUid}
    // THEN: status 403, error="No tiene permiso"
}

// Test 4: DELETE endpoint retorna 403 si STORE_ADMIN intenta cross-ecommerce
@Test
public void testDeleteUser_CrossEcommerceDelete_Returns403() {
    // GIVEN: STORE_ADMIN(ecom=A) intenta DELETE usuario de ecom=B
    // WHEN: DELETE /api/v1/users/{uidInEcomB}
    // THEN: status 403
}

// Test 5: DELETE endpoint retorna 404 si usuario no existe
@Test
public void testDeleteUser_NotFound_Returns404() {
    // GIVEN: uid=invalid-999 (no existe)
    // WHEN: DELETE /api/v1/users/invalid-999
    // THEN: status 404, error="Usuario no encontrado"
}

// Test 6: DELETE endpoint retorna 401 si token falta/expira
@Test
public void testDeleteUser_NoToken_Returns401() {
    // WHEN: DELETE sin header Authorization
    // THEN: status 401
}
```

#### Fase 2: Unit Tests — Services

**Archivo objetivo:** `UserServiceTest.java`

```java
// Test 7: UserService.hardDeleteUser() elimina físicamente usuario
@Test
public void testHardDeleteUser_Success() {
    // GIVEN: usuario válido, autorización OK
    // WHEN: userService.hardDeleteUser(uid)
    // THEN: usuario no existe en BD (verify delete fue llamado)
}

// Test 8: UserService.hardDeleteUser() lanza BadRequestException si self-delete
@Test
public void testHardDeleteUser_SelfDelete_ThrowsBadRequest() {
    // GIVEN: uid == currentUserUid
    // WHEN: hardDeleteUser(uid)
    // THEN: BadRequestException("No puede eliminarse a sí mismo")
}

// Test 9: UserService.hardDeleteUser() lanza AuthorizationException si no es ADMIN
@Test
public void testHardDeleteUser_NotAdmin_ThrowsAuthorizationException() {
    // GIVEN: currentUser.role = STANDARD
    // WHEN: hardDeleteUser(otherUid)
    // THEN: AuthorizationException
}

// Test 10: UserService.hardDeleteUser() valida ecommerce_id para STORE_ADMIN
@Test
public void testHardDeleteUser_StoreAdminCrossEcommerce_ThrowsAuthorizationException() {
    // GIVEN: STORE_ADMIN(ecom=A) intenta DELETE user(ecom=B)
    // WHEN: hardDeleteUser(userUidInEcomB)
    // THEN: AuthorizationException
}

// Test 11: UserService.hardDeleteUser() lanza ResourceNotFoundException si no existe
@Test
public void testHardDeleteUser_NotFound_ThrowsResourceNotFound() {
    // GIVEN: uid=invalid
    // WHEN: hardDeleteUser(uid=invalid)
    // THEN: ResourceNotFoundException
}

// Test 12: UserService crea audit_log ANTES de hard delete
@Test
public void testHardDeleteUser_CreatesAuditLogBeforeDelete() {
    // GIVEN: usuario con datos completos
    // WHEN: hardDeleteUser(uid)
    // THEN: 
    //   - auditService.logUserDeletion(user, currentUid) fue llamado
    //   - audit_log contiene: user_id, username, email, role_name, ecommerce_id, action="USER_DELETE"
}

// Test 13: Cascada ON DELETE SET NULL funciona post-hard delete
@Test
public void testHardDeleteUser_CascadeAuditLog_UserIdBecomesNull() {
    // GIVEN: usuario con múltiples audit_log entries
    // WHEN: hardDeleteUser(uid)
    // THEN: usuario eliminado, audit_log.user_id = NULL (para registros antiguos)
}
```

#### Fase 3: Integration Tests (opcional, post unit tests)

```java
// Test 14: Hard delete + cascada + BD
@Test
@Transactional
public void testHardDeleteUser_Integration_DeleteAndCascade() {
    // FULL FLOW: create user → log audit entries → hard delete → verify user gone, audit_log preserved
}
```

### 7.2 Test Strategy: Frontend

#### Fase 1: Unit Tests — Components + Services

**Archivo objetivo:** `UserList.test.js`, `ConfirmDeleteModal.test.js`, `userService.test.js`

```javascript
// Test 1: click "Eliminar" abre ConfirmDeleteModal
test("UserList: click Eliminar button opens ConfirmDeleteModal", () => {
  // WHEN: user clicks button with onClick={openDeleteModal(uid)}
  // THEN: <ConfirmDeleteModal isOpen={true} /> rendered
});

// Test 2: click "Cancelar" cierra modal
test("ConfirmDeleteModal: click Cancelar closes modal", () => {
  // WHEN: click "Cancelar" button
  // THEN: isOpen=false, UserList rendered again
});

// Test 3: click "Eliminar" en modal llama deleteUserService(uid)
test("ConfirmDeleteModal: click Eliminar calls deleteUserService", async () => {
  // GIVEN: modal abierto con uid=X
  // WHEN: click "Eliminar" button
  // THEN: deleteUserService(X) called
});

// Test 4: deleteUserService retorna { success: true } si 204
test("deleteUserService: 204 returns { success: true }", async () => {
  // GIVEN: fetch mock retorna 204
  // WHEN: deleteUserService(uid)
  // THEN: resolve { success: true }
});

// Test 5: deleteUserService lanza Error si 400/403/404
test("deleteUserService: 400 throws Error with message", async () => {
  // GIVEN: fetch retorna 400 + { error: "..." }
  // WHEN: deleteUserService(uid)
  // THEN: throw Error(errorData.error)
});

// Test 6: UserList refrescar tabla post-delete
test("UserList: after delete refresh table (removes user from rows)", async () => {
  // GIVEN: usuario visible en tabla
  // WHEN: delete exitoso
  // THEN: usuario NO aparece en tabla, fetchUsers() llamado
});

// Test 7: Toast success post-delete
test("UserList: after delete shows success toast", async () => {
  // WHEN: delete exitoso
  // THEN: toast("Usuario eliminado")
});

// Test 8: Toast error si 400/403/404
test("UserList: error toast on 400/403/404", async () => {
  // WHEN: delete retorna error
  // THEN: toast(error.message)
});

// Test 9: Redirect to login si 401
test("deleteUserService: 401 redirige a login", async () => {
  // GIVEN: fetch retorna 401
  // WHEN: deleteUserService(uid)
  // THEN: window.location = "/login"
});
```

### 7.3 Coverage Esperado

| Componente | Coverage Esperado | Tests |
|------------|-------------------|-------|
| UserController#deleteUser() | 100% | 6 tests (controller) |
| UserService#hardDeleteUser() | 100% | 7 tests (service) |
| UserList component | 90%+ | 3 tests (list, delete button, refresh) |
| ConfirmDeleteModal | 90%+ | 3 tests (confirm, cancel, delete) |
| deleteUserService() | 100% | 5 tests (204, 400, 403, 404, 401) |
| **Total Estimado** | **80%+** | **24+ tests** |
