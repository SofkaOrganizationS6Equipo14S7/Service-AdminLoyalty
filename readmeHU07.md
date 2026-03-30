# HU07 - Tope Maximo y Prioridad de Descuentos

## 1. Contexto de la HU

**Historia de usuario**

Como usuario de LOYALTY, quiero definir el tope maximo y la prioridad de descuentos para proteger la rentabilidad del negocio.

**Servicios involucrados**

1. `service-admin` (dueno de configuracion)
2. `service-engine` (consumidor read-only de configuracion)
3. RabbitMQ (sincronizacion por eventos)

**Contrato principal**

1. `POST /api/v1/configurations` crea/reemplaza configuracion completa
2. `PATCH /api/v1/configurations/{ecommerceId}` actualiza parcialmente (merge)
3. `POST /api/v1/discounts/calculate` aplica prioridad + cap en engine

## 2. Reglas funcionales HU07

1. El cap debe ser valido (`value > 0`).
2. La prioridad debe ser deterministica (sin duplicados de `order` ni `type`).
3. El engine aplica descuentos en orden de prioridad.
4. Si el total supera el cap, recorta al tope configurado.
5. Si ecommerce no tiene configuracion, engine usa defaults (`COP`, `HALF_UP`, sin cap).

## 3. Hardening de produccion aplicado

1. Logging estructurado con `X-Correlation-Id` y MDC en ambos servicios.
2. RabbitMQ con retry + backoff exponencial configurable.
3. DLQ configurada para eventos de configuracion y API keys.
4. Idempotencia:
   1. `CONFIG_UPDATED` en engine aplica solo versiones mas nuevas.
   2. Eventos de API key deduplicados por `eventId`.

## 4. Prerequisitos

1. Java 17
2. Maven 3.9+
3. Docker Desktop
4. Puertos libres:
   1. `8081` (admin)
   2. `8082` (engine)
   3. `5432` (postgres admin)
   4. `5433` (postgres engine)
   5. `5672` y `15672` (RabbitMQ + Management)

## 5. Arranque de infraestructura

Desde la raiz del repo:

```powershell
cd backend
docker compose up -d
docker compose ps
```

RabbitMQ Management:

1. URL: `http://localhost:15672`
2. User: `guest`
3. Password: `guest`

## 6. Arranque de microservicios

Terminal 1:

```powershell
cd backend/service-admin
mvn spring-boot:run
```

Terminal 2:

```powershell
cd backend/service-engine
mvn spring-boot:run
```

## 7. Datos minimos para usar HU07

Para operar endpoints protegidos necesitas:

1. Un usuario `ROLE_ADMIN` en `service-admin`.
2. Un `ecommerceId` valido.
3. Una API Key activa para ese ecommerce (se crea en admin y se replica al engine por RabbitMQ).

Nota: si tu ambiente no tiene datos seed, cargalos segun la estrategia del equipo (SQL seed o script interno).

## 8. Flujo E2E recomendado (HU07)

## Paso 1: Login en admin

```http
POST http://localhost:8081/api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

Guardar `token`.

## Paso 2: Crear configuracion de cap/prioridad

```http
POST http://localhost:8081/api/v1/configurations
Authorization: Bearer <JWT_ADMIN>
Content-Type: application/json

{
  "ecommerceId": "11111111-1111-1111-1111-111111111111",
  "currency": "COP",
  "roundingRule": "HALF_UP",
  "cap": {
    "type": "PERCENTAGE",
    "value": 20,
    "appliesTo": "SUBTOTAL"
  },
  "priority": [
    { "type": "SEASONAL", "order": 1 },
    { "type": "LOYALTY", "order": 2 }
  ]
}
```

## Paso 3: Crear API key para el ecommerce

```http
POST http://localhost:8081/api/v1/ecommerces/11111111-1111-1111-1111-111111111111/api-keys
Authorization: Bearer <JWT_ADMIN>
Content-Type: application/json
```

Guardar la key retornada enmascarada/valor de uso segun respuesta de tu ambiente.

## Paso 4: Esperar sincronizacion

Espera unos segundos para que:

1. `CONFIG_UPDATED` llegue al engine.
2. Evento de API key llegue al cache del engine.

## Paso 5: Calcular descuentos en engine

```http
POST http://localhost:8082/api/v1/discounts/calculate
Authorization: Bearer <API_KEY_ECOMMERCE>
Content-Type: application/json

{
  "ecommerceId": "11111111-1111-1111-1111-111111111111",
  "subtotal": 1000,
  "total": 1190,
  "beforeTax": 1000,
  "afterTax": 1190,
  "discounts": [
    { "type": "LOYALTY", "amount": 150 },
    { "type": "SEASONAL", "amount": 100 }
  ]
}
```

Esperado:

1. Orden aplicado por prioridad (`SEASONAL` primero).
2. Total calculado recortado por cap del 20% sobre `subtotal` (= 200).

## 9. Casos de aceptacion clave HU07

1. Configuracion valida:
   1. `POST /configurations` retorna `201`.
   2. Engine consume evento y aplica nueva version.
2. Tope invalido (`<= 0`):
   1. Admin retorna `400 VALIDATION_ERROR`.
   2. Se mantiene ultima config valida.
3. Prioridad ambigua (duplicados):
   1. Admin retorna `400 VALIDATION_ERROR`.
   2. No cambia configuracion vigente.
4. Acumulacion supera tope:
   1. Engine recorta descuento final al cap configurado.

## 10. Observabilidad y resiliencia

## Logs estructurados

Busca en logs claves como:

1. `event=config_updated_published`
2. `event=config_updated_consumed`
3. `event=config_event_ignored` (stale/duplicado)
4. `event=api_key_event_duplicate_ignored`

Incluyen `trace=<correlationId>` y datos de ecommerce/version.

## RabbitMQ y DLQ

Colas/exchanges relevantes:

1. Exchange: `loyalty.config.exchange`
2. Exchange DLX: `loyalty.config.dlx`
3. Queue admin config: `loyalty.config.updated.queue`
4. Queue engine config: `engine.config.updated.queue`
5. DLQ config engine: `engine.config.updated.dlq`
6. Queue API keys engine: `engine-api-keys-queue`
7. DLQ API keys engine: `engine-api-keys-dlq`

Si hay errores de consumo repetidos, valida mensajes en DLQ desde RabbitMQ UI.

## 11. Pruebas automaticas recomendadas

```powershell
cd backend/service-admin
mvn verify -DskipITs

cd ../service-engine
mvn verify -DskipITs
```

Cobertura actual valida reglas de prioridad, cap y casos negativos en unitarias/integracion.

## 12. Troubleshooting rapido

1. `401` en engine:
   1. Falta header `Authorization: Bearer <API_KEY>`.
   2. API key no sincronizada aun.
   3. API key invalida/revocada.
2. `400` en admin al crear config:
   1. `cap.value <= 0`.
   2. `priority` con `order` o `type` duplicado.
3. Engine no refleja config nueva:
   1. Revisar RabbitMQ queues/bindings.
   2. Confirmar evento `CONFIG_UPDATED` en logs.
   3. Verificar que version enviada sea mayor a la cacheada.
