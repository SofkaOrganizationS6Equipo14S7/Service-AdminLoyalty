# Service Engine - Mejoras aplicadas

Este documento resume las mejoras recientes realizadas en `service-engine` para estabilizar pruebas y facilitar seguimiento QA/DevOps.

## Mejoras implementadas

## 1) Pruebas unitarias agregadas

- `src/test/java/com/loyalty/service_engine/infrastructure/util/HashingUtilTest.java`
  - Valida que `sha256` sea determinístico.
  - Verifica longitud fija de 64 caracteres.
  - Verifica formato hexadecimal minúsculo.
  - Verifica que entradas distintas generen hash distinto.

- `src/test/java/com/loyalty/service_engine/presentation/controller/HealthControllerTest.java`
  - Valida `GET /health` a nivel unitario.
  - Asegura `HTTP 200`.
  - Asegura payload esperado:
    - `status = UP`
    - `service = service-engine`
    - `timestamp > 0`

## 2) Estabilidad backend integrada

Se integró el script central:

- `backend/scripts/check-backend-stability.ps1`

Este script ejecuta:

- `mvn -q clean test` en `service-engine`
- `mvn -q clean test` en `service-admin`
- Resumen final por servicio (`PASS/FAIL`)

Uso:

```powershell
cd backend
powershell -ExecutionPolicy Bypass -File .\scripts\check-backend-stability.ps1
```

Opcional:

```powershell
# solo admin
powershell -ExecutionPolicy Bypass -File .\scripts\check-backend-stability.ps1 -SkipEngine

# solo engine
powershell -ExecutionPolicy Bypass -File .\scripts\check-backend-stability.ps1 -SkipAdmin
```

## Validación realizada

- `service-engine`: `mvn -q clean test` en verde.
- `service-admin`: `mvn -q clean test` en verde.
- Ejecución consolidada desde frontend:
  - `npm run backend:stability` en verde.

## Nota de integración

`service-engine` se consume desde backend (server-to-server) mediante `apikey`. No está expuesto para consumo directo del frontend.

