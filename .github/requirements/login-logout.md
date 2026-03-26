# HU-01: Inicio y cierre de sesión (Auth Management)

## Historia de Usuario

Como Administrador, quiero iniciar y cerrar sesión en el Dashboard, para gestionar las reglas de negocio de forma segura mediante un token de identidad.

## Criterios de Aceptación

### Scenario: Inicio de sesión exitoso
**Given** usuario registrado con credenciales válidas en loyalty-admin  
**When** envía POST /api/v1/auth/login  
**Then** el sistema retorna un JWT válido con los claims de userId y roles  
**And** el status code es 200 OK

### Scenario: Intento con credenciales erróneas o campos vacíos
**When** envía credenciales inválidas o falta el username/password  
**Then** el sistema retorna 401 Unauthorized  
**And** el mensaje indica "Invalid credentials" o "Missing fields"

### Scenario: Cierre de sesión (Logout)
**Given** usuario con JWT activo  
**When** envía POST /api/v1/auth/logout  
**Then** el sistema invalida el token (vía blacklist en caché o simplemente borrando el token en el cliente)  
**And** el acceso a endpoints protegidos queda denegado

## Notas Técnicas (Constraints)

- **Service:** Implementar exclusivamente en loyalty-admin (Puerto 8081).
- **Security:** Contraseñas hasheadas con BCrypt.
- **JWT:** Generación usando la clave secreta compartida (Symmetric).
- **Stateless:** No usar HttpSession. Todo se maneja vía el Header Authorization: Bearer {token}.
- **Output:** El login debe retornar un JSON con el token y la fecha de expiración.
