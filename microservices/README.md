# Loyalty Microservices Archetype

## Estructura

- `admin-service`: backoffice y configuracion.
- `discount-engine-service`: motor de calculo de descuentos.

## Levantar servicios

### Admin service

```bash
cd microservices/admin-service
mvn spring-boot:run
```

### Discount engine

```bash
cd microservices/discount-engine-service
mvn spring-boot:run
```

## Endpoints principales

### admin-service

- `POST /auth/login`
- `CRUD /users`
- `CRUD /ecommerce`
- `CRUD /reglas`
- `PATCH /reglas/{id}/estado`
- `GET /reglas/ecommerce/{ecommerceId}`
- `GET /configuracion/{ecommerceId}`
- `POST /apikey/validar`

### discount-engine-service

- `POST /calcular-descuento`
- `POST /clasificar-cliente`

## Ejemplo request/response

### Login

Request:

```json
{
  "username": "admin",
  "password": "admin123"
}
```

Response:

```json
{
  "token": "YWRtaW46QURNSU46MTcwMDAwMDAwMDAwMDpsb3lhbHR5LXNlY3JldC1rZXk=",
  "tipo": "Bearer",
  "username": "admin"
}
```

### Calcular descuento

Request:

```json
{
  "apiKey": "demo-api-key",
  "ecommerceId": 1,
  "clienteId": "CLI-001",
  "puntosFidelidad": 2600,
  "temporada": "BLACK_FRIDAY",
  "items": [
    {
      "sku": "SKU-001",
      "cantidad": 2,
      "precioUnitario": 120.00
    }
  ]
}
```

Response:

```json
{
  "clienteId": "CLI-001",
  "nivelFidelidad": "PLATA",
  "subtotal": 240.00,
  "descuentoAplicado": 24.00,
  "total": 216.00,
  "reglasAplicadas": [
    "Black Friday"
  ]
}
```
