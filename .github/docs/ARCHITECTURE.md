# Guía de Conexiones del Framework ASDD

Este documento explica cómo se relacionan los archivos del `.github/` entre sí.

---

## Mapa de Archivos

```
.github/
│
├── AGENTS.md                    ← Reglas de oro (siempre activas)
├── copilot-instructions.md      ← Instrucciones para Copilot Chat
│
├── agents/                     ← Definición de agentes (@nombre)
│   ├── orchestrator.agent.md
│   ├── backend-developer.agent.md
│   └── ...
│
├── skills/                     ← Comandos slash (/comando)
│   ├── asdd-orchestrate/
│   │   └── SKILL.md
│   ├── generate-spec/
│   │   └── SKILL.md
│   └── ...
│
├── instructions/               ← Instrucciones por tipo de archivo
│   ├── backend.instructions.md
│   └── ...
│
├── docs/guidelines/           ← Lineamientos y guías técnicas
│   ├── dev-guidelines.md
│   ├── qa-guidelines.md
│   └── technical-architecture.md
│
├── requirements/              ← Requerimientos de negocio (input)
│   └── hu-03-api-keys.md
│
└── specs/                    ← Especificaciones técnicas (output)
    └── hu-03-api-keys.spec.md (si se generó)
```

---

## Flujo de Carga de Archivos

### 1. Copilot Chat Activo

Cuando usás Copilot Chat, se carga automáticamente:

```
copilot-instructions.md  ← Siempre activo
         ↓
AGENTS.md               ← Reglas de oro siempre visibles
```

### 2. Al invocar un Agente

Ejemplo: `@Backend Developer`

```
backend-developer.agent.md  → Define el rol y herramientas
         ↓
backend.instructions.md     → Se aplica por el patrón "applyTo"
         ↓
dev-guidelines.md          → Lineamientos de desarrollo
         ↓
requirements/              → HU del proyecto
```

### 3. Al usar un Skill

Ejemplo: `/generate-spec`

```
skills/generate-spec/SKILL.md  → Define cómo ejecutar el skill
         ↓
agents/spec-generator.agent.md  → Agente que ejecuta
         ↓
requirements/                 → Requerimiento base
```

---

## Relación Agente ↔ Skill

| Agente | Skill | Comando |
|--------|-------|---------|
| Orchestrator | asdd-orchestrate | `/asdd-orchestrate` |
| Spec Generator | generate-spec | `/generate-spec` |
| Backend Developer | implement-backend | `/implement-backend` |
| QA Agent | gherkin-case-generator | `/gherkin-case-generator` |

---

## applyTo (Instrucciones por Archivo)

El campo `applyTo` en `instructions/` determina qué instrucción se aplica según el archivo activo:

```yaml
# backend.instructions.md
applyTo: "**/*.java"
```

Cuando editás un archivo `.java`, se inyecta automáticamente el contenido de `backend.instructions.md`.

---

## Entrada → Salida del Pipeline

```
requirements/              → [Spec Generator] → specs/
     (input)                                      (output)
```

```
requirements/hu-03-api-keys.md
         ↓
/generate-spec "hu-03"
         ↓
specs/hu-03-api-keys.spec.md  (DRAFT)
         ↓
Usuario aprueba (APPROVED)
         ↓
@Backend Developer implementa
```

---

## Resumen de Carga

| Acción | Archivos que se cargan |
|--------|------------------------|
| Copilot Chat abierto | `copilot-instructions.md`, `AGENTS.md` |
| `@Backend Developer` | `backend-developer.agent.md`, `backend.instructions.md`, `dev-guidelines.md` |
| `@QA Agent` | `qa.agent.md`, `qa-guidelines.md` |
| `/generate-spec` | `spec-generator.agent.md`, `skills/generate-spec/SKILL.md` |
| Editar `.java` | `backend.instructions.md` (inyectado) |
