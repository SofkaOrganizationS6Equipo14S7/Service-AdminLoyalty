---
id: SPEC-005
status: APPROVED
feature: api-keys
created: 2026-03-25
updated: 2026-03-26
author: spec-generator
version: "1.0"
related-specs: []
---

# Spec: Gestión y Validación de API Keys

> **Estado:** `APPROVED` — Implementación completada y validada.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPRECATED

---

## 1. REQUERIMIENTOS

### Descripción

El Administrador del sistema necesita crear y gestionar API Keys para cada ecommerce, asegurando que solo sistemas autorizados puedan acceder a los recursos de la plataforma. El Engine Service validará estas claves en tiempo de ejecución mediante una caché sincronizada desde el Admin Service vía RabbitMQ, garantizando seguridad perimetral de microsegundos sin impacto en base de datos.

### Requerimiento de Negocio

Como Super Admin, quiero gestionar y validar las API Keys de cada ecommerce, para asegurar que solo sistemas autorizados puedan acceder a sus recursos en la plataforma.

### Historias de Usuario

#### HU-03.1: Crear API Key para un ecommerce

```
Como:        Super Admin
Quiero:      crear una nueva clave de acceso (API Key) para un ecommerce registrado
Para:        permitir que ese ecommerce acceda de forma segura a los recursos del sistema

Prioridad:   Alta
Estimación:  M
Dependencias: Ecommerce debe estar registrado
Capa:        Backend + Engine Service sync
```

#### Criterios de Aceptación — HU-03.1

**Happy Path**
```gherkin
CRITERIO-3.1.1: Crear API Key exitosamente para ecommerce válido
  Dado que:      soy un Super Admin autenticado
  Y              existe un ecommerce registrado en la base de datos
  Cuando:        envío POST /api/v1/ecommerces/{ecommerceId}/api-keys sin body
  Entonces:      el sistema genera una nueva API Key en formato UUID v4
  Y              persiste en loyalty_admin.api_key
  Y              publica evento en loyalty.config.exchange
  Y              responde con HTTP 201 Created + payload con UID, key (parcial), created_at

CRITERIO-3.1.2: Verificar formato de key y hashing
  Dado que:      acabo de crear una API Key
  Cuando:        consulto el payload de respuesta
  Entonces:      la key mostrada en la response tiene formato ****XXXX (últimos 4 caracteres)
  Y              solo se persiste el hash SHA-256 en la BD (nunca el valor plano)
```

**Error Path**
```gherkin
CRITERIO-3.1.3: Rechazar creación sin autenticación
  Dado que:      no envío token de autenticación
  Cuando:        envío POST /api/v1/ecommerces/{ecommerceId}/api-keys
  Entonces:      responde con HTTP 401 Unauthorized

CRITERIO-3.1.4: Rechazar creación para ecommerce inexistente
  Dado que:      soy Super Admin autenticado
  Cuando:        envío POST /api/v1/ecommerces/invalid-id/api-keys
  Entonces:      responde con HTTP 404 Not Found + mensaje "Ecommerce no encontrado"
```

---

#### HU-03.2: Listar API Keys de un ecommerce

```
Como:        Super Admin
Quiero:      consultar todas las API Keys asociadas a un ecommerce
Para:        auditar qué sistemas tienen acceso y gestionar ciclo de vida de claves

Prioridad:   Alta
Estimación:  S
Dependencias: HU-03.1 (debe haber al menos una key creada)
Capa:        Backend
```

#### Criterios de Aceptación — HU-03.2

**Happy Path**
```gherkin
CRITERIO-3.2.1: Listar API Keys con masking de seguridad
  Dado que:      existen 3 API Keys registradas para un ecommerce
  Y              soy Super Admin autenticado
  Cuando:        envío GET /api/v1/ecommerces/{ecommerceId}/api-keys
  Entonces:      responde con HTTP 200 OK
  Y              lista las 3 API Keys en formato JSON array
  Y              cada key muestra solo ****XXXX, uid, created_at, updated_at
  Y              la key completa NO aparece en la respuesta

CRITERIO-3.2.2: Listar vacío si no hay keys
  Dado que:      un ecommerce NO tiene API Keys registradas
  Cuando:        envío GET /api/v1/ecommerces/{ecommerceId}/api-keys
  Entonces:      responde con HTTP 200 OK
  Y              retorna array vacío []
```

**Error Path**
```gherkin
CRITERIO-3.2.3: Rechazar listado sin autenticación
  Dado que:      no envío token
  Cuando:        envío GET /api/v1/ecommerces/{ecommerceId}/api-keys
  Entonces:      responde con HTTP 401 Unauthorized

CRITERIO-3.2.4: Rechazar listado para ecommerce inexistente
  Dado que:      soy Super Admin autenticado
  Cuando:        envío GET /api/v1/ecommerces/invalid-id/api-keys
  Entonces:      responde con HTTP 404 Not Found
```

---

#### HU-03.3: Validar API Key en Engine Service

```
Como:        Sistema externo con API Key válida
Quiero:      enviar requests al Engine Service usando mi API Key
Para:        acceder a operaciones de cálculo de descuentos sin mantener BD en el cliente

Prioridad:   Crítica
Estimación:  L (requiere implementación en Engine)
Dependencias: HU-03.1 (key debe estar sincronizada vía RabbitMQ)
Capa:        Engine Service (OncePerRequestFilter)
```

#### Criterios de Aceptación — HU-03.3

**Happy Path**
```gherkin
CRITERIO-3.3.1: Validar API Key válida en caché de Engine
  Dado que:      una API Key válida fue creada en Admin y sincronizada vía RabbitMQ a Engine
  Cuando:        envío GET /api/v1/engine/health con header Authorization: Bearer <valid-key>
  Entonces:      el Engine valida la key contra Caffeine Cache en < 1ms
  Y              permite la request (next filter)
  Y              NO consulta loyalty_engine DB

CRITERIO-3.3.2: Rechazar API Key inválida
  Dado que:      envío una key que NO existe en caché
  Cuando:        envío GET /api/v1/engine/health con header Authorization: Bearer invalid-key-xyz
  Entonces:      responde con HTTP 401 Unauthorized
  Y              mensaje: "API Key inválida o expirada"

CRITERIO-3.3.3: Rechazar request sin header Authorization
  Dado que:      no incluyo header Authorization
  Cuando:        envío GET /api/v1/engine/health
  Entonces:      responde con HTTP 401 Unauthorized
  Y              mensaje: "Header Authorization requerido"
```

**Edge Case**
```gherkin
CRITERIO-3.3.4: Validar key después del reinicio de Engine (cold start)
  Dado que:      Engine Service se reinicia
  Y              Caffeine Cache está vacía
  Cuando:        arranca (Spring @PostConstruct)
  Entonces:      carga todas las API Keys desde loyalty_engine.api_key a Caffeine
  Y              posteriormente valida requests contra la caché cargada
```

---

#### HU-03.4: Eliminar API Key (Revocar acceso)

```
Como:        Super Admin
Quiero:      eliminar (revocar) una API Key de un ecommerce
Para:        bloquear acceso de sistemas no autorizados o comprometidos

Prioridad:   Media
Estimación:  S
Dependencias: HU-03.1
Capa:        Backend
```

#### Criterios de Aceptación — HU-03.4

**Happy Path**
```gherkin
CRITERIO-3.4.1: Eliminar API Key exitosamente
  Dado que:      soy Super Admin autenticado
  Y              existe una API Key registrada
  Cuando:        envío DELETE /api/v1/ecommerces/{ecommerceId}/api-keys/{keyId}
  Entonces:      el sistema elimina el registro de loyalty_admin.api_key
  Y              publica evento en loyalty.config.exchange (API_KEY_DELETED)
  Y              Engine Service recibe evento y elimina de Caffeine Cache
  Y              responde con HTTP 204 No Content
```

**Error Path**
```gherkin
CRITERIO-3.4.2: Rechazar eliminación de key inexistente
  Dado que:      soy Super Admin autenticado
  Cuando:        envío DELETE /api/v1/ecommerces/{ecommerceId}/api-keys/invalid-id
  Entonces:      responde con HTTP 404 Not Found + mensaje "API Key no encontrada"
```

---

### Reglas de Negocio

1. **Generación de Key**: El formato es UUID v4, generado por `java.util.UUID.randomUUID()`
2. **Hashing**: Al generar el UUID, se hashea usando SHA-256. El valor plano (Plain Text) solo se envía al cliente en el 201 Created y se destruye inmediatamente.
3. **Almacenamiento**: Solo se persiste el hash SHA-256 en PostgreSQL (campo hashed_key). Nunca se guarda el valor plano.
4. **Masking**: En respuestas JSON, mostrar solo `****XXXX` (últimos 4 caracteres del hash)
5. **Unicidad**: Una key no puede estar duplicada en la base de datos (UNIQUE constraint)
6. **Pertenencia**: Una API Key pertenece a exactamente un ecommerce (FK constraint)
7. **Autorización**: Solo Super Admins autenticados pueden gestionar API Keys
8. **Sincronización**: Cambios en Admin publican en `loyalty.config.exchange` (fanout) → Engine consume y actualiza caché
9. **Validación en Engine**: Cuando el Engine recibe `Authorization: Bearer <key>`, primero la hashea con SHA-256 y luego busca ese hash en la Caffeine Cache. Si coincide, tenemos un match.
10. **Cold Start**: Al arrancar Engine, carga todas las keys desde `loyalty_engine.api_keys` a Caffeine

---

## 2. DISEÑO

### Modelos de Datos

#### Entidades afectadas

| Entidad | BD | Cambios | Descripción |
|---------|----|---------|----|
| `Ecommerce` | loyalty_admin | existente (referencia FK) | Entidad propietaria de las API Keys |
| `ApiKey` | loyalty_admin | **nueva** | Par (hashed_key, key_prefix, ecommerce_id) con timestamps |
| `Caffeine Cache` | Engine (in-memory) | **nueva** | Diccionario {hashed_key → ecommerce_id} |
| `key_prefix` | VARCHAR(10) | sí | NOT NULL | INDEX | Prefijo visible de la key |
| `hashed_key` | VARCHAR(255) | sí | UNIQUE NOT NULL | UNIQUE | hash SHA-256 de la API Key |
| `ecommerce_id` | UUID | sí | FK → ecommerce.id | FK | relación al ecommerce propietario |
| `expires_at` | TIMESTAMP WITH TIME ZONE | sí | NOT NULL | - | Fecha de expiración de la key |
| `is_active` | BOOLEAN | sí | DEFAULT TRUE | INDEX | Estado de la key |
| `created_at` | TIMESTAMP WITH TIME ZONE | sí | DEFAULT NOW() | - | timestamp UTC de creación |
| `updated_at` | TIMESTAMP WITH TIME ZONE | sí | DEFAULT NOW() | - | timestamp UTC de actualización |

**Índices:**
- `UNIQUE(hashed_key)` — búsqueda fast por key durante validación
- `INDEX(ecommerce_id)` — listar keys por ecommerce
- `INDEX(is_active)` — filtrar keys activas

---

#### Tabla: `api_keys` (loyalty_engine)

Copia de sincronización (solo lectura en Engine, poblada vía eventos RabbitMQ). Misma estructura que admin, excepto:
- NO tiene UPDATE triggers locales
- Poblada únicamente vía evento `ApiKeyAdded`, `ApiKeyDeleted`
- Al startup de Engine, carga esta tabla a Caffeine Cache

---

#### Modelos Java (Admin Service)

**ApiKeyCreateRequest** (Java Record)
```java
public record ApiKeyCreateRequest() { }
// Body vacío — la key se genera en el servidor
```

**ApiKeyResponse** (Java Record)
```java
public record ApiKeyResponse(
    String id,                      // id (UUID)
    String maskedKey,              // "****XXXX"
    String keyPrefix,              // Prefijo visible
    String ecommerceId,            // FK ecommerce.id
    Instant expiresAt,             // Fecha de expiración
    boolean isActive,              // Estado
    Instant createdAt,
    Instant updatedAt
) { }
```

**ApiKeyListResponse** (Java Record)
```java
public record ApiKeyListResponse(
    String uid,
    String maskedKey,
    Instant createdAt,
    Instant updatedAt
) { }
```

---

#### Modelos Java (Engine Service)

**ApiKeyValidationContext** (Java Record — interno)
```java
public record ApiKeyValidationContext(
    String hashedKey,
    String ecommerceId,
    Instant validatedAt
) { }
```

---

### API Endpoints

#### POST /api/v1/ecommerces/{ecommerceId}/api-keys
- **Descripción**: Genera una nueva API Key para un ecommerce
- **Auth requerida**: sí (Super Admin via Bearer token)
- **Request Body**: `{}`  (vacío)
- **Response 201**:
  ```json
  {
    "uid": "550e8400-e29b-41d4-a716-446655440000",
    "maskedKey": "****0000",
    "ecommerceId": "220e8400-e29b-41d4-a716-446655440111",
    "createdAt": "2026-03-25T10:30:00Z",
    "updatedAt": "2026-03-25T10:30:00Z"
  }
  ```
- **Response 400**: ecommerceId no es UUID válido
- **Response 401**: token ausente o inválido
- **Response 404**: ecommerce no encontrado
- **Publicación de evento**: `api_key_created` en `loyalty.config.exchange`

---

#### GET /api/v1/ecommerces/{ecommerceId}/api-keys
- **Descripción**: Lista todas las API Keys de un ecommerce
- **Auth requerida**: sí (Super Admin)
- **Query params**: ninguno
- **Response 200**:
  ```json
  [
    {
      "uid": "550e8400-e29b-41d4-a716-446655440000",
      "maskedKey": "****0000",
      "createdAt": "2026-03-25T10:30:00Z",
      "updatedAt": "2026-03-25T10:30:00Z"
    },
    {
      "uid": "660e8400-e29b-41d4-a716-446655440111",
      "maskedKey": "****1111",
      "createdAt": "2026-03-25T11:00:00Z",
      "updatedAt": "2026-03-25T11:00:00Z"
    }
  ]
  ```
- **Response 401**: sin token
- **Response 404**: ecommerce no encontrado

---

#### DELETE /api/v1/ecommerces/{ecommerceId}/api-keys/{keyId}
- **Descripción**: Elimina (revoca) una API Key
- **Auth requerida**: sí (Super Admin)
- **Response 204**: eliminado exitosamente
- **Response 401**: sin token
- **Response 404**: key no encontrada
- **Publicación de evento**: `api_key_deleted` en `loyalty.config.exchange`

---

### Engine Service — Validación de API Key

#### OncePerRequestFilter: `ApiKeyAuthenticationFilter`

**Lógica:**
1. Intercepta cada request HTTP entrante
2. Extrae header `Authorization: Bearer <key>`
3. Hashea la key recibida con SHA-256
4. Busca el hash en Caffeine Cache `{hashed_key → ecommerce_id}`
5. Si existe → establece contexto de seguridad (Security context) + next
6. Si NO existe → responde HTTP 401 Unauthorized

**Comportamiento edge case (Cold Start):**
- Al arrancar Engine, ejecuta `@PostConstruct` → carga todas las keys desde `loyalty_engine.api_keys` a Caffeine
- Si tabla está vacía, caché queda vacía (esperando eventos)

#### Consumidor RabbitMQ: `ApiKeyEventListener`

**Eventos esperados en queue `engine-api-keys-queue` (listener de `loyalty.config.exchange`):**

**Evento: `ApiKeyCreated`**
```json
{
  "eventType": "API_KEY_CREATED",
  "keyId": "550e8400-e29b-41d4-a716-446655440000",
  "hashedKey": "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3",
  "ecommerceId": "220e8400-e29b-41d4-a716-446655440111",
  "createdAt": "2026-03-25T10:30:00Z"
}
```

**Consumidor action:**
1. Persiste en `loyalty_engine.api_keys` (tabla de sincronización)
2. Inserta en Caffeine Cache: `cache.put(hashedKey, ecommerceId)`

---

**Evento: `ApiKeyDeleted`**
```json
{
  "eventType": "API_KEY_DELETED",
  "keyId": "550e8400-e29b-41d4-a716-446655440000",
  "hashedKey": "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3",
  "ecommerceId": "220e8400-e29b-41d4-a716-446655440111",
  "deletedAt": "2026-03-25T10:35:00Z"
}
```

**Consumidor action:**
1. Elimina de `loyalty_engine.api_keys`
2. Elimina de Caffeine Cache: `cache.invalidate(hashedKey)`

---

### Configuración RabbitMQ

**Admin Service (Producer):**
- Publica eventos en `loyalty.config.exchange` (tipo: FANOUT)

**Engine Service (Consumer):**
- Queue: `engine-api-keys-queue`
- Binding: `engine-api-keys-queue` → `loyalty.config.exchange` (fanout)
- Listener: `ApiKeyEventListener.onApiKeyEvent(String payload)`

---

### Notas de Implementación

1. **No encriptación de key**: Las claves se almacenan en texto plano. En producción, considerar encripción en BD.
2. **UUID sin guiones opcionales**: El formato es con guiones: `550e8400-e29b-41d4-a716-446655440000`
3. **Masking**: Los últimos 4 caracteres después del último guión. Ej: para `550e8400-e29b-41d4-a716-446655440000`, mostrar `****0000`
4. **Atomicidad**: Pub/Sub puede sufrir pérdidas. Considerar re-sync periódica o DLQ.
5. **Concurrencia**: Usar constructores inyectados (no `@Autowired` en campos) para thread-safety

---

## 3. LISTA DE TAREAS

> Checklist accionable para todos los agentes. Marcar cada ítem (`[x]`) al completarlo.
> El Orchestrator monitorea este checklist para determinar el progreso.

### Backend — Admin Service

#### Implementación — Modelos & Datos
- [x] Crear `ApiKeyEntity` con `@Entity`, `@Table("api_key")`
- [x] Crear constructor, getters (Lombok `@Data`)
- [x] Agregar JPA annotations: `@Id`, `@GeneratedValue`, `@ManyToOne` (FK a Ecommerce)
- [x] Crear `ApiKeyRepository extends JpaRepository<ApiKeyEntity, UUID>`
- [x] Implementar método `findByHashedKey(String hashedKey): Optional<ApiKeyEntity>`
- [x] Implementar método `findByEcommerceId(UUID ecommerceId): List<ApiKeyEntity>`
- [x] Crear migration Flyway V1__Create_api_keys_table.sql con constraints UNIQUE(hashed_key), FK(ecommerce_id)

#### Implementación — Servicios
- [x] Crear `ApiKeyService` (constructor injection)
- [x] Método `createApiKey(UUID ecommerceId): ApiKeyResponse`
  - [x] Validar que `ecommerceId` existe en BD
  - [x] Generar UUID v4 vía `UUID.randomUUID()`
  - [x] Persistir en BD
  - [x] Publicar evento `ApiKeyCreated` en RabbitMQ
  - [x] Retornar `ApiKeyResponse` con masked key
- [x] Método `getApiKeysByEcommerce(UUID ecommerceId): List<ApiKeyListResponse>`
  - [x] Validar que ecommerce existe
  - [x] Retornar lista con masked keys
- [x] Método `deleteApiKey(UUID ecommerceId, UUID keyId): void`
  - [x] Validar que key pertenece a ecommerce
  - [x] Eliminar de BD
  - [x] Publicar evento `ApiKeyDeleted` en RabbitMQ

#### Implementación — Controllers
- [x] Crear `ApiKeyController` (constructor injection)
- [x] POST `/api/v1/ecommerces/{ecommerceId}/api-keys`
  - [x] Validar token (Auth header)
  - [x] Llamar `apiKeyService.createApiKey(ecommerceId)`
  - [x] Retornar 201 Created + payload
- [x] GET `/api/v1/ecommerces/{ecommerceId}/api-keys`
  - [x] Validar token
  - [x] Llamar `apiKeyService.getApiKeysByEcommerce(ecommerceId)`
  - [x] Retornar 200 OK + lista
- [x] DELETE `/api/v1/ecommerces/{ecommerceId}/api-keys/{keyId}`
  - [x] Validar token
  - [x] Llamar `apiKeyService.deleteApiKey(ecommerceId, keyId)`
  - [x] Retornar 204 No Content

#### Implementación — RabbitMQ Producer
- [x] Crear `ApiKeyEventPublisher` (constructor injection de `RabbitTemplate`)
- [x] Método `publishApiKeyCreated(ApiKeyEntity): void`
  - [x] Serializar evento JSON
  - [x] Publicar en `loyalty.config.exchange` con routing key vacío
- [x] Método `publishApiKeyDeleted(UUID keyId, String hashedKey, UUID ecommerceId): void`
  - [x] Serializar evento JSON
  - [x] Publicar en `loyalty.config.exchange`

### Backend — Engine Service

#### Implementación — Modelos & Datos
- [x] Crear `ApiKeyEntity` con `@Entity`, `@Table("api_key")`
- [x] Crear `ApiKeyRepository extends JpaRepository<ApiKeyEntity, UUID>`
- [x] Implementar `findByHashedKey(String hashedKey): Optional<ApiKeyEntity>`
- [x] Poblar Caffeine Cache: `cache.put(hashedKey, ecommerceId) for each`
- [x] Método `validateKey(String plainKey): boolean`
  - [x] Hashear la key recibida con SHA-256
  - [x] Retornar `cache.getIfPresent(hashedKey) != null`
- [x] Método `addKey(String plainKey, UUID ecommerceId): void`
  - [x] Hashear la key con SHA-256
  - [x] Insertar en BD y caché: `cache.put(hashedKey, ecommerceId)`
- [x] Método `removeKey(String hashedKey): void`
    - [x] Eliminar de BD
    - [x] Invalidar en caché

#### Implementación — Seguridad (Filter)
- [x] Crear `ApiKeyAuthenticationFilter extends OncePerRequestFilter`
  - [x] Override `doFilterInternal(HttpServletRequest, HttpServletResponse, FilterChain)`
  - [x] Extraer header `Authorization`
  - [x] Parsear Bearer token
  - [x] Validar vía `apiKeyCache.validateKey(token)`
  - [x] Si válido → establecer Security Context + `filterChain.doFilter(...)`
  - [x] Si inválido → retornar 401 Unauthorized
- [x] Registrar filter en `SecurityConfiguration` (orden: antes de autenticación estándar)

#### Implementación — RabbitMQ Consumer
- [x] Crear `ApiKeyEventListener` (componente `@Component`)
- [x] Método `@RabbitListener(queues = "engine-api-keys-queue") onApiKeyEvent(String payload): void`
  - [x] Parsear JSON del evento
  - [x] Si `eventType == API_KEY_CREATED` → llamar `apiKeyCache.addKey(...)`
  - [x] Si `eventType == API_KEY_DELETED` → llamar `apiKeyCache.removeKey(...)`

#### Configuración RabbitMQ — Engine
- [x] Declarar queue `engine-api-keys-queue` en `RabbitConfiguration`
- [x] Declarar exchange `loyalty.config.exchange` (tipo: FANOUT)
- [x] Binding: queue → exchange

### Frontend

- [ ] No se requieren cambios en fase 1 (HU-03 es admin-only, no tiene UI de usuario)
- [ ] Opcional: Crear página admin `/admin/api-keys` para gestión visual (fase posterior)

### QA

- [ ] Ejecutar skill `/gherkin-case-generator` → criterios CRITERIO-3.1.1 → 3.4.2
- [ ] Ejecutar skill `/risk-identifier` → clasificación ASD de riesgos
- [ ] Ejecutar skill `/performance-analyzer` → SLA de validación de key (< 1ms en caché)
- [ ] Revisar cobertura de tests: 100% de criterios de aceptación
- [ ] Validar integración RabbitMQ: pub en admin → evento en engine → caché actualizada
- [ ] Validar cold start: reiniciar engine → caché cargada
- [ ] Validar masking: keys nunca aparecen completas en logs/responses
- [ ] Actualizar estado spec: `status: IMPLEMENTED` al completar todas las tareas

---

## Tests Backend

### Unit Tests (ApiKeyService)

- [ ] `testCreateApiKey_generatesUuidAndHashes` — verifica generación de UUID y hashing SHA-256
- [ ] `testCreateApiKey_validEcommerce_savesAndPublishes` — happy path: guarda en repo y publica evento
- [ ] `testCreateApiKey_ecommerceNotFound_throwsException` — error cuando ecommerce no existe
- [ ] `testCreateApiKey_returnsFullKeyOnCreation` — al crear retorna la key completa (no masked)
- [ ] `testListApiKeys_returnsMaskedKeys` — listar siempre retorna ****XXXX (nunca la key real)
- [ ] `testDeleteApiKey_exists_deletesAndPublishes` — elimina y publica evento
- [ ] `testDeleteApiKey_notFound_throwsException` — error cuando key no existe

### Unit Tests (ApiKeyController)

- [ ] `testPostApiKey_returns201` — endpoint creación retorna 201
- [ ] `testGetApiKeys_returns200` — endpoint listado retorna 200
- [ ] `testDeleteApiKey_returns204` — endpoint eliminación retorna 204

### Unit Tests (ApiKeyAuthenticationFilter)

- [ ] `testDoFilter_validKey_continuesChain` — filtro permite request válida
- [ ] `testDoFilter_invalidKey_returns401` — filtro rechaza key inválida
- [ ] `testDoFilter_noHeader_returns401` — filtro rechaza sin header Authorization

### Integration Tests (Service)

- [ ] `testCreateApiKey_integration_persistsToDatabase` — flujo completo guarda en BD
- [ ] `testListApiKeys_integration_returnsFromDatabase` — listado trae datos reales

### Integration Tests (RabbitMQ)

- [ ] `testApiKeyEventPublisher_sendsMessage` — publica mensaje en el exchange

### Integration Tests (Cache)

- [ ] `testApiKeyCache_addAndRetrieve` — Caffeine cache guarda y recupera
- [ ] `testApiKeyCache_loadsOnStartup` — al iniciar carga keys desde BD

