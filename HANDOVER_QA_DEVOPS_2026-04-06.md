# Handover QA + DevOps (Completo)

Fecha de corte: 2026-04-06  
Repositorio: `LOYALTY_S7_1`  
Responsable del handover: QA/DevOps (auditoría técnica sobre código + ejecución de pruebas)

## 1) Resumen ejecutivo

- Proyecto backend con 2 microservicios Spring Boot (`service-admin`, `service-engine`) + infraestructura local vía Docker Compose.
- Estado de calidad actual: **NO listo para release productiva**.
- Hallazgos críticos:
  - No existe pipeline CI/CD versionado en `.github/workflows`.
  - `service-admin` falla pruebas unitarias/integración.
  - `service-engine` falla discovery de pruebas por `NoClassDefFoundError`.
  - Hay desalineación entre seguridad y endpoint real en engine (`/api/v1/engine/calculate`).
  - Secretos y credenciales hardcodeados en repo.
- Riesgo global: **ALTO** (operacional + seguridad + regresión).

## 2) Inventario del sistema

### 2.1 Componentes
- `backend/service-admin`: API administrativa (autenticación JWT, usuarios, ecommerces, configuración, reglas, logs, API keys).
- `backend/service-engine`: motor de cálculo de descuentos S2S (API key auth, cálculo de carrito, health).
- `backend/docker-compose.yml`: Postgres admin, Postgres engine, RabbitMQ.
- Documentación funcional/QA:
  - `Readme.md`
  - `TEST_PLAN.md`
  - `TEST_CASES.md`
  - `backend/service-admin/ENDPOINTS.md`
  - `backend/service-admin/API_DOCUMENTATION.md`

### 2.2 Stack técnico
- Java 21 (validado en entorno local).
- Spring Boot 3.5.12.
- Maven 3.9.x.
- PostgreSQL 15.
- RabbitMQ 3-management.
- Flyway para migraciones.

## 3) Topología e infraestructura

### 3.0 Baseline operativo (alineado a `Readme.md`)
- Pre-requisitos declarados:
  - Java 21+
  - Maven (o `mvnw`)
  - Docker + Docker Compose
- Orden de arranque recomendado por README:
  1. `cd backend && docker compose up -d`
  2. levantar `service-admin` en `8081`
  3. levantar `service-engine` en `8082`
- Puertos de referencia documentados:
  - admin `8081`
  - engine `8082`
  - rabbit `5672/15672`
  - postgres admin `5432`
  - postgres engine `5433`

Nota de handover: este baseline es correcto como guía de operación local, pero hoy presenta brechas de ejecución (ver 3.3 y 7.1).

### 3.1 Docker Compose (local)
Archivo: `backend/docker-compose.yml`

Servicios:
- `postgres-admin`: `localhost:5432`, DB `loyalty_admin`.
- `postgres-engine`: `localhost:5433`, DB `loyalty_engine`.
- `rabbitmq`: `5672`, UI en `15672`.

### 3.2 Credenciales/secretos detectados en código
- DB password: `loyalty123`.
- RabbitMQ credenciales: `guest/guest`.
- JWT secret fijo por defecto en ambos servicios.

Esto requiere rotación y externalización inmediata (ver sección 10).

### 3.3 Brecha concreta contra README
- README indica uso de wrapper (`.\mvnw.cmd spring-boot:run` / `mvnw` incluido).
- En la auditoría, `mvnw.cmd` falló en ambos servicios con `Cannot start maven from wrapper`.
- Implicación DevOps: el comando oficial de arranque/build no es confiable en el estado actual; se necesita corregir wrapper o actualizar README con comando alternativo validado (`mvn`).

## 4) Arquitectura de datos y eventos

### 4.1 Base de datos admin
- Migraciones:
  - `V1__Create_database_schema.sql`
  - `V2__Seed_initial_data.sql`
  - `V3__Create_rule_customer_tiers_table.sql`
- Incluye entidades de identidad/tenant, configuración de descuentos, reglas dinámicas, customer tiers, API keys, auditoría y logs.

### 4.2 Base de datos engine
- Migración:
  - `V1__Engine_Service_Database_Schema.sql`
- Replica estructuras necesarias para cálculo y logging transaccional (`transaction_logs`), sincronizadas por eventos RabbitMQ.

### 4.3 Mensajería
- Ambos servicios tienen configuración de exchanges/queues/routing keys para:
  - config updates
  - api keys
  - seasonal rules
  - clasificación
  - customer tiers
  - prioridades
  - product rules

No se encontró runbook de DLQ/reprocesos ni alarmas.

## 5) Seguridad y control de acceso

### 5.1 Admin
- JWT + `AuthenticationFilter`.
- Multi-tenant con `TenantInterceptor` y `SecurityContextHelper`.
- Seguridad HTTP configurada en `SecurityConfiguration`.

### 5.2 Engine
- API key auth con `ApiKeyAuthenticationFilter`.
- Seguridad declarada en `SecurityConfig`.

### 5.3 Hallazgo crítico de seguridad/funcionalidad
- El controlador expone `POST /api/v1/engine/calculate`.
- `SecurityConfig` protege `POST /api/v1/discount/calculate` y `POST /api/v1/discounts/calculate`.
- Resultado esperado: el endpoint real puede quedar denegado por reglas de autorización (`anyRequest().denyAll()`), según cadena de seguridad.

Esto debe corregirse antes de salida.

## 6) Inventario de APIs (estado real vs docs)

### 6.1 service-admin
- Controllers presentes para `auth`, `users`, `roles/permissions`, `ecommerces`, `api-keys`, `configurations`, `discount-config`, `rules`, `customer-tiers`, `audit-logs`, `discount-logs`.

### 6.2 service-engine
- Endpoints detectados:
  - `GET /api/v1/health`
  - `POST /api/v1/engine/calculate`

### 6.3 Inconsistencias documentación vs código
- `EcommerceController` implementa `PUT /api/v1/ecommerces/{uid}/status`.
- Documentación publicada menciona `PUT /ecommerces/{uid}`.

Impacto: contratos API ambiguos para frontend/consumidores.

## 7) Estado QA (evidencia ejecutada)

## 7.1 Ejecución de pruebas
- Se intentó `mvnw.cmd` en ambos servicios y falló con:
  - `Cannot start maven from wrapper`.
- Se ejecutó `mvn -q test` fuera del sandbox para obtener resultado real.

### 7.2 Resultado `service-admin`
- Estado: **FAILED**
- Resumen:
  - `ConfigurationServiceTest`: 1 failure.
  - `ConfigurationMapperTest`: 2 failures.
  - `ConfigurationControllerTest`: 2 errors (fallo de contexto Spring).
- Evidencias principales:
  - Se espera `BadRequestException` pero ocurre `AuthorizationException`.
  - Se espera moneda uppercase (`COP`, `USD`) pero retorna lowercase (`cop`, `usd`).
  - Falla inyección en test slice: no bean `SecurityContextHelper` al levantar contexto.

### 7.3 Resultado `service-engine`
- Estado: **FAILED**
- Error de discovery JUnit:
  - `TestEngine with ID 'junit-jupiter' failed to discover tests`
  - `NoClassDefFoundError: ...application.dto.calculate.DiscountCalculateRequestV2`
- Causa probable: test(s) referencian paquete/clase antigua movida o renombrada.

### 7.4 Cobertura real
- Solo hay carpeta de tests en `service-admin` y evidencias previas para engine en `target/surefire-reports`.
- No hay suite visible de Karate/Serenity/k6 en este repo, aunque `TEST_PLAN.md` los define como estrategia.

## 8) Estado DevOps

### 8.1 CI/CD
- No existe carpeta `.github/workflows`.
- Sin pipeline de PR, sin quality gate automático, sin publicación de artefactos de test.

### 8.2 Build y empaquetado
- No se detectaron `Dockerfile` en backend.
- No hay definición de deploy por ambiente (dev/qa/prod) dentro del repositorio.

### 8.3 Observabilidad
- Logging configurado por consola.
- No se ve integración con stack centralizada (ELK/Datadog/Prometheus/Grafana/Sentry/OpenTelemetry).
- `actuator/health` está permitido en seguridad engine, pero no hay dependencia `spring-boot-starter-actuator` declarada.

## 9) Riesgos priorizados

### Críticos (P0)
- Sin CI/CD.
- Tests rotos en ambos servicios.
- Desalineación security mapping vs endpoint real en engine.
- Secretos hardcodeados.

### Altos (P1)
- Contratos API no totalmente consistentes con docs.
- Estrategia QA documentada no materializada en repositorio (Karate/Serenity/k6 ausentes).
- Wrapper Maven no funcional en entorno auditado.

### Medios (P2)
- Falta runbook formal de incidentes, respaldos, DLQ y recuperación.
- Falta estandarización de perfiles de configuración por entorno.

## 10) Plan de remediación recomendado (orden de ejecución)

### Fase 0 (24-48h)
- Corregir endpoint security en engine (`/api/v1/engine/calculate`).
- Corregir tests fallando en admin y engine hasta verde.
- Rotar secretos y mover a variables de entorno/secret manager.
- Alinear docs de endpoints con implementación real.

### Fase 1 (48-96h)
- Crear CI mínimo en GitHub Actions:
  - Build + test por servicio.
  - Reporte surefire.
  - Gate de merge.
- Agregar Dockerfiles para servicios.
- Definir estrategia de versionado/artefactos.

### Fase 2 (1-2 semanas)
- Incorporar pruebas faltantes (API contract + integración + smoke).
- Integrar observabilidad (healthchecks reales, métricas, trazas, logs estructurados).
- Runbooks operativos (incidentes, rollback, DLQ replay, backup/restore).

## 11) Checklist de transferencia (handover)

### Documentación
- [x] Inventario de componentes.
- [x] Dependencias y stack.
- [x] Endpoints principales.
- [x] Estado de pruebas y calidad.
- [x] Riesgos y plan de mitigación.
- [ ] Matriz de owners por módulo (falta definir responsables operativos).
- [ ] SLO/SLA y on-call (no encontrado en repo).

### Operación
- [x] Flujo de arranque local documentado.
- [x] Puertos y servicios base documentados.
- [x] Baseline operativo contrastado contra `Readme.md`.
- [ ] Comando oficial de arranque/build (`mvnw`) validado end-to-end.
- [ ] Proceso de despliegue por ambiente.
- [ ] Estrategia de rollback validada.
- [ ] Backup/restore probado.

### Calidad
- [x] Evidencia de ejecución de pruebas incluida.
- [ ] Pipeline de regresión automatizada implementada.
- [ ] Criterios de salida de QA cumplidos.

## 12) Comandos auditados (resumen)

- Inventario repo: `rg --files`, `Get-ChildItem -Recurse`.
- Configuración: lectura de `application.properties`, migraciones y controladores.
- QA:
  - `mvnw.cmd -q test` (falló wrapper).
  - `mvn -q test` (ejecutado fuera de sandbox para evidencia real).
- Evidencia de reportes:
  - `target/surefire-reports/*.txt`
  - dumps de surefire en engine.

---

## Conclusión de handover

La base funcional existe, pero la operación de entrega continua y la confiabilidad de pruebas están incompletas.  
Antes de transferir a operación estable o producción, el equipo debe cerrar P0/P1 de esta guía para evitar incidentes de seguridad, regresiones y bloqueos de release.
