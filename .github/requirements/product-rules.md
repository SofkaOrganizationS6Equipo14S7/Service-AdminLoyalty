# HU-07: Gestión de Reglas por Tipo de Producto

## Historia de Usuario

Como usuario de LOYALTY, quiero crear, editar y eliminar reglas por tipo de producto, para automatizar las promociones en base a inventario.

## Criterios de Aceptación

### Scenario: Creación exitosa de una regla por tipo de producto
**Given** no hay una regla activa para ese tipo de producto  
**When** se define una nueva regla de descuento con parámetros válidos  
**Then** la regla queda registrada  
**And** la regla puede ser aplicada por el motor

### Scenario: Rechazo de creación por duplicidad
**Given** existe una regla para el mismo tipo de producto  
**When** se registra una regla  
**Then** el sistema rechaza la creación  
**And** reporta conflicto de reglas para ese tipo de producto

### Scenario: Edición exitosa de una regla por tipo de producto
**Given** existe una regla por tipo de producto registrada  
**When** se modifican sus parámetros dentro de los límites permitidos  
**Then** el sistema guarda la nueva versión  
**And** las nuevas evaluaciones consideran la configuración actualizada

### Scenario: Rechazo de edición por datos incompletos
**Given** la regla requiere tipo de producto y beneficio para ser válida  
**When** la actualización omite uno o más datos obligatorios  
**Then** el sistema rechaza la modificación  
**And** mantiene la versión previamente válida

### Scenario: Eliminación exitosa de una regla por tipo de producto
**Given** existe una regla por tipo de producto  
**When** se solicita su eliminación  
**Then** la regla deja de estar disponible para nuevas transacciones

### Scenario: Rechazo de eliminación por regla no encontrada
**Given** no existe una regla para el tipo de producto indicado  
**When** se intenta eliminar la regla  
**Then** el sistema rechaza la operación  
**And** mantiene sin cambios la configuración

### Scenario: Rechazo de edición de regla inexistente
**Given** no existe una regla asociada al tipo de producto indicado  
**When** se solicita su edición  
**Then** el sistema rechaza la operación  
**And** informa que no existe una regla para el identificador solicitado

## Notas Técnicas (Constraints)

- **Stack:** Java 21 + Spring Boot 3.x.
- **Auth:** JWT para gestión de reglas.
- **Almacenamiento:** PostgreSQL con tablas para `product_rules`.
- **Performance:** Las reglas deben evaluarse en memoria para no impactar la latencia del /calculate.
- **Sync:** Sincronización de reglas vía RabbitMQ al Engine Service.
- Las reglas por tipo de producto tienen: nombre, tipo de producto, porcentaje de descuento, beneficio.
- Solo puede existir una regla activa por tipo de producto.
- Los límites de descuento (mín/máx) deben validarse contra la configuración global.
- Las reglas eliminadas no participan en evaluaciones futuras.
