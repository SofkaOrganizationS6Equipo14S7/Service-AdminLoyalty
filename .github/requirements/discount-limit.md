# HU-07: Límite y Prioridad de Descuentos

## Historia de Usuario

Como usuario de LOYALTY, quiero definir el tope máximo y la prioridad de descuentos, para proteger la rentabilidad del negocio.

## Criterios de Aceptación

### Scenario: Configuración válida de tope y prioridad
**Given** existen tipos de descuento disponibles en el ecommerce  
**And** el tope máximo propuesto es un valor positivo  
**And** la prioridad define un orden único entre descuentos  
**When** se registra la configuración de topes y prioridad  
**Then** la configuración queda vigente para el ecommerce  
**And** el motor utiliza ese orden para resolver descuentos concurrentes

### Scenario: Rechazo por tope máximo inválido
**Given** existen límites de negocio para proteger la rentabilidad  
**When** se intenta registrar un tope máximo menor o igual a cero  
**Then** la configuración es rechazada  
**And** se mantiene la última configuración válida

### Scenario: Rechazo por prioridad ambigua
**Given** la prioridad de descuentos debe ser determinística  
**When** se registra una prioridad con empates o niveles duplicados  
**Then** la configuración es rechazada  
**And** no se altera la prioridad vigente

### Scenario: Aplicación del tope ante acumulación de descuentos
**Given** existe una configuración vigente de tope y prioridad  
**And** una transacción califica para múltiples descuentos  
**When** el descuento total calculado supera el tope máximo  
**Then** el descuento final aplicado se limita al tope máximo configurado

## Notas Técnicas (Constraints)

- **Stack:** Java 21 + Spring Boot 3.x.
- **Auth:** JWT para gestión de configuración.
- **Almacenamiento:** PostgreSQL con tablas para `discount_config`.
- **Performance:** La validación de límites debe ejecutarse en memoria para no impactar la latencia del `/calculate`.
- **Sync:** Sincronización de configuración vía RabbitMQ si es necesario.
- El tope máximo debe ser un número positivo mayor a cero.
- Cada tipo de descuento debe tener un valor de prioridad único.
- La resolución de descuentos debe ser atómica para evitar condiciones de carrera.
- Se debe definir una prioridad por defecto si no existe configuración.
