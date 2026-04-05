# Technical Architecture & System Design

> **Uso:** Entender la infraestructura y comunicación de servicios.

## Arquitectura

Microservicios desacoplados con comunicación asíncrona (Event-Driven).

## Componentes & Data Strategy

| Servicio | Puerto | Base de Datos | Responsabilidad Core |
|----------|--------|---------------|---------------------|
| Admin Service | 8081 | `loyalty_admin` | System of Record. CRUD de reglas y API Keys. |
| Engine Service | 8082 | `loyalty_engine` | High-Performance Engine. Valida Auth y calcula descuentos. |

**Migraciones:** Flyway para versionamiento de esquemas en ambos servicios.

## Flujo de Datos

1. Cambio en Admin Service → Publica en Fanout Exchange (`loyalty.config.exchange`)
2. Engine Service consume el evento → Actualiza caché + persiste en `loyalty_engine`

## Cold Start Problem

El Engine Service usa Caffeine Cache (In-memory). Al reiniciar, la caché queda vacía.

**Solución:**
- Cada evento recibido por RabbitMQ → persiste una copia en `loyalty_engine`
- Al arrancar (Startup) → carga reglas desde `loyalty_engine` a Caffeine

## Security Strategy (S2S)

El Engine Service es el responsable de la seguridad perimetral:

- **API Key Validation:** El Engine intercepta cada request en un `OncePerRequestFilter`
- **Auth Cache:** Las API Keys se sincronizan vía RabbitMQ desde Admin para validar en microsegundos sin consultar DB

## Observabilidad

Ambos servicios exponen `/actuator/health` para que Docker Compose sepa cuándo están listos.

## Despliegue Local

Docker Compose: Postgres (5432), RabbitMQ (5672/15672), Admin (8081), Engine (8082).
