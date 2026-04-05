# HU-07: Gestión de Reglas de Clasificación de Clientes (Classification Rules)

## Historia de Usuario

Como usuario de LOYALTY (STORE_ADMIN), quiero crear reglas de clasificación de clientes por nivel de fidelidad (customer_tiers), para aplicar diferentes descuentos según el comportamiento y métricas del cliente.

## Criterios de Aceptación

### Scenario: Creación exitosa de una regla de clasificación
**Given** existe un nivel de fidelidad (customer_tier) disponible en el sistema  
**And** no hay una regla activa para ese tier con el mismo metricType  
**When** se ejecuta POST /api/v1/rules/customer-tiers/{tierId} con metricType (total_spent|order_count|loyalty_points|custom), minValue, maxValue, priority  
**Then** la regla se almacena en tabla `rules` con type=CLASSIFICATION  
**And** se registra la relación en tabla `rule_customer_tiers` (junction table)  
**And** los atributos (metricType, minValue, maxValue, priority) se guardan en `rule_attributes`

### Scenario: Validación de rango de valores (minValue < maxValue)
**Given** se intenta crear una regla de clasificación  
**When** minValue >= maxValue  
**Then** el sistema rechaza con HTTP 400  
**And** informa que minValue debe ser menor que maxValue

### Scenario: Validación de metricType enum
**Given** se intenta crear una regla de clasificación  
**When** metricType no está en la lista permitida (total_spent, order_count, loyalty_points, custom)  
**Then** el sistema rechaza con HTTP 400  
**And** informa que el metricType no es válido

### Scenario: Listar reglas de clasificación para un tier
**Given** existe un customer_tier con identificador {tierId}  
**When** se ejecuta GET /api/v1/rules/customer-tiers/{tierId}  
**Then** devuelve lista paginada de reglas de clasificación para ese tier  
**And** cada respuesta incluye metricType, minValue, maxValue, priority, createdAt, updatedAt

### Scenario: Obtener detalles de una regla de clasificación
**Given** existe una regla de clasificación para un tier  
**When** se ejecuta GET /api/v1/rules/customer-tiers/{tierId}/{ruleId}  
**Then** devuelve detalles completos de la regla con todos sus atributos

### Scenario: Edición exitosa de una regla de clasificación
**Given** existe una regla de clasificación registrada  
**When** se ejecuta PUT /api/v1/rules/customer-tiers/{tierId}/{ruleId} con campos actualizables (metricType, minValue, maxValue, priority)  
**Then** el sistema actualiza la regla en tabla `rules`  
**And** los atributos en `rule_attributes` se actualizan correspondentemente  
**And** valida nuevamente que minValue < maxValue

### Scenario: Eliminación exitosa de una regla de clasificación (soft delete)
**Given** existe una regla de clasificación registrada  
**When** se ejecuta DELETE /api/v1/rules/customer-tiers/{tierId}/{ruleId}  
**Then** la regla marca is_active=false en tabla `rules`  
**And** deja de participar en la evaluación de clasificación de clientes

### Scenario: Rechazo de operación con tier inexistente
**Given** no existe un customer_tier con el identificador {tierId}  
**When** se intenta crear, editar o eliminar una regla para ese tier  
**Then** el sistema rechaza con HTTP 404  
**And** informa que el tier no existe

### Scenario: Rechazo de operación con regla inexistente
**Given** no existe una regla de clasificación para el ruleId especificado  
**When** se intenta editar o eliminar esa regla  
**Then** el sistema rechaza con HTTP 404  
**And** informa que la regla no existe

## Notas Técnicas (Constraints)

- **Stack:** Java 21 + Spring Boot 3.x.
- **Auth:** JWT para gestión de reglas (@PreAuthorize("hasRole('STORE_ADMIN')")).
- **Almacenamiento:** PostgreSQL con tabla unificada `rules` normalizada:
  - `rules`: almacena id, discount_priority_id, name, discount_percentage, is_active, created_at, updated_at
  - `rule_customer_tiers`: junction table que vincula rules con customer_tiers (many-to-many)
  - `rule_attributes`: key-value pairs con metricType, minValue, maxValue, priority
  - El tipo CLASSIFICATION se deriva de: `discount_priority -> discount_type_id -> code='CLASSIFICATION'`
- **Endpoints:**
  - POST /api/v1/rules/customer-tiers/{tierId} - Crear regla
  - GET /api/v1/rules/customer-tiers/{tierId} - Listar reglas del tier
  - GET /api/v1/rules/customer-tiers/{tierId}/{ruleId} - Detalle de regla
  - PUT /api/v1/rules/customer-tiers/{tierId}/{ruleId} - Actualizar regla
  - DELETE /api/v1/rules/customer-tiers/{tierId}/{ruleId} - Eliminar (soft delete)
  
- **Métricas Permitidas:** total_spent, order_count, loyalty_points, custom
- **Validación:** minValue < maxValue es obligatoria.
- **Performance:** Las reglas se cachean en Engine Service (Caffeine) para evaluación rápida.
- **Sync:** Cambios se sincronizan vía RabbitMQ al Engine Service.
- **Atomicidad:** Las operaciones de crear/editar/eliminar son atómicas (transaccionales).
- Las reglas eliminadas (soft delete: is_active=false) no participan en evaluaciones futuras.
- Cada tier puede tener múltiples reglas de clasificación (uno a muchos vía junction table).
