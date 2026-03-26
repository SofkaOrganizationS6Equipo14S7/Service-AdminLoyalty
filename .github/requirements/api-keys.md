# HU-03: Gestión y Validación de API Keys

## Historia de Usuario

Como Super Admin, quiero gestionar y validar las API Keys de cada ecommerce, para asegurar que solo sistemas autorizados puedan acceder a sus recursos en la plataforma.

## Criterios de Aceptación

### Scenario: Crear una clave de acceso para un ecommerce válido
**Given** soy un Super Admin con acceso al sistema  
**And** existe un ecommerce registrado  
**When** creo una nueva clave de acceso para ese ecommerce  
**Then** el sistema genera la clave de acceso correctamente

### Scenario: Ver las claves de acceso de un ecommerce
**Given** soy un Super Administrador con acceso al sistema  
**And** existen claves de acceso registradas para un ecommerce  
**When** consulto las claves de acceso de ese ecommerce  
**Then** el sistema muestra la lista de claves de acceso asociadas  
**And** oculta parte de la información de cada clave para proteger su seguridad

### Scenario: Validar API Key en Engine Service
**Given** soy un sistema con una API Key válida  
**When** envío una request al Engine Service  
**Then** el sistema valida la API Key contra la caché local  
**And** permite el acceso si es válida

### Scenario: Rechazar API Key inválida
**Given** soy un sistema con una API Key inválida  
**When** envío una request al Engine Service  
**Then** el sistema responde con HTTP 401 Unauthorized

## Notas Técnicas (Constraints)

- **Sync:** API Keys se sincronizan de Admin Service a Engine Service vía RabbitMQ.
- **Formato:** UUID v4.
- **Masking:** mostrar solo `****XXXX` (últimos 4 caracteres).
- **Caché:** Engine Service mantiene caché local de API Keys válidas.
- **Stack:** Java 21 + Spring Boot 3.x.
