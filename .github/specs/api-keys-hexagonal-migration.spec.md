---
id: SPEC-001
status: APPROVED
feature: api-keys-hexagonal-migration
created: 2026-04-06
updated: 2026-04-06
author: spec-generator
version: "1.0"
related-specs: []
---

# Spec: Migración de API Keys a Arquitectura Hexagonal con TDD

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de iniciar migración.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPRECATED

---

## 1. REQUERIMIENTOS

### Descripción
Refactorizar el feature **API Keys** del service-admin desde una arquitectura monolítica tradicional a una **Arquitectura Hexagonal (Ports & Adapters)**, manteniendo 100% de funcionalidad actual y validando el cumplimiento del modelo estándar. La migración se ejecutará usando **TDD**, generando unit tests de servicios primero, luego migrando la lógica de negocio a puertos/adapters, y finalmente testando controllers.

### Requerimiento de Negocio
El feature api-keys está funcional pero viola el modelo hexagonal estándar del proyecto. Se requiere:
1. Separar responsabilidades entre puertos (entrada/salida) y servicios
2. Aislar persistencia y eventos en adapters
3. Permitir testabilidad completa de la lógica de negocio sin Spring Context
4. Asegurar que 100% de la funcionalidad actual se preserve

### Historias de Usuario

#### HU-01: Crear Puerto de Entrada (ApiKeyUseCase)

```
Como:        Backend Developer
Quiero:      Definir una interfaz de puertos (in) para todos los casos de uso de API Keys
Para:        Abstraer la implementación del servicio del controller y permitir múltiples adaptadores

Prioridad:   Alta
Estimación:  S
Dependencias: Ninguna
Capa:        Backend
```

**Criterios de Aceptación — HU-01**

```gherkin
CRITERIO-1.1: Puerto de entrada define contratos de negocio
  Dado que:  el feature api-keys existe y está implementado
  Cuando:    reviso application/port/in/ApiKeyUseCase.java
  Entonces:  el puerto contiene exactamente 3 métodos públicos:
             - createApiKey(UUID ecommerceId): ApiKeyCreatedResponse
             - getApiKeysByEcommerce(UUID ecommerceId): List<ApiKeyListResponse>
             - deleteApiKey(UUID ecommerceId, UUID keyId): void
```

#### HU-02: Crear Puertos de Salida (Persistencia y Eventos)

```
Como:        Backend Developer
Quiero:      Definir una interfaz de puertos (out) para persistencia y eventos
Para:        Aislar las integraciones externas (BD, RabbitMQ) del servicio

Prioridad:   Alta
Estimación:  S
Dependencias: HU-01
Capa:        Backend
```

**Criterios de Aceptación — HU-02**

```gherkin
CRITERIO-2.1: Puerto de persistencia define operaciones de datos
  Dado que:  necesito abstraer el repositorio JPA
  Cuando:    reviso application/port/out/ApiKeyPersistencePort.java
  Entonces:  el puerto contiene exactamente estos métodos:
             - save(ApiKeyEntity entity): ApiKeyEntity
             - findById(UUID id): Optional<ApiKeyEntity>
             - findByEcommerceId(UUID ecommerceId): List<ApiKeyEntity>
             - deleteById(UUID id): void
             - existsByHashedKey(String hashedKey): boolean

CRITERIO-2.2: Puerto de eventos define publicación asíncrona
  Dado que:  necesito abstraer el publisher de RabbitMQ
  Cuando:    reviso application/port/out/ApiKeyEventPort.java
  Entonces:  el puerto contiene exactamente estos métodos:
             - publishApiKeyCreated(ApiKeyEventPayload event): void
             - publishApiKeyDeleted(ApiKeyEventPayload event): void
```

#### HU-03: Implementar ApiKeyServiceImpl (Lógica de Negocio con TDD)

```
Como:        Backend Developer / Test Engineer
Quiero:      Implementar la lógica de negocio como service testeable desacoplado
Para:        Validar toda la lógica de API Keys sin Spring Context, RabbitMQ ni BD real

Prioridad:   Alta
Estimación:  M
Dependencias: HU-01, HU-02
Capa:        Backend
```

**Criterios de Aceptación — HU-03**

```gherkin
CRITERIO-3.1: Service implementa el puerto de entrada
  Dado que:  defino ApiKeyServiceImpl
  Cuando:    reviso application/service/ApiKeyService.java
  Entonces:  la clase:
             - @Service @RequiredArgsConstructor
             - implements ApiKeyUseCase
             - tiene exactamente 2 campos final inyectados (PersistencePort, EventPort)
             - NO inyecta Repository directo (solo puertos)

CRITERIO-3.2: Service delega a puertos, no implementa detalles
  Dado que:  el service ofrece createApiKey(ecommerceId)
  Cuando:    ejecuto el método
  Entonces:  el service:
             - valida que ecommerce existe (delega a servicio)
             - generaUUID v4 como plainkey
             - hashea con SHA-256 para persistencia
             - delega save a persistencePort
             - delega publish a eventPort
             - retorna ApiKeyCreatedResponse con plainkey (sin enmascarar)

CRITERIO-3.3: Service entra en caché antes de eventos
  Dado que:  creo o borro una API Key
  Cuando:    el service completa la operación
  Entonces:  el evento se publica SIEMPRE (con o sin BD)
             - para crear: ApiKeyEventPayload con hashedKey
             - para borrar: ApiKeyEventPayload con hashedKey
             - headers incluyen x-event-type, x-event-id, x-key-id, x-ecommerce-id

CRITERIO-3.4: Service valida reglas de negocio
  Dado que:  valido pertenencia de key a ecommerce
  Cuando:    deleteApiKey(ecommerceId, keyId)
  Entonces:  el service verifica que key.ecommerceId == ecommerceId
             - si no coincide → lanza ApiKeyNotFoundException
             - si no existe → lanza ApiKeyNotFoundException
```

#### HU-04: Crear Adapter de Persistencia (JpaApiKeyAdapter)

```
Como:        Backend Developer
Quiero:      Implementar el puerto de persistencia usando JPA
Para:        Mantener la integración con PostgreSQL sin acoplamiento en el service

Prioridad:   Alta
Estimación:  XS
Dependencias: HU-02
Capa:        Backend
```

**Criterios de Aceptación — HU-04**

```gherkin
CRITERIO-4.1: Adapter implementa puerto de persistencia
  Dado que:  necesito persistir API Keys
  Cuando:    reviso infrastructure/persistence/jpa/JpaApiKeyAdapter.java
  Entonces:  la clase:
             - @Component @RequiredArgsConstructor
             - implements ApiKeyPersistencePort
             - tiene field final: ApiKeyRepository repository
             - delega TODO a repository sin lógica adicional

CRITERIO-4.2: Repository sigue siendo JPA Repository
  Dado que:  necesito acceso a datos con JPA
  Cuando:    reviso domain/repository/ApiKeyRepository.java
  Entonces:  la interfaz:
             - extends JpaRepository<ApiKeyEntity, UUID>
             - tiene método: findByEcommerceId(UUID): List<ApiKeyEntity>
             - tiene método: existsByHashedKey(String): boolean
             - vive en domain/repository/ (sin cambios de ubicación)
```

#### HU-05: Crear Adapter de Eventos (ApiKeyEventAdapter)

```
Como:        Backend Developer
Quiero:      Implementar el puerto de eventos usando RabbitMQ
Para:        Mantener la publicación asíncrona sin acoplamiento en el service

Prioridad:   Alta
Estimación:  XS
Dependencias: HU-02
Capa:        Backend
```

**Criterios de Aceptación — HU-05**

```gherkin
CRITERIO-5.1: Adapter implementa puerto de eventos
  Dado que:  necesito publicar eventos de API Keys
  Cuando:    reviso infrastructure/messaging/ApiKeyEventAdapter.java
  Entonces:  la clase:
             - @Component @RequiredArgsConstructor
             - implements ApiKeyEventPort
             - tiene field final: ApiKeyEventPublisher publisher
             - delega getApiKeyCreated/Deleted a publisher

CRITERIO-5.2: Publisher mantiene interfaz RabbitMQ existente
  Dado que:  el publisher actual funciona
  Cuando:    reviso infrastructure/rabbitmq/ApiKeyEventPublisher.java
  Entonces:  la clase NO cambia:
             - mantiene @Component @RequiredArgsConstructor
             - mantiene métodos publishApiKeyCreated/Deleted
             - mantiene configuración RabbitMQ
             - mantiene headers x-event-type, x-key-id, etc.
             - mantiene HashingUtil.sha256() para hashedKey
```

#### HU-06: Actualizar Controller (Inyectar Puerto, no Servicio Concreto)

```
Como:        Backend Developer
Quiero:      Cambiar el controller para inyectar el puerto (ApiKeyUseCase) en lugar del service concreto
Para:        Desacoplar el controller de la implementación y permitir mocking en tests

Prioridad:   Alta
Estimación:  XS
Dependencias: HU-01, HU-03
Capa:        Backend
```

**Criterios de Aceptación — HU-06**

```gherkin
CRITERIO-6.1: Controller inyecta puerto, no implementación
  Dado que:  actualizo ApiKeyController.java
  Cuando:    reviso presentation/controller/ApiKeyController.java
  Entonces:  la clase:
             - @RestController @RequestMapping("/api/v1/ecommerces/{ecommerceId}/api-keys")
             - @RequiredArgsConstructor
             - tiene field final: ApiKeyUseCase apiKeyUseCase (NO ApiKeyService!)
             - métodos POST/GET/DELETE deleguen a apiKeyUseCase

CRITERIO-6.2: Endpoints mantienen contrato actual (routes, request, response, status)
  Dado que:  actualizo el controller
  Cuando:    ejecuto POST /api/v1/ecommerces/{ecommerceId}/api-keys
  Entonces:  retorna:
             - Status: 201 Created
             - Body: ApiKeyCreatedResponse (con plainkey sin enmascarar)
             - Comportamiento idéntico a la versión anterior

CRITERIO-6.3: Endpoints GET mantienen masking de keys
  Dado que:  ejecuto GET /api/v1/ecommerces/{ecommerceId}/api-keys
  Entonces:  retorna:
             - Status: 200 OK
             - Body: List<ApiKeyListResponse>
             - maskedKey en formato ****XXXX (últimos 4 caracteres)
             - idéntico a la versión anterior
```

#### HU-07: Unit Tests de Service (TDD-First)

```
Como:        Test Engineer / Backend Developer
Quiero:      Generar suite completa de tests unitarios para ApiKeyServiceImpl
Para:        Validar lógica de negocio sin BD, sin RabbitMQ, sin Spring Context

Prioridad:   Alta
Estimación:  L
Dependencias: HU-01, HU-02, HU-03
Capa:        Backend / Tests
```

**Criterios de Aceptación — HU-07**

```gherkin
CRITERIO-7.1: Tests de createApiKey - happy path
  Dado que:  ejecuto createApiKey(ecommerceId) en service
  Cuando:    ecommerce existe y no hay errores
  Entonces:  el test verifica:
             - generateUUID() retorna plainkey válido (UUID v4)
             - persistencePort.save() es llamado con entity correcta
             - entity tiene hashedKey = SHA256(plainkey)
             - eventPort.publishApiKeyCreated() es llamado con payload
             - response contiene plainkey SIN enmascarar
             - timestamps createdAt/updatedAt están presentes

CRITERIO-7.2: Tests de createApiKey - error paths
  Dado que:  valido errores en createApiKey
  Cuando:    ecommerce NO existe
  Entonces:  el test verifica:
             - se lanza EcommerceNotFoundException
             - repository.save() NO es llamado
             - eventPort NO publica nada

CRITERIO-7.3: Tests de getApiKeysByEcommerce - happy path
  Dado que:  ejecuto getApiKeysByEcommerce(ecommerceId)
  Cuando:    existen keys en ese ecommerce
  Entonces:  el test verifica:
             - persistencePort.findByEcommerceId() retorna list de entities
             - cada response tiene maskedKey en formato ****XXXX
             - length de maskedKey es siempre 8 caracteres
             - respuesta es List<ApiKeyListResponse>

CRITERIO-7.4: Tests de getApiKeysByEcommerce - lista vacía
  Dado que:  ejecuto getApiKeysByEcommerce(ecommerceId)
  Cuando:    NO existen keys en ese ecommerce
  Entonces:  el test verifica:
             - persistencePort.findByEcommerceId() retorna empty list
             - response es List vacía (size == 0)
             - NO hay errores

CRITERIO-7.5: Tests de deleteApiKey - happy path
  Dado que:  ejecuto deleteApiKey(ecommerceId, keyId)
  Cuando:    key existe y pertenece a ecommerce
  Entonces:  el test verifica:
             - persistencePort.findById() retorna Optional<entity>
             - entity.ecommerceId == ecommerceId ✅
             - persistencePort.deleteById() es llamado con keyId
             - eventPort.publishApiKeyDeleted() es llamado
             - NO hay excepción

CRITERIO-7.6: Tests de deleteApiKey - error: key no existe
  Dado que:  valido error en deleteApiKey
  Cuando:    persistencePort.findById() retorna empty
  Entonces:  el test verifica:
             - se lanza ApiKeyNotFoundException
             - deleteById() NO es llamado
             - eventPort NO publica

CRITERIO-7.7: Tests de deleteApiKey - error: key no pertenece a ecommerce
  Dado que:  valido error de pertenencia
  Cuando:    key.ecommerceId != ecommerceId solicitado
  Entonces:  el test verifica:
             - se lanza ApiKeyNotFoundException
             - deleteById() NO es llamado
             - eventPort NO publica

CRITERIO-7.8: Tests usan Mockito para puertos
  Dado que:  configuro tests
  Cuando:    reviso ApiKeyServiceImplTest.java
  Entonces:  el test:
             - @ExtendWith(MockitoExtension.class)
             - @Mock ApiKeyPersistencePort persistencePort
             - @Mock ApiKeyEventPort eventPort
             - @InjectMocks ApiKeyServiceImpl service
             - NO carga Spring Context (@SpringBootTest forbidden)
```

#### HU-08: Validación de Funcionalidad Post-Migración

```
Como:        QA Engineer / Backend Developer
Quiero:      Validar que 100% de endpoints funcionen igual que antes
Para:        Asegurar que la migración a hexagonal NO rompe funcionalidad

Prioridad:   Alta
Estimación:  M
Dependencias: HU-01, HU-02, HU-03, HU-04, HU-05, HU-06, HU-07
Capa:        Backend / Tests
```

**Criterios de Aceptación — HU-08**

```gherkin
CRITERIO-8.1: Endpoint POST sigue devolviendo 201 con plainkey
  Dado que:  llamo POST /api/v1/ecommerces/{ecommerceId}/api-keys
  Cuando:    el request es válido
  Entonces:  responde 201 Created
             - body contiene plainkey SIN enmascarar
             - plainkey es UUID v4 válido
             - expiresAt está en 365 días

CRITERIO-8.2: Endpoint GET sigue devolviendo 200 con masking
  Dado que:  llamo GET /api/v1/ecommerces/{ecommerceId}/api-keys
  Cuando:    existen keys en el ecommerce
  Entonces:  responde 200 OK
             - body es List<ApiKeyListResponse>
             - cada item tiene maskedKey = ****XXXX
             - isActive es booleano correcto

CRITERIO-8.3: Endpoint DELETE sigue devolviendo 204
  Dado que:  llamo DELETE /api/v1/ecommerces/{ecommerceId}/api-keys/{keyId}
  Cuando:    key existe y pertenece al ecommerce
  Entonces:  responde 204 No Content
             - body está vacío
             - key es eliminada de BD
             - evento ApiKeyDeleted es publicado en RabbitMQ

CRITERIO-8.4: RabbitMQ sigue recibiendo eventos
  Dado que:  creo o elimino una API Key
  Cuando:    la operación completa
  Entonces:  RabbitMQ recibe evento ApiKeyEventPayload con:
             - eventType: "API_KEY_CREATED" or "API_KEY_DELETED"
             - hashedKey: SHA-256 hash (nunca plainkey)
             - keyId, ecommerceId, timestamp correctos
             - headers x-event-id, x-event-type, x-key-id, x-ecommerce-id presentes
```

### Reglas de Negocio
1. **Arquitectura Hexagonal Obligatoria**: Service solo depende de puertos (in/out), nunca de implementaciones concretas
2. **TDD-First para Services**: Tests unitarios de service ANTES de migrar su implementación
3. **Zero Breaking Changes**: 100% de endpoints, DTOs, RabbitMQ y funcionalidad se mantienen idéntica
4. **Persistencia en Adapter**: `dao/repository` acceso está SOLO en `JpaApiKeyAdapter`, nunca en `ApiKeyServiceImpl`
5. **Eventos en Adapter**: Publicación de RabbitMQ está SOLO en `ApiKeyEventAdapter`, nunca en `ApiKeyServiceImpl`
6. **Controllers = Delgados**: Controllers solo traducen HTTP → puertos → HTTP, sin lógica de negocio
7. **DTOs = Intactos**: Todas las clases DTO existentes (Request, Response, EventPayload) NO se modifican
8. **Hashing = Seguro**: Keys se almacenan siempre como hash SHA-256, nunca plaintext en BD

---

## 2. DISEÑO

### Modelos de Datos

#### Entidades afectadas (sin cambios)
| Entidad | Almacén | Cambios | Descripción |
|---------|---------|---------|-------------|
| `ApiKeyEntity` | tabla `api_keys` | ninguno | Se mantiene idéntica, solo migra a usar adapter |

#### Estructura de carpetas post-migración
```
src/main/java/com/loyalty/service_admin/
├── application/
│   ├── dto/apikey/                    [EXISTENTE]
│   │   ├── ApiKeyCreateRequest.java
│   │   ├── ApiKeyCreatedResponse.java
│   │   ├── ApiKeyListResponse.java
│   │   ├── ApiKeyResponse.java
│   │   └── ApiKeyEventPayload.java
│   ├── port/                          [NUEVO]
│   │   ├── in/
│   │   │   └── ApiKeyUseCase.java     [NUEVO]
│   │   └── out/
│   │       ├── ApiKeyPersistencePort.java  [NUEVO]
│   │       └── ApiKeyEventPort.java        [NUEVO]
│   └── service/
│       └── ApiKeyService.java      [MIGRADO: extiende ApiKeyUseCase]
│
├── domain/
│   ├── entity/
│   │   └── ApiKeyEntity.java          [EXISTENTE, sin cambios]
│   └── repository/
│       └── ApiKeyRepository.java      [EXISTENTE, sin cambios]
│
├── infrastructure/
│   ├── persistence/
│   │   └── jpa/
│   │       └── JpaApiKeyAdapter.java  [NUEVO]
│   ├── messaging/
│   │   └── ApiKeyEventAdapter.java    [NUEVO]
│   └── rabbitmq/
│       └── ApiKeyEventPublisher.java  [EXISTENTE, sin cambios]
│
└── presentation/
    └── controller/
        └── ApiKeyController.java      [ACTUALIZADO: inyecta puerto, no service]
```

### API Endpoints (SIN CAMBIOS)

#### POST /api/v1/ecommerces/{ecommerceId}/api-keys
- **Descripción**: Crea una nueva API Key
- **Auth requerida**: sí (@PreAuthorize("isAuthenticated()"))
- **Request Body**: `{}` vacío (`ApiKeyCreateRequest`)
- **Response 201**:
  ```json
  {
    "uid": "550e8400-e29b-41d4-a716-446655440000",
    "key": "550e8400-e29b-41d4-a716-446655440000",
    "expiresAt": "2027-04-06T10:00:00Z",
    "ecommerceId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "createdAt": "2026-04-06T10:00:00Z",
    "updatedAt": "2026-04-06T10:00:00Z"
  }
  ```
- **Response 400**: ecommerce inválido
- **Response 401**: token ausente o expirado
- **Response 403**: STORE_ADMIN intenta acceder a otro ecommerce

#### GET /api/v1/ecommerces/{ecommerceId}/api-keys
- **Descripción**: Lista todas las API Keys del ecommerce
- **Auth requerida**: sí
- **Response 200**:
  ```json
  [
    {
      "uid": "550e8400-e29b-41d4-a716-446655440000",
      "maskedKey": "****0000",
      "expiresAt": "2027-04-06T10:00:00Z",
      "isActive": true,
      "createdAt": "2026-04-06T10:00:00Z",
      "updatedAt": "2026-04-06T10:00:00Z"
    }
  ]
  ```
- **Response 401**: token ausente

#### DELETE /api/v1/ecommerces/{ecommerceId}/api-keys/{keyId}
- **Descripción**: Elimina una API Key
- **Auth requerida**: sí
- **Response 204**: eliminado exitosamente
- **Response 401**: token ausente
- **Response 403**: STORE_ADMIN intenta acceder a otro ecommerce
- **Response 404**: key no encontrada

### Arquitectura y Dependencias

#### Dependencias de Inyección (DI) Post-Migración

**Controller (no cambia la DI, solo refactoriza):**
```
ApiKeyController
  ├─> ApiKeyUseCase (interfaz — PUERTO)
  │   └─> ApiKeyServiceImpl (implementación)
  │       ├─> ApiKeyPersistencePort (interfaz)
  │       │   └─> JpaApiKeyAdapter (implementación)
  │       │       └─> ApiKeyRepository (JPA)
  │       └─> ApiKeyEventPort (interfaz)
  │           └─> ApiKeyEventAdapter (implementación)
  │               └─> ApiKeyEventPublisher
```

#### Inyecciones de Dependencias Detalladas

**ApiKeyController:**
```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ecommerces/{ecommerceId}/api-keys")
public class ApiKeyController {
    private final ApiKeyUseCase apiKeyUseCase;  // PUERTO ✅
    
    // POST, GET, DELETE deleguen a apiKeyUseCase
}
```

**ApiKeyServiceImpl:**
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyServiceImpl implements ApiKeyUseCase {
    private final ApiKeyPersistencePort persistencePort;   // PUERTO ✅
    private final ApiKeyEventPort eventPort;               // PUERTO ✅
    private final EcommerceService ecommerceService;       // Otro servicio OK
    
    // Implementa createApiKey, getApiKeysByEcommerce, deleteApiKey
}
```

**JpaApiKeyAdapter:**
```java
@Component
@RequiredArgsConstructor
public class JpaApiKeyAdapter implements ApiKeyPersistencePort {
    private final ApiKeyRepository repository;  // JPA directo OK (adapter)
    
    // Delega todo a repository
}
```

**ApiKeyEventAdapter:**
```java
@Component
@RequiredArgsConstructor
public class ApiKeyEventAdapter implements ApiKeyEventPort {
    private final ApiKeyEventPublisher publisher;  // RabbitMQ directo OK (adapter)
    
    // Delega todo a publisher
}
```

### Notas de Implementación

#### Estrategia TDD Propuesta
1. **Fase 1 (Tests First)**: Escribir tests unitarios de `ApiKeyServiceImpl` usando Mockito
   - Mock `ApiKeyPersistencePort`
   - Mock `ApiKeyEventPort`
   - Test happy path y error paths
2. **Fase 2 (Ports)**: Crear interfaces `ApiKeyUseCase`, `ApiKeyPersistencePort`, `ApiKeyEventPort`
3. **Fase 3 (Service)**: Implementar `ApiKeyServiceImpl` según los tests (TDD)
4. **Fase 4 (Adapters)**: Crear `JpaApiKeyAdapter` y `ApiKeyEventAdapter` (delegación simple)
5. **Fase 5 (Controller)**: Cambiar inyección a puerto `ApiKeyUseCase`
6. **Fase 6 (Integration)**: Tests de integración con BD/RabbitMQ si es necesario

#### Archivos que NO se modifican
- `ApiKeyEntity.java` → sin cambios
- `ApiKeyRepository.java` → sin cambios (se mantiene en `domain/repository/`)
- `ApiKeyCreateRequest.java` → sin cambios
- `ApiKeyCreatedResponse.java` → sin cambios
- `ApiKeyListResponse.java` → sin cambios
- `ApiKeyResponse.java` → sin cambios
- `ApiKeyEventPayload.java` → sin cambios
- `ApiKeyEventPublisher.java` → sin cambios

#### Archivos que se crean
- `application/port/in/ApiKeyUseCase.java`
- `application/port/out/ApiKeyPersistencePort.java`
- `application/port/out/ApiKeyEventPort.java`
- `application/service/ApiKeyService.java`
- `infrastructure/persistence/jpa/JpaApiKeyAdapter.java`
- `infrastructure/messaging/ApiKeyEventAdapter.java`
- `application/service/ApiKeyServiceImplTest.java` (tests)

#### Archivos que se actualizan
- `presentation/controller/ApiKeyController.java`
  - Cambio: inyectar `ApiKeyUseCase` en lugar de `ApiKeyService`
  - Cambio: usar `apiKeyUseCase.createApiKey()` en lugar de `apiKeyService.createApiKey()`
  - Cambio: usar `apiKeyUseCase.getApiKeysByEcommerce()` en lugar de `apiKeyService.getApiKeysByEcommerce()`
  - Cambio: usar `apiKeyUseCase.deleteApiKey()` en lugar de `apiKeyService.deleteApiKey()`

---

## 3. LISTA DE TAREAS

> Checklist accionable para todos los agentes. Marcar cada ítem (`[x]`) al completarlo.

### Backend — Fase 1: Puertos (Arq. Hexagonal)

#### HU-01: Puerto de Entrada (ApiKeyUseCase)
- [ ] Crear archivo `application/port/in/ApiKeyUseCase.java`
- [ ] Definir 3 métodos públicos: createApiKey, getApiKeysByEcommerce, deleteApiKey
- [ ] Documentar con @Contract JavaDoc (qué contrato define)
- [ ] Validar que NO contiene lógica (solo interfaz)

#### HU-02: Puertos de Salida (Persistencia y Eventos)
- [ ] Crear archivo `application/port/out/ApiKeyPersistencePort.java`
  - [ ] Métodos: save, findById, findByEcommerceId, deleteById, existsByHashedKey
- [ ] Crear archivo `application/port/out/ApiKeyEventPort.java`
  - [ ] Métodos: publishApiKeyCreated, publishApiKeyDeleted
- [ ] Documentar ambos puertos con @Contract JavaDoc

### Backend — Fase 2: Tests Unitarios (TDD-First)

#### HU-07: Unit Tests de Service
- [ ] Crear archivo `application/service/ApiKeyServiceImplTest.java`
- [ ] Test 01: `test_createApiKey_success`
  - [ ] Verifica que persistencePort.save() es llamado
  - [ ] Verifica que eventPort.publishApiKeyCreated() es llamado
  - [ ] Verifica que response contiene plainkey sin enmascarar
- [ ] Test 02: `test_createApiKey_ecommerce_not_found`
  - [ ] Verifica que lanza EcommerceNotFoundException
  - [ ] Verifica que persistencePort.save() NO es llamado
- [ ] Test 03: `test_getApiKeysByEcommerce_success`
  - [ ] Verifica que persistencePort.findByEcommerceId() es llamado
  - [ ] Verifica que cada response tiene maskedKey = ****XXXX
- [ ] Test 04: `test_getApiKeysByEcommerce_empty`
  - [ ] Verifica que retorna lista vacía
- [ ] Test 05: `test_deleteApiKey_success`
  - [ ] Verifica que persistencePort.findById() es llamado
  - [ ] Verifica que persistencePort.deleteById() es llamado
  - [ ] Verifica que eventPort.publishApiKeyDeleted() es llamado
- [ ] Test 06: `test_deleteApiKey_not_found`
  - [ ] Verifica que lanza ApiKeyNotFoundException
  - [ ] Verifica que persistencePort.deleteById() NO es llamado
- [ ] Test 07: `test_deleteApiKey_wrong_ecommerce`
  - [ ] Verifica que lanza ApiKeyNotFoundException si no pertenece
  - [ ] Verifica que persistencePort.deleteById() NO es llamado
- [ ] Configurar tests con @ExtendWith(MockitoExtension.class)
- [ ] Usar @Mock para puertos, @InjectMocks para service
- [ ] Validar cobertura mínima 90% líneas de código

### Backend — Fase 3: Implementación Service

#### HU-03: ApiKeyServiceImpl (Lógica de Negocio)
- [ ] Crear archivo `application/service/ApiKeyService.java`
- [ ] Anotar con @Service @RequiredArgsConstructor @Slf4j
- [ ] Implementar `ApiKeyUseCase`
- [ ] Inyectar SOLO puertos (ApiKeyPersistencePort, ApiKeyEventPort)
- [ ] Inyectar `EcommerceService` para validación de existencia
- [ ] Implementar `createApiKey(UUID ecommerceId): ApiKeyCreatedResponse`
  - [ ] Validar que ecommerce existe
  - [ ] Generar UUID v4 como plainkey
  - [ ] Hashear con SHA-256 para persistencia
  - [ ] Crear entity con expiresAt = 365 días
  - [ ] Delegar save a persistencePort
  - [ ] Publicar evento a eventPort
  - [ ] Retornar response con plainkey sin enmascarar
- [ ] Implementar `getApiKeysByEcommerce(UUID): List<ApiKeyListResponse>`
  - [ ] Delegar findByEcommerceId a persistencePort
  - [ ] Mapear cada entity a response con masking (****XXXX)
  - [ ] Retornar list (puede ser vacía)
- [ ] Implementar `deleteApiKey(UUID, UUID): void`
  - [ ] Validar que key existe (findById en persistencePort)
  - [ ] Validar que pertenece al ecommerce (key.ecommerceId == ecommerceId)
  - [ ] Delegar delete a persistencePort
  - [ ] Publicar evento a eventPort
- [ ] Agregar método privado `maskKey(String): String`
  - [ ] Retorna formato ****XXXX (últimos 4 caracteres)
  - [ ] Valida que input tiene mínimo 4 caracteres

### Backend — Fase 4: Adapters

#### HU-04: JpaApiKeyAdapter (Persistencia)
- [ ] Crear archivo `infrastructure/persistence/jpa/JpaApiKeyAdapter.java`
- [ ] Anotar con @Component @RequiredArgsConstructor
- [ ] Implementar `ApiKeyPersistencePort`
- [ ] Inyectar `ApiKeyRepository`
- [ ] Implementar todos los métodos:
  - [ ] `save(entity): entity` → delegar repository.save(entity)
  - [ ] `findById(id): Optional<entity>` → delegar repository.findById(id)
  - [ ] `findByEcommerceId(id): List<entity>` → delegar repository.findByEcommerceId(id)
  - [ ] `deleteById(id): void` → delegar repository.deleteById(id)
  - [ ] `existsByHashedKey(key): boolean` → delegar repository.existsByHashedKey(key)
- [ ] Validar que NO contiene lógica (solo delegación)

#### HU-05: ApiKeyEventAdapter (Eventos)
- [ ] Crear archivo `infrastructure/messaging/ApiKeyEventAdapter.java`
- [ ] Anotar con @Component @RequiredArgsConstructor
- [ ] Implementar `ApiKeyEventPort`
- [ ] Inyectar `ApiKeyEventPublisher`
- [ ] Implementar métodos:
  - [ ] `publishApiKeyCreated(event): void` → delegar publisher.publishApiKeyCreated(event)
  - [ ] `publishApiKeyDeleted(event): void` → delegar publisher.publishApiKeyDeleted(event)
- [ ] Validar que NO contiene lógica (solo delegación)

#### Validación de Repository
- [ ] Verifica que `ApiKeyRepository` tiene métodos:
  - [ ] `findByEcommerceId(UUID): List<ApiKeyEntity>`
  - [ ] `existsByHashedKey(String): boolean`
- [ ] Repositorio sigue siendo JPA Repository, sin cambios

### Backend — Fase 5: Controller

#### HU-06: Actualizar ApiKeyController
- [ ] Abrir archivo `presentation/controller/ApiKeyController.java`
- [ ] Cambiar inyección:
  - [ ] De: `private final ApiKeyService apiKeyService;`
  - [ ] A: `private final ApiKeyUseCase apiKeyUseCase;`
- [ ] Actualizar método POST:
  - [ ] Cambiar: `apiKeyService.createApiKey(...)` → `apiKeyUseCase.createApiKey(...)`
- [ ] Actualizar método GET:
  - [ ] Cambiar: `apiKeyService.getApiKeysByEcommerce(...)` → `apiKeyUseCase.getApiKeysByEcommerce(...)`
- [ ] Actualizar método DELETE:
  - [ ] Cambiar: `apiKeyService.deleteApiKey(...)` → `apiKeyUseCase.deleteApiKey(...)`
- [ ] Validar que NO cambio rutas, status codes, DTOs request/response

### Backend — Fase 6: Validación Post-Migración

#### HU-08: Validación de Funcionalidad
- [ ] Test POST /api/v1/ecommerces/{ecommerceId}/api-keys
  - [ ] Status 201 Created
  - [ ] Response contiene plainkey sin enmascarar
  - [ ] Response contiene expiresAt (365 días adelante)
- [ ] Test GET /api/v1/ecommerces/{ecommerceId}/api-keys
  - [ ] Status 200 OK
  - [ ] Response es List<ApiKeyListResponse>
  - [ ] Cada item tiene maskedKey = ****XXXX
  - [ ] Campo isActive es booleano correcto
- [ ] Test DELETE /api/v1/ecommerces/{ecommerceId}/api-keys/{keyId}
  - [ ] Status 204 No Content
  - [ ] Key es eliminada de BD (verificar con query)
  - [ ] RabbitMQ recibe evento ApiKeyDeleted
- [ ] Validar que no rompe endpoints existentes (regression test)
- [ ] Validar que no rompe seguridad Spring (@PreAuthorize funciona)

### Configuración Spring (si aplica)
- [ ] Verificar que ambos adapters están anotados con @Component
- [ ] Verificar que DI automático encuentra los beans
- [ ] Validar que no hay conflictos ambigüos de beans

---

## Resumen de Cambios

| Archivo | Acción | Razón |
|---------|--------|-------|
| `application/port/in/ApiKeyUseCase.java` | **Crear** | Puerto de entrada hexagonal |
| `application/port/out/ApiKeyPersistencePort.java` | **Crear** | Puerto de salida (persistencia) |
| `application/port/out/ApiKeyEventPort.java` | **Crear** | Puerto de salida (eventos) |
| `application/service/ApiKeyService.java` | **Crear** | Implementación del use case |
| `application/service/ApiKeyServiceImplTest.java` | **Crear** | Tests unitarios TDD |
| `infrastructure/persistence/jpa/JpaApiKeyAdapter.java` | **Crear** | Adapter persistencia |
| `infrastructure/messaging/ApiKeyEventAdapter.java` | **Crear** | Adapter eventos |
| `presentation/controller/ApiKeyController.java` | **Actualizar** | Inyectar puerto, no service |
| `application/dto/apikey/ApiKeyCreateRequest.java` | Sin cambios | DTO existente |
| `application/dto/apikey/ApiKeyCreatedResponse.java` | Sin cambios | DTO existente |
| `application/dto/apikey/ApiKeyListResponse.java` | Sin cambios | DTO existente |
| `application/dto/apikey/ApiKeyResponse.java` | Sin cambios | DTO existente |
| `application/dto/apikey/ApiKeyEventPayload.java` | Sin cambios | DTO existente |
| `domain/entity/ApiKeyEntity.java` | Sin cambios | Entidad JPA |
| `domain/repository/ApiKeyRepository.java` | Sin cambios | JPA Repository |
| `infrastructure/rabbitmq/ApiKeyEventPublisher.java` | Sin cambios | Publisher RabbitMQ |

