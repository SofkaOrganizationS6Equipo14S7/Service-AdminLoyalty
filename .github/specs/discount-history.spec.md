---
id: SPEC-012
status: DRAFT
feature: discount-history
created: 2026-03-30
updated: 2026-03-30
author: spec-generator
version: "1.0"
related-specs: []
---

# Spec: Historial de Descuentos Aplicados

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de iniciar implementación.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPRECATED

---

## 1. REQUERIMIENTOS

### Descripción

Funcionalidad que permite a los usuarios autenticados del dashboard de LOYALTY consultar el historial de descuentos aplicados en las transacciones de los últimos 7 días. La consulta incluye los detalles técnicos de cada transacción (reglas aplicadas, descuentos calculados, descuentos finales) sin exponer datos personales del cliente final (PII).

### Requerimiento de Negocio

```
HU-12: Historial de Descuentos Aplicados

Como usuario de LOYALTY, quiero consultar los descuentos aplicados en las transacciones 
de los últimos siete días, para verificar que el motor de cálculo esté operando bajo 
las reglas y topes máximos configurados.
```

### Historias de Usuario

#### HU-12.1: Consultar historial de descuentos con filtros

```
Como:        Usuario autenticado de LOYALTY con acceso al dashboard
Quiero:      Consultar el historial de descuentos aplicados en transacciones recientes
Para:        Verificar que el motor de descuentos opera bajo las reglas configuradas

Prioridad:   Alta
Estimación:  M
Dependencias: HU-03 (motor de descuentos), HU-11 (configuración de topes)
Capa:        Backend + Frontend
```

#### Criterios de Aceptación — HU-12.1

**Flujo feliz: Consulta exitosa de 7 días**

```gherkin
CRITERIO-12.1.1: Listar transacciones con descuentos en los últimos 7 días
  Dado que:  soy un usuario autenticado de LOYALTY con acceso al dashboard
  Cuando:    accedo al módulo de "Historial de Descuentos" sin especificar filtros
  Entonces:  el sistema muestra una lista paginada de transacciones de los últimos 7 días
  Y:         cada fila contiene: id de transacción, fecha/hora UTC, reglas aplicadas, 
             descuento calculado (sum de descuentos originales), descuento final (aplicado)
  Y:         la lista está ordenada por fecha descente (más reciente primero)
  Y:         cada página muestra máximo 20 registros por defecto
```

```gherkin
CRITERIO-12.1.2: Filtrar por rango de fechas personalizado
  Dado que:  estoy consultando el historial de descuentos
  Cuando:    especifico un rango de fechas (from_date y to_date en ISO 8601)
  Entonces:  el sistema filtra las transacciones dentro de ese rango
  Y:         se respeta el límite de retención: máximo últimos 7 días desde today
  Y:         si el rango excede 7 días, el sistema trunca a 7 días y notifica al usuario
```

```gherkin
CRITERIO-12.1.3: Resaltar transacciones con descuentos rechazados
  Dado que:  estoy viendo el historial filtrado
  Cuando:    una transacción tiene descuento_final < descuento_calculado 
             (descuento aplicado fue menor por alcanzar tope máximo)
  Entonces:  el sistema marca esa fila con un indicador visual (ej. badge "TRUNCADO")
  Y:         muestra en tooltip: "Descuento rechazado: tope máximo de $XXX alcanzado"
  Y:         el usuario puede hacer click para ver detalles de la regla que fue truncada
```

```gherkin
CRITERIO-12.1.4: Paginación y navegación
  Dado que:  el historial tiene múltiples páginas
  Cuando:    navego entre páginas (botones anterior/siguiente o selector de página)
  Entonces:  la consulta se ejecuta con parámetros page y size
  Y:         el servidor responde con: total de registros, página actual, total de páginas
  Y:         si solicito una página fuera de rango (ej. page=999), retorna error 400
```

**Validaciones y errores**

```gherkin
CRITERIO-12.1.5: El usuario no autenticado no puede acceder
  Dado que:  no tengo un token JWT válido
  Cuando:    intento acceder a GET /api/v1/discount-history
  Entonces:  el sistema retorna 401 Unauthorized
  Y:         no se devuelven datos
```

```gherkin
CRITERIO-12.1.6: Validación de parámetros de paginación
  Dado que:  estoy consultando el historial
  Cuando:    envío page=0 o size > 100 (valores inválidos)
  Entonces:  el sistema retorna 400 Bad Request
  Y:         el mensaje de error indica: "page debe ser >= 1" o "size debe ser <= 100"
```

```gherkin
CRITERIO-12.1.7: Validación de rango de fechas inválido
  Dado que:  estoy filtrando por rango personalizado
  Cuando:    envío from_date > to_date
  Entonces:  el sistema retorna 400 Bad Request
  Y:         el mensaje de error: "from_date debe ser menor que to_date"
```

**Privacidad: No mostrar PII**

```gherkin
CRITERIO-12.1.8: Garantizar que no se expongan datos personales
  Dado que:  el historial contiene información de transacciones
  Cuando:    el sistema devuelve los registros en la respuesta JSON
  Entonces:  NO aparecen: nombres del cliente, correos, direcciones, identificadores de cliente
  Y:         SOLO aparecen: transaction_id, timestamp, payload técnico (reglas y montos)
  Y:         si una regla incluye identificador de cliente, debe ser omitido o hasheado
```

---

## 2. DISEÑO

### 2.1 Arquitectura: Service-Admin (8081) es responsable

La tabla `transaction_logs` y el endpoint GET viven en **Service-Admin**, no en Service-Engine.

**Razones:**
1. **Service-Engine es "sordo y mudo"** para humanos: solo máquinas (e-commerce backends) le envían eventos vía RabbitMQ. No expone datos a dashboards.
2. **Carga computacional**: filtrado complejo, paginación y reportes pertenecen al Admin Service.
3. **Separación de responsabilidades**: Engine calcula; Admin reporta.
4. **Consistencia arquitectónica**: Frontend conecta al Admin (8081), no al Engine (8082).

### 2.2 Flujo de Datos (Event-Driven)

```
1. E-commerce Backend →(RabbitMQ)→ Service-Engine (DiscountCalculationEngine)
2. Service-Engine →(CalculationProcessedEvent)→ RabbitMQ exchange
3. Service-Admin →(CalculationEventConsumer)→ Escucha evento
4. Service-Admin →(limpia PII)→ Inserta en transaction_logs (loyalty_admin BD)
5. Frontend →(REST)→ GET /api/v1/discount-history en Service-Admin
```

### 2.3 Modelo de Datos (Service-Admin)

#### Tabla: `transaction_logs` (Nueva en `loyalty_admin`)

**SQL: Migration `V13__Create_transaction_logs_table.sql` en service-admin**

**Base de Datos:** `loyalty_admin` (service-admin)

```sql
CREATE TABLE IF NOT EXISTS transaction_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ecommerce_id UUID NOT NULL,
    transaction_id VARCHAR(255) NOT NULL,
    timestamp_utc TIMESTAMP WITH TIME ZONE NOT NULL,
    payload_received JSONB NOT NULL,
    rules_applied JSONB NOT NULL,
    discount_calculated NUMERIC(19,2) NOT NULL,
    discount_final NUMERIC(19,2) NOT NULL,
    discount_exceeded BOOLEAN NOT NULL DEFAULT false,
    currency_code VARCHAR(3) NOT NULL DEFAULT 'USD',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para optimizar queries
CREATE INDEX idx_transaction_logs_ecommerce_date 
    ON transaction_logs(ecommerce_id, timestamp_utc DESC);

CREATE INDEX idx_transaction_logs_exceeded 
    ON transaction_logs(ecommerce_id, discount_exceeded) 
    WHERE discount_exceeded = true;

CREATE INDEX idx_transaction_logs_created_at 
    ON transaction_logs(created_at DESC);

-- Política de retención: comentario para recordar que se debe ejecutar limpieza
COMMENT ON TABLE transaction_logs IS 
    'Almacena el historial de descuentos por transacción. 
     Política: retener 7 días (configurable). Ejecutar limpieza manual o vía cron job.';
```

#### Entidad JPA: `TransactionLog` (Service-Admin Domain)

```java
package com.loyalty.service_admin.domain.entity;

@Entity
@Table(name = "transaction_logs", indexes = {
    @Index(name = "idx_transaction_logs_ecommerce_date", 
            columnList = "ecommerce_id, timestamp_utc DESC"),
    @Index(name = "idx_transaction_logs_exceeded", 
            columnList = "ecommerce_id, discount_exceeded")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "ecommerce_id", nullable = false)
    private UUID ecommerceId;
    
    @Column(name = "transaction_id", nullable = false, length = 255)
    private String transactionId;
    
    @Column(name = "timestamp_utc", nullable = false)
    private Instant timestampUtc;
    
    @Column(name = "payload_received", nullable = false, columnDefinition = "jsonb")
    private String payloadReceived; // JSON string, sin PII
    
    @Column(name = "rules_applied", nullable = false, columnDefinition = "jsonb")
    private String rulesApplied; // JSON string con reglas y montos
    
    @Column(name = "discount_calculated", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountCalculated; // Suma total de descuentos calculados
    
    @Column(name = "discount_final", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountFinal; // Descuento aplicado (puede ser menor por tope)
    
    @Column(name = "discount_exceeded", nullable = false)
    private Boolean discountExceeded; // true si discount_final < discount_calculated
    
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
}
```

### 2.4 API Endpoints (Service-Admin 8081)

#### Endpoint 1: Consultar historial de descuentos (GET con paginación y filtros)

**Ruta:** `GET /api/v1/discount-history` (Service-Admin)

**Servicio:** Service-Admin (puerto 8081)

**Headers:**
```
Authorization: Bearer <jwt_token>
```

**Query Parameters:**
```
?from_date=2026-03-23T00:00:00Z       (opcional, default: 7 días atrás)
&to_date=2026-03-30T23:59:59Z         (opcional, default: now)
&page=1                               (opcional, default: 1, min: 1)
&size=20                              (opcional, default: 20, max: 100)
&sort=timestamp_utc,desc              (opcional, default: timestamp_utc,desc)
```

**Response (200 OK):**

```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "transactionId": "TXN-20260330-001234",
      "timestampUtc": "2026-03-30T14:23:45Z",
      "rulesApplied": [
        {
          "type": "FIDELITY_RANGE",
          "ruleId": "FID-001",
          "appliedAmount": 50.00,
          "description": "Descuento por rango de fidelidad (Platinum)"
        },
        {
          "type": "SEASONAL",
          "ruleId": "SEASONAL-001",
          "appliedAmount": 25.00,
          "description": "Descuento estacional (Verano 2026)"
        }
      ],
      "discountCalculated": 75.00,
      "discountFinal": 60.00,
      "discountExceeded": true,
      "currencyCode": "USD",
      "exceededReason": "Tope máximo configurado: $1000.00 alcanzado en la transacción"
    },
    {
      "id": "550e8400-e29b-41d4-a716-446655440002",
      "transactionId": "TXN-20260330-001233",
      "timestampUtc": "2026-03-29T10:15:30Z",
      "rulesApplied": [
        {
          "type": "FIDELITY_RANGE",
          "ruleId": "FID-002",
          "appliedAmount": 30.00,
          "description": "Descuento por rango de fidelidad (Gold)"
        }
      ],
      "discountCalculated": 30.00,
      "discountFinal": 30.00,
      "discountExceeded": false,
      "currencyCode": "USD",
      "exceededReason": null
    }
  ],
  "pagination": {
    "totalElements": 142,
    "totalPages": 8,
    "currentPage": 1,
    "pageSize": 20,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

**Response (400 Bad Request):**

```json
{
  "error": "VALIDATION_ERROR",
  "message": "page debe ser >= 1",
  "timestamp": "2026-03-30T14:30:00Z",
  "path": "/api/v1/discount-history"
}
```

**Response (401 Unauthorized):**

```json
{
  "error": "UNAUTHORIZED",
  "message": "Token JWT no válido o expirado",
  "timestamp": "2026-03-30T14:30:00Z",
  "path": "/api/v1/discount-history"
}
```

---

### 2.5 Componentes Frontend (React)

#### Componente: `DiscountHistory.jsx` (Nueva página)

**Características:**
- Tabla con columnas: Transaction ID, Timestamp, Reglas Aplicadas, Descuento Calculado, Descuento Final, Estado
- Filtro por rango de fechas (DatePicker from/to)
- Indicador visual (badge) para transacciones con descuento truncado
- Tooltip con razón del truncado
- Paginación: botones anterior/siguiente + selector de página
- Loading state mientras se cargan datos
- Mensaje de error si la consulta falla
- Soporte para ordenamiento por timestamp

**Estructura:**
```
frontend/src/
  pages/
    DiscountHistory.jsx      (componente principal)
    DiscountHistory.module.css
  services/
    discountHistoryService.js (llamadas a API)
  components/
    TransactionRow.jsx       (fila de la tabla)
    TransactionRow.module.css
```

**API Service: `discountHistoryService.js`**

```javascript
import axios from 'axios';
const API_BASE = import.meta.env.VITE_API_URL;

export async function getDiscountHistory(filters, token) {
  // filters = { fromDate, toDate, page, size, sort }
  const params = new URLSearchParams();
  if (filters.fromDate) params.append('from_date', filters.fromDate);
  if (filters.toDate) params.append('to_date', filters.toDate);
  if (filters.page) params.append('page', filters.page);
  if (filters.size) params.append('size', filters.size);
  if (filters.sort) params.append('sort', filters.sort);

  const res = await axios.get(
    `${API_BASE}/api/v1/discount-history?${params.toString()}`,
    {
      headers: { Authorization: `Bearer ${token}` }
    }
  );
  return res.data;
}
```

---

### 2.6 Flujo Event-Driven: RabbitMQ → Service-Admin

#### Service-Engine (NO CAMBIA)

- Sigue emitiendo `CalculationProcessedEvent` (ver SPEC-011) tras cada cálculo de descuentos.
- Event payload incluye: `transactionId`, `ecommerceId`, `timestampUtc`, `payloadReceived`, `rulesApplied`, `discountCalculated`, `discountFinal`, `discountExceeded`.
- Exchange: `discount-exchange` | Routing key: `calculation.processed`

#### Service-Admin: CalculationEventConsumer (NUEVO)

**Responsabilidades:**
1. Escuchar eventos `CalculationProcessedEvent` desde RabbitMQ
2. **Sanitizar el payload** — eliminar campos PII (emails, nombres, teléfonos) si están presentes
3. Persistir en `transaction_logs` de `loyalty_admin` BD
4. Loguear errores pero NO fallar la transacción original en Engine

**Código esquemático:**

```java
@Service
public class CalculationEventConsumer {

    @RabbitListener(queues = "calculation.processed.queue")
    public void handleCalculationProcessedEvent(CalculationProcessedEvent event) {
        // 1. Sanitizar
        String sanitizedPayload = sanitizePayload(event.getPayloadReceived());
        String sanitizedRules = sanitizeRules(event.getRulesApplied());
        
        // 2. Crear entidad
        TransactionLog log = TransactionLog.builder()
            .ecommerceId(event.getEcommerceId())
            .transactionId(event.getTransactionId())
            .timestampUtc(event.getTimestampUtc())
            .payloadReceived(sanitizedPayload)
            .rulesApplied(sanitizedRules)
            .discountCalculated(event.getDiscountCalculated())
            .discountFinal(event.getDiscountFinal())
            .discountExceeded(event.isDiscountExceeded())
            .currencyCode(event.getCurrencyCode())
            .build();
        
        // 3. Guardar
        transactionLogRepository.save(log);
    }
    
    private String sanitizePayload(String jsonPayload) {
        // Remover campos que parezcan PII: email, name, phone, ssn, etc.
        // Usar Jackson ObjectMapper para manipular JSON de forma segura
        // Retornar JSON limpio
    }
    
    private String sanitizeRules(String jsonRules) {
        // Similar: remover cualquier referencia a cliente final
    }
}
```

**RabbitMQ Configuration (Service-Admin):**

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

# application.yml o application-admin.yml
rabbitmq:
  calculation.processed.queue: calculation.processed.queue
  calculation.processed.exchange: discount-exchange
  calculation.processed.routing-key: calculation.processed
```

```java
@Configuration
public class RabbitMQConfigAdmin {

    public static final String CALCULATION_QUEUE = "calculation.processed.queue";
    public static final String DISCOUNT_EXCHANGE = "discount-exchange";
    public static final String CALCULATION_ROUTING_KEY = "calculation.processed";

    @Bean
    public Queue calculationQueue() {
        return new Queue(CALCULATION_QUEUE, true);
    }

    @Bean
    public TopicExchange discountExchange() {
        return new TopicExchange(DISCOUNT_EXCHANGE, true, false);
    }

    @Bean
    public Binding calculationBinding() {
        return BindingBuilder.bind(calculationQueue())
                .to(discountExchange())
                .with(CALCULATION_ROUTING_KEY);
    }
}
```

---

## 3. LISTA DE TAREAS

### Backend (Service-Admin 8081) — Consumer Event-Driven + API Lectura

**Fase 1: Infraestructura RabbitMQ**
- [ ] Crear `RabbitMQConfigAdmin` con queue `calculation.processed.queue`, exchange `discount-exchange`, routing key `calculation.processed`
- [ ] Crear DTO `CalculationProcessedEvent` (deserialization desde RabbitMQ)

**Fase 2: Persistencia**
- [ ] Crear migración `V13__Create_transaction_logs_table.sql` en service-admin
- [ ] Crear entidad JPA: `TransactionLog` en domain/entity
- [ ] Crear repositorio: `TransactionLogRepository` con método `findByEcommerceIdAndTimestampUtcBetween(..., Pageable)`

**Fase 3: Event Consumer (Core Business Logic)**
- [ ] Crear servicio `CalculationEventConsumer` decorador con `@RabbitListener`
- [ ] Implementar método `sanitizePayload(String)` — remover emails, nombres, teléfonos del JSON
- [ ] Implementar método `sanitizeRules(String)` — asegurar que rules no contienen PII
- [ ] Implementar persistencia en transaction_logs (usar `@Transactional`)
- [ ] Loguear errores sin fallar la transacción del Engine (log level ERROR, no throw)

**Fase 4: Servicio de Lectura + Controller**
- [ ] Crear servicio `TransactionHistoryService` con método `getDiscountHistory(UUID ecommerceId, LocalDateTime from, LocalDateTime to, Pageable)`
- [ ] Validar que ecommerce_id pertenece al usuario autenticado (desde JWT)
- [ ] Validar que rango de fechas <= 7 días (si excede, truncar y loguear warning)
- [ ] Crear DTO `TransactionLogResponse` (para serializar sin campos sensibles)
- [ ] Crear DTO `DiscountHistoryFilterRequest` (from_date, to_date, page, size)
- [ ] Crear controlador `DiscountHistoryController` con endpoint GET `/api/v1/discount-history`

**Fase 5: Validaciones + Excepciones**
- [ ] Validar JWT y extraer ecommerce_id del token
- [ ] Validar parámetros: page >= 1, size <= 100
- [ ] Validar rango de fechas: from_date < to_date
- [ ] Retornar 401 si token inválido
- [ ] Retornar 400 si parámetros inválidos
- [ ] Retornar 403 si ecommerce_id del request =/= ecommerce_id del usuario

**Fase 6: Tests Backend**
- [ ] Test unitario: `CalculationEventConsumer.sanitizePayload()` — verificar que emails/nombres se remuevan
- [ ] Test unitario: `TransactionHistoryService.getDiscountHistory()` — happy path con paginación
- [ ] Test unitario: validación de rango de fechas (truncar si > 7 días)
- [ ] Test de integración: RabbitMQ consumer escucha evento e inserta en BD
- [ ] Test de controlador: endpoint retorna 200 con payload correcto
- [ ] Test de seguridad: 401 sin JWT, 403 si ecommerce_id no coincide

### Frontend (React)

- [ ] Crear página `DiscountHistory.jsx` con filtros y tabla
- [ ] Crear servicio `discountHistoryService.js` para llamadas API
- [ ] Crear componente `TransactionRow.jsx` para cada fila de la tabla
- [ ] Implementar indicador visual (badge "TRUNCADO") para `discountExceeded = true`
- [ ] Implementar paginación (botones, selector de página)
- [ ] Implementar filtro por rango de fechas (DatePicker)
- [ ] Implementar loading state y estado de error
- [ ] Crear estilos CSS Module
- [ ] Tests unitarios: rendering, filtros, paginación

### QA

- [ ] Caso Gherkin: Flujo feliz (consulta 7 días por defecto)
- [ ] Caso Gherkin: Filtro por rango personalizado
- [ ] Caso Gherkin: Resaltar truncados
- [ ] Caso Gherkin: Validaciones (401, 400)
- [ ] Validación de privacidad: verificar que no aparecen datos personales en response
- [ ] Test de paginación: navegar por múltiples páginas
- [ ] Test de performance: 1000+ registros con paginación
- [ ] Risk Assessment: datos sensibles, retención, carga de API
