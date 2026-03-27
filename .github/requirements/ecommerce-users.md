# HU-02: Gestión de Usuarios por Ecommerce

## Historia de Usuario

Como Super Admin, quiero crear usuarios vinculados a un ecommerce, para garantizar que cada uno gestione únicamente sus propias reglas de descuento sin afectar a otros ecommerce.

## Criterios de Aceptación

### Scenario: Creación de perfil asociado a un ecommerce específico
**Given** existe un usuario con rol super admin  
**And** un ecommerce contrató los servicios de LOYALTY  
**When** el Super Admin crea un perfil de usuario asociado a ese ecommerce  
**Then** el perfil queda vinculado exclusivamente a dicho ecommerce

### Scenario: Validar que el usuario solo accede a su ecommerce
**Given** existe un usuario asociado a un ecommerce  
**When** el usuario inicia sesión  
**Then** solo puede visualizar y gestionar información de su ecommerce  
**And** no puede acceder a datos de otros ecommerce

### Scenario: Crear usuario admin para ecommerce
**Given** soy un Super Admin con acceso al sistema  
**And** existe un ecommerce registrado  
**When** creo un nuevo usuario asociado a ese ecommerce  
**Then** el sistema genera las credenciales del usuario  
**And** el usuario queda vinculado al ecommerce especificado

### Scenario: Listar usuarios por ecommerce
**Given** soy un Super Admin con acceso al sistema  
**And** existen usuarios asociados a un ecommerce  
**When** consulto los usuarios de ese ecommerce  
**Then** el sistema muestra la lista de usuarios vinculados  
**And** cada usuario muestra: username, rol, email, fecha de creación

### Scenario: Actualizar usuario (cambio de ecommerce)
**Given** soy un Super Admin  
**And** existe un usuario asociado a un ecommerce  
**When** intento cambiar el ecommerce asociado al usuario  
**Then** el sistema actualiza la vinculación  
**And** el usuario ahora pertenece al nuevo ecommerce

### Scenario: Eliminar usuario
**Given** soy un Super Admin  
**And** existe un usuario asociado a un ecommerce  
**When** elimino el usuario  
**Then** el sistema elimina el perfil de forma permanente  
**And** el usuario ya no puede acceder al sistema

## Notas Técnicas (Constraints)

- **Vinculación:** Cada usuario debe estar vinculado a exactamente un ecommerce.
- **Aislamiento:** Consultas en BD filtran por `ecommerce_id` del contexto de seguridad.
- **Auth:** JWT con claim `ecommerce_id` para usuarios no-super-admin.
- **Stack:** Java 21 + Spring Boot 3.x.
- **Validación:** No permitir crear usuarios sin asignar un ecommerce válido.
- **Super Admin:** Tiene acceso a todos los ecommerce, no tiene restricción de `ecommerce_id`.
