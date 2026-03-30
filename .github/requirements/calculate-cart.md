# HU-11: Cálculo de Carrito con Descuentos Aplicados

## Historia de Usuario

Como ecommerce, quiero enviar el carrito y recibir el precio final con descuentos aplicados, para mostrarle al usuario final en el checkout.

## Criterios de Aceptación

### Scenario: Cálculo exitoso de precio final
**Given** el ecommerce está autorizado para consumir el servicio  
**And** el carrito contiene ítems válidos con cantidades y precios válidos  
**And** existen reglas de descuento vigentes aplicables  
**And** existe una configuración vigente de topes y prioridad de descuentos  
**And** el carrito califica para múltiples descuentos  
**When** el ecommerce solicita el cálculo del carrito  
**Then** el servicio responde el precio final del carrito  
**And** el total incluye los descuentos aplicados según reglas vigentes y prioridades configuradas  
**And** el orden de aplicación de descuentos respeta la prioridad configurada  
**And** el descuento total no supera el tope máximo vigente  
**And** el precio final refleja ambas restricciones de negocio

### Scenario: Carrito sin descuentos aplicables
**Given** el ecommerce está autorizado para consumir el servicio  
**And** el carrito es válido  
**And** no existen descuentos aplicables para ese carrito  
**When** el ecommerce solicita el cálculo del carrito  
**Then** el precio final es igual al subtotal del carrito  
**And** la respuesta indica que no hubo descuentos aplicados

### Scenario: Rechazo por carrito inválido
**Given** el servicio requiere estructura mínima y datos válidos de carrito  
**When** el ecommerce envía un carrito con datos incompletos o inconsistentes  
**Then** la solicitud es rechazada  
**And** no se retorna precio final hasta contar con un carrito válido

## Notas Técnicas (Constraints)

- **Stack:** Java 21 + Spring Boot 3.x.
- **Auth:** API Key en header para consumo del Engine Service.
- **Endpoint:** POST /api/v1/engine/calculate
- **Input:** Carrito con items (product_id, quantity, unit_price), cliente payload (para clasificación).
- **Almacenamiento:** No almacenar el carrito, solo logs de la transacción.
- **Performance:** El cálculo debe ejecutarse en memoria (Caffeine cache) para mantener latencia baja.
- **Sync:** Las reglas se sincronizan desde Admin Service vía RabbitMQ.
- El descuento total se limita al tope máximo configurado.
- La prioridad determina el orden de aplicación de descuentos.
- La respuesta debe incluir: subtotal, descuento aplicado, precio final, detalle de reglas aplicadas.
