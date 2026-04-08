---
id: SPEC-016
status: APPROVED
feature: fidelity-ranges-validations
created: 2026-04-08
updated: 2026-04-08
author: spec-generator
version: "1.0"
related-specs:
  - loyalty-tiers.spec.md
---

# Spec: Fidelity Ranges - Validaciones Faltantes (HU-08)

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de iniciar implementación.
> **Relacionada:** Contexto de [loyalty-tiers.spec.md](loyalty-tiers.spec.md) — criterios de clasificación para customer-tiers.

---

## 1. REQUERIMIENTOS

### Descripción
Al crear y actualizar reglas de clasificación (`classification_rules`) en el Admin Service, se deben implementar tres validaciones críticas que garanticen la integridad de los rangos de fidelidad: continuidad (sin huecos), jerarquía ascendente por `hierarchy_level` (`priority`), y unicidad de `hierarchy_level` por ecommerce.

### Requerimiento de Negocio

**HU-08: Fidelity Ranges - Validaciones Faltantes**

Problema:
- Al crear reglas de clasificación (classification rules) en el endpoint:
  ```
  POST /api/v1/rules/customer-tiers/{tierId}
  ```
- Actualmente se valida **SOLO** que `minValue < maxValue`, pero faltan validaciones críticas.

### Historias de Usuario

#### HU-08.1: Validar Continuidad de Rangos (sin huecos)

```
Como:        Administrador de ecommerce
Quiero:      Asegurar que no haya huecos entre los rangos de clasificación
Para:        Evitar que clientes caigan en rangos indefinidos

Prioridad:   Alta
Estimación:  M
Dependencias: Modelos clasificación rule existentes
Capa:        Backend (RuleService)
```

**Criterios de Aceptación — HU-08.1**

**CRITERIO-8.1.1: Continuidad en rangos válidos (Happy Path)**
```gherkin
Dado que:  existe un tier con regla de rango 0-1000
Y:         intento crear una nueva regla con rango 1000-2000
Cuando:    envío POST /api/v1/rules/customer-tiers/{tierId} hacia createClassificationRuleForTier()
Entonces:  la regla se crea exitosamente (201)
Y:         los rangos son continuos: [0-1000] ⟶ [1000-2000]
```

**CRITERIO-8.1.2: Validación de huecos (Error Path)**
```gherkin
Dado que:  existe regla A con rango 0-1000
Y:         existe regla B con rango 2000-3000
Cuando:    intento crear regla C con rango 500-800
Entonces:  la creación falla con 400 BadRequestException
Y:         el mensaje es "Gap detected: rule with range [500-800] has gap with existing rule [0-1000]"
```

**CRITERIO-8.1.3: Validación al actualizar (Edge Case)**
```gherkin
Dado que:  existe regla A con rango 0-1000
Y:         existe regla B con rango 1000-2000
Cuando:    intento actualizar regla A a rango 0-500 (creando hueco)
Entonces:  la actualización falla con 400
Y:         el mensaje es "Update would create gap between [0-500] and [1000-2000]"
```

#### HU-08.2: Validar Jerarquía Ascendente de `priority`

```
Como:        Administrador de ecommerce
Quiero:      Garantizar que cada nivel de prioridad sea mayor que el anterior
Para:        Mantener una evaluación consistente según hierarchy_level

Prioridad:   Alta
Estimación:  M
Dependencias: HU-08.1
Capa:        Backend (RuleService)
```

**Criterios de Aceptación — HU-08.2**

**CRITERIO-8.2.1: Jerarquía válida (Happy Path)**
```gherkin
Dado que:  no existen reglas para el tier
Cuando:    creo regla 1: priority=1, minValue=0, maxValue=1000
Y:         creo regla 2: priority=2, minValue=1000, maxValue=2000
Entonces:  ambas reglas se crean exitosamente
Y:         la jerarquía es ascendente: priority 1 < 2
```

**CRITERIO-8.2.2: Rechazo de jerarquía descendente (Error Path)**
```gherkin
Dado que:  existe regla con priority=2, minValue=1000, maxValue=2000
Cuando:    intento crear regla con priority=1, minValue=1000, maxValue=2000
Entonces:  falla con 409 ConflictException
Y:         el mensaje es "Hierarchy violation: new priority (1) must be > max existing priority (2) for this tier"
```

**CRITERIO-8.2.3: Validación al actualizar priority (Edge Case)**
```gherkin
Dado que:  existe regla A (priority=1)
Y:         existe regla B (priority=2)
Cuando:    intento actualizar regla A a priority=3 (permitido por la lógica del negocio)
Entonces:  la regla se actualiza exitosamente si la nueva jerarquía sigue siendo válida
Y:         si causa inversión, falla con 409
```

#### HU-08.3: Validar Unicidad de `hierarchy_level` (priority)

```
Como:        Administrador de ecommerce
Quiero:      Asegurar que no existan dos reglas con el mismo hierarchy_level para un ecommerce
Para:        Evitar ambigüedad en la evaluación de criterios de clasificación

Prioridad:   Alta
Estimación:  S
Dependencias: HU-08.1, HU-08.2
Capa:        Backend (RuleService)
```

**Criterios de Aceptación — HU-08.3**

**CRITERIO-8.3.1: Unicidad de priority (Happy Path)**
```gherkin
Dado que:  no existen reglas con priority=1 para el ecommerce
Cuando:    creo regla con priority=1, minValue=0, maxValue=1000
Entonces:  la regla se crea exitosamente (201)
```

**CRITERIO-8.3.2: Duplicado rechazado (Error Path)**
```gherkin
Dado que:  existe regla A con priority=1 para el ecommerce
Y:         existe un tier diferente Y
Cuando:    intento crear regla en tier Y con priority=1
Entonces:  falla con 409 ConflictException
Y:         el mensaje es "Duplicate hierarchy_level: priority (1) already exists for ecommerce {ecommerceId}"
```

**CRITERIO-8.3.3: Actualización que causa duplicado (Error Path)**
```gherkin
Dado que:  existe regla A con priority=1
Y:         existe regla B con priority=2
Cuando:    intento actualizar regla B a priority=1
Entonces:  falla con 409
Y:         el mensaje es "Duplicate hierarchy_level: cannot update to priority (1) — already used"
```

---

### Reglas de Negocio

1. **Continuidad**: Al crear/actualizar una regla de clasificación, el sistema debe verificar que no exista un hueco entre `maxValue` de una regla y `minValue` de la siguiente. 
   - Si minValue de nueva regla ≠ maxValue de regla anterior (ordenadas por `minValue`) → **rechazo con 400**.

2. **Jerarquía Ascendente**: Cada nivel de `priority` (alias `hierarchy_level`) debe ser **estrictamente mayor** que el anterior.
   - Validar contra TODAS las reglas del **mismo ecommerce** (no solo del tier).
   - Si nueva `priority` ≤ max `priority` existente → **rechazo con 409**.

3. **Unicidad de priority**: Un `hierarchy_level` (priority) no puede repetirse para el mismo ecommerce.
   - Consultar todas las reglas clasificación del ecommerce.
   - Si existe otra regla con el mismo `priority` (incluso en diferente tier) → **rechazo con 409**.

4. **Scope de validación**:
   - Validaciones aplican al **crear** (POST) y **actualizar** (PUT) classification rules.
   - Scope: **por ecommerce** (aunque reglas estén asociadas a tiers específicos).

5. **Manejo de soft-delete**: Las reglas eliminadas (soft-delete, `isActive=false`) **NO** participan en validaciones de continuidad, jerarquía ni unicidad.

6. **Response HTTP**:
   - **201**: Creación exitosa.
   - **400 BadRequestException**: Errores de validación de continuidad o estructura.
   - **409 ConflictException**: Errores de jerarquía o unicidad.

---

## 2. DISEÑO

### Modelos de Datos

#### Entidades afectadas

| Entidad | Almacén | Cambios | Descripción |
|---------|---------|---------|-------------|
| `RuleEntity` | tabla `rules` | no modificar estructura | Regla padre; ya existe |
| `RuleCustomerTierEntity` | tabla `rule_customer_tiers` | no modificar | Vinculación regla ↔ tier; ya existe |
| `RuleAttributeValueEntity` | tabla `rule_attribute_values` | no modificar | Almacena `minValue`, `maxValue`, `priority`; ya existe |

#### Validaciones en el modelo de datos

El sistema **NO** añade columnas de BD. Las validaciones se implementan en la lógica de aplicación (`RuleService`):
- Las validaciones consultan el estado actual de reglas en el ecommerce.
- Usan transacciones para garantizar consistencia.

#### Índices / Constraints (no cambian)
- Índice existente en `rules.ecommerce_id` — usado en consultas de validación.
- Índice existente en `rule_customer_tiers.customer_tier_id` — usado en listados por tier.

---

### API Endpoints (Existentes - No cambian firmsa)

#### POST /api/v1/rules/customer-tiers/{tierId}
- **Descripción**: Crea `classification_rule` válida para un tier.
- **Auth requerida**: Bearer token (admin)
- **Request Body**:
  ```json
  {
    "discountPriorityId": "uuid",
    "name": "string",
    "description": "string (opcional)",
    "discountPercentage": 5.00,
    "metricType": "TOTAL_SPENT|ORDER_COUNT|LOYALTY_POINTS|CUSTOM",
    "minValue": 100.00,
    "maxValue": 1000.00,
    "priority": 1
  }
  ```
- **Validaciones NUEVAS**:
  - Continuidad: verificar no-huecos (CRITERIO-8.1.1, 8.1.2)
  - Jerarquía: verificar `priority` > max existente (CRITERIO-8.2.2, 8.2.3)
  - Unicidad: verificar `priority` único en ecommerce (CRITERIO-8.3.2)
  
- **Response 201**:
  ```json
  {
    "uid": "uuid",
    "name": "string",
    "metricType": "string",
    "minValue": 100.00,
    "maxValue": 1000.00,
    "priority": 1,
    "discountPercentage": 5.00,
    "created_at": "2026-04-08T10:00:00Z",
    "updated_at": "2026-04-08T10:00:00Z"
  }
  ```

- **Response 400**: Errores de continuidad o estructura
  ```json
  {
    "error": "BadRequestException",
    "message": "Gap detected: rule with range [100-500] has gap with existing rule [0-1000]",
    "timestamp": "2026-04-08T10:00:00Z"
  }
  ```

- **Response 409**: Errores de jerarquía o unicidad
  ```json
  {
    "error": "ConflictException",
    "message": "Hierarchy violation: new priority (1) must be > max existing priority (2) for this ecommerce",
    "timestamp": "2026-04-08T10:00:00Z"
  }
  ```

#### PUT /api/v1/rules/customer-tiers/{tierId}/{ruleId}
- **Descripción**: Actualiza `classification_rule` aplicando mismas validaciones que POST.
- **Auth requerida**: Bearer token (admin)
- **Request Body**: Campos opcionales de ClassificationRuleUpdateRequest
- **Validaciones NUEVAS**:
  - Aplicar las mismas validaciones de continuidad, jerarquía y unicidad (CRITERIOS-8.1.3, 8.2.3, 8.3.3)
  - Si solo se actualiza `name` o `description`, no re-validar.
  - Si se actualiza `minValue`, `maxValue` o `priority`, ejecutar todas las validaciones.
  
- **Response 200**: Regla actualizada
- **Response 400 / 409**: Como en POST

---

### Lógica de Validación (Backend)

#### 1. Validador de Continuidad — `ContinuityValidator`

```java
/**
 * Valida que no existan huecos entre rangos de clasificación.
 * 
 * Lógica:
 * 1. Obtener todas las reglas activas del ecommerce (solo tipo CLASSIFICATION)
 * 2. Ordenar por minValue ascendente
 * 3. Para la nueva regla (minValue, maxValue):
 *    - Encontrar la regla anterior (maxValue anterior = minValue nueva?)
 *    - Si no coinciden → error 400
 * 4. Encontrar la regla siguiente (minValue siguiente = maxValue nueva?)
 *    - Si no coinciden → error 400
 * 5. Si son coincidentes → OK
 *
 * Nota: Se ignoran reglas soft-deleted (isActive=false)
 */
public void validateContinuity(UUID ecommerceId, BigDecimal minValue, BigDecimal maxValue, UUID excludeRuleId) 
    throws BadRequestException {
    
    List<RuleEntity> activeRules = ruleRepository
        .findActiveClassificationRulesByEcommerce(ecommerceId, excludeRuleId);  // CustomQueryMethod
    
    // Obtener rangos de todas las reglas
    List<RangeData> ranges = activeRules.stream()
        .map(rule -> extractRangeFromRule(rule))
        .sorted(Comparator.comparing(r -> r.minValue))
        .collect(Collectors.toList());
    
    // Buscar hueco
    boolean hasGapBefore = ranges.isEmpty() ? minValue.compareTo(BigDecimal.ZERO) != 0
        : ranges.get(ranges.size() - 1).maxValue.compareTo(minValue) != 0;
    
    if (hasGapBefore && !ranges.isEmpty()) {
        throw new BadRequestException(
            "Gap detected: rule with range [" + minValue + "-" + maxValue + 
            "] has gap with existing rule [" + ranges.last().minValue + "-" + ranges.last().maxValue + "]"
        );
    }
    
    // Buscar hueco después
    Optional<RangeData> nextRange = ranges.stream()
        .filter(r -> r.minValue.compareTo(maxValue) > 0)
        .findFirst();
    
    if (nextRange.isPresent() && nextRange.get().minValue.compareTo(maxValue) != 0) {
        throw new BadRequestException(
            "Gap detected: rule with range [" + minValue + "-" + maxValue + 
            "] has gap with next rule [" + nextRange.get().minValue + "-" + nextRange.get().maxValue + "]"
        );
    }
}
```

#### 2. Validador de Jerarquía — `HierarchyValidator`

```java
/**
 * Valida que priority (hierarchy_level) sea ascendente por ecommerce.
 * 
 * Lógica:
 * 1. Obtener todas las reglas activas de clasificación del ecommerce
 * 2. Extraer máximo priority actual (excluyendo la regla siendo actualizada)
 * 3. Si nueva priority ≤ max priority → error 409
 * 4. Si nueva priority > max priority → OK
 */
public void validateHierarchy(UUID ecommerceId, Integer newPriority, UUID excludeRuleId) 
    throws ConflictException {
    
    List<RuleEntity> activeRules = ruleRepository
        .findActiveClassificationRulesByEcommerce(ecommerceId, excludeRuleId);
    
    Integer maxPriority = activeRules.stream()
        .map(rule -> extractPriorityFromRule(rule))
        .max(Integer::compareTo)
        .orElse(0);
    
    if (newPriority <= maxPriority) {
        throw new ConflictException(
            "Hierarchy violation: new priority (" + newPriority + 
            ") must be > max existing priority (" + maxPriority + ") for this ecommerce"
        );
    }
}
```

#### 3. Validador de Unicidad — `UniquePriorityValidator`

```java
/**
 * Valida que priority no se repita en el ecommerce.
 * 
 * Lógica:
 * 1. Obtener todas las reglas activas de clasificación del ecommerce
 * 2. Extraer priorities existentes (excluyendo la regla siendo actualizada)
 * 3. Si nueva priority ∈ existentes → error 409
 * 4. Si nueva priority ∉ existentes → OK
 */
public void validateUniquePriority(UUID ecommerceId, Integer newPriority, UUID excludeRuleId) 
    throws ConflictException {
    
    List<RuleEntity> activeRules = ruleRepository
        .findActiveClassificationRulesByEcommerce(ecommerceId, excludeRuleId);
    
    boolean priorityExists = activeRules.stream()
        .map(rule -> extractPriorityFromRule(rule))
        .anyMatch(p -> p.equals(newPriority));
    
    if (priorityExists) {
        throw new ConflictException(
            "Duplicate hierarchy_level: priority (" + newPriority + 
            ") already exists for ecommerce " + ecommerceId
        );
    }
}
```

#### 4. Integración en `RuleService`

```java
@Transactional
public ClassificationRuleResponse createClassificationRuleForTier(
        UUID ecommerceId, 
        UUID tierId, 
        ClassificationRuleCreateRequest request
) {
    // ... validaciones existentes (minValue < maxValue, tipoEnum, etc.) ...
    
    // VALIDACIONES NUEVAS (HU-08)
    continuityValidator.validateContinuity(
        ecommerceId, 
        request.minValue(), 
        request.maxValue(), 
        null  // null = crear, no actualizar
    );
    
    hierarchyValidator.validateHierarchy(
        ecommerceId, 
        request.priority(), 
        null
    );
    
    uniquePriorityValidator.validateUniquePriority(
        ecommerceId, 
        request.priority(), 
        null
    );
    
    // ... resto de lógica de creación ...
}

@Transactional
public ClassificationRuleResponse updateClassificationRuleForTier(
        UUID ecommerceId, 
        UUID tierId, 
        UUID ruleId,
        ClassificationRuleUpdateRequest request
) {
    // ... validaciones existentes ...
    
    // Si se actualizan minValue, maxValue o priority → re-validar (HU-08)
    if (request.minValue() != null || request.maxValue() != null || request.priority() != null) {
        BigDecimal actualMinValue = request.minValue() != null ? request.minValue() : getCurrentMinValue(ruleId);
        BigDecimal actualMaxValue = request.maxValue() != null ? request.maxValue() : getCurrentMaxValue(ruleId);
        Integer actualPriority = request.priority() != null ? request.priority() : getCurrentPriority(ruleId);
        
        continuityValidator.validateContinuity(ecommerceId, actualMinValue, actualMaxValue, ruleId);
        hierarchyValidator.validateHierarchy(ecommerceId, actualPriority, ruleId);
        uniquePriorityValidator.validateUniquePriority(ecommerceId, actualPriority, ruleId);
    }
    
    // ... resto de lógica de actualización ...
}
```

---

## 3. LISTA DE TAREAS

> Checklist accionable para los agentes. Marcar cada ítem (`[x]`) al completarlo.

### Backend

#### Implementación

- [ ] **Crear validator `ContinuityValidator`**
  - [ ] Implementar `validateContinuity(UUID ecommerceId, BigDecimal minValue, BigDecimal maxValue, UUID excludeRuleId)`
  - [ ] Extraer rango (minValue, maxValue) de atributos rule_attributes
  - [ ]Ordenar rangos y detectar huecos
  - [ ] Lanzar `BadRequestException` con mensaje detallado

- [ ] **Crear validator `HierarchyValidator`**
  - [ ] Implementar `validateHierarchy(UUID ecommerceId, Integer newPriority, UUID excludeRuleId)`
  - [ ] Extraer priority máximo de reglas existentes
  - [ ] Lanzar `ConflictException` si nueva priority ≤ max

- [ ] **Crear validator `UniquePriorityValidator`**
  - [ ] Implementar `validateUniquePriority(UUID ecommerceId, Integer newPriority, UUID excludeRuleId)`
  - [ ] Verificar que priority no exista en ecommerce
  - [ ] Lanzar `ConflictException` si duplicada

- [ ] **Métodos helper en `RuleRepository`**
  - [ ] `findActiveClassificationRulesByEcommerce(UUID ecommerceId, UUID excludeRuleId)` — custom query
  - [ ] Filtrar: `isActive=true` Y `type=CLASSIFICATION` Y excluir ruleId si aplica

- [ ] **Métodos helper en `RuleService`**
  - [ ] `extractRangeFromRule(RuleEntity)` — obtener minValue, maxValue de rule_attributes
  - [ ] `extractPriorityFromRule(RuleEntity)` — obtener priority de rule_attributes
  - [ ] `getCurrentMinValue(UUID ruleId)`, `getCurrentMaxValue(UUID ruleId)`, `getCurrentPriority(UUID ruleId)`

- [ ] **Integración en `RuleService.createClassificationRuleForTier()`**
  - [ ] Invocar 3 validadores antes de crear la regla
  - [ ] Orden: continuidad → jerarquía → unicidad
  - [ ] Capturar excepciones y dejarlas propagarse (Spring maneja HTTP status)

- [ ] **Integración en `RuleService.updateClassificationRuleForTier()`**
  - [ ] Invocar 3 validadores SOLO si se actualizan `minValue`, `maxValue` o `priority`
  - [ ] Usar `ruleId` como `excludeRuleId` para no compararse consigo mismo
  - [ ] Capturar excepciones

- [ ] **Dependency Injection**
  - [ ] Inyectar 3 validators en `RuleService` (constructor)
  - [ ] registrar validators como `@Component` o `@Service`

#### Tests Backend

- [ ] **Test `ContinuityValidator`**
  - [ ] `test_continuity_valid_consecutive_ranges()` — [100-500] + [500-1000] = OK
  - [ ] `test_continuity_gap_detected()` — [100-500] + [700-1000] = ERROR 400
  - [ ] `test_continuity_first_rule_zero()` — primera regla empezando en 0 = OK
  - [ ] `test_continuity_update_creates_gap()` — actualizar crea hueco = ERROR
  - [ ] `test_continuity_ignores_inactive_rules()` — reglas con isActive=false no se validan

- [ ] **Test `HierarchyValidator`**
  - [ ] `test_hierarchy_valid_ascending()` — priority 1, then 2, then 3 = OK
  - [ ] `test_hierarchy_invalid_descending()` — priority 2 cuando max=2 = ERROR 409
  - [ ] `test_hierarchy_invalid_equal()` — priority 2 cuando max=2 = ERROR 409
  - [ ] `test_hierarchy_update_increases()` — cambiar de 1 a 3 cuando max=2 = OK
  - [ ] `test_hierarchy_update_decreases()` — cambiar de 3 a 1 cuando max=3 = ERROR
  - [ ] `test_hierarchy_ignores_inactive_rules()` — inactive rules no afectan

- [ ] **Test `UniquePriorityValidator`**
  - [ ] `test_unique_priority_new_value()` — priority=1, then=2 = OK
  - [ ] `test_unique_priority_duplicate_same_tier()` — priority=1 duplicada en mismo tier = ERROR 409
  - [ ] `test_unique_priority_duplicate_different_tier()` — priority=1 en tieres diferentes = ERROR 409
  - [ ] `test_unique_priority_update_to_duplicate()` — actualizar a priority duplicada = ERROR
  - [ ] `test_unique_priority_ignores_inactive()` — priority inactive no cuenta

- [ ] **Test `RuleService.createClassificationRuleForTier()`**
  - [ ] `test_create_classification_rule_all_validations_pass()` — happy path
  - [ ] `test_create_classification_rule_gap_error()` — error continuidad
  - [ ] `test_create_classification_rule_hierarchy_error()` — error jerarquía
  - [ ] `test_create_classification_rule_unique_priority_error()` — error unicidad

- [ ] **Test `RuleService.updateClassificationRuleForTier()`**
  - [ ] `test_update_classification_rule_name_only()` — actualizar solo nombre = sin re-validar
  - [ ] `test_update_classification_rule_minmax_change()` — cambiar rango = re-validar
  - [ ] `test_update_classification_rule_priority_change()` — cambiar priority = re-validar
  - [ ] `test_update_classification_rule_creates_gap()` — update crea hueco = ERROR
  - [ ] `test_update_classification_rule_violates_hierarchy()` — update viola jerarquía = ERROR
  - [ ] `test_update_classification_rule_duplicate_priority()` — update a priority existente = ERROR

### QA & Documentation

- [ ] **Ejecutar `/gherkin-case-generator`**
  - [ ] Generar escenarios para CRITERIO-8.1.1 a 8.3.3
  - [ ] Generar datos de prueba (ranges, priorities)
  - [ ] Outputs en `docs/output/qa/gherkin/`

- [ ] **Ejecutar `/risk-identifier`**
  - [ ] Clasificar riesgos (Alto/Medio/Bajo)
  - [ ] Data integrity risk (continuidad, jerarquía, unicidad)
  - [ ] Concurrency risk (transacciones simultáneas)

- [ ] **Revisar cobertura tests vs. criterios**
  - [ ] Cada CRITERIO-8.x.x tiene al menos 1 test

- [ ] **Validar status spec**
  - [ ] Actualizar a `status: APPROVED` cuando esté lista
  - [ ] Actualizar a `status: IMPLEMENTED` cuando coding complete

---

## 4. NOTAS DE IMPLEMENTACIÓN

### Puntos Clave Técnicos

1. **Scope de validación**: Las validaciones aplican **por ecommerce**, no por tier. Dos reglas en tiers diferentes del mismo ecommerce deben cumplir las reglas de jerarquía y unicidad.

2. **Soft-delete**: Reglas con `isActive=false` **NO** participan en ninguna validación. Solo se consideran reglas activas (`isActive=true`).

3. **Extracción de atributos**: Los valores `minValue`, `maxValue` y `priority` están almacenados en la tabla `rule_attribute_values` como strings. 
   - Crear helper method `extractRangeFromRule()` y `extractPriorityFromRule()` para deserializar.
   - Usar transacciones para garantizar lectura consistente.

4. **Query personalizada**: `RuleRepository.findActiveClassificationRulesByEcommerce(ecommerceId, excludeRuleId)` — debe usar JOIN con `rule_attributes` para obtener el tipo de la regla Y excluir la regla siendo actualizada.

5. **Mensajes de error descriptivos**: Los mensajes deben ser lo suficientemente específicos para debugging pero no revelar detalles innecesarios al cliente.

6. **Transacción**: Los métodos de creación y actualización **ya son `@Transactional`**. Las validaciones se ejecutan dentro de la transacción.

7. **Orden de validaciones**: 
   - **Primero**: Continuidad (estructura de datos)
   - **Segundo**: Jerarquía (orden)
   - **Tercero**: Unicidad (identidad)
   - Esto evita crear inconsistencias.

8. **Performance**: Las consultas de validación deben ser **indexadas** en `ecommerce_id` e `isActive`. Check que los índices existen.

---

## 5. DEPENDENCIAS Y RIESGOS

### Dependencias
- **Tabla `rules`**: Ya existe, no renovada en esta implement.
- **Tabla `rule_customer_tiers`**: Ya existe.
- **Tabla `rule_attribute_values`**: Ya existe, se consulta para extraer atributos.

### Riesgos Identificados

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|-------------|--------|-----------|
| **Concurrencia**: Dos requests simultáneos crean duplicados | Media | Alto | Usar `@Transactional` con aislamiento adecuado en DB (SERIALIZABLE si critical) |
| **Performance**: Queries de validación lentas** | Media | Medio | Indexar `rules.ecommerce_id` e `rules.isActive`; usar LIMIT 1 en búsquedas |
| **Backward compatibility**: Reglas existentes pueden violar nuevas validaciones | Alta | Bajo | Restricción: App asume BD ya es consistente; no migrar datos viejos |
| **Parsing de atributos**: Error al extraer minValue/maxValue | Baja | Alto | Unit tests en `extractRangeFromRule()`; validaciones de tipo |

