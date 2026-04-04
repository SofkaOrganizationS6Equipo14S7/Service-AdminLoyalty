# ENDPOINTS SERVICE-ADMIN (Postman)

**Base URL:** `http://localhost:8081/api/v1`
**Puerto:** 8081

---

## AUTH

| Método | Endpoint | Body |
|--------|----------|------|
| POST | /auth/login | `{"username":"","password":""}` |
| POST | /auth/logout | (sin body, requiere header Authorization) |
| GET | /auth/me | (sin body, requiere header Authorization) |

---

## USERS

| Método | Endpoint | Body |
|--------|----------|------|
| POST | /users | `{"username":"","email":"","password":"","roleId":"uuid","ecommerceId":"uuid"}` |
| GET | /users | (sin body, query: ?ecommerceId=uuid) |
| GET | /users/{uid} | (sin body) |
| PUT | /users/{uid} | `{"username":"","email":"","password":"","ecommerceId":"uuid","active":true}` |
| DELETE | /users/{uid} | (sin body) |
| PUT | /users/me | `{"name":"","email":""}` |
| PUT | /users/me/password | `{"currentPassword":"","newPassword":"","confirmPassword":""}` |

---

## ECOMMERCE

| Método | Endpoint | Body |
|--------|----------|------|
| POST | /ecommerces | `{"name":"","slug":""}` |
| GET | /ecommerces | (sin body, query: ?status=ACTIVE&page=0&size=50) |
| GET | /ecommerces/{uid} | (sin body) |
| PUT | /ecommerces/{uid} | `{"status":"ACTIVE"}` |

---

## API KEYS

| Método | Endpoint | Body |
|--------|----------|------|
| POST | /ecommerces/{ecommerceId}/api-keys | `{}` |
| GET | /ecommerces/{ecommerceId}/api-keys | (sin body) |
| DELETE | /ecommerces/{ecommerceId}/api-keys/{keyId} | (sin body) |

---

## CONFIGURATION

| Método | Endpoint | Body |
|--------|----------|------|
| POST | /configurations | `{"ecommerceId":"uuid","currency":"CLP","roundingRule":"","cap":{"type":"PERCENTAGE","value":0,"appliesTo":"SUBTOTAL"},"priority":[{"type":"","order":1}]}` |
| PATCH | /configurations/{ecommerceId} | `{"currency":"","roundingRule":"","cap":{"type":"","value":0,"appliesTo":""},"priority":[{"type":"","order":1}]}` |

---

## DISCOUNT CONFIG

| Método | Endpoint | Body |
|--------|----------|------|
| POST | /discount-config | `{"ecommerceId":"uuid","maxDiscountLimit":0,"currencyCode":"","allowStacking":true,"roundingRule":"","capType":"","capValue":0,"capAppliesTo":""}` |
| GET | /discount-config?ecommerceId=uuid | (sin body) |
| POST | /discount-priority | `{"discountConfigId":"","priorities":[{"discountType":"","priorityLevel":1}]}` |
| GET | /discount-priority?configId= | (sin body) |

---

## RULES

| Método | Endpoint | Body |
|--------|----------|------|
| POST | /rules | `{"name":"","description":"","discountPercentage":0,"discountPriorityId":"","attributes":{}}` |
| GET | /rules | (sin body, query: ?page=0&size=20&active=true) |
| GET | /rules/{ruleId} | (sin body) |
| PUT | /rules/{ruleId} | (mismo que POST) |
| DELETE | /rules/{ruleId} | (sin body) |
| POST | /rules/{ruleId}/tiers | `{"customerTierIds":["uuid1","uuid2"]}` |
| GET | /rules/{ruleId}/tiers | (sin body) |

---

## AUDIT LOGS (Solo SUPER_ADMIN)

| Método | Endpoint | Body |
|--------|----------|------|
| GET | /audit-logs | (sin body, query: ?entityName=&ecommerceId=&page=0&size=50) |
| GET | /audit-logs/{logId} | (sin body) |

---

## DISCOUNT LOGS

| Método | Endpoint | Body |
|--------|----------|------|
| GET | /discount-logs | (sin body, query: ?ecommerceId=&externalOrderId=&page=0&size=50) |
| GET | /discount-logs/{logId} | (sin body) |

---

## AUTENTICACIÓN

- Obtener token: `POST /auth/login`
- Usar en headers: `Authorization: Bearer <TOKEN>`

## EJEMPLO CURL

```bash
# Login
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}'

# Listar users
curl -X GET http://localhost:8081/api/v1/users \
  -H "Authorization: Bearer <TOKEN>"
```