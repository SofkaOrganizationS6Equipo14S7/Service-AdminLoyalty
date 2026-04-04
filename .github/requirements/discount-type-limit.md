# HU-XX: Límite de Descuento por Tipo Específico

## Historia de Usuario

Como usuario de LOYALTY, quiero definir un límite máximo de descuento diferente para cada tipo de descuento, para tener control granular sobre la rentabilidad del negocio por categoría.

## Criterios de Aceptación

### Scenario: Configuración exitosa de límites por tipo
**Given** existen tipos de descuento disponibles en el ecommerce  
**And** el límite propuesto para cada tipo es un valor positivo  
**When** se registra la configuración de límites por tipo  
**Then** cada tipo de descuento queda configurado con su límite específico  
**And** el motor respeta el límite correspondiente al tipo al aplicar descuentos

### Scenario: Actualización de límite por tipo específico
**Given** existe una configuración de límites por tipo vigente  
**When** se actualiza el límite de un tipo específico  
**Then** el sistema guarda el nuevo límite  
**And** las nuevas evaluaciones consideran el límite actualizado

### Scenario: Rechazo por límite inválido
**Given** existen límites de negocio para proteger la rentabilidad  
**When** se intenta registrar un límite menor o igual a cero para un tipo  
**Then** la configuración es rechazada  
**And** se mantiene la última configuración válida

### Scenario: Aplicación del límite por tipo
**Given** existe una configuración de límites por tipo vigente  
**And** un descuento de tipo específico supera su límite configurado  
**When** el motor calcula el descuento total  
**Then** el descuento de ese tipo se limita a su máximo específico

### Scenario: Fallback a límite global cuando no hay límite por tipo
**Given** existe un límite global configurado  
**And** no existe límite específico para un tipo de descuento  
**When** el motor aplica ese tipo de descuento  
**Then** se utiliza el límite global como tope

## Notas Técnicas (Constraints)

- **Stack:** Java 21 + Spring Boot 3.x.
- **Auth:** JWT para gestión de configuración.
- **Almacenamiento:** Nueva tabla `discount_type_limits` o extensión de `discount_settings`.
- **Performance:** La validación de límites debe ejecutarse en memoria.
- **Sync:** Sincronización de configuración vía RabbitMQ si es necesario.
- Cada tipo de descuento puede tener su propio límite máximo.
- Si no existe límite por tipo, se utiliza el límite global como fallback.
- Los límites por tipo deben ser números positivos mayores a cero.
