# HU-06: Gestión de Reglas por Tipo de Producto (Product Rules)

## Historia de Usuario

Como usuario de LOYALTY (STORE_ADMIN), quiero crear, editar y eliminar reglas por tipo de producto, para automatizar las promociones en base al inventario y categorías de producto.

## Criterios de Aceptación

### Scenario: Creación exitosa de una regla por tipo de producto
**Given** no hay una regla activa para ese tipo de producto en el ecommerce  
**When** se ejecuta POST /api/v1/rules con type derivado de discountTypeId (PRODUCT), name, discountPercentage, productType en atributos  
**Then** la regla se almacena en tabla `rules` con type=PRODUCT  
**And** el atributo productType se guarda en tabla `rule_attributes` (clave-valor)  
**And** la regla queda disponible para evaluación inmediata

### Scenario: Rechazo de creación por duplicidad de productType
**Given** existe una regla activa para el mismo productType en el mismo ecommerce  
**When** se ejecuta POST /api/v1/rules con type=PRODUCT y productType duplicado  
**Then** el sistema rechaza la creación con HTTP 409  
**And** reporta conflicto: "Ya existe una regla activa para este tipo de producto"

### Scenario: Validación de existencia de atributo productType
**Given** se intenta crear una regla de tipo PRODUCT  
**When** el atributo productType es null o vacío  
**Then** el sistema rechaza la creación con HTTP 400  
**And** informa que productType es obligatorio

### Scenario: Edición exitosa de una regla por tipo de producto
**Given** existe una regla por tipo de producto registrada en `rules` (type=PRODUCT)  
**When** se ejecuta PUT /api/v1/rules/{ruleId} con parámetros actualizables (name, productType, discountPercentage)  
**Then** el sistema actualiza la regla en tabla `rules`  
**And** los atributos en `rule_attributes` se actualizan correspondentemente  
**And** si el productType cambió, valida que no exista otro conflicto

### Scenario: Rechazo de edición por cambio a productType duplicado
**Given** existe una regla activa con productType='electronics'  
**When** se intenta actualizar otra regla para cambiar su productType a 'electronics'  
**Then** el sistema rechaza con HTTP 409  
**And** informa del conflicto de duplicidad

### Scenario: Eliminación exitosa de una regla por tipo de producto (soft delete)
**Given** existe una regla por tipo de producto (type=PRODUCT)  
**When** se ejecuta DELETE /api/v1/rules/{ruleId}  
**Then** la regla marca is_active=false en tabla `rules`  
**And** deja de estar disponible para nuevas transacciones

### Scenario: Rechazo de eliminación por regla no encontrada
**Given** no existe una regla (type=PRODUCT) para el identificador indicado  
**When** se ejecuta DELETE /api/v1/rules/{ruleId}  
**Then** el sistema rechaza la operación con HTTP 404

## Notas Técnicas (Constraints)

- **Stack:** Java 21 + Spring Boot 3.x.
- **Auth:** JWT para gestión de reglas (@PreAuthorize("hasRole('STORE_ADMIN')")).
- **Almacenamiento:** PostgreSQL con tabla unificada `rules` normalizada:
  - `rules`: almacena id, discount_priority_id, name, discount_percentage, is_active, created_at, updated_at
  - `rule_attributes`: key-value pairs con productType (atributo requerido)
  - El tipo PRODUCT se deriva de: `discount_priority -> discount_type_id -> code='PRODUCT'`
- **Endpoint:** POST/GET/PUT/DELETE /api/v1/rules (el tipo se determina por discount_priority)
- **Validación:** productType es atributo requerido y único por ecommerce.
- **Unicidad:** Solo puede existir UNA regla activa por productType en el mismo ecommerce.
- **Performance:** Las reglas se cacheан en Engine Service (Caffeine).
- **Sync:** Cambios se sincronizan vía RabbitMQ al Engine Service.
- Los límites de descuento (mín/máx) se validan contra discount_config global.
- Las reglas eliminadas (soft delete: is_active=false) no participan en evaluaciones.
