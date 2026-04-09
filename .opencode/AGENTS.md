# AGENTS.md — ASDD Project (OpenCode)

> Canonical shared version: this file is the source of truth for shared agent guidelines.

Este archivo define la guía general para todos los agentes de IA que trabajan en este repositorio, siguiendo el flujo de trabajo **ASDD (Agent Spec Software Development)** con OpenCode.

## Project Summary

> Ver `README.md` en la raíz del proyecto para stack, arquitectura y estructura de carpetas.
> Ver `.opencode/README.md` para la estructura completa del framework ASDD.

## ASDD Workflow

**Cada nueva funcionalidad debe seguir este pipeline:**

```
[FASE 1 — Secuencial]
spec-generator    → /generate-spec      → .opencode/specs/<feature>.spec.md

[FASE 2 — Paralelo ∥]
database-agent    → modelos, migrations, seeders  (si hay cambios de DB)
backend-developer → capas del proyecto (routes/services/repos/models)
frontend-developer→ páginas / componentes / hooks / servicios

[FASE 3 — Paralelo ∥]
test-engineer-backend  → backend/src/test/
test-engineer-frontend → frontend/src/__tests__/

[FASE 4 — Secuencial]
qa-agent          → /gherkin-case-generator, /risk-identifier, …

[FASE 5 — Opcional]
documentation-agent → README, API docs, ADRs
```

## Agentes Disponibles (Invocación con @)

| Agente | Descripción | Cómo invocar |
|--------|-------------|---------------|
| `@orchestrator` | Orquesta el flujo completo ASDD | `@orchestrator` |
| `@spec-generator` | Genera especificaciones técnicas | `@spec-generator` |
| `@backend-developer` | Implementa backend Spring Boot | `@backend-developer` |
| `@frontend-developer` | Implementa frontend React | `@frontend-developer` |
| `@database-agent` | Diseña modelos y migraciones | `@database-agent` |
| `@test-engineer-backend` | Genera tests backend | `@test-engineer-backend` |
| `@test-engineer-frontend` | Genera tests frontend | `@test-engineer-frontend` |
| `@qa-agent` | Ejecuta QA (Gherkin, riesgos) | `@qa-agent` |
| `@documentation-agent` | Genera documentación | `@documentation-agent` |

## Skills (Slash Commands)

Los skills son conjuntos de instrucciones portables que se invocan como `/comando`:

### ASDD Core
| Skill | Comando | Descripción |
|-------|---------|-------------|
| asdd-orchestrate | `/asdd-orchestrate` | Orquesta el flujo completo ASDD o consulta estado |
| generate-spec | `/generate-spec` | Genera spec técnica en `.opencode/specs/` |
| implement-backend | `/implement-backend` | Implementa feature completo en el backend |
| implement-frontend | `/implement-frontend` | Implementa feature completo en el frontend |
| unit-testing | `/unit-testing` | Genera suite de tests (backend + frontend) |

### QA
| Skill | Comando | Descripción |
|-------|---------|-------------|
| gherkin-case-generator | `/gherkin-case-generator` | Genera casos Given-When-Then + datos de prueba |
| risk-identifier | `/risk-identifier` | Clasifica riesgos con Regla ASD (Alto/Medio/Bajo) |
| automation-flow-proposer | `/automation-flow-proposer` | Propone flujos a automatizar y framework |
| performance-analyzer | `/performance-analyzer` | Planifica y analiza pruebas de performance |

## Lineamientos y Contexto

Los agentes deben cargar estos archivos como **primer paso** antes de generar cualquier código:

| Documento | Ruta | Agentes que lo cargan |
|---|---|---|
| Lineamientos de Desarrollo | `.opencode/docs/guidelines/dev-guidelines.md` | Backend Developer, Frontend Developer, Database Agent |
| Lineamientos QA | `.opencode/docs/guidelines/qa-guidelines.md` | Test Engineer Backend, Test Engineer Frontend, QA Agent |
| Technical Architecture | `.opencode/docs/guidelines/technical-architecture.md` | Backend Developer, Database Agent |
| Reglas de Oro | `.opencode/AGENTS.md` | Todos (siempre activas) |
| Stack y restricciones | `.opencode/instructions/backend.instructions.md` | Backend Developer, Frontend Developer, Database Agent, Spec Generator |
| Arquitectura | `.opencode/instructions/backend.instructions.md` | Backend Developer, Frontend Developer, Spec Generator |
| Instrucciones Tests | `.opencode/instructions/tests.instructions.md` | Test Engineer Backend, Test Engineer Frontend |

---

## Reglas de Oro

> Principio rector: todas las contribuciones de la IA deben ser seguras, transparentes, con propósito definido y alineadas con las instrucciones explícitas del usuario.

### I. Integridad del Código y del Sistema
- **No código no autorizado**: no escribir, generar ni sugerir código nuevo a menos que el usuario lo solicite explícitamente.
- **No modificaciones no autorizadas**: no modificar, refactorizar ni eliminar código, archivos o estructuras existentes sin aprobación explícita del usuario.
- **Preservar la lógica existente**: respetar patrones arquitectónicos, estilo de codificación y lógica operativa del proyecto.

### II. Clarificación de Requisitos
- **Clarificación obligatoria**: si la solicitud es ambigua, incompleta o poco clara, detenerse y solicitar clarificación antes de proceder.
- **No realizar suposiciones**: basar todas las acciones estrictamente en información explícita proporcionada por el usuario.

### III. Transparencia Operativa
- **Explicar antes de actuar**: antes de cualquier acción, explicar qué se va a hacer y posibles implicaciones.
- **Detención ante la incertidumbre**: si surge inseguridad o un conflicto con estas reglas, detenerse y consultar al usuario.
- **Acciones orientadas a un propósito**: cada acción debe ser directamente relevante para la solicitud explícita.

---

## Entradas al Pipeline ASDD

| Tipo | Directorio | Descripción |
|------|-----------|-------------|
| Requerimientos de negocio | `.opencode/requirements/` | Input: descripción funcional del feature |
| Especificaciones técnicas | `.opencode/specs/` | Output del Spec Generator, fuente de verdad para implementación |

## Critical Rules for All Agents

1. **No implementation without a spec.** Always check `.opencode/specs/` first.
2. **Backend architecture is layered** — follow the pattern defined in `.opencode/instructions/backend.instructions.md`. Never bypass layers.
3. **Dependency wiring happens at the entry layer** (controller/router) — inject dependencies downward, never upward.
4. **UI state follows the project architecture** — use a single authoritative source of truth; no parallel state sources.
5. **I/O operations follow the project concurrency model** — sync or async as defined in `.opencode/instructions/backend.instructions.md`.
6. **Never commit secrets or credentials** — `.env`, credential files and API keys must be in `.gitignore`.

## Development Commands & Integration Notes

> Ver `README.md` en la raíz del proyecto.