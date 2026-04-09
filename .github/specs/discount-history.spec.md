---
id: SPEC-012
status: APPROVED
feature: discount-history-audit-trail
created: 2026-04-05
updated: 2026-04-05
author: spec-generator
version: "1.2"
related-specs:
  - SPEC-010 (loyalty-tiers-classification)
  - SPEC-011 (cart-calculation)
---

# Spec: Discount History — Historial de Descuentos y Auditoría (HU-12)

> **Estado:** `APPROVED` — Listo para implementación.
> **Ciclo de vida:** APPROVED → IN_PROGRESS → IMPLEMENTED

> **Contexto (ARQUITECTURA DISTRIBUIDA):** 
> - **Engine Service (HU-12)**: Responsable de ESCRIBIR logs en `transaction_logs` y EMITIR eventos a RabbitMQ
> - **Admin Service**: Responsable de CONSUMIR eventos y EXPONER endpoints de consulta/filtrado y dashboard
> - No hay endpoints de lectura en Engine Service (reduce acoplamiento y complejidad)
> - Retención automática de 7 días (expires_at generado por DB)
> - No almacena PII de clientes finales, solo métricas y detalles de reglas

---

## 1. REQUERIMIENTOS

### Descripción
Implementar un sistema distribuido de auditoría donde:
1. **Engine Service** registra todos los cálculos de descuentos (HU-11) en `transaction_logs` y emite eventos a RabbitMQ.
2. **Admin Service** consume esos eventos, sincroniza `transaction_logs`, y expone APIs de consulta/filtrado y dashboard.

Datos con retención automática de 7 días (expires_at generado por DB). Excluye PII, solo almacena métricas y detalles de reglas para auditoría interna.

### Requerimiento de Negocio
Como Admin del Sistema Loyalty, quiero que el Motor de Descuentos (Engine) ESCRIBA automáticamente un log de cada transacción, y que luego pueda CONSULTAR ese historial en los últimos 7 días (desde el Admin Dashboard), filtrar por fecha/estado, ver qué reglas se aplicaron en cada caso y si fueron capeadas por tope máximo, para auditar la correctitud del motor y validar rentabilidad de promociones.

### Historias de Usuario

#### HU-12: Historial de Descuentos — Escritura en Engine + Lectura en Admin (Arquitectura Distribuida)

```
Como:        Sistema de Auditoría Distribuida
Quiero:      (Engine) Escribir logs de descuentos en transaction_logs
             (Engine) Emitir evento a RabbitMQ por cada cálculo
             (Admin) Consumir eventos y sincronizar replica de logs
             (Admin) Exponer APIs de consulta y visualizar en Dashboard
Para:        Auditar el motor de descuentos sin acoplamiento, validar reglas correctas

Prioridad:   Media
Estimación:  M
Dependencias: HU-11 debe generar logs; RabbitMQ disponible; Admin Service debe consumir eventos
Capa:        Backend (Engine Service: WRITE) + Backend (Admin Service: READ) + Frontend (Dashboard)
```

#### Criterios de Aceptación — HU-12

**CRITERIO-12.1 (ENGINE): Escritura de log en transaction_logs tras cálculo exitoso**
```gherkin
Dado que:      HU-11 (DiscountCalculationServiceV2) calcula un descuento para un carrito
Y:             el cálculo completa exitosamente
Cuando:        se invoca TransactionLogWriter.saveLog(CalculationResultDTO)
Entonces:      se crea un NEW registro en transaction_logs con:
               - external_order_id (unique)
               - subtotal_amount, discount_calculated, discount_applied, final_amount
               - was_capped = true|false
               - status = "SUCCESS"
               - applied_rules_json = array de reglas aplicadas
               - customer_tier, client_metrics_json (sin PII)
               - calculated_at = timestamp del cálculo
               - created_at = timestamp de inserción
               - expires_at = calculated_at + 7 días (auto-generado)
Y:             la inserción es TRANSACCIONAL con el cálculo de HU-11
Y:             NO se pierde ningún log aunque falle la emisión del evento
```

**CRITERIO-12.2 (ENGINE): Emisión de evento a RabbitMQ por cada transacción**
```gherkin
Dado que:      se guardó un log en transaction_logs
Cuando:        se completa la inserción del log
Entonces:      se emite un evento a RabbitMQ con topic "discount-transactions.created"
Y:             el evento contiene: transaction_id, ecommerce_id, external_order_id, subtotal, discount_applied, customer_tier
Y:             el evento se emite ASINCRONAMENTE (no bloquea el cálculo original)
Y:             si falla la emisión, se reintenta hasta 3 veces (con backoff exponencial)
Y:             después de 3 intentos fallidos, se guarda en tabla de eventos fallidos para retry manual
```

**CRITERIO-12.3 (ADMIN): Consumo de evento y sincronización en Admin Service**
```gherkin
Dado que:      Engine emite evento "discount-transactions.created" a RabbitMQ
Cuando:        Admin Service consume el evento (listener en queue admin.discount-transactions.sync)
Entonces:      Admin sincroniza el log en su propia BD (réplica de transaction_logs desde Admin)
Y:             el evento se procesa en < 2 segundos
Y:             si hay error de sincronización, se reintenta automáticamente
Y:             Admin puede ahora CONSULTAR desde su propia BD (sin hacer llamadas síncronas a Engine)
```

**CRITERIO-12.4 (ADMIN): Consulta de transacciones en rango de fechas**
```gherkin
Dado que:      Admin Service tiene réplica sincronizada de transaction_logs
Y:             existen transacciones:
               - Trans A: created_at = 2026-04-05 10:00:00
               - Trans B: created_at = 2026-04-04 15:30:00
               - Trans C: created_at = 2026-03-29 08:00:00 (> 7 días)
Y:             hoy es 2026-04-05
Cuando:        GET /api/v1/admin/discount-history?ecommerce_id=X&start_date=2026-04-01&end_date=2026-04-05
               (Nota: ADMIN solo, no Engine)
Entonces:      retorna 200 OK con Trans A y Trans B (últimos 7 días)
Y:             NOT incluye Trans C (fuera del rango o ya expirada)
```

**CRITERIO-12.5 (ADMIN): Transacción completa con detalles de reglas**
```gherkin
Dado que:      una transacción tiene 2 reglas aplicadas
Cuando:        se consulta GET /api/v1/admin/discount-history/{transaction_id}
               (Nota: ADMIN solo, no Engine)
Entonces:      retorna 200 OK con:
               {
                 "transaction_id": "txn-uuid",
                 "external_order_id": "order-12345",
                 "ecommerce_id": "ecommerce-uuid",
                 "subtotal_amount": 1000.00,
                 "discount_calculated": 200.00,
                 "discount_applied": 150.00,
                 "final_amount": 850.00,
                 "was_capped": true,
                 "customer_tier": "Gold",
                 "status": "SUCCESS",
                 "applied_rules": [
                   {
                     "rule_id": "rule-uuid",
                     "rule_name": "VIP Fidelity 15%",
                     "discount_percentage": 15.0,
                     "discount_type_code": "FIDELITY"
                   },
                   {
                     "rule_id": "rule-uuid-2",
                     "rule_name": "Seasonal 10%",
                     "discount_percentage": 10.0,
                     "discount_type_code": "SEASONAL"
                   }
                 ],
                 "calculated_at": "2026-04-05T14:30:00Z",
                 "created_at": "2026-04-05T14:30:00Z",
                 "expires_at": "2026-04-12T14:30:00Z"
               }
```

**CRITERIO-12.6 (ADMIN): Listado paginado de transacciones**
```gherkin
Dado que:      Admin tiene réplica sincronizada con 250 transacciones
Y:             página default = 0, size = 20
Cuando:        GET /api/v1/admin/discount-history?ecommerce_id=X&page=0&size=20&start_date=...&end_date=...
               (Nota: ADMIN solo, no Engine)
Entonces:      retorna 200 OK con:
               {
                 "data": [ {...}, {...}, ... ], // 20 items
                 "total_elements": 250,
                 "total_pages": 13,
                 "current_page": 0,
                 "page_size": 20
               }
```

**CRITERIO-12.7 (ADMIN): Filtro por status de transacción**
```gherkin
Dado que:      transacciones con status: SUCCESS, PARTIALLY_APPLIED, REJECTED (en Admin BD)
Cuando:        GET /api/v1/admin/discount-history?ecommerce_id=X&status=SUCCESS
               (Nota: ADMIN solo, no Engine)
Entonces:      retorna solo transacciones con status=SUCCESS
```

**CRITERIO-12.8 (ADMIN): Filtro por was_capped (transacciones que superaron tope)**
```gherkin
Dado que:      en Admin BD: algunas transacciones have was_capped=true, otras false
Cuando:        GET /api/v1/admin/discount-history?ecommerce_id=X&was_capped=true
               (Nota: ADMIN solo, no Engine)
Entonces:      retorna solo transacciones donde fue_capeada=true
Y:             muestra el monto calculado vs aplicado con indicador visual
```

**CRITERIO-12.9 (ENGINE + ADMIN): Historial automáticamente limpiado a 7 días**
```gherkin
Dado que:      transacción A creada hace 8 días atrás
Y:             la DB (Engine + Admin) está configurada con índice en expires_at
Cuando:        el sistema ejecuta limpieza automática (cada medianoche)
Entonces:      transacción A se elimina automáticamente de transaction_logs
Y:             las transacciones < 7 días siguen disponibles en Admin para consulta
Y:             Engine también limpia su BD para no guardar datos antiguos
```

**CRITERIO-12.10 (ADMIN): Dashboard visual con gráficos**
```gherkin
Dado que:      administrador accede a /dashboard/discount-history (Admin Service)
Cuando:        selecciona rango de fechas (últimos 7 días, últimos 30 días, custom)
Entonces:      ve (desde Admin BD sincronizada):
               - Tabla de transacciones más recientes (20 primeras)
               - Cards de resumen: total de transacciones, descuento promedio, topes aplicados
               - Gráfico de series temporales: descuentos aplicados/día
               - Gráfico de distribución de reglas: qué reglas se usaron más
               - Filtros interactivos: status, was_capped, customer_tier
```

**CRITERIO-12.11 (ENGINE): Protección de datos — NO almacena PII**
```gherkin
Dado que:      Engine registra una transacción en transaction_logs
Cuando:        se consulta transaction_logs en Engine BD
Entonces:      NO contiene:
               - customer name
               - email
               - phone
               - address
Y:             SÍ contiene (sin PII):
               - customer_id (hash o external_id)
               - customer_tier
               - total_spent, order_count, membership_days (métricas)
```

**CRITERIO-12.12 (ENGINE + ADMIN): Auditoría con timestamps UTC**
```gherkin
Dado que:      transacción se procesa en server UTC
Cuando:        se registra en transaction_logs (Engine) y sincroniza a Admin
Entonces:      calculated_at, created_at, expires_at están en UTC ISO8601
Y:             al consultar desde diferentes zonas horarias, se normaliza a UTC
```

**CRITERIO-12.13 (ADMIN): Validación de authorization en consultas**
```gherkin
Dado que:      usuario sin JWT intenta consultar historial
Cuando:        GET /api/v1/admin/discount-history sin header Authorization
               (Nota: ADMIN solo, no Engine)
Entonces:      retorna 401 Unauthorized
Y:             mensaje: "Missing or invalid JWT token"
```



### Reglas de Negocio

1. **Retención de 7 días**: Los registros en `transaction_logs` se borran automáticamente pasados 7 días (expires_at es auto-generado).
2. **Datos sin PII**: Nunca se almacenan nombre, email, teléfono, dirección del cliente final.
3. **external_order_id único**: Cada carrito procesado tiene un ID externo único (no se repite).
4. **was_capped flag**: Marcar claramente si descuento fue limitado por max_discount_cap.
5. **Status de transacción**: SUCCESS (todo OK), PARTIALLY_APPLIED (algunas reglas), REJECTED (ninguna).
6. **Timestamps en UTC**: Todos los timestamps son ISO8601 UTC.
7. **Consultas indexadas rápidamente**: Índices en (ecommerce_id, created_at DESC) para búsqueda de historial.
8. **Desglose de reglas**: Cada regla aplicada está documentada con nombre, porcentaje/monto y tipo.

---

## 2. DISEÑO

### Modelos de Datos

#### Entidades afectadas

| Entidad | Almacén | Cambios | Descripción |
|---------|---------|---------|-------------|
| `TransactionLogEntity` | tabla `transaction_logs` | consultada/filtrada | Auditoría de cálculos (escrita por HU-11) |

#### Estructura completa de `transaction_logs`

| Campo | Tipo | Obligatorio | Validación | Descripción |
|-------|------|-------------|------------|-------------|
| `id` | UUID | sí | PK auto-gen | Identificador único del log |
| `ecommerce_id` | UUID | sí | FK, indexed | E-commerce propietario |
| `external_order_id` | string | sí | unique, max 255 | ID externo del pedido (para evitar duplicados) |
| `subtotal_amount` | decimal(12,4) | sí | >= 0 | Suma de items antes de descuento |
| `discount_calculated` | decimal(12,2) | sí | >= 0 | Descuento antes de aplicar cap |
| `discount_applied` | decimal(12,2) | sí | >= 0 | Descuento final (respeta cap) |
| `final_amount` | decimal(12,2) | sí | >= 0 | subtotal - discount_applied |
| `was_capped` | boolean | sí | default false | true si se aplicó max_discount_cap |
| `status` | enum | sí | SUCCESS|PARTIALLY|REJECTED | Estado de la transacción |
| `error_message` | text | no | max 500 | Mensaje de error si status != SUCCESS |
| `applied_rules_json` | JSONB | sí | array de rules | Detalle de reglas aplicadas (no-PII) |
| `priority_evaluation_json` | JSONB | no | metadata | Detalles de evaluación de prioridades |
| `customer_tier` | string | no | max 100 | Tier del cliente (sin PII) |
| `client_metrics_json` | JSONB | no | { total_spent, order_count, membership_days } | Métricas sin PII |
| `calculated_at` | timestamp UTC | sí | auto-set | Momento del cálculo (HU-11) |
| `created_at` | timestamp UTC | sí | auto-set | Momento de inserción en DB |
| `expires_at` | timestamp UTC | sí | generated | created_at + 7 days (auto-limpieza) |

#### Estructura de applied_rules_json

```json
[
  {
    "rule_id": "550e8400-e29b-41d4-a716-446655440000",
    "rule_name": "VIP Fidelity 15%",
    "discount_type_code": "FIDELITY",
    "discount_type": "PERCENTAGE",
    "applied_with": "INDIVIDUAL",
    "discount_percentage": 15.0,
    "discount_amount": 150.00,
    "priority_level": 1
  },
  {
    "rule_id": "550e8400-e29b-41d4-a716-446655440001",
    "rule_name": "Seasonal 10%",
    "discount_type_code": "SEASONAL",
    "discount_type": "PERCENTAGE",
    "applied_with": "CUMULATIVE",
    "discount_percentage": 10.0,
    "discount_amount": 100.00,
    "priority_level": 2
  }
]
```

#### Estructura de client_metrics_json (sin PII)

```json
{
  "customer_id_masked": "cust-...xyz",
  "total_spent": 2500.00,
  "order_count": 15,
  "membership_days": 180,
  "last_purchase_date": "2026-03-15T10:30:00Z"
}
```

#### Índices / Constraints

- **PK**: `id`
- **Unique**: `(ecommerce_id, external_order_id)` — evita transacciones duplicadas
- **Index**: `(ecommerce_id, created_at DESC)` — búsqueda rápida de historial
- **Index**: `(created_at, expires_at)` — limpieza automática de registros expirados
- **Index**: `(status)` — filtro por estado
- **Index**: `(was_capped)` — filtro por transacciones capeadas
- **Index**: `(ecommerce_id, status, created_at DESC)` — búsqueda combinada

### Arquitectura Distribuida — Engine WRITE + Admin READ

#### Engine Service — SOLO Escritura (HU-12)

**Endpoints de Engine Service:**
- `POST /api/v1/engine/calculate` (HU-11) — Cálculo de descuentos + logging + eventos
- `GET /health` — Health check

**NO hay endpoints de lectura** en Engine Service. La responsabilidad de Engine es:

1. **Guardar log en transaction_logs** (transaccionally con cálculo)
2. **Emitir evento a RabbitMQ** con "discount-transactions.created" para notificar al Admin

#### Servicios en Engine Service

##### `TransactionLogWriter`
**Responsabilidad**: Escribir logs de transacciones en BD.

```java
public class TransactionLogWriter {
    
    // Invocada por DiscountCalculationServiceV2 (HU-11) tras cálculo exitoso
    @Transactional
    public TransactionLogEntity saveLog(CalculationResultDTO calculationResult) {
        // 1. Crear entity TransactionLogEntity (sin PII)
        // 2. Guardar en transaction_logs (transaccional con cálculo)
        // 3. Retornar entity guardada
        // Validar: external_order_id unique, financial consistency, etc.
    }
}
```

##### `DiscountTransactionEventPublisher`
**Responsabilidad**: Emitir eventos a RabbitMQ tras guardar log.

```java
@Component
public class DiscountTransactionEventPublisher {
    
    // Invocada ASINCRONAMENTE tras saveLog()
    public void publishTransactionEvent(TransactionLogEntity log) {
        // 1. Convertir entity a DTO (sin PII)
        // 2. Emitir a RabbitMQ topic "discount-transactions.created"
        // 3. Retry automático si falla (max 3 intentos)
        // 4. Dead-letter queue si falla permanentemente
        
        // Payload:
        // {
        //   "transaction_id": "uuid",
        //   "ecommerce_id": "uuid",
        //   "external_order_id": "string",
        //   "subtotal_amount": decimal,
        //   "discount_applied": decimal,
        //   "was_capped": boolean,
        //   "customer_tier": "string",
        //   "applied_rules_json": [...],
        //   "calculated_at": "ISO8601 UTC"
        // }
    }
}
```

---

#### Admin Service — SOLO Lectura + Dashboard (HU-12)

**API Endpoints (ADMIN Service):**
- `GET /api/v1/admin/discount-history` — Listar transacciones con filtros
- `GET /api/v1/admin/discount-history/{transaction_id}` — Detalles de transacción
- `GET /api/v1/admin/discount-history/stats` — Estadísticas resumidas
- `GET /dashboard/discount-history` — UI de Dashboard

**Sincronización (Admin Service):**

##### `DiscountTransactionSyncConsumer`
**Queue**: `admin.discount-transactions.sync` (listener del topic "discount-transactions.created")
**Responsabilidad**: Recibir eventos de Engine y sincronizar BD local.

```java
@Service
public class DiscountTransactionSyncConsumer {
    
    @RabbitListener(queues = "admin.discount-transactions.sync")
    public void onTransactionCreated(DiscountTransactionEvent event) {
        // 1. Recibir evento desde Engine
        // 2. Crear TransactionLogEntity en Admin BD (misma estructura que Engine)
        // 3. Persistir en table transaction_logs
        // 4. Validar integridad: external_order_id unique, financial consistency
        // 5. Commit transaccional
        // Si falla: event se reintenta automáticamente (x3)
    }
}
```

##### `TransactionLogQueryService`
**Responsabilidad**: Consultar transaction_logs locales (Admin BD).

```java
public class TransactionLogQueryService {
    
    public Page<TransactionLogDTO> findWithFilters(
        UUID ecommerceId,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String status,
        Boolean wasCapped,
        String customerTier,
        Pageable pageable
    ) {
        // Consultar Admin BD
        // Construir query dinámica con filtros
        // ORDER BY created_at DESC
        // Validar que rango <= 7 días (opcional)
    }
    
    public Optional<TransactionLogDetailDTO> findById(UUID transactionId, UUID ecommerceId) {
        // Obtener detalles completos con applied_rules_json deserializado
        // Validar permisos de ecommerce
    }
    
    public TransactionStatsDTO getStats(UUID ecommerceId, LocalDateTime startDate, LocalDateTime endDate) {
        // Calcular totales, promedios, top rules, top tiers desde Admin BD
    }
}
```

##### Controller: `DiscountHistoryController` (ADMIN)
**Responsabilidad**: Endpoints REST

```java
@RestController
@RequestMapping("/api/v1/admin/discount-history")
public class DiscountHistoryController {
    
    @GetMapping
    public ResponseEntity<PaginatedResponse<TransactionLogDTO>> list(...) { }
    
    @GetMapping("/{transaction_id}")
    public ResponseEntity<TransactionLogDetailDTO> getDetail(...) { }
    
    @GetMapping("/stats")
    public ResponseEntity<TransactionStatsDTO> getStats(...) { }
}
```

### DTOs

#### `TransactionLogDTO` (Lista)
```java
public record TransactionLogDTO(
    UUID transactionId,
    String externalOrderId,
    BigDecimal subtotalAmount,
    BigDecimal discountCalculated,
    BigDecimal discountApplied,
    BigDecimal finalAmount,
    String customerTier,
    Boolean wasCapped,
    String status,
    Integer appliedRulesCount,
    Instant calculatedAt,
    Instant createdAt,
    Instant expiresAt
) { }
```

#### `TransactionLogDetailDTO` (Detalle)
```java
public record TransactionLogDetailDTO(
    UUID transactionId,
    String externalOrderId,
    UUID ecommerceId,
    BigDecimal subtotalAmount,
    BigDecimal discountCalculated,
    BigDecimal discountApplied,
    BigDecimal finalAmount,
    Boolean wasCapped,
    String status,
    String customerTier,
    List<AppliedRuleDetail> appliedRules,
    ClientMetricsDTO clientMetrics,
    Instant calculatedAt,
    Instant createdAt,
    Instant expiresAt
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

public record ClientMetricsDTO(
    BigDecimal totalSpent,
    Integer orderCount,
    Integer membershipDays
) { }
```

#### `TransactionStatsDTO`
```java
public record TransactionStatsDTO(
    Long totalTransactions,
    Long successfulTransactions,
    Long rejectedTransactions,
    BigDecimal totalDiscountApplied,
    BigDecimal averageDiscountPerTransaction,
    Long transactionsCapped,
    BigDecimal percentageCapped,
    List<RuleStatsDTO> topRules,
    List<TierStatsDTO> topTiers
) { }

public record RuleStatsDTO(
    String ruleName,
    Integer usageCount,
    BigDecimal totalDiscount
) { }

public record TierStatsDTO(
    String tierName,
    Integer transactionCount,
    BigDecimal avgDiscount
) { }
```

---

### Frontend — Dashboard

#### Componentes

**1. TransactionHistoryTable**
- Tabla responsive de transacciones
- Columnas: OrderID, Subtotal, Descuento, Final, Capeado, Estado, Fecha
- Paginación y sorting
- Click en fila → modal con detalles

**2. FilterBar**
- Campo de rango de fechas (default: último 7 días)
- Select: Status (SUCCESS, REJECTED, etc.)
- Checkbox: "Solo transacciones capeadas"
- Select: Tier (Bronze, Silver, Gold, Platinum)
- Botón: "Aplicar Filtros"

**3. StatsCards**
- Card 1: Total de transacciones
- Card 2: Descuento promedio
- Card 3: % de transacciones capeadas
- Card 4: Descuento total aplicado

**4. Charts**
- **Series temporal**: Descuentos/día (línea)
- **Distribución de reglas**: Top 5 reglas más usadas (bar chart)
- **Distribución de tiers**: Transacciones por tier (pie chart)

**5. TransactionDetailModal**
- Desglose completo:
  - Subtotal, Descuento calculado vs aplicado, Final
  - Lista de reglas con nombres y porcentajes
  - Tier del cliente
  - Fecha, ID de transacción
  - Banner si was_capped=true

**Layout de Dashboard:**
```
┌─────────────────────────────────────┐
│       Historial de Descuentos        │
├─────────────────────────────────────┤
│ [ Filtros ]                          │
├─────────────────────────────────────┤
│ [Stats Cards] [Stats Cards] ...      │
├─────────────────────────────────────┤
│       [Series temporal chart]        │
├─────────────────────────────────────┤
│  [Distribución Rules] [Dist. Tiers]  │
├─────────────────────────────────────┤
│      [TransactionHistoryTable]       │
│      (con paginación)                │
└─────────────────────────────────────┘
```

---

## 3. LISTA DE TAREAS

### Backend (Engine Service)

#### 3.1 Entidades y Repositorios
- [ ] Confirmar existencia de `TransactionLogEntity` con estructura completa
- [ ] Crear `TransactionLogRepository extends JpaRepository<TransactionLogEntity, UUID>`
- [ ] Crear custom query repository con métodos para filtros
- [ ] Validar índices en DB:
  - `(ecommerce_id, created_at DESC)`
  - `(status)`
  - `(was_capped)`
  - `(expires_at)` para limpieza automática
- [ ] Configurar `@DynamicUpdate` en TransactionLogEntity para optimizar writes

#### 3.2 Servicio de Consulta
- [ ] Crear `TransactionLogQueryService` en `application/service/`
- [ ] Implementar `findWithFilters()` con query dinámica (Specifications o QueryDSL)
- [ ] Filtros: ecommerce_id, (start_date, end_date), status, was_capped, customer_tier
- [ ] Validar rango máximo = 7 días (no permitir consultas más viejas)
- [ ] Ordenamiento por created_at DESC (por defecto)
- [ ] Paginación con `Page<TransactionLogDTO>`

#### 3.3 Servicio de Estadísticas
- [ ] Crear `TransactionLogStatsService` en `application/service/`
- [ ] Implementar `getStats()` para dashboard:
  - Total de transacciones
  - % success vs rejected vs partially_applied
  - Total descuento aplicado (SUM)
  - Promedio descuento por transacción
  - % transacciones capeadas
  - Top 5 reglas más usadas (GROUP BY rule_name, COUNT, SUM)
  - Top 5 tiers (GROUP BY customer_tier, COUNT, AVG)

#### 3.4 Controller (Admin)
- [ ] Crear `DiscountHistoryController` en `presentation/controller/`
- [ ] GET `/api/v1/admin/discount-history` (listar con filtros)
- [ ] GET `/api/v1/admin/discount-history/{transaction_id}` (detalle)
- [ ] GET `/api/v1/admin/discount-history/stats` (estadísticas)
- [ ] Auth: JWT

#### 3.5 DTOs y Mapeos
- [ ] `TransactionLogDTO`, `TransactionLogDetailDTO`, `TransactionStatsDTO`

#### 3.6 Manejo de Errores
- [ ] Exception `TransactionNotFoundException`
- [ ] Exception `InvalidDateRangeException`
- [ ] Global handler → 400, 401, 403, 404

#### 3.7 Tests Unitarios (Admin)
- [ ] Test: Consume evento y sincroniza en BD
- [ ] Test: Listar transacciones (rango 7 días)
- [ ] Test: Filtro por status, was_capped, customer_tier
- [ ] Test: Paginación
- [ ] Test: Obtener detalle con applied_rules
- [ ] Test: Stats (totales, promedio, top rules, top tiers)
- [ ] Test: Sin JWT → 401
- [ ] Test: Event consumer retry en fallo

### Frontend (Dashboard)

#### 3.9 Estructura de Carpetas
```
src/components/
  ├── TransactionHistory/
  │   ├── TransactionHistoryTable.jsx
  │   ├── TransactionDetailModal.jsx
  │   ├── FilterBar.jsx
  │   └── StatsCards.jsx
  ├── Dashboard/
  │   ├── DashboardLayout.jsx
  │   ├── SeriesChart.jsx
  │   ├── RuleDistributionChart.jsx
  │   └── TierDistributionChart.jsx
src/pages/
  ├── DiscountHistoryPage.jsx
  └── ...
src/services/
  ├── transactionService.js
  └── ...
```

#### 3.10 Componente de Tabla
- [ ] Crear `TransactionHistoryTable.jsx`
- [ ] Props: `transactions`, `onRowClick`, `pagination`, `onPageChange`
- [ ] Columnas: OrderID, Subtotal, Descuento Aplicado, Final, Capeado, Estado, Fecha
- [ ] Indicador visual si was_capped=true (rojo o icon)
- [ ] Click en fila → dispara `onRowClick(transactionId)`
- [ ] Responsive (horizontal scroll en mobile)

#### 3.11 Componente de Filtros
- [ ] Crear `FilterBar.jsx`
- [ ] Date range picker (react-datepicker o similar)
- [ ] Default: últimos 7 días
- [ ] Select para Status (SUCCESS, REJECTED, PARTIALLY_APPLIED)
- [ ] Checkbox para was_capped
- [ ] Select para customer_tier (Bronze, Silver, Gold, Platinum)
- [ ] Botón "Aplicar Filtros" → fetch con params

#### 3.12 Componente de Stats
- [ ] Crear `StatsCards.jsx`
- [ ] 4 Cards:
  1. Total Transacciones
  2. Descuento Promedio
  3. % Transacciones Capeadas
  4. Descuento Total Aplicado
- [ ] Props: `stats` (TransactionStatsDTO)
- [ ] Formateo de números: 2 decimales para money

#### 3.13 Gráficos
- [ ] Usar Chart.js o Recharts
- [ ] **Series temporal**: Eje X = Fecha, Eje Y = Descuento/día
- [ ] **Bar Chart**: Top 5 reglas (X=Nombre regla, Y=Cantidad usos)
- [ ] **Pie Chart**: Tiers (slices por tier_name, tamaño por transaction_count)

#### 3.14 Modal de Detalle
- [ ] Crear `TransactionDetailModal.jsx`
- [ ] Props: `transaction` (TransactionLogDetailDTO), `isOpen`, `onClose`
- [ ] Mostrar:
  - Montos: Subtotal, Desc. Calculado, Desc. Aplicado, Final
  - Cliente: Tier, Gasto Histórico, Órdenes
  - Reglas: Tabla con [Nombre, Tipo, Porcentaje, Monto]
  - Flags: ¿Fue capeado?
  - Timestamps: Cuándo se calculó (UTC)
- [ ] Banner rojo si was_capped=true

#### 3.15 Página de Dashboard
- [ ] Crear `DiscountHistoryPage.jsx`
- [ ] Layout:
  1. Header: "Historial de Descuentos"
  2. FilterBar (con date range, status, filters)
  3. StatsCards (resumen)
  4. Charts (series temporal, distribuciones)
  5. TransactionHistoryTable (paginada)
  6. TransactionDetailModal (opens on row click)
- [ ] Integración con Redux o Context API para estado global
- [ ] Loading spinners mientras fetch
- [ ] Error boundaries para fallos

#### 3.15 Servicio de Cliente
- [ ] Crear `transactionService.js`
- [ ] `getTransactions(ecommerceId, filters, pagination)` → GET /api/v1/admin/discount-history
- [ ] `getTransactionDetail(ecommerceId, transactionId)` → GET /api/v1/admin/discount-history/{id}
- [ ] `getStats(ecommerceId, startDate, endDate)` → GET /api/v1/admin/discount-history/stats
- [ ] Error handling: interceptar 401 → redirect login, 404 → toast, etc.

#### 3.16 Rutas
- [ ] Agregar ruta: `/dashboard/discount-history` → `DiscountHistoryPage`
- [ ] Proteger con PrivateRoute (requiere JWT)

### QA / Testing

#### 3.17 Casos de Prueba (Gherkin)
- [ ] Listado de transacciones últimos 7 días
- [ ] Filtro por status SUCCESS
- [ ] Filtro por was_capped=true
- [ ] Paginación: page=0, size=20
- [ ] Detalle completo de transacción con reglas
- [ ] Stats: total, promedio, capeadas
- [ ] Rango > 7 días rechazado
- [ ] Sin JWT → 401
- [ ] Dashboard carga gráficos correctamente
- **Alto**: Exposición accidental de PII → validar ningún dto contiene customer name, email
- **Medio**: Índices en DB insuficientes → performance < 200ms para historial
- **Medio**: Limpieza automática de 7 días falla → validates `expires_at` y scheduled task
- **Bajo**: Gráficos con datos extremos → test valores edge (0 transacciones, 1 transacción)

---

## Notas Técnicas

### Limpieza Automática de Datos (7 días)
- DB genera `expires_at = created_at + INTERVAL '7 days'` automáticamente
- Opción 1: Usar `@CreationTimestamp` en JPA + triggerSQL
- Opción 2: Scheduled task en Spring: `@Scheduled(cron = "0 0 * * * *")` que DELETE WHERE `expires_at < NOW()`
- Opción 3: Usar PostgreSQL native function con evento

### Protección de PII
Nunca en transaction_logs:
- ❌ customer_name
- ❌ customer_email
- ❌ customer_phone
- ❌ customer_address

Sí permitido:
- ✅ customer_id (external, masked si es necesario)
- ✅ customer_tier (Gold, Silver, etc.)
- ✅ total_spent, order_count, membership_days (métricas sin identidad)

### Índices Críticos
```sql
CREATE INDEX idx_transaction_logs_ecommerce_created 
  ON transaction_logs(ecommerce_id, created_at DESC);

CREATE INDEX idx_transaction_logs_status 
  ON transaction_logs(status);

CREATE INDEX idx_transaction_logs_was_capped 
  ON transaction_logs(was_capped);

CREATE INDEX idx_transaction_logs_expires 
  ON transaction_logs(expires_at);
```

### Performance
- Listar 20 transacciones: < 100ms
- Stats con agregaciones: < 200ms
- Paginación debe ser cursor-based o offset, ambos soportados

### Relaciones entre HUs
- **HU-11 → HU-12**: Cálculo genera logs en transaction_logs
- **HU-10 → HU-12**: Clasificación se loguea como `customer_tier` (no-PII)
- **HU-12 → Dashboard**: Logs se consultan y visualizan en Dashboard
