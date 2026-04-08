 Sincronización de Rules a Engine (FIDELITY, SEASONAL, PRODUCT)
Problema
Solo CLASSIFICATION se sincroniza. FIDELITY, SEASONAL y PRODUCT no tienen eventos hacia Engine.
Opción A: Eventos Genéricos (Recomendado)
Un solo tipo de evento que funcione para todos los tipos:
{
  "eventType": "RULE_CREATED" | "RULE_UPDATED" | "RULE_DELETED",
  "ruleId": "uuid",
  "ecommerceId": "uuid",
  "name": "string",
  "description": "string",
  "discountTypeCode": "FIDELITY | SEASONAL | PRODUCT | CLASSIFICATION",
  "discountValue": 20.00,
  "priorityLevel": 1,
  "logicConditions": { ... },  // atributos serializados
  "isActive": true
}
Implementar:
1. En RuleService, al crear/actualizar/eliminar regla, publicar evento
2. En Engine, unificar consumidores (o crear uno nuevo que maneje todos los tipos)
Ubicación:
- Admin: RuleService.java (después de save)
- Engine: actualizar engine_rules con cualquier tipo