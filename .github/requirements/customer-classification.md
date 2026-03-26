# HU-08: Clasificación Dinámica de Clientes (Loyalty Tiers)

## Historia de Usuario

Como Engine Service, quiero clasificar al cliente en un nivel de fidelidad (Bronce, Plata, Oro, Platino) según el payload recibido del e-commerce, para determinar qué reglas de descuento aplicarle.

## Criterios de Aceptación

### Scenario: Clasificación exitosa
**Given** existe una matriz de reglas en la caché de Caffeine (Engine)  
**And** el payload del e-commerce es válido (ej: total_spent, order_count)  
**When** el motor evalúa el payload contra la matriz  
**Then** el cliente queda asignado a un único nivel de fidelidad (Tier)

### Scenario: Rechazo por datos inválidos o incompletos
**When** el payload no tiene los atributos obligatorios o tiene valores negativos  
**Then** el sistema rechaza la clasificación y devuelve un error descriptivo  
**And** no se aplica ningún descuento de fidelidad

### Scenario: Sincronización de Matriz (Admin -> Engine)
**When** el Admin actualiza la matriz de clasificación en loyalty-admin  
**Then** se publica un evento en RabbitMQ  
**And** el loyalty-engine actualiza su caché local inmediatamente

### Scenario: Consistencia y Determinismo
**When** se evalúa el mismo payload dos veces bajo la misma configuración  
**Then** el resultado de la clasificación debe ser idéntico

## Notas Técnicas (Constraints)

- **Stack:** Java 21 (Records para el payload) + Spring Boot 3.x.
- **Auth:** Gestión de Matriz (Admin): JWT. Ejecución de Clasificación (Engine - /calculate): API Key.
- **Performance:** La clasificación debe ejecutarse en memoria (Caffeine) para no impactar la latencia del /calculate.
- **Sync:** Comunicación asíncrona vía RabbitMQ (Fanout).
- **No X-User-ID:** La identidad del e-commerce se extrae del SecurityContext tras validar la API Key.
- La matriz de clasificación debe tener al menos un nivel definido.
- Cada cliente debe tener exactamente un nivel de fidelidad asignado.
- Los atributos obligatorios deben estar documentados.
- El cache debe tener TTL configurado para evitar stale data.
- La clasificación debe ser determinística (mismo payload = mismo resultado).
