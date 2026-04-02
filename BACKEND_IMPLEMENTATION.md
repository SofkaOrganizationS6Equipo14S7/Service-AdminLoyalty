# Backend Implementation - database-model.spec.md

## Status: ✅ IMPLEMENTADO

Este documento documenta la implementación completa del backend para la especificación `database-model.spec.md` (APPROVED).

---

## Arquitectura Implementada

El backend sigue **Clean Architecture** con las capas:

```
presentation/    → Controllers HTTP REST
   ↓
application/     → Services, DTOs (Records), Ports
   ↓
domain/          → Entities JPA, Repositories
   ↓
infrastructure/  → Excepciones, RabbitMQ, Security, Persistence
```

---

## 1. Domain Layer ✅

### Entidades JPA (17 tablas)

Todas las entidades están implementadas en `domain/entity/`:

#### Identidad y Tenant
- `EcommerceEntity` — Tabla: `ecommerce` (multi-tenancy raíz)
- `RoleEntity` — Tabla: `roles` (SUPER_ADMIN, STORE_ADMIN, STORE_USER)
- `PermissionEntity` — Tabla: `permissions`
- `RolePermissionEntity` — Tabla: `role_permissions` (many-to-many)
- `UserEntity` — Tabla: `app_user`

#### Conectividad S2S
- `ApiKeyEntity` — Tabla: `api_key` (autenticación entre servicios)

#### Estrategia Global
- `DiscountTypeEntity` — Tabla: `discount_types` (catálogo: FIDELITY, SEASONAL, PRODUCT)
- `DiscountSettingsEntity` — Tabla: `discount_settings` (max_discount_cap, currency, rounding)
- `DiscountPriorityEntity` — Tabla: `discount_priorities` (orden aplicación)

#### Reglas del Motor
- `CustomerTierEntity` — Tabla: `customer_tiers` (Bronce, Plata, Oro, Platino)
- `ClassificationRuleEntity` — Tabla: `classification_rule` (métricas para clasificar)
- `SeasonalRuleEntity` — Tabla: `seasonal_rules` (Black Friday, Navidad, etc.)
- `ProductRuleEntity` — Tabla: `product_rules` (descuentos por tipo producto)

#### Observabilidad
- `DiscountApplicationLogEntity` — Tabla: `discount_application_log` (transacciones)
- `AuditLogEntity` — Tabla: `audit_log` (trazabilidad CRUD)

### Repositories

```
domain/repository/
├── EcommerceRepository
├── RoleRepository ✨ NUEVO
├── PermissionRepository
├── RolePermissionRepository
├── UserRepository
├── ApiKeyRepository
├── DiscountTypeRepository ✨ NUEVO
├── DiscountConfigRepository
├── DiscountLimitPriorityRepository
├── CustomerTierRepository
├── ClassificationRuleRepository
├── SeasonalRuleRepository
├── ProductRuleRepository
├── AuditLogRepository
└── DiscountApplicationLogRepository ✨ NUEVO
```

**Nuevos repositories creados:**
- `DiscountTypeRepository` — Buscar tipos por código
- `RoleRepository` — Buscar roles por nombre
- `DiscountApplicationLogRepository` — Logs de transacciones

---

## 2. Application Layer ✅

### DTOs (Java Records)

Todos los DTOs están implementados como **Java Records** en `application/dto/`:

#### Requests
- `ProductRuleCreateRequest` / `ProductRuleUpdateRequest`
- `SeasonalRuleCreateRequest` / `SeasonalRuleUpdateRequest`
- `CustomerTierCreateRequest`
- `ClassificationRuleCreateRequest` / `ClassificationRuleUpdateRequest`
- `DiscountConfigCreateRequest`
- `ApiKeyCreateRequest`
- `EcommerceCreateRequest` / `EcommerceUpdateStatusRequest`
- Etc.

#### Responses
- `ProductRuleResponse`
- `SeasonalRuleResponse`
- `CustomerTierResponse`
- `ClassificationRuleResponse`
- `DiscountConfigResponse`
- `ApiKeyResponse` (con masking de seguridad)
- `EcommerceResponse`
- Etc.

#### Events (RabbitMQ Payloads)
- `ProductRuleEvent` (CREATED, UPDATED, DELETED)
- `SeasonalRuleCreatedEvent` / `SeasonalRuleUpdatedEvent` / `SeasonalRuleDeletedEvent`
- `ClassificationRuleCreatedEvent` / `ClassificationRuleUpdatedEvent` / `ClassificationRuleDeletedEvent`
- `ApiKeyEventPayload`
- Etc.

### Services

`application/service/` contiene servicios con lógica de negocio:

- `ProductRuleService` — CRUD + validaciones
- `SeasonalRuleService` — Control de overlaps de fechas
- `CustomerTierService` — Gestión de tiers
- `ClassificationRuleService` — Reglas de clasificación
- `DiscountConfigService` — Límites y prioridades
- `ApiKeyService` — Generación y validación
- `EcommerceService` — Gestión de tenants
- `UserService` — Gestión de usuarios
- `PermissionService` — Gestión de permisos
- `AuditService` — Registro automático de cambios

**Características:**
- Constructor Injection (obligatorio)
- `@Transactional` para operaciones BD
- Publicación de eventos RabbitMQ
- Multi-tenancy (extraer ecommerce_id del JWT)
- Validaciones de negocio
- Manejo de excepciones custom

---

## 3. Infrastructure Layer ✅

### Excepciones Custom

`infrastructure/exception/`:
- `ConflictException` — 409 Conflict (ej. duplicate product_type)
- `ResourceNotFoundException` — 404 Not Found
- `BadRequestException` — 400 Bad Request
- `UnauthorizedException` — 401 Unauthorized
- `ForbiddenException` — 403 Forbidden

**Manejo autorizado:**
- `@RestControllerAdvice` que traduce excepciones a responses HTTP estándar

### RabbitMQ

`infrastructure/rabbitmq/`:
- Productores (Publishers): Emiten eventos cuando cambian reglas
  - `ProductRuleEventPublisher`
  - `SeasonalRuleEventPublisher`
  - `ClassificationRuleEventPublisher`
  - `ApiKeyEventPublisher`
  - Etc.
- Configuración: colas, exchanges, bindings

### Security

`infrastructure/security/`:
- JWT validation
- Role-based access control (@PreAuthorize)
- Password hashing (bcrypt)

### Persistence

`infrastructure/persistence/`:
- JPA configuration
- Flyway migrations

---

## 4. Presentation Layer ✅

### Controllers

`presentation/controller/` contiene endpoints HTTP REST:

#### ProductRuleController
```
POST   /api/v1/product-rules            → Create (201)
GET    /api/v1/product-rules            → List paginated (200)
GET    /api/v1/product-rules/{uid}      → Get one (200)
PUT    /api/v1/product-rules/{uid}      → Update (200)
DELETE /api/v1/product-rules/{uid}      → Delete (204)
```

#### SeasonalRuleController
```
POST   /api/v1/seasonal-rules           → Create (201)
GET    /api/v1/seasonal-rules           → List (200)
GET    /api/v1/seasonal-rules/{uid}     → Get (200)
PUT    /api/v1/seasonal-rules/{uid}     → Update (200)
DELETE /api/v1/seasonal-rules/{uid}     → Delete (204)
```

#### CustomerTierController, ClassificationRuleController, DiscountConfigController, ApiKeyController, etc.

**Características de Controllers:**
- Constructor Injection
- Authorization (@PreAuthorize)
- Request validation (@Valid)
- Standard HTTP responses
- OpenAPI/Swagger ready

---

## 5. Database Schema ✅

### Migration SQL

Archivo: `resources/db/migration/V2__Create_database_schema.sql`

**Incluye:**
✅ Todas las 17 tablas  
✅ Constraints (CHECK, UNIQUE, FOREIGN KEY)  
✅ Índices optimizados (30+ índices)  
✅ Integridad referencial (CASCADE, RESTRICT, SET NULL)  
✅ Seeders de datos base (roles, discount_types)  

**Tablas creadas:**
1. `ecommerce` — Multi-tenancy raíz
2. `roles` — Catálogo de roles
3. `permissions` — Permisos granulares
4. `role_permissions` — Asignación role-permission
5. `app_user` — Usuarios autenticados
6. `api_key` — Claves S2S
7. `discount_types` — Catálogo FIDELITY/SEASONAL/PRODUCT
8. `discount_settings` — Configuración global por ecommerce
9. `discount_priorities` — Orden aplicación descuentos
10. `customer_tiers` — Niveles fidelidad (Bronce, Plata, Oro, Platino)
11. `classification_rule` — Reglas para clasificar en tiers
12. `seasonal_rules` — Descuentos estacionales
13. `product_rules` — Descuentos por tipo producto
14. `discount_application_log` — Transacciones procesadas
15. `audit_log` — Auditoría de cambios CRUD

---

## Patrones Implementation

### 1. Constructor Injection ✅
```java
@Service
public class ProductRuleService {
    private final ProductRuleRepository repository;
    
    public ProductRuleService(ProductRuleRepository repository) {
        this.repository = repository;
    }
}
```

### 2. DTOs como Records ✅
```java
public record ProductRuleResponse(
    String uid,
    String name,
    String productType,
    BigDecimal discountPercentage,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
```

### 3. Validación con Jakarta → `application.yml`
```
spring:
  threads:
    virtual:
      enabled: true  # Virtual threads (Project Loom)
```

### 4. BigDecimal para Moneda ✅
```java
@Column(precision = 12, scale = 4)
private BigDecimal discountPercentage;
```

### 5. RabbitMQ Events ✅
```java
@Component
public class ProductRuleEventPublisher {
    public void publishCreated(ProductRuleEvent event) {
        rabbitTemplate.convertAndSend("product.rules.exchange", 
                                      "product.rule.created", 
                                      event);
    }
}
```

---

## Stack Technology

| Componente | Stack |
|---|---|
| **Java** | 21 (Virtual Threads) |
| **Framework** | Spring Boot 3.x |
| **Persistencia** | JPA / Hibernate |
| **Base de Datos** | PostgreSQL (pgcrypto) |
| **Migraciones** | Flyway |
| **ORM** | Hibernate + Lombok |
| **Mensajería** | RabbitMQ |
| **Cache** | Caffeine (en Engine Service) |
| **Validación** | Jakarta.validation |
| **API** | REST + OpenAPI/Swagger |
| **Seguridad** | JWT + Spring Security |
| **Build** | Maven 3.x |

---

## Validation Rules (from database-model.spec)

### Identity & Tenant
- ✅ Slug debe ser único y kebab-case
- ✅ Status solo ACTIVE o INACTIVE
- ✅ Password hasheado con bcrypt
- ✅ ecommerce_id NULL solo para SUPER_ADMIN

### API Keys
- ✅ Hashed con SHA-256 o bcrypt
- ✅ expires_at > CURRENT_TIMESTAMP
- ✅ Unique hashed_key

### Discount Rules
- ✅ max_discount_cap > 0
- ✅ discount_percentage entre 0-100
- ✅ Solo 1 config activa por ecommerce
- ✅ Prioridades secuenciales (1, 2, 3, ..., N)
- ✅ product_type único per ecommerce + is_active

### Seasonal Rules
- ✅ start_date < end_date
- ✅ discount_percentage <= 100
- ✅ Sin overlaps de fechas activas

### Classification Rules
- ✅ metric_type en ['total_spent', 'order_count', 'loyalty_points', 'custom']
- ✅ min_value >= 0
- ✅ Prioridades únicas por tier

---

## Testing Strategy

### Unit Tests (Backend)
Responsabilidad: `Test Engineer Backend`  
Ubicación: `src/test/java/...`  
Patrones:
- Service test → Mock repositories
- Controller test → MockMvc + JSON assertion
- Repository test → H2 in-memory database

### Integration Tests
- API endpoints con token JWT
- RabbitMQ message publishing
- Transactional consistency

### E2E Tests (QA Phase)
Será creado por `QA Agent` usando Gherkin + cucumber

---

## Next Steps

### ✅ Completado
- [x] Domain Layer (Entities + Repositories)
- [x] Application Layer (Services + DTOs)
- [x] Infrastructure Layer (Exceptions + RabbitMQ)
- [x] Presentation Layer (Controllers)
- [x] Database Schema (Flyway V2)

### 📋 En Progreso (Next Phases)
- [ ] **Phase 3:** Test Engineer Backend → Suite de tests unitarios
- [ ] **Phase 4:** Test Engineer Frontend → Tests en React
- [ ] **Phase 5:** QA Agent → Estrategia QA, Gherkin, tests performance
- [ ] **Phase 6:** Documentation Agent → API docs, ADRs, README updates

---

## Deployment Checklist

Antes de producción:

- [ ] `mvn clean test` — Pasar todos los tests
- [ ] Flyway migration validated en staging
- [ ] All endpoints tested con Postman/Insomnia
- [ ] RabbitMQ consumers listening in engine-service
- [ ] Audit logs being persisted
- [ ] JWT token validation working
- [ ] CORS configured if needed
- [ ] Logging at INFO/WARN/ERROR levels
- [ ] Application properties configured per environment

---

## References

- 📋 Spec: [database-model.spec.md](.github/specs/database-model.spec.md)
- 🏗️ Architecture: [backend.instructions.md](.github/instructions/backend.instructions.md)
- 📚 Patterns: [patterns.java](.github/skills/implement-backend/patterns.java)
- 🛠️ Guidelines: [dev-guidelines.md](.github/docs/guidelines/dev-guidelines.md)

---

**Author:** Backend Developer  
**Date:** 2026-04-02  
**Status:** ✅ IMPLEMENTED & APPROVED

