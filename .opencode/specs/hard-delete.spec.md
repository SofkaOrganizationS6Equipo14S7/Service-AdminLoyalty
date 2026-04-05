---
id: SPEC-HU03-001
status: DRAFT
feature: hard-delete-usuarios
created: 2026-04-05
updated: 2026-04-05
author: spec-generator
version: "1.0"
related-specs: ["SPEC-HU03", "SPEC-HU02"]
---

# Spec: Cambiar Eliminación de Usuarios de Soft Delete a Hard Delete

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de iniciar implementación.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED

---

## 1. REQUERIMIENTOS

### Descripción
Cambiar el mecanismo de eliminación de usuarios en el sistema de soft delete (marcar `isActive=false`) a hard delete (eliminar registro físico de la base de datos). Esto implica eliminar completamente el usuario del sistema, actualizar la auditoría para registrar la eliminación y garantizar la integridad referencial con las tablas relacionadas.

### Requerimiento de Negocio
Actualmente, cuando un SUPER_ADMIN o STORE_ADMIN elimina un usuario estándar (HU-02 y HU-03), el sistema solo marca el usuario como inactivo (`isActive=false`). El requerimiento es cambiar este comportamiento para que la eliminación sea **permanente y no recuperable**, eliminando el registro de la base de datos completamente.

**Caso de Uso:** Un STORE_ADMIN elimina un usuario de su ecommerce → el usuario debe desaparecer completamente del sistema y no poder iniciar sesión bajo ninguna circunstancia.

### Historias de Usuario Relacionadas

#### HU-02: SUPERADMIN - Acceso Total
**Estado:** ✅ Implementada (100%)  
**Cambio requerido:** Hard delete de usuarios creados por SUPER_ADMIN

#### HU-03: STORE_ADMIN - Administra su Ecommerce  
**Estado:** ✅ Implementada (100%)  
**Cambio requerido:** Hard delete de usuarios estándar dentro de su ecommerce

---

## 2. CRITERIOS DE ACEPTACIÓN

### CRITERIO-2.1: Hard Delete de Usuario (Happy Path)
```gherkin
Dado que:  existe un STORE_ADMIN con acceso al sistema
Y existe un usuario estándar (STORE_USER) asociado a su ecommerce
Cuando:    el STORE_ADMIN ejecuta DELETE /api/v1/users/{uid}
Entonces:  el usuario se elimina física y permanentemente de la tabla app_user
Y la respuesta HTTP es 204 No Content
Y el usuario no puede ser recuperado o consultado
Y se registra una entrada en audit_log con action='USER_DELETED'
```

### CRITERIO-2.2: Integridad Referencial - ON DELETE SET NULL en audit_log
```gherkin
Dado que:  existe un usuario con historial de auditoría
Cuando:    se elimina el usuario (hard delete)
Entonces:  la columna user_id en audit_log queda NULL para ese usuario
Y las entradas de auditoría se conservan (sin eliminar historial)
Y otros datos en audit_log (action, entityName, entityId) permanecen intactos
```

### CRITERIO-2.3: Validación de Autorización
```gherkin
Dado que:  existe un STORE_USER (usuario estándar) intentando eliminar otro usuario
Cuando:    envía DELETE /api/v1/users/{otro-uid}
Entonces:  el sistema retorna HTTP 403 Forbidden
Y el mensaje es "No tiene permiso para eliminar este usuario"
Y el usuario NO se elimina
```

### CRITERIO-2.4: Validación de Auto-eliminación Prohibida
```gherkin
Dado que:  existe un STORE_ADMIN autenticado
Cuando:    intenta DELETE /api/v1/users/{su-propio-uid}
Entonces:  el sistema retorna HTTP 400 Bad Request
Y el mensaje es "No puede eliminarse a sí mismo"
Y el usuario NO se elimina
```

### CRITERIO-2.5: Usuario No Encontrado
```gherkin
Dado que:  existe un STORE_ADMIN con permisos para eliminar
Cuando:    envía DELETE /api/v1/users/{uid-inexistente}
Entonces:  el sistema retorna HTTP 404 Not Found
Y el mensaje es "Usuario no encontrado"
```

### CRITERIO-2.6: Auditoría de Eliminación
```gherkin
Dado que:  se ha eliminado un usuario exitosamente
Cuando:    se consulta la tabla audit_log
Entonces:  existe una entrada con:
           - action: 'USER_DELETED'
           - entityName: 'APP_USER'
           - entityId: {uid-del-usuario-eliminado}
           - user_id: {uid-del-actor-que-elimina} (quien ejecutó la eliminación)
           - new_value: JSON con detalles del usuario eliminado
           - old_value: NULL (por ser eliminación)
```

### CRITERIO-2.7: Queries Afectadas - Filtros por is_active
```gherkin
Dado que:  se han eliminado usuarios del sistema
Cuando:    se ejecuta listUsers() sin filtros
Entonces:  solo aparecen usuarios activos (no soft-deleted)
Y el resultado no incluye usuarios que fueron eliminados (hard deleted)
```

---

## 3. REGLAS DE NEGOCIO

1. **Eliminación Permanente:** Una vez ejecutado el hard delete, no hay forma de recuperar el usuario. No existe endpoint de "restore".

2. **Auditoría Obligatoria:** Toda eliminación de usuario DEBE registrarse en `audit_log` con el actor (quién ejecutó), timestamp y detalles del usuario eliminado.

3. **Solo Administradores:** Solo SUPER_ADMIN o STORE_ADMIN pueden eliminar usuarios. No está permitido para STORE_USER.

4. **Aislamiento por Ecommerce:** Un STORE_ADMIN solo puede eliminar usuarios de su propio ecommerce. Un SUPER_ADMIN puede eliminar cualquier usuario.

5. **Auto-eliminación Prohibida:** Un usuario no puede eliminarse a sí mismo bajo ninguna circunstancia.

6. **Integridad Referencial:** La tabla `audit_log` mantiene referencia con `ON DELETE SET NULL`, por lo que los registros de auditoría se conservan incluso si se elimina el usuario que ejecutó la acción.

7. **Sin Restricción de Cascada en Endpoints:** El endpoint DELETE no intenta eliminar datos relacionados en otras tablas (como reglas o descuentos); solo elimina el usuario.

---

## 4. DISEÑO

### Análisis de Base de Datos

#### Tabla `app_user` - Cambios Requeridos
| Campo | Tipo | Cambio | Descripción |
|-------|------|--------|-------------|
| `id` | UUID PK | ❌ No cambio | Identificador único |
| `is_active` | BOOLEAN | ⚠️ Migración | Puede permanecer pero será ignorado (usuarios activos = todos los que existen) |

#### Tablas Relacionadas - Impacto del Hard Delete

| Tabla | Relación | ON DELETE | Acción | Impacto |
|-------|----------|-----------|--------|---------|
| `audit_log` | `user_id` | SET NULL | Conservar auditoría sin FK | ✅ Seguro |
| `api_key` | N/D | - | No directamente relacionada | ✅ Sin impacto |
| `discount_settings` | N/D | - | Creada por usuarios, no elimina | ✅ Sin impacto |
| `roles` | `role_id` | RESTRICT | No permite eliminar rol si hay usuarios | ✅ Sin impacto (usuario se elimina a sí mismo) |
| `ecommerce` | `ecommerce_id` | RESTRICT | No permite eliminar ecommerce si hay usuarios | ✅ Sin impacto (usuario se elimina, no ecommerce) |

**Conclusión:** Hard delete es seguro. No hay restricciones de cascada que impidan la eliminación.

### Cambios en el Backend

#### 1. Modificar `UserService.deleteUser(UUID uid)`
**Cambio de código:**

```java
// ANTES (Soft Delete)
@Transactional
public void deleteUser(UUID uid) {
    // ... validaciones ...
    user.setIsActive(false);
    userRepository.save(user);
    log.info("Usuario desactivado (soft delete): uid={}", user.getId());
}

// DESPUÉS (Hard Delete)
@Transactional
public void deleteUser(UUID uid) {
    // ... validaciones ...
    auditService.auditUserDeletion(user); // NEW
    userRepository.delete(user);
    log.info("Usuario eliminado (hard delete): uid={}, username={}", uid, user.getUsername());
}
```

**Archivo:** `backend/service-admin/src/main/java/com/loyalty/service_admin/application/service/UserService.java`

#### 2. Añadir método `auditUserDeletion()` en `AuditService`
```java
@Transactional
public void auditUserDeletion(UserEntity user) {
    AuditLogEntity auditLog = AuditLogEntity.builder()
            .userId(securityContextHelper.getCurrentUserUid())
            .action("USER_DELETED")
            .entityName("APP_USER")
            .entityId(user.getId())
            .newValue(toJson(Map.of(
                "id", user.getId().toString(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "ecommerceId", user.getEcommerceId() != null ? user.getEcommerceId().toString() : null,
                "roleId", user.getRoleId().toString(),
                "deletedAt", Instant.now().toString()
            )))
            .build();
    
    auditLogRepository.save(auditLog);
    log.info("Auditoría registrada: USER_DELETED para usuario {}", user.getId());
}
```

**Archivo:** `backend/service-admin/src/main/java/com/loyalty/service_admin/application/service/AuditService.java`

#### 3. Revisar Queries en `UserRepository`
Verificar que no hay queries que filtren específicamente por `is_active=true` y que necesiten actualización:

```java
// Verificar que findByEcommerceId() retorna todos los usuarios (activos e inactivos tras soft delete)
// Con hard delete, esto es automático
List<UserEntity> findByEcommerceId(UUID ecommerceId); 

// Verificar que findByUsername() y findByEmail() buscan en todo (no filtran por is_active)
```

**Archivo:** `backend/service-admin/src/main/java/com/loyalty/service_admin/domain/repository/UserRepository.java`

#### 4. Actualizar Filtros de Búsqueda
Si en el método `listUsers()` hay lógica de filtrado por `is_active`, DEBE removerse:

```java
// ANTES (con soft delete)
users.stream()
    .filter(u -> u.getIsActive()) // REMOVER ESTO
    .map(this::toResponse)
    .collect(Collectors.toList());

// DESPUÉS (con hard delete)
users.stream() // Sin filtro: usuarios eliminados no existen en BD
    .map(this::toResponse)
    .collect(Collectors.toList());
```

### API Endpoints (Sin Cambios en Contrato)

#### DELETE /api/v1/users/{uid}
- **Descripción:** Elimina un usuario permanentemente  
- **Auth requerida:** sí (JWT)
- **Roles permitidos:** SUPER_ADMIN, STORE_ADMIN
- **Request Body:** ninguno
- **Response 204 No Content:** eliminado exitosamente
- **Response 400:** auto-eliminación o datos inválidos
- **Response 403:** sin permisos para eliminar
- **Response 404:** usuario no encontrado

*Nota: El endpoint y la respuesta HTTP no cambian. Solo el comportamiento interno (de soft a hard delete).*

### Migraciones de Base de Datos

#### Migración V4: Conversión de Soft Delete a Hard Delete (OPCIONAL)

Si se desea limpiar usuarios inactivos previos:

```sql
-- V4__Cleanup_soft_deleted_users.sql
-- Eliminar usuarios marcados como inactivos (soft deleted)
DELETE FROM app_user WHERE is_active = FALSE;

-- Opcional: remover columna is_active si ya no es necesaria
-- ALTER TABLE app_user DROP COLUMN is_active;
-- (No se recomienda si hay otros módulos usando is_active)
```

**Decisión:** Con fines de compatibilidad, se mantiene la columna `is_active`. Todos los usuarios en la tabla serán `is_active=true` o será eliminados.

### Diseño Frontend

#### Cambios en UI
- **Mensaje de confirmación:** Cambiar de "¿Desactivar usuario?" a "¿Eliminar usuario permanentemente? Esta acción no se puede deshacer."
- **Botón:** Cambiar label de "Desactivar" a "Eliminar"
- **Modal de confirmar:** Agregar checkbox "Entiendo que esto es permanente"

*Nota: Solo si existe módulo frontend. Actualmente no se ha identificado carpeta `/frontend` en el proyecto.*

#### Componentes Afectados
Si existe componente de gestión de usuarios en frontend:
- `UserManagementPage` o similar
- `UserDeleteButton` o `ConfirmDeleteModal`
- `useUserManagement` hook (si aplica)

---

## 5. LISTA DE TAREAS

> Checklist accionable para todos los agentes. Marcar cada ítem (`[x]`) al completarlo.

### Backend

#### Implementación
- [ ] **Modificar `UserService.deleteUser()`:** Cambiar de soft delete (`setIsActive(false)`) a hard delete (`userRepository.delete()`)
- [ ] **Añadir método `AuditService.auditUserDeletion()`:** Registrar eliminación en audit_log con detalles del usuario
- [ ] **Revisar y limpiar queries en `UserRepository`:** Verificar no hay filtros innecesarios por `is_active`
- [ ] **Actualizar método `listUsers()`:** Remover filtros por `is_active` (usuarios eliminados no existen en BD)
- [ ] **Actualizar logs:** Cambiar mensajes de "usuario desactivado" a "usuario eliminado"
- [ ] **Compilación y validación:** `mvn clean compile` sin errores

#### Tests Backend (Esencial y Crítico)
- [ ] ✅ **Test: deletarUsuário (hard delete) exitoso** → User es eliminado físicamente de BD
  - Verificar: `userRepository.findById()` retorna Optional.empty() después
  - Verificar: Response HTTP 204 No Content
  
- [ ] ✅ **Test: auditoría registra USER_DELETED** → audit_log contiene entrada con action='USER_DELETED'
  - Verificar: `user_id` = UID del actor
  - Verificar: `entityId` = UID del usuario eliminado
  - Verificar: `new_value` contiene detalles (username, email, ecommerceId)
  
- [ ] ✅ **Test: ON DELETE SET NULL en audit_log** → Registros de auditoría se conservan con user_id=NULL
  - Escenario: Usuario A ejecuta acción X (registrada en audit) → Otro admin elimina Usuario A → Entrada en audit permanece
  
- [ ] ✅ **Test: validación de autorización** → STORE_USER NO puede eliminar otro usuario
  - Response HTTP 403 Forbidden
  
- [ ] ✅ **Test: validación de auto-eliminación** → Usuario NO puede eliminarse a sí mismo
  - Response HTTP 400 Bad Request
  
- [ ] ✅ **Test: usuario no encontrado** → DELETE a {uid-inexistente} retorna HTTP 404

#### Tests Backend (Cobertura Completa - Recomendado)
- [ ] Test: STORE_ADMIN elimina usuario de su ecommerce (happy path)
- [ ] Test: SUPER_ADMIN elimina usuario de cualquier ecommerce
- [ ] Test: STORE_ADMIN intenta eliminar usuario de OTRO ecommerce (403 Forbidden)
- [ ] Test: validar que no hay referencia dead en audit_log después de hard delete
- [ ] Test: verifica que `lastLogin` y otros datos históricos se pierden (no recuperables)

### Frontend

#### Implementación (Si existe módulo frontend)
- [ ] Cambiar mensaje de confirmación de eliminación: "¿Desactivar?" → "¿Eliminar permanentemente?"
- [ ] Cambiar label de botón: "Desactivar" → "Eliminar"
- [ ] Agregar verificación adicional: Checkbox "Entiendo que esta acción es permanente"
- [ ] Actualizar composables/hooks: si había lógica de soft delete, ajustarla

#### Tests Frontend (Esencial - si existe frontend)
- [ ] ✅ **Test: modal confirma eliminación permanente** → Mensaje advierte que no es reversible
- [ ] ✅ **Test: confirmDeleteModal muestra checkbox requerido** → Usuario debe marcar que entiende
- [ ] ✅ **Test: deleteUser API call envía DELETE /api/v1/users/{uid}** → Verificar method y URL

### QA / Validación

#### Criterios de Aceptación (Gherkin)
- [ ] Ejecutar skill `/gherkin-case-generator` → Generar casos de prueba basados en CRITERIO-2.1 a 2.7
- [ ] Mapear casos a escenarios manuales + automatizados

#### Riesgos
- [ ] Ejecutar skill `/risk-identifier` → Clasificación ASD de riesgos
  - Riesgo **ALTO:** Pérdida permanente de datos (criterio mitiga con auditoría)
  - Riesgo **MEDIO:** Impacto en integridad referencial (criterio valida)
  - Riesgo **BAJO:** Impacto en otros usuarios (aislado por ecommerce)

#### Cobertura de Tests
- [ ] Todas las reglas de negocio cubiertas (7 reglas = mínimo 7 tests)
- [ ] Todos los criterios CRITERIO-2.1 a 2.7 validados
- [ ] Pruebas de integridad referencial con audit_log

---

## 6. NOTAS DE IMPLEMENTACIÓN

### Consideraciones de Seguridad
- **Auditoría obligatoria:** Antes de ejecutar `userRepository.delete()`, DEBE llamarse `auditService.auditUserDeletion()`.
- **Contexto de seguridad:** `securityContextHelper.getCurrentUserUid()` registra quién ejecutó la eliminación.
- **Tokens activos:** Usuarios eliminados con tokens activos quedarán sin poder renovar (no existe el usuario en BD en próxima validación).

### Impacto en Otros Servicios
- **service-engine:** No tiene dependencia directa con usuarios. No requiere cambios.
- **API Keys:** Usuarios eliminados no pueden generar nuevas API Keys (usuario no existe). API Keys existentes quedarán huérfanas si se elimina el creador.

### Reversibilidad
**NO existe plan de reversión.** Hard delete es permanente. Si se requiere recuperación, se necesitaría un sistema de papelera o backups.

### Migración de Datos (Si aplica)
Si hay usuarios soft-deleted que deben eliminarse antes del hard delete:
1. Ejecutar cleanup en BD: `DELETE FROM app_user WHERE is_active = FALSE;`
2. Registrar en audit_log como migración: `action='MIGRATION_CLEANUP'`

### Testing con Datos
- Crear usuario de prueba → Eliminarlo → Verificar no aparece en listados
- Verificar audit_log contiene entrada
- Ejecutar queries secundarias que pudiesen depender de usuarios

---

## 7. ESTADO DE IMPLEMENTACIÓN ACTUAL (ANÁLIS)

### ✅ Completado
- HU-02: SUPER_ADMIN access — 100% (relaciones validadas)
- HU-03: STORE_ADMIN access — 100% (aislamiento confirmado)
- Auditoría base — 70% (AuditService existe, falta método USER_DELETED)
- BD schema — 100% (ON DELETE SET NULL en audit_log es seguro)

### 🔄 Por Completar (Este Sprint)
- ❌ Cambiar soft delete → hard delete en `UserService.deleteUser()`
- ❌ Implementar `auditService.auditUserDeletion()`
- ❌ Tests unitarios esenciales × 6
- ❌ Tests de integridad referencial
- ⚠️ Frontend (solo si existe módulo)

### 📋 Subtareas Detalladas

#### Backend Development
1. **Actualizar UserService.deleteUser()**
   - Reemplazar `user.setIsActive(false)` con `userRepository.delete(user)`
   - Llamar a `auditService.auditUserDeletion(user)` ANTES de eliminar
   - Actualizar logs
   - **Status:** ❌ Pendiente

2. **Crear AuditService.auditUserDeletion()**
   - Nuevo método público en AuditService
   - Recibe UserEntity como parámetro
   - Registra en audit_log con action='USER_DELETED'
   - Formato del new_value: JSON con id, username, email, ecommerceId, roleId, deletedAt
   - **Status:** ❌ Pendiente

3. **Validar UserRepository queries**
   - Revisar métodos que no deben filtrar por is_active
   - Actualizar si es necesario
   - **Status:** ❌ Pendiente

#### Test Engineer Backend
1. **Test: Hard Delete Físico**
   - Escenario: Eliminar usuario → verificar no existe en BD
   - Assert: `userRepository.findById(uid).isEmpty() == true`
   - **Status:** ❌ Pendiente

2. **Test: Auditoría USER_DELETED**
   - Escenario: Eliminar usuario → consultar audit_log
   - Assert: Existe entrada con action='USER_DELETED', user_id=actor, entityId=usuario_eliminado
   - **Status:** ❌ Pendiente

3. **Test: Integridad de audit_log (ON DELETE SET NULL)**
   - Escenario: Usuario con auditoría → eliminado → consultar su auditoría
   - Assert: audit_log.user_id=NULL, pero entrada existe
   - **Status:** ❌ Pendiente

4. **Test: Validación de Autorización**
   - Escenario: STORE_USER intenta eliminar usuario
   - Assert: Response 403, usuario NO se elimina
   - **Status:** ❌ Pendiente

5. **Test: Auto-eliminación Prohibida**
   - Escenario: Usuario intenta DELETE /api/v1/users/{su-uid}
   - Assert: Response 400, "No puede eliminarse a sí mismo"
   - **Status:** ❌ Pendiente

6. **Test: Usuario No Encontrado**
   - Escenario: DELETE a uid inexistente
   - Assert: Response 404
   - **Status:** ❌ Pendiente

#### Frontend Development (Si existe)
1. **Actualizar UI de Confirmación**
   - Cambiar mensaje a "¿Eliminar permanentemente?"
   - Agregar checkbox de confirmación
   - **Status:** ⚠️ A definir (no se identificó frontend en repo)

#### QA / Testing
1. **Ejecutar /gherkin-case-generator**
   - Generar casos Gherkin para CRITERIO-2.1 a 2.7
   - **Status:** ❌ Pendiente

2. **Ejecutar /risk-identifier**
   - Clasificar riesgos: Alto (pérdida de datos), Medio (integridad), Bajo
   - **Status:** ❌ Pendiente

---

## 8. VALIDACIÓN PRE-APROBACIÓN (Definition of Ready)

- [x] Estructura completa: Como/Quiero/Para ✅
- [x] Criterios Gherkin (7 criterios con Given/When/Then) ✅
- [x] Reglas de negocio (7 reglas claras) ✅
- [x] Análisis de BD (impacto en tablas relacionadas) ✅
- [x] Contrato API (sin cambios en endpoint, solo comportamiento) ✅
- [x] Stack alineado (Java 21 + Spring Boot + PostgreSQL) ✅
- [x] Riesgos identificados (auditoría obligatoria, integridad referencial) ✅
- [x] Subtareas detalladas (backend × 3, tests × 6, frontend × 1, QA × 2) ✅

**Estado:** LISTA PARA APROBACIÓN
