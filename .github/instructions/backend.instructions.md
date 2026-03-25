---
applyTo: "**/*.java"
---

# Instrucciones para Backend (Java 21 / Spring Boot)

## Arquitectura en Capas

```
controller/   → HTTP (recibe request, delega al service)
service/      → Lógica de negocio
repository/   → Acceso a datos (JPA, Flyway)
```

**Flujo:** `Controller → Service → Repository`

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

> Para estándares de código, seguridad y observabilidad, ver `.github/docs/guidelines/dev-guidelines.md`
