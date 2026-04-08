---
id: SPEC-009
status: APPROVED
feature: rule-status-sync-engine
created: 2026-04-08
updated: 2026-04-08
author: spec-generator
version: "1.0"
related-specs: ["SPEC-008"]
---

# Spec: Sincronización de Cambio de Status de Reglas a Engine Service

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de iniciar implementación.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPRECATED

---

## 1. REQUERIMIENTOS

### Descripción
Implementar un consumer RabbitMQ en el Engine Service que escuche cambios de activación/desactivación de reglas publicados por la Admin Service. El Engine Service mantiene un clone simplificado de la BD Admin (arquitectura sin asociaciones complejas para operaciones de lectura rápida): `engine_rules` replicará los cambios de estado, invalidará el caché Caffeine para que los cálculos subsecuentes usen el estado actualizado.

### Requerimiento de Negocio
El requerimiento original de HU-14:

```
Sincronización de Cambio de Estado a Engine
Al activar/desactivar una regla, no se notifica al Engine Service para que actualice su caché.

Implementar:
a) Evento de cambio de estado:
{
  "eventType": "RULE_STATUS_CHANGED",
  "ruleId": "uuid",
  "ecommerceId": "uuid", 
  "isActive": true | false,
  "timestamp": "ISO-8601"
}

b) Consumer en Engine:
Nuevo RuleStatusEventConsumer que actualice engine_rules.is_active
```

### Historias de Usuario

#### HU-14: Sincronizar Cambio de Status de Regla a Engine Service

```
Como:        Especialista de operaciones de Engine Service
Quiero:      Que los cambios de activación/desactivación de reglas se reflejen automáticamente en caché
Para:        Asegurar que los cálculos de descuento usen el estado actual de las reglas

Prioridad:   Alta
Estimación:  S
Dependencias: SPEC-008 (Admin emite evento), HU-11 (Motor de cálculo de descuentos)
Capa:        Backend (Engine Service)
```

#### Criterios de Aceptación — HU-14

**Happy Path: Desactivar regla en Engine**
```gherkin
CRITERIO-14.8: Recibir evento de desactivación y actualizar DB replica
  Dado que:  Admin Service publica evento RuleStatusChangedEvent 
             con ruleId, ecommerceId, newStatus=false
  Cuando:    Engine Service consume el evento desde RabbitMQ
  Entonces:  Se actualiza engine_rules.is_active = false para esa regla
             y el campo updated_at se actualiza a la hora actual (UTC)
             y el Caffeine cache se invalida para el ecommerceId
             y los cálculos subsecuentes NO aplican esta regla
```

**Happy Path: Reactivar regla en Engine**
```gherkin
CRITERIO-14.9: Recibir evento de activación y actualizar DB replica
  Dado que:  Admin Service publica evento RuleStatusChangedEvent 
             con ruleId, ecommerceId, newStatus=true
  Cuando:    Engine Service consume el evento desde RabbitMQ
  Entonces:  Se actualiza engine_rules.is_active = true para esa regla
             y el campo updated_at se actualiza a la hora actual (UTC)
             y el Caffeine cache se invalida para el ecommerceId
             y los cálculos subsecuentes APLICAN esta regla nuevamente
```

**Error Path: Regla inexistente en Engine**
```gherkin
CRITERIO-14.10: Evento para regla no replicada aún
  Dado que:  Un evento RuleStatusChangedEvent llega con ruleId
             pero la regla no existe en engine_rules (replicación pendiente)
  Cuando:    Engine Service consume el evento
  Entonces:  Se log un warning (no error crítico)
             y se continúa sin fallar
             (la regla será replicada en su próximo evento de CREATE/UPDATE desde Admin)
```

**Error Path: Evento malformado**
```gherkin
CRITERIO-14.11: Payload del evento inválido o incompleto
  Dado que:  Se recibe un evento con campos faltantes (ruleId=null, etc.)
  Cuando:    Engine Service intenta procesarlo
  Entonces:  Se log un error de validación
             y el evento se descarta (no se procesa)
             y se envía a Dead Letter Queue (DLQ) para auditoría
```

**Idempotencia**
```gherkin
CRITERIO-14.12: Mismo evento procesado multiple veces
  Dado que:  El mismo evento RuleStatusChanged se recibe 2+ veces (reintento de RabbitMQ)
  Cuando:    Engine Service lo procesa múltiples veces
  Entonces:  El resultado es idéntico (is_active = valor final)
             sin duplicación de registros o cambios erráticos
```

### Reglas de Negocio

1. **Event Source of Truth**: Admin Service es única fuente de verdad. Engine solo es consumer (read-only after sync).
2. **Clone Simplificado**: `engine_rules` es réplica simplificada del Admin:
   - No replicar asociaciones complejas (no foreign keys entre replicas)
   - Atributos dinámicos desnormalizados en JSONB (no table separada engine_rule_attributes)
   - Propósito: operaciones de lectura rápida sin joins complejos
3. **Sync Pattern**: Admin emite eventos para CREATE, UPDATE y status changes. Engine consume y actualiza replica.
4. **Cache Invalidation**: Cada cambio de status (o cualquier atributo) invalida Caffeine cache para ese ecommerce, forzando reload lazy.
5. **Async Processing**: @RabbitListener procesa eventos asincronicamente sin bloquear Admin.
6. **Idempotencia**: Procesar el mismo evento 2+ veces = resultado idéntico. UPDATE por id es idempotente.
7. **Dead Letter Queue**: Eventos inválidos (null fields, etc.) van a DLQ para auditoría.
8. **Read-Only en Engine**: Engine **nunca** inserta nuevas reglas directamente. Solo replica desde Admin.

---

## 2. DISEÑO

### Contexto Arquitectónico: Clone Simplificado

El Engine Service mantiene un **clone simplificado** de la BD Admin con 5 tablas sincronizadas (no una réplica compleja):

| Tabla Admin | Tabla Engine | Sincronización | Características |
|---|---|---|---|
| `api_keys` | `engine_api_keys` | Solo datos necesarios | Hash, ecommerce_id, is_active |
| `discount_settings` | `engine_discount_settings` | Sync completo | Config de negocio |
| `discount_priorities` | `engine_discount_priorities` | Sync completo | Prioridades de descuento |
| `customer_tiers` | `engine_customer_tiers` | Sync completo | Niveles de fidelidad |
| `rules` + `rule_attribute_values` | `engine_rules` (JSONB) | Sync completo, atributos desnormalizados | Reglas simplificadas = lectura rápida |

**Beneficio**: Operaciones de lectura rápida sin joins complejos. Todos los datos que necesita calcular descuentos están en 5 tablas simples.

### Modelos de Datos

#### Tabla: `engine_rules` (Replica Simplificada)

**Propósito**: Snapshot de configuraciones de reglas del Admin para cálculos en tiempo real. No es entidad transaccional; es una copia sincronizada. Los atributos dinámicos se almacenan desnormalizados en JSONB (no en tabla separada) para evitar joins complejos.

**Estructura**:
| Campo | Tipo | Cambios | Descripción |
|-------|------|---------|-------------|
| `id` | UUID | — | PK — Identificador único (sincronizado desde Admin rules.id) |
| `ecommerce_id` | UUID | — | Tenant ID |
| `discount_priority_id` | UUID | — | FK a engine_discount_priorities |
| `name` | String | — | Nombre de la regla |
| `description` | String | — | Descripción opcional |
| `discount_percentage` | BigDecimal | — | Porcentaje 0-100 |
| `priority_level` | Integer | — | Orden de aplicación (1=first, 2=second, etc.) |
| `discount_type_code` | String | — | PRODUCT, SEASONAL, o CLASSIFICATION |
| `is_active` | Boolean | ✅ **ACTUALIZAR** | Flag de activación (true=aplica, false=ignorar) |
| `attributes` | JSONB | — | Atributos dinámicos (product_type, start_date, end_date, etc.) serializados en JSON |
| `created_at` | Instant | — | Timestamp de creación (UTC) |
| `updated_at` | Instant | ✅ **ACTUALIZAR** | Timestamp de última sincronización (UTC) |

**Índices**: `idx_ecommerce_active` para queries frecuentes (rules activas por ecommerce)

**Ejemplo de `attributes` (JSONB)**:
```json
{
  "product_type": "electronics",
  "start_date": "2026-04-01",
  "end_date": "2026-06-30"
}
```

### Evento RabbitMQ (Entrada)

#### RuleStatusChangedEvent (Admin → Engine)
Estructura del evento (publicado por Admin desde SPEC-008):

```java
public record RuleStatusChangedEvent(
    UUID ruleId,           // Règle a sincronizar
    UUID ecommerceId,      // Tenant ID
    Boolean newStatus,     // true = activa, false = inactiva
    Boolean previousStatus,  // Previous status (para auditoría)
    Instant changedAt      // Timestamp UTC cuando cambió en Admin
) {}
```

**Propiedades RabbitMQ**:
- **Exchange**: `loyalty.events` (existente)
- **Routing Key**: `rule.status.changed` (del publisher en SPEC-008)
- **Queue**: `engine-service-rule-status-changes` (nueva, crear si no existe)
- **Formato**: JSON (Spring Boot maneja deserialization)

### Componentes Backend

#### 1. Consumer — `RuleStatusEventConsumer`
**Ubicación**: `infrastructure/rabbitmq/RuleStatusEventConsumer.java`

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class RuleStatusEventConsumer {

    private final EngineRuleRepository engineRuleRepository;
    private final RuleCaffeineCacheService cacheService;

    @RabbitListener(queues = "${rabbitmq.queue.rule-status-changes:engine-service-rule-status-changes}")
    public void handleRuleStatusChanged(RuleStatusChangedEvent event) {
        
        // 1. Validar payload
        if (event == null || event.ruleId() == null || event.ecommerceId() == null) {
            log.error("Received invalid RuleStatusChangedEvent: {}", event);
            return;
        }
        
        try {
            // 2. Buscar regla en DB replica
            EngineRule engineRule = engineRuleRepository
                .findByIdAndEcommerceId(event.ruleId(), event.ecommerceId())
                .orElseGet(() -> {
                    log.warn("Rule {} not found in engine_rules. Skipping status update. " +
                            "Will be replicated on next CREATE/UPDATE event.", event.ruleId());
                    return null;
                });
            
            if (engineRule == null) return;
            
            // 3. Actualizar status
            engineRule.setIsActive(event.newStatus());
            engineRule.setUpdatedAt(Instant.now());
            engineRuleRepository.save(engineRule);
            
            log.info("Rule status updated in engine_rules: ruleId={}, newStatus={}, ecommerceId={}",
                    event.ruleId(), event.newStatus(), event.ecommerceId());
            
            // 4. Invalidar cache para el ecommerce
            cacheService.invalidateEcommerce(event.ecommerceId());
            log.debug("Cache invalidated for ecommerce: {}", event.ecommerceId());
            
        } catch (Exception ex) {
            log.error("Error processing RuleStatusChangedEvent: ruleId={}, ecommerceId={}",
                    event.ruleId(), event.ecommerceId(), ex);
            throw ex; // Reintento automático + DLQ
        }
    }
}
```

#### 2. Repository — `EngineRuleRepository`
Debe tener método helper (si no existe):

```java
Optional<EngineRule> findByIdAndEcommerceId(UUID id, UUID ecommerceId);
```

**Ubicación**: `domain/repository/EngineRuleRepository.java` (probablemente ya existe)

#### 3. Cache Service — `RuleCaffeineCacheService`
Asume método de invalidación:

```java
public void invalidateEcommerce(UUID ecommerceId) {
    // Invalida todas las claves de caché para ese ecommerce
    // Trigger reload lazy en próxima consulta
}
```

**Ubicación**: `application/service/RuleCaffeineCacheService.java` (verificar existencia)

### Configuración RabbitMQ

**En `application.properties` o `application.yml` del Engine Service:**

```properties
# Rule Status Changes Queue
rabbitmq.queue.rule-status-changes=engine-service-rule-status-changes
rabbitmq.exchange.events=loyalty.events
rabbitmq.routing.rule-status-changed=rule.status.changed

# Dead Letter Queue (reintentosfallos)
rabbitmq.queue.rule-status-changes-dlq=engine-service-rule-status-changes.dlq
rabbitmq.exchange.rule-status-dlx=loyalty.rule-status.dlx
rabbitmq.routing.rule-status-changed-dlq=rule.status.changed.dlq
```

**Bean de Configuración** (crear si no existe):

```java
@Configuration
public class RuleStatusEventConfig {

    @Bean
    public Queue ruleStatusQueue() {
        return QueueBuilder
            .durable("engine-service-rule-status-changes")
            .withArgument("x-dead-letter-exchange", "loyalty.rule-status.dlx")
            .build();
    }

    @Bean
    public Binding ruleStatusBinding() {
        return BindingBuilder
            .bind(ruleStatusQueue())
            .to(TopicExchange.of("loyalty.events"))
            .with("rule.status.changed");
    }

    @Bean
    public Queue ruleStatusDLQ() {
        return QueueBuilder.durable("engine-service-rule-status-changes.dlq").build();
    }

    @Bean
    public DirectExchange ruleStatusDLX() {
        return new DirectExchange("loyalty.rule-status.dlx", true, false);
    }

    @Bean
    public Binding ruleStatusDLQBinding() {
        return BindingBuilder
            .bind(ruleStatusDLQ())
            .to(ruleStatusDLX())
            .with("rule.status.changed.dlq");
    }
}
```

### Arquitectura y Dependencias

#### Paquetes / Clases nuevas
- `infrastructure/rabbitmq/RuleStatusEventConsumer.java` — Consumer listener
- `infrastructure/config/RuleStatusEventConfig.java` — RabbitMQ bindings (si no existe)

#### Dependencias modificadas
- `application.properties` — nuevas keys de configuración RabbitMQ
- Potencialmente `RuleCaffeineCacheService` — verificar método `invalidateEcommerce()`

#### Servicios externos
- **RabbitMQ**: Consume desde exchange `loyalty.events` routing key `rule.status.changed`
- **DB Replica**: Lee/escribe en tabla `engine_rules` (PostgreSQL)
- **Caffeine Cache**: Invalidación de caché en memoria

#### Notas de Implementación
> - Seguir patrón existente de `ClassificationRuleEventConsumer`
> - Usar `@RabbitListener(queues = "...")` con `@Value` para propiedad de nombre de queue
> - No ejecutar queries costosas dentro del listener (async, fire-and-forget)
> - `EngineRuleRepository.findByIdAndEcommerceId()` debe retornar Optional (es método estándar CRUD)
> - `RuleCaffeineCacheService.invalidateEcommerce(ecommerceId)` invalida todas las keys del caché para ese ecommerce
> - Estructura JSONB: los atributos dinámicos se almacenan como JSON string, no como table normalizadas
> - Dead Letter Queue: Spring auto-enviará después de reintentos configurados en RabbitMQ redelivery policy
> - Logging importante: ruleId+ecommerceId en todos los logs para auditoría
> - No Rollback: No lanzar excepciones no-recoverable que bloqueen el listener. Logging + low-level retries.

---

## 3. LISTA DE TAREAS

### Backend (Engine Service)

#### Implementación
- [ ] Crear/verificar DTO `RuleStatusChangedEvent` en `application/dto/rules/` (debe ser compatible con Admin SPEC-008)
- [ ] Crear consumer `RuleStatusEventConsumer` en `infrastructure/rabbitmq/`
- [ ] Crear/verificar `EngineRuleRepository.findByIdAndEcommerceId()` en `domain/repository/`
- [ ] Verificar/crear `RuleCaffeineCacheService.invalidateEcommerce(ecommerceId)` en `application/service/`
- [ ] Crear bean de configuración para queue/exchange/binding en `infrastructure/config/RuleStatusEventConfig.java`
- [ ] Agregar propiedades RabbitMQ en `application.properties` (queues, exchange, routing keys)
- [ ] Verificar que tabla `engine_rules` existe con:
  - Columnas: `id`, `ecommerce_id`, `discount_priority_id`, `name`, `description`, `discount_percentage`, `priority_level`, `discount_type_code`, `is_active`, `attributes` (JSONB), `created_at`, `updated_at`
  - Índice: `idx_ecommerce_active` para queries rápidas

#### Tests Backend
- [ ] `test_consumer_rule_deactivated_updates_db_and_invalidates_cache` — desactivar
- [ ] `test_consumer_rule_activated_updates_db_and_invalidates_cache` — activar
- [ ] `test_consumer_rule_not_found_logs_warning_no_error` — regla no existe
- [ ] `test_consumer_invalid_event_payload_discarded` — evento malformado
- [ ] `test_consumer_idempotent_same_event_twice` — procesado 2 veces = mismo resultado
- [ ] `test_consumer_event_sent_to_dlq_on_failure` — manejo DLQ

### QA
- [ ] Ejecutar skill `/gherkin-case-generator` → criterios CRITERIO-14.8 a 14.12
- [ ] Ejecutar skill `/risk-identifier` → clasificación ASD de riesgos
- [ ] Verificar integración con Admin (SPEC-008): evento llega a Engine
- [ ] Validar que cache se invalida correctamente post-sincronización
- [ ] Prueba de estrés: múltiples eventos simultáneos

### Documentación (opcional)
- [ ] Actualizar diagrama arquitectura RabbitMQ con nuevo consumer
- [ ] Documentar flow de sincronización Reglas en README
- [ ] Agregar Swagger/OpenAPI si se documenta (no aplica para consumer)
