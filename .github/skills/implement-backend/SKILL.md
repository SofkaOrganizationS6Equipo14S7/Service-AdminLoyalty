---
name: implement-backend
description: Implementa un feature completo en el backend Spring Boot. Requiere spec con status APPROVED en .github/specs/.
argument-hint: "<nombre-feature>"
---

# Implement Backend

## Prerequisitos
1. Leer spec: `.github/specs/<feature>.spec.md` — sección 2 (modelos, endpoints)
2. Leer stack y arquitectura: `.github/instructions/backend.instructions.md`
3. Leer referencia de patrones: `.github/skills/implement-backend/patterns.java`

## Arquitectura Clean Architecture

```
domain/           → entities, repository interfaces (sin dependencias externas)
application/      → services, DTOs (lógica de negocio, usa domain)
infrastructure/  → rabbitmq, cache, security, exceptions (implementaciones externas)
presentation/     → controllers, DTOs request/response (HTTP)
```

| Capa | Responsabilidad |
|------|-----------------|
| **domain/entity** | Entidades JPA (solo anotaciones de persistencia, sin lógica) |
| **domain/repository** | Interfaces JpaRepository (contratos, sin implementación) |
| **application/service** | Lógica de negocio (usa repositories, orchestación) |
| **application/dto** | DTOs input/output (Java Records) |
| **infrastructure/exception** | Custom exceptions + @RestControllerAdvice |
| **infrastructure/rabbitmq** | Productores/ consumidores de eventos |
| **presentation/controller** | Endpoints HTTP (recibe request, delega a service) |

## Flujo de dependencias

```
presentation → application → domain ← infrastructure
     ↑                                   
     └────────────── (implementa) ──────┘
```

- **Domain** no puede depender de ninguna otra capa
- **Application** depende solo de Domain
- **Infrastructure** implementa interfaces de Domain

## Dependency Injection (obligatorio)

Usar **Constructor Injection**. Spring inyecta automáticamente.

```java
@RestController
public class FeatureController {
    private final FeatureService service;

    public FeatureController(FeatureService service) {
        this.service = service;
    }
}
```

## Reglas
Ver `.github/instructions/backend.instructions.md` — DTOs como Records, Virtual Threads, BigDecimal, Flyway.

## Restricciones
- Solo directorio de backend del proyecto. No tocar frontend.
- No generar tests (responsabilidad de `test-engineer-backend`).