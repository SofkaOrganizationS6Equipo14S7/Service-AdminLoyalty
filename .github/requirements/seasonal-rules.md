# HU-05: Gestión de Reglas de Temporada (Seasonal Rules)

## Historia de Usuario

Como usuario de LOYALTY (STORE_ADMIN), quiero crear, editar y eliminar reglas de temporada para automatizar las promociones por demanda de temporada.

## Criterios de Aceptación

### Scenario: Creación exitosa de una regla de temporada
**Given** no hay una regla activa para esa temporada en el ecommerce  
**When** se ejecuta POST /api/v1/rules con type derivado de discountTypeId (SEASONAL), name, discountPercentage, startDate, endDate  
**Then** la regla se almacena en tabla `rules` con type=SEASONAL  
**And** los atributos startDate y endDate se guardan en tabla `rule_attributes` (key-value pairs)  
**And** la regla queda disponible para su aplicación durante la vigencia definida

### Scenario: Rechazo de creación por superposición de fechas
**Given** ya hay una regla activa de tipo SEASONAL para esa temporada  
**When** se ejecuta POST /api/v1/rules con type=SEASONAL y fechas overlapping  
**Then** el sistema rechaza el registro con HTTP 400  
**And** informa el conflicto de superposición de fechas entre reglas de temporada

### Scenario: Validación de rango de fechas (startDate < endDate)
**Given** se intenta crear una regla de temporada  
**When** startDate >= endDate  
**Then** el sistema rechaza la creación con HTTP 400  
**And** informa que startDate debe ser menor que endDate

### Scenario: Edición exitosa de una regla de temporada
**Given** existe una regla de temporada registrada en `rules` (type=SEASONAL)  
**When** se ejecuta PUT /api/v1/rules/{ruleId} con campos actualizables (name, startDate, endDate, discountPercentage)  
**Then** el sistema actualiza la regla en tabla `rules`  
**And** los atributos en `rule_attributes` se actualizan correspondentemente  
**And** la versión actualizada es la considerada para nuevas evaluaciones

### Scenario: Eliminación exitosa de una regla de temporada (soft delete)
**Given** existe una regla de temporada registrada (type=SEASONAL)  
**When** se ejecuta DELETE /api/v1/rules/{ruleId}  
**Then** la regla marca is_active=false en tabla `rules`  
**And** deja de participar en evaluaciones futuras

### Scenario: Rechazo de eliminación de regla inexistente
**Given** no existe una regla (SEASONAL) asociada al identificador solicitado  
**When** se ejecuta DELETE /api/v1/rules/{ruleId}  
**Then** el sistema rechaza la operación con HTTP 404  
**And** informa que no existe una regla para el identificador solicitado

## Notas Técnicas (Constraints)

- **Stack:** Java 21 + Spring Boot 3.x.
- **Auth:** JWT para gestión de reglas (@PreAuthorize("hasRole('STORE_ADMIN')")).
- **Almacenamiento:** PostgreSQL con tabla unificada `rules` normalizada:
  - `rules`: almacena id, discount_priority_id, name, discount_percentage, is_active, created_at, updated_at
  - `rule_attributes`: key-value pairs para startDate, endDate (en formato ISO-8601 string)
  - El tipo SEASONAL se deriva de: `discount_priority -> discount_type_id -> code='SEASONAL'`
- **Endpoint:** POST/GET/PUT/DELETE /api/v1/rules (el tipo se determina por discount_priority)
- **Performance:** Las reglas deben cachearse en memoria (Caffeine) en Engine Service.
- **Sync:** Sincronización de cambios vía RabbitMQ al Engine Service.
- Validación: startDate < endDate (obligatorio).
- Las reglas de temporada (type=SEASONAL en atributos) tienen: nombre, startDate, endDate, porcentaje de descuento.
- No puede haber superposición de fechas para el mismo ecommerce y tipo de regla.
- Los límites de descuento (mín/máx) se validan contra la configuración global (discount_config).
- Las reglas eliminadas (soft delete: is_active=false) no participan en evaluaciones futuras.
