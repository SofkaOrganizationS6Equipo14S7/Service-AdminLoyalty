1. HU-08: Fidelity Ranges - Validaciones Faltantes
Problema
Al crear reglas de clasificación (classification rules) en el endpoint:
POST /api/v1/rules/customer-tiers/{tierId}
Solo se valida que minValue < maxValue, pero faltan validaciones críticas.
Validaciones a implementar:
a) Continuidad (sin vacíos)
- Al crear/actualizar una regla, verificar que no haya huecos entre el maxValue de una regla y el minValue de la siguiente
- Ejemplo: Si hay regla con rango 0-1000, no puede haber otra que empiece en 2000 (falta 1000-2000)
b) Jerarquía ascendente
- Validar que cada nivel tenga un hierarchy_level mayor que el nivel anterior
- Si nivel 1 tiene minValue=0, nivel 2 debe tener minValue > maxValue del nivel 1
c) Unicidad de hierarchy_level
- No puede haber dos reglas con el mismo hierarchy_level para el mismo ecommerce
Ubicación:
RuleService.java → método createClassificationRuleForTier() y updateClassificationRuleForTier()
---