# Loyalty Microservices Archetype

## Estructura

- `admin-service`: backoffice y configuracion.
- `discount-engine-service`: motor de calculo de descuentos.

## Prerrequisitos

- Java 17
- Maven 3.9+

## Configuracion rapida

### Variables de entorno relevantes

- `ADMIN_SERVICE_URL` (default: `http://localhost:8081`)
- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `DB_DRIVER`

### Perfiles de base de datos (admin-service)

- Default: H2 en memoria.
- PostgreSQL: ejecutar con perfil `postgres`.

## Levantar servicios

Orden recomendado: primero `admin-service`, luego `discount-engine-service`.

### Admin service

```bash
cd microservices/admin-service
mvn spring-boot:run
```

Con PostgreSQL:

```bash
cd microservices/admin-service
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
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
- `GET /reglas?ecommerceId={id}`
- `PATCH /reglas/{id}/estado`
- `GET /reglas/ecommerce/{ecommerceId}`
- `POST /configuracion`
- `GET /configuracion/{ecommerceId}`
- `POST /apikey/validar`
- `GET /rangos-fidelidad?ecommerceId={id}`
- `POST /rangos-fidelidad`
- `PUT /rangos-fidelidad/{id}`
- `DELETE /rangos-fidelidad/{id}`

### discount-engine-service

- `POST /calcular-descuento`
- `POST /clasificar-cliente`

## Integracion entre servicios

`discount-engine-service` consume `admin-service` via REST:

- `GET /configuracion/{ecommerceId}`
- `GET /reglas/ecommerce/{ecommerceId}`
- `POST /apikey/validar`

Implementacion desacoplada:

- `client/AdminServiceClient` (contrato)
- `client/RestAdminServiceClient` (adaptador REST)

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

## Notas importantes

- La autenticacion actual es mock/simple (token generado de forma basica para fines de arquetipo).
- `discount-engine-service` es stateless y no usa base de datos.

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
