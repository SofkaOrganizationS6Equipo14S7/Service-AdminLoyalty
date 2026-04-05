---
id: SPEC-010
status: APPROVED
feature: loyalty-tiers-classification
created: 2026-04-05
updated: 2026-04-05
author: spec-generator
version: "1.1"
related-specs:
  - SPEC-011 (cart-calculation)
  - SPEC-012 (discount-history)
---

# Spec: Loyalty Tiers — Clasificación Dinámica de Clientes (HU-10)

> **Estado:** `APPROVED` — Listo para implementación.
> **Ciclo de vida:** APPROVED → IN_PROGRESS → IMPLEMENTED

> **Contexto:** Esta HU establece el sistema interno de clasificación de clientes en tiers de fidelidad. 
> **IMPORTANTE**: Los endpoints de clasificación NO se exponen públicamente en Engine Service.
> La clasificación es un **servicio interno** evaluado durante el cálculo de descuentos (`/calculate` en HU-11).
> El Admin Service es responsable de exponer APIs para gestionar y visualizar tiers.

---

## 1. REQUERIMIENTOS

### Descripción
Implementar un servicio interno de clasificación dinámica que evalúe a los clientes en tiers de fidelidad (Bronze, Silver, Gold, Platinum, etc.) basándose en criterios del payload del e-commerce (total_spent, order_count, membership_days, etc.). Los tiers se sincronizan desde Admin Service vía RabbitMQ y se cachean en memoria con Caffeine para evaluaciones de baja latencia. La clasificación debe ser **determinista**: el mismo payload de cliente siempre produce el mismo tier. Este servicio es consumido internamente por HU-11 (cálculo de descuentos).

### Requerimiento de Negocio
Como motor de cálculo del Engine Service, necesito evaluar dinámicamente a los clientes en tiers de fidelidad (sin exponerlo como endpoint público), basándome en sus métricas (gasto total, cantidad de órdenes, días de membresía), para que el cálculo de descuentos (HU-11) pueda aplicar descuentos específicos por tier. La clasificación debe ser rápida (en memoria) y consistente con las reglas definidas en el Admin.

### Historias de Usuario

#### HU-10: Clasificación Dinámica de Clientes en Loyalty Tiers

```
Como:        Motor de Cálculo del Engine Service
Quiero:      Clasificar dinámicamente a clientes en tiers de fidelidad (Bronze, Silver, Gold, Platinum)
Para:        Aplicar descuentos específicos por tier en el cálculo de carrito (HU-11)

Prioridad:   Alta
Estimación:  L
Dependencias: Admin Service exporta tiers vía RabbitMQ
Capa:        Backend (Engine Service)
```

#### Criterios de Aceptación — HU-10

**CRITERIO-10.1: Clasificación exitosa basada en total_spent**
```gherkin
Dado que:      existen tiers configurados en el Admin:
               | tier_name | min_spent | max_spent | discount_pct |
               | Bronze    | 0         | 500       | 2%           |
               | Silver    | 500       | 2000      | 5%           |
               | Gold      | 2000      | 10000     | 10%          |
               | Platinum  | 10000     | unlimited | 15%          |
Y:             el cliente tiene un total_spent de 2500
Cuando:        el evaluador de tiers consulta la clasificación del cliente
Entonces:      el sistema clasifica el cliente en el tier "Gold"
Y:             la respuesta contiene: { "tier_uid": "uuid", "tier_name": "Gold", "discount_percentage": 10.0 }
```

**CRITERIO-10.2: Clasificación determinista (mismo payload = mismo resultado)**
```gherkin
Dado que:      cliente con payload { "customer_id": "123", "total_spent": 2500, "order_count": 15, "membership_days": 180 }
Cuando:        se evalúa la clasificación 3 veces en la misma sesión
Entonces:      la respuesta es idéntica en todas las ocasiones (determinismo)
Y:             el tier resultado siempre es "Gold" para ese payload
```

**CRITERIO-10.3: Evaluación de múltiples criterios (total_spent + order_count)**
```gherkin
Dado que:      tier "Platinum" requiere: (total_spent >= 10000) OR (order_count >= 50)
Y:             cliente A tiene: total_spent=5000, order_count=60
Y:             cliente B tiene: total_spent=12000, order_count=10
Cuando:        se evalúan ambos clientes
Entonces:      ambos califican como "Platinum"
Y:             el evaluador soporta lógica compleja en logic_conditions JSONB
```

**CRITERIO-10.4: Sincronización RabbitMQ desde Admin al Engine (eventual consistency)**
```gherkin
Dado que:      Admin Service crea un nuevo tier "VIP" con min_spent=50000
Y:             emite evento "customer-tiers.updated" vía RabbitMQ
Cuando:        el Engine Service recibe el evento
Entonces:      el evento es procesado en < 1 segundo
Y:             el nuevo tier se almacena en engine_customer_tiers
Y:             el Caffeine cache se invalida y se recarga
Y:             la siguiente evaluación incluye el tier "VIP"
```

**CRITERIO-10.5: Cache Caffeine con TTL y invalidación por evento**
```gherkin
Dado que:      cache está poblado con tiers de Admin
Cuando:        pasa 1 hora (TTL default)
Y:             no hay actualización de Admin en ese tiempo
Entonces:      los datos del cache siguen siendo válidos (refresh)
Y:             cuando recibe evento "customer-tiers.updated"
Entonces:      el cache se invalida inmediatamente y se recarga
```

**CRITERIO-10.6: Cliente sin clasificación por criterios no cumplidos**
```gherkin
Dado que:      cliente con total_spent=0, order_count=0 (nuevo cliente)
Cuando:        se evalúa la clasificación
Entonces:      el cliente se clasifica en el tier más bajo configurado (ej. "Bronze")
Y:             la respuesta contiene el tier mínimo disponible
```

**CRITERIO-10.7: Validación de payload del cliente**
```gherkin
Dado que:      cliente envía payload con campos incompletos o faltantes
Cuando:        se intenta clasificar sin total_spent o sin order_count
Entonces:      el sistema retorna 400 Bad Request
Y:             el mensaje detalla qué campos son obligatorios: ["total_spent", "order_count", "membership_days"]
```

**CRITERIO-10.8: Auditoría de clasificación (logs no-PII)**
```gherkin
Dado que:      cliente es clasificado en "Gold"
Cuando:        la evaluación completa
Entonces:      el sistema registra un log sin PII:
               { "customer_id_hash": "...", "evaluated_at": "...", "tier_result": "Gold", "criteria_met": [...] }
```

### Reglas de Negocio

1. **Determinismo obligatorio**: El mismo cliente_id + payload siempre produce la misma clasificación en la misma sesión.
2. **Jerarquía de tiers**: Los tiers tienen un `hierarchy_level` que define su posición en la escala (Bronze=1, Silver=2, Gold=3, Platinum=4).
3. **Sincronización eventual**: Los datos del Admin se replican al Engine con delay < 1 segundo vía RabbitMQ.
4. **Cache en memoria**: Se utiliza Caffeine con TTL de 1 hora y invalidación por evento.
5. **No almacenar PII**: Los logs de evaluación NO pueden contener información de identidad del cliente (customer_name, email, etc.).
6. **Criterios dinámicos en JSONB**: Los tiers se evalúan según `logic_conditions` JSONB con campos como `min_spent`, `max_spent`, `min_order_count`, `min_membership_days`.
7. **Fallback a tier mínimo**: Si el cliente no cumple ningún criterio, se clasifica en Bronze (tier más bajo).

---

## 2. DISEÑO

### Modelos de Datos

#### Entidades afectadas

| Entidad | Almacén | Cambios | Descripción |
|---------|---------|---------|-------------|
| `CustomerTierReplicaEntity` | tabla `engine_customer_tiers` | usado/sincronizado | Replica de tiers desde Admin vía RabbitMQ |
| `EngineRuleEntity` | tabla `engine_rules` | usado en logic_conditions | Reglas con criterios JSONB para clasificación |

#### Campos del modelo — `engine_customer_tiers`

| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `id` | UUID | sí | PK | Identificador único (generado por Admin) |
| `ecommerce_id` | UUID | sí | FK | Identifica el e-commerce propietario |
| `name` | string | sí | max 100 chars, unique(ecommerce_id) | Nombre del tier (Bronze, Silver, Gold, Platinum) |
| `discount_percentage` | decimal(5,2) | sí | 0-100 | Descuento base del tier |
| `hierarchy_level` | integer | sí | >= 1 | Posición en escala: 1=Bronze, 2=Silver, 3=Gold, 4=Platinum |
| `is_active` | boolean | sí | default true | Marca si el tier está activo |
| `synced_at` | timestamp UTC | sí | auto-set | Última sincronización desde Admin |
| `created_at` | timestamp UTC | sí | auto-set | Creación del tier |
| `updated_at` | timestamp UTC | sí | auto-set | Última actualización |

#### Campos del modelo — `engine_rules` (para lógica de clasificación)

| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `id` | UUID | sí | PK | Identificador único |
| `ecommerce_id` | UUID | sí | FK | E-commerce propietario |
| `name` | string | sí | max 255 chars | Nombre descriptivo (ej. "Gold Tier Rule") |
| `discount_type_code` | enum | sí | = "CLASSIFICATION" | Marca esta regla como clasificación |
| `logic_conditions` | JSONB | sí | ver esquema | Criterios dinámicos de clasificación |
| `priority_level` | integer | sí | >= 1 | Orden de evaluación |
| `is_active` | boolean | sí | default true | Activa/inactiva |
| `synced_at` | timestamp UTC | sí | auto-set | Sincronización desde Admin |
| `created_at` | timestamp UTC | sí | auto-set | Creación |
| `updated_at` | timestamp UTC | sí | auto-set | Actualización |

#### Estructura de `logic_conditions` JSONB

```json
{
  "min_spent": {
    "type": "NUMERIC",
    "value": 2000.00
  },
  "max_spent": {
    "type": "NUMERIC",
    "value": 10000.00
  },
  "min_order_count": {
    "type": "INTEGER",
    "value": 10
  },
  "min_membership_days": {
    "type": "INTEGER",
    "value": 90
  },
  "evaluation_logic": {
    "type": "STRING",
    "value": "(total_spent >= 2000 AND total_spent < 10000) AND (order_count >= 10 OR membership_days >= 90)"
  }
}
```

#### Índices / Constraints

- **`engine_customer_tiers`**:
  - PK: `id`
  - FK: `ecommerce_id` (índice)
  - Unique: `(ecommerce_id, name)`
  - Index: `(ecommerce_id, is_active, hierarchy_level)` para búsqueda rápida

- **`engine_rules`** (usado en clasificación):
  - PK: `id`
  - FK: `ecommerce_id` (índice)
  - Index: `(ecommerce_id, discount_type_code, is_active)` para tiers

### API Endpoints

> **NOTA CRÍTICA**: Los endpoints de clasificación NO se exponen públicamente en Engine Service.
> La clasificación es un **servicio interno** evaluado durante el endpoint `/api/v1/engine/calculate` (HU-11).
> El Admin Service es responsable de exponer endpoints para gestionar y visualizar tiers.
> 
> Engine Service solo expone:
> - `POST /api/v1/engine/calculate` (HU-11) — Cálculo de descuentos con clasificación interna
> - `GET /health` — Health check

### Arquitectura y Servicios

#### `FidelityClassificationService`
**Responsabilidad**: Lógica interna de clasificación (usada por HU-11 durante cálculo de descuentos).

```java
public class FidelityClassificationService {
    
    // Clasifica un cliente basado en su payload
    // Usada internamente por DiscountCalculationServiceV2 (HU-11)
    public ClassificationResult classify(ClassifyCustomerRequest request) {
        // 1. Validar payload
        // 2. Consultar cache Caffeine -> tiers
        // 3. Evaluar logic_conditions JSONB
        // 4. Retornar tier más alto calificado
        // 5. Log sin PII para auditoría
    }
}
```

#### `ClassificationMatrixCaffeineCacheService`
**Responsabilidad**: Gestión del cache Caffeine con tiers y reglas.

```java
public class ClassificationMatrixCaffeineCacheService {
    
    // Carga y cachea tiers desde DB
    // TTL: 1 hora, invalidable por evento RabbitMQ
    public void loadAndCacheTiers(UUID ecommerceId) { }
    
    // Invalida cache por evento de cambio en Admin
    public void invalidateOnTierUpdate(CustomerTierEvent event) { }
    
    // Obtiene un tier del cache
    public Optional<CustomerTierDTO> getTierFromCache(UUID ecommerceId, String tierName) { }
}
```

#### `RabbitMQ Consumer`: `CustomerTierSyncConsumer`
**Responsabilidad**: Escucha eventos de cambio de tiers desde Admin.

```java
@Service
public class CustomerTierSyncConsumer {
    
    @RabbitListener(queues = "customer-tiers.sync")
    public void onCustomerTierUpdated(CustomerTierEvent event) {
        // 1. Actualizar engine_customer_tiers en DB
        // 2. Invalidar Caffeine cache
        // 3. Log del evento (sin PII)
    }
}
```

### DTOs

#### Solicitud — `ClassifyCustomerRequest`
```java
public record ClassifyCustomerRequest(
    UUID ecommerceId,
    String customerId,          // Customer ID externo (no se almacena)
    BigDecimal totalSpent,
    Integer orderCount,
    Integer membershipDays,
    Instant lastPurchaseDate
) { }
```

#### Respuesta — `ClassificationResult`
```java
public record ClassificationResult(
    UUID tierUid,
    String tierName,
    BigDecimal discountPercentage,
    Integer hierarchyLevel,
    List<String> criteriaMetList,
    Instant classifiedAt
) { }
```

#### Cache — `CustomerTierDTO`
```java
public record CustomerTierDTO(
    UUID uid,
    UUID ecommerceId,
    String name,
    BigDecimal discountPercentage,
    Integer hierarchyLevel,
    boolean isActive,
    Instant syncedAt
) { }
```

---

## 3. LISTA DE TAREAS

### Backend (Engine Service)

#### 3.1 Entidades y Repositorios
- [ ] Confirmar existencia de `CustomerTierReplicaEntity` en `domain/entity/`
- [ ] Confirmar existencia de `CustomerTierRepository extends JpaRepository<CustomerTierReplicaEntity, UUID>`
- [ ] Validar índices en DB schema (ecommerce_id, hierarchy_level, is_active)

#### 3.2 Servicio de Clasificación
- [ ] Crear `FidelityClassificationService` en `application/service/`
- [ ] Implementar método `classify(ClassifyCustomerRequest)` con lógica determinista
- [ ] Implementar evaluación de `logic_conditions` JSONB
- [ ] Implementar fallback a tier mínimo si no hay match
- [ ] Agregar logs de auditoría sin PII (usar hash de customer_id)

#### 3.3 Cache Caffeine
- [ ] Crear `ClassificationMatrixCaffeineCacheService` en `infrastructure/cache/`
- [ ] Configurar Caffeine con TTL = 1 hora
- [ ] Implementar `invalidateOnTierUpdate(event)` para eventos RabbitMQ
- [ ] Implementar preload al startup (`@PostConstruct`)
- [ ] Agregar métricas de hit/miss rate del cache

#### 3.4 RabbitMQ Consumer
- [ ] Crear `CustomerTierSyncConsumer` en `infrastructure/rabbitmq/`
- [ ] Escuchar queue `customer-tiers.sync` (Fanout desde Admin)
- [ ] Implementar Handler para evento `customer-tiers.updated`
- [ ] Actualizar `engine_customer_tiers` en DB
- [ ] Disparar invalidación de cache Caffeine
- [ ] Log de eventos recibidos

#### 3.5 DTOs Internos
- [ ] Crear `ClassifyCustomerRequest` (DTO interno, no expuesto en controller)
- [ ] Crear `ClassificationResult` (DTO de retorno interno)
- [ ] Crear `CustomerTierDTO` (DTO de cache)
- [ ] Usar `@Value` de Lombok o Records de Java 21 para DTOs

#### 3.6 Manejo de Errores
- [ ] Exception `ClassificationException` para errores de lógica
- [ ] Exception `CacheUnavailableException` si Caffeine falla
- [ ] Global `ExceptionHandler` para respuestas consistentes
- [ ] Mensajes de error detallados (field, message)

#### 3.7 Tests Unitarios
- [ ] Test: Clasificación correcta por total_spent
- [ ] Test: Clasificación correcta por order_count
- [ ] Test: Determinismo (X llamadas = Y resultado idéntico)
- [ ] Test: Cliente sin match → tier mínimo
- [ ] Test: Evaluación de criterios complejos (AND, OR)
- [ ] Test: Invalidación de cache por evento RabbitMQ
- [ ] Test: Payload inválido → 400 Bad Request
- [ ] Test: Customer no cumple ningún criterio → Bronze (mínimo)

### Frontend (Dashboard Opcional)

#### 3.8 Frontend (NO aplica a esta HU)
- [ ] Dashboard de Tiers es responsabilidad del **Admin Service**, no del Engine
- [ ] Engine Service no expone endpoints públicos para visualizar tiers

### QA / Testing

#### 3.9 Casos de Prueba
- [ ] Gherkin: Clasificación por total_spent
- [ ] Gherkin: Clasificación por múltiples criterios (AND)
- [ ] Gherkin: Sincronización RabbitMQ < 1s
- [ ] Gherkin: Cache con TTL 1 hora
- [ ] Gherkin: Invalidación por evento
- [ ] Performance: 1000 clasificaciones/segundo con Caffeine
- [ ] Performance: Latencia P99 < 50ms

#### 3.10 Riesgos de Calidad (ASDD Risk Classification)
- **Alto**: Determinismo comprometido → validar mismo payload 10x opcionales:
- **Medio**: Cache corrupto → validar eventos RabbitMQ llegan correctamente
- **Medio**: JSONB logic_conditions mal formado → validar contra schema en DB
- **Bajo**: Tier mínimo incorrecto → validar fallback en tests

---

## Notas Técnicas

### Determinismo
- Usar `BigDecimal` para operaciones numéricas (nunca `double`)
- No usar `System.currentTimeMillis()` dentro de clasificación
- Evaluación siempre en el mismo orden de criterios
- Resultado debe ser repeatable en la misma sesión

### Cache Strategy
- TTL: 1 hora (configurable en `application.yml`)
- Max size: 10,000 tiers en memoria
- Invalidación: por evento RabbitMQ (inmediato)
- Preload: al startup del Engine Service

### RabbitMQ Events
- Queue: `customer-tiers.sync` (Fanout desde Admin)
- Event type: `customer_tier.updated | created | deleted`
- Payload: Tier completo con `logic_conditions` JSONB

### Seguridad
- **Sin endpoints públicos**: La clasificación es un servicio interno del Engine
- **Confidencialidad**: NO manejar credenciales de clientes finales en payload
- **Auditoría**: Logs sin PII (hash customer_id si es necesario)
- **Sin acoplamiento**: Clasificación no requiere Spring Security adicional (es consumida internamente por `/calculate`)

### Performance
- Evaluación: < 50ms P99 (Caffeine en memoria)
- Throughput: 1,000+ clasificaciones/segundo
- Cache hit rate > 95% esperado (1 hora TTL)

---

## Flujo de Integración

**ESTRUCTURA INTERNA DEL ENGINE SERVICE**:

```
[Admin Service]
    ↓ [RabbitMQ: customer-tiers.updated]
[CustomerTierSyncConsumer] → [engine_customer_tiers]
    ↓
[ClassificationMatrixCaffeineCacheService] → [Caffeine Cache]
    ↓
[FidelityClassificationService] ← Evaluación interna
    ↓ [Retorna: ClassificationResult]
[DiscountCalculationServiceV2] (HU-11) → POST /api/v1/engine/calculate
    ↓
[TransactionLogWriter] → [transaction_logs] con customer_tier
```

**Datos transferidos entre HU-10 y HU-11:**
- HU-10 retorna `ClassificationResult` (tier_name, discount_percentage, hierarchy_level)
- HU-11 consume `ClassificationResult` internamente
- HU-11 guarda `customer_tier` en `transaction_logs` para auditoría (HU-12)

---

## Decisión Arquitectónica

### Por qué NO exponemos endpoints de clasificación en Engine Service

1. **Separación de responsabilidades**: El Admin Service (especializado en gestión) es el propietario de los tiers, no el Engine
2. **Evitar acoplamiento**: Exponer endpoints de clasificación requeriría Spring Security adicional en Engine, aumentando complejidad innecesaria
3. **Enfoque especializado**: El Engine Service es un motor de evaluación de reglas, no un API de gestión
4. **Arquitectura limpia**: La clasificación es un servicio INTERNO consumido por `/api/v1/engine/calculate`, no un endpoint público
5. **Admin Service es responsable**: Debe exponer sus propios endpoints para que el frontend/usuarios puedan:
   - Listar tiers (`GET /api/v1/admin/tiers`)
   - Crear/editar tiers (`POST/PUT /api/v1/admin/tiers`)
   - Un EventPublisher para notificar al Engine vía RabbitMQ en cambios

### Endpoints de Engine Service en esta HU
- **NO hay endpoints nuevos** (la clasificación se integra internamente en HU-11)
- Engine Service mantiene solo sus dos endpoints base:
  - `POST /api/v1/engine/calculate` (HU-11)
  - `GET /health` (health check)
