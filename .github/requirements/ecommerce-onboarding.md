# HU-13: Registro de Ecommerces (Onboarding)

## Historia de Usuario

Como Super Admin, quiero registrar y gestionar los ecommerces en la plataforma, para habilitar un entorno aislado (tenant) donde cada cliente pueda operar de forma segura.

## Criterios de Aceptación

### Scenario: Registro exitoso de un nuevo ecommerce
**Given** soy un Super Admin autenticado  
**When** registro un nuevo ecommerce con un nombre (ej: "Tienda Nike") y un identificador único o slug (ej: "nike-store")  
**Then** el sistema genera un UUID único para ese ecommerce  
**And** el estado inicial es "Activo"  
**And** el ecommerce ya aparece disponible para vincularle usuarios en la HU 2

### Scenario: Rechazo por identificador duplicado
**Given** ya existe un ecommerce con el slug "nike-store"  
**When** intento registrar otro con el mismo identificador  
**Then** el sistema rechaza el registro e informa que el nombre ya está en uso

### Scenario: Desactivación de un ecommerce (Baja del servicio)
**Given** un ecommerce está registrado y activo  
**When** el Super Admin cambia su estado a "Inactivo"  
**Then** todos los Usuarios (HU 2) vinculados a ese ecommerce pierden el acceso al dashboard de inmediato  
**And** las API Keys (HU 3) asociadas dejan de validar transacciones en el motor

## Notas Técnicas (Constraints)

- **Identificador único:** El campo `slug` debe ser único a nivel de sistema.
- **UUID:** Cada ecommerce tiene un UUIDv4 generado automáticamente.
- **Estado inicial:** Al crear un ecommerce, el estado por defecto es "Activo".
- **Estados posibles:** "Activo", "Inactivo".
- **Aislamiento:** Cada ecommerce funciona como tenant independiente.
- **Cascada:** Al desactivar un ecommerce, se debe invalidar tokens de usuarios y revocar acceso a API Keys.
- **Stack:** Java 21 + Spring Boot 3.x.
- **Referencia HU 2:** Ver `ecommerce-users.md` para gestión de usuarios por ecommerce.
- **Referencia HU 3:** Ver `api-keys.md` para gestión de API Keys.
