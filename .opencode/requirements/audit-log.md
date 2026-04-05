## HU-13: Trazabilidad de Cambios de Reglas (Auditoría)

Como super admin, quiero ver el registro de los cambios de las reglas de descuento de todos los ecommerce, para identificar que usuarios realizaron modificaciones y en que momentos.

### Criterios de Aceptación

#### Scenario: Trazabilidad de modificaciones por el Super Admin
**Given** que soy un Super Admin y accedo al panel global de auditoría  
**When** consulto el historial de cambios de un ecommerce específico  
**Then** el sistema debe mostrar una tabla cronológica con: usuario que realizó el cambio, fecha/hora, regla afectada, valor anterior y valor nuevo  
**And** debe permitir filtrar por tipo de regla Temporada, Producto o Fidelidad

### Notas Técnicas (Constraints)

- **Stack:** Java 21 + Spring Boot 3.x.
- **Auth:** JWT con rol SUPER_ADMIN para acceso global.
- **Almacenamiento:** PostgreSQL con tablas para `audit_logs`.
- **Events:** Cada modificación de regla genera un evento de auditoría.
- **Retención:** Logs de auditoría se mantienen por tiempo indefinido (o configurable).
- **Sync:** Los logs se escriben de forma síncrona en Admin Service.
- **Estructura del log:** id, timestamp, user_id, ecommerce_id, rule_type, rule_id, action (CREATE/UPDATE/DELETE), old_value, new_value, ip_address.
- **Filtros:** Por ecommerce, por tipo de regla, por usuario, por rango de fechas.
- **UI:** Panel de auditoría con tabla filtrable y exportable.
