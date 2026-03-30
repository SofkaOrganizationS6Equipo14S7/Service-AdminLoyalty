# HU-04: Usuario Estándar

## Historia de Usuario

**Como** STORE_USER
**Quiero** tener acceso limitado según los permisos que me asigne el STORE_ADMIN  
**Para** visualizar y gestionar únicamente la información de mi ecommerce, sin poder crear otros usuarios y estando vinculado a un ecommerce específico.

## Criterios de Aceptación

### Scenario: Iniciar sesión en el sistema
**Given** existe un usuario con rol STORE_USER  
**And** está vinculado a un ecommerce específico  
**When** el usuario inicia sesión  
**Then** el sistema valida las credenciales  
**And** redirige al dashboard de su ecommerce

### Scenario: Acceder solo a su ecommerce
**Given** soy un STORE_USER vinculado a un ecommerce  
**When** intento acceder a información de otro ecommerce  
**Then** el sistema deniega el acceso  
**And** muestra un mensaje de error

### Scenario: Visualizar información del ecommerce
**Given** soy un STORE_USER con acceso al sistema  
**When** consulto la información de mi ecommerce  
**Then** el sistema muestra los datos accesibles según mis permisos  
**And** no puedo modificar configuraciones del ecommerce

### Scenario: Actualizar mi perfil
**Given** soy un STORE_USER  
**When** actualizo mi información de perfil (nombre, email, contraseña)  
**Then** el sistema actualiza los datos  
**And** mi vinculación con el ecommerce se mantiene

## Notas Técnicas (Constraints)

- **Vinculación:** STORE_USER debe estar vinculado a exactamente un ecommerce.
- **Aislamiento:** Consultas en BD filtran por `ecommerce_id` del contexto de seguridad.
- **Auth:** JWT con claim `role: STORE_USER` y `ecommerce_id`.
- **Stack:** Java 21 + Spring Boot 3.x.
- **Validación:** No puede crear usuarios, solo acceso limitado según permisos asignados por STORE_ADMIN.
- **Permisos:** Solo puede visualizar y gestionar información según los permisos asignados.
