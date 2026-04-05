## HU-03: Administra su Ecommerce

**Como** STORE_ADMIN
**Quiero** ser administrador de mi ecommerce  
**Para** crear, modificar y eliminar usuarios estándar de mi ecommerce, gestionar reglas de descuento, productos y clientes, y acceder exclusivamente a la información y métricas de mi ecommerce sin poder ver datos de otros ecommerce.

### Criterios de Aceptación

#### Scenario: Crear usuario estándar por STORE_ADMIN
**Given** existe un usuario con rol STORE_ADMIN  
**When** el STORE_ADMIN crea un usuario estándar asociado a ese ecommerce  
**Then** el usuario queda vinculado exclusivamente a dicho ecommerce

#### Scenario: Validar que el usuario estándar solo accede a su ecommerce
**Given** existe un usuario estándar asociado a un ecommerce  
**When** el usuario inicia sesión  
**Then** solo puede visualizar y gestionar información de su ecommerce  
**And** no puede acceder a datos de otros ecommerce

#### Scenario: Crear usuario estándar para ecommerce
**Given** soy un STORE_ADMIN con acceso al sistema  
**And** existe un ecommerce registrado  
**When** creo un nuevo usuario estándar asociado a ese ecommerce  
**Then** el sistema genera las credenciales del usuario  
**And** el usuario queda vinculado al ecommerce especificado

#### Scenario: Listar usuarios por ecommerce
**Given** soy un STORE_ADMIN con acceso al sistema  
**And** existen usuarios estándar asociados a un ecommerce  
**When** consulto los usuarios de ese ecommerce  
**Then** el sistema muestra la lista de usuarios vinculados  
**And** cada usuario muestra: username, rol, email, fecha de creación

#### Scenario: Actualizar datos de perfil de usuario estándar
**Given** soy un STORE_ADMIN  
**And** existe un usuario estándar asociado a mi ecommerce  
**When** actualizo los datos del usuario (nombre, email)  
**Then** el sistema actualiza la información del usuario  
**And** el usuario mantiene la vinculación con mi ecommerce

#### Scenario: Eliminar usuario estándar
**Given** soy un STORE_ADMIN  
**And** existe un usuario estándar asociado a un ecommerce  
**When** elimino el usuario  
**Then** el sistema elimina el perfil de forma permanente  
**And** el usuario ya no puede acceder al sistema

### Notas Técnicas (Constraints)

- **Vinculación:** STORE_ADMIN debe estar vinculado a exactamente un ecommerce.
- **Aislamiento:** Consultas en BD filtran por `ecommerce_id` del contexto de seguridad.
- **Auth:** JWT con claim `role: STORE_ADMIN` y `ecommerce_id`.
- **Stack:** Java 21 + Spring Boot 3.x.
- **Validación:** STORE_ADMIN solo puede crear/gestionar usuarios estándar de su propio ecommerce.
- **Permisos:** Puede gestionar reglas de descuento, productos y clientes de su ecommerce.
