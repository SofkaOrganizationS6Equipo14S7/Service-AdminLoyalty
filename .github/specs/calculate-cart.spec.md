---
id: SPEC-011
status: DRAFT
feature: calculate-cart
created: 2026-03-30
updated: 2026-03-30
author: spec-generator
version: "1.1"
related-specs: ["SPEC-007"]
---

# Spec: Cálculo de Carrito con Descuentos Aplicados

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de iniciar implementación.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPRECATED

---

## 1. REQUERIMIENTOS

### Descripción
El servicio Engine calcula el precio final de un carrito de ecommerce, incluyendo los descuentos aplicables según las reglas vigentes, la configuración de límite máximo y la prioridad configurada. El cálculo respeta las restricciones de negocio (límite máximo de descuento + prioridad entre tipos de descuentos) y retorna un desglose completo para que el ecommerce muestre el precio final en checkout.

### Requerimiento Original (HU-11)

```
Como ecommerce, quiero enviar el carrito y recibir el precio final con descuentos aplicados, 
para mostrarle al usuario final en el checkout.
```

**Stack técnico:**
- Java 21 + Spring Boot 3.x
- Auth: API Key en header
- Endpoint: POST /api/v1/engine/calculate
- Performance: Cálculo en memoria con Caffeine cache
- Sincronización: Reglas desde Admin Service vía RabbitMQ

---

## 2. DISEÑO

### 2.1 Modelos de Datos

#### Entidades afectadas
| Entidad | Almacén | Cambios | Descripción |
|---------|---------|---------|-------------|
| `ProcessedTransaction` | tabla `processed_transactions` (Engine) | **nueva** | Índice mínimalista para idempotencia y conteo después de restart |
| `CalculationProcessedEvent` | RabbitMQ | **nuevo evento** | Evento asíncrono que se publica por cada cálculo |
| Existentes | - | sin cambios | `DiscountSettingsEntity`, `DiscountPriorityEntity`, `SeasonalRuleEntity` |

#### ProcessedTransactionEntity (Service-Engine)
```sql
CREATE TABLE IF NOT EXISTS processed_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ecommerce_id UUID NOT NULL,
    transaction_id VARCHAR(100) UNIQUE NOT NULL,
    processed_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_processed_transactions_transaction_id ON processed_transactions(transaction_id);
CREATE INDEX idx_processed_transactions_ecommerce_id ON processed_transactions(ecommerce_id);
```

**Propósito:** Garantizar idempotencia después de restart del servicio. O almacenar carrito completo en Admin Service vía evento.

> **🛡️ Zona Horaria Agnóstica:** El Engine es agnóstico a regiones. Usamos `TIMESTAMPTZ` para que PostgreSQL almacene automáticamente el timezone (siempre UTC internamente). Esta es la fuente única de verdad temporal.

| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `id` | UUID | sí | auto-generado | PK |
| `ecommerce_id` | UUID | sí | - | Ecommerce que envió el carrito |
| `transaction_id` | string | sí | única, max 100 chars | ID único del lado del ecommerce |
| `processed_at` | TIMESTAMPTZ (UTC) | sí | auto-generado | Timestamp de registro (zona horaria agnóstica) |

#### CalculationProcessedEvent (Event Message)
```json
{
  "eventId": "uuid",
  "eventType": "CALCULATION_PROCESSED",
  "timestamp": "2026-03-30T15:45:30Z",
  "ecommerceId": "uuid-obtenido-del-api-key-validado",
  "transactionId": "TXN-2026-03-30-00001",
  "requestPayload": { "items": [...], "customerPayload": {...} },
  "subtotal": 200.00,
  "totalDiscount": 30.00,
  "finalPrice": 170.00,
  "discountRulesApplied": ["RULE-001", "RULE-002"],
  "exceedsLimit": false
}
```

**Destino:** Exchange `calculation-exchange`, routing key `calculation.processed`. Admin Service escucha y persiste en su BD histórica.

**Nota sobre ecommerceId:** Este ID se obtiene al validar el header X-API-KEY en el Paso 1. El mismo ecommerceId validado se usa en TODOS los pasos (3, 8, 9) para asegurar que la auditoría está correctamente asociada al ecommerce que envió el carrito.

---

### 2.2 Historias de Usuario

#### HU-11-001: Cálculo exitoso con descuentos aplicables

```
Como:        Ecommerce autorizado (API Key válida)
Quiero:      Enviar un carrito con items y recibir el precio final
Para:        Mostrar al usuario el total en checkout

Prioridad:   Alta
Estimación:  M
Dependencias: HU-010 (descuentos limitados)
Capa:        Backend
```

#### Criterios de Aceptación — HU-11-001

**Scenario 1: Cálculo exitoso con descuentos aplicables**
```gherkin
CRITERIO-1.1: Carrito válido con reglas aplicables
  Dado que:  El ecommerce está autorizado (API Key válida)
  Y que:     El carrito contiene items válidos con product_id, quantity, unit_price
  Y que:     Existen reglas de descuento vigentes (seasonal, loyalty, etc.)
  Y que:     Existe configuración de límite máximo y prioridad activa
  Cuando:    El ecommerce envía POST /api/v1/engine/calculate
  Entonces:  El servicio retorna 200 OK
  Y que:     El response incluye: subtotal, total_discount, final_price
  Y que:     El descuento aplicado ≤ límite máximo configurado
  Y que:     El orden de aplicación respeta la prioridad configurada
  Y que:     Se registra un log de la transacción (auditoria)
```

**Scenario 2: Carrito sin descuentos aplicables**
```gherkin
CRITERIO-1.2: Carrito válido sin reglas aplicables
  Dado que:  El ecommerce está autorizado
  Y que:     El carrito es válido
  Y que:     No existen descuentos aplicables (fuera de seasonal, no es loyalty user, etc.)
  Cuando:    El ecommerce envía POST /api/v1/engine/calculate
  Entonces:  El servicio retorna 200 OK
  Y que:     final_price == subtotal (sin descuentos)
  Y que:     total_discount == 0
  Y que:     discount_rules_applied es un array vacío
  Y que:     Se registra el log sin descuentos
```

**Scenario 3: Error: API Key inválida o ausente**
```gherkin
CRITERIO-1.3: Autorización fallida
  Dado que:  El header Authorization es inválido u omitido
  Cuando:    El ecommerce envía POST /api/v1/engine/calculate
  Entonces:  El servicio retorna 401 Unauthorized
  Y que:     El mensaje indica "Invalid or missing API Key"
  Y que:     NO se registra log de la transacción
```

**Scenario 4: Error: Carrito inválido**
```gherkin
CRITERIO-1.4: Validación de payload
  Dado que:  El ecommerce está autorizado
  Y que:     El carrito falta transaction_id o items está vacío
  Cuando:    El ecommerce envía POST /api/v1/engine/calculate
  Entonces:  El servicio retorna 400 Bad Request
  Y que:     El mensaje detalla el campo faltante
  Y que:     NO se registra log de la transacción
```

**Scenario 5: Idempotencia: Reintentos rechazados**
```gherkin
CRITERIO-1.5: Idempotencia por transaction_id (en caché local)
  Dado que:  El Engine tiene transaction_id en caché local (TTL 1 hora)
  Cuando:    El ecommerce re-envía el mismo transaction_id (dentro de 1 hora)
  Entonces:  El servicio retorna 409 Conflict
  Y que:     El mensaje indica "Transaction ID already processed"
  Y que:     NO se envía un nuevo evento a RabbitMQ
```

**Scenario 6: Recuperación después de restart**
```gherkin
CRITERIO-1.6: Integridad después de restart del Engine
  Dado que:  El Engine se reinició y el caché Caffeine se limpió
  Y que:     Los transaction_id procesados antes del restart están en tabla processed_transactions
  Cuando:    El ecommerce re-envía un transaction_id del antes del restart
  Entonces:  El servicio retorna 409 Conflict
  Y que:     NO se re-procesa el cálculo
```

---

### 2.3 Reglas de Negocio

1. **Validación de carrito:**
   - `transaction_id` es obligatorio y debe ser único
   - `items` debe ser un array no vacío
   - Cada item requiere: `product_id`, `quantity` > 0, `unit_price` ≥ 0
   - `customer_payload` es un objeto JSON que puede incluir: `age`, `loyalty_tier`, `location`, etc. (para clasificación de descuentos)

2. **Cálculo de subtotal:**
   - `subtotal = SUM(item.unit_price * item.quantity)`
   - Usar `BigDecimal` para aritmética monetaria (nunca `double`/`float`)

3. **Descuentos aplicables:**
   - Evaluar **seasonal rules** vigentes (fecha actual vs fecha_inicio/fecha_fin)
   - Evaluar **loyalty rules** (si customer_payload incluye loyalty_tier y hay reglas para ese tier)
   - Evaluar **categoria rules** (si el producto pertenece a una categoría con descuento)
   - La selección de qué descuentos aplican es externa (engine obtiene lista de descuentos candidatos)

4. **Respeto de prioridad:**
   - Obtener la configuración de prioridad vigente (`DiscountPriorityEntity`)
   - Ordenar descuentos candidatos por `priority_level` (1 = máxima prioridad)
   - Acumular montos de descuento en orden hasta alcanzar el "límite máximo configurado"

5. **Límite máximo:**
   - La configuración define: `maxDiscountLimit` (ej. $100 o 30% del subtotal)
   - `total_discount` nunca puede exceder este valor
   - Si se alcanza el límite, descartar descuentos restantes (mismo si hay suficiente espacio)

6. **Autenticación:**
   - Validar API Key en header `Authorization: Bearer <API_KEY>`
   - Asociar la transacción al `ecommerce_id` del API Key
   - Rechazar si la clave es inválida, expirada o desactivada

7. **Auditoria:**
   - Registrar SIEMPRE en `cart_audit_logs` (éxito o error de validación)
   - Incluir payload original, subtotal, descuento, precio final, timestamp

---

### 2.4 Modelos DTO (Java Records)

```java
// ==================== REQUEST ====================

public record CartCalculationRequest(
    String transactionId,              // Identificador único del lado del ecommerce
    List<CartItemDTO> items,           // Items del carrito
    CustomerPayloadDTO customerPayload  // Datos del cliente para clasificación
) {}

public record CartItemDTO(
    String productId,          // UUID o identificador del producto
    Integer quantity,          // Cantidad (debe ser > 0)
    BigDecimal unitPrice       // Precio unitario (debe ser >= 0)
) {}

public record CustomerPayloadDTO(
    String loyaltyTier,         // ej. "GOLD", "SILVER", "BRONZE" (opcional)
    Integer age,                // Edad (opcional)
    String location,            // Ubicación geográfica (opcional)
    String customAttribute      // Campo extensible para clasificaciones futuras
) {}

// ==================== RESPONSE ====================

public record CartCalculationResponse(
    String transactionId,
    BigDecimal subtotal,                    // Suma total sin descuentos
    BigDecimal totalDiscount,               // Descuento aplicado (≤ límite máximo)
    BigDecimal finalPrice,                  // subtotal - totalDiscount
    List<DiscountAppliedDTO> appliedDiscounts,  // Desglose de descuentos
    Boolean exceedsLimit,                   // true si descuentos fueron recortados
    Instant calculatedAt
) {}

public record DiscountAppliedDTO(
    String ruleId,              // UUID de la regla aplicada
    String ruleType,            // "SEASONAL", "LOYALTY", "CATEGORY"
    String ruleName,            // "Summer Sale 20%", "Gold Member Extra 10%"
    BigDecimal originalAmount,  // Monto original del descuento
    BigDecimal appliedAmount,   // Monto realmente aplicado (puede ser < original si alcanzó límite)
    Integer priorityLevel       // Nivel de prioridad (1 = alta)
) {}
```

---

### 2.5 API Endpoint

#### POST /api/v1/engine/calculate

**Descripción:** Calcula el precio final de un carrito aplicando descuentos según reglas vigentes, prioridad y límite máximo.

**Auth requerida:** Sí (API Key en header `X-API-KEY`)

**Request Headers:**
```
X-API-KEY: <API_KEY>
Content-Type: application/json
```

**Request Body:**
```json
{
  "transactionId": "TXN-2026-03-30-00001",
  "items": [
    {
      "productId": "PROD-001",
      "quantity": 2,
      "unitPrice": 50.00
    },
    {
      "productId": "PROD-002",
      "quantity": 1,
      "unitPrice": 100.00
    }
  ],
  "customerPayload": {
    "loyaltyTier": "GOLD",
    "age": 35,
    "location": "CO",
    "customAttribute": "value"
  }
}
```

**Response 200 OK:**
```json
{
  "transactionId": "TXN-2026-03-30-00001",
  "subtotal": 200.00,
  "totalDiscount": 30.00,
  "finalPrice": 170.00,
  "appliedDiscounts": [
    {
      "ruleId": "RULE-001-UUID",
      "ruleType": "SEASONAL",
      "ruleName": "Easter Sale 10%",
      "originalAmount": 20.00,
      "appliedAmount": 20.00,
      "priorityLevel": 1
    },
    {
      "ruleId": "RULE-002-UUID",
      "ruleType": "LOYALTY",
      "ruleName": "Gold Member Extra 5%",
      "originalAmount": 10.00,
      "appliedAmount": 10.00,
      "priorityLevel": 2
    }
  ],
  "exceedsLimit": false,
  "calculatedAt": "2026-03-30T15:45:30Z"
}
```

**Response 400 Bad Request:**
```json
{
  "error": "INVALID_REQUEST",
  "message": "Field 'transaction_id' is required",
  "timestamp": "2026-03-30T15:45:30Z"
}
```
**Causas:** Carrito inválido, items vacío, unitPrice negativo, etc.

**Response 401 Unauthorized:**
```json
{
  "error": "INVALID_API_KEY",
  "message": "X-API-KEY header missing or invalid",
  "timestamp": "2026-03-30T15:45:30Z"
}
```

**Response 409 Conflict:**
```json
{
  "error": "DUPLICATE_TRANSACTION",
  "message": "Transaction ID 'TXN-2026-03-30-00001' already exists",
  "timestamp": "2026-03-30T15:45:30Z"
}
```

---

### 2.6 Flujo de Cálculo (Algoritmo)

```
1. Validar header X-API-KEY y extraer ecommerceId
   ├─ Consultar ApiKeyRepository por el valor de X-API-KEY
   ├─ Extraer ecommerceId del API Key válido
   ├─ Si inválido, expirado o no existe → 401 Unauthorized
   └─ Proceder con ecommerceId para los pasos siguientes (CRÍTICO: usar este ID en pasos 3, 8, 9)

2. Validar CartCalculationRequest
   ├─ transaction_id obligatorio
   ├─ items no vacío
   ├─ Cada item: quantity > 0, unitPrice ≥ 0
   └─ Si error → 400 Bad Request

3. Verificar idempotencia (rápido, en memoria)
   ├─ Buscar transaction_id en caché local Caffeine (TTL 1 hora)
   ├─ Si encontrado → 409 Conflict (no procesar)
   └─ Si no encontrado → continuar

4. Calcular subtotal
   └─ SUM(item.unitPrice * item.quantity)

5. Obtener descuentos candidatos
   ├─ Consultar DiscountConfigService (config vigente + límite máximo, desde caché)
   ├─ Consultar reglas aplicables basadas en:
   │  ├─ Seasonal Rules (fecha actual en rango)
   │  ├─ Loyalty Rules (si customerPayload.loyaltyTier aplica)
   │  ├─ Category Rules (si producto está en categoría descuentada)
   │  └─ Custom Rules (extensibles)
   └─ Resultado: lista de descuentos candidatos con montos

6. Aplicar prioridad y límite máximo
   ├─ Obtener DiscountPriorityService (orden de aplicación, desde caché)
   ├─ Ordenar descuentos por priorityLevel (1 = máxima)
   ├─ Acumular montos hasta alcanzar maxDiscountLimit
   └─ Resultado: totalDiscount final, lista de descuentos aplicados

7. Calcular precio final
   └─ finalPrice = subtotal - totalDiscount

8. TRANSACCIÓN LOCAL ATÓMICA: Guardar en tabla local (garantiza integridad post-restart)
   ├─ INICIO: @Transactional(readOnly=false) → Abre transacción ACID local
   ├─ Guardar (ecommerce_id, transaction_id, processed_at) en processed_transactions
   ├─ Agregar transaction_id al caché Caffeine
   ├─ COMMIT automático al salir del método si no hay excepciones
   ├─ ROLLBACK automático si cualquier paso 4-7 falló → NO marcar como procesado
   └─ GARANTÍA: Si el cálculo (pasos 4-7) falla, ni siquiera se intenta guardar en BD

9. Publicar evento asíncrono a RabbitMQ (non-blocking, DESPUÉS de paso 8)
   ├─ CalculationProcessedEvent con: eventId, eventType, timestamp, ecommerceId (del API Key validado), transactionId, requestPayload, subtotal, discount, finalPrice, discountRulesApplied
   ├─ Exchange: calculation-exchange, routing key: calculation.processed
   ├─ Admin Service escucha y persiste en su BD histórica (tabla auditoría centralizada)
   └─ Si RabbitMQ falla, log el error pero NO bloquear respuesta al cliente

10. Retornar CartCalculationResponse (200 OK) inmediatamente
    └─ JSON con desglose completo (sin esperar a confirmación de RabbitMQ)

**GARANTÍAS ATÓMICAS FINALES:**
* Paso 8 envuelto en @Transactional → Si cualquier paso previo (4-7) falla, TODO se revierte
* Paso 9 ocurre DESPUÉS de paso 8 → Solo se publica evento si BD fue exitosa
* Paso 10 retorna ANTES de esperar a RabbitMQ → Respuesta rápida, auditoría asíncrona
```

---

### 2.7 Gestión de Cache

#### Caché de Descuentos (existente)
- **Cache:** Caffeine in-memory en `DiscountCacheConfig`
- **TTL:** 10 minutos (consistente con HU-010)
- **Keys:** DiscountConfig, DiscountPriority
- **Invalidación:** Automática cuando `DiscountConfigUpdated` event llega vía RabbitMQ
- **Fallback:** Si cache expira, consultar BD normalmente

#### Caché de Idempotencia (nuevo)
- **Cache:** Caffeine in-memory en `IdempotencyCacheConfig`
- **TTL:** 1 hora (para cubrir reintentos accidentales)
- **Keys:** `transaction_id` (STRING)
- **Values:** Boolean (true = procesado)
- **Propósito:** Rechazar duplicados en memoria (respuesta 409 en < 1ms)
- **Fallback:** Si cache expira, verificar tabla `processed_transactions` en BD

---

## 3. LISTA DE TAREAS

### 3.1 Backend (Service-Engine)

#### Base de datos
- [ ] Crear migration `V13__Create_processed_transactions_table.sql` (tabla mínimalista: ecommerce_id, transaction_id, processed_at)
- [ ] Crear índices en transaction_id (único), ecommerce_id

#### Domain Layer
- [ ] Crear `ProcessedTransactionEntity` (Entidad)
- [ ] Crear `ProcessedTransactionRepository` (Interfaz repository con `findByTransactionId`)
- [ ] Crear `CartCalculationService` (Lógica de negocio de carrito)

#### Application Layer
- [ ] Crear `CartCalculationRequest`, `CartItemDTO`, `CustomerPayloadDTO` records
- [ ] Crear `CartCalculationResponse`, `DiscountAppliedDTO` records
- [ ] Crear validadores para CartCalculationRequest (Non-null checks, quantity > 0, unitPrice ≥ 0)
- [ ] Crear `CartCalculationService` con método `calculateDiscounts(request, ecommerceId)` **DECORADO CON @Transactional(readOnly=false)**
  - [ ] Paso 1: Validar X-API-KEY (extraer ecommerceId) — sin transacción
  - [ ] Paso 2: Validar request — sin transacción
  - [ ] Paso 3: Verificar caché idempotencia — sin transacción
  - [ ] Pasos 4-7: Calcular descuentos — dentro de transacción
  - [ ] Paso 8: Guardar en processed_transactions — dentro de transacción (ROLLBACK si pasos 4-7 fallan)
  - [ ] Paso 9: Publicar evento (non-blocking, DESPUÉS de transacción exitosa)
  - [ ] Paso 10: Retornar response
- [ ] Integrar con `DiscountCalculationEngine` existente
- [ ] Manejo de excepciones: InvalidRequestException, UnauthorizedException, DuplicateTransactionException

#### Infrastructure Layer
- [ ] Crear `ProcessedTransactionRepositoryImpl` (implementación de BD con `findByTransactionId(String)` y `save(ProcessedTransactionEntity)`)
- [ ] Crear `ApiKeyValidator` para validar header `X-API-KEY` (consultar DB y extraer ecommerceId)
- [ ] Crear `CalculationEventPublisher` para publicar `CalculationProcessedEvent` a RabbitMQ (DESPUÉS de @Transactional exitosa)
  - [ ] Usar `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` para garantizar que evento se publica SÓ si BD fue exitosa
  - [ ] Si RabbitMQ falla, NO debe revertir la transacción local
- [ ] Crear `CalculationProcessedEvent` (Java Record con: eventId, eventType, timestamp, ecommerceId, transactionId, requestPayload, subtotal, totalDiscount, finalPrice, discountRulesApplied)
- [ ] Configurar caché Caffeine para transaction_id (TTL 1 hora) `IdempotencyCacheConfig`
- [ ] Integración con `DiscountConfigService` y `DiscountPriorityService` (caché existente)
- [ ] Integración con `CalculationEventPublisher` (inyectar en `CartCalculationService`)

#### Presentation Layer
- [ ] Crear `CartCalculationController` con endpoint `POST /api/v1/engine/calculate`
- [ ] Validar header `X-API-KEY`
- [ ] Manejo de errores con códigos HTTP correctos (400, 401, 409)
- [ ] Logging de requests/responses (sin exponer payloads sensibles)

#### Tests
- [ ] Unit tests para `CartCalculationService` (lógica de prioridad + límite)
- [ ] Unit tests para `CartCalculationService.calculateDiscounts()` con @Transactional (verificar rollback si cálculo falla)
- [ ] Unit tests para `ProcessedTransactionRepository` (findByTransactionId, save)
- [ ] Integration tests para endpoint POST /api/v1/engine/calculate
- [ ] Tests de transacción: Si paso 4-7 falla, no debe guardarse en BD
- [ ] Tests de idempotencia: reintentos con mismo transaction_id retornan 409
- [ ] Tests negativos: API Key inválida (401), carrito vacío (400), transaction_id duplicado (409)

### 3.2 Frontend *(Opcional — solo si Admin UI necesita visualizar logs)*

- [ ] Componente `CartAuditLogListPage` (visualizar logs de carritos calculados)
- [ ] Filtros: por ecommerce_id, fecha_rango, transaction_id
- [ ] Búsqueda y paginación

### 3.3 QA / Testing

- [ ] Plan de test de integración (curl/Postman con cartios reales)
- [ ] Escenarios Gherkin para cada criterio de aceptación
- [ ] Data de prueba: items, seasonal rules, loyalty rules, límite máximo
- [ ] Test de performance: calcular 100 carritos en paralelo (latencia < 200ms)

### 3.4 Documentación

- [ ] API documentation (OpenAPI/Swagger)
- [ ] README update con ejemplos de request/response
- [ ] Diagramas de flujo (cálculo de descuentos)
