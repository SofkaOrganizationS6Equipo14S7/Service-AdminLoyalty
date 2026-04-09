## HU-14: Activar/Desactivar Reglas

Como usuario de LOYALTY, quiero poder activar o desactivar reglas con un solo click, para reaccionar rápidamente a cambios en el mercado sin tener que borrar la configuración.

### Criterios de Aceptación

#### Scenario: Desactivación inmediata de una regla activa
**Given** que existe una regla de descuento por Temporada en estado "activa"  
**When** el usuario hace click en el botón para cambiarlo a "Inactivo"  
**Then** el sistema debe actualizar el estado de la regla inmediatamente  
**And** el motor de cálculo debe ignorar esta regla en todas las peticiones S2S recibidas a partir de ese momento

#### Scenario: Reactivación de regla sin pérdida de datos
**Given** que una regla de fidelidad oro fue desactivada previamente  
**When** el usuario activa nuevamente  
**Then** la regla debe volver a participar en el cálculo del motor con sus parámetros originales sin necesidad de reconfigurarla

### Notas Técnicas (Constraints)

- **Stack:** Java 21 + Spring Boot 3.x.
- **Auth:** JWT para gestión de reglas.
- **Almacenamiento:** PostgreSQL con campo `status` (ACTIVE/INACTIVE) en tablas de reglas.
- **Performance:** Cambio de estado debe reflejarse inmediatamente en caché del Engine Service.
- **Sync:** Publicación de evento de cambio de estado vía RabbitMQ al Engine Service.
- **Estados:** ACTIVE, INACTIVE.
- La activación/desactivación es una operación de cambio de estado, no elimina los datos.
- Las reglas inactivas no participan en el cálculo de descuentos.
- El historial de cambios debe registrar estas activaciones/desactivaciones (HU-11).
