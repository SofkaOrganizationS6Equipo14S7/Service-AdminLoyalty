---
description: 'Ejecuta el Database Agent para diseñar esquemas de datos, generar scripts de migración Flyway, y optimizar queries a partir de la spec aprobada.'
agent: Database Agent
---

Ejecuta el Database Agent para diseñar y gestionar el modelo de persistencia del feature.

**Feature**: ${input:featureName:nombre del feature en kebab-case}

**Instrucciones para @Database Agent:**

1. Lee `.github/instructions/backend.instructions.md` — confirma el motor de BD aprobado (PostgreSQL).
2. Lee la **Sección 2 — DISEÑO — Modelos de Datos** de `.github/specs/${input:featureName}.spec.md`
3. Escanea entidades y repositorios existentes en `backend/src/main/java/com/loyalty/entity/` y `backend/src/main/java/com/loyalty/repository/`
4. Ejecuta el flujo completo:
   - Diseña o actualiza el esquema de datos (entidades, campos, índices)
   - Genera migración Flyway: `backend/src/main/resources/db/migration/V1__create_<feature>.sql`
   - Registra ADR si hay decisiones de diseño relevantes
5. Presenta reporte consolidado de cambios al modelo de datos

**Prerequisito:** Debe existir `.github/specs/${input:featureName}.spec.md` con estado APPROVED y Sección 2 completa. Si no, ejecutar `/generate-spec` primero.

**Nota:** Ejecutar ANTES o en paralelo con el Backend Developer para que los contratos de persistencia estén definidos antes de implementar los repositorios.
