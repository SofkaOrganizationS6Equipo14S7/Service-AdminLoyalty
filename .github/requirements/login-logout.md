# HU-01: Inicio y cierre de sesión
## Historia de Usuario
Como usuario de LOYALTY, quiero iniciar sesión y cerrar sesión, para acceder de forma segura al dashboard.

## Criterios de Aceptación
**Scenario: Inicio de sesión exitoso**
- Given que existe un usuario registrado en el LOYALTY
- When intenta ingresar con credenciales válidas
- Then el sistema le otorga el acceso
- And debe redirigir el usuario al Dashboard correspondiente según el rol con todas las funciones pertinentes

**Scenario: Intento de inicio de sesión con credenciales erróneas**
- Given que existe un usuario registrado en el LOYALTY
- When intenta ingresar con credenciales inválidas
- Then el sistema debe rechazar el ingreso de sesión
- And muestra un mensaje de credenciales inválidas

**Scenario: Validación de campos obligatorios**
- Given que existe un usuario registrado en el LOYALTY
- When intenta ingresar olvidando llenar los campos obligatorios
- Then el sistema debe negar la solicitud
- And muestra un mensaje de faltan campos obligatorios