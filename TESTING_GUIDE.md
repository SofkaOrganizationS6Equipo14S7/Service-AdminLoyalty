# 🧪 Guía de Prueba - Endpoints Discount-Limit

## Problema Identificado y Solucionado

**Síntoma**: Con API Key válida retornaba 403 Forbidden  
**Causa**: El `ApiKeyAuthenticationFilter` validaba la key pero NO creaba un `Authentication` token en el `SecurityContext`  
**Solución**: ✅ El filter ahora crea `UsernamePasswordAuthenticationToken` y lo setea en `SecurityContextHolder`

---

## 📋 Flujo de Seguridad CORREGIDO

```
Request con Authorization: Bearer {API_KEY}
    ↓
ApiKeyAuthenticationFilter
    ├─ Valida formato Bearer ✓
    ├─ Busca key en caché ✓
    └─ Si válida → Crea Authentication + setea en SecurityContext ✓
    ↓
SecurityConfig.authorizeHttpRequests()
    ├─ Ve que hay Authentication en contexto ✓
    └─ Permite continuar ✓
    ↓
DiscountConfigController/Endpoint
    └─ Procesa request normalmente ✓
```

---

## 🚀 Cómo Probar

### **Paso 1: Obtener API Key válida**

1. Ir a **service-admin** (puerto 8080)
2. Login con credenciales:
   ```bash
   POST http://localhost:8080/api/v1/auth/login
   {
     "username": "admin",
     "password": "admin123"
   }
   ```
3. Recibirás un **JWT Token**
4. Usar ese token para obtener/crear una API Key

### **Paso 2: Crear una API Key** (en service-admin)

```bash
POST http://localhost:8080/api/v1/api-keys
Authorization: Bearer {JWT_TOKEN}
X-Ecommerce-ID: {ECOMMERCE_UUID}

Response 201:
{
  "uid": "550e8400-e29b-41d4-a716-446655440000",
  "keyString": "sk_test_abc123...",  ← COPIAR ESTO
  "createdAt": "2026-03-26T..."
}
```

### **Paso 3: Probar Endpoints de Discount** (en service-engine, puerto 8081)

#### **3a. Configurar límite máximo**

```bash
POST http://localhost:8081/api/v1/discount/config
Authorization: Bearer sk_test_abc123...
Content-Type: application/json

{
  "maxDiscountLimit": 100.00,
  "currencyCode": "USD"
}

✅ Response 201 Created
{
  "uid": "uuid-xxx",
  "maxDiscountLimit": 100.00,
  "currencyCode": "USD",
  "isActive": true,
  "createdAt": "2026-03-26T..."
}
```

#### **3b. Obtener configuración vigente**

```bash
GET http://localhost:8081/api/v1/discount/config
Authorization: Bearer sk_test_abc123...

✅ Response 200 OK
{
  "uid": "uuid-xxx",
  "maxDiscountLimit": 100.00,
  ...
}
```

#### **3c. Configurar prioridad de descuentos**

```bash
POST http://localhost:8081/api/v1/discount/priority
Authorization: Bearer sk_test_abc123...

{
  "discount_config_id": "uuid-xxx",
  "priorities": [
    {
      "discount_type": "LOYALTY_POINTS",
      "priority_level": 1
    },
    {
      "discount_type": "COUPON",
      "priority_level": 2
    }
  ]
}

✅ Response 201 Created
```

#### **3d. Obtener prioridades**

```bash
GET http://localhost:8081/api/v1/discount/priority
Authorization: Bearer sk_test_abc123...

✅ Response 200 OK
```

#### **3e. Calcular descuentos**

```bash
POST http://localhost:8081/api/v1/discount/calculate
Authorization: Bearer sk_test_abc123...

{
  "transaction_id": "txn-12345",
  "discounts": [
    {
      "discount_type": "LOYALTY_POINTS",
      "amount": 50.00
    },
    {
      "discount_type": "COUPON",
      "amount": 40.00
    }
  ]
}

✅ Response 200 OK
{
  "transaction_id": "txn-12345",
  "original_discounts": [
    { "discount_type": "LOYALTY_POINTS", "amount": 50.00 },
    { "discount_type": "COUPON", "amount": 40.00 }
  ],
  "applied_discounts": [
    { "discount_type": "LOYALTY_POINTS", "amount": 50.00 },
    { "discount_type": "COUPON", "amount": 40.00 }
  ],
  "total_original": 90.00,
  "total_applied": 90.00,
  "max_discount_limit": 100.00,
  "limit_exceeded": false,
  "calculated_at": "2026-03-26T..."
}
```

---

## 🔍 Códigos de Respuesta Esperados

| Caso | Código | Descripción |
|------|--------|-------------|
| ✅ Request válido | **200** | GET exitoso |
| ✅ Creación exitosa | **201** | POST exitoso |
| ❌ Sin Authorization header | **401** | "Header Authorization requerido" |
| ❌ API Key inválida | **401** | "API Key inválida o expirada" |
| ❌ Datos inválidos | **400** | "Validation failed: ..." |
| ❌ Config no existe | **404** | "discount_config_not_found" |
| ❌ **[ANTES]** API Key válida | **403** | ❌ BUG (ya corregido) |

---

## 📌 Notas Importantes

1. **API Key ≠ JWT Token**
   - Service-admin maneja **JWT Tokens**
   - Service-engine espera **API Keys** (en Bearer)
   - Son dos sistemas de autenticación diferentes

2. **Puertos**
   - service-admin: `8080`
   - service-engine: `8081`

3. **Format Authorization Header**
   - ✅ Correcto: `Authorization: Bearer sk_test_abc123...`
   - ❌ Incorrecto: `Authorization: sk_test_abc123...`
   - ❌ Incorrecto: `Authorization: Bearer eyJhbGc...` (JWT)

4. **Si sigue dando 403**
   - Verifica en logs que dice `Authentication set in SecurityContext`
   - Si no aparece, la key no se reconoce como válida
   - Revisa que la key está en la caché de service-engine

---

## 🐛 Debugging

### Ver logs detallados
```bash
# En cada terminal, ver logs en tiempo real
mvn spring-boot:run -Dspring-boot.run.arguments="--logging.level.com.loyalty=DEBUG"
```

### Verificar que filter se ejecuta
```
[DEBUG] API Key validated successfully from: 127.0.0.1
[DEBUG] Authentication set in SecurityContext for API Key validation
```

### Si ves estos errores
```
[WARN] Request without Authorization header
       → Agregar Authorization header
       
[WARN] Invalid Authorization header format
       → Cambiar "Bearer " al inicio
       
[WARN] Invalid or expired API Key
       → Crear nueva API Key
```

