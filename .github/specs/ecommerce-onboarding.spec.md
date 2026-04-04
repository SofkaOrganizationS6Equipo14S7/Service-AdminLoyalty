---
id: SPEC-001
status: APPROVED
feature: ecommerce-onboarding
created: 2026-03-29
updated: 2026-03-29
author: spec-generator
version: "1.0"
related-specs: []
---

# Spec: Registro y Gestión de Ecommerces (Onboarding)

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de iniciar implementación.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPRECATED

---

## 1. REQUERIMIENTOS

### Descripción
Esta funcionalidad permite a los Super Administradores registrar y gestionar ecommerces en la plataforma. Cada ecommerce funciona como un tenant aislado donde se pueden vincular usuarios y API Keys. Al desactivar un ecommerce, todos sus usuarios pierden acceso inmediato y sus API Keys dejan de validar transacciones.

### Requerimiento de Negocio (HU-13)
Como Super Admin, quiero registrar y gestionar los ecommerces en la plataforma, para habilitar un entorno aislado (tenant) donde cada cliente pueda operar de forma segura.

### Historias de Usuario

#### HU-13.1: Registro exitoso de un nuevo ecommerce

```
Como:        Super Admin autenticado
Quiero:      registrar un nuevo ecommerce con nombre y slug único
Para:        habilitar un tenant aislado para ese cliente

Prioridad:   Alta
Estimación:  M
Dependencias: Ninguna
Capa:        Backend
```

#### Criterios de Aceptación — HU-13.1

**Happy Path**
```gherkin
CRITERIO-1.1: Registro exitoso de ecommerce con datos válidos
  Dado que:  soy un Super Admin autenticado con token JWT válido
  Cuando:    realizo POST /api/v1/ecommerces con body:
             {
               "name": "Tienda Nike",
               "slug": "nike-store"
             }
  Entonces:  el sistema retorna 201 Created con:
             {
               "uid": "<uuid-generado>",
               "name": "Tienda Nike",
               "slug": "nike-store",
               "status": "ACTIVE",
               "created_at": "2026-03-29T10:30:00Z",
               "updated_at": "2026-03-29T10:30:00Z"
             }
  Y:         el ecommerce queda guardado en BD con estado ACTIVE
```

**Error Path**
```gherkin
CRITERIO-1.2: Rechazo por slug duplicado
  Dado que:  extiende un ecommerce con slug "nike-store" en el sistema
  Cuando:    intento crear otro ecommerce con slug: "nike-store"
  Entonces:  el sistema retorna 409 Conflict con mensaje:
             "El slug 'nike-store' ya está en uso. Eligé uno diferente."
  Y:         ningún registro nuevo se crea

CRITERIO-1.3: Rechazo por datos incompletos
  Dado que:  soy Super Admin autenticado
  Cuando:    POST /api/v1/ecommerces sin campo "name":
             {
               "slug": "tienda-test"
             }
  Entonces:  el sistema retorna 400 Bad Request con:
             "El campo 'name' es obligatorio."

CRITERIO-1.4: Rechazo sin autenticación
  Dado que:  no tengo token JWT o está expirado
  Cuando:    POST /api/v1/ecommerces
  Entonces:  el sistema retorna 401 Unauthorized

CRITERIO-1.5: Rechazo por rol insuficiente
  Dado que:  soy un Usuario normal (no Super Admin)
  Cuando:    intento POST /api/v1/ecommerces
  Entonces:  el sistema retorna 403 Forbidden
```

---

#### HU-13.2: Listar y obtener ecommerces

```
Como:        Super Admin
Quiero:      listar todos los ecommerces o consultar uno específico por uid
Para:        gestionar y auditar los tenants registrados

Prioridad:   Alta
Estimación:  S
Dependencias: HU-13.1
Capa:        Backend
```

#### Criterios de Aceptación — HU-13.2

**Happy Path**
```gherkin
CRITERIO-2.1: Listar todos los ecommerces
  Dado que:  soy Super Admin autenticado
  Cuando:    GET /api/v1/ecommerces
  Entonces:  retorna 200 OK con array de ecommerces:
             [
               {
                 "uid": "uuid-1",
                 "name": "Nike Store",
                 "slug": "nike-store",
                 "status": "ACTIVE",
                 "created_at": "...",
                 "updated_at": "..."
               },
               ...
             ]

CRITERIO-2.2: Obtener ecommerce por uid
  Dado que:  existe un ecommerce con uid "abc-123"
  Cuando:    GET /api/v1/ecommerces/abc-123
  Entonces:  retorna 200 OK con el ecommerce completo
```

**Error Path**
```gherkin
CRITERIO-2.3: Uid no encontrado
  Dado que:  intento acceder a un ecommerce inexistente
  Cuando:    GET /api/v1/ecommerces/uid-inexistente
  Entonces:  retorna 404 Not Found
             "El ecommerce con uid 'uid-inexistente' no existe."
```

---

#### HU-13.3: Actualizar estado de un ecommerce

```
Como:        Super Admin
Quiero:      cambiar el estado (ACTIVE ↔ INACTIVE) de un ecommerce
Para:        desactivar un tenant sin eliminar sus datos

Prioridad:   Alta
Estimación:  M
Dependencias: HU-13.1, HU-13.2
Capa:        Backend / Servicios de caché + eventos
```

#### Criterios de Aceptación — HU-13.3

**Happy Path**
```gherkin
CRITERIO-3.1: Desactivación exitosa de ecommerce
  Dado que:  existe un ecommerce activo con uid "abc-123"
  Cuando:    PUT /api/v1/ecommerces/abc-123 con:
             {
               "status": "INACTIVE"
             }
  Entonces:  retorna 200 OK con ecommerce actualizado:
             {
               "uid": "abc-123",
               "name": "Nike Store",
               "slug": "nike-store",
               "status": "INACTIVE",
               "updated_at": "2026-03-29T11:00:00Z"
             }
  Y:         todos los Usuarios (HU-2) del ecommerce pierden acceso inmediato
  Y:         todas las API Keys (HU-3) del ecommerce dejan de validar

CRITERIO-3.2: Reactivación de ecommerce
  Dado que:  existe un ecommerce INACTIVE
  Cuando:    PUT /api/v1/ecommerces/<uid> con status: "ACTIVE"
  Entonces:  retorna 200 OK
  Y:         Usuarios recuperan acceso (nuevos tokens)
  Y:         API Keys vuelven a validar
```

**Error Path**
```gherkin
CRITERIO-3.3: Status inválido
  Dado que:  intento actualizar un ecommerce
  Cuando:    PUT con status: "PENDIENTE" (valor inválido)
  Entonces:  retorna 400 Bad Request
             "El status debe ser 'ACTIVE' o 'INACTIVE'."

CRITERIO-3.4: No se pueden actualizar otros campos
  Dado que:  POST/PUT a un ecommerce
  Cuando:    intento cambiar "name" o "slug" directamente
  Entonces:  el sistema ignora esos cambios o retorna 400
             "Solo el campo 'status' puede actualizarse."
```

---

### Reglas de Negocio

1. **Slug Único**: El campo `slug` debe ser único a nivel de sistema y no puede contener espacios ni caracteres especiales (solo letras, números, guiones).
2. **UUID Generado Automáticamente**: Cada ecommerce recibe un UUIDv4 auto-generado al crearse; el usuario no lo proporciona.
3. **Estado Inicial**: Todo ecommerce nuevo nace con estado `ACTIVE` por defecto.
4. **Estados Permitidos**: Solo `ACTIVE` e `INACTIVE`; no hay borradura física (soft delete vía estado).
5. **Aislamiento de Tenant**: Cada ecommerce es un contexto independiente con sus propios usuarios y API Keys.
6. **Cascada de Desactivación**: Al pasar a `INACTIVE`, todos los usuarios del ecommerce pierden acceso inmediatamente y las API Keys dejan de validar (evento RabbitMQ al engine).
7. **Autorización**: Solo Super Admins (`role: SUPER_ADMIN`) pueden crear, actualizar o listar ecommerces.
8. **Validación de Integridad**: No se puede desactivar el único ecommerce activo si hay usuarios vinculados sin alternativa (regla de negocio flexible; revisar con PM).

---

## 2. DISEÑO

### Modelos de Datos

#### Entidades afectadas

| Entidad | Almacén | Cambios | Descripción |
|---------|---------|---------|-------------|
| `EcommerceEntity` | tabla `ecommerce` | **nueva** | Registro de ecommerce con id, name, slug, status |
| `UserEntity` | tabla `app_user` | **ya existe** `ecommerce_id` | Se valida FK a `ecommerce.id` |
| `ApiKeyEntity` | tabla `api_key` | **ya existe** `ecommerce_id` | Se valida FK a `ecommerce.id` |

#### Foreign Keys con tablas existentes

```sql
-- En tabla app_user (ya existe, asegurar constraints)
ALTER TABLE app_user 
  ADD CONSTRAINT fk_app_user_ecommerce_id 
  FOREIGN KEY (ecommerce_id) REFERENCES ecommerce(id) ON DELETE RESTRICT;

-- En tabla api_key (ya existe, asegurar constraints)
ALTER TABLE api_key
  ADD CONSTRAINT fk_api_key_ecommerce_id
  FOREIGN KEY (ecommerce_id) REFERENCES ecommerce(id) ON DELETE CASCADE;
```

#### Foreign Keys con tablas existentes

```sql
-- En tabla users (ya existe, asegurar constraints)
-- IMPORTANTE: ecommerce_id debe referenciar el UUID (uid) de ecommerces, NO un ID secuencial
ALTER TABLE users 
  ADD CONSTRAINT fk_users_ecommerce_id 
  FOREIGN KEY (ecommerce_id) REFERENCES ecommerces(uid) ON DELETE RESTRICT;

-- En tabla api_keys (ya existe, asegurar constraints)
-- IMPORTANTE: ecommerce_id debe referenciar el UUID (uid) de ecommerces, NO un ID secuencial
ALTER TABLE api_keys
  ADD CONSTRAINT fk_api_keys_ecommerce_id
  FOREIGN KEY (ecommerce_id) REFERENCES ecommerces(uid) ON DELETE RESTRICT;
```

**Nota crítica — UUID Consistency:**
- El mapeo `ecommerce_id` → `ecommerce.id` garantiza que todos los registros de usuarios y API Keys apunten al UUID del ecommerce padre, NO a un ID secuencial interno.
- Esto mantiene la consistencia arquitectónica de identificadores UUID en toda la capa de servicio y facilita auditoría y sincronización multi-tenant.

#### Campos del modelo `EcommerceEntity` (normalizado)

| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `id` | UUID | sí | auto-generado (gen_random_uuid()) | Identificador único |
| `name` | VARCHAR(255) | sí | min 3, max 255 chars | Nombre del ecommerce (ej. "Tienda Nike") |
| `slug` | VARCHAR(255) | sí | unique, regex `^[a-z0-9]([a-z0-9-]{0,252}[a-z0-9])?$` | Identificador amigable para URL (ej. "nike-store") |
| `status` | VARCHAR(20) | sí | `ACTIVE` \| `INACTIVE` | Estado del ecommerce |
| `created_at` | TIMESTAMP WITH TIME ZONE | sí | auto-generado | Timestamp creación |
| `updated_at` | TIMESTAMP WITH TIME ZONE | sí | auto-generado | Timestamp actualización |

#### Índices / Constraints
- **PRIMARY KEY**: `id` (UUID)
- **UNIQUE**: `slug`
- **INDEX**: `idx_ecommerce_status` (status)
- **INDEX**: `idx_ecommerce_slug` (slug)
- **CHECK**: `status IN ('ACTIVE', 'INACTIVE')`
- **CHECK**: `slug` regex `^[a-z0-9]([a-z0-9-]{0,252}[a-z0-9])?$`

---

### API Endpoints

#### POST /api/v1/ecommerces
- **Descripción**: Crea un nuevo ecommerce
- **Auth requerida**: sí (Super Admin)
- **Request Body**:
  ```json
  {
    "name": "Tienda Nike",
    "slug": "nike-store"
  }
  ```
- **Response 201 Created**:
  ```json
  {
    "uid": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Tienda Nike",
    "slug": "nike-store",
    "status": "ACTIVE",
    "created_at": "2026-03-29T10:30:00Z",
    "updated_at": "2026-03-29T10:30:00Z"
  }
  ```
- **Response 400**: Campo obligatorio faltante o formato inválido (ej. slug con espacios)
  ```json
  {
    "error": "El campo 'name' es obligatorio.",
    "timestamp": "2026-03-29T10:30:00Z"
  }
  ```
- **Response 401**: Token ausente o expirado
- **Response 403**: Rol insuficiente (no es Super Admin)
- **Response 409 Conflict**: Slug duplicado
  ```json
  {
    "error": "El slug 'nike-store' ya está en uso. Elige uno diferente.",
    "timestamp": "2026-03-29T10:30:00Z"
  }
  ```

#### GET /api/v1/ecommerces
- **Descripción**: Lista todos los ecommerces
- **Auth requerida**: sí (Super Admin)
- **Query Parameters** (opcionales):
  - `status=ACTIVE` — filtrar por estado
  - `page=0&size=20` — paginación (default: size=50)
- **Response 200**:
  ```json
  {
    "content": [
      {
        "uid": "550e8400-e29b-41d4-a716-446655440000",
        "name": "Tienda Nike",
        "slug": "nike-store",
        "status": "ACTIVE",
        "created_at": "2026-03-29T10:30:00Z",
        "updated_at": "2026-03-29T10:30:00Z"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "currentPage": 0,
    "size": 20
  }
  ```
- **Response 401**: Sin autenticación
- **Response 403**: Rol insuficiente

#### GET /api/v1/ecommerces/{uid}
- **Descripción**: Obtiene un ecommerce específico por uid
- **Auth requerida**: sí (Super Admin)
- **Path Parameters**: `uid` — UUID del ecommerce
- **Response 200**:
  ```json
  {
    "uid": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Tienda Nike",
    "slug": "nike-store",
    "status": "ACTIVE",
    "created_at": "2026-03-29T10:30:00Z",
    "updated_at": "2026-03-29T10:30:00Z"
  }
  ```
- **Response 404 Not Found**: Ecommerce no existe
  ```json
  {
    "error": "El ecommerce con uid 'uid-inexistente' no existe.",
    "timestamp": "2026-03-29T10:30:00Z"
  }
  ```
- **Response 401/403**: Sin autenticación o rol insuficiente

#### PUT /api/v1/ecommerces/{uid}
- **Descripción**: Actualiza el estado de un ecommerce
- **Auth requerida**: sí (Super Admin)
- **Path Parameters**: `uid` — UUID del ecommerce
- **Request Body**:
  ```json
  {
    "status": "INACTIVE"
  }
  ```
- **Response 200**:
  ```json
  {
    "uid": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Tienda Nike",
    "slug": "nike-store",
    "status": "INACTIVE",
    "created_at": "2026-03-29T10:30:00Z",
    "updated_at": "2026-03-29T11:00:00Z"
  }
  ```
- **Response 400**: Status inválido o intento de cambiar otros campos
  ```json
  {
    "error": "El status debe ser 'ACTIVE' o 'INACTIVE'.",
    "timestamp": "2026-03-29T11:00:00Z"
  }
  ```
- **Response 404**: Ecommerce no existe
- **Response 401/403**: Sin autenticación o rol insuficiente

#### Side Effects: Cascada en Desactivación

Cuando un ecommerce cambia a `INACTIVE`:
1. **RabbitMQ Fanout Exchange Event**: Emitir evento `EcommerceStatusChangedEvent` a un **Fanout Exchange** (`loyalty.events`) para que múltiples consumidores puedan reaccionar:
   ```json
   {
     "ecommerceId": "550e8400-e29b-41d4-a716-446655440000",
     "newStatus": "INACTIVE",
     "timestamp": "2026-03-29T11:00:00Z",
     "eventType": "ECOMMERCE_STATUS_CHANGED"
   }
   ```
   - **Consumidor 1 (service-admin)**: Invalida JWT activos de todos los usuarios del ecommerce en caché + sesiones activas
   - **Consumidor 2 (service-engine)**: Invalida la máscara de API Keys del ecommerce en caché Caffeine (sin validar nuevas transacciones)

2. **RabbitMQ Routing Details**:
   - **Exchange Type**: Fanout (asegura que ambos servicios reciban el evento)
   - **Queue Admin**: `loyalty.admin.ecommerce.events` → bindeado a exchange `loyalty.events`
   - **Queue Engine**: `loyalty.engine.ecommerce.events` → bindeado a exchange `loyalty.events`
   - **Durabilidad**: Ambas queues persistentes (durable=true), eventos no se pierden si un servicio está temporalmente caído

3. **Atomicidad**: La operación de update + publish ocurre dentro de una transacción DB + TX TX RabbitMQ (`@Transactional` + confirmación de publisher)

4. **Timeout y Retry**: Si RabbitMQ no confirma en < 5s, la transacción se revierte (no se cambia estado sin confirmación de entrega del evento)

---

### Diseño Frontend

#### Componentes nuevos

| Componente | Archivo | Props principales | Descripción |
|------------|---------|------------------|-------------|
| `EcommerceCard` | `components/EcommerceCard.jsx` | `item, onEdit, onToggleStatus` | Tarjeta de ecommerce con nombre, slug, status, botones editar/toggle |
| `EcommerceFormModal` | `components/EcommerceFormModal.jsx` | `isOpen, onSubmit, onClose, editingItem` | Modal para crear/editar ecommerce (solo status en edición) |

#### Páginas nuevas

| Página | Archivo | Ruta | Protegida | Rol requerido |
|--------|---------|------|-----------|---------------|
| `EcommercesPage` | `pages/EcommercesPage.jsx` | `/ecommerces` | sí | SUPER_ADMIN |

#### Hooks y State

| Hook | Archivo | Retorna | Descripción |
|------|---------|---------|-------------|
| `useEcommerces` | `hooks/useEcommerces.js` | `{ items, loading, error, create, getById, update, list }` | CRUD para ecommerces con manejo de errores |

#### Services (llamadas API)

| Función | Archivo | Endpoint | Método |
|---------|---------|---------|---------|
| `listEcommerces(token, { status, page, size })` | `services/ecommerceService.js` | `GET /api/v1/ecommerces` | GET |
| `getEcommerceById(uid, token)` | `services/ecommerceService.js` | `GET /api/v1/ecommerces/{uid}` | GET |
| `createEcommerce(data, token)` | `services/ecommerceService.js` | `POST /api/v1/ecommerces` | POST |
| `updateEcommerceStatus(uid, status, token)` | `services/ecommerceService.js` | `PUT /api/v1/ecommerces/{uid}` | PUT |

#### UI/UX Considerations
- **Listado**: Tabla con columnas uid, name, slug, status (badge ACTIVE/INACTIVE), acciones (editar estado, eliminar lógico)
- **Crear**: Modal con campos nombre, slug, validación en tiempo real (no espacios en slug)
- **Editar**: Modal solo para cambiar status (toggle ACTIVE ↔ INACTIVE), confirmación para desactivación
- **Mensajes**: Toast/Snackbar para confirmaciones y errores
- **Responsividad**: Tabla en desktop, accordion en móvil

---

### Arquitectura y Dependencias

#### Backend
- **Paquetes nuevos**: 
  - `com.loyalty.service_admin.domain.model.ecommerce` — `EcommerceEntity`, `EcommerceStatus` enum
  - `com.loyalty.service_admin.domain.repository` — `EcommerceRepository` interface
  - `com.loyalty.service_admin.application.ecommerce` — `EcommerceService`, DTOs
  - `com.loyalty.service_admin.presentation.ecommerce` — `EcommerceController`
  - `com.loyalty.service_admin.infrastructure.persistence.jpa` — `EcommerceJpaRepository` (implementación)
  - `com.loyalty.service_admin.infrastructure.event` — `EcommerceStatusChangedEvent`, `EcommerceEventPublisher`

- **Dependencias externas**: 
  - `spring-data-jpa` (ya disponible)
  - `spring-amqp` / `spring-rabbit` (para RabbitMQ, ya disponible)
  - `hibernate-validator` (para validaciones @Valid)

- **Servicios externos**:
  - **RabbitMQ**: Evento `ecommerce.status.changed` emitido a exchange `loyalty.events` (para que engine lo consuma y actualice caché)

- **Impacto en punto de entrada**:
  - Registrar repositorio JPA en `@EntityScan`
  - Registrar controller en escaneo de componentes (automático con `@SpringBootApplication`)
  - Agregar migration Flyway: `db/migration/V6__Create_ecommerces_table.sql`

#### Frontend
- **Paquetes nuevos**:
  - `services/ecommerceService.js` — funciones de API
  - `hooks/useEcommerces.js` — state management (context o custom hook)
  - `pages/EcommercesPage.jsx` — página principal
  - `components/EcommerceCard.jsx` — componente tarjeta
  - `components/EcommerceFormModal.jsx` — modal de formulario

- **Dependencias**:
  - `react` (ya disponible)
  - `axios` o `fetch` para HTTP (ya disponible)

- **Impacto en rutas**:
  - Agregar ruta `/ecommerces` en `Router` con protección de rol `SUPER_ADMIN`

---

### Notas de Implementación

1. **Migraciones Flyway**: Crear `V6__Create_ecommerces_table.sql` con DDL de tabla y constraints.
2. **Validación de Slug**: Usar annotation custom `@ValidSlug` o regex directo en entity, con error message claro.
3. **UUID Mapping Crítico**: El campo `ecommerce_id` en tablas `users` y `api_keys` DEBE apuntar al UUID (`uid`) de `ecommerces`, NO a un ID secuencial. Garantiza consistencia de identificadores en toda la arquitectura.
4. **Cascada RabbitMQ (Fanout)**: 
   - Usar Fanout Exchange (`loyalty.events`)
   - service-admin escucha en queue `loyalty.admin.ecommerce.events` → invalida JWT y sesiones
   - service-engine escucha en queue `loyalty.engine.ecommerce.events` → invalida API Keys en caché Caffeine
   - Ambas queues "durable=true" para resiliencia
5. **Atomicidad de Delete Cascade**: Operación de update status + publish RabbitMQ en transacción única. Si publish falla, revertir DB change.
6. **Soft Delete**: No usar DELETE físico; desactivación vía `status = 'INACTIVE'` es suficiente.
7. **Formula en JPA**: Campo `updated_at` con `@UpdateTimestamp` para actualización automática en cada cambio.
8. **Constructor inyección**: Seguir patrón Clean Architecture — servicios con constructor injection, sin `@Autowired`.
9. **DTOs en Request/Response**: Usar Java Records para claridad (ej. `EcommerceCreateRequest`, `EcommerceResponse`).
10. **Autorización**: Validar rol `SUPER_ADMIN` en controlador con `@PreAuthorize("hasRole('SUPER_ADMIN')")` o en servicio.
11. **Transacciones**: Operación de create + event publish en transacción única (`@Transactional`). Considerar `@Transactional(propagation=Propagation.REQUIRED)` para consistencia.
12. **Testing**: Mock de `EcommerceRepository`, `EcommerceEventPublisher` y `RabbitTemplate` en tests unitarios. Validar que eventos se publican con timestamp y ecommerceId correctos.
13. **Observabilidad**: Loguear cada cambio de estado con ecommerceId, timestamp, usuario admin que realizó el cambio. Útil para auditoría de desactivaciones.

---

## 3. LISTA DE TAREAS

> Checklist accionable para todos los agentes. Marcar cada ítem (`[x]`) al completarlo.
> El Orchestrator monitorea este checklist para determinar el progreso.

### Backend

#### Implementación
- [ ] Crear migración Flyway `V6__Create_ecommerces_table.sql` (DDL tabla + índices + constraints)
- [ ] Crear enumeración `EcommerceStatus` (ACTIVE, INACTIVE) en `domain.model.ecommerce`
- [ ] Crear entidad `EcommerceEntity` con campos uid, name, slug, status, created_at, updated_at
- [ ] Crear validator custom `@ValidSlug` para validación de formato slug
- [ ] Crear interface `EcommerceRepository` en `domain.repository`
- [ ] Crear repositorio JPA `EcommerceJpaRepository` en `infrastructure.persistence.jpa`
- [ ] Crear DTOs: `EcommerceCreateRequest`, `EcommerceUpdateStatusRequest`, `EcommerceResponse`
- [ ] Crear `EcommerceService` — métodos: create, list, getById, updateStatus + validaciones
- [ ] Crear event `EcommerceStatusChangedEvent` + publisher `EcommerceEventPublisher` para RabbitMQ
- [ ] Crear `EcommerceController` — endpoints POST, GET /list, GET /{uid}, PUT /{uid}
- [ ] Registrar rutas en router de aplicación
- [ ] Agregar @PreAuthorize("hasRole('SUPER_ADMIN')") a nivel de controlador

#### Tests Backend
- [ ] `test_ecommerce_service_create_success` — validar persistencia y UUID generado
- [ ] `test_ecommerce_service_create_duplicate_slug_raises_conflict` — validar uniqueness
- [ ] `test_ecommerce_service_create_invalid_slug_raises_error` — validar regex
- [ ] `test_ecommerce_service_get_by_id_not_found` — 404 handling
- [ ] `test_ecommerce_service_update_status_active_to_inactive` — cambio de estado
- [ ] `test_ecommerce_service_update_status_publishes_event` — RabbitMQ event
- [ ] `test_ecommerce_repo_find_by_slug` — queries JPA
- [ ] `test_ecommerce_repo_find_by_status` — queries JPA
- [ ] `test_ecommerce_controller_post_returns_201` — endpoint create
- [ ] `test_ecommerce_controller_post_returns_409_duplicate` — error handling
- [ ] `test_ecommerce_controller_post_returns_403_not_super_admin` — autorización
- [ ] `test_ecommerce_controller_get_list_returns_200` — listado
- [ ] `test_ecommerce_controller_get_by_id_returns_200` — detalle
- [ ] `test_ecommerce_controller_put_status_returns_200` — actualización
- [ ] `test_ecommerce_controller_put_status_returns_400_invalid_status` — validación

### Frontend

#### Implementación
- [ ] Crear `services/ecommerceService.js` — funciones listEcommerces, getEcommerceById, createEcommerce, updateEcommerceStatus
- [ ] Crear `hooks/useEcommerces.js` — state management (items, loading, error, CRUD actions)
- [ ] Crear componente `EcommerceCard.jsx` — display nombre, slug, status badge, botones de acción
- [ ] Crear componente `EcommerceFormModal.jsx` — forma modal para crear/editar (edición solo status)
- [ ] Crear página `EcommercesPage.jsx` — layout con tabla/lista, botón crear, búsqueda/filtrado por estado
- [ ] Registrar ruta `/ecommerces` en Router protegida con rol `SUPER_ADMIN`
- [ ] Agregar en header/navbar link a `/ecommerces` (visible solo para SUPER_ADMIN)
- [ ] Estilos (CSS/TailwindCSS) para card, modal, página

#### Tests Frontend
- [ ] `[EcommerceService] listEcommerces calls GET /api/v1/ecommerces`
- [ ] `[EcommerceService] createEcommerce calls POST with correct payload`
- [ ] `[useEcommerces] hook initializes with empty items`
- [ ] `[useEcommerces] hook loads items on mount`
- [ ] `[useEcommerces] hook handles create and adds item to list`
- [ ] `[useEcommerces] hook handles update status`
- [ ] `[useEcommerces] hook handles errors gracefully`
- [ ] `[EcommerceCard] renders name and slug correctly`
- [ ] `[EcommerceCard] displays status badge with correct color`
- [ ] `[EcommerceCard] calls onToggleStatus when button clicked`
- [ ] `[EcommerceFormModal] submits form with name and slug`
- [ ] `[EcommerceFormModal] edit mode only shows status field`
- [ ] `[EcommercesPage] renders list of ecommerces`
- [ ] `[EcommercesPage] shows create modal on button click`
- [ ] `[EcommercesPage] handles API errors with toast/alert`

### QA
- [ ] Ejecutar skill `/gherkin-case-generator` → criterios CRITERIO-1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 3.1, 3.2, 3.3, 3.4
- [ ] Ejecutar skill `/risk-identifier` → clasificación ASD de riesgos (multi-tenant, cascada de desactivación)
- [ ] Ejecutar skill `/performance-analyzer` → plan de tests de performance (carga, spike, soak)
- [ ] Ejecutar skill `/automation-flow-proposer` → propuesta de automatización (ROI de flujos)
- [ ] Revisar cobertura de tests unitarios contra criterios de aceptación
- [ ] Validar que todas las reglas de negocio están cubiertas (unicidad slug, cascada, autorización)
- [ ] Pruebas manuales: crear, listar, editar estado, validar cascada (usuarios pierden acceso)
- [ ] Performance: listar 1000+ ecommerces con paginación
- [ ] Seguridad: intentos de acceso sin rol SUPER_ADMIN, modificación de slug/name en PUT (rechazados)
- [ ] Validar RabbitMQ event llega al engine correctamente
- [ ] Actualizar estado spec: `status: IMPLEMENTED` (después de completar todo)

