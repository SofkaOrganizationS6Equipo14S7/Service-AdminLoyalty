# 📊 Endpoints Implementados - Discount-Limit

## 🏗️ Arquitectura de Servicios

```
┌─────────────────────────────────────────────────────────────┐
│                    CLIENT / POSTMAN / FRONTEND              │
└────────────────────────┬──────────────────────────────────────┘
                        │
                        ├─ JWT Token ──→ service-admin:8080
                        │                  (Auth, API Keys)
                        │
                        └─ API Key ────→ service-engine:8081
                                        (Discounts)
```

---

## 🔐 Flujo de Autenticación CORREGIDO

```
1. Client envía: Authorization: Bearer {API_KEY}
                 ↓
2. ApiKeyAuthenticationFilter (service-engine)
   ├─ Valida formato Bearer ✓
   ├─ Busca key en caché Caffeine ✓
   └─ Crea Authentication + SecurityContext ✓  ← FIXED
                 ↓
3. SecurityConfig
   └─ Verifica que Authentication existe ✓
                 ↓
4. Spring permite continuar → Controller procesa ✓
```

---

## 📍 Endpoints por Servicio

### **Service-Admin (8080)** - Authentication & API Keys
```
POST   /api/v1/auth/login          → Obtener JWT Token
GET    /api/v1/auth/me             → Usuario actual  
POST   /api/v1/auth/logout         → Cerrar sesión

POST   /api/v1/api-keys            → Crear API Key (requiere JWT)
GET    /api/v1/api-keys/{ecomId}   → Listar API Keys (requiere JWT)
DELETE /api/v1/api-keys/{keyId}    → Eliminar API Key (requiere JWT)
```

### **Service-Engine (8081)** - Discount Configuration & Calculation
```
┌─── DISCOUNT CONFIG ───────────────────────────┐
│ POST   /api/v1/discount/config                │
│ GET    /api/v1/discount/config                │
└───────────────────────────────────────────────┘

┌─── DISCOUNT PRIORITY ─────────────────────────┐
│ POST   /api/v1/discount/priority              │
│ GET    /api/v1/discount/priority              │
└───────────────────────────────────────────────┘

┌─── DISCOUNT CALCULATION ──────────────────────┐
│ POST   /api/v1/discount/calculate             │
└───────────────────────────────────────────────┘

❗ TODOS REQUIEREN: Authorization: Bearer {API_KEY}
```

---

## 🧪 Ejemplo Completo de Flujo

### **1. Login en service-admin (obtener JWT)**
```bash
$ curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'

Response:
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tipo": "Bearer",
  "username": "admin",
  "role": "ADMIN"
}
```

### **2. Crear API Key en service-admin (usar JWT)**
```bash
$ curl -X POST http://localhost:8080/api/v1/api-keys \
  -H "Authorization: Bearer eyJhbGc..." \
  -H "X-Ecommerce-ID: 220e8400-e29b-41d4-a716-446655440111" \
  -H "Content-Type: application/json"

Response:
{
  "uid": "550e8400-e29b-41d4-a716-446655440000",
  "keyString": "sk_test_1234567890abcdef",
  "createdAt": "2026-03-26T..."
}
```

### **3. Usar API Key en service-engine (configurar discuentos)**
```bash
$ curl -X POST http://localhost:8081/api/v1/discount/config \
  -H "Authorization: Bearer sk_test_1234567890abcdef" \
  -H "Content-Type: application/json" \
  -d '{
    "maxDiscountLimit": 100.00,
    "currencyCode": "USD"
  }'

Response: 201 Created ✅
{
  "uid": "uuid-xxx",
  "maxDiscountLimit": 100.00,
  "currencyCode": "USD",
  "isActive": true
}
```

### **4. Calcular descuentos respetando límite**
```bash
$ curl -X POST http://localhost:8081/api/v1/discount/calculate \
  -H "Authorization: Bearer sk_test_1234567890abcdef" \
  -H "Content-Type: application/json" \
  -d '{
    "transaction_id": "txn-001",
    "discounts": [
      { "discount_type": "LOYALTY_POINTS", "amount": 50.00 },
      { "discount_type": "COUPON", "amount": 40.00 },
      { "discount_type": "BIRTHDAY", "amount": 30.00 }
    ]
  }'

Response: 200 OK ✅
{
  "transaction_id": "txn-001",
  "original_discounts": [
    { "discount_type": "LOYALTY_POINTS", "amount": 50.00 },
    { "discount_type": "COUPON", "amount": 40.00 },
    { "discount_type": "BIRTHDAY", "amount": 30.00 }
  ],
  "applied_discounts": [
    { "discount_type": "LOYALTY_POINTS", "amount": 50.00 },  ← Priority 1
    { "discount_type": "COUPON", "amount": 40.00 },          ← Priority 2
    { "discount_type": "BIRTHDAY", "amount": 10.00 }         ← Priority 3, capped at limit
  ],
  "total_original": 120.00,
  "total_applied": 100.00,
  "max_discount_limit": 100.00,
  "limit_exceeded": true,
  "calculated_at": "2026-03-26T..."
}
```

---

## ✅ Fixed Issues

| Problema | Antes | Después | Causa |
|----------|-------|---------|-------|
| 403 Forbidden con API Key válida | ❌ 403 | ✅ 200/201 | Filter ahora crea `Authentication` en `SecurityContext` |
| 401 sin Authorization header | ❌ 401 | ✅ 401 | Correcto: rechaza sin credenciales |
| 401 con API Key inválida | ❌ 401 | ✅ 401 | Correcto: rechaza key inválida |

---

## 🔄 Request/Response Cycle Detallado

```
Client Request
├─ Header: Authorization: Bearer sk_test_...
├─ URL: /api/v1/discount/config
└─ Method: POST

        ↓ (Intercepted by OncePerRequestFilter)

ApiKeyAuthenticationFilter.doFilterInternal()
├─ Extracts: "sk_test_..."
├─ Validates against Caffeine cache ✓
├─ Creates: new UsernamePasswordAuthenticationToken(apiKey, null, authorities)
├─ Sets: SecurityContextHolder.getContext().setAuthentication(auth)  ← KEY FIX
└─ Forwards: filterChain.doFilter() ✓

        ↓

SecurityConfig.securityFilterChain()
├─ Checks: .anyRequest().authenticated()
├─ Finds: SecurityContextHolder.getContext().getAuthentication() != null ✓
└─ Result: ALLOW ✓

        ↓

DiscountConfigController.updateDiscountConfig()
├─ Receives: @RequestBody DiscountConfigCreateRequest
├─ Invokes: discountConfigService.updateConfig()
├─ Publishes: RabbitMQ event (DiscountConfigUpdated)
├─ Creates: New discount config record
└─ Returns: 201 Created ✓

        ↓

Client Response
├─ Status: 201 Created
├─ Body: { "uid": "...", "maxDiscountLimit": ... }
└─ Success: ✅
```

---

## 📝 Headers Requeridos

```
Authorization: Bearer {API_KEY}     ← OBLIGATORIO en service-engine
Content-Type: application/json      ← Para POST/PUT
X-User-ID: {UUID}                   ← Algunas operaciones (admin)
X-Ecommerce-ID: {UUID}              ← Para crear API Keys (admin)
```

---

## 🚨 Códigos de Error

```
401 Unauthorized
├─ "Header Authorization requerido"          → Falta Authorization header
├─ "Formato de Authorization inválido"       → No comienza con "Bearer "
└─ "API Key inválida o expirada"             → Key no existe en caché

400 Bad Request
├─ "max_discount_limit must be greater than 0"       → Config inválida
└─ "priority_sequence_invalid: must be sequential"   → Prioridades inválidas

404 Not Found
├─ "discount_config_not_found"       → No existe config activa
└─ "discount_priority_not_found"     → No existen prioridades

500 Internal Server Error
├─ RabbitMQ offline (no impacta BD, solo cache)
└─ Database connection failure
```

---

## 📦 Carga Máxima de Discuentos

```
POST /api/v1/discount/calculate
Config: maxDiscountLimit = 100.00

Request con 3 descuentos (total 250.00):
├─ LOYALTY_POINTS: 50.00 (priority 1)  ✓ Se aplica
├─ COUPON: 40.00 (priority 2)          ✓ Se aplica
├─ BIRTHDAY: 30.00 (priority 3)        ⚠️ Solo 10.00 se aplica
└─ SEASONAL: 130.00 (priority 4)       ✗ No se aplica (limite alcanzado)

Response:
{
  "total_original": 250.00,
  "total_applied": 100.00,
  "limit_exceeded": true
}
```

