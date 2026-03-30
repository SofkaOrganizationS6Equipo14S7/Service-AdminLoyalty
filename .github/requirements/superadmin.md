# HU-02: Acceso Total

## Historia de Usuario

**Como** SUPERADMIN
**Quiero** tener acceso total a la plataforma  
**Para** gestionar todos los ecommerce registrados, crear/modificar/eliminar admins de cada marca y configurar el sistema global sin estar vinculado a un ecommerce específico.

## Criterios de Aceptación

### Scenario: SUPERADMIN crea STORE_ADMIN para un ecommerce
**Given** existe un usuario con rol SUPERADMIN  
**And** un ecommerce contrató los servicios de LOYALTY  
**When** el SUPERADMIN crea un STORE_ADMIN para ese ecommerce  
**Then** el STORE_ADMIN queda vinculado al ecommerce  
**And** el STORE_ADMIN puede gestionar usuarios estándar de su ecommerce

### Scenario: Listar todos los ecommerce
**Given** soy un SUPERADMIN con acceso al sistema  
**When** consulto todos los ecommerce registrados  
**Then** el sistema muestra la lista de todos los ecommerce  
**And** puedo gestionar los STORE_ADMIN de cada ecommerce

### Scenario: Modificar STORE_ADMIN de un ecommerce
**Given** soy un SUPERADMIN  
**And** existe un STORE_ADMIN asociado a un ecommerce  
**When** modifico los datos del STORE_ADMIN   
**Then** el sistema actualiza la información del STORE_ADMIN  
**And** el STORE_ADMIN mantiene la vinculación con su ecommerce

### Scenario: Cambiar usuario de ecommerce
**Given** soy un SUPERADMIN  
**And** existe un usuario (STORE_ADMIN o STORE_USER) asociado a un ecommerce  
**When** cambio el ecommerce asociado al usuario  
**Then** el sistema actualiza la vinculación  
**And** el usuario ahora pertenece al nuevo ecommerce

### Scenario: Eliminar STORE_ADMIN de un ecommerce
**Given** soy un SUPERADMIN  
**And** existe un STORE_ADMIN asociado a un ecommerce  
**When** elimino el STORE_ADMIN  
**Then** el sistema elimina el perfil de forma permanente  
**And** el STORE_ADMIN ya no puede acceder al sistema

## Notas Técnicas (Constraints)

- **Acceso global:** SUPERADMIN no está vinculado a ningún ecommerce específico.
- **Aislamiento:** No aplica filtro por `ecommerce_id` para SUPERADMIN.
- **Auth:** JWT con claim `role: SUPERADMIN`.
- **Stack:** Java 21 + Spring Boot 3.x.
- **Validación:** SUPERADMIN puede gestionar todos los ecommerce sin restricciones.
