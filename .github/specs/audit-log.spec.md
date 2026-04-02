---
id: SPEC-013
status: DRAFT
feature: audit-log
created: 2026-03-30
updated: 2026-03-30
author: spec-generator
version: "1.0"
related-specs: []
---

# Spec: Trazabilidad de Cambios de Reglas (Auditoría)

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de iniciar implementación.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPRECATED

---

## 1. REQUERIMIENTOS

### Descripción
Sistema de auditoría que registra todas las modificaciones de reglas de descuento en el Admin Service, permitiendo a los superadministradores consultar un historial cronológico completo con filtros por ecommerce, tipo de regla, usuario y rango de fechas.

### Requerimiento de Negocio
Como super admin, quiero ver el registro de los cambios de las reglas de descuento de todos los ecommerce, para identificar que usuarios realizaron modificaciones y en que momentos.

### Historias de Usuario

#### HU-13.1: Registrar cambios de reglas en auditoría

```
Como:        Sistema (Admin Service)
Quiero:      Capturar automáticamente todos los cambios (CREATE, UPDATE, DELETE) 
             en reglas de descuento, producto y fidelidad
Para:        Mantener un registro auditable de todas las modificaciones

Prioridad:   Alta
Estimación:  M
Dependencias: Implementación previa de endpoints de reglas (Descuento, Producto, Fidelidad)
Capa:        Backend
```

#### Criterios de Aceptación — HU-13.1

**Happy Path**
```gherkin
CRITERIO-13.1.1: Registrar CREATE de regla de descuento
  Dado que:  Un administrador crea una nueva regla de descuento temporal
  Cuando:    La regla se guarda exitosamente en BD
  Entonces:  El sistema genera automáticamente un AuditLog con:
             - action: "CREATE"
             - rule_type: "DISCOUNT"
             - old_value: null
             - new_value: <valores completos de la regla>
             - timestamp: fecha/hora actual UTC
             - user_id: identificador del administrador autenticado
             - ip_address: IP del cliente
             Y el log se persiste de forma síncrona en tabla audit_logs
```

```gherkin
CRITERIO-13.1.2: Registrar UPDATE de regla de producto
  Dado que:  Un administrador modifica una regla de producto existente
  Cuando:    La regla se actualiza exitosamente en BD
  Entonces:  El sistema genera un AuditLog con:
             - action: "UPDATE"
             - rule_type: "PRODUCT"
             - old_value: <estado anterior completo>
             - new_value: <estado nuevo completo>
             - rule_id: identificador de la regla modificada
             Y el log se persiste de forma síncrona
```

```gherkin
CRITERIO-13.1.3: Registrar DELETE de regla de fidelidad
  Dado que:  Un administrador elimina una regla de fidelidad
  Cuando:    La regla se elimina exitosamente de BD
  Entonces:  El sistema genera un AuditLog con:
             - action: "DELETE"
             - rule_type: "FIDELITY"
             - old_value: <estado completo antes de eliminar>
             - new_value: null
             Y el log se persiste de forma síncrona
```

**Error Path** — No aplica. Los eventos de auditoría no generan errores; se registran siempre.

#### HU-13.2: Consultar historial de auditoría con filtros

```
Como:        Super Admin
Quiero:      Consultar todos los cambios de reglas de cualquier ecommerce 
             con filtros por tipo de regla, usuario, fecha y ecommerce
Para:        Verificar quién cambió qué, cuándo y qué valores modificó

Prioridad:   Alta
Estimación:  M
Dependencias: HU-13.1 (auditoría registrada)
Capa:        Backend + Frontend
```

#### Criterios de Aceptación — HU-13.2

**Happy Path**
```gherkin
CRITERIO-13.2.1: Listar historial de auditoría sin filtros
  Dado que:  Soy un Super Admin autenticado
  Cuando:    Realizo GET /api/v1/audit-logs
  Y:         Envío header Authorization: Bearer <JWT con rol SUPER_ADMIN>
  Entonces:  El sistema retorna 200 OK con:
             - Array de AuditLog (máximo 100 por página, ordenados DESC por timestamp)
             - Cada item incluye: id, timestamp, user_id, ecommerce_id, rule_type, 
               rule_id, action, old_value, new_value, ip_address, user_email (si existe)
             - Paginación: page, size, totalElements, totalPages
```

```gherkin
CRITERIO-13.2.2: Filtrar por tipo de regla
  Dado que:  Existen AuditLogs de múltiples tipos (DISCOUNT, PRODUCT, FIDELITY)
  Cuando:    Realizo GET /api/v1/audit-logs?ruleType=DISCOUNT
  Entonces:  El sistema retorna 200 OK solo con AuditLogs cuyo rule_type sea DISCOUNT
```

```gherkin
CRITERIO-13.2.3: Filtrar por ecommerce
  Dado que:  Existen AuditLogs de múltiples ecommerce
  Cuando:    Realizo GET /api/v1/audit-logs?ecommerceId=<uid>
  Entonces:  El sistema retorna 200 OK solo con AuditLogs del ecommerce especificado
```

```gherkin
CRITERIO-13.2.4: Filtrar por rango de fechas
  Dado que:  Existen AuditLogs en diferentes fechas
  Cuando:    Realizo GET /api/v1/audit-logs?startDate=2026-01-01T00:00:00Z&endDate=2026-03-31T23:59:59Z
  Entonces:  El sistema retorna 200 OK solo con AuditLogs dentro del rango
             (timestamps se comparan como > startDate Y < endDate)
```

```gherkin
CRITERIO-13.2.5: Combinar múltiples filtros
  Dado que:  Existen AuditLogs de múltiples ecommerce, tipos y fechas
  Cuando:    Realizo GET /api/v1/audit-logs?ecommerceId=<uid>&ruleType=DISCOUNT&startDate=<date1>&endDate=<date2>
  Entonces:  El sistema retorna 200 OK con AuditLogs que cumplen TODOS los criterios
```

**Error Path**
```gherkin
CRITERIO-13.2.6: Acceso sin autenticación
  Dado que:  No envío header Authorization
  Cuando:    Realizo GET /api/v1/audit-logs
  Entonces:  El sistema retorna 401 UNAUTHORIZED con mensaje:
             { "error": "Missing or invalid Authorization header" }
```

```gherkin
CRITERIO-13.2.7: Acceso sin rol SUPER_ADMIN
  Dado que:  Estoy autenticado con rol ADMIN (no SUPER_ADMIN)
  Cuando:    Realizo GET /api/v1/audit-logs
  Entonces:  El sistema retorna 403 FORBIDDEN con mensaje:
             { "error": "Only SUPER_ADMIN can access audit logs" }
```

```gherkin
CRITERIO-13.2.8: Filtro con valor inválido
  Dado que:  Envío un ruleType no soportado ( ej. "INVALID")
  Cuando:    Realizo GET /api/v1/audit-logs?ruleType=INVALID
  Entonces:  El sistema retorna 400 BAD REQUEST con mensaje:
             { "error": "ruleType must be one of: DISCOUNT, PRODUCT, FIDELITY" }
```

```gherkin
CRITERIO-13.2.9: Rango de fechas inválido (startDate > endDate)
  Dado que:  startDate es mayor que endDate
  Cuando:    Realizo GET /api/v1/audit-logs?startDate=2026-03-31&endDate=2026-01-01
  Entonces:  El sistema retorna 400 BAD REQUEST con mensaje:
             { "error": "startDate must be before endDate" }
```

#### HU-13.3: Mostrar panel de auditoría con tabla y filtros

```
Como:        Super Admin (UI)
Quiero:      Ver una tabla filtrable y paginable de cambios de reglas 
             con opciones de exportación
Para:        Analizar el historial de cambios de forma visual e intuitiva

Prioridad:   Media
Estimación:  L
Dependencias: HU-13.2 (endpoint GET de auditoría)
Capa:        Frontend
```

#### Criterios de Aceptación — HU-13.3

**Happy Path**
```gherkin
CRITERIO-13.3.1: Renderizar tabla de auditoría con datos
  Dado que:  Soy un Super Admin en la página /audit-logs
  Cuando:    La página carga y obtiene datos del backend
  Entonces:  Se renderiza una tabla con columnas:
             - Timestamp (formato: DD/MM/YYYY HH:mm:ss UTC)
             - Usuario (email o nombre)
             - Ecommerce
             - Tipo de Regla (DISCOUNT, PRODUCT, FIDELITY)
             - Acción (CREATE, UPDATE, DELETE)
             - Detalles (enlace o modal para expandir old_value/new_value)
             - IP Address
             Y se muestran máximo 25 filas por página con paginación
```

```gherkin
CRITERIO-13.3.2: Aplicar filtro por tipo de regla en UI
  Dado que:  Veo la tabla de auditoría renderizada
  Cuando:    Selecciono un tipo de regla en el dropdown (ej. DISCOUNT)
  Entonces:  La tabla se filtra de inmediato (con loading spinner) 
             y muestra solo AuditLogs de ese tipo
```

```gherkin
CRITERIO-13.3.3: Aplicar filtro por ecommerce en UI
  Dado que:  Veo dropdown de ecommerces
  Cuando:    Selecciono un ecommerce específico
  Entonces:  La tabla se filtra y muestra solo AuditLogs de ese ecommerce
```

```gherkin
CRITERIO-13.3.4: Aplicar filtro por rango de fechas
  Dado que:  Veo date pickers de "Desde" y "Hasta"
  Cuando:    Selecciono rango (ej. 01/01/2026 - 31/03/2026)
  Entonces:  La tabla se filtra y muestra solo AuditLogs dentro del rango
```

```gherkin
CRITERIO-13.3.5: Exportar como CSV
  Dado que:  Veo tabla renderizada con datos filtrados
  Cuando:    Hago click en botón "Exportar a CSV"
  Entonces:  Se descarga un archivo audit_logs_<timestamp>.csv con:
             - Headers: Timestamp, Usuario, Ecommerce, Tipo Regla, Acción, Valor Anterior, Valor Nuevo, IP
             - Todas las filas que cumplen los filtros actuales
             - Valores JSON complejos se formatean como strings legibles
```

**Error Path**
```gherkin
CRITERIO-13.3.6: Error al cargar datos del backend
  Dado que:  El backend retorna 500 Internal Server Error
  Cuando:    La página intenta recargar la tabla
  Entonces:  Se muestra mensaje de error: "Error al cargar auditoría. Intente más tarde."
             Y un botón "Reintentar"
```

### Reglas de Negocio
1. **Captura automática:** Cada CREATE, UPDATE, DELETE de regla genera un AuditLog automáticamente (síncrono).
2. **Solo SUPER_ADMIN:** Solo usuarios con rol SUPER_ADMIN pueden consultar auditoría.
3. **Retención indefinida:** Los AuditLogs se mantienen indefinidamente (sin purga automática).
4. **Timestamp UTC:** Todos los timestamps se almacenan y retornan en UTC ISO 8601.
5. **IP dinámima:** Se captura la IP del cliente de cada request (header `X-Forwarded-For` o `RemoteAddr`).
6. **Validación de filtros:** startDate debe ser < endDate, y ruleType debe estar en [DISCOUNT, PRODUCT, FIDELITY].

---

## 2. DISEÑO

### Modelos de Datos

#### Entidades afectadas
| Entidad | Almacén | Cambios | Descripción |
|---------|---------|---------|-------------|
| `AuditLogEntity` | tabla `audit_log` (nueva) | nueva | Log de cambios de reglas (Management) |

#### Campos del modelo `AuditLogEntity` (normalizado)
| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `id` | UUID | sí | auto-generado (gen_random_uuid()) | Identificador único del log |
| `user_id` | UUID | no | FK a user.id (SET NULL) | Usuario que realizó el cambio |
| `action` | VARCHAR(20) | sí | CREATE, UPDATE, DELETE | Tipo de acción realizada |
| `target_entity` | VARCHAR(50) | sí | RULE, CONFIG, USER, PRODUCT, FIDELITY | Entidad afectada |
| `change_data` | JSONB | no | – | Datos del cambio: {"old": {...}, "new": {...}} |
| `created_at` | TIMESTAMP WITH TIME ZONE | sí | auto-generado | Timestamp creación del log en UTC |

#### Índices / Constraints
```sql
-- PRIMARY KEY
PRIMARY KEY (id)

-- FK (nullable, SET NULL on delete)
FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE SET NULL

-- Índices para búsquedas frecuentes
CREATE INDEX idx_audit_log_user ON audit_log(user_id);
CREATE INDEX idx_audit_log_action ON audit_log(action);
CREATE INDEX idx_audit_log_target ON audit_log(target_entity);
CREATE INDEX idx_audit_log_created ON audit_log(created_at DESC);

-- CHECK constraints
CHECK (action IN ('CREATE', 'UPDATE', 'DELETE'))
CHECK (target_entity IN ('RULE', 'CONFIG', 'USER', 'PRODUCT', 'FIDELITY', 'ECOMMERCE'))

-- Índice GIN para búsquedas en JSONB (opcional)
CREATE INDEX idx_audit_log_change_data_gin ON audit_log USING GIN (change_data);
```

#### Migration (Flyway)
```sql
-- V1__Create_audit_log_table.sql
-- Auditoría de cambios de configuración de reglas (Management)
CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES user(id) ON DELETE SET NULL,
    action VARCHAR(20) NOT NULL CHECK (action IN ('CREATE', 'UPDATE', 'DELETE')),
    target_entity VARCHAR(50) NOT NULL,
    change_data JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT ck_audit_action CHECK (action IN ('CREATE', 'UPDATE', 'DELETE')),
    CONSTRAINT ck_audit_target CHECK (target_entity IN ('RULE', 'CONFIG', 'USER', 'PRODUCT', 'FIDELITY', 'ECOMMERCE'))
);
    rule_id UUID NOT NULL,
    action VARCHAR(10) NOT NULL CHECK (action IN ('CREATE', 'UPDATE', 'DELETE')),
    old_value JSONB,
    new_value JSONB,
    ip_address VARCHAR(45) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para optimizar queries frecuentes
CREATE INDEX idx_config_audit_logs_ecommerce_id ON config_audit_logs(ecommerce_id);
CREATE INDEX idx_config_audit_logs_timestamp ON config_audit_logs(timestamp DESC);
CREATE INDEX idx_config_audit_logs_ecommerce_type ON config_audit_logs(ecommerce_id, rule_type);
CREATE INDEX idx_config_audit_logs_type_timestamp ON config_audit_logs(rule_type, timestamp DESC);

-- Índice GIN para búsquedas eficientes en JSON (opcional pero útil para queries avanzadas)
CREATE INDEX idx_config_audit_logs_jsonb_new_value ON config_audit_logs USING GIN (new_value);
```

### API Endpoints

#### POST /api/v1/audit-logs (Interno — no expuesto a clientes)
- **Descripción**: Registra un evento de auditoría (llamado internamente por AOP @Auditable en servicios de reglas)
- **Auth requerida**: JWT (cualquier admin, capturado automáticamente del contexto)
- **Request Body** (típicamente generado por el Aspecto, no manualmente):
  ```json
  {
    "ecommerceId": "uuid",
    "ruleType": "DISCOUNT|PRODUCT|FIDELITY",
    "ruleId": "uuid",
    "action": "CREATE|UPDATE|DELETE",
    "oldValue": { /* object JSON completo del estado anterior */ },
    "newValue": { /* object JSON completo del estado nuevo */ }
  }
  ```
- **Response 201**:
  ```json
  {
    "id": "uuid",
    "timestamp": "2026-03-30T14:30:45Z",
    "userId": "uuid",
    "ecommerceId": "uuid",
    "ruleType": "DISCOUNT",
    "ruleId": "uuid",
    "action": "CREATE",
    "oldValue": null,
    "newValue": { /* ... */ },
    "ipAddress": "192.168.1.1"
  }
  ```
- **Response 400**: Falta campo obligatorio o valor inválido
- **Response 401**: Token ausente o expirado
- **Response 403**: No es administrador

#### GET /api/v1/audit-logs
- **Descripción**: Lista el historial de auditoría de cambios de configuración con filtros y paginación (SOLO lectura para SUPER_ADMIN)
- **Auth requerida**: sí (solo SUPER_ADMIN)
- **Query Parameters**:
  ```
  ?page=0
  &size=25
  &ruleType=DISCOUNT|PRODUCT|FIDELITY  (opcional, filtro por tipo de regla)
  &ecommerceId=uuid                     (opcional, filtro por ecommerce)
  &userId=uuid                          (opcional, filtro por usuario que hizo el cambio)
  &startDate=2026-01-01T00:00:00Z       (opcional, ISO 8601 TIMESTAMPTZ)
  &endDate=2026-03-31T23:59:59Z         (opcional, ISO 8601 TIMESTAMPTZ)
  &sortBy=timestamp                     (opcional, timestamp|userId|ruleType, default: timestamp DESC)
  ```
- **Response 200**:
  ```json
  {
    "content": [
      {
        "id": "uuid",
        "timestamp": "2026-03-30T14:30:45Z",
        "userId": "uuid",
        "userEmail": "admin@example.com",
        "ecommerceId": "uuid",
        "ecommerceName": "Store A",
        "ruleType": "DISCOUNT",
        "ruleId": "uuid",
        "action": "UPDATE",
        "oldValue": { /* ... */ },
        "newValue": { /* ... */ },
        "ipAddress": "192.168.1.1"
      },
      /* ... más items ... */
    ],
    "page": 0,
    "size": 25,
    "totalElements": 150,
    "totalPages": 6
  }
  ```
- **Response 400**: Parámetro inválido (ej. ruleType=INVALID, startDate > endDate)
- **Response 401**: Token ausente o expirado
- **Response 403**: No es SUPER_ADMIN

#### GET /api/v1/audit-logs/{id}
- **Descripción**: Obtiene detalles completos de un AuditLog específico
- **Auth requerida**: sí (solo SUPER_ADMIN)
- **Response 200**: AuditLog completo con old_value y new_value expandidos
- **Response 404**: No existe el AuditLog

### Diseño Frontend

#### Componentes nuevos
| Componente | Archivo | Props principales | Descripción |
|------------|---------|------------------|-------------|
| `AuditLogsPage` | `pages/AuditLogsPage.jsx` | – | Página completa con tabla y filtros |
| `AuditLogsTable` | `components/AuditLogsTable.jsx` | `logs, loading, page, totalPages, onPageChange` | Tabla de logs paginada |
| `AuditLogDetailModal` | `components/AuditLogDetailModal.jsx` | `log, isOpen, onClose` | Modal de detalles con diff visual (rojo=borrado, verde=añadido) usando jsondiffpatch |
| `AuditFilterBar` | `components/AuditFilterBar.jsx` | `onFilter` | Barra de filtros |
| `ExportButton` | `components/ExportButton.jsx` | `logs, filters` | Botón para exportar CSV |

#### Páginas nuevas
| Página | Archivo | Ruta | Protegida | Roles requeridos |
|--------|---------|------|-----------|-----------------|
| `AuditLogsPage` | `pages/AuditLogsPage.jsx` | `/audit-logs` | sí | SUPER_ADMIN |

#### Hooks y State
| Hook | Archivo | Retorna | Descripción |
|------|---------|---------|-------------|
| `useAuditLogs` | `hooks/useAuditLogs.js` | `{ logs, loading, error, page, totalPages, filters, setFilters, goToPage, export }` | CRUD + paginación de logs |
| `useAuditFilters` | `hooks/useAuditFilters.js` | `{ filters, setFilter, resetFilters }` | Gestión de estado de filtros |

#### Services (llamadas API)
| Función | Archivo | Endpoint | Descripción |
|---------|---------|---------|-------------|
| `getAuditLogs(filters, page, size, token)` | `services/auditLogService.js` | `GET /api/v1/audit-logs?...` | Lista con filtros |
| `getAuditLogDetail(id, token)` | `services/auditLogService.js` | `GET /api/v1/audit-logs/{id}` | Obtiene un log específico |
| `exportAuditLogsCSV(logs)` | `services/auditLogService.js` | – | Genera y descarga CSV local |

#### Rutas nuevas
- Registrar `/audit-logs` en el sistema de rutas con guard `SUPER_ADMIN`

### Arquitectura y Dependencias
- **Paquetes nuevos:** Ninguno (usamos stacks existentes: Spring Data JPA, PostgreSQL, React)
- **Servicios externos:** Ninguno
- **Impacto en punto de entrada:**
  - Backend: Registrar `AuditLogRepository`, `AuditLogService`, `AuditLogController` en contexto Spring
  - Frontend: Registrar ruta `/audit-logs` en Router
- **Event Emitters:** Los servicios de reglas existentes (DiscountService, ProductRuleService, FidelityRuleService) deben emitir eventos de auditoría después de cada operación CRUD

### Notas de Implementación

#### Backend — Captura Automática vía AOP
1. **Anotación personalizada:** Crear `@Auditable(ruleType = RuleType.DISCOUNT)` que se aplica a métodos Service que modifican reglas.
2. **Aspecto `AuditAspect` — Diseño robusto (crítico):** Implementar un aspecto que:
   - Intercepte métodos anotados con `@Auditable` (POST, PUT, DELETE en servicios)
   - Capture automáticamente los argumentos de entrada (estado anterior si es UPDATE)
   - Capture el resultado del método (estado nuevo)
   - **Error handling de extracción:** Envolver en try-catch separado la obtención de `user_id` e `ip_address`
     - Si obtener `user_id` falla → loguear WARN, usar "UNKNOWN" como fallback, **continuar**
     - Si obtener `ip_address` falla → loguear WARN, usar "0.0.0.0" como fallback, **continuar**
   - **Error handling de persistencia:** Envolver `AuditLogService.createLog()` en try-catch
     - Si la auditoría falla (ej. BD no disponible) → loguear ERROR pero **NO interrumpir** la transacción principal
     - **Criterio:** Un administrador creando una regla urgente NUNCA puede ser bloqueado porque falló la auditoría
   - NO requiere que los controladores escriban `auditLogService.save(...)`
3. **Tabla `config_audit_logs`:** Usar JSONB en PostgreSQL (extremadamente eficiente) en lugar de TEXT o MongoDB.
4. **TIMESTAMPTZ:** Usar `TIMESTAMPTZ` en lugar de `TIMESTAMP` para garantizar consistencia UTC en toda la BD.
5. **Extracción de IP:** En el Aspecto, obtener la IP desde `HttpServletRequest.getHeader("X-Forwarded-For")` o `RemoteAddr` como fallback, dentro del try-catch descrito en punto 2.
6. **Serialización JSON:** Los campos `old_value` y `new_value` se serializan como JSONB completos usando Jackson `ObjectMapper`.
7. **Usuario autenticado:** Obtener el `user_id` desde el contexto de seguridad Spring (`SecurityContextHolder.getContext().getAuthentication()`), dentro del try-catch descrito en punto 2.
8. **Índices JSONB:** El índice GIN en `new_value` permite búsquedas eficientes si en el futuro se necesita consultar por valores específicos dentro del JSON.

#### Frontend — Diff Visual
9. **Librería jsondiffpatch:** Instalar `npm install jsondiffpatch` para renderizar diffs en `AuditLogDetailModal`.
10. **Resaltado visual:** Mostrar en rojo (`-`) lo que se borró y en verde (`+`) lo que se añadió. Usar CSS o componentes de jsondiffpatch para mejor UX.
11. **Rendimiento:** Los índices en `timestamp` y `ecommerce_id` son críticos para queries rápidas en tablas con miles de logs.
12. **Paginación obligatoria:** El endpoint GET retorna máximo 100 items por página para evitar respuestas masivas.

#### Diferenciación de Auditorías
13. **SPEC-012 vs SPEC-013:**
    - **SPEC-012 (transaction_audit_logs):** Alto volumen, transaccional, viene del Engine vía RabbitMQ (lectura de eventos).
    - **SPEC-013 (config_audit_logs):** Bajo volumen, Management, generada en Admin Service (escritura de cambios configuracionales).
    - Usar nombres de tablas distintos para evitar confusiones.

---

## 3. LISTA DE TAREAS

> Checklist accionable para todos los agentes. Marcar cada ítem (`[x]`) al completarlo.
> El Orchestrator monitorea este checklist para determinar el progreso.

### Backend

#### Implementación
- [ ] Crear migration Flyway `V13__Create_config_audit_logs_table.sql` con tabla JSONB, índices e índice GIN
- [ ] Diseñar y documentar modelos:
  - [ ] `ConfigAuditLogEntity` (JPA Entity mapeando tabla config_audit_logs)
  - [ ] `CreateConfigAuditLogRequest` (Record/DTO de entrada)
  - [ ] `ConfigAuditLogResponse` (Record/DTO de salida con timestamp en ISO 8601)
- [ ] Crear anotación personalizada `@Auditable` (define ruleType automáticamente)
- [ ] Implementar `AuditAspect` (aspecto AOP que intercepta métodos anotados)
  - [ ] Capturar estado anterior (si es UPDATE)
  - [ ] Capturar estado posterior (resultado del método)
  - [ ] **Robustez crítica:** Envolver extracción de `user_id` e `ip_address` en try-catch
    - Loguear errores al obtener contexto (WARN level)
    - Usar valores fallback seguros (ej. "UNKNOWN" para user_id, "0.0.0.0" para IP)
    - NO interrumpir la transacción principal bajo ninguna circunstancia
  - [ ] Invocar `AuditLogService.createLog()` dentro de try-catch separado
    - Si la auditoría falla, loguear error pero permitir que continúe la operación principal
    - Justificación: Un administrador abriendo una regla urgente NO puede ser bloqueado porque falló la auditoría
  - [ ] Manejar excepciones heredadas (no lanzar, solo loguear)
- [ ] Implementar `ConfigAuditLogRepository` (Spring Data JPA) — custom queries para filtros
  - [ ] findByEcommerceIdAndRuleTypeAndTimestampBetween(...)
  - [ ] findByRuleIdAndAction(...)
- [ ] Implementar `ConfigAuditLogService` — lógica de creación, listado y filtrado
- [ ] Implementar `ConfigAuditLogController` — endpoints GET /api/v1/audit-logs y POST (interno)
- [ ] Configurar seguridad — validación de rol SUPER_ADMIN en `@PreAuthorize`
- [ ] Configurar extracción de IP desde request:
  - [ ] `HttpServletRequest.getHeader("X-Forwarded-For")` como preferencia
  - [ ] Fallback a `RemoteAddr`
- [ ] Aplicar anotación `@Auditable` en métodos de services (DiscountService, ProductRuleService, FidelityRuleService)

#### Tests Backend
- [ ] **AOP/Aspecto — Captura normal:**
  - [ ] `test_auditable_aspect_captures_create_automatically` — @Auditable en CREATE intercepta y crea log
  - [ ] `test_auditable_aspect_captures_update_with_old_and_new_values` — @Auditable en UPDATE captura antes/después
  - [ ] `test_auditable_aspect_captures_delete_with_old_value_only` — @Auditable en DELETE captura solo old_value
- [ ] **AOP/Aspecto — Robustez (error handling):**
  - [ ] `test_auditable_aspect_extracts_user_from_security_context` — obtiene user_id del contexto Spring (path feliz)
  - [ ] `test_auditable_aspect_extracts_ip_from_header_x_forwarded_for` — extrae IP de header (path feliz)
  - [ ] `test_auditable_aspect_uses_ip_fallback_when_x_forwarded_for_missing` — fallback a RemoteAddr si falta header
  - [ ] `test_auditable_aspect_uses_UNKNOWN_user_when_context_unavailable` — fallback a "UNKNOWN" si contexto falla, continúa la transacción
  - [ ] `test_auditable_aspect_uses_0_0_0_0_ip_when_extraction_fails` — fallback a "0.0.0.0" si extracción de IP falla, continúa
  - [ ] `test_auditable_aspect_tolerates_audit_log_creation_failure_without_breaking_transaction` — error en auditoría NO interrumpe, solo loguea WARN/ERROR
- [ ] **Servicio de Auditoría:**
  - [ ] `test_config_audit_log_service_create_success` — guardar log en BD
  - [ ] `test_config_audit_log_service_old_value_and_new_value_serialized_as_jsonb` — JSONB correctamente serializado
- [ ] **API GET:**
  - [ ] `test_get_config_audit_logs_no_filter_returns_200` — listar sin filtros
  - [ ] `test_get_config_audit_logs_filter_by_rule_type_returns_filtered` — filtro por tipo de regla
  - [ ] `test_get_config_audit_logs_filter_by_ecommerce_returns_filtered` — filtro por ecommerce
  - [ ] `test_get_config_audit_logs_filter_by_date_range_returns_filtered` — filtro por rango de fechas (TIMESTAMPTZ)
  - [ ] `test_get_config_audit_logs_invalid_rule_type_returns_400` — validación ruleType inválido
  - [ ] `test_get_config_audit_logs_start_date_after_end_date_returns_400` — validación fechas inválidas
  - [ ] `test_get_config_audit_logs_without_super_admin_role_returns_403` — autorización por rol
  - [ ] `test_get_config_audit_logs_without_token_returns_401` — autenticación requerida
  - [ ] `test_get_config_audit_log_detail_returns_200_with_jsonb_expanded` — obtener un log con JSON expandido
  - [ ] `test_get_config_audit_log_detail_not_found_returns_404` — log no existe
  - [ ] `test_config_audit_log_pagination_returns_page_info` — validar paginación (máx 100 items)

### Frontend

#### Implementación
- [ ] **Dependencias:** Instalar `npm install jsondiffpatch` para renderizar diffs visuales
- [ ] Crear `auditLogService.js` — funciones getAuditLogs, getAuditLogDetail, exportCSV
- [ ] Crear `useAuditLogs` hook — estado + llamadas API
- [ ] Crear `useAuditFilters` hook — gestión de filtros
- [ ] Crear `AuditFilterBar` componente — dropdowns de filtros + date pickers (rango: startDate < endDate)
- [ ] Crear `AuditLogsTable` componente — tabla paginada con datos (máx 25 filas/página)
- [ ] Crear `AuditLogDetailModal` componente — modal con diff visual usando jsondiffpatch (rojo=borrado, verde=añadido)
- [ ] Crear `ExportButton` componente — genera CSV desde logs con escaping y formateo de JSON
- [ ] Crear `AuditLogsPage` — layout completo integrando todos
- [ ] Registrar ruta `/audit-logs` en Router con guard SUPER_ADMIN
- [ ] Agregar enlace a "Auditoría" en navbar/sidebar (solo visible para SUPER_ADMIN)

#### Tests Frontend
- [ ] `AuditLogsPage renders without crashing`
- [ ] `AuditLogsTable displays logs correctly with all columns (Timestamp, Usuario, Ecommerce, etc.)`
- [ ] `AuditLogsTable handles pagination click and refetches data`
- [ ] `AuditFilterBar filters by rule type and triggers API call`
- [ ] `AuditFilterBar filters by ecommerce and triggers API call`
- [ ] `AuditFilterBar validates date range (startDate < endDate, rejects invalid)`
- [ ] `AuditLogDetailModal renders old_value and new_value with jsondiffpatch diff visual`
- [ ] `AuditLogDetailModal shows deleted changes in red (-)` — powered by jsondiffpatch
- [ ] `AuditLogDetailModal shows added changes in green (+)` — powered by jsondiffpatch
- [ ] `ExportButton generates CSV with correct headers and proper escaping`
- [ ] `ExportButton includes all filtered rows in CSV and converts JSON to readable strings`
- [ ] `useAuditLogs loads logs on mount with default filters`
- [ ] `useAuditLogs handles API error gracefully with retry button`
- [ ] `useAuditFilters state updates trigger useAuditLogs refetch`

### QA
- [ ] **Clarificación de Contexto:**
  - [ ] Verificar que el equipo entiende la diferenciación: SPEC-012 (transactional_audit_logs, RabbitMQ, Engine) vs SPEC-013 (config_audit_logs, síncrono, Admin)
  - [ ] Validar que las tablas tienen nombres distintos (config_audit_logs) para evitar confusiones
- [ ] Ejecutar skill `/gherkin-case-generator` → generar test cases Gherkin desde CRITERIO-13.1.1 a 13.3.6
- [ ] Ejecutar skill `/risk-identifier` → clasificación ASD de riesgos (datos sensibles JSON, autorización SUPER_ADMIN, performance en queries largas)
- [ ] Ejecutar skill `/performance-analyzer` → definir SLAs para queries con >10k logs, load test con k6 (filtros complejos, paginación)
- [ ] Revisar cobertura de tests contra todos los criterios BDD
- [ ] Validar que filtros con valores nulos retornan datos correctos (ej. no filtro por userId si es null)
- [ ] Validar exportación CSV con caracteres especiales en JSON, comillas, saltos de línea
- [ ] Validar que TIMESTAMPTZ se persiste y se retorna correctamente en ISO 8601
- [ ] Actualizar estado spec: `status: IMPLEMENTED`
