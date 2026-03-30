# HU-02: Acceso Total

## Historia de Usuario

**Como** SUPER_ADMIN
**Quiero** tener acceso total a la plataforma  
**Para** gestionar todos los ecommerce registrados, crear/modificar/eliminar admins de cada marca y configurar el sistema global sin estar vinculado a un ecommerce específico.

## Criterios de Aceptación

### Scenario: SUPER_ADMIN crea STORE_ADMIN para un ecommerce
**Given** existe un usuario con rol SUPER_ADMIN  
**And** un ecommerce contrató los servicios de LOYALTY  
**When** el SUPER_ADMIN crea un STORE_ADMIN para ese ecommerce  
**Then** el STORE_ADMIN queda vinculado al ecommerce  
**And** el STORE_ADMIN puede gestionar usuarios estándar de su ecommerce

### Scenario: Listar todos los ecommerce
**Given** soy un SUPER_ADMIN con acceso al sistema  
**When** consulto todos los ecommerce registrados  
**Then** el sistema muestra la lista de todos los ecommerce  
**And** puedo gestionar los STORE_ADMIN de cada ecommerce

### Scenario: Modificar STORE_ADMIN de un ecommerce
**Given** soy un SUPER_ADMIN  
**And** existe un STORE_ADMIN asociado a un ecommerce  
**When** modifico los datos del STORE_ADMIN   
**Then** el sistema actualiza la información del STORE_ADMIN  
**And** el STORE_ADMIN mantiene la vinculación con su ecommerce

### Scenario: Cambiar usuario de ecommerce
**Given** soy un SUPER_ADMIN  
**And** existe un usuario (STORE_ADMIN o STORE_USER) asociado a un ecommerce  
**When** cambio el ecommerce asociado al usuario  
**Then** el sistema actualiza la vinculación  
**And** el usuario ahora pertenece al nuevo ecommerce

### Scenario: Eliminar STORE_ADMIN de un ecommerce
**Given** soy un SUPER_ADMIN  
**And** existe un STORE_ADMIN asociado a un ecommerce  
**When** elimino el STORE_ADMIN  
**Then** el sistema elimina el perfil de forma permanente  
**And** el STORE_ADMIN ya no puede acceder al sistema

## Notas Técnicas (Constraints)

- **Acceso global:** SUPER_ADMIN no está vinculado a ningún ecommerce específico.
- **Aislamiento:** No aplica filtro por `ecommerce_id` para SUPER_ADMIN.
- **Auth:** JWT con claim `role: SUPER_ADMIN`.
- **Stack:** Java 21 + Spring Boot 3.x.
- **Validación:** SUPER_ADMIN puede gestionar todos los ecommerce sin restricciones.
