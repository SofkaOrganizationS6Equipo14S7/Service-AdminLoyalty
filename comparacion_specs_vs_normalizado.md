# Comparación: Specs vs Modelo Normalizado

## 1. USERS

| Aspecto | Spec (login-logout.spec.md) | Modelo Normalizado | Estado |
|---------|----------------------------|---------------------|--------|
| Tabla | `users` | `user` | ⚠️ Nombre en singular |
| PK | `id` (Long auto-increment) | `id` (UUID) | ❌ Diferente |
| Second unique ID | `uid` (UUID) | No tiene (usa solo UUID) | ❌ Falta en spec |
| FK ecommerce | No tiene | `ecommerce_id` FK | ❌ Falta en spec |
| FK role | `role` (VARCHAR) | `role_id` FK | ❌ No normalizado |
| Campo email | No tiene | `email` (VARCHAR) | ❌ Falta en spec |
| Campo last_login | No tiene | `last_login` | ❌ Falta en spec |

---

## 2. ECOMMERCE

| Aspecto | Spec (ecommerce-onboarding.spec.md) | Modelo Normalizado | Estado |
|---------|-------------------------------------|---------------------|--------|
| Tabla | `ecommerces` | `ecommerce` | ⚠️ Nombre singular |
| PK | `uid` (UUID) | `id` (UUID) | ⚠️ Cambia uid→id |
| Campos | uid, name, slug, status, created_at, updated_at | id, name, slug, status, created_at, updated_at | ✅ Compatible |

---

## 3. SEASONAL_RULES

| Aspecto | Spec (seasonal-rules.spec.md) | Modelo Normalizado | Estado |
|---------|-------------------------------|---------------------|--------|
| Tabla | `seasonal_rules` | `seasonal_rule` | ⚠️ Singular |
| PK | `uid` | `id` | ⚠️ uid→id |
| FK ecommerce | `ecommerce_id` | `ecommerce_id` | ✅ |
| Campos | uid, ecommerce_id, name, description, discount_percentage, discount_type, start_date, end_date, is_active, created_at, updated_at | id, ecommerce_id, name, description, discount_percentage, discount_type, start_date, end_date, is_active, created_at, updated_at | ✅ |

---

## 4. PRODUCT_RULES

| Aspecto | Spec (product-rules.spec.md) | Modelo Normalizado | Estado |
|---------|------------------------------|---------------------|--------|
| Tabla | `product_rules` | `product_rule` | ⚠️ Singular |
| PK | `uid` | `id` | ⚠️ uid→id |
| FK ecommerce | `ecommerce_id` | `ecommerce_id` | ✅ |
| Unique | `(ecommerce_id, product_type, is_active)` | `(ecommerce_id, product_type, is_active)` | ✅ |
| Campos | uid, ecommerce_id, name, product_type, discount_percentage, benefit, is_active, created_at, updated_at | id, ecommerce_id, name, product_type, discount_percentage, benefit, is_active, created_at, updated_at | ✅ |

---

## 5. FIDELITY_RANGES

| Aspecto | Spec (fidelity-ranges.spec.md) | Modelo Normalizado | Estado |
|---------|--------------------------------|---------------------|--------|
| Tabla | `fidelity_ranges` | `fidelity_range` | ⚠️ Singular |
| PK | `uid` | `id` | ⚠️ uid→id |
| FK ecommerce | `ecommerce_id` | `ecommerce_id` | ✅ |
| Campos | uid, ecommerce_id, name, min_points, max_points, discount_percentage, is_active, created_at, updated_at | id, ecommerce_id, name, min_points, max_points, discount_percentage, is_active, created_at, updated_at | ✅ |

---

## 6. DISCOUNT_CONFIG / DISCOUNT_SETTING

| Aspecto | Spec (discount-limit.spec.md) | Modelo Normalizado | Estado |
|---------|-------------------------------|---------------------|--------|
| Tabla | `discount_config` | `discount_setting` | ❌ Renombrado |
| PK | `uid` | `id` | ⚠️ uid→id |
| FK ecommerce | `ecommerceId` | `ecommerce_id` | ⚠️ CamelCase |
| Campo | `maxDiscountLimit` | `max_discount_cap` | ❌ Renombrado |
| Campo | `currencyCode` | `currency_code` | ⚠️ CamelCase |
| Campo | `isActive` | No tiene (usa soft delete) | ⚠️ Diferente |
| Campo | No tiene | `rounding_rule` | ❌ Falta |
| Tabla hija | `discount_priority` | `discount_priority` | ✅ |

---

## 7. CUSTOMER_TIERS / CLASSIFICATION_RULES

| Aspecto | Spec (customer-classification.spec.md) | Modelo Normalizado | Estado |
|---------|------------------------------------------|---------------------|--------|
| Tabla tiers | `customer_tiers` | `customer_tier` | ⚠️ Singular |
| PK tiers | `uid` | `id` | ⚠️ uid→id |
| Campos tiers | uid, name, level, is_active, created_at, updated_at | id, name, level, is_active, created_at, updated_at | ✅ |
| Tabla rules | `classification_rules` | `classification_rule` | ⚠️ Singular |
| PK rules | `uid` | `id` | ⚠️ uid→id |
| FK tier | `customer_tier_uid` | `customer_tier_id` | ⚠️ uid→id |
| Campos rules | uid, customer_tier_uid, min_total_spent, max_total_spent, min_order_count, max_order_count, metric_type, priority, is_active, created_at, updated_at | id, customer_tier_id, metric_type, min_value, max_value, priority, is_active, created_at, updated_at | ❌ Campos renombrados |

---

## 8. PERMISOS (No hay spec específico)

| Aspecto | Migración actual | Modelo Normalizado | Estado |
|---------|------------------|---------------------|--------|
| Tabla permisos | `permissions` | `permission` | ⚠️ Singular |
| Tabla roles | No existe como tabla | `role` | ❌ Falta |
| Relación | `role_permissions` | `role_permission` | ⚠️ Singular |
| FK users | `role` (VARCHAR) | `role_id` FK | ❌ No normalizado |

---

## 9. AUDIT_LOG

| Aspecto | Spec (audit-log.spec.md) | Modelo Normalizado | Estado |
|---------|-------------------------|---------------------|--------|
| Tabla | `audit_logs` | `audit_log` | ⚠️ Plural |
| PK | No especifica | `id` (UUID) | ⚠️ |
| FK users | Especifica `user_uid` y `actor_uid` | `user_id` | ⚠️ Diferente |
| Campos | id, user_uid, action, description, actor_uid, created_at | id, user_id, action, target_entity, change_data (JSONB), created_at | ❌ Estructura diferente |

---

## 10. TABLAS FALTANTES (No existen en specs actuales)

| Tabla | Descripción | Existe en spec? |
|-------|-------------|------------------|
| `discount_application_log` | Log de descuentos aplicados | ❌ NO |
| `discount_usage_history` | Historial de uso de descuentos | ❌ NO |

---

## RESUMEN DE CORRECCIONES REQUERIDAS

| # | Problema | Cantidad |
|---|----------|----------|
| 1 | PK cambia de `uid` a `id` (UUID) | ~12 tablas |
| 2 | Nombre de tablas singular | ~12 tablas |
| 3 | Users: agregar FK a role y ecommerce | 1 tabla |
| 4 | Users:role normalizado a tabla | 1 tabla + 1 nueva |
| 5 | discount_config → discount_setting | 1 tabla |
| 6 | classification_rules: renombrar campos | 1 tabla |
| 7 | Agregar tablas de logging/historial | 2 tablas |
| 8 | audit_log: restructure | 1 tabla |
