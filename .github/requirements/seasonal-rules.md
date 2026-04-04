# HU-06: Gestión de Reglas de Temporada

## Historia de Usuario

Como usuario de LOYALTY, quiero crear, editar y eliminar reglas de temporada para automatizar las promociones por demanda de temporada.

## Criterios de Aceptación

### Scenario: Creación exitosa de una regla de temporada
**Given** no hay una regla activa para esa temporada  
**When** se ejecuta POST /api/v1/rules con { type: "SEASONAL", name, description, discountPercentage, startDate, endDate, discountTypeId }  
**Then** la regla queda almacenada en el sistema  
**And** la regla queda disponible para su aplicación durante la vigencia definida

### Scenario: Rechazo de creación por superposición de fechas
**Given** ya hay una regla activa de tipo SEASONAL para esa temporada  
**When** se ejecuta POST /api/v1/rules con type=SEASONAL y fechas overlapping  
**Then** el sistema rechaza el registro con HTTP 400  
**And** informa el conflicto de superposición de fechas entre reglas de temporada

### Scenario: Edición exitosa de una regla de temporada
**Given** existe una regla de temporada registrada (type=SEASONAL)  
**When** se ejecuta PUT /api/v1/rules/{uid} con campos actualizables (name, description, startDate, endDate, discountPercentage)  
**Then** el sistema conserva la regla con la nueva configuración  
**And** la versión actualizada es la considerada para nuevas evaluaciones

### Scenario: Rechazo de edición por incumplir rangos
**Given** existen límites de descuento definidos  
**When** la actualización propuesta excede los límites permitidos  
**Then** el sistema rechaza la modificación  
**And** mantiene la última configuración válida

### Scenario: Eliminación exitosa de una regla de temporada (soft delete)
**Given** existe una regla de temporada registrada (type=SEASONAL)  
**When** se ejecuta DELETE /api/v1/rules/{uid}  
**Then** la regla marca is_active=false y deja de participar en evaluaciones futuras

### Scenario: Rechazo de eliminación de regla inexistente
**Given** no existe una regla (SEASONAL) asociada al identificador solicitado  
**When** se ejecuta DELETE /api/v1/rules/{uid}  
**Then** el sistema rechaza la operación con HTTP 404  
**And** informa que no existe una regla para el identificador solicitado

### Scenario: Rechazo de edición de regla inexistente
**Given** no existe una regla (SEASONAL) asociada al identificador solicitado  
**When** se ejecuta PUT /api/v1/rules/{uid}  
**Then** el sistema rechaza la operación con HTTP 404  
**And** informa que no existe una regla para el identificador solicitado

## Notas Técnicas (Constraints)

- **Stack:** Java 21 + Spring Boot 3.x.
- **Auth:** JWT para gestión de reglas.
- **Almacenamiento:** PostgreSQL con tabla unificada `rules` (type=SEASONAL).
- **Endpoint Unificado:** POST/GET/PUT/DELETE /api/v1/rules (parámetro type diferencia tipos de regla)
- **Performance:** Las reglas deben evaluarse en memoria para no impactar la latencia del /calculate.
- **Sync:** Sincronización de reglas vía RabbitMQ al Engine Service.
- Las reglas de temporada (type=SEASONAL) tienen: nombre, fecha inicio, fecha fin, porcentaje de descuento, tipo de descuento.
- No puede haber superposición de fechas para el mismo ecommerce y tipo de regla.
- Los límites de descuento (mín/máx) deben validarse contra la configuración global.
- Las reglas eliminadas (soft delete: is_active=false) no participan en evaluaciones futuras.
