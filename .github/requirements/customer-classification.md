# Requerimiento: Clasificación de Cliente para Descuentos

## Descripción General

Como motor de descuentos, quiero clasificar al cliente según el payload recibido, para asignar su nivel de descuento.

## Problema / Necesidad

El sistema LOYALTY necesita un motor de clasificación que evalúe las características del cliente y determine su nivel de fidelidad para aplicar el descuento correspondiente. Sin esta clasificación, no es posible ofrecer descuentos personalizados basados en el comportamiento o perfil del cliente.

## Solución Propuesta

### 1. Matriz de Clasificación

Definir una matriz de clasificación vigente que contenga:
- Atributos del cliente a evaluar.
- Reglas de validación por atributo.
- Rangos o valores que determinan cada nivel de fidelidad.
- Niveles de fidelidad disponibles (ej: Bronce, Plata, Oro, Platino).

### 2. Motor de Clasificación

Componente del sistema de descuentos que:
- Recibe el payload del cliente.
- Valida que contenga todos los atributos requeridos.
- Verifica que los valores estén dentro del dominio válido.
- Evalúa contra la matriz de clasificación vigente.
- Asigna un único nivel de fidelidad.

### 3. Cache de Clasificación

Para garantizar consistencia:
- Almacenar resultados de clasificación con hash del payload.
- Retornar resultado cacheado si el mismo payload se evalúa nuevamente.

## Contexto Técnico

- **Backend:** Node.js/Express service en `backend/service-admin`.
- **Base de datos:** PostgreSQL con tablas para `classification_matrix`, `customer_classification`.
- **Endpoints de API:**
  - `POST /api/discounts/classify` - Clasificar cliente según payload
  - `GET /api/discounts/classification-matrix` - Obtener matriz de clasificación vigente
  - `PUT /api/discounts/classification-matrix` - Actualizar matriz de clasificación (admin)
- **Cache:** Redis o en memoria para resultados de clasificación.
- **Auth:** Validación de token vía middleware.

## Criterios de Aceptación

### Scenario: Clasificación exitosa con payload completo y válido
**Given** existe una matriz de clasificación vigente  
**And** el payload contiene todos los atributos requeridos  
**When** el motor evalúa el payload del cliente  
**Then** el cliente queda asignado a un único nivel de fidelidad  
**And** el nivel asignado corresponde a las reglas de clasificación vigentes

### Scenario: Rechazo por payload incompleto
**Given** existen atributos obligatorios para clasificar al cliente  
**When** se recibe un payload sin uno o más atributos obligatorios  
**Then** la clasificación es rechazada  
**And** se informa que no es posible determinar el nivel de descuento

### Scenario: Rechazo por valores fuera de dominio
**Given** existen reglas de validación para los atributos del payload  
**When** se recibe un payload con valores inválidos para el dominio definido  
**Then** la clasificación es rechazada  
**And** no se asigna nivel de descuento al cliente

### Scenario: Consistencia de clasificación para mismo payload
**Given** existe una configuración de clasificación vigente  
**When** el motor evalúa dos veces el mismo payload  
**Then** el nivel de fidelidad asignado es el mismo en ambas evaluaciones

## Restricciones

- Cada cliente debe tener exactamente un nivel de fidelidad asignado.
- La matriz de clasificación debe tener al menos un nivel definido.
- Los atributos obligatorios deben estar documentados.
- El cache debe tener TTL configurado para evitar stale data.
- La clasificación debe ser determinística (mismo payload = mismo resultado).

## Prioridad

Alta — Fundamental para el sistema de descuentos personalizados de LOYALTY.
