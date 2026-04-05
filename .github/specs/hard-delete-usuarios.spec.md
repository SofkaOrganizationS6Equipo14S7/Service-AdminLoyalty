---
id: SPEC-003
status: DRAFT
feature: hu-02-hard-delete-usuarios
created: 2026-04-05
updated: 2026-04-05
author: spec-generator
version: "1.0"
related-specs: []
---

# Spec: HU-02 Gestión de Usuarios por Ecommerce - Hard Delete

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de iniciar implementación.
> **Ficción técnica:** Cambiar el mecanismo de eliminación de usuarios de soft delete (isActive=false) a hard delete (eliminación física de la base de datos).

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

### Modelos de Datos

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

### API Endpoints

#### DELETE /api/v1/users/{uid}
- **Descripción**: Elimina un usuario de forma permanente e irrecuperable
- **Auth requerida**: sí (Bearer token OAuth/JWT con claims: `ecommerce_id`, `role`)
- **Parámetros**: 
  - Path: `uid` (string/UUID) — UID del usuario a eliminar
- **Request Body**: vacío (sin body)
- **Validaciones previas**:
  1. Token válido y no expirado
  2. Usuario autenticado existe
  3. Usuario target existe (uid)
  4. Usuario autenticado != usuario target (validar uid !== currentUserUid)
  5. Si role=STORE_ADMIN: ecommerce_id del usuario target == ecommerce_id del currentUser
  6. Si role no es SUPER_ADMIN o STORE_ADMIN: retornar 403
- **Response 204 No Content** (exitoso):
  ```
  (sin body)
  HTTP 204 No Content
  ```
- **Response 400 Bad Request** (error validación):
  ```json
  {
    "error": "No puede eliminarse a sí mismo"
  }
  ```
- **Response 403 Forbidden** (autorización denegada):
  ```json
  {
    "error": "No tiene permiso para eliminar este usuario"
  }
  ```
- **Response 404 Not Found** (usuario no existe):
  ```json
  {
    "error": "Usuario no encontrado"
  }
  ```
- **Response 401 Unauthorized** (token faltante o expirado):
  ```json
  {
    "error": "Token ausente o expirado"
  }
  ```

**Workflow de ejecución:**
1. Validar token y extraer claims (role, ecommerce_id, uid=currentUserUid)
2. Recuperar UserEntity del uid (si no existe → 404)
3. Si currentUserUid == uid → 400 "No puede eliminarse a sí mismo"
4. Si role != SUPER_ADMIN Y role != STORE_ADMIN → 403 "No tiene permiso"
5. Si role == STORE_ADMIN Y ecommerce_id != user.ecommerce_id → 403 "No tiene permiso para eliminar este usuario"
6. **Crear registro en audit_log** con action=USER_DELETE, capturando: user_id, username, email, role_name, ecommerce_id, actor_uid, timestamp
7. **Ejecutar DELETE** físicamente (userRepository.delete(user))
8. Retornar 204 No Content
9. Log: "Hard delete ejecutado: uid={uid}, username={username}, ecommerce={ecommerce_id}, actor={currentUserUid}"

### Diseño Frontend

#### Cambios de UI (si aplica)
- **Modal de confirmación**: Ya debe existir un modal de "¿Estás seguro de eliminar?". Verificar que:
  - Muestra nombre del usuario a eliminar
  - Advierte que la eliminación es permanente e irrecuperable
  - Botón rojo "Eliminar" + "Cancelar"
- **Handling de 204**: Frontend debe refrescar la tabla y mostrar notificación "Usuario eliminado exitosamente"
- **Handling de errores**: 
  - 400/403/404 → mostrar mensaje de error en toast
  - 401 → redirigir a login

#### Servicios (llamadas API)
```javascript
export const deleteUser = async (uid, token) => {
  const response = await fetch(`${API_BASE_URL}/api/v1/users/${uid}`, {
    method: 'DELETE',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  });

  if (response.status === 204) {
    return { success: true };
  }

  const errorData = await response.json();
  throw new Error(errorData.error || 'Error al eliminar usuario');
};
```

#### Componente: ConfirmDeleteModal (actualización si existe)
```javascript
// Debe mostrar:
- Nombre del usuario
- "Esta acción es permanente e irrecuperable"
- Botón "Eliminar" (rojo) → llama a deleteUser(uid)
- Botón "Cancelar"
// Al completar:
- Cierra modal
- Refresca tabla de usuarios
- Muestra toast "Usuario eliminado"
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

### Dependencias y Orden de Ejecución
1. **AuditService** debe estar listo (logging de USER_DELETE action)
2. **UserService.deleteUser()** llama a auditService antes de delete
3. **UserController** mapear DELETE /api/v1/users/{uid}

### Patrón a Usar
```java
@Transactional
public void deleteUser(UUID uid) {
    // 1. Validar token, role, ecommerce_id
    // 2. Fetch user (throw 404 si no existe)
    // 3. Self-delete check (throw 400)
    // 4. Authorization check (throw 403)
    // 5. Log en audit_log (NEW)
    // 6. DELETE físico (CAMBIO: de setIsActive(false) a delete(user))
    // 7. Log info
}
```

### Schema BD a Verificar
- `audit_log.user_id` debe tener `ON DELETE SET NULL` ✅
- Otras FKs a app_user deben tener `ON DELETE RESTRICT` (para detectar violaciones)

### Cambios en UserService
**ANTES (línea 260-283):**
```java
user.setIsActive(false);
userRepository.save(user);
```

**DESPUÉS:**
```java
// Registrar en audit_log ANTES de eliminar
auditService.logUserDeletion(user, currentUserUid);
// Hard delete
userRepository.delete(user);
```

### Tests a Ejecutar Antes de Commit
```bash
mvn clean test -Dtest="UserServiceTest#*delete*" -Dtest="UserControllerTest#*delete*"
mvn clean test -Dtest="*AuditTest*delete*"
```

### Frontend: Validar
- Modal de confirmación muestra "Esta acción es permanente"
- 204 → table refresh + toast
- 400/403/404 → error toast
- 401 → redirect to login

---

## Aprobación y Estado

**Status Actual:** `DRAFT`

**Cambios Requeridos (si aplica):**
- [ ] Validar que ON DELETE SET NULL está en audit_log.user_id
- [ ] Confirmar que no hay otras FKs a app_user sin validar
- [ ] Revisar si isActive se usa en otros lugares (queries, filtros)

**Requiere aprobación de:**
1. Product Owner (criterios Gherkin)
2. Arquitecto/Lead Backend (impacto auditoría, cascada)
3. QA Lead (estrategia de testing)

Cambiar a `status: APPROVED` cuando sea aprobada por todos.
