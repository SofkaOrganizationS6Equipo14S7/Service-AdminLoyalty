# Requerimiento: Límite y Prioridad de Descuentos

## Descripción General

Como usuario de LOYALTY, quiero definir el tope máximo y la prioridad de descuentos, para proteger la rentabilidad del negocio.

## Problema / Necesidad

El negocio necesita establecer mecanismos de control para evitar una acumulación excesiva de descuentos que pueda afectar negativamente la rentabilidad. Sin límites y reglas de prioridad definidas, los descuentos concurrentes podrían aplicarse de manera impredecible, erosionando los márgenes de ganancia.

## Solución Propuesta

### 1. Configuración de Tope Máximo

Un panel de configuración en el admin dashboard permite establecer un límite máximo de descuento por transacción:
- Campo de entrada para valor numérico positivo (moneda).
- Validación para rechazar valores ≤ 0.
- Configuración persistida para el ecommerce.

### 2. Configuración de Prioridad de Descuentos

Definir una jerarquía ordenada para la aplicación de descuentos cuando hay múltiples descuentos simultáneos:
- Interfaz de ordenamiento numérico.
- Cada tipo de descuento recibe un nivel de prioridad único.
- No se permiten prioridades duplicadas.

### 3. Motor de Resolución de Descuentos

Cuando una transacción califica para múltiples descuentos:
1. Calcular todos los descuentos aplicables.
2. Aplicarlos en orden de prioridad (mayor prioridad primero).
3. Verificar el total acumulado contra el límite máximo.
4. Aplicar el límite máximo al descuento final.

## Contexto Técnico

- **Backend:** Node.js/Express service en `backend/service-admin`.
- **Base de datos:** PostgreSQL con tablas para `discount_config` (max_limit, priority_rules).
- **Endpoints de API:**
  - `POST /api/discounts/config` - Establecer límite máximo y prioridad
  - `GET /api/discounts/config` - Obtener configuración actual
  - `POST /api/discounts/calculate` - Calcular descuentos aplicables para una transacción
- **Auth:** Validación de token vía middleware.
- **Concurrencia:** El motor debe manejar solicitudes concurrentes de forma segura.

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

## Restricciones

- El tope máximo debe ser un número positivo mayor a cero.
- Cada tipo de descuento debe tener un valor de prioridad único.
- La resolución de descuentos debe ser atómica para evitar condiciones de carrera.
- Los cambios de configuración deben ser registrados en auditoría.
- Se debe definir una prioridad por defecto si no existe configuración.

## Prioridad

Alta — Crítico para el control de rentabilidad del negocio en el sistema LOYALTY.
