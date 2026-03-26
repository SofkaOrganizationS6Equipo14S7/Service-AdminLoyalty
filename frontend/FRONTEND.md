# Frontend - Sistema de Autenticación LOYALTY

## Descripción
Implementación del frontend para la funcionalidad de login/logout del sistema LOYALTY usando React, TypeScript, Vite y React Router.

## Estructura de Directorios

```
frontend/
├── src/
│   ├── components/         # Componentes reutilizables
│   │   ├── LoginForm.tsx
│   │   ├── LoginForm.module.css
│   │   ├── ProtectedRoute.tsx
│   ├── context/            # Context API para estado global
│   │   └── AuthContext.tsx
│   ├── hooks/              # Custom hooks
│   │   └── useAuth.ts
│   ├── pages/              # Páginas/vistas
│   │   ├── LoginPage.tsx
│   │   ├── LoginPage.module.css
│   │   ├── Dashboard.tsx
│   │   └── Dashboard.module.css
│   ├── services/           # Servicios API
│   │   └── authService.ts
│   ├── styles/             # Estilos globales
│   ├── App.tsx             # Configuración de rutas
│   ├── main.tsx            # Punto de entrada
│   └── index.css           # Estilos base
├── .env                    # Variables de entorno (local)
├── .env.example            # Plantilla de variables de entorno
├── package.json
├── index.html
├── vite.config.ts
└── tsconfig.json
```

## Stack Tecnológico

- **React 19** - UI framework
- **TypeScript 5.9** - Tipado estático
- **Vite 8** - Build tool y dev server
- **React Router 6** - Enrutamiento
- **Axios** - Cliente HTTP
- **CSS Modules** - Estilos locales

## Instalación

### 1. Instalar dependencias

```bash
cd frontend
npm install
```

### 2. Configurar variables de entorno

Copiar `.env.example` a `.env` y ajustar la URL del API:

```bash
cp .env.example .env
```

Contenido de `.env`:
```
VITE_API_URL=http://localhost:8081
```

## Scripts

- `npm run dev` - Inicia el servidor de desarrollo (http://localhost:5173)
- `npm run build` - Compila para producción
- `npm run preview` - Vista previa del build de producción
- `npm run lint` - Ejecuta ESLint

## Features Implementadas

### ✅ Autenticación (Auth)
- [x] Servicio de autenticación (`authService.ts`)
- [x] Context API para estado global (`AuthContext.tsx`)
- [x] Hook personalizado (`useAuth.ts`)
- [x] Persistencia en localStorage (token + user data)

### ✅ Componentes
- [x] Formulario de login (`LoginForm.tsx`)
- [x] Ruta protegida (`ProtectedRoute.tsx`)

### ✅ Páginas
- [x] Página de login (`LoginPage.tsx`)
- [x] Dashboard protegido (`Dashboard.tsx`)

### ✅ Enrutamiento
- [x] React Router configurado
- [x] Rutas protegidas
- [x] Redirecciones automáticas

### ✅ Estilos
- [x] CSS Modules
- [x] Diseño responsive
- [x] Formulario de login estilizado
- [x] Dashboard con header y contenido

## Flow de Autenticación

```
1. Usuario navega a /login (sin auth)
2. Completa formulario (usuario/contraseña)
3. LoginForm → authService.login() → Backend
4. Backend retorna token JWT
5. Token se guarda en localStorage
6. Context se actualiza → isAuthenticated = true
7. App redirige automáticamente a /dashboard
8. ProtectedRoute valida token y renderiza Dashboard
9. Usuario puede hacer click en "Cerrar Sesión"
10. logout() → limpia localStorage → redirige a /login
```

## Variables de Entorno

| Variable | Descripción | Default |
|----------|-------------|---------|
| `VITE_API_URL` | URL base del backend API | `http://localhost:8081` |

## Credenciales Demo (Backend)

Para testing local, el backend crea un usuario demo:
- **Usuario**: `admin`
- **Contraseña**: `admin123`
- **Rol**: `ADMIN`

![Vista de login demo al cargar](./screenshots/login-page.png)

## Estructura del Token JWT

El backend genera tokens en formato Base64:
```
formato: username:role:timestamp:secret
ejemplo: YWRtaW46QURNSTo...
validez: 24 horas
header: Authorization: Bearer <token>
```

## Características del Formulario

### Validación Frontend
- Username obligatorio
- Password obligatorio
- Mensajes de error claros

### Estados
- ❌ Validación fallida → muestra error
- ⏳ Enviando → desactiva inputs y botón
- ✅ Éxito → limpia formulario, redirige
- ⚠️ Error server → muestra mensaje

## Características del Dashboard

- Header con usuario y botón logout
- Información del usuario autenticado
- Grid de funcionalidades (placeholder)
- Logout tolerante (limpia datos locales)

## Notas de Seguridad

1. **Token en localStorage**: Usado para persistencia entre recargas
2. **XSS**: Token no se expone en console logs durante normal operation
3. **CORS**: Configurar en backend si frontend está en dominio diferente
4. **HTTPS**: En producción, siempre usar HTTPS
5. **Token expirado**: Automaticamente redirige a login si recibe 401

## Testing Local

```bash
# Terminal 1: Backend
cd backend/service-admin
mvn spring-boot:run

# Terminal 2: Frontend
cd frontend
npm run dev
```

Visitar `http://localhost:5173/login` y usar credenciales demo.

## Próximas Mejoras (Future)

- [ ] Refresh token automático
- [ ] Token blacklist en logout
- [ ] Hashing bcrypt de passwords
- [ ] Two-factor authentication (2FA)
- [ ] Social login (Google, GitHub)
- [ ] Password reset flow
- [ ] Remember me checkbox
- [ ] Role-based access control (RBAC) en componentes

## Problemas Conocidos

Ninguno reportado en esta versión (v1.0).

## Licencia

Propiedad de LOYALTY - Sistema de Autenticación
