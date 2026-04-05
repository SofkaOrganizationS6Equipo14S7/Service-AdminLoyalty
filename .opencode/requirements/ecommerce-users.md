## HU-02: Gestión de Usuarios por Ecommerce

Como Admin, quiero crear usuarios vinculados a un ecommerce, para garantizar que cada uno gestione únicamente sus propias reglas de descuento sin afectar a otros ecommerce.

### Criterios de Aceptación

#### Scenario: Creación de usuario estándar por ADMIN
**Given** existe un usuario con rol ADMIN  
**And** un ecommerce contrató los servicios de LOYALTY  
**When** el ADMIN crea un usuario estándar asociado a ese ecommerce  
**Then** el usuario queda vinculado exclusivamente a dicho ecommerce

#### Scenario: Validar que el usuario estándar solo accede a su ecommerce
**Given** existe un usuario estándar asociado a un ecommerce  
**When** el usuario inicia sesión  
**Then** solo puede visualizar y gestionar información de su ecommerce  
**And** no puede acceder a datos de otros ecommerce

#### Scenario: Crear usuario estándar para ecommerce
**Given** soy un ADMIN con acceso al sistema  
**And** existe un ecommerce registrado  
**When** creo un nuevo usuario estándar asociado a ese ecommerce  
**Then** el sistema genera las credenciales del usuario  
**And** el usuario queda vinculado al ecommerce especificado

#### Scenario: Listar usuarios por ecommerce
**Given** soy un ADMIN con acceso al sistema  
**And** existen usuarios estándar asociados a un ecommerce  
**When** consulto los usuarios de ese ecommerce  
**Then** el sistema muestra la lista de usuarios vinculados  
**And** cada usuario muestra: username, rol, email, fecha de creación

#### Scenario: Actualizar usuario (cambio de ecommerce)
**Given** soy un ADMIN  
**And** existe un usuario estándar asociado a un ecommerce  
**When** intento cambiar el ecommerce asociado al usuario  
**Then** el sistema actualiza la vinculación  
**And** el usuario ahora pertenece al nuevo ecommerce

#### Scenario: Eliminar usuario estándar
**Given** soy un ADMIN  
**And** existe un usuario estándar asociado a un ecommerce  
**When** elimino el usuario  
**Then** el sistema elimina el perfil de forma permanente  
**And** el usuario ya no puede acceder al sistema

#### Scenario: SUPERADMIN crea ADMIN para un ecommerce
**Given** existe un usuario con rol SUPERADMIN  
**And** un ecommerce contrató los servicios de LOYALTY  
**When** el SUPERADMIN crea un ADMIN para ese ecommerce  
**Then** el ADMIN queda vinculado al ecommerce  
**And** el ADMIN puede gestionar usuarios estándar de su ecommerce

#### Scenario: Listar todos los ecommerce (SUPERADMIN)
**Given** soy un SUPERADMIN con acceso al sistema  
**When** consulto todos los ecommerce registrados  
**Then** el sistema muestra la lista de todos los ecommerce  
**And** puedo gestionar los ADMIN de cada ecommerce

### Notas Técnicas (Constraints)

- **Vinculación:** 
  - USUARIO ESTÁNDAR: Debe estar vinculado a exactamente un ecommerce.
  - ADMIN: Debe estar vinculado a exactamente un ecommerce.
  - SUPERADMIN: No está vinculado a ningún ecommerce específico (acceso global).
- **Aislamiento:** Consultas en BD filtran por `ecommerce_id` del contexto de seguridad.
- **Auth:** JWT con claim `ecommerce_id` y `role` para control de acceso.
- **Stack:** Java 21 + Spring Boot 3.x.
- **Validación:** No permitir crear usuarios sin asignar un ecommerce válido (excepto SUPERADMIN).
- **SUPERADMIN:** Acceso total, no tiene restricción de `ecommerce_id`, puede gestionar todos los ecommerce.
- **ADMIN:** Acceso completo a su ecommerce específico, puede crear/gestionar usuarios estándar de su ecommerce.
- **USUARIO ESTÁNDAR:** Acceso limitado según permisos asignados por el ADMIN de su ecommerce.
