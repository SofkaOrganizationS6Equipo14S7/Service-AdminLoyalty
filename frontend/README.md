# Loyalty Frontend

Frontend administrativo para `service-admin` (React + Vite + TypeScript).

## Alcance actual

- Autenticación y sesión con JWT contra `service-admin`.
- Guards de autenticación y roles (`SUPER_ADMIN`, `STORE_ADMIN`, `ADMIN`).
- Módulos UI conectados al backend admin:
  - Dashboard
  - Perfil y cambio de contraseña
  - Users (listado, detalle, create, update, delete)
  - Ecommerces
  - API Keys
  - Discount Setup
  - Rules + Customer Tiers
  - Roles y permisos
  - Logs

## Nota importante de arquitectura

- El frontend **no** consume `service-engine` directamente.
- El consumo de `service-engine` se hace server-to-server desde backend.
- Para engine se utiliza `apikey` (no token JWT de front).

## Requisitos

- Node.js 20+
- npm 10+
- `service-admin` levantado en `http://localhost:8081`

## Ejecutar frontend

```bash
cd frontend
npm install
npm run dev
```

## Scripts disponibles

- `npm run dev`: entorno local Vite.
- `npm run build`: compilación TypeScript + build de Vite.
- `npm run preview`: previsualizar build.
- `npm run backend:stability`: ejecuta pruebas de estabilidad backend (engine + admin) usando `../backend/scripts/check-backend-stability.ps1`.

## Estructura

- `src/`: aplicación React (vistas, sesión, rutas).
- `api-client/`: cliente TypeScript para `service-admin`.
- `auth-core/`: manejo de sesión y guards por rol.

