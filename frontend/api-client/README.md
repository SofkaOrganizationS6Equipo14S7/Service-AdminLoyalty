# Service Admin API Client (Front Base)

Módulo base TypeScript para consumir `service-admin` sin acoplarse a framework.

## Uso rápido

```ts
import { createServiceAdminClient } from "./index";

let token: string | null = null;

const client = createServiceAdminClient({
  baseUrl: "http://localhost:8081",
  getToken: () => token,
  onUnauthorized: () => {
    token = null;
  },
});

const login = await client.auth.login({ username: "admin", password: "admin123" });
token = login.token;

const me = await client.auth.me();
const users = await client.users.list(me.ecommerceId ?? undefined);
```

## Reglas implementadas

- `Authorization: Bearer <token>` automático para endpoints protegidos.
- Manejo centralizado de errores HTTP con `ApiError`.
- Callback `onUnauthorized` para sesión expirada (`401`).
- Tipos con IDs como `UUID string`.

