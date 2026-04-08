---
id: SPEC-0006
status: IN_PROGRESS
feature: rule-validations-date-discount
created: 2026-04-07
updated: 2026-04-07
author: spec-generator
version: "1.0"
related-specs:
  - HU-06 (seasonal-rules)
  - HU-07 (product-rules)
  - HU-09 (discount-limits)
  - SPEC-011 (cart-calculation)
---

# Spec: Rule Validations — Superposición de Fechas + Límites de Descuento (HU-06 + HU-07)

> **Estado:** `IN_PROGRESS` — Backend implementado. Tests y Frontend pendientes.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED

> **Contexto:** Esta especificación implementa 2 validaciones críticas para el sistema de reglas:
> 1. **Superposición de Fechas (HU-06)**: Evita que existan reglas SEASONAL con fechas solapadas
> 2. **Límites de Descuento (HU-06/HU-07)**: Valida que los descuentos no excedan los límites configurados globalmente
>
> **Arquitectura:** Validación en **Application Layer** (RuleService) con datos extraídos de atributos dinámicos (`rule_attribute_values`).
> **Stack:** Java 21 + Spring Boot 3.x + PostgreSQL + Flyway migrations
> **Impacto BD:** MÍNIMO — Solo 1 nueva migration con 2 INSERT statements + 1 índice

---

## 1. REQUERIMIENTOS

### Descripción

Implementar dos validaciones de negocio en el servicio de creación/actualización de reglas:

1. **Validación de Superposición de Fechas (HU-06)**: Cuando se crea o actualiza una regla de tipo SEASONAL, validar que sus fechas (`start_date` y `end_date`) no se superpongan con otras reglas SEASONAL activas del mismo ecommerce.

2. **Validación de Límites de Descuento (HU-06/HU-07)**: Cuando se crea o actualiza cualquier regla (SEASONAL, PRODUCT, CLASSIFICATION), validar que su `discountPercentage` no exceda el `max_discount_cap` configurado en `discount_settings` para ese ecommerce.

### Requerimiento de Negocio

Como administrador del sistema Loyalty, necesito que:
- Las reglas de temporada NO se superpongan en fechas (evitar conflictos de aplicación)
- Los descuentos respeten los límites configurados globales (proteger rentabilidad)
- Las validaciones se hagan en tiempo de creación/edición (fail-fast)
- Los errores sean claros y específicos para el usuario

### Historias de Usuario Relacionadas

#### HU-06: Gestión de Reglas de Temporada (SEASONAL RULE VALIDATIONS)

```
Como:        Admin del ecommerce
Quiero:      Crear/editar reglas de temporada sin conflictos de fechas
Para:        Asegurar que solo una regla se aplica en cada período

Prioridad:   Alta
Estimación:  M
Dependencias: Schema de rule_attributes debe tener start_date, end_date
Capa:        Backend (Admin Service)
```

#### HU-07: Gestión de Reglas por Producto (PRODUCT RULE VALIDATIONS)

```
Como:        Admin del ecommerce
Quiero:      Crear/editar reglas de producto respetando límites de descuento
Para:        No superar los topes globales de rentabilidad

Prioridad:   Alta
Estimación:  M
Dependencias: DiscountSettingsEntity debe existir
Capa:        Backend (Admin Service)
```

---

## 2. CRITERIOS DE ACEPTACIÓN

### VALIDACIÓN 1: Superposición de Fechas SEASONAL

#### Scenario 1.1: Creación exitosa sin overlap

```gherkin
Dado que:      existen las siguientes reglas SEASONAL activas:
               | rule_id | start_date | end_date   |
               | A       | 2026-01-01 | 2026-01-31 |
               | B       | 2026-03-01 | 2026-03-31 |
Y:             no hay reglas SEASONAL en febrero
Cuando:        se intenta crear nueva regla SEASONAL:
               { start_date: "2026-02-01", end_date: "2026-02-28" }
Entonces:      la regla se crea exitosamente
Y:             la respuesta incluye: { "id": "uuid", "status": "CREATED" }
```

#### Scenario 1.2: Rechazo por superposición total

```gherkin
Dado que:      existe regla SEASONAL activa:
               { id: "A", start_date: "2026-01-15", end_date: "2026-01-31" }
Cuando:        se intenta crear regla con range idéntico:
               { start_date: "2026-01-15", end_date: "2026-01-31" }
Entonces:      la creación se RECHAZA con status 409 (Conflict)
Y:             la respuesta contiene: 
               { "error": "SEASONAL rule date overlap detected. 
                           Existing rule (A) covers 2026-01-15 to 2026-01-31" }
```

#### Scenario 1.3: Rechazo por superposición parcial (inicio)

```gherkin
Dado que:      existe regla activa:
               { start_date: "2026-01-20", end_date: "2026-01-31" }
Cuando:        se intenta crear:
               { start_date: "2026-01-10", end_date: "2026-01-25" }
Entonces:      la creación se RECHAZA (solapan desde 2026-01-20 a 2026-01-25)
```

#### Scenario 1.4: Rechazo por fecha de inicio antes de fin

```gherkin
Dado que:      se intenta crear regla SEASONAL
Cuando:        start_date es posterior a end_date:
               { start_date: "2026-02-15", end_date: "2026-02-10" }
Entonces:      la creación se RECHAZA con status 400 (Bad Request)
Y:             la respuesta contiene:
               { "error": "start_date must be before or equal to end_date" }
```

#### Scenario 1.5: Actualización sin overlap con rule actual

```gherkin
Dado que:      existe regla SEASONAL con ID "A":
               { start_date: "2026-01-01", end_date: "2026-01-31" }
Y:             existe otra regla "B":
               { start_date: "2026-03-01", end_date: "2026-03-31" }
Cuando:        se actualiza regla "A" con:
               { start_date: "2026-02-01", end_date: "2026-02-28" }
Entonces:      la actualización es exitosa
Y:             la regla "A" no genera overlap
Y:             la validación EXCLUYE a la regla actual del check
```

#### Scenario 1.6: Validación solo aplica a SEASONAL

```gherkin
Dado que:      existe regla SEASONAL:
               { start_date: "2026-01-01", end_date: "2026-01-31" }
Cuando:        se crea regla PRODUCT (diferente tipo):
               { type: "PRODUCT", product_type: "electronics" }
Entonces:      NO se valida superposición de fechas (N/A)
Y:             la regla PRODUCT se crea exitosamente
```

---

### VALIDACIÓN 2: Límites de Descuento

#### Scenario 2.1: Creación exitosa dentro de límite

```gherkin
Dado que:      DiscountSettingsEntity para ecommerce "shop1":
               { max_discount_cap: 50.00 }
Cuando:        se crea regla con:
               { discountPercentage: 35.00 }
Entonces:      la regla se crea exitosamente
Y:             no se genera validación error
```

#### Scenario 2.2: Rechazo por excedera límite máximo

```gherkin
Dado que:      DiscountSettingsEntity:
               { max_discount_cap: 50.00 }
Cuando:        se intenta crear regla con:
               { discountPercentage: 75.00 }
Entonces:      la creación se RECHAZA con status 400
Y:             la respuesta contiene:
               { "error": "Discount percentage 75.00% exceeds maximum allowed: 50.00%" }
```

#### Scenario 2.3: Rechazo en actualización por exceso

```gherkin
Dado que:      existe regla con discountPercentage: 40.00
Y:             max_discount_cap: 50.00
Cuando:        se intenta actualizar a discountPercentage: 60.00
Entonces:      la actualización se RECHAZA
Y:             se mantiene la versión previa válida (40.00)
```

#### Scenario 2.4: Validación aplica a todos los tipos

```gherkin
Dado que:      max_discount_cap: 50.00
Cuando:        se crean reglas de tipo:
               - SEASONAL con discountPercentage: 45.00
               - PRODUCT con discountPercentage: 48.00
               - CLASSIFICATION con discountPercentage: 50.00
Entonces:      TODAS pasan validación
Y:             si cualquiera fuera > 50.00, sería rechazada
```

#### Scenario 2.5: Validación contra discount_settings del ecommerce correcto

```gherkin
Dado que:      DiscountSettings para ecommerce_id="shop1": max_discount_cap: 40.00
Y:             DiscountSettings para ecommerce_id="shop2": max_discount_cap: 60.00
Cuando:        se crea regla en "shop1" con discountPercentage: 50.00
Entonces:      se RECHAZA (contra 40.00 de shop1)
Y:             si se creara en "shop2", sería aceptada (50.00 <= 60.00)
```

#### Scenario 2.6: Error si discount_settings no existe

```gherkin
Dado que:      ecommerce "shop3" NO tiene DiscountSettingsEntity
Cuando:        se intenta crear regla
Entonces:      se RECHAZA con status 404
Y:             la respuesta contiene:
               { "error": "Discount settings not found for ecommerce: uuid" }
```

---

## 3. NOTAS TÉCNICAS

### Arquitectura: Opción A (Híbrido + Validación en Service)

**Principio:** Guardar atributos dinámicos (`start_date`, `end_date`) en `rule_attribute_values` + validar en RuleService (Application Layer).

### Cambios en Base de Datos

#### Migration V4: Agregar metadatos de atributos SEASONAL

**Archivo:** `backend/service-admin/src/main/resources/db/migration/V4__Add_seasonal_rule_attributes.sql`

```sql
-- Agregar atributos REQUIRED para SEASONAL
INSERT INTO rule_attributes (discount_type_id, attribute_name, attribute_type, is_required, description)
SELECT dt.id, 'start_date', 'DATE', TRUE, 'Season rule start date (YYYY-MM-DD format)'
FROM discount_types dt WHERE dt.code = 'SEASONAL'
AND NOT EXISTS (SELECT 1 FROM rule_attributes WHERE discount_type_id = dt.id AND attribute_name = 'start_date')
ON CONFLICT DO NOTHING;

INSERT INTO rule_attributes (discount_type_id, attribute_name, attribute_type, is_required, description)
SELECT dt.id, 'end_date', 'DATE', TRUE, 'Season rule end date (YYYY-MM-DD format)'
FROM discount_types dt WHERE dt.code = 'SEASONAL'
AND NOT EXISTS (SELECT 1 FROM rule_attributes WHERE discount_type_id = dt.id AND attribute_name = 'end_date')
ON CONFLICT DO NOTHING;

-- Create index for faster attribute lookups
CREATE INDEX IF NOT EXISTS idx_rule_attributes_name_key ON rule_attributes(attribute_name, discount_type_id);
```

**Impacto:**
- ✅ 0 cambios en estructura de tables
- ➕ +2 INSERT statements (idempotentes, reejecutables)
- ➕ +1 índice opcional para optimización
- 🟢 Risk Level: BAJO

### Cambios en Código Java

#### RuleService: 4 nuevos métodos privados + 2 integraciones

**1. validateSeasonalDateOverlap(UUID ecommerceId, UUID ruleId, LocalDate startDate, LocalDate endDate)**
- Valida orden de fechas: startDate <= endDate
- Query todas las reglas SEASONAL activas del ecommerce
- Extrae start_date y end_date de rule_attribute_values
- Detecta overlap: !end1.isBefore(start2) && !end2.isBefore(start1)
- Excluye current rule si es update (passed ruleId)
- Throws: `BadRequestException` (error de entrada), `ConflictException` (overlap detectado)

**2. validateDiscountLimits(UUID ecommerceId, BigDecimal discountPercentage)**
- Query DiscountSettingsEntity por ecommerceId
- Compara discountPercentage <= max_discount_cap
- Throws: `BadRequestException` (exceeds limit), `ResourceNotFoundException` (settings no existe)

**3. getAttributeDate(RuleEntity rule, String attributeName) → LocalDate**
- Extrae valores de rule_attribute_values
- Parsea formato ISO 8601 (YYYY-MM-DD)
- Throws: `BadRequestException` (formato inválido, atributo no encontrado)

**4. datesOverlap(LocalDate s1, LocalDate e1, LocalDate s2, LocalDate e2) → boolean**
- Helper: lógica matemática de overlap
- Retorna true si ranges se solapan

**5. findActiveSeasonalRulesByEcommerce(UUID ecommerceId) → List<RuleEntity>**
- Query optimizada: fetch todas las reglas activas, filter por tipo SEASONAL
- Evita N+1 queries usando stream + filter

#### Integración en createRule() y updateRule()

**createRule():**
```java
if ("SEASONAL".equals(typeCode)) {
    LocalDate startDate = LocalDate.parse(request.attributes().get("start_date"));
    LocalDate endDate = LocalDate.parse(request.attributes().get("end_date"));
    validateSeasonalDateOverlap(ecommerceId, null, startDate, endDate);
}
validateDiscountLimits(ecommerceId, request.discountPercentage());
```

**updateRule():**
```java
if ("SEASONAL".equals(typeCode)) {
    LocalDate startDate = LocalDate.parse(request.attributes().get("start_date"));
    LocalDate endDate = LocalDate.parse(request.attributes().get("end_date"));
    validateSeasonalDateOverlap(ecommerceId, ruleId, startDate, endDate); // Pass ruleId
}
validateDiscountLimits(ecommerceId, request.discountPercentage());
```

---

## 4. STACK TÉCNICO

| Componente | Versión | Notas |
|-----------|---------|-------|
| Java | 21+ | Virtual threads enabled |
| Spring Boot | 3.x | @Transactional para atomicidad |
| PostgreSQL | 13+ | TIMESTAMPTZ, UUID type |
| Flyway | 9.x | Version-based migrations |
| Jakarta Validation | 3.x | DTOs con @Valid, @NotNull |

### Tipo de Datos

- **Fechas:** `LocalDate` (Java) ↔ `DATE` (PostgreSQL)
- **Descuentos:** `BigDecimal` (nunca double/float)
- **IDs:** `UUID` (gen_random_uuid en DB)
- **Timestamps:** `Instant` (UTC en Java) ↔ `TIMESTAMPTZ` (PostgreSQL)

### Validación: Niveles

1. **DTO Level** (Jackson + Jakarta Validation):
   - `@NotBlank`, `@NotNull`, `@DecimalMin`, `@DecimalMax`

2. **Service Level** (RuleService - esta spec):
   - Validación de overlap de fechas
   - Validación de límites de descuento

3. **DB Level** (Constraints SQL):
   - UNIQUE constraints en rule_attributes (idempotencia)
   - CHECK constraints en discount_settings (max_discount_cap > 0)

---

## 5. FLUJOS DE ERROR

### Error 1: Superposición de Fechas (HU-06)

```
POST /api/v1/rules
{
  "name": "Black Friday 2026",
  "discountPercentage": "25.00",
  "discountPriorityId": "550e...",  // SEASONAL
  "attributes": {
    "start_date": "2026-11-27",
    "end_date": "2026-11-30"
  }
}

HTTP 409 Conflict
{
  "error": "SEASONAL rule date overlap detected. 
            Existing rule (5e8d...) covers 2026-11-25 to 2026-11-28"
}
```

### Error 2: Límite de Descuento

```
POST /api/v1/rules
{
  "name": "Promo 2026",
  "discountPercentage": "75.00",  // Exceeds max_discount_cap: 50.00
  "discountPriorityId": "..."
}

HTTP 400 Bad Request
{
  "error": "Discount percentage 75.00% exceeds maximum allowed: 50.00%"
}
```

---

## 6. CASOS DE ÉXITO

### Caso 1: Crear regla SEASONAL sin overlap

```
POST /api/v1/rules
{ "name": "Valentine's Day 2026", ... }

HTTP 201 Created
{ "id": "uuid", "name": "Valentine's Day 2026", ... }
```

### Caso 2: Actualizar descuentorespetando límites

```
PUT /api/v1/rules/{ruleId}
{ "discountPercentage": "45.00" }

HTTP 200 OK
{ "id": "ruleId", "discountPercentage": "45.00", ... }
```

---

## 7. IMPACTO

### Base de Datos

| Aspecto | Cambio |
|--------|--------|
| **Tables** | ➕ 0 nuevas tablas |
| **Columns** | ➕ 0 nuevas columnas |
| **Migrations** | ➕ 1 (V4) |
| **Indices** | ➕ 1 (idx_rule_attributes_name_key) |
| **Data Integrity** | ✅ Mejora: evita conflictos de fechas |
| **Performance** | ✅ Neutral (queries optimizadas con índices) |

### Código Java

| Aspecto | Cambio |
|--------|--------|
| **Métodos** | ➕ 4 privados |
| **Líneas de Código** | ➕ ~120 líneas |
| **Ficheros tocados** | ✏️ 1 (RuleService.java) |
| **Dependencias** | ✅ 0 nuevas |
| **Test Coverage** | ⚠️ Requiere 15-20 unit tests |

---

## 8. TESTING

### Unit Tests (RuleService)

1. **Validación de Superposición:**
   - ✓ No overlap: OK
   - ✓ Overlap total: ConflictException
   - ✓ Overlap parcial (inicio): ConflictException
   - ✓ Overlap parcial (fin): ConflictException
   - ✓ startDate > endDate: BadRequestException
   - ✓ Update exnorte current rule del check: OK
   - ✓ Validación solo para SEASONAL: OK (PRODUCT/CLASSIFICATION ignoran)

2. **Validación de Limits:**
   - ✓ Discount <= max_cap: OK
   - ✓ Discount > max_cap: BadRequestException
   - ✓ Aplica a TODAS las reglas: OK
   - ✓ Multi-tenancy: cada ecommerce tiene su max_cap: OK
   - ✓ discount_settings no existe: ResourceNotFoundException

### Integration Tests

- ✓ HU-06 create + overlap detection: OK
- ✓ HU-07 create + discount validation: OK
- ✓ RabbitMQ event emitido tras creación exitosa
- ✓ Rollback transaccional si falla validación

---

## 9. ROADMAP DE IMPLEMENTACIÓN

| Fase | Задача | Tiempo Est. |
|------|--------|------------|
| **1** | Crear V4 Migration | 5 min |
| **2** | Implementar 4 métodos privados en RuleService | 60 min |
| **3** | Integrar validaciones en createRule/updateRule | 30 min |
| **4** | Verificar repositorios | 10 min |
| **5** | Unit tests (15-20 tests) | 45 min |
| **6** | Integration tests | 30 min |
| **TOTAL** | | **~3 horas** |

---

## 10. DEFINICIÓN DE "LISTO"

✅ **Spec APROBADO cuando:**
- [ ] Requerimientos son claros y alineados con HU-06/HU-07
- [ ] Criterios de aceptación cubre todos los casos (success + error)
- [ ] Arquitectura (Opción A) es aceptada por el usuario
- [ ] Impacto en BD es mínimo y entendido
- [ ] Team acepta cronograma (~3 horas)

✅ **Implementación COMPLETA cuando:**
- [ ] V4 Migration existe e es idempotente
- [ ] Todos los métodos de validación implementados
- [ ] Tests pasan (unit + integration)
- [ ] Commits separados por layer (domain → application → infrastructure)
- [ ] Code review aprobado

---

## REFERENCIAS

- **HU-06:** `.github/requirements/seasonal-rules.md`
- **HU-07:** `.github/requirements/product-rules.md`
- **HU-09:** `.github/requirements/discount-limits.md`
- **Schema:** `backend/service-admin/src/main/resources/db/migration/V1__Create_database_schema.sql`
- **Architecture:** `.github/docs/guidelines/technical-architecture.md`
- **Backend Instr:** `.github/instructions/backend.instructions.md`

---

## CHANGELOG

| Versión | Fecha | Cambio |
|---------|-------|--------|
| 1.0 | 2026-04-07 | Spec inicial: validación overlap + discount limits |

