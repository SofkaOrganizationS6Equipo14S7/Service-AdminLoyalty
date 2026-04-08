---
id: SPEC-100
status: APPROVED
feature: hexagonal-architecture-refactoring
created: 2026-04-08
updated: 2026-04-08
author: spec-generator
version: "1.0"
related-specs:
  - HU-06-seasonal-rules
  - HU-07-product-rules
  - HU-08-fidelity-ranges
  - HU-09-discount-limits
  - HU-10-loyalty-tiers
  - HU-14-rule-activation
---

# Spec: Refactoring de 6 Features a Arquitectura Hexagonal (Ports & Adapters)

> **Estado:** `DRAFT` → Aprobado con `status: APPROVED` antes de iniciar implementación.  
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED

> **Propósito:** Migrar 6 features completamente implementados (RuleService, CustomerTierService, DiscountConfigService, etc.) de una arquitectura monolítica a Hexagonal Architecture, reduciendo acoplamiento, mejorando testabilidad sin dependencias de Spring Security, y estableciendo patrones de DI basados en puertos.

---

## 1. REQUERIMIENTOS

### Descripción

El `service-admin` actualmente implementa 6 features con patrones arquitectónicos mixtos. Los controllers inyectan servicios **concretos** que implementan lógica de dominio acoplada directamente a repositorios JPA. El objetivo es refactorizar estas 6 features siguiendo **Arquitectura Hexagonal (Ports & Adapters)** donde:

- Controllers inyectarán **puertos de entrada** (interfaces UseCase) en lugar de servicios concretos.
- Services implementarán puertos y dependerán de **puertos de salida** (interfaces) para persistencia y eventos, no de implementaciones concretas.
- Adapters implementarán puertos de salida, desacoplando la lógica de dominio de frameworks (JPA, RabbitMQ).
- La estrategia TDD asegura que tests unitarios de servicios sean completamente independientes de Spring Security.

### Requerimiento de Negocio

Desde `.github/requirements/hexagonal-architecture.md`: "Establecer el modelo estándar para implementar cualquier feature del microservicio service-admin siguiendo **Hexagonal Architecture (Ports & Adapters)** de manera agnóstica, garantizando consistencia y bajo acoplamiento."

---

## 2. HISTORIAS DE USUARIO AFECTADAS

### HU-06: Seasonal Rules

```
Como:        Backend Developer
Quiero:      Migrar RuleService (seasonal) a arquitectura hexagonal
Para:        Reducir acoplamiento y mejorar testing de lógica de dominio

Prioridad:   Alta
Estimación:  M
Dependencias: Puertos de RuleService
Capa:        Backend (Service Architecture)
```

#### Criterios de Aceptación — HU-06

**Cambio Arquitectónico:**
```gherkin
CRITERIO-6.1: [Seasonal Rule UseCase Interface]
  Dado que:  Existe RuleController inyectando RuleService concreto
  Cuando:    Se migra a: Controller inyecta RuleUseCase (interfaz)
  Entonces:  RuleUseCase define: createSeasonalRule(), updateSeasonalRule(), deleteSeasonalRule(), findSeasonalRuleById()
  
CRITERIO-6.2: [Seasonal Rule Service Conforma Puerto]
  Dado que:  RuleService implementa lógica de validación y persistencia
  Cuando:    Se refactoriza: service inyecta RulePersistencePort (interfaz)
  Entonces:  Service NO depende de RuleRepository concreto
  Y:        Service NO depende de RabbitMQ concreto (inyecta RuleEventPort)

CRITERIO-6.3: [Tests Unitarios sin Spring Security]
  Dado que:  Se escriben tests con Mockito de puertos
  Cuando:    Se ejecuta: mvn clean test
  Entonces:  Tests unitarios de RuleService pasan sin dependency injection de Authentication
```

---

### HU-07: Product Rules

```
Como:        Backend Developer
Quiero:      Migrar ProductRuleService a arquitectura hexagonal
Para:        Aislar lógica de negocio de persistencia concreta

Prioridad:   Alta
Estimación:  M
Dependencias: HU-06 (patrón base establecido)
Capa:        Backend (Service Architecture)
```

#### Criterios de Aceptación — HU-07

**Cambio Arquitectónico:**
```gherkin
CRITERIO-7.1: [Product Rule UseCase]
  Dado que:  No existe interfaz ProductRuleUseCase
  Cuando:    Se crea: application/port/in/ProductRuleUseCase.java
  Entonces:  Define: createProductRule(), updateProductRule(), deleteProductRule()
  
CRITERIO-7.2: [Product Rule Persistence Port]
  Dado que:  RuleService inyecta ProductRuleRepository directamente
  Cuando:    Se crea: application/port/out/ProductRulePersistencePort.java
  Entonces:  RuleService inyecta SOLO ProductRulePersistencePort (interfaz)
```

---

### HU-08: Fidelity Ranges

```
Como:        Backend Developer
Quiero:      Migrar FidelityRangeService a arquitectura hexagonal
Para:        Consistencia arquitectónica con outras features

Prioridad:   Alta
Estimación:  M
Dependencias: HU-06, HU-07
Capa:        Backend (Service Architecture)
```

---

### HU-09: Discount Limits

```
Como:        Backend Developer
Quiero:      Migrar DiscountConfigService a arquitectura hexagonal
Para:        Reducir acoplamiento y mejorar testabilidad

Prioridad:   Alta
Estimación:  M
Dependencias: HU-06, HU-07, HU-08
Capa:        Backend (Service Architecture)
```

---

### HU-10: Loyalty Tiers (Classification)

```
Como:        Backend Developer
Quiero:      Migrar CustomerTierService a arquitectura hexagonal
Para:        Establecer patrón consistente para servicios de clasificación

Prioridad:   Alta
Estimación:  M
Dependencias: HU-06, HU-07, HU-08, HU-09
Capa:        Backend (Service Architecture)
```

---

### HU-14: Rule Activation

```
Como:        Backend Developer
Quiero:      Migrar lógica de activación/desactivación de reglas a hexagonal
Para:        Completar refactoring de todas operaciones sobre Rule

Prioridad:   Alta
Estimación:  S
Dependencias: HU-06, HU-07
Capa:        Backend (Service Architecture)
```

---

## 2. DISEÑO

### Modelo Arquitectónico General

Cada feature seguirá la estructura hexagonal del requerimiento `.github/requirements/hexagonal-architecture.md`:

```
src/main/java/com/loyalty/service_admin/
├── application/
│   ├── dto/<feature>/
│   │   ├── <Feature>Request.java       # DTO de entrada HTTP
│   │   ├── <Feature>Response.java      # DTO de salida HTTP
│   │   └── <Feature>EventPayload.java  # DTO para eventos RabbitMQ
│   ├── port/
│   │   ├── in/
│   │   │   └── <Feature>UseCase.java   # ✅ INTERFAZ - Casos de uso
│   │   └── out/
│   │       ├── <Feature>PersistencePort.java  # ✅ INTERFAZ - Acceso a datos
│   │       └── <Feature>EventPort.java        # ✅ INTERFAZ - Mensajería asíncrona
│   ├── service/
│   │   └── <Feature>ServiceImpl.java    # Implementación UseCase + lógica negocio
│   └── usecase/ (opcional, alternativa)
│       └── <Feature>UseCaseImpl.java    # Si prefieres separar service del usecase
│
├── domain/
│   ├── entity/
│   │   └── <Feature>Entity.java        # Entidad JPA (mapeo BD)
│   ├── model/
│   │   └── <Feature>.java              # Modelo dominio (puro, sin anotaciones)
│   └── repository/
│       └── <Feature>Repository.java    # ✅ INTERFAZ JPA Spring Data
│
├── infrastructure/
│   ├── persistence/
│   │   └── jpa/
│   │       └── Jpa<Feature>Adapter.java     # ✅ ADAPTER - implementa PersistencePort
│   ├── messaging/
│   │   └── <Feature>EventAdapter.java       # ✅ ADAPTER - implementa EventPort
│   └── rabbitmq/
│       └── <Feature>EventPublisher.java     # RabbitMQ concreto
│
└── presentation/
    └── controller/
        └── <Feature>Controller.java    # ✅ Inyecta UseCase (interfaz)
```

---

### Patrones de Inyección de Dependencias (DI)

#### ❌ ANTES (Anti-patrón actual)

```java
// RuleController.java
@RestController
public class RuleController {
    private final RuleService ruleService;  // ❌ Concreto
    // ...
}

// RuleService.java
@Service
public class RuleService {
    private final RuleRepository repository;  // ❌ JPA concreto
    private final RabbitTemplate rabbitTemplate;  // ❌ RabbitMQ concreto
    // ...
}
```

#### ✅ DESPUÉS (Patrón hexagonal)

```java
// RuleController.java
@RestController
public class RuleController {
    private final RuleUseCase ruleUseCase;  // ✅ Interfaz
    // ...
}

// RuleServiceImpl.java (implementa RuleUseCase)
@Service
public class RuleServiceImpl implements RuleUseCase {
    private final RulePersistencePort persistencePort;  // ✅ Interfaz
    private final RuleEventPort eventPort;  // ✅ Interfaz
    // ...
}

// JpaRuleAdapter.java (implementa RulePersistencePort)
@Component
public class JpaRuleAdapter implements RulePersistencePort {
    private final RuleRepository repository;  // JPA concreto (aceptable en adapter)
    // ...
}

// RuleEventAdapter.java (implementa RuleEventPort)
@Component
public class RuleEventAdapter implements RuleEventPort {
    private final RabbitTemplate rabbitTemplate;  // RabbitMQ concreto (aceptable)
    // ...
}
```

---

### Features a Refactorizar — Módulos de Salida

#### 1. **RuleService** → RuleUseCase (HU-06, HU-07, HU-14)

**Puertos a crear:**

| Puerto | Interfaz | Métodos Principales | Responsabilidad |
|--------|----------|---------------------|-----------------|
| **Entrada** | `RuleUseCase` | `createRule()`, `updateRule()`, `deleteRule()`, `getRuleById()`, `listRules()`, `activateRule()`, `deactivateRule()` | Orquestar creación/edición/eliminación de reglas (temporada, producto, etc.) |
| **Salida-Persistencia** | `RulePersistencePort` | `saveRule()`, `findRuleById()`, `findRulesByEcommerce()`, `deleteRule()`, `findRulesByStatus()` | Acceso a BD (abstraer JPA) |
| **Salida-Eventos** | `RuleEventPort` | `publishRuleCreated()`, `publishRuleUpdated()`, `publishRuleDeleted()`, `publishRuleActivated()`, `publishRuleDeactivated()` | Publicar eventos a RabbitMQ |

**Adapters:**

| Adapter | Implementa | Recurso Concreto |
|---------|-----------|-----------------|
| `JpaRuleAdapter` | `RulePersistencePort` | `RuleRepository` (JPA Spring Data) |
| `RuleEventAdapter` | `RuleEventPort` | `RabbitTemplate` (Spring AMQP) |

**Tests Asociados:**

- `RuleServiceImplTest`: Unitarios con Mockito (mock puertos, sin Spring)
- `JpaRuleAdapterTest`: Integración con BD (TestContainers + H2)
- `RuleEventAdapterTest`: Integración con RabbitMQ (mock broker)

---

#### 2. **CustomerTierService** → CustomerTierUseCase (HU-10)

**Puertos a crear:**

| Puerto | Interfaz | Métodos Principales |
|--------|----------|---------------------|
| **Entrada** | `CustomerTierUseCase` | `createTier()`, `updateTier()`, `deleteTier()`, `getTierById()`, `listTiers()` |
| **Salida-Persistencia** | `CustomerTierPersistencePort` | `saveTier()`, `findTierById()`, `findByHierarchyLevel()`, `deleteTier()` |
| **Salida-Eventos** | `CustomerTierEventPort` | `publishTierCreated()`, `publishTierUpdated()`, `publishTierDeleted()` |

---

#### 3. **DiscountConfigService** → DiscountConfigUseCase (HU-09)

**Puertos a crear:**

| Puerto | Interfaz | Métodos Principales |
|--------|----------|---------------------|
| **Entrada** | `DiscountConfigUseCase` | `createConfiguration()`, `updateConfiguration()`, `getConfiguration()`, `validateLimits()` |
| **Salida-Persistencia** | `DiscountConfigPersistencePort` | `saveConfig()`, `findConfigByEcommerce()`, `findDiscountLimits()` |
| **Salida-Eventos** | `DiscountConfigEventPort` | `publishConfigUpdated()`, `publishLimitBreachAlert()` |

---

#### 4. **FidelityRangeService** → FidelityRangeUseCase (HU-08)

**Puertos a crear:**

| Puerto | Interfaz | Métodos Principales |
|--------|----------|---------------------|
| **Entrada** | `FidelityRangeUseCase` | `defineFidelityRanges()`, `validateRanges()`, `updateRanges()`, `getRanges()` |
| **Salida-Persistencia** | `FidelityRangePersistencePort` | `saveRange()`, `findRangeByLevel()`, `findAllRanges()`, `deleteRange()` |
| **Salida-Eventos** | `FidelityRangeEventPort` | `publishRangeUpdated()` |

---

#### 5. Ejemplos de Código — Feature ProductRule

**5.1. Puerto de Entrada (`application/port/in/ProductRuleUseCase.java`)**

```java
public interface ProductRuleUseCase {
    ProductRuleResponse createProductRule(ProductRuleRequest request);
    ProductRuleResponse updateProductRule(UUID ruleId, ProductRuleRequest request);
    void deleteProductRule(UUID ruleId);
    ProductRuleResponse getProductRuleById(UUID ruleId);
    Page<ProductRuleResponse> listProductRules(Pageable pageable);
}
```

**5.2. Puerto de Salida - Persistencia (`application/port/out/ProductRulePersistencePort.java`)**

```java
public interface ProductRulePersistencePort {
    ProductRuleEntity saveProductRule(ProductRuleEntity entity);
    Optional<ProductRuleEntity> findProductRuleById(UUID ruleId);
    Page<ProductRuleEntity> findProductRulesByEcommerce(UUID ecommerceId, Pageable pageable);
    void deleteProductRule(ProductRuleEntity entity);
    boolean existsProductRuleByProductType(String productType, UUID ecommerceId);
}
```

**5.3. Puerto de Salida - Eventos (`application/port/out/ProductRuleEventPort.java`)**

```java
public interface ProductRuleEventPort {
    void publishProductRuleCreated(ProductRuleEventPayload event);
    void publishProductRuleUpdated(ProductRuleEventPayload event);
    void publishProductRuleDeleted(ProductRuleEventPayload event);
}
```

**5.4. Implementación del Use Case (`application/service/ProductRuleServiceImpl.java`)**

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductRuleServiceImpl implements ProductRuleUseCase {
    
    private final ProductRulePersistencePort persistencePort;  // ✅ Puerto (interfaz)
    private final ProductRuleEventPort eventPort;              // ✅ Puerto (interfaz)
    
    @Override
    public ProductRuleResponse createProductRule(ProductRuleRequest request) {
        // 1. Validar: no exista regla para ese productType
        if (persistencePort.existsProductRuleByProductType(request.productType(), request.ecommerceId())) {
            throw new ProductRuleConflictException("Product rule already exists for this type");
        }
        
        // 2. Crear entidad de dominio
        ProductRuleEntity entity = ProductRuleEntity.builder()
            .productType(request.productType())
            .discountPercentage(request.discountPercentage())
            .active(true)
            .build();
        
        // 3. Persistir a través del puerto
        ProductRuleEntity saved = persistencePort.saveProductRule(entity);
        
        // 4. Publicar evento a través del puerto
        eventPort.publishProductRuleCreated(
            ProductRuleEventPayload.from(saved)
        );
        
        return ProductRuleResponse.from(saved);
    }
    
    // ... updateProductRule(), deleteProductRule(), etc.
}
```

**5.5. Adapter de Persistencia (`infrastructure/persistence/jpa/JpaProductRuleAdapter.java`)**

```java
@Component
@RequiredArgsConstructor
public class JpaProductRuleAdapter implements ProductRulePersistencePort {
    
    private final ProductRuleRepository repository;  // JPA (aceptable aquí)
    
    @Override
    public ProductRuleEntity saveProductRule(ProductRuleEntity entity) {
        return repository.save(entity);
    }
    
    @Override
    public Optional<ProductRuleEntity> findProductRuleById(UUID ruleId) {
        return repository.findById(ruleId);
    }
    
    // ... más métodos delegando al repository
}
```

**5.6. Adapter de Eventos (`infrastructure/messaging/ProductRuleEventAdapter.java`)**

```java
@Component
@RequiredArgsConstructor
public class ProductRuleEventAdapter implements ProductRuleEventPort {
    
    private final ProductRuleEventPublisher publisher;  // RabbitMQ concreto
    
    @Override
    public void publishProductRuleCreated(ProductRuleEventPayload event) {
        publisher.publishToExchange("product.rule.events", 
            "rule.created", event);
    }
    
    // ... más métodos
}
```

**5.7. Controller Refactorizado (`presentation/controller/RuleController.java`)**

```java
@RestController
@RequestMapping("/api/v1/rules")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class RuleController {
    
    private final RuleUseCase ruleUseCase;  // ✅ Puerto (interfaz)
    private final SecurityContextHelper securityContextHelper;
    
    @PostMapping
    public ResponseEntity<RuleResponse> createRule(
            @Valid @RequestBody RuleCreateRequest request) {
        UUID ecommerceId = securityContextHelper.getCurrentUserEcommerceId();
        RuleResponse response = ruleUseCase.createRule(ecommerceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    // ... otros endpoints
}
```

---

### Cambios Incrementales: Qué Se Modifica y Qué Se Mantiene

#### ✅ Mantener Intacto

- ✅ **Endpoints REST** — URLs no cambian (`/api/v1/rules`, `/api/v1/customer-tiers`, etc.)
- ✅ **DTOs de presentación** — `Request` y `Response` no cambian totalmente (solo agregar si es necesario)
- ✅ **Entidades JPA** — `@Entity`, tablas, mappings permanecen igual
- ✅ **Repositorios** — `JpaRepository<T>` interfaces se mantienen (se mueven a `domain/repository/`)
- ✅ **Lógica de negocio** — Misma validación, mismas reglas (solo reorganizada)
- ✅ **Base de datos** — No hay migrations; mismas tablas, mismo schema
- ✅ **RabbitMQ topics/exchanges** — Mismos eventos publicados

#### 🔄 Cambiar/Refactorizar

- 🔄 **Controllers** — Cambiar DI: inyectar `UseCase` en lugar de `ServiceImpl`
- 🔄 **Services** — Crear interfaces (`UseCase`), cambiar `@Service` por `@Component` si es necesario o mantener, pero inyectar puertos
- 🔄 **DI de puertos** — Agregar `@Autowired` (o constructor) de `PersistencePort`, `EventPort`
- 🔄 **Estructura de carpetas** — Agregar `application/port/in/`, `application/port/out/`, `infrastructure/`

#### ⚠️ Notas Delicadas

- **Spring Security**: Controllers mantendrán `@PreAuthorize()` y `SecurityContextHelper`. Tests unitarios de services NO testearán controllers (por eso excluimos controller tests).
- **Tests**: Los servicios tendrán tests unitarios con **Mockito** (100% testeable sin Spring). Controllers tendrán tests de integración con Spring Boot Test (si se requiere), pero NO serán obligatorios.

---

## 3. LISTA DE TAREAS

> Checklist accionable para todos los agentes. Marcar cada ítem (`[x]`) al completarlo.

---

### Fase 1: Preparación y Creación de Puertos

#### Backend Architecture Setup

- [ ] Crear estructura de carpetas:
  - [ ] `application/port/in/` para interfaces UseCase
  - [ ] `application/port/out/` para interfaces de puertos de salida
  - [ ] `infrastructure/persistence/jpa/` para adapters JPA
  - [ ] `infrastructure/messaging/` para adapters de eventos

#### Crear Puertos para HU-06 (Seasonal Rules)

- [ ] [application/port/in/RuleUseCase.java](application/port/in/RuleUseCase.java) — Define interface para seasonal rules
  - [ ] Métodos: `createSeasonalRule()`, `updateSeasonalRule()`, `deleteSeasonalRule()`, `getSeasonalRuleById()`, `listSeasonalRules()`
  - [ ] Cada método retorna `RuleResponse` y recibe `RuleRequest`

- [ ] [application/port/out/RulePersistencePort.java](application/port/out/RulePersistencePort.java)
  - [ ] Métodos: `saveRule()`, `findRuleById()`, `findRulesByEcommerce()`, `deleteRule()`, `findRulesByType(String type)`

- [ ] [application/port/out/RuleEventPort.java](application/port/out/RuleEventPort.java)
  - [ ] Métodos: `publishRuleCreated()`, `publishRuleUpdated()`, `publishRuleDeleted()`

#### Crear Puertos para HU-07 (Product Rules)

- [ ] [application/port/in/ProductRuleUseCase.java](application/port/in/ProductRuleUseCase.java)
  - [ ] Métodos: `createProductRule()`, `updateProductRule()`, `deleteProductRule()`, `getProductRuleById()`, `listProductRules()`

- [ ] [application/port/out/ProductRulePersistencePort.java](application/port/out/ProductRulePersistencePort.java)
  - [ ] Métodos: `saveProductRule()`, `findProductRuleById()`, `findProductRulesByEcommerce()`, `deleteProductRule()`

- [ ] [application/port/out/ProductRuleEventPort.java](application/port/out/ProductRuleEventPort.java)
  - [ ] Métodos: `publishProductRuleCreated()`, `publishProductRuleUpdated()`, `publishProductRuleDeleted()`

#### Crear Puertos para HU-08 (Fidelity Ranges)

- [ ] [application/port/in/FidelityRangeUseCase.java](application/port/in/FidelityRangeUseCase.java)
  - [ ] Métodos: `defineFidelityRanges()`, `validateRanges()`, `updateRanges()`, `getRanges()`

- [ ] [application/port/out/FidelityRangePersistencePort.java](application/port/out/FidelityRangePersistencePort.java)
- [ ] [application/port/out/FidelityRangeEventPort.java](application/port/out/FidelityRangeEventPort.java)

#### Crear Puertos para HU-09 (Discount Limits)

- [ ] [application/port/in/DiscountConfigUseCase.java](application/port/in/DiscountConfigUseCase.java)
  - [ ] Métodos: `createConfiguration()`, `updateConfiguration()`, `getConfiguration()`, `validateLimits()`

- [ ] [application/port/out/DiscountConfigPersistencePort.java](application/port/out/DiscountConfigPersistencePort.java)
- [ ] [application/port/out/DiscountConfigEventPort.java](application/port/out/DiscountConfigEventPort.java)

#### Crear Puertos para HU-10 (Loyalty Tiers)

- [ ] [application/port/in/CustomerTierUseCase.java](application/port/in/CustomerTierUseCase.java)
  - [ ] Métodos: `createTier()`, `updateTier()`, `deleteTier()`, `getTierById()`, `listTiers()`

- [ ] [application/port/out/CustomerTierPersistencePort.java](application/port/out/CustomerTierPersistencePort.java)
- [ ] [application/port/out/CustomerTierEventPort.java](application/port/out/CustomerTierEventPort.java)

#### Crear Puertos para HU-14 (Rule Activation)

- [ ] [application/port/in/RuleActivationUseCase.java](application/port/in/RuleActivationUseCase.java)
  - [ ] Métodos: `activateRule(UUID ruleId)`, `deactivateRule(UUID ruleId)`
  - [ ] Nota: Puede estar integrada en `RuleUseCase` junto con HU-06

---

### Fase 2: Implementar Servicios (Implementar Puertos de Entrada)

#### Refactorizar RuleService → Implementar RuleUseCase

- [ ] [application/service/RuleServiceImpl.java](application/service/RuleServiceImpl.java)
  - [ ] Renombrar o crear nueva clase: `RuleServiceImpl implements RuleUseCase`
  - [ ] Cambiar DI: `private final RulePersistencePort persistencePort;` (puerto, no repo)
  - [ ] Cambiar DI: `private final RuleEventPort eventPort;` (puerto)
  - [ ] Mantener anotación `@Service`
  - [ ] Implementar todos los métodos de `RuleUseCase`
  - [ ] Remover inyección directa de `RuleRepository` concreto
  - [ ] Validar que servicios de dominio (validaciones) permanecen igual

- [ ] [application/service/ProductRuleServiceImpl.java](application/service/ProductRuleServiceImpl.java)
  - [ ] Crear si no existe, implementar `ProductRuleUseCase`
  - [ ] Inyectar `ProductRulePersistencePort`, `ProductRuleEventPort`
  - [ ] Mantener lógica de negocio original

- [ ] [application/service/FidelityRangeServiceImpl.java](application/service/FidelityRangeServiceImpl.java)
  - [ ] Similar a ProductRuleServiceImpl

- [ ] [application/service/DiscountConfigServiceImpl.java](application/service/DiscountConfigServiceImpl.java)
  - [ ] Refactorizar DiscountConfigService existente
  - [ ] Implementar `DiscountConfigUseCase`
  - [ ] Cambiar DI de repos concretos a puertos

- [ ] [application/service/CustomerTierServiceImpl.java](application/service/CustomerTierServiceImpl.java)
  - [ ] Refactorizar CustomerTierService existente
  - [ ] Implementar `CustomerTierUseCase`
  - [ ] Cambiar DI de repos concretos a puertos

- [ ] Validación de servicios:
  - [ ] Ningún servicio inyecta `*Repository` concreto
  - [ ] Todo acceso a BD es a través de `*PersistencePort`
  - [ ] Todo envío de eventos es a través de `*EventPort`

---

### Fase 3: Crear Adapters (Implementar Puertos de Salida)

#### Adapters de Persistencia (JPA)

- [ ] [infrastructure/persistence/jpa/JpaRuleAdapter.java](infrastructure/persistence/jpa/JpaRuleAdapter.java)
  - [ ] `@Component implements RulePersistencePort`
  - [ ] Inyectar `RuleRepository`
  - [ ] Delegar todas las operaciones al repositorio

- [ ] [infrastructure/persistence/jpa/JpaProductRuleAdapter.java](infrastructure/persistence/jpa/JpaProductRuleAdapter.java)
- [ ] [infrastructure/persistence/jpa/JpaFidelityRangeAdapter.java](infrastructure/persistence/jpa/JpaFidelityRangeAdapter.java)
- [ ] [infrastructure/persistence/jpa/JpaDiscountConfigAdapter.java](infrastructure/persistence/jpa/JpaDiscountConfigAdapter.java)
- [ ] [infrastructure/persistence/jpa/JpaCustomerTierAdapter.java](infrastructure/persistence/jpa/JpaCustomerTierAdapter.java)

#### Adapters de Eventos (RabbitMQ)

- [ ] [infrastructure/messaging/RuleEventAdapter.java](infrastructure/messaging/RuleEventAdapter.java)
  - [ ] `@Component implements RuleEventPort`
  - [ ] Inyectar `RabbitTemplate` o evento publisher
  - [ ] Delegar publicaciones de eventos

- [ ] [infrastructure/messaging/ProductRuleEventAdapter.java](infrastructure/messaging/ProductRuleEventAdapter.java)
- [ ] [infrastructure/messaging/FidelityRangeEventAdapter.java](infrastructure/messaging/FidelityRangeEventAdapter.java)
- [ ] [infrastructure/messaging/DiscountConfigEventAdapter.java](infrastructure/messaging/DiscountConfigEventAdapter.java)
- [ ] [infrastructure/messaging/CustomerTierEventAdapter.java](infrastructure/messaging/CustomerTierEventAdapter.java)

---

### Fase 4: Refactorizar Controllers (Inyectar Puertos en lugar de ServiceImpl)

- [ ] [presentation/controller/RuleController.java](presentation/controller/RuleController.java)
  - [ ] Cambiar: `private final RuleService ruleService;` → `private final RuleUseCase ruleUseCase;`
  - [ ] Actualizar todas las llamadas: `ruleService.method()` → `ruleUseCase.method()`
  - [ ] Mantener `@PreAuthorize()`, `SecurityContextHelper`

- [ ] [presentation/controller/RuleController.java](presentation/controller/RuleController.java) — Endpoints de Product Rules
  - [ ] Cambiar: inyectar `ProductRuleUseCase`

- [ ] [presentation/controller/FidelityRangeController.java](presentation/controller/FidelityRangeController.java)
  - [ ] Cambiar: inyectar `FidelityRangeUseCase`

- [ ] [presentation/controller/DiscountConfigController.java](presentation/controller/DiscountConfigController.java)
  - [ ] Cambiar: inyectar `DiscountConfigUseCase`

- [ ] [presentation/controller/CustomerTierController.java](presentation/controller/CustomerTierController.java)
  - [ ] Cambiar: inyectar `CustomerTierUseCase`

---

### Fase 5: Testing Backend (TDD Primero)

#### Unit Tests de Servicios (SIN Spring Security)

Todos los tests unitarios usan **Mockito** para mockear puertos. NO requieren Spring Boot Test.

- [ ] [src/test/java/com/loyalty/service_admin/application/service/RuleServiceImplTest.java](src/test/java/com/loyalty/service_admin/application/service/RuleServiceImplTest.java)
  - [ ] `test_createSeasonalRule_success` — Camino feliz
  - [ ] `test_createSeasonalRule_duplicateThrowsConflict` — Validación conflicto
  - [ ] `test_updateSeasonalRule_success`
  - [ ] `test_deleteSeasonalRule_success`
  - [ ] `test_getSeasonalRuleById_notFoundThrowsException`
  - [ ] `test_listSeasonalRules_returnsPage`
  - [ ] **Cobertura esperada:** ≥ 80% de lógica de servicio

- [ ] [src/test/java/com/loyalty/service_admin/application/service/ProductRuleServiceImplTest.java](src/test/java/com/loyalty/service_admin/application/service/ProductRuleServiceImplTest.java)
  - [ ] `test_createProductRule_success`
  - [ ] `test_createProductRule_duplicateProductTypeThrowsConflict`
  - [ ] `test_updateProductRule_exceedLimitThrowsException`
  - [ ] `test_deleteProductRule_success`

- [ ] [src/test/java/com/loyalty/service_admin/application/service/CustomerTierServiceImplTest.java](src/test/java/com/loyalty/service_admin/application/service/CustomerTierServiceImplTest.java)
  - [ ] `test_createCustomerTier_success`
  - [ ] `test_createCustomerTier_invalidHierarchyThrowsException`
  - [ ] `test_updateCustomerTier_success`
  - [ ] `test_deleteCustomerTier_success`

- [ ] [src/test/java/com/loyalty/service_admin/application/service/DiscountConfigServiceImplTest.java](src/test/java/com/loyalty/service_admin/application/service/DiscountConfigServiceImplTest.java)
  - [ ] `test_createConfiguration_success`
  - [ ] `test_createConfiguration_negativeLimitThrowsException`
  - [ ] `test_validateLimits_exceedThrowsException`

- [ ] [src/test/java/com/loyalty/service_admin/application/service/FidelityRangeServiceImplTest.java](src/test/java/com/loyalty/service_admin/application/service/FidelityRangeServiceImplTest.java)
  - [ ] `test_defineFidelityRanges_success`
  - [ ] `test_defineFidelityRanges_overlappingThrowsException`
  - [ ] `test_validateRanges_discontinuousThrowsException`

#### Integration Tests de Adapters

Adapters son testeados CON base de datos (TestContainers o H2).

- [ ] [src/test/java/com/loyalty/service_admin/infrastructure/persistence/jpa/JpaRuleAdapterTest.java](src/test/java/com/loyalty/service_admin/infrastructure/persistence/jpa/JpaRuleAdapterTest.java)
  - [ ] `test_saveRule_persistsSuccessfully`
  - [ ] `test_findRuleById_returnsSavedRule`
  - [ ] `test_deleteRule_removesFromDatabase`

- [ ] [src/test/java/com/loyalty/service_admin/infrastructure/persistence/jpa/JpaProductRuleAdapterTest.java](src/test/java/com/loyalty/service_admin/infrastructure/persistence/jpa/JpaProductRuleAdapterTest.java)
- [ ] [src/test/java/com/loyalty/service_admin/infrastructure/persistence/jpa/JpaCustomerTierAdapterTest.java](src/test/java/com/loyalty/service_admin/infrastructure/persistence/jpa/JpaCustomerTierAdapterTest.java)
- [ ] [src/test/java/com/loyalty/service_admin/infrastructure/persistence/jpa/JpaDiscountConfigAdapterTest.java](src/test/java/com/loyalty/service_admin/infrastructure/persistence/jpa/JpaDiscountConfigAdapterTest.java)

#### Event Adapter Tests

- [ ] [src/test/java/com/loyalty/service_admin/infrastructure/messaging/RuleEventAdapterTest.java](src/test/java/com/loyalty/service_admin/infrastructure/messaging/RuleEventAdapterTest.java)
  - [ ] `test_publishRuleCreated_sendsEventToRabbitMQ`
  - [ ] Mock RabbitTemplate usando Mockito

- [ ] [src/test/java/com/loyalty/service_admin/infrastructure/messaging/ProductRuleEventAdapterTest.java](src/test/java/com/loyalty/service_admin/infrastructure/messaging/ProductRuleEventAdapterTest.java)
- [ ] [src/test/java/com/loyalty/service_admin/infrastructure/messaging/CustomerTierEventAdapterTest.java](src/test/java/com/loyalty/service_admin/infrastructure/messaging/CustomerTierEventAdapterTest.java)

#### Ejecución de Tests

- [ ] Comando: `mvn clean test` desde `backend/service-admin/`
- [ ] Todos los tests unitarios pasan
- [ ] Todos los tests de integración pasan
- [ ] Cobertura de código ≥ 80% en `application/service/`
- [ ] Cobertura de código ≥ 70% en `infrastructure/`

---

### Fase 6: QA y Smoke Tests

#### Escenarios BDD Críticos

- [ ] Lista de escenarios Gherkin por feature:
  - [ ] **HU-06**: Crear, editar, eliminar seasonal rules; validar superposición de fechas
  - [ ] **HU-07**: Crear, editar, eliminar product rules; validar duplicidad
  - [ ] **HU-08**: Definir rangos de fidelidad; validar superposición y discontinuidad
  - [ ] **HU-09**: Configurar límites y prioridad; validar validaciones de negocio
  - [ ] **HU-10**: Listar tiers; validar jerarquía
  - [ ] **HU-14**: Activar/desactivar reglas; verificar cambio inmediato

#### Smoke Tests Post-Migración

- [ ] Ejecutar manual o automatizado:
  - [ ] `POST /api/v1/rules` — Crear seasonal rule (validar respuesta 201)
  - [ ] `GET /api/v1/rules/{ruleId}` — Obtener regla (validar respuesta 200)
  - [ ] `PUT /api/v1/rules/{ruleId}` — Actualizar regla (validar respuesta 200)
  - [ ] `DELETE /api/v1/rules/{ruleId}` — Eliminar regla (validar respuesta 204)
  - [ ] `PATCH /api/v1/rules/{ruleId}/activate` — Activar regla
  - [ ] `PATCH /api/v1/rules/{ruleId}/deactivate` — Desactivar regla

- [ ] Validar eventos RabbitMQ:
  - [ ] Después de crear regla, evento `rule.created` en cola
  - [ ] Después de actualizar regla, evento `rule.updated` en cola
  - [ ] Después de activar regla, evento `rule.activated` en cola

- [ ] Validar que engine-service recibe y cachea eventos correctamente

---

### Fase 7: Registro y Documentación de Cambios

#### Commits Git (Formato Obligatorio)

Cada commit debe seguir: `tipo(alcance): descripción`

- [ ] `feat(hexagonal): create RuleUseCase port interface`
- [ ] `feat(hexagonal): create RulePersistencePort and RuleEventPort`
- [ ] `feat(hexagonal): implement RuleServiceImpl from RuleUseCase`
- [ ] `feat(hexagonal): create JpaRuleAdapter implementing RulePersistencePort`
- [ ] `feat(hexagonal): create RuleEventAdapter implementing RuleEventPort`
- [ ] `refactor(controllers): update RuleController to inject RuleUseCase`
- [ ] `test(hexagonal): add RuleServiceImplTest with unit tests`
- [ ] `test(hexagonal): add JpaRuleAdapterTest with integration tests`
- [ ] [ ] Repetir para: ProductRule, FidelityRange, DiscountConfig, CustomerTier

#### Documentación Actualizada

- [ ] [docs/BACKEND_ARCHITECTURE_MAP.md](docs/BACKEND_ARCHITECTURE_MAP.md)
  - [ ] Agregar sección "Hexagonal Architecture (Ports & Adapters)"
  - [ ] Diagrama de Puerto → Service → Adapter

- [ ] [API_DOCUMENTATION.md](backend/service-admin/API_DOCUMENTATION.md)
  - [ ] Confirmar que endpoints NO cambian (URLs idénticas)

- [ ] [ARCHITECTURE_DECISION_RECORD.md](docs/ARCHITECTURE_DECISION_RECORD.md)
  - [ ] [ ] Crear ADR: "Por qué migramos a Hexagonal Architecture"
  - [ ] Razones: testabilidad, bajo acoplamiento, independencia de frameworks

---

## 4. DEPENDENCIAS Y RIESGOS

### Dependencias

| Dependencia | Versión Requerida | Justificación |
|-------------|------------------|--------------|
| Java | 21+ | Virtual threads, records, pattern matching |
| Spring Boot | 3.1+ | Jakarta Persistence, modern DI |
| Mockito | 5.x+ | Para tests unitarios de servicios |
| TestContainers | 1.17+ | Para tests de integración con BD |
| Hamcrest | 2.2+ | Matchers para assertions en tests |
| RabbitMQ | 3.8+ | Broker de mensajes (infraestructura) |
| PostgreSQL | 14+ | Base de datos |

### Riesgos

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|-------------|--------|-----------|
| **Regresión funcional** — Cambios en DI rompen endpoints | Alta | Crítico | Ejecutar smoke tests antes de deploy; compararrespuestas antes/después |
| **Spring Security en tests** — Inyectar Authentication en tests de controllers es difícil | Media | Alto | Excluir controller tests de suite unitaria; solo tests integración si es crítico |
| **Performance** — Nuevas capas de adapters añaden overhead | Baja | Medio | Profiling post-migración con JMH o gatling; comparar latencias |
| **RabbitMQ downtime** — EventPort falla si broker no está disponible | Baja | Medio | Resilience4j circuit breaker en adapters; retry logic con exponential backoff |
| **Inconsistencia en puertos** — Algunos services siguen inyectando repos concretos | Media | Alto | Code review riguroso; herramienta static analysis (SonarQube) para detectar anti-patrones |

### Criterios de Aceptación Global

- [ ] **Funcionalidad reproducida:** Todos los endpoints retornan exactamente los mismos resultados (before/after migration)
- [ ] **Cobertura de tests unitarios:** ≥ 80% en `application/service/`
- [ ] **Cobertura de tests integración:** ≥ 70% en `infrastructure/`
- [ ] **Cobertura global:** ≥ 75% en el código refactorizado
- [ ] **SonarQube score:** ≥ A (no regresar en code smells o technical debt)
- [ ] **Deployment exitoso:** Staging environment sin errores; rollback plan preparado
- [ ] **Performance non-regression:** Latencia p99 no aumenta > 5% post-migración
- [ ] **Eventos publicados correctamente:** Verificar en RabbitMQ messages históricos

---

## 5. HITOS Y TIMELINE

| Hito | Duración Estimada | Features | Bloqueador |
|------|------------------|----------|-----------|
| **Hito 1: Creación de Puertos** | 1-2 días | Todas 6 features | Ninguno |
| **Hito 2: Refactor Services** | 2-3 días | Todas 6 features | Hito 1 ✅ |
| **Hito 3: Crear Adapters** | 1-2 días | Todas 6 features | Hito 2 ✅ |
| **Hito 4: Refactor Controllers** | 1 día | Todas 6 features | Hito 3 ✅ |
| **Hito 5: Unit Tests** | 2-3 días | Todas 6 features | Hito 2 ✅ |
| **Hito 6: Integration Tests** | 1-2 días | Todas 6 features | Hito 3 ✅ |
| **Hito 7: QA / Smoke Tests** | 1 día | Todas 6 features | Hito 6 ✅ |
| **Total Estimado** | **9-14 días** | Refactoring completo | — |

---

## 6. WORKFLOW TDD EXPLÍCITO

```
PARA CADA FEATURE (ej. ProductRule):

1. CREAR PUERTO (interface)
   ├─ application/port/in/ProductRuleUseCase.java       (define casos de uso)
   ├─ application/port/out/ProductRulePersistencePort   (define operaciones BD)
   └─ application/port/out/ProductRuleEventPort         (define eventos)

2. ESCRIBIR TESTS UNITARIOS (Mockito)
   └─ src/test/java/.../ProductRuleServiceImplTest.java
      ├─ Mock ProductRulePersistencePort
      ├─ Mock ProductRuleEventPort
      └─ Ejecutar: mvn test (todos FALLAN, rojo)

3. IMPLEMENTAR SERVICE
   └─ application/service/ProductRuleServiceImpl implements ProductRuleUseCase
      ├─ Inyectar puertos (NO repos concretos)
      └─ Ejecutar: mvn test (todos PASAN, verde)

4. CREAR ADAPTERS
   ├─ infrastructure/persistence/jpa/JpaProductRuleAdapter implements ProductRulePersistencePort
   └─ infrastructure/messaging/ProductRuleEventAdapter implements ProductRuleEventPort

5. ESCRIBIR TESTS DE INTEGRACIÓN (TestContainers)
   ├─ src/test/java/.../JpaProductRuleAdapterTest
   └─ src/test/java/.../ProductRuleEventAdapterTest

6. REFACTOR CONTROLLER
   └─ presentation/controller/RuleController
      └─ Cambiar: inyectar ProductRuleUseCase (no RuleService concreto)

7. SMOKE TESTS
   └─ POST /api/v1/rules (create)
   └─ GET /api/v1/rules/{id} (read)
   └─ PUT /api/v1/rules/{id} (update)
   └─ DELETE /api/v1/rules/{id} (delete)

REPETIR para cada una de las 6 features
```

---

## 7. NOTAS Y OBSERVACIONES FINALES

### ¿Por qué Arquitectura Hexagonal?

1. **Testabilidad sin Spring**: Services se usan sin framework; tests unitarios con Mockito puro.
2. **Bajo acoplamiento**: Controllers desacoplados de implementations (servicios concretos).
3. **Reusabilidad**: Adapters son intercambiables; cambiar de JPA a MongoDB sin afectar services.
4. **Limpieza**: Separación clara entre lógica de dominio (services) e infraestructura (adapters).

### Excluir Controller Tests

- Controllers tienen `@PreAuthorize()` y `SecurityContextHelper` que inyectan `Authentication`.
- Mockear `Authentication` en tests es tedioso y fuera de alcance.
- **Decisión**: No escribir tests unitarios de controllers; confiar en smoke tests manuales o integration tests con `@SpringBootTest`.

### Migración Sin Downtime

- No hay cambios de BD (mismo schema).
- Endpoints no cambian (backward compatible).
- Deployment: Feature flag activar/desactivar nueva implementación si es necesario.

### Próximos Pasos (Después de esta spec APPROVED)

1. **Backend Developer** ejecuta `/implement-backend` → Implementa todos los puertos, servicios, adapters, tests.
2. **Test Engineer** ejecuta `/unit-testing` → Revisa cobertura de tests, valida SonarQube.
3. **QA Agent** ejecuta `/gherkin-case-generator` → Define casos BDD, ejecuta smoke tests.
4. **Orchestrator** ejecuta `/asdd-orchestrate` → Coordina la ejecución y transición a siguiente fase.

---

**Fecha de Creación**: 2026-04-08  
**Versión**: 1.0  
**Estado**: DRAFT → Pendiente aprobación del usuario

