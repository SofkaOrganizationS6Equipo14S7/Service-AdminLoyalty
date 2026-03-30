# HU-12: Historial de Descuentos Aplicados

## Historia de Usuario

Como usuario de LOYALTY, quiero consultar los descuentos aplicados en las transacciones de los últimos siete días, para verificar que el motor de cálculo esté operando bajo las reglas y topes máximos configurados.

## Criterios de Aceptación

### Scenario: Verificación exitosa de transacciones recientes
**Given** soy un usuario de LOYALTY con acceso al dashboard de mi ecommerce  
**When** accedo al módulo de "Historial de Descuentos" y filtro por los últimos 7 días  
**Then** el sistema debe mostrar una lista con: id de transacción, fecha/hora, reglas aplicadas, descuento calculado y descuento final  
**And** debe resaltar aquellas transacciones donde el descuento fue rechazado por alcanzar el tope máximo configurado

### Scenario: Cumplimiento de privacidad de datos
**Given** que el sistema registra el uso del motor de descuentos  
**When** se visualiza el detalle de una transacción en el log  
**Then** el sistema no debe mostrar nombres, correos, ni datos de identidad del comprador final  
**And** solo se debe mostrar el payload técnico que justifica el descuento

## Notas Técnicas (Constraints)

- **Stack:** Java 21 + Spring Boot 3.x.
- **Auth:** JWT para acceso al dashboard.
- **Almacenamiento:** PostgreSQL con tablas para `transaction_logs`.
- **Retención:** Los logs se mantienen por 7 días (configurable).
- **Performance:** Consultas con paginación y filtros por fecha.
- **Privacidad:** No almacenar PII (Personally Identifiable Information) del cliente final.
- **Logs:** Incluyen: transaction_id, ecommerce_id, timestamp, payload_recibido, reglas_aplicadas, descuento_calculado, descuento_final, descuento_rechazado_por_tope.
- **UI:** Dashboard con tabla filtrable por rango de fechas.
- Las transacciones donde el descuento fue truncado por el tope deben marcarse visualmente.
