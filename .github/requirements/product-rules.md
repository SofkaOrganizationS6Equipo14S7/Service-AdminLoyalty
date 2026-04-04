# HU-07: Gestión de Reglas por Tipo de Producto

## Historia de Usuario

Como usuario de LOYALTY, quiero crear, editar y eliminar reglas por tipo de producto, para automatizar las promociones en base a inventario.

## Criterios de Aceptación

### Scenario: Creación exitosa de una regla por tipo de producto
**Given** no hay una regla activa para ese tipo de producto  
**When** se ejecuta POST /api/v1/rules con { type: "PRODUCT", name, productType, discountPercentage, discountTypeId }  
**Then** la regla queda registrada  
**And** la regla puede ser aplicada por el motor

### Scenario: Rechazo de creación por duplicidad
**Given** existe una regla activa para el mismo tipo de producto  
**When** se ejecuta POST /api/v1/rules con type=PRODUCT y productType duplicado  
**Then** el sistema rechaza la creación con HTTP 409  
**And** reporta conflicto de reglas para ese tipo de producto

### Scenario: Edición exitosa de una regla por tipo de producto
**Given** existe una regla por tipo de producto registrada (type=PRODUCT)  
**When** se ejecuta PUT /api/v1/rules/{uid} con parámetros actualizables (name, productType, discountPercentage, discountTypeId)  
**Then** el sistema guarda la nueva versión  
**And** las nuevas evaluaciones consideran la configuración actualizada

### Scenario: Rechazo de edición por datos incompletos
**Given** la regla requiere tipo de producto y beneficio para ser válida  
**When** la actualización omite uno o más datos obligatorios  
**Then** el sistema rechaza la modificación  
**And** mantiene la versión previamente válida

### Scenario: Eliminación exitosa de una regla por tipo de producto (soft delete)
**Given** existe una regla por tipo de producto (type=PRODUCT)  
**When** se ejecuta DELETE /api/v1/rules/{uid}  
**Then** la regla marca is_active=false y deja de estar disponible para nuevas transacciones

### Scenario: Rechazo de eliminación por regla no encontrada
**Given** no existe una regla (type=PRODUCT) para el tipo de producto indicado  
**When** se ejecuta DELETE /api/v1/rules/{uid}  
**Then** el sistema rechaza la operación con HTTP 404  
**And** mantiene sin cambios la configuración

### Scenario: Rechazo de edición de regla inexistente
**Given** no existe una regla (type=PRODUCT) asociada al tipo de producto indicado  
**When** se ejecuta PUT /api/v1/rules/{uid}  
**Then** el sistema rechaza la operación con HTTP 404  
**And** informa que no existe una regla para el identificador solicitado

## Notas Técnicas (Constraints)

- **Stack:** Java 21 + Spring Boot 3.x.
- **Auth:** JWT para gestión de reglas.
- **Almacenamiento:** PostgreSQL con tabla unificada `rules` (type=PRODUCT).
- **Endpoint Unificado:** POST/GET/PUT/DELETE /api/v1/rules (parámetro type diferencia tipos de regla)
- **Performance:** Las reglas deben evaluarse en memoria para no impactar la latencia del /calculate.
- **Sync:** Sincronización de reglas vía RabbitMQ al Engine Service.
- Las reglas por tipo de producto (type=PRODUCT) tienen: nombre, tipo de producto, porcentaje de descuento, tipo de descuento.
- Solo puede existir una regla activa por tipo de producto en el mismo ecommerce.
- Los límites de descuento (mín/máx) deben validarse contra la configuración global.
- Las reglas eliminadas (soft delete: is_active=false) no participan en evaluaciones futuras.
