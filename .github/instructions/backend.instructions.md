---
applyTo: "**/*.java"
---

# Instrucciones para Backend (Java 21 / Spring Boot)

## Arquitectura Clean Architecture

```
domain/           → entities, repository interfaces (sin dependencias externas)
application/      → services, DTOs (lógica de negocio)
infrastructure/  → rabbitmq, cache, security, exceptions (implementaciones externas)
presentation/     → controllers, DTOs request/response (HTTP)
```

**Flujo de dependencias:**
```
presentation → application → domain ← infrastructure
     ↑                                   
     └────────────── (implementa) ──────┘
```

### Regla de dependencias
- **Domain**: no puede depender de ninguna otra capa
- **Application**: depende solo de Domain (interfaces de repository)
- **Infrastructure**: implementa las interfaces definidas en Domain

---

## Dependency Injection

**Constructor Injection Obligatorio.** NUNCA usar `@Autowired` en campos.

```java
// ✅ Correcto
@RestController
public class DiscountController {
    private final DiscountService discountService;

    public DiscountController(DiscountService discountService) {
        this.discountService = discountService;
    }
}
```

---

## RabbitMQ

- **Admin Service (Producer):** Emite eventos cuando cambia reglas o API Keys
- **Engine Service (Consumer):** Recibe eventos y actualiza caché (Caffeine)
- **Events:** Java Records en formato JSON

---

## Convenciones

| Concepto | Regla |
|----------|-------|
| **DTOs** | Java Records |
| **Virtual Threads** | `spring.threads.virtual.enabled=true` |
| **Moneda** | Siempre `BigDecimal` (nunca `double`/`float`) |
| **Migraciones** | Scripts `.sql` en `resources/db/migration` (Flyway) |
| **Validación** | `@Valid` + `jakarta.validation` en Request Bodies |

---

## NUNCA HACER

- Cálculos monetarios con `double`/`float`
- Commits sin ejecutar `mvn clean test`
- Hardcodear credenciales o nombres de colas (usar `application.yml`)

---

## Flujo de Commits y Code Review

**Después de completar cada funcionalidad (backend, frontend o test), se DEBE hacer commit.**

1. **No hacer múltiples funcionalidades en un solo commit** — cada feature/tarea en commit separado
2. **Formato de commit obligatorio**:
   ```
   tipo(alcance): descripción corta
   ```
   - `tipo`: `feat`, `fix`, `docs`, `test`, `refactor`, `chore`
   - `alcance`: módulo o funcionalidad affected
   - `descripción`: en presente, max 50 caracteres

   **Ejemplos:**
   - `feat(auth): add login endpoint with JWT token`
   - `fix(api-key): resolve validation error on create`
   - `docs(readme): update API documentation`
   - `test(auth): add login validation tests`

3. **Workflow obligatorio**:
   ```
   Implementar feature → Commit → Code Review del usuario → Approve → Continuar siguiente feature
   ```

4. **El usuario debe aprobar cada commit** antes de continuar con la siguiente funcionalidad.

---

> Para estándares de código, seguridad y observabilidad, ver `.github/docs/guidelines/dev-guidelines.md`
