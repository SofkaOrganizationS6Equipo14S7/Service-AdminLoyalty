# Diagrama ER - Service Engine

```mermaid
erDiagram
    %% Auth & Tenant Context
    api_key_replica {
        uuid id PK
        varchar hashed_key UK
        uuid ecommerce_id FK
        boolean is_active
    }

    %% Configuration Snapshots (Read-Only for Engine)
    discount_setting_replica {
        uuid id PK
        uuid ecommerce_id FK
        decimal max_discount_cap
        varchar currency_code
        varchar rounding_rule
    }

    discount_priority_replica {
        uuid id PK
        uuid discount_setting_id FK
        varchar discount_type
        int priority_level
    }

    %% Rules Engine (Read-Only for Engine)
    seasonal_rule_replica {
        uuid id PK
        uuid ecommerce_id FK
        numeric discount_percentage
        timestamp start_date
        timestamp end_date
        boolean is_active
    }

    product_rule_replica {
        uuid id PK
        uuid ecommerce_id FK
        varchar product_type_key
        numeric discount_percentage
        boolean is_active
    }

    fidelity_range_replica {
        uuid id PK
        uuid ecommerce_id FK
        int min_points
        int max_points
        numeric discount_percentage
        boolean is_active
    }

    %% Customer Classification (Read-Only for Engine)
    customer_tier_replica {
        uuid id PK
        varchar name
        int level
        boolean is_active
    }

    classification_rule_replica {
        uuid id PK
        uuid customer_tier_id FK
        varchar metric_type
        numeric min_value
        numeric max_value
        int priority
        boolean is_active
    }

    %% Transactional Output (Write-Only for Engine)
    applied_discount_log {
        uuid id PK
        uuid ecommerce_id FK
        varchar request_id UK
        decimal original_total
        decimal calculated_discount
        decimal final_total
        jsonb logic_breakdown "Detalle de qué reglas aplicaron"
        timestamp processed_at
    }

    discount_setting_replica ||--o{ discount_priority_replica : "defines_order"
    api_key_replica ||--o{ applied_discount_log : "authorizes"
    customer_tier_replica ||--o{ classification_rule_replica : "governs"
```

---

## Resumen de Tablas

| Tabla | Tipo | Descripción |
|-------|------|-------------|
| `api_key_replica` | Read | Réplica de API keys (auth) |
| `discount_setting_replica` | Read | Réplica de configuración de descuentos |
| `discount_priority_replica` | Read | Réplica de prioridades de descuento |
| `seasonal_rule_replica` | Read | Réplica de reglas de temporada |
| `product_rule_replica` | Read | Réplica de reglas por producto |
| `fidelity_range_replica` | Read | Réplica de rangos de fidelidad |
| `customer_tier_replica` | Read | Réplica de niveles de cliente |
| `classification_rule_replica` | Read | Réplica de reglas de clasificación |
| `applied_discount_log` | Write | Log de descuentos aplicados (transaccional) |