 HU-14: Sincronización de Cambio de Estado a Engine
Problema
Al activar/desactivar una regla, no se notifica al Engine Service para que actualice su caché.
Implementar:
a) Evento de cambio de estado:
{
  "eventType": "RULE_STATUS_CHANGED",
  "ruleId": "uuid",
  "ecommerceId": "uuid", 
  "isActive": true | false,
  "timestamp": "ISO-8601"
}
b) Publicador:
Crear RuleStatusEventPublisher (similar a ClassificationRuleEventPublisher)
c) Consumer en Engine:
Nuevo RuleStatusEventConsumer que actualice engine_rules.is_active
Ubicación:
- Admin: infrastructure/rabbitmq/RuleStatusEventPublisher.java
- Engine: nuevo consumer en infrastructure/rabbitmq/