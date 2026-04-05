---
name: performance-analyzer
description: Analiza y define estrategias de performance testing. Clasifica pruebas en Load, Stress, Spike y Soak. Define SLAs, umbrales de alerta y el plan de ejecución con k6.
argument-hint: "<nombre-feature | nombre-proyecto>"
---

# Skill: performance-analyzer [QA]

Define y planifica pruebas de performance basadas en los SLAs del SPEC.

## Tipos de pruebas

### Load Testing (Carga normal)
```
Propósito:  Comportamiento bajo la carga esperada en producción
Cuándo:     Antes de cada release con cambios en APIs de alto tráfico
Duración:   15-30 minutos
Patrón k6:  0 → 100 VUs (2min) → 100 VUs sostenido (15min) → 0 (2min)
```

### Stress Testing (Carga máxima)
```
Propósito:  Encontrar el punto de quiebre y validar degradación graceful
Cuándo:     Antes de lanzamientos importantes
Duración:   30-60 minutos
```

### Spike Testing (Picos repentinos)
```
Propósito:  Verificar recuperación ante picos abruptos de tráfico
Cuándo:     Eventos programados
Duración:   20-30 minutos
```

### Soak Testing (Resistencia)
```
Propósito:  Detectar memory leaks, connection pool exhaustion
Cuándo:     Antes de releases mayores
Duración:   2-4 horas mínimo
```

## Umbrales SLA base

```javascript
export const thresholds = {
  'http_req_duration': [
    'p(50) < 200ms',
    'p(95) < 1000ms',
    'p(99) < 2000ms',
  ],
  'http_req_failed': ['rate < 0.01'],
  'http_reqs': ['rate > 100'],
};
```

## Entregable: `performance-plan.md`

Genera en `docs/output/qa/performance-plan.md`:

```markdown
# Plan de Performance — [Proyecto]

## SLAs Definidos
| Métrica    | Objetivo   | Mínimo Aceptable |
|------------|------------|------------------|
| P95        | < 500ms    | < 1000ms         |
| TPS        | > 200      | > 100            |
| Error rate | < 0.1%     | < 1%             |

## Pruebas Planificadas
| Tipo  | Endpoint(s) | VUs    | Duración |
|-------|------------|--------|----------|
| Load  | POST /faq  | 100    | 20 min   |
| Soak  | GET /faq   | 50     | 2 horas  |

## Ambiente y Datos
- Ambiente: staging (NO producción)
- Datos: sintéticos
```

## Reglas

- Línea base de performance OBLIGATORIA en pipeline CI
- Duración mínima de soak: 120 minutos