---
name: implement-backend
description: Implementa un feature completo en el backend Spring Boot. Requiere spec con status APPROVED en .github/specs/.
argument-hint: "<nombre-feature>"
---

# Implement Backend

## Prerequisitos
1. Leer spec: `.github/specs/<feature>.spec.md` — sección 2 (modelos, endpoints)
2. Leer stack y arquitectura: `.github/instructions/backend.instructions.md`

## Orden de implementación
```
entities → repositories → services → controllers → excepción
```

| Capa | Responsabilidad |
|------|-----------------|
| **Entities** | Mapping a tabla DB (JPA) |
| **DTOs** | Input/output (Java Records) |
| **Repositories** | Acceso a datos (`JpaRepository`) |
| **Services** | Lógica de negocio (`@Service`) |
| **Controllers** | Endpoints HTTP (`@RestController`) |
| **Excepciones** | Custom exceptions + `@RestControllerAdvice` |

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
