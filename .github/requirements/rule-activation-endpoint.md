2. HU-14: Activar/Desactivar Reglas - Endpoint Dedicado
Problema
Actualmente la activación/desactivación se hace mediante DELETE (soft delete) o PUT (update completo), pero:
1. No hay un endpoint específico para cambiar estado
2. No se sincroniza el cambio de estado con Engine
Implementar:
a) Nuevo endpoint PATCH:
PATCH /api/v1/rules/{ruleId}/status
Body: { "active": true | false }
b) Lógica:
- Si active = true: cambia isActive = true (reactivar)
- Si active = false: cambia isActive = false (desactivar)
- Mantiene todos los demás datos intactos
- No permite si la regla no existe (404)
c) Validaciones:
- Verificar que la regla exista y pertenezca al ecommerce del usuario
- Verificar tenant isolation
Ubicación:
RuleController.java y RuleService.java