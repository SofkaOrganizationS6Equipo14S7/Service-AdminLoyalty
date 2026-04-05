---
id: SPEC-011
status: APPROVED
feature: cart-calculation-discounts
created: 2026-04-05
updated: 2026-04-05
author: spec-generator
version: "1.0"
related-specs:
  - SPEC-010 (loyalty-tiers-classification)
  - SPEC-012 (discount-history)
---

# Spec: Cart Calculation — Cálculo de Carrito con Descuentos Aplicados (HU-11)

> **Estado:** `APPROVED` — Listo para implementación.
> **Ciclo de vida:** APPROVED → IN_PROGRESS → IMPLEMENTED

> **Contexto:** Esta HU es el core del motor de descuentos. Recibe un carrito con items + cliente, consulta la clasificación (HU-10), evalúa reglas desde `engine_rules` con prioridades, aplica descuentos según `applied_with` (INDIVIDUAL/CUMULATIVE/EXCLUSIVE), respeta caps en `engine_discount_settings`, y finalmente registra el resultado en `transaction_logs` (HU-12).

---

## 1. REQUERIMIENTOS

### Descripción
Implementar un endpoint que calcule el precio final de un carrito aplicando múltiples descuentos en orden de prioridad. El sistema evaluará reglas consolidadas en `engine_rules` usando criterios JSONB dinámicos, aplicará descuentos según su tipo (PERCENTAGE|FIXED_AMOUNT) y modo de aplicación (INDIVIDUAL|CUMULATIVE|EXCLUSIVE), respetará el tope máximo de descuento (`max_discount_cap`), y retornará un desglose detallado de todas las reglas aplicadas. El resultado se registrará automáticamente en `transaction_logs` para auditoría.

### Requerimiento de Negocio
Como plataforma de e-commerce, quiero calcular descuentos dinámicos sobre un carrito en tiempo real, aplicando múltiples reglas en orden de prioridad (fidelidad, estacional, producto, clasificación), respetando configuración global de caps y stacking, para que el cliente vea el precio final correcto y el Admin pueda auditar qué descuentos se aplicaron.

### Historias de Usuario

#### HU-11: Cálculo de Carrito con Descuentos Aplicados

```
Como:        Motor de Cálculo del Engine Service
Quiero:      Calcular el precio final de un carrito aplicando múltiples descuentos con prioridades y reglas dinámicas
Para:        Retornar al e-commerce un breakdown completo de subtotal, descuentos, topes aplicados y precio final

Prioridad:   Alta
Estimación:  XL
Dependencias: HU-10 (Clasificación debe funcionar), engine_rules con logic_conditions, engine_discount_priorities
Capa:        Backend (Engine Service)
```

#### Criterios de Aceptación — HU-11

**CRITERIO-11.1: Cálculo basic con carrito sin descuentos**
```gherkin
Dado que:      carrito con items:
               | product_id | quantity | unit_price |
               | prod-1     | 2        | 100.00     |
               | prod-2     | 1        | 50.00      |
Y:             no hay reglas activas o cliente no califica
Cuando:        se realiza POST /api/v1/engine/calculate
Entonces:      el sistema retorna 200 OK con:
               { 
                 "subtotal": 250.00,
                 "discount_calculated": 0.00,
                 "discount_applied": 0.00,
                 "final_amount": 250.00,
                 "applied_rules": [],
                 "was_capped": false
               }
```

**CRITERIO-11.2: Aplicación de descuento INDIVIDUAL (PERCENTAGE)**
```gherkin
Dado que:      carrito subtotal = 1000.00
Y:             existe regla activa:
               {
                 "name": "VIP Fidelity 15%",
                 "discount_type_code": "FIDELITY",
                 "discount_type": "PERCENTAGE",
                 "discount_value": 15.00,
                 "applied_with": "INDIVIDUAL",
                 "logic_conditions": { "min_spent": { "type": "NUMERIC", "value": 500 } }
               }
Y:             cliente total_spent = 2000 (cumple min_spent)
Cuando:        se calcula el carrito
Entonces:      descuento = 1000.00 * 15% = 150.00
Y:             final_amount = 1000.00 - 150.00 = 850.00
Y:             applied_rules contiene la regla con descuento_percentage: 15.0
```

**CRITERIO-11.3: Aplicación de descuento INDIVIDUAL (FIXED_AMOUNT)**
```gherkin
Dado que:      carrito subtotal = 500.00
Y:             existe regla: "Promo Electrónica $50 off"
               {
                 "discount_type": "FIXED_AMOUNT",
                 "discount_value": 50.00,
                 "applied_with": "INDIVIDUAL"
               }
Cuando:        se calcula
Entonces:      descuento = 50.00 (monto fijo)
Y:             final_amount = 500.00 - 50.00 = 450.00
```

**CRITERIO-11.4: CUMULATIVE: Dos descuentos se suman (10% + 5%)**
```gherkin
Dado que:      carrito subtotal = 1000.00
Y:             regla #1 (priority=1, CUMULATIVE): 10% Fidelity
               regla #2 (priority=2, CUMULATIVE): 5% Estacional
Y:             cliente califica para ambas
Cuando:        se calcula
Entonces:      descuento_total = (1000 * 10%) + (1000 * 5%) = 150.00
Y:             final_amount = 1000.00 - 150.00 = 850.00
Y:             applied_rules contiene ambas con percentages = [10.0, 5.0]
```

**CRITERIO-11.5: EXCLUSIVE: Descuento de mayor valor reemplaza otros**
```gherkin
Dado que:      carrito subtotal = 1000.00
Y:             regla #1 (priority=1, INDIVIDUAL): 5% Fidelity = 50.00
               regla #2 (priority=2, EXCLUSIVE): 10% Black Friday = 100.00
Y:             cliente califica para ambas
Y:             engine_discount_settings.allow_stacking = true (permite mezcla)
Cuando:        se calcula
Entonces:      descuento_final = 100.00 (el EXCLUSIVE gana)
Y:             applied_rules muestra EXCLUSIVE con mayor descuento
Y:             final_amount = 1000.00 - 100.00 = 900.00
```

**CRITERIO-11.6: max_discount_cap: Descuento limitado a tope máximo**
```gherkin
Dado que:      carrito subtotal = 10000.00
Y:             engine_discount_settings.max_discount_cap = 500.00
Y:             regla: 20% Descuento = 2000.00 (supera el tope)
Cuando:        se calcula
Entonces:      descuento_calculated = 2000.00 (lo que matemáticamente se calculó)
Y:             descuento_applied = 500.00 (limitado por cap)
Y:             was_capped = true
Y:             final_amount = 10000.00 - 500.00 = 9500.00
Y:             el response indica que fue capeado: { "was_capped": true, "cap_reason": "max_discount_cap" }
```

**CRITERIO-11.7: allow_stacking=false: Solo un descuento (el de mayor valor)**
```gherkin
Dado que:      engine_discount_settings.allow_stacking = false
Y:             carrito subtotal = 1000.00
Y:             regla #1: 5% Fidelity = 50.00
               regla #2: 10% Seasonal = 100.00
Y:             cliente califica para ambas (CUMULATIVE o INDIVIDUAL)
Cuando:        se calcula
Entonces:      se aplica solo el descuento mayor: 100.00
Y:             final_amount = 1000.00 - 100.00 = 900.00
Y:             applied_rules contiene solo regla #2
```

**CRITERIO-11.8: Redondeo de moneda (rounding_rule)**
```gherkin
Dado que:      engine_discount_settings.rounding_rule = "ROUND_HALF_UP"
Y:             carrito subtotal = 333.33
Y:             descuento = 33.333... (resultado con decimales)
Cuando:        se calcula
Entonces:      descuento redondeado = 33.33 (2 decimales, ROUND_HALF_UP)
Y:             final_amount = 333.33 - 33.33 = 300.00
```

**CRITERIO-11.9: Validación de carrito**
```gherkin
Dado que:      carrito vacío o sin items
Cuando:        POST /api/v1/engine/calculate
Entonces:      retorna 400 Bad Request
Y:             mensaje: "Carrito sin items"
```

**CRITERIO-11.10: Respuesta con desglose completo de reglas**
```gherkin
Dado que:      se aplican 3 reglas exitosamente
Cuando:        se calcula
Entonces:      response contiene applied_rules[] con:
               [
                 {
                   "rule_id": "uuid-1",
                   "rule_name": "VIP Fidelity 15%",
                   "discount_type_code": "FIDELITY",
                   "applied_with": "INDIVIDUAL",
                   "discount_percentage": 15.0,
                   "discount_amount": 150.00
                 },
                 ...
               ]
```

**CRITERIO-11.11: No evaluar reglas inactivas**
```gherkin
Dado que:      existe regla con is_active = false
Cuando:        se calcula
Entonces:      la regla inactiva se ignora completamente
Y:             no aparece en applied_rules
```

**CRITERIO-11.12: Validación de API Key**
```gherkin
Dado que:      e-commerce envía request sin API Key o clave inválida
Cuando:        POST /api/v1/engine/calculate
Entonces:      retorna 401 Unauthorized
Y:             mensaje: "Invalid or missing API Key"
```

### Reglas de Negocio

1. **Prioridades de descuentos**: Se evalúan en orden de `priority_level` en `engine_discount_priorities` (1=primero, N=último).
2. **Modos de aplicación**:
   - `INDIVIDUAL`: Solo esta regla
   - `CUMULATIVE`: Se suma con otras del mismo tipo
   - `EXCLUSIVE`: Reemplaza a otras (si mayor)
3. **max_discount_cap**: Límite máximo absoluto de descuento por transacción; se registra en `transaction_logs.was_capped`.
4. **allow_stacking**: Si FALSE, solo se aplica el descuento de mayor valor.
5. **Redondeo**: Se aplica según `rounding_rule` (ROUND_HALF_UP, FLOOR, CEIL) a 2 decimales.
6. **Determinismo**: Mismo carrito + cliente = Mismo resultado.
7. **No evaluar inactivas**: Reglas con `is_active=false` se ignoran.
8. **Integridad financiera**: `final_amount = subtotal - discount_applied` debe cumplirse siempre.
9. **Registro automático**: Cada cálculo se registra en `transaction_logs` con `external_order_id` único.

---

## 2. DISEÑO

### Modelos de Datos

#### Entidades afectadas

| Entidad | Almacén | Cambios | Descripción |
|---------|---------|---------|-------------|
| `EngineRuleEntity` | tabla `engine_rules` | consultada | Reglas con criterios, prioridades y tipos de descuento |
| `DiscountPriorityEntity` | tabla `engine_discount_priorities` | consultada | Orden de evaluación por tipo y ecommerce |
| `DiscountSettingsEntity` | tabla `engine_discount_settings` | consultada | Configuración global (cap, stacking, redondeo) |
| `TransactionLogEntity` | tabla `transaction_logs` | escrita | Auditoría de cálculos (sin PII) |

#### Estructura de Request — `DiscountCalculateRequestV2`

| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `ecommerce_id` | UUID | sí | FK válido | Identifica e-commerce |
| `external_order_id` | string | sí | unique, max 255 | ID externo del pedido (para auditoría) |
| `customer_id` | string | sí | max 100 | ID externo del cliente (no se almacena en logs) |
| `total_spent` | decimal(12,4) | sí | >= 0 | Gasto histórico del cliente (para clasificación HU-10) |
| `order_count` | integer | sí | >= 0 | Cantidad histórica de órdenes |
| `membership_days` | integer | sí | >= 0 | Días de membresía |
| `items[]` | array | sí | min 1 | Array de items del carrito |
| `items[].product_id` | string | sí | max 100 | ID del producto |
| `items[].quantity` | integer | sí | > 0 | Cantidad de unidades |
| `items[].unit_price` | decimal(12,4) | sí | >= 0 | Precio unitario |
| `items[].category` | string | no | max 100 | Categoría para criterios de productos |

#### Estructura de Response — `DiscountCalculateResponseV2`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `subtotal_amount` | decimal(12,4) | Suma de (quantity * unit_price) |
| `discount_calculated` | decimal(12,2) | Descuento antes de aplicar cap |
| `discount_applied` | decimal(12,2) | Descuento final (respeta cap) |
| `final_amount` | decimal(12,2) | subtotal - discount_applied |
| `customer_tier` | string | Tier del cliente (ej. "Gold") |
| `was_capped` | boolean | true si se aplicó max_discount_cap |
| `cap_reason` | string | "max_discount_cap" o null |
| `applied_rules[]` | array | Desglose de reglas aplicadas (ver abajo) |
| `transaction_id` | UUID | ID de registro en transaction_logs |
| `calculated_at` | timestamp UTC | Momento del cálculo |

#### Estructura de applied_rules[] Item

```json
{
  "rule_id": "550e8400-e29b-41d4-a716-446655440000",
  "rule_name": "VIP Fidelity 15%",
  "discount_type_code": "FIDELITY",
  "discount_type": "PERCENTAGE",
  "applied_with": "INDIVIDUAL",
  "discount_percentage": 15.0,
  "discount_amount": 150.00,
  "priority_level": 1
}
```

#### Índices / Constraints

- **`engine_rules`**:
  - PK: `id`
  - Index: `(ecommerce_id, discount_type_code, priority_level, is_active)` para búsqueda rápida

- **`engine_discount_priorities`**:
  - PK: `id`
  - Index: `(ecommerce_id, priority_level)` para orden de evaluación
  - Unique: `(ecommerce_id, discount_type_code)`

- **`engine_discount_settings`**:
  - PK: `ecommerce_id`

- **`transaction_logs`**:
  - PK: `id`
  - Index: `(ecommerce_id, created_at DESC)` para búsqueda en historial (HU-12)
  - Index: `(created_at, expires_at)` para limpieza automática
  - Unique: `(ecommerce_id, external_order_id)` para evitar duplicados

### API Endpoints

#### POST /api/v1/engine/calculate

- **Descripción**: Calcula el precio final de un carrito aplicando descuentos dinámicos
- **Auth requerida**: API Key (header: `X-API-Key`)
- **Request Body**: `DiscountCalculateRequestV2` (JSON)

```json
{
  "ecommerce_id": "550e8400-e29b-41d4-a716-446655440000",
  "external_order_id": "order-20260405-12345",
  "customer_id": "cust-xyz-789",
  "total_spent": 2500.00,
  "order_count": 15,
  "membership_days": 180,
  "items": [
    {
      "product_id": "prod-1",
      "quantity": 2,
      "unit_price": 100.00,
      "category": "electronics"
    },
    {
      "product_id": "prod-2",
      "quantity": 1,
      "unit_price": 50.00,
      "category": "accessories"
    }
  ]
}
```

- **Response 200 OK**:
  ```json
  {
    "subtotal_amount": 250.00,
    "discount_calculated": 37.50,
    "discount_applied": 37.50,
    "final_amount": 212.50,
    "customer_tier": "Gold",
    "was_capped": false,
    "applied_rules": [
      {
        "rule_id": "uuid-1",
        "rule_name": "Gold Fidelity 15%",
        "discount_type_code": "FIDELITY",
        "discount_type": "PERCENTAGE",
        "applied_with": "INDIVIDUAL",
        "discount_percentage": 15.0,
        "discount_amount": 37.50,
        "priority_level": 1
      }
    ],
    "transaction_id": "txn-550e8400-e29b-41d4",
    "calculated_at": "2026-04-05T14:35:00Z"
  }
  ```

- **Response 400 Bad Request**: Carrito inválido
  ```json
  {
    "error": "INVALID_REQUEST",
    "message": "Carrito inválido",
    "details": [
      { "field": "items", "message": "Se requiere al menos 1 item" }
    ]
  }
  ```

- **Response 401 Unauthorized**: API Key inválida

- **Response 503 Service Unavailable**: Cache o DB no disponible

---

### Arquitectura y Servicios

#### `DiscountCalculationServiceV2`
**Responsabilidad**: Core de cálculo de descuentos.

```java
public class DiscountCalculationServiceV2 {
    
    public DiscountCalculateResponseV2 calculate(DiscountCalculateRequestV2 request) {
        // 1. Validar request y calcular subtotal
        // 2. Consultar clasificación del cliente (HU-10)
        // 3. Consultar DiscountSettings para este ecommerce
        // 4. Consultar DiscountPriorities para orden de evaluación
        // 5. Para cada prioridad:
        //    - Consultar reglas de engine_rules
        //    - Evaluar logic_conditions JSONB
        //    - Evaluar si regla aplica al carrito y cliente
        //    - Agregar descuento según applied_with
        // 6. Aplicar max_discount_cap
        // 7. Aplicar allow_stacking si FALSE
        // 8. Redondear según rounding_rule
        // 9. Guardar en transaction_logs
        // 10. Retornar response con desglose
    }
}
```

#### `DiscountCappingEngine`
**Responsabilidad**: Aplicar topes y validaciones.

```java
public class DiscountCappingEngine {
    
    public DiscountResult applyCap(
        BigDecimal discountCalculated,
        BigDecimal maxCap,
        CurrencyCode currency
    ) {
        // Retorna { discount_applied, was_capped, cap_reason }
    }
    
    public BigDecimal applyRounding(
        BigDecimal amount,
        RoundingRule rule,
        int scale
    ) {
        // Retorna monto redondeado
    }
}
```

#### `DiscountPriorityEvaluator`
**Responsabilidad**: Evaluar reglas en orden de prioridad.

```java
public class DiscountPriorityEvaluator {
    
    public List<AppliedRule> evaluateByPriority(
        DiscountCalculateRequestV2 request,
        List<DiscountPriorityEntity> priorities,
        DiscountSettingsEntity settings
    ) {
        // 1. Obtener reglas por cada prioridad
        // 2. Evaluar logic_conditions contra request
        // 3. Calcular descuentos (PERCENTAGE o FIXED_AMOUNT)
        // 4. Aplicar lógica de INDIVIDUAL/CUMULATIVE/EXCLUSIVE
        // 5. Retornar lista de reglas aplicadas ordenadas por prioridad
    }
}
```

#### `LogicConditionEvaluator`
**Responsabilidad**: Evaluar criterios JSONB dinámicos.

```java
public class LogicConditionEvaluator {
    
    public boolean evaluateCondition(
        JsonNode logicConditionsJsonb,
        DiscountCalculateRequestV2 request,
        ClassificationResult customerTier
    ) {
        // Parsear JSONB: { "min_spent": {...}, "categories": {...}, "evaluation_logic": {...} }
        // Evaluar cada criterio:
        //   - NUMERIC: comparar total_spent, subtotal, etc.
        //   - ARRAY: verificar que items.category está en array
        //   - STRING: comparar tier nombre
        //   - DATE_RANGE: verificar fecha de compra
        //   - INTEGER: comparar order_count, membership_days
        // Combinar con evaluation_logic si existe (AND, OR)
        // Retornar boolean resultado
    }
}
```

#### `TransactionLogWriter`
**Responsabilidad**: Escribir auditoría en `transaction_logs`.

```java
public class TransactionLogWriter {
    
    public TransactionLogEntity writeLog(
        DiscountCalculateRequestV2 request,
        DiscountCalculateResponseV2 response,
        List<AppliedRule> appliedRules
    ) {
        // Crear TransactionLogEntity:
        // - external_order_id (de request)
        // - subtotal_amount, discount_calculated, discount_applied, final_amount
        // - was_capped (boolean)
        // - applied_rules_json (JSONB serializado)
        // - customer_tier (clasificación)
        // - client_metrics_json (total_spent, order_count, membership_days sin PII)
        // - calculated_at, created_at, expires_at (calculated_at + 7 days)
        // Guardar en DB con transacción
    }
}
```

### DTOs

#### Request — `DiscountCalculateRequestV2`
```java
public record DiscountCalculateRequestV2(
    UUID ecommerceId,
    String externalOrderId,
    String customerId,
    BigDecimal totalSpent,
    Integer orderCount,
    Integer membershipDays,
    List<CartItemRequest> items
) { }

public record CartItemRequest(
    String productId,
    Integer quantity,
    BigDecimal unitPrice,
    String category  // opcional
) { }
```

#### Response — `DiscountCalculateResponseV2`
```java
public record DiscountCalculateResponseV2(
    BigDecimal subtotalAmount,
    BigDecimal discountCalculated,
    BigDecimal discountApplied,
    BigDecimal finalAmount,
    String customerTier,
    Boolean wasCapped,
    String capReason,
    List<AppliedRuleDetail> appliedRules,
    UUID transactionId,
    Instant calculatedAt
) { }

public record AppliedRuleDetail(
    UUID ruleId,
    String ruleName,
    String discountTypeCode,
    String discountType,
    String appliedWith,
    BigDecimal discountPercentage,
    BigDecimal discountAmount,
    Integer priorityLevel
) { }
```

---

## 3. LISTA DE TAREAS

### Backend (Engine Service)

#### 3.1 Entidades y Repositorios
- [ ] Confirmar existencia de `EngineRuleEntity` con campos: discount_type, discount_value, applied_with, logic_conditions, priority_level
- [ ] Confirmar existencia de `DiscountPriorityEntity` con índices (ecommerce_id, priority_level)
- [ ] Confirmar existencia de `DiscountSettingsEntity` con max_discount_cap, allow_stacking, rounding_rule
- [ ] Crear/Confirmar `TransactionLogEntity` con estructura completa (ver ENGINE_DATABASE_FINAL_SPECIFICATION.md)
- [ ] Crear repositorios:
  - `EngineRuleRepository extends JpaRepository`
  - `TransactionLogRepository extends JpaRepository`

#### 3.2 Servicio Core de Cálculo
- [ ] Crear `DiscountCalculationServiceV2` en `application/service/`
- [ ] Implementar método `calculate(DiscountCalculateRequestV2)` con flujo completo
- [ ] Integrar clasificación de cliente (llamar `FidelityClassificationService` de HU-10)
- [ ] Integrar cálculo de subtotal desde items del carrito
- [ ] Integrar validación de carrito (min 1 item)

#### 3.3 Evaluador de Prioridades
- [ ] Crear `DiscountPriorityEvaluator` en `application/service/`
- [ ] Consultar `engine_discount_priorities` por ecommerce_id
- [ ] Para cada prioridad (1 a N):
  -Obtener reglas activas (is_active=true)
  - Iterar reglas y evaluar contra request+cliente
  - Calcular descuentos (PERCENTAGE o FIXED_AMOUNT)
  - Acumular descuentos según `applied_with`

#### 3.4 Evaluador de logic_conditions JSONB
- [ ] Crear `LogicConditionEvaluator` en `application/service/`
- [ ] Parser de JSONB con tipos: NUMERIC, ARRAY, STRING, DATE_RANGE, INTEGER
- [ ] Evaluación de criterios:
  - `min_spent`, `max_spent` contra subtotal o total_spent del cliente
  - `categories` (ARRAY) contra items.category del carrito
  - `customer_tier` (STRING) contra classificación
  - `date_range` contra request timestamp
  - `min_order_count`, `min_membership_days` contra métricas cliente
- [ ] Soporte para `evaluation_logic` field: "total_spent >= 2000 AND order_count >= 10" (parser básico)

#### 3.5 Motor de Cappings y Redondeos
- [ ] Crear `DiscountCappingEngine` en `application/service/`
- [ ] Implementar `applyCap(discount, maxCap)` → retorna (appliedDis, wasCapped, reason)
- [ ] Implementar `applyRounding(amount, rule)` con estrategias:
  - ROUND_HALF_UP (default)
  - FLOOR
  - CEIL
- [ ] Asegurar que `max_discount_cap` nunca se excede
- [ ] Implementar lógica `allow_stacking=false` → solo mayor descuento

#### 3.6 Escritor de Auditoría (TransactionLogs)
- [ ] Crear `TransactionLogWriter` en `application/service/`
- [ ] Escribir en `transaction_logs`:
  - `external_order_id` (único, de request)
  - `ecommerce_id`, `subtotal`, `discount_calculated`, `discount_applied`, `final_amount`
  - `was_capped` (boolean)
  - `status` (SUCCESS|PARTIALLY_APPLIED|REJECTED)
  - `applied_rules_json` (JSONB serializado)
  - `customer_tier` (string, no-PII)
  - `client_metrics_json` { "total_spent": ..., "order_count": ..., "membership_days": ... } (no-PII)
  - `calculated_at`, `created_at`, `expires_at` (calculated_at + 7 days)
- [ ] Validar constraint de integridad: final_amount = subtotal - discount_applied

#### 3.7 Controller y DTOs
- [ ] Confirmar existencia de `DiscountCalculationControllerV2` en `presentation/controller/`
- [ ] Endpoint POST `/api/v1/engine/calculate` (auth: API Key)
- [ ] Validación con `@Valid` en RequestBody
- [ ] Crear DTOs:
  - `DiscountCalculateRequestV2`
  - `CartItemRequest`
  - `DiscountCalculateResponseV2`
  - `AppliedRuleDetail`
- [ ] Mapeos entre entidades y DTOs

#### 3.8 Cache y Sincronización
- [ ] Asegurar que `DiscountSettingsEntity` se cachea en Caffeine
- [ ] Listener RabbitMQ para invalidar cache en eventos de cambio de settings
- [ ] Listener RabbitMQ para invalidar cache en cambios de reglas

#### 3.9 Manejo de Errores
- [ ] Exception `DiscountCalculationException` para errores de lógica
- [ ] Exception `InvalidCartException` para carrito sin items
- [ ] Exception `RuleEvaluationException` para JSONB malformado
- [ ] Global `ExceptionHandler` —  respuestas consistentes
- [ ] Log de errores (sin PII)

#### 3.10 Tests Unitarios (FUNDAMENTAL)
- [ ] Test: Carrito sin descuentos → final_amount = subtotal
- [ ] Test: Descuento INDIVIDUAL PERCENTAGE correcto
- [ ] Test: Descuento INDIVIDUAL FIXED_AMOUNT correcto
- [ ] Test: CUMULATIVE: dos descuentos se suman
- [ ] Test: EXCLUSIVE: descuento mayor reemplaza otros
- [ ] Test: max_discount_cap aplicado, was_capped=true
- [ ] Test: allow_stacking=false → solo mayor descuento
- [ ] Test: Redondeo ROUND_HALF_UP a 2 decimales
- [ ] Test: Constraint integridad financiera
- [ ] Test: Carrito sin items → 400 Bad Request
- [ ] Test: Reglas inactivas se ignoran
- [ ] Test: LogicConditions JSONB: min_spent, categories, customer_tier
- [ ] Test: TransactionLog se escribe correctamente con externa_order_id único

### Frontend (Dashboard Opcional)

#### 3.11 Componente de Simulación de Descuentos (Opcional)
- [ ] Formulario para simular carrito y ver breakdown de descuentos
- [ ] Visualización de reglas aplicadas con nombres y porcentajes
- [ ] Indicador visual si fue capeado

### QA / Testing

#### 3.12 Casos de Prueba (Gherkin)
- [ ] Carrito simple sin descuentos
- [ ] Descuento PERCENTAGE INDIVIDUAL
- [ ] Descuento FIXED_AMOUNT INDIVIDUAL
- [ ] CUMULATIVE: múltiples descuentos se suman
- [ ] EXCLUSIVE: mayor gana
- [ ] max_discount_cap limitando descuento
- [ ] allow_stacking=false: solo uno
- [ ] Redondeo automático
- [ ] Carrito vacío → 400
- [ ] API Key inválida → 401

#### 3.13 Riesgos de Calidad (ASDD)
- **Alto**: Integridad financiera comprometida → validar constraint en tests
- **Alto**: JSONB logic_conditions malformado → validar schema
- **Medio**: Cache con datos stale → invalidar por eventos RabbitMQ
- **Medio**: Double-booking de descuentos (CUMULATIVE vs EXCLUSIVE) → prioridades clara
- **Bajo**: Redondeo incorrecto → test de casos edge con decimales

---

## Notas Técnicas

### Flujo de Cálculo Resumido
1. Validar request y calcular subtotal
2. Clasificar cliente (HU-10) → tier
3. Obtener settings de ecommerce (max_discount_cap, allow_stacking, rounding_rule)
4. Obtener prioridades de descuento (1, 2, 3, ...)
5. Para cada prioridad:
   - Obtener reglas activas → filtrar, evaluar logic_conditions, calcular descuento
6. Agregar descuentos según applied_with
7. Aplicar cap si suma > max_discount_cap
8. Si allow_stacking=false, solo mayor descuento
9. Redondear a 2 decimales
10. Escribir en transaction_logs
11. Retornar response con desglose

### Criterios JSONB — Ejemplos

**Para clasificación por tier:**
```json
{
  "customer_tier": { "type": "STRING", "value": "Gold" }
}
```

**Para descuento por gasto mínimo:**
```json
{
  "min_spent": { "type": "NUMERIC", "value": 500.00 }
}
```

**Para descuento estacional con rango de fechas:**
```json
{
  "date_range": {
    "type": "DATE_RANGE",
    "value": {
      "start": "2026-11-01T00:00:00Z",
      "end": "2026-12-31T23:59:59Z"
    }
  }
}
```

**Para descuento por categoría de producto:**
```json
{
  "product_categories": {
    "type": "ARRAY",
    "value": ["electronics", "home", "furniture"]
  }
}
```

### Performance
- Evaluación de 1000+ carritos/segundo esperado
- Latencia P99 < 100ms por carrito
- Cache hit rate > 95% para settings y tiers
- Índices en DB: (ecommerce_id, priority_level), (ecommerce_id, discount_type_code)

### Integridad de Datos
- `external_order_id` es único por ecommerce (evita duplicados)
- Constraint: `final_amount = subtotal - discount_applied`
- Logs con expires_at automático (7 días)
- NO almacenar PII de cliente final (customer_name, email, etc.)

### Relaciones entre HUs
- **HU-10 → HU-11**: Clasificación alimenta cálculo de descuentos
- **HU-11 → HU-12**: Cálculo genera logs en transaction_logs
