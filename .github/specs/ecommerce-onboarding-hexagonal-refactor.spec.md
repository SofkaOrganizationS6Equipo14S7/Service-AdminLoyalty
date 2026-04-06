---
id: SPEC-015
status: IMPLEMENTED
feature: ecommerce-onboarding-hexagonal-refactor
created: 2026-04-05
updated: 2026-04-06
author: spec-generator
version: "1.0"
related-specs: ["ecommerce-onboarding.md", "hexagonal-architecture.md"]
---

# Spec: Ecommerce Onboarding con Arquitectura Hexagonal y Cascada de Acciones

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de iniciar implementación.
> **Objetivo:** Completar la funcionalidad parcialmente implementada de ecommerce-onboarding, refactorizar a arquitectura hexagonal (ports & adapters) y validar con TDD.

---

## 1. REQUERIMIENTOS

### Descripción
Migración y completación del feature de gestión de ecommerces (HU-15) del monolito actual a arquitectura hexagonal. La funcionalidad actual crea y actualiza ecommerces, pero falta: (1) publicar eventos en `createEcommerce`, (2) cascada de acciones al desactivar ecommerce (inactivar usuarios vinculados y desactivar API Keys), y (3) refactorizar a puertos y adaptadores manteniendo la funcionalidad existente mediante TDD.

### Requerimiento de Negocio
Del archivo `.github/requirements/ecommerce-onboarding.md`:
- Super Admin registra ecommerces (HU-15)
- Al desactivar un ecommerce, usuarios y API Keys vinculadas deben perder acceso inmediatamente
- Cada ecommerce es un tenant aislado
- Cascada: cambio de estado → eventos → propagar a servicios dependientes (Admin y Engine)

### Historias de Usuario

#### HU-15a: Completar publicación de eventos en createEcommerce

```
Como:        Super Admin que automatiza infraestructura
Quiero:      que se publique un evento cuando se crea un ecommerce
Para:        mantener sincronizadas las réplicas en otros servicios (Engine)

Prioridad:   Alta
Estimación:  S
Dependencias: Ninguna
Capa:        Backend
```

#### HU-15b: Implementar cascada de acciones al desactivar ecommerce

```
Como:        Super Admin que desactiva un ecommerce
Quiero:      que se inactiven automáticamente todos sus usuarios y se revoquen sus API Keys
Para:        garantizar que no hay acceso residual a datos o transacciones del ecommerce inactivo

Prioridad:   Alta
Estimación:  M
Dependencias: HU-15a
Capa:        Backend
```

#### HU-15c: Refactorizar Ecommerce a arquitectura hexagonal (Ports & Adapters)

```
Como:        Desarrollador mantenedor
Quiero:      que el módulo Ecommerce siga el patrón hexagonal sin cambiar su funcionalidad
Para:        facilitar futuras extensiones, testabilidad y bajo acoplamiento

Prioridad:   Alta
Estimación:  L
Dependencias: HU-15a, HU-15b
Capa:        Backend
```

---

## 2. CRITERIOS DE ACEPTACIÓN

### HU-15a: Publicar evento en createEcommerce

#### CRITERIO-15a.1: Evento publicado al crear ecommerce exitosamente
```gherkin
Dado que:  soy un Super Admin autenticado
Cuando:    creo un ecommerce con {"name": "Nike Store", "slug": "nike-store"}
Entonces:  el ecommerce se crea con status=ACTIVE
  Y:       se publica evento "ECOMMERCE_CREATED" a RabbitMQ con {uid, name, slug, status, timestamp}
  Y:       se retorna 201 Created con la respuesta del ecommerce
```

#### CRITERIO-15a.2: Manejo de error en publicación de evento
```gherkin
Dado que:  soy un Super Admin autenticado
  Y:       RabbitMQ está indisponible
Cuando:    creo un ecommerce
Entonces:  se retorna 500 Internal Server Error
  Y:       se registra el error en logs
  Y:       la BD rollback la creación del ecommerce (transacción atómica)
```

---

### HU-15b: Cascada de acciones en updateEcommerceStatus

#### CRITERIO-15b.1: Cascada de inactivación al cambiar a INACTIVE
```gherkin
Dado que:  existe un ecommerce ACTIVE con 3 usuarios vinculados y 2 API Keys activas
Cuando:    cambio el status del ecommerce a INACTIVE
Entonces:  el ecommerce cambia a status=INACTIVE
  Y:       todos sus usuarios (3) se inactivan (isActive=false)
  Y:       todas sus API Keys (2) se desactivan (isActive=false)
  Y:       se publica evento "ECOMMERCE_STATUS_CHANGED" con uid, newStatus=INACTIVE, oldStatus=ACTIVE
  Y:       se retorna 200 OK con el ecommerce actualizado
```

#### CRITERIO-15b.2: Validación de ecommerce no encontrado
```gherkin
Dado que:  NO existe un ecommerce con uid=invalid-uuid
Cuando:    intento cambiar su status
Entonces:  se retorna 404 Not Found con mensaje "El ecommerce con uid 'invalid-uuid' no existe"
  Y:       ningún usuario ni API Key se modifica
```

#### CRITERIO-15b.3: Sin cambio de status → sin cascada
```gherkin
Dado que:  existe un ecommerce INACTIVE
Cuando:    intento cambiar su status a INACTIVE (sin cambio)
Entonces:  se retorna 200 OK
  Y:       NO se inactivan usuarios ni API Keys
  Y:       NO se publica evento (no hay cambio)
```

#### CRITERIO-15b.4: Preservar información al inactivar usuarios
```gherkin
Dado que:  existe un usuario con campos {id, username, email, ecommerceId, isActive=true, createdAt, updatedAt}
Cuando:    se inactiva por cascada del ecommerce
Entonces:  todos sus campos se preservan
  Y:       solo isActive cambia a false
  Y:       updatedAt se actualiza al timestamp actual (UTC)
```

---

### HU-15c: Refactorizar a arquitectura hexagonal

#### CRITERIO-15c.1: EcommerceController sin lógica de negocio
```gherkin
Dado que:  he refactorizado a hexagonal
Cuando:    reviso EcommerceController.java
Entonces:  solo hace routing, validación de entrada y mapeo HTTP
  Y:       toda lógica de negocio → EcommerceServiceImpl (implementa puerto)
  Y:       acceso a datos → JpaEcommerceAdapter (implementa puerto persistencia)
  Y:       eventos → RabbitMqEcommerceAdapter (implementa puerto eventos)
```

#### CRITERIO-15c.2: Puertos definidos sin dependencias de frameworks
```gherkin
Dado que:  he definido los puertos
Cuando:    reviso EcommerceUseCase, EcommercePersistencePort, EcommerceEventPort
Entonces:  son interfaces en `application/port/{in,out}`
  Y:       NO tienen imports de Spring, JPA, RabbitMQ ni Jackson
  Y:       solo usan tipos de dominio (DTOs, enums, excepciones)
```

#### CRITERIO-15c.3: Adapters sin lógica de negocio
```gherkin
Dado que:  he refactorizado los adapters
Cuando:    reviso JpaEcommerceAdapter y RabbitMqEcommerceAdapter
Entonces:  solo implementan puertos
  Y:       delegación pura a JPA/RabbitMQ sin lógica adicional
  Y:       mapean entidades ↔ DTOs sin transformaciones complejas
```

#### CRITERIO-15c.4: Tests unitarios pasan (TDD first)
```gherkin
Dado que:  he escrito tests ANTES de refactorizar
Cuando:    ejecuto mvn clean test
Entonces:  todos los tests de EcommerceServiceTest pasan (mocking puertos)
  Y:       todos los tests de EcommerceControllerTest pasan (MockMvc)
  Y:       cobertura ≥ 80% en EcommerceService
```

#### CRITERIO-15c.5: Comportamiento funcional idéntico pre/post refactor
```gherkin
Dado que:  hay tests de integración contra los endpoints
Cuando:    ejecuto contra el código refactorizado
Entonces:  todos los tests pasan sin cambios
  Y:       API contracts son idénticos (request/response)
  Y:       no hay regresiones en lógica de negocio
```

---

## 3. DISEÑO

### Modelo de Datos (Sin cambios — ya existen)

#### Entidades afectadas
| Entidad | Almacén | Cambios | Descripción |
|---------|---------|---------|-------------|
| `EcommerceEntity` | tabla `ecommerce` | Ninguno | Debe tener relación implícita con Users y ApiKeys |
| `UserEntity` | tabla `app_user` | Campos `isActive` ya existen | Se actualiza cascada al inactivar ecommerce |
| `ApiKeyEntity` | tabla `api_key` | Campo `isActive` ya existe | Se desactiva cascada al inactivar ecommerce |

#### Cambios de BD: NINGUNO
> Las tablas ya existen. Solo cambios en lógica de aplicación.

### Puertos (Interfaces sin dependencias de frameworks)

#### Puerto de Entrada: `EcommerceUseCase` (in)
```java
package com.loyalty.service_admin.application.port.in;

public interface EcommerceUseCase {
    EcommerceResponse createEcommerce(EcommerceCreateRequest request);
    EcommerceResponse updateEcommerceStatus(UUID uid, EcommerceUpdateStatusRequest request);
    EcommerceResponse getEcommerceById(UUID uid);
    Page<EcommerceResponse> listEcommerces(String status, int page, int size);
    void validateEcommerceExists(UUID ecommerceId);
}
```

#### Puerto de Salida: Persistencia (`EcommercePersistencePort`)
```java
package com.loyalty.service_admin.application.port.out;

public interface EcommercePersistencePort {
    EcommerceEntity save(EcommerceEntity entity);
    Optional<EcommerceEntity> findById(UUID id);
    Page<EcommerceEntity> findAll(Specification<EcommerceEntity> spec, Pageable pageable);
    boolean existsBySlug(String slug);
    boolean existsById(UUID id);
    
    // Cascada de acciones
    List<UserEntity> findUsersByEcommerceId(UUID ecommerceId);
    void inactivateUsers(List<UserEntity> users);
    List<ApiKeyEntity> findApiKeysByEcommerceId(UUID ecommerceId);
    void deactivateApiKeys(List<ApiKeyEntity> keys);
}
```

#### Puerto de Salida: Eventos (`EcommerceEventPort`)
```java
package com.loyalty.service_admin.application.port.out;

public interface EcommerceEventPort {
    void publishEcommerceCreated(UUID ecommerceId, String name, String slug, String status);
    void publishEcommerceStatusChanged(UUID ecommerceId, String newStatus, String oldStatus);
}
```

### Adaptadores (Implementaciones concretas)

#### Adapter Persistencia: `JpaEcommerceAdapter`
```java
package com.loyalty.service_admin.infrastructure.persistence.jpa;

@Service
public class JpaEcommerceAdapter implements EcommercePersistencePort {
    private final EcommerceRepository ecommerceRepository;
    private final UserRepository userRepository;
    private final ApiKeyRepository apiKeyRepository;
    
    // Implementar métodos del puerto
    @Override
    public EcommerceEntity save(EcommerceEntity entity) {
        return ecommerceRepository.save(entity);
    }
    
    @Override
    public List<UserEntity> findUsersByEcommerceId(UUID ecommerceId) {
        return userRepository.findByEcommerceId(ecommerceId);
    }
    
    @Override
    public void inactivateUsers(List<UserEntity> users) {
        users.forEach(u -> u.setIsActive(false));
        userRepository.saveAll(users);
        // Log
    }
    
    // ... resto de métodos
}
```

#### Adapter Eventos: `RabbitMqEcommerceAdapter`
```java
package com.loyalty.service_admin.infrastructure.messaging.rabbitmq;

@Service
public class RabbitMqEcommerceAdapter implements EcommerceEventPort {
    private final RabbitTemplate rabbitTemplate;
    private final EcommerceEventPublisher eventPublisher; // O implementar acá
    
    @Override
    public void publishEcommerceCreated(UUID ecommerceId, String name, String slug, String status) {
        // Publicar a "ecommerce.created" exchange
    }
    
    @Override
    public void publishEcommerceStatusChanged(UUID ecommerceId, String newStatus, String oldStatus) {
        // Publicar a "ecommerce.status-changed" exchange
    }
}
```

### Servicio (Implementación de Use Case)

#### `EcommerceServiceImpl` (implementa `EcommerceUseCase`)
```java
package com.loyalty.service_admin.application.service;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EcommerceServiceImpl implements EcommerceUseCase {
    
    private final EcommercePersistencePort persistencePort;
    private final EcommerceEventPort eventPort;
    
    @Override
    public EcommerceResponse createEcommerce(EcommerceCreateRequest request) {
        // 1. Validar slug duplicado
        // 2. Crear entidad con status=ACTIVE
        // 3. Persistir
        // 4. Publicar evento "ECOMMERCE_CREATED" ← NUEVO
        // 5. Retornar DTO
    }
    
    @Override
    @Transactional
    public EcommerceResponse updateEcommerceStatus(UUID uid, EcommerceUpdateStatusRequest request) {
        // 1. Obtener ecommerce por uid (validar existe)
        // 2. Cambiar status
        // 3. Si status → INACTIVE: ← NUEVO
        //    a. Obtener usuarios vinculados
        //    b. Inactivar todos los usuarios
        //    c. Obtener API Keys vinculadas
        //    d. Desactivar todas las API Keys
        // 4. Persistir ecommerce
        // 5. Publicar evento "ECOMMERCE_STATUS_CHANGED"
        // 6. Retornar DTO
    }
    
    @Override
    @Transactional(readOnly = true)
    public void validateEcommerceExists(UUID ecommerceId) {
        if (!persistencePort.existsById(ecommerceId)) {
            throw new EcommerceNotFoundException(...);
        }
    }
}
```

---

## 4. ESTRUCTURA DE CARPETAS POST-REFACTOR

```
backend/service-admin/src/main/java/com/loyalty/service_admin/
├── presentation/
│   └── controller/
│       └── EcommerceController.java        # ← SIN cambios en firma, solo delegación a puerto
│
├── application/
│   ├── port/
│   │   ├── in/
│   │   │   └── EcommerceUseCase.java       # ← NUEVO (interfaz)
│   │   └── out/
│   │       ├── EcommercePersistencePort.java  # ← NUEVO (interfaz)
│   │       └── EcommerceEventPort.java        # ← NUEVO (interfaz)
│   ├── service/
│   │   └── EcommerceService.java        # ← REFACTORIZADO (implementa puerto)
│   └── dto/ecommerce/
│       ├── EcommerceCreateRequest.java     # ← SIN cambios
│       ├── EcommerceUpdateStatusRequest.java # ← SIN cambios
│       └── EcommerceResponse.java          # ← SIN cambios
│
├── domain/
│   ├── entity/
│   │   ├── EcommerceEntity.java            # ← SIN cambios
│   │   ├── UserEntity.java                 # ← SIN cambios
│   │   └── ApiKeyEntity.java               # ← SIN cambios
│   ├── model/
│   │   └── ecommerce/
│   │       └── EcommerceStatus.java        # ← SIN cambios
│   └── repository/
│       ├── EcommerceRepository.java        # ← SIN cambios
│       ├── UserRepository.java             # ← SIN cambios
│       └── ApiKeyRepository.java           # ← SIN cambios
│
└── infrastructure/
    ├── persistence/
    │   └── jpa/
    │       └── JpaEcommerceAdapter.java     # ← NUEVO (implementa puerto persistencia)
    ├── messaging/
    │   └── rabbitmq/
    │       ├── EcommerceEventPublisher.java # ← REFACTORIZADO (usa port)
    │       └── RabbitMqEcommerceAdapter.java # ← NUEVO (implementa puerto eventos)
    └── exception/
        └── EcommerceNotFoundException.java  # ← SIN cambios
```

---

## 5. REGLAS DE NEGOCIO Y CONSTRAINTS

### Cascada de Acciones
1. **Atomicidad**: Si falla inactivar usuarios, NO se cambia status del ecommerce (rollback).
2. **Orden**: Validar ecommerce → Cambiar status → Cascada (usuarios + API Keys) → Eventos.
3. **Usuarios vinculados**: Solo usuarios con `ecommerceId = uid` se inactivan.
4. **API Keys vinculadas**: Solo API Keys con `ecommerceId = uid` se desactivan.
5. **Preservación de datos**: No se eliminan registros, solo se marcan como inactivos.

### Eventos
1. **Sincronización**: Eventos se publican DESPUÉS de persistir cambios (garantizar consistencia eventual).
2. **Formato**: JSON con timestamp UTC, uid, status antiguo, status nuevo.
3. **Tolerancia a fallos**: Si RabbitMQ falla, rollback la transacción (falla transaccional antes que eventual).
4. **Idempotencia**: Listeners en Engine deben ser idempotentes (repetir no causa duplicados).

### Autorización
1. Solo `SUPER_ADMIN` puede crear ecommerces.
2. Solo `SUPER_ADMIN` puede cambiar status de ecommerces.
3. `ADMIN` de un ecommerce puede leer su propio ecommerce (sin cambiar status).

---

## 6. NOTAS DE IMPLEMENTACIÓN

### Estrategia TDD — Orden de trabajo
1. **Fase 1: Tests → Implementación completiva** (HU-15a, HU-15b)
   - Escribir tests unitarios de `EcommerceServiceTest` (mocking puertos) ← PRIMERO
   - Escribir tests de `EcommerceControllerTest` (MockMvc) ← PRIMERO
   - Implementar métodos en `EcommerceService` para pasar tests
   - Implementar `EcommerceEventPublisher` y cascada de acciones

2. **Fase 2: Refactor a hexagonal** (HU-15c)
   - Extraer interfaces de puertos (`EcommerceUseCase`, `EcommercePersistencePort`, `EcommerceEventPort`)
   - Reescribir `EcommerceService` → `EcommerceServiceImpl` (implementa puerto)
   - Crear `JpaEcommerceAdapter` y `RabbitMqEcommerceAdapter`
   - Refactorizar controller para inyectar puerto (no servicio directo)
   - Ejecutar tests existentes (deben pasar sin cambios)

3. **Fase 3: Validación**
   - Verificar cobertura ≥ 80% en `EcommerceService`
   - Ejecutar tests de integración contra endpoints
   - Validar publicación de eventos en RabbitMQ con testcontainers

### Dependencias
- **Actual**: `EcommerceService` inyecta `EcommerceRepository`, `EcommerceEventPublisher`
- **Post-refactor**: `EcommerceServiceImpl` inyecta `EcommercePersistencePort`, `EcommerceEventPort`
- **Backward compat**: Controller sigue inyectando el servicio (implementa puerto)

### Cambios en método signature
```java
// ANTES (actual)
public EcommerceResponse updateEcommerceStatus(UUID uid, EcommerceUpdateStatusRequest request) {
    // Solo cambio de status + evento
}

// DESPUÉS (refactorizado)
public EcommerceResponse updateEcommerceStatus(UUID uid, EcommerceUpdateStatusRequest request) {
    // IGUAL firma
    // + cascada de acciones (usuarios + API Keys)
    // + lógica delegada al puerto de persistencia
}
```

### Transaccionalidad
- `EcommerceService.updateEcommerceStatus()` marcada `@Transactional` (fallida = rollback completo)
- `JpaEcommerceAdapter.inactivateUsers()` y `deactivateApiKeys()` se ejecutan en la misma tx
- Eventos publicados DESPUÉS de commit (usar `@TransactionalEventListener` o delay en broker)

### Mapeo de DTOs
- Mantener DTOs existentes adentro del `application/dto/ecommerce/`
- No crear nuevos DTOs innecesarios
- Mapeo ↔ entre DTOs y entidades ocurre en service

### Testing
- Mockito para mock de puertos en `EcommerceServiceTest`
- MockMvc para tests de controller
- TestContainers para validación de RabbitMQ (en suite de integración, no unitaria)

---

## 7. LISTA DE TAREAS

> Checklist ASDD para Orchestrator y agentes. Marcar cada ítem (`[x]`) al completarlo.

### Backend — Fase 1: Completar funcionalidad (HU-15a, HU-15b)

#### Tests Unitarios (TDD — escribir PRIMERO)
- [ ] `EcommerceServiceTest.testCreateEcommerce_success` — crear + validar slug único
- [ ] `EcommerceServiceTest.testCreateEcommerce_publishesEvent` — evento ECOMMERCE_CREATED publicado
- [ ] `EcommerceServiceTest.testCreateEcommerce_eventPublishFailure_rollsBack` — rollback si RabbitMQ falla
- [ ] `EcommerceServiceTest.testUpdateEcommerceStatus_success` — cambio de status exitoso
- [ ] `EcommerceServiceTest.testUpdateEcommerceStatus_cascadeInactivateUsers` — usuarios se inactivan
- [ ] `EcommerceServiceTest.testUpdateEcommerceStatus_cascadeDeactivateApiKeys` — API Keys se desactivan
- [ ] `EcommerceServiceTest.testUpdateEcommerceStatus_noStatusChangeNoEvent` — sin cambio, sin evento
- [ ] `EcommerceServiceTest.testUpdateEcommerceStatus_notFound` — 404 si no existe
- [ ] `EcommerceControllerTest.testCreateEcommerce_returns201` — controller crea exitosamente
- [ ] `EcommerceControllerTest.testCreateEcommerce_returns409_slugDuplicate` — 409 si slug existe
- [ ] `EcommerceControllerTest.testUpdateEcommerceStatus_returns200` — controller actualiza
- [ ] `EcommerceControllerTest.testUpdateEcommerceStatus_returns401_noAuth` — sin token = 401
- [ ] `EcommerceControllerTest.testUpdateEcommerceStatus_returns403_notSuperAdmin` — no SUPER_ADMIN = 403

#### Implementación de funcionalidad
- [ ] Refactorizar `EcommerceService.createEcommerce()` → publicar evento "ECOMMERCE_CREATED"
- [ ] Implementar cascada en `EcommerceService.updateEcommerceStatus()`:
  - [ ] Obtener usuarios por `ecommerceId`
  - [ ] Inactivar todos los usuarios (`isActive = false`)
  - [ ] Obtener API Keys por `ecommerceId`
  - [ ] Desactivar todas las API Keys (`isActive = false`)
- [ ] Validar transaccionalidad = fallida Si cascada → rollback completo
- [ ] Actualizar `EcommerceEventPublisher` para soportar evento `ECOMMERCE_CREATED`
- [ ] Loguear todos los cambios (usuarios inactivados, API Keys desactivadas)

---

### Backend — Fase 2: Refactor a Hexagonal (HU-15c)

#### Definir Puertos
- [ ] Crear `application/port/in/EcommerceUseCase.java` (interface)
- [ ] Crear `application/port/out/EcommercePersistencePort.java` (interface cascada incluida)
- [ ] Crear `application/port/out/EcommerceEventPort.java` (interface)

#### Crear Adaptadores
- [ ] Crear `infrastructure/persistence/jpa/JpaEcommerceAdapter.java` (implementa `EcommercePersistencePort`)
  - [ ] Métodos CRUD delegados a repositories
  - [ ] Métodos de cascada: `findUsersByEcommerceId`, `inactivateUsers`, `findApiKeysByEcommerceId`, `deactivateApiKeys`
- [ ] Crear `infrastructure/messaging/rabbitmq/RabbitMqEcommerceAdapter.java` (implementa `EcommerceEventPort`)
  - [ ] Delegación pura a `EcommerceEventPublisher`

#### Refactorizar Service
- [ ] Renombrar `EcommerceService` → `EcommerceServiceImpl`
- [ ] Hacer que implemente `EcommerceUseCase`
- [ ] Cambiar inyecciones: `EcommercePersistencePort`, `EcommerceEventPort` (no repos ni publishers)
- [ ] **SIN cambios en lógica** — solo delegación a puertos
- [ ] Ejecutar tests existentes (deben pasar sin módficaciones)

#### Refactorizar Controller
- [ ] Cambiar inyección: `EcommerceUseCase` en lugar de `EcommerceService` (por interface)
- [ ] **SIN cambios en endpoints** — solo delegación
- [ ] Validar que MockMvc tests pasan sin cambios

#### Validación de Hexagonal
- [ ] `EcommerceUseCase`, `EcommercePersistencePort`, `EcommerceEventPort` = sin imports Spring/JPA/RabbitMQ
- [ ] Adaptadores solo implementan puertos sin lógica adicional
- [ ] Servicio solo contiene lógica de dominio (sin detalles de BD o mensajería)

---

### Tests — Toda la suite
- [ ] Ejecutar `mvn clean test` → todos pasan
- [ ] Cobertura ≥ 80% en `EcommerceService` / `EcommerceServiceImpl`
- [ ] Cobertura ≥ 70% en `EcommerceController`
- [ ] SIN tests de integración con BD real (usar H2 en memoria con `@DataJpaTest`)
- [ ] SIN tests de integración con RabbitMQ real en unit suite

---

### QA
- [ ] Ejecutar `/gherkin-case-generator` → generar casos Gherkin para CRITERIO-15a.1, 15b.1, 15c.4
- [ ] Ejecutar `/risk-identifier` → clasificar riesgos (cascada + eventos = Alto)
- [ ] Validar que todos los criterios de aceptación están cubiertos por tests
- [ ] Actualizar estado spec → `status: APPROVED` (usuario aprueba antes de implementar)

---

## 8. DEPENDENCIAS Y RIESGOS

### Dependencias
- Hexagonal architecture guidelines en `.github/requirements/hexagonal-architecture.md` (ya definidas)
- `EcommerceEventPublisher` debe existir y soportar "ECOMMERCE_CREATED"
- `UserRepository` y `UserEntity` deben tener soporte para inactivar en lote
- `ApiKeyRepository` y `ApiKeyEntity` deben tener soporte para desactivar en lote

### Riesgos
| Riesgo | Nivel | Mitigación |
|--------|-------|-----------|
| Cascada de inactivación falla → datos inconsistentes | Alto | Transacción atómica con rollback en excepción |
| Evento no publicado → BD cambia pero Engine desincronizado | Medio | Usar `@TransactionalEventListener` post-commit |
| Refactor introduce regresiones | Medio | TDD + tests existentes deben pasar sin cambios |
| Rendimiento: cascada N usuarios + M API Keys | Bajo | Usar bulk update en JPA (`@Modifying` + JPQL) si escala |

---

## 9. ACEPTACIÓN DE SPEC

| Rol | Estado | Comentarios |
|-----|--------|-----------|
| Spec Generator | ✅ DRAFT | Especificación completa lista para revisión |
| Product Owner | ⏳ PENDING | Aprobar criterios de aceptación |
| Tech Lead | ⏳ PENDING | Validar arquitectura hexagonal vs estándares |
| Developer Lead | ⏳ PENDING | Confirmar estimaciones (S + M + L = ~4 sprints) |

**Siguiente paso:** Usuario aprueba spec con `status: APPROVED` → Orchestrator inicia implementación.

