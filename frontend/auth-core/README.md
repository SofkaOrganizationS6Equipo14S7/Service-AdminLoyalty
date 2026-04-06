# Auth Core (Session + Role Guards)

Capa agnóstica de framework para manejar sesión JWT y autorización por rol sobre `service-admin`.

## Componentes

- `SessionManager`: bootstrap, login, logout, refresh de usuario actual.
- `SessionStoragePort`: persistencia de token (memoria o localStorage).
- `role-guards`: utilidades para validar autenticación y roles.

## Ejemplo rápido

```ts
import {
  LocalStorageSessionStorage,
  SessionManager,
  hasAnyRole,
} from "./index";

const session = new SessionManager({
  baseUrl: "http://localhost:8081",
  storage: new LocalStorageSessionStorage("loyalty_admin_jwt"),
});

await session.bootstrap();

await session.login({ username: "admin", password: "admin123" });

const state = session.getState();
const canManageUsers = hasAnyRole(state.user, ["SUPER_ADMIN", "STORE_ADMIN"]);
```

## Notas

- Si el backend responde `401`, el token se limpia automáticamente.
- `SessionManager` expone `getApiClient()` para usar `auth/users` con la misma sesión.
- Todas las IDs se mantienen como `UUID string`.

