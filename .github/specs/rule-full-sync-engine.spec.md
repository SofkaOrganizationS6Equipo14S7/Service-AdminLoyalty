---
id: SPEC-010
status: DRAFT
feature: rule-full-sync-engine
created: 2026-04-08
updated: 2026-04-08
author: spec-generator
version: "1.0"
related-specs: ["SPEC-008", "SPEC-009"]
---

# Spec: Sincronización Completa de Rules a Engine (FIDELITY, SEASONAL, PRODUCT, CLASSIFICATION)

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de iniciar implementación.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPRECATED

---

## 1. REQUERIMIENTOS

### Descripción
Implementar sincronización completa mediante eventos genéricos para todos los tipos de reglas de descuento (FIDELITY, SEASONAL, PRODUCT, CLASSIFICATION) desde Admin Service al Engine Service. Actualmente solo CLASSIFICATION se sincroniza mediante eventos. Se implementará un evento genérico `RuleEvent` que maneje CREATE, UPDATE y DELETE para todos los tipos, consolidando la lógica de sincronización en un único consumer.

### Requerimiento de Negocio
El requerimiento original HU-015:

```
Sincronización de Rules a Engine (FIDELITY, SEASONAL, PRODUCT)
Problema: Solo CLASSIFICATION se sincroniza. FIDELITY, SEASONAL y PRODUCT no tienen eventos hacia Engine.

Solución: Eventos Genéricos (Recomendado)
Un solo tipo de evento que funcione para todos los tipos:
{
  "eventType": "RULE_CREATED" | "RULE_UPDATED" | "RULE_DELETED",
  "ruleId": "uuid",
  "ecommerceId": "uuid",
  "name": "string",
  "description": "string",
  "discountTypeCode": "FIDELITY | SEASONAL | PRODUCT | CLASSIFICATION",
  "discountValue": 20.00,
  "priorityLevel": 1,
  "logicConditions": { ... },  // atributos serializados
  "isActive": true
}

Implementación:
1. En RuleService (Admin), al crear/actualizar/eliminar regla, publicar evento
2. En Engine, unificar consumidor (o crear uno nuevo que maneje todos los tipos)
```

### Historias de Usuario

#### HU-15.1: Crear Regla y Sincronizar a Engine

```
Como:        Administrador de ecommerce
Quiero:      Que al crear una regla (FIDELITY, SEASONAL, PRODUCT), se publique un evento hacia Engine
Para:        Que el Engine Service sincronice la regla en su BD replica (engine_rules) y caché

Prioridad:   Alta
Estimación:  M
Dependencias: HU-07 (Reglas base), SPEC-008, SPEC-009
Capa:        Backend (Admin + Engine)
```

#### Criterios de Aceptación — HU-15.1

**Happy Path: Crear regla FIDELITY**
```gherkin
CRITERIO-15.1.1: Crear regla FIDELITY y emitir evento RuleCreated
  Dado que:  Un administrador crea una regla FIDELITY en Admin Service
             con datos válidos (name, discountValue, priorityLevel, isActive, logicConditions)
  Cuando:    Ejecuta POST /api/v1/rules
             con body { "discountTypeCode": "FIDELITY", ... }
  Entonces:  La respuesta es 201 Created
             y la regla se persiste en admin.rules
             y se publica evento RuleEvent a RabbitMQ con:
               eventType: "RULE_CREATED"
               ruleId: {uuid generado}
               ecommerceId: {del usuario}
               discountTypeCode: "FIDELITY"
               logicConditions: {atributos desnormalizados}
               timestamp: {fecha/hora actual UTC}
```

**Happy Path: Crear regla SEASONAL**
```gherkin
CRITERIO-15.1.2: Crear regla SEASONAL y emitir evento
  Dado que:  Un administrador crea una regla SEASONAL en Admin Service
             con atributos start_date, end_date, discount_value, priority_level
  Cuando:    Ejecuta POST /api/v1/rules
             con body { "discountTypeCode": "SEASONAL", "attributes": { "start_date": "...", "end_date": "..." } }
  Entonces:  La respuesta es 201 Created
             y se publica evento RuleEvent con:
               eventType: "RULE_CREATED"
               discountTypeCode: "SEASONAL"
               logicConditions: {start_date, end_date, discount_value}
```

**Happy Path: Crear regla PRODUCT**
```gherkin
CRITERIO-15.1.3: Crear regla PRODUCT y emitir evento
  Dado que:  Un administrador crea una regla PRODUCT
             con atributos product_type, discount_value
  Cuando:    Ejecuta POST /api/v1/rules
             con body { "discountTypeCode": "PRODUCT", "attributes": { "product_type": "electronics" } }
  Entonces:  La respuesta es 201 Created
             y se publica evento RuleEvent con:
               eventType: "RULE_CREATED"
               discountTypeCode: "PRODUCT"
               logicConditions: {product_type, discount_value}
```

**Happy Path: Crear regla CLASSIFICATION**
```gherkin
CRITERIO-15.1.4: Crear regla CLASSIFICATION y emitir evento
  Dado que:  Un administrador crea una regla CLASSIFICATION (ya existente)
             con atributos min_value, max_value, metricType
  Cuando:    Ejecuta POST /api/v1/rules
             con body { "discountTypeCode": "CLASSIFICATION", "attributes": { "min_value": 100, "max_value": 500 } }
  Entonces:  La respuesta es 201 Created
             y se publica evento RuleEvent con:
               eventType: "RULE_CREATED"
               discountTypeCode: "CLASSIFICATION"
               logicConditions: {min_value, max_value, metricType}
```

**Error Path: Validación fallida**
```gherkin
CRITERIO-15.1.5: Crear regla con datos inválidos (no emitir evento)
  Dado que:  Se intenta crear una regla con name vacío o discountValue <= 0
  Cuando:    Ejecuta POST /api/v1/rules con body inválido
  Entonces:  La respuesta es 400 Bad Request
             y NO se publica evento a RabbitMQ
             y se retorna ApiErrorResponse con detalles de validación
```

**Error Path: Sin autenticación**
```gherkin
CRITERIO-15.1.6: Crear regla sin token JWT
  Dado que:  Se intenta crear una regla sin token de autenticación
  Cuando:    Ejecuta POST /api/v1/rules sin header Authorization
  Entonces:  La respuesta es 401 Unauthorized
             y NO se publica evento
```

---

#### HU-15.2: Actualizar Regla y Sincronizar a Engine

```
Como:        Administrador de ecommerce
Quiero:      Que al actualizar una regla, se publique evento con nuevos datos hacia Engine
Para:        Que Engine sincronice los cambios en engine_rules y caché Caffeine

Prioridad:   Alta
Estimación:  M
Dependencias: HU-15.1
Capa:        Backend (Admin + Engine)
```

#### Criterios de Aceptación — HU-15.2

**Happy Path: Actualizar regla FIDELITY**
```gherkin
CRITERIO-15.2.1: Actualizar regla FIDELITY y emitir evento RuleUpdated
  Dado que:  Existe una regla FIDELITY con id={ruleId} y discountValue=10.00
  Cuando:    Se envía PUT /api/v1/rules/{ruleId}
             con body { "name": "New Name", "discountValue": 15.00, ... }
  Entonces:  La respuesta es 200 OK
             y se actualiza la regla en admin.rules
             y se publica evento RuleEvent con:
               eventType: "RULE_UPDATED"
               ruleId: {ruleId}
               discountValue: 15.00
               name: "New Name"
               timestamp: {fecha/hora actual UTC}
             y updated_at se actualiza a la hora actual
```

**Happy Path: Actualizar atributos de regla SEASONAL**
```gherkin
CRITERIO-15.2.2: Actualizar start_date/end_date de regla SEASONAL
  Dado que:  Existe una regla SEASONAL con id={ruleId}
             y tiene start_date y end_date
  Cuando:    Se envía PUT /api/v1/rules/{ruleId}
             con body { "attributes": { "start_date": "2026-06-01", "end_date": "2026-07-01" } }
  Entonces:  La respuesta es 200 OK
             y se publica evento RuleEvent con:
               eventType: "RULE_UPDATED"
               logicConditions: {start_date: "2026-06-01", end_date: "2026-07-01", ...}
```

**Happy Path: Actualizar estado activo/inactivo**
```gherkin
CRITERIO-15.2.3: Cambiar estado isActive en evento UPDATE
  Dado que:  Existe una regla con id={ruleId} e isActive=true
  Cuando:    Se envía PUT /api/v1/rules/{ruleId}
             con body { "isActive": false }
  Entonces:  La respuesta es 200 OK
             y se publica evento RuleEvent con:
               eventType: "RULE_UPDATED"
               isActive: false
             y Engine actualiza engine_rules.is_active = false
```

**Error Path: Regla no encontrada**
```gherkin
CRITERIO-15.2.4: Intentar actualizar regla inexistente
  Dado que:  Se intenta actualizar regla con id={invalidRuleId}
  Cuando:    Se envía PUT /api/v1/rules/{invalidRuleId}
             con body válido
  Entonces:  La respuesta es 404 Not Found
             y NO se publica evento
```

**Error Path: Violación de Tenant Isolation**
```gherkin
CRITERIO-15.2.5: Intentar actualizar regla de otro ecommerce
  Dado que:  Un usuario autenticado con ecommerce={ecommerceId}
             intenta actualizar regla que pertenece a otro ecommerce={otherEcommerceId}
  Cuando:    Se envía PUT /api/v1/rules/{ruleId}
  Entonces:  La respuesta es 404 Not Found
             y NO se publica evento (seguridad por omisión)
```

---

#### HU-15.3: Eliminar Regla y Sincronizar a Engine

```
Como:        Administrador de ecommerce
Quiero:      Que al eliminar una regla, se publique evento RULE_DELETED hacia Engine
Para:        Que Engine elimine o marque como inactiva la regla en engine_rules

Prioridad:   Media
Estimación:  S
Dependencias: HU-15.1
Capa:        Backend (Admin + Engine), Soft Delete o Hard Delete según arquitectura
```

#### Criterios de Aceptación — HU-15.3

**Happy Path: Eliminar regla (Soft Delete)**
```gherkin
CRITERIO-15.3.1: Soft-delete de regla y emitir evento RULE_DELETED
  Dado que:  Existe una regla con id={ruleId} y isActive=true
  Cuando:    Se envía DELETE /api/v1/rules/{ruleId}
  Entonces:  La respuesta es 204 No Content
             y se marca regla como isActive=false o se registra en deleted_at
             y se publica evento RuleEvent con:
               eventType: "RULE_DELETED"
               ruleId: {ruleId}
               ecommerceId: {ecommerceId}
               discountTypeCode: {tipo}
               timestamp: {fecha/hora UTC}
```

**Happy Path: Eliminar regla de cada tipo**
```gherkin
CRITERIO-15.3.2: RULE_DELETED aplica a todos los tipos (FIDELITY, SEASONAL, PRODUCT, CLASSIFICATION)
  Dado que:  Existen reglas de los 4 tipos
  Cuando:    Se envía DELETE /api/v1/rules/{ruleId} para cada tipo
  Entonces:  Para cada tipo se publica RuleEvent con eventType="RULE_DELETED"
             y Engine invalida caché para ese ecommerce
```

**Error Path: Regla no encontrada**
```gherkin
CRITERIO-15.3.3: Intentar eliminar regla inexistente
  Dado que:  Se intenta eliminar regla con id={invalidRuleId}
  Cuando:    Se envía DELETE /api/v1/rules/{invalidRuleId}
  Entonces:  La respuesta es 404 Not Found
             y NO se publica evento
```

**Error Path: Regla ya eliminada**
```gherkin
CRITERIO-15.3.4: Eliminar regla ya eliminada (idempotencia)
  Dado que:  Una regla ya fue eliminada (isActive=false o deleted_at != null)
  Cuando:    Se intenta eliminar nuevamente
  Entonces:  La respuesta es 204 No Content (idempotente)
             y se publica evento RuleEvent (auditoría, incluso si ya estaba eliminada)
```

---

### Reglas de Negocio

1. **Evento Genérico Unificado**: Todos los tipos de descuento (FIDELITY, SEASONAL, PRODUCT, CLASSIFICATION) usan el mismo evento `RuleEvent` con campo `eventType` = CREATE|UPDATE|DELETE.

2. **Event Source of Truth**: Admin Service es la única fuente de verdad. Engine Service solo consume y replica.

3. **Atributos Desnormalizados en logicConditions**: Los campos dinámicos de cada regla (product_type, start_date, end_date, min_value, max_value, etc.) se serializan en JSONB `logicConditions` para flexibilidad sin cambios de schema.

4. **Sync Pattern Completo**: 
   - CREATE → Engine insertar fila en engine_rules
   - UPDATE → Engine hacer UPDATE de columnas + logicConditions JSONB
   - DELETE → Engine marcar is_active=false o eliminar fila

5. **Async Processing**: Evento publicado de forma asincrónica sin bloquear el response HTTP. Usa `@RabbitListener` en Engine con AckMode.MANUAL para garantizar procesamiento.

6. **Idempotencia**: Procesar el mismo evento 2+ veces = resultado idéntico. UPDATE por ruleId es idempotente.

7. **Invalidación de Caché**: Cada evento invalida Caffeine cache para el ecommerce_id, forzando reload lazy en próximo cálculo.

8. **Tenant Isolation**: `ecommerceId` en evento y BD para aislar datos por tenant.

9. **Dead Letter Queue**: Eventos malformados (campos null requeridos, etc.) van a DLQ para auditoría y reprocessamiento manual.

10. **Timestamp en UTC**: Todos los `timestamp` en evento en UTC (Instant).

---

## 2. DISEÑO

### Contexto Arquitectónico

El Engine Service mantiene un **clone simplificado** de 5 tablas Admin sincronizadas via RabbitMQ:

| Tabla Admin | Tabla Engine | Evento | Características |
|---|---|---|---|
| `api_keys` | `engine_api_keys` | ApiKeyEvent | Hash, ecommerce_id, is_active |
| `discount_settings` | `engine_discount_settings` | DiscountSettingsEvent | Config negocio |
| `discount_priorities` | `engine_discount_priorities` | DiscountPriorityEvent | Prioridades |
| `customer_tiers` | `engine_customer_tiers` | CustomerTierEvent | Fidelidad |
| `rules` (todas) + `rule_attribute_values` | `engine_rules` (JSONB) | **`RuleEvent` (genérico)** | Todas las reglas |

**Nueva en SPEC-010**: Campo `discount_type_code` en `RuleEvent` permite un consumer único.

### Modelos de Datos

#### Tabla: `engine_rules` (Replica Simplificada) — Ya Existe

```sql
CREATE TABLE IF NOT EXISTS engine_rules (
    id UUID PRIMARY KEY,
    ecommerce_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    discount_type_code VARCHAR(50) NOT NULL,  -- FIDELITY, SEASONAL, PRODUCT, CLASSIFICATION
    discount_type VARCHAR(50) NOT NULL,
    discount_value DECIMAL(12,4) NOT NULL,
    applied_with VARCHAR(50) NOT NULL DEFAULT 'INDIVIDUAL',
    logic_conditions JSONB NOT NULL,  -- {start_date, end_date, product_type, min_value, max_value, ...}
    priority_level INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    synced_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT ck_discount_value CHECK (discount_value > 0),
    CONSTRAINT ck_priority_level CHECK (priority_level > 0),
    CONSTRAINT uk_ecommerce_priority_type UNIQUE(ecommerce_id, priority_level, discount_type_code)
);

CREATE INDEX IF NOT EXISTS idx_engine_rules_ecommerce ON engine_rules(ecommerce_id);
CREATE INDEX IF NOT EXISTS idx_engine_rules_type ON engine_rules(discount_type_code);
CREATE INDEX IF NOT EXISTS idx_engine_rules_active ON engine_rules(is_active);
```

**Campos nuevos o adaptados**:
- `discount_type_code`: Almacena FIDELITY|SEASONAL|PRODUCT|CLASSIFICATION
- `logic_conditions` (JSONB): Flexible para atributos de cada tipo sin columnas adicionales

---

### Event DTO: `RuleEvent` (Record)

**Ubicación**: `backend/service-engine/src/main/java/com/loyalty/service_engine/application/dto/events/RuleEvent.java`

```java
package com.loyalty.service_engine.application.dto.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Generic unified event for all rule types (FIDELITY, SEASONAL, PRODUCT, CLASSIFICATION).
 * Published by Admin Service RuleService on CREATE, UPDATE, DELETE operations.
 * Consumed by Engine Service RuleEventConsumer to sync engine_rules table.
 *
 * SPEC-010: Unifies CLASSIFICATION, PRODUCT, SEASONAL, FIDELITY rule events.
 * Replaces individual ClassificationRuleCreatedEvent, ProductRuleEvent, etc.
 *
 * Example payload:
 * {
 *   "eventType": "RULE_CREATED",
 *   "ruleId": "550e8400-e29b-41d4-a716-446655440000",
 *   "ecommerceId": "550e8400-e29b-41d4-a716-446655440001",
 *   "name": "Summer Discount FIDELITY",
 *   "description": "20% discount for Gold tier",
 *   "discountTypeCode": "FIDELITY",
 *   "discountValue": 20.00,
 *   "priorityLevel": 1,
 *   "logicConditions": {
 *     "tierCode": "GOLD",
 *     "minSpend": 1000.00,
 *     "applicableTo": "all_categories"
 *   },
 *   "isActive": true,
 *   "appliedWith": "INDIVIDUAL",
 *   "timestamp": "2026-04-08T15:30:45Z"
 * }
 */
public record RuleEvent(
    String eventType,                      // "RULE_CREATED" | "RULE_UPDATED" | "RULE_DELETED"
    UUID ruleId,                           // Rule ID (uuid from Admin)
    UUID ecommerceId,                      // Tenant ID (multi-tenant isolation)
    String name,                           // Rule display name
    String description,                    // Rule description (nullable)
    String discountTypeCode,               // "FIDELITY" | "SEASONAL" | "PRODUCT" | "CLASSIFICATION"
    BigDecimal discountValue,              // Discount amount or percentage
    Integer priorityLevel,                 // Evaluation order (1, 2, 3, ...)
    Map<String, Object> logicConditions,   // Flexible JSONB: {start_date, end_date, product_type, min_value, ...}
    Boolean isActive,                      // true = active, false = inactive
    String appliedWith,                    // "INDIVIDUAL" | "STACKED" (optional, defaults to "INDIVIDUAL")
    Instant timestamp                      // UTC timestamp of event
) {
}
```

**Notas**:
- `eventType`: Identificador del tipo de evento (no es enum para flexibilidad JSON deserialization)
- `logicConditions`: JSONB flexible que contiene atributos específicos de cada tipo
- `timestamp`: UTC Instant para auditoría y reproducibilidad

---

### API Endpoints (Admin Service)

#### POST /api/v1/rules — CREATE

```
POST /api/v1/rules
Authorization: Bearer {token}

Body:
{
  "name": "Gold Tier Discount",
  "description": "Extra 10% for Gold customers",
  "discountPriorityId": "550e8400-e29b-41d4-a716-446655440000",
  "discountValue": 10.00,
  "attributes": {
    "tierCode": "GOLD",
    "minSpend": 1000.00,
    "maxSpend": 50000.00,
    "applicableTo": "all_categories"
  },
  "isActive": true
}

Response 201:
{
  "uid": "550e8400-e29b-41d4-a716-446655440099",
  "name": "Gold Tier Discount",
  "discountTypeCode": "FIDELITY",
  "discountValue": 10.00,
  "priorityLevel": 1,
  "isActive": true,
  "created_at": "2026-04-08T15:30:45Z",
  "updated_at": "2026-04-08T15:30:45Z"
}

Lado del servidor (implícito):
- Publica RuleEvent a RabbitMQ:
  eventType: "RULE_CREATED"
  ruleId: {uid generado}
  discountTypeCode: {resuelto de discountPriorityId}
  logicConditions: {attributes desnormalizados}
  timestamp: {ahora}
```

#### PUT /api/v1/rules/{ruleId} — UPDATE

```
PUT /api/v1/rules/{ruleId}
Authorization: Bearer {token}

Body (parcial, solo campos a actualizar):
{
  "name": "Gold Tier Discount Updated",
  "discountValue": 12.00,
  "attributes": {
    "tierCode": "GOLD",
    "minSpend": 2000.00
  }
}

Response 200:
{
  "uid": "{ruleId}",
  "name": "Gold Tier Discount Updated",
  "discountValue": 12.00,
  "updated_at": "2026-04-08T15:35:20Z"
}

Lado del servidor (implícito):
- Publica RuleEvent a RabbitMQ:
  eventType: "RULE_UPDATED"
  ruleId: {ruleId}
  discountValue: 12.00
  name: "Gold Tier Discount Updated"
  logicConditions: {attributes actualizados}
  timestamp: {ahora}
```

#### DELETE /api/v1/rules/{ruleId} — DELETE

```
DELETE /api/v1/rules/{ruleId}
Authorization: Bearer {token}

Response 204 No Content

Lado del servidor (implícito):
- Marca regla como isActive=false o deleted_at=now()
- Publica RuleEvent a RabbitMQ:
  eventType: "RULE_DELETED"
  ruleId: {ruleId}
  ecommerceId: {ecommerceId de la regla}
  discountTypeCode: {tipo de la regla}
  timestamp: {ahora}
```

---

### Implementación: Admin Service (RuleService)

**Ubicación**: `backend/service-admin/src/main/java/com/loyalty/service_admin/application/service/RuleService.java`

#### Método: createRule() — Emitir RULE_CREATED

```java
public RuleResponse createRule(UUID ecommerceId, RuleCreateRequest request) {
    // ... validaciones existentes ...
    
    RuleEntity savedRule = ruleRepository.save(ruleEntity);
    
    // NEW: Publicar evento RULE_CREATED a RabbitMQ
    publishRuleEvent(
        "RULE_CREATED",
        savedRule,
        ecommerceId,
        serializeLogicConditions(request.attributes()) // JSONB
    );
    
    return mapToResponse(savedRule);
}

private void publishRuleEvent(String eventType, RuleEntity rule, UUID ecommerceId, Map<String, Object> logicConditions) {
    RuleEvent event = new RuleEvent(
        eventType,
        rule.getId(),
        ecommerceId,
        rule.getName(),
        rule.getDescription(),
        resolveDiscountTypeCode(rule.getDiscountTypeId()),
        rule.getDiscountValue(),
        rule.getPriorityLevel(),
        logicConditions,
        rule.isActive(),
        rule.getAppliedWith(),
        Instant.now()
    );
    
    rabbitTemplate.convertAndSend(
        "loyalty.events",           // exchange
        "rule.updated",             // routing key (universal para todos los tipos)
        event
    );
    log.info("Published RuleEvent: eventType={}, ruleId={}, discountTypeCode={}", 
             eventType, rule.getId(), event.discountTypeCode());
}
```

#### Método: updateRule() — Emitir RULE_UPDATED

```java
public RuleResponse updateRule(UUID ecommerceId, UUID ruleId, RuleUpdateRequest request) {
    RuleEntity existingRule = ruleRepository.findById(ruleId)
        .orElseThrow(() -> new ResourceNotFoundException("Rule not found"));
    
    // Validate tenant isolation
    if (!existingRule.getEcommerceId().equals(ecommerceId)) {
        throw new ResourceNotFoundException("Rule not found"); // Hide from other tenants
    }
    
    // Apply updates
    existingRule.setName(request.name());
    existingRule.setDescription(request.description());
    existingRule.setDiscountValue(request.discountValue());
    // ... más actualizaciones ...
    
    RuleEntity updatedRule = ruleRepository.save(existingRule);
    
    // NEW: Publicar evento RULE_UPDATED
    publishRuleEvent(
        "RULE_UPDATED",
        updatedRule,
        ecommerceId,
        serializeLogicConditions(request.attributes())
    );
    
    return mapToResponse(updatedRule);
}
```

#### Método: deleteRule() — Emitir RULE_DELETED

```java
public void deleteRule(UUID ecommerceId, UUID ruleId) {
    RuleEntity rule = ruleRepository.findById(ruleId)
        .orElseThrow(() -> new ResourceNotFoundException("Rule not found"));
    
    if (!rule.getEcommerceId().equals(ecommerceId)) {
        throw new ResourceNotFoundException("Rule not found");
    }
    
    // Soft delete: mark as inactive
    rule.setActive(false);
    rule.setDeletedAt(Instant.now());
    RuleEntity deletedRule = ruleRepository.save(rule);
    
    // NEW: Publicar evento RULE_DELETED
    publishRuleEvent(
        "RULE_DELETED",
        deletedRule,
        ecommerceId,
        serializeLogicConditions(rule.getAttributes()) // last known state
    );
    
    log.info("Rule deleted: ruleId={}, ecommerceId={}", ruleId, ecommerceId);
}
```

---

### Implementación: Engine Service (Unified Consumer)

**Ubicación**: `backend/service-engine/src/main/java/com/loyalty/service_engine/infrastructure/rabbitmq/RuleEventConsumer.java`

```java
package com.loyalty.service_engine.infrastructure.rabbitmq;

import com.loyalty.service_engine.application.dto.events.RuleEvent;
import com.loyalty.service_engine.application.service.EngineRuleService;
import com.loyalty.service_engine.infrastructure.cache.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Unified consumer for all rule types: FIDELITY, SEASONAL, PRODUCT, CLASSIFICATION.
 * 
 * SPEC-010: Handles CREATE, UPDATE, DELETE events from Admin Service.
 * Synchronizes engine_rules table and invalidates Caffeine cache per ecommerce.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RuleEventConsumer {
    
    private final EngineRuleService engineRuleService;
    private final CacheService cacheService;
    
    @RabbitListener(queues = "loyalty.rules.queue")
    public void consume(
        @Payload RuleEvent event,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) {
        try {
            log.info("Received RuleEvent: eventType={}, ruleId={}, discountTypeCode={}, ecommerceId={}",
                     event.eventType(), event.ruleId(), event.discountTypeCode(), event.ecommerceId());
            
            // Validate event
            validateRuleEvent(event);
            
            // Route to appropriate handler
            switch (event.eventType()) {
                case "RULE_CREATED" -> handleRuleCreated(event);
                case "RULE_UPDATED" -> handleRuleUpdated(event);
                case "RULE_DELETED" -> handleRuleDeleted(event);
                default -> {
                    log.warn("Unknown event type: {}", event.eventType());
                    throw new IllegalArgumentException("Unknown eventType: " + event.eventType());
                }
            }
            
            // Invalidate cache for this ecommerce
            cacheService.invalidateRulesCache(event.ecommerceId());
            
            log.info("Successfully processed RuleEvent: ruleId={}, eventType={}", event.ruleId(), event.eventType());
            
        } catch (Exception e) {
            log.error("Error processing RuleEvent: eventType={}, ruleId={}, error={}",
                     event.eventType(), event.ruleId(), e.getMessage(), e);
            // Send to DLQ on error
            throw new RuntimeException("RuleEvent processing failed", e);
        }
    }
    
    private void handleRuleCreated(RuleEvent event) {
        engineRuleService.createOrUpdateEngineRule(
            event.ruleId(),
            event.ecommerceId(),
            event.name(),
            event.description(),
            event.discountTypeCode(),
            event.discountValue(),
            event.priorityLevel(),
            event.logicConditions(),
            event.isActive(),
            event.appliedWith()
        );
    }
    
    private void handleRuleUpdated(RuleEvent event) {
        // UPDATE is idempotent: same as CREATE for Caffeine cache invalidation
        handleRuleCreated(event);
    }
    
    private void handleRuleDeleted(RuleEvent event) {
        engineRuleService.markRuleAsDeleted(event.ruleId(), event.ecommerceId());
    }
    
    private void validateRuleEvent(RuleEvent event) {
        if (event.ruleId() == null || event.ecommerceId() == null || 
            event.discountTypeCode() == null || event.eventType() == null) {
            throw new IllegalArgumentException("RuleEvent missing required fields");
        }
    }
}
```

---

### RabbitMQ Configuration

**Ubicación**: `backend/service-admin/src/main/java/com/loyalty/service_admin/infrastructure/config/RuleEventProducerConfig.java`

```java
package com.loyalty.service_admin.infrastructure.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RuleEventProducerConfig {
    
    // Exchange
    @Bean
    public TopicExchange loyaltyEventExchange() {
        return new TopicExchange("loyalty.events", true, false);
    }
    
    // Queue (for Engine consumer)
    @Bean
    public Queue ruleEventQueue() {
        return QueueBuilder.durable("loyalty.rules.queue")
            .deadLetterExchange("loyalty.dlx")
            .deadLetterRoutingKey("loyalty.dlq")
            .build();
    }
    
    // Binding: loyalty.events exchange -> rule.updated routing key -> loyalty.rules.queue
    @Bean
    public Binding ruleEventBinding(Queue ruleEventQueue, TopicExchange loyaltyEventExchange) {
        return BindingBuilder.bind(ruleEventQueue)
            .to(loyaltyEventExchange)
            .with("rule.*");  // Matches rule.created, rule.updated, rule.deleted (universal)
    }
    
    // Dead Letter Exchange
    @Bean
    public TopicExchange dlExchange() {
        return new TopicExchange("loyalty.dlx", true, false);
    }
    
    // Dead Letter Queue
    @Bean
    public Queue dlQueue() {
        return QueueBuilder.durable("loyalty.dlq").build();
    }
    
    @Bean
    public Binding dlBinding(Queue dlQueue, TopicExchange dlExchange) {
        return BindingBuilder.bind(dlQueue)
            .to(dlExchange)
            .with("#");
    }
}
```

---

### Notas de Implementación

1. **Serialización de Atributos**: Los atributos dinámicos (start_date, end_date, producto_type, etc.) se serializan en JSONB `logicConditions` para evitar cambios de esquema.

2. **Idempotencia**: UPDATE y DELETE son operaciones idempotentes. Procesar el mismo evento 2+ veces produce el mismo estado final.

3. **Async Processing**: Las publicaciones a RabbitMQ son asincrónicas y no bloquean el response HTTP.

4. **Tenant Isolation**: Todos los events incluyen `ecommerceId` para validación de aislamiento de tenants.

5. **Cache Invalidation**: Cada evento invalida Caffeine cache para el ecommerce, forzando reload lazy.

6. **Error Handling**: Eventos malformados van a DLQ para auditoría y reprocessamiento manual.

7. **Dead Letter Queue**: Implementar DLQ para eventos rechazados (null fields requeridos, etc.)

8. **Consolidación de Consumers**: Reemplaza `ClassificationRuleEventConsumer`, `ProductRuleEventListener`, etc. con un único `RuleEventConsumer` genérico.

9. **Migration Path**: Ya existe `engine_rules` con estructura JSONB. No requiere cambios de BD.

10. **Backwards Compatibility**: El nuevo `RuleEventConsumer` coexiste con consumers selectivos por tipo si es necesario durante transición.

---

## 3. LISTA DE TAREAS

> Checklist accionable para implementación, tests y QA.
> El Orchestrator monitorea este checklist.

### Backend (Admin Service)

#### Implementación

- [ ] **DTO: RuleEvent Record**
  - [ ] Crear `RuleEvent` record en `dto/events/`
  - [ ] Incluir campos: eventType, ruleId, ecommerceId, name, description, discountTypeCode, discountValue, priorityLevel, logicConditions, isActive, appliedWith, timestamp
  - [ ] Serialización JSON correcta (Jackson)

- [ ] **RabbitMQ Configuration**
  - [ ] Crear `RuleEventProducerConfig` con exchange `loyalty.events`
  - [ ] Definir routing key `rule.*` (universal para todos los tipos)
  - [ ] Configurar Dead Letter Queue (DLQ)

- [ ] **RuleService: Emitir eventos en CREATE**
  - [ ] En método `createRule()`, después de persistir, publicar evento RULE_CREATED
  - [ ] Serializar `attributes` en `logicConditions` (JSONB)
  - [ ] Validar que no se emita evento si validación falla

- [ ] **RuleService: Emitir eventos en UPDATE**
  - [ ] En método `updateRule()`, después de update, publicar evento RULE_UPDATED
  - [ ] Incluir todos los campos actualizados en el evento
  - [ ] Incluir timestamp UTC en el evento

- [ ] **RuleService: Emitir eventos en DELETE**
  - [ ] En método `deleteRule()`, marcar como isActive=false (soft delete)
  - [ ] Publicar evento RULE_DELETED con último estado conocido
  - [ ] Incluir ecommerceId y discountTypeCode para Engine synchronization

- [ ] **Helper: Serializar Atributos Dinámicos**
  - [ ] Crear método `serializeLogicConditions(Map<String, Object>)` → JSONB String
  - [ ] Crear método `resolveDiscountTypeCode(UUID discountTypeId)` → String (FIDELITY|SEASONAL|PRODUCT|CLASSIFICATION)

- [ ] **Inyección de RabbitTemplate**
  - [ ] Verificar que `RabbitTemplate` está inyectado en RuleService
  - [ ] Usar constructor injection (nunca @Autowired)

#### Tests Backend (Admin)

- [ ] **test_RuleService_createRule_publishesRuleCreateEvent**
  - [ ] Verificar que se publica evento RULE_CREATED
  - [ ] Verificar campos del evento (ruleId, ecommerceId, discountTypeCode)

- [ ] **test_RuleService_updateRule_publishesRuleUpdateEvent**
  - [ ] Verificar que se publica evento RULE_UPDATED
  - [ ] Verificar que cambios se reflejan en el evento

- [ ] **test_RuleService_deleteRule_publishesRuleDeleteEvent**
  - [ ] Verificar que se publica evento RULE_DELETED
  - [ ] Verificar soft delete (isActive=false)

- [ ] **test_RuleService_createRule_invalidData_noEventPublished**
  - [ ] Si validación falla, NO se publica evento

- [ ] **test_RuleService_tenantIsolation_updateOtherEcommerce_throws404**
  - [ ] Intentar actualizar regla de otro ecommerce → 404
  - [ ] NO se publica evento

- [ ] **test_RuleController_post_201_withRuleEvent**
  - [ ] POST /api/v1/rules → 201 Created
  - [ ] Respuesta incluye uid, name, discountTypeCode

- [ ] **test_RuleController_put_200_withRuleEvent**
  - [ ] PUT /api/v1/rules/{ruleId} → 200 OK
  - [ ] updated_at se actualiza

- [ ] **test_RuleController_delete_204_withRuleEvent**
  - [ ] DELETE /api/v1/rules/{ruleId} → 204 No Content
  - [ ] Regla marcada como inactiva

---

### Backend (Engine Service)

#### Implementación

- [ ] **DTO: RuleEvent Record**
  - [ ] Importar `RuleEvent` de DTOs compartidas o replicar en Engine
  - [ ] Asegurar deserialización correcta

- [ ] **Unified Consumer: RuleEventConsumer**
  - [ ] Crear `RuleEventConsumer` en `infrastructure/rabbitmq/`
  - [ ] Implementar `@RabbitListener(queues = "loyalty.rules.queue")`
  - [ ] Routing: RULE_CREATED → handleRuleCreated(), RULE_UPDATED → handleRuleUpdated(), RULE_DELETED → handleRuleDeleted()
  - [ ] Validar evento (null checks)
  - [ ] Invalidar Caffeine cache para ecommerce
  - [ ] Error handling: enviar a DLQ si falla

- [ ] **EngineRuleService Métodos**
  - [ ] `createOrUpdateEngineRule(...)` — INSERT o UPDATE en engine_rules
  - [ ] `markRuleAsDeleted(ruleId, ecommerceId)` — UPDATE is_active=false
  - [ ] Ambos métodos idempotentes

- [ ] **Cache Invalidation**
  - [ ] `CacheService.invalidateRulesCache(ecommerceId)` debe ser llamado en consumer
  - [ ] Invalida caché Caffeine para ese ecommerce

- [ ] **RabbitMQ Listener Configuration**
  - [ ] Configurar `loyalty.rules.queue` con manual ACK
  - [ ] Handler que llama a RuleEventConsumer

#### Tests Backend (Engine)

- [ ] **test_RuleEventConsumer_handleRuleCreated_insertsEngineRule**
  - [ ] Consumir evento RULE_CREATED
  - [ ] Verificar INSERT en engine_rules
  - [ ] Verificar logicConditions JSONB almacenado correctamente

- [ ] **test_RuleEventConsumer_handleRuleUpdated_updatesEngineRule**
  - [ ] Consumir evento RULE_UPDATED
  - [ ] Verificar UPDATE en engine_rules
  - [ ] Verificar updated_at actualizado

- [ ] **test_RuleEventConsumer_handleRuleDeleted_marksAsInactive**
  - [ ] Consumir evento RULE_DELETED
  - [ ] Verificar is_active=false
  - [ ] Verificar cache invalidation

- [ ] **test_RuleEventConsumer_invalidEvent_noRequiredFields_sendsToD LQ**
  - [ ] Evento sin ruleId o ecommerceId
  - [ ] Debe ir a DLQ sin procesar

- [ ] **test_RuleEventConsumer_cacheInvalidation_afterAnyEvent**
  - [ ] Verificar que `CacheService.invalidateRulesCache(ecommerceId)` es llamado
  - [ ] Para CREATE, UPDATE, DELETE

- [ ] **test_RuleEventConsumer_idempotence_sameEventTwice_resultIdentical**
  - [ ] Procesar el mismo evento 2 veces
  - [ ] Verificar que engine_rules tiene 1 fila (no duplicado)
  - [ ] Verificar que estado final es idéntico

- [ ] **test_RuleEventConsumer_multipleTypes_allProcessedCorrectly**
  - [ ] Consumir eventos de FIDELITY, SEASONAL, PRODUCT, CLASSIFICATION
  - [ ] Verificar que todos se sincronizan correctamente
  - [ ] Verificar discount_type_code en engine_rules

- [ ] **test_EngineRuleService_createOrUpdateEngineRule_idempotent**
  - [ ] Llamar 2 veces con mismo ruleId
  - [ ] Verificar que hay 1 fila (UPDATE, no INSERT)

---

### QA / Integration

#### Funcional

- [ ] **Test: Create → Sync → Calculate**
  - [ ] Crear regla FIDELITY en Admin
  - [ ] Verificar que aparece en engine_rules
  - [ ] Verificar que se usa en cálculo de descuento en Engine

- [ ] **Test: Update → Sync → Recalculate**
  - [ ] Actualizar descountValue de regla
  - [ ] Verificar que engine_rules se actualiza
  - [ ] Verificar que cálculo usa nuevo valor

- [ ] **Test: Delete → Sync → NoApply**
  - [ ] Eliminar regla (soft delete)
  - [ ] Verificar que engine_rules.is_active=false
  - [ ] Verificar que cálculo NO aplica regla

- [ ] **Test: All Types Sync**
  - [ ] Crear reglas de cada tipo: FIDELITY, SEASONAL, PRODUCT, CLASSIFICATION
  - [ ] Verificar que todas sincronizan a engine_rules
  - [ ] Verificar discount_type_code correcto en cada una

#### Performance

- [ ] **Load Test: Event Publishing**
  - [ ] Simular 1000 reglas siendo creadas en paralelo
  - [ ] Medir latencia de publishing + consuming
  - [ ] Verificar que cache invalidation no bloquea

- [ ] **Load Test: Cache Invalidation**
  - [ ] 100 eventos simultáneos de actualización
  - [ ] Verificar que Caffeine invalidation es rápida (<100ms)

#### Seguridad

- [ ] **Test: Tenant Isolation**
  - [ ] Admin A intentar actualizar regla de Admin B → 404
  - [ ] Engine solo procesa eventos del tenant correcto

- [ ] **Test: Malformed Event → DLQ**
  - [ ] Simular evento sin ruleId
  - [ ] Verificar que no bloquea sistema
  - [ ] Verificar que va a DLQ para auditoría

---

### Documentation

- [ ] **Actualizar API Documentation** (`ENDPOINTS.md`)
  - [ ] POST /api/v1/rules (ya existe, pero clarificar que emite evento)
  - [ ] PUT /api/v1/rules/{ruleId} (clarificar que emite evento)
  - [ ] DELETE /api/v1/rules/{ruleId} (clarificar que emite evento)

- [ ] **README Architecture**
  - [ ] Documentar flujo: Admin emit evento → Engine consume → sync engine_rules + cache invalidation
  - [ ] Includedir diagrama de flujo FIDELITY|SEASONAL|PRODUCT|CLASSIFICATION

---

## Resumen de Cambios

| Componente | Admin Service | Engine Service |
|---|---|---|
| **Nueva DTO** | `RuleEvent.java` | `RuleEvent.java` (reuse o replica) |
| **Nuevo Publisher Config** | `RuleEventProducerConfig.java` | N/A |
| **Modificado: RuleService** | Emitir eventos CREATED/UPDATED/DELETED | N/A |
| **Nuevo Consumer** | N/A | `RuleEventConsumer.java` (unificado) |
| **Nuevo Service** | N/A | `EngineRuleService.createOrUpdateEngineRule()` + `markRuleAsDeleted()` |
| **RabbitMQ Queues** | Exchange: `loyalty.events` | Queue: `loyalty.rules.queue`, DLQ: `loyalty.dlq` |
| **Cache** | N/A | Invalidar Caffeine por ecommerce |
| **DB Changes** | Ninguno (engine_rules ya existe) | Ninguno (JSONB flexible) |

