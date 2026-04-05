## HU-06: Gestión de Reglas de Temporada

Como usuario de LOYALTY, quiero crear, editar y eliminar reglas de temporada para automatizar las promociones por demanda de temporada.

### Criterios de Aceptación

#### Scenario: Creación exitosa de una regla de temporada
**Given** no hay una regla activa para esa temporada  
**When** se registra una regla de descuento con vigencia y beneficio válidos  
**Then** la regla queda almacenada en el sistema  
**And** la regla queda disponible para su aplicación durante la vigencia definida

#### Scenario: Rechazo de creación por superposición de fechas
**Given** ya hay una regla activa para esa temporada  
**When** se intenta registrar una nueva regla con las mismas fechas  
**Then** el sistema rechaza el registro  
**And** informa el conflicto de superposición de fechas entre reglas de temporada

#### Scenario: Edición exitosa de una regla de temporada
**Given** existe una regla de temporada registrada  
**When** se actualizan sus condiciones con valores válidos  
**Then** el sistema conserva la regla con la nueva configuración  
**And** la versión actualizada es la considerada para nuevas evaluaciones

#### Scenario: Rechazo de edición por incumplir rangos
**Given** existen límites de descuento definidos  
**When** la actualización propuesta excede los límites permitidos  
**Then** el sistema rechaza la modificación  
**And** mantiene la última configuración válida

#### Scenario: Eliminación exitosa de una regla de temporada
**Given** existe una regla de temporada registrada  
**When** se confirma su eliminación  
**Then** la regla deja de participar en evaluaciones futuras

#### Scenario: Rechazo de eliminación de regla inexistente
**Given** no existe una regla asociada al identificador solicitado  
**When** se solicita su eliminación  
**Then** el sistema rechaza la operación  
**And** informa que no existe una regla para el identificador solicitado

#### Scenario: Rechazo de edición de regla inexistente
**Given** no existe una regla asociada al identificador solicitado  
**When** se solicita su edición  
**Then** el sistema rechaza la operación  
**And** informa que no existe una regla para el identificador solicitado

### Notas Técnicas (Constraints)

- **Stack:** Java 21 + Spring Boot 3.x.
- **Auth:** JWT para gestión de reglas.
- **Almacenamiento:** PostgreSQL con tablas para `seasonal_rules`.
- **Performance:** Las reglas deben evaluarse en memoria para no impactar la latencia del /calculate.
- **Sync:** Sincronización de reglas vía RabbitMQ al Engine Service.
- Las reglas de temporada tienen: nombre, fecha inicio, fecha fin, porcentaje de descuento, tipo de descuento.
- No puede haber superposición de fechas para el mismo ecommerce.
- Los límites de descuento (mín/máx) deben validarse contra la configuración global.
- Las reglas eliminadas no participan en evaluaciones futuras.
