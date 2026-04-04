# LOYALTY S7

## Requisitos previos

- Java 21+
- Maven (o usar el wrapper `mvnw` incluido)
- Docker y Docker Compose

---

## 1. Levantar infraestructura (Docker)

Desde la carpeta `backend/` ejecutar:

```bash
cd backend
docker compose up -d
```

Esto levanta:

| Servicio          | Puerto        | Descripción              |
|-------------------|---------------|--------------------------|
| `postgres-admin`  | `5432`        | DB del service-admin     |
| `postgres-engine` | `5433`        | DB del service-engine    |
| `rabbitmq`        | `5672 / 15672`| Message broker + UI mgmt |

> RabbitMQ Management UI: http://localhost:15672 (guest / guest)

Para detener la infraestructura:

```bash
docker compose down
```

---

## 2. Levantar service-admin

Puerto: `8081`

```powershell
cd backend\service-admin
$env:JAVA_HOME="C:\jdk-21.0.9_windows-x64_bin\jdk-21.0.9"
.\mvnw.cmd spring-boot:run
```

> Si `JAVA_HOME` ya apunta a JDK 21, podés omitir la segunda línea.

---

## 3. Levantar service-engine

Puerto: `8082`

```powershell
cd backend\service-engine
$env:JAVA_HOME="C:\jdk-21.0.9_windows-x64_bin\jdk-21.0.9"
.\mvnw.cmd spring-boot:run
```

> Si `JAVA_HOME` ya apunta a JDK 21, podés omitir la segunda línea.

---

## Orden de arranque recomendado

1. `docker compose up -d` — infraestructura primero
2. `service-admin` — esperar a que esté listo en `:8081`
3. `service-engine` — esperar a que esté listo en `:8082`

---

## Puertos de referencia

| Componente        | URL                          |
|-------------------|------------------------------|
| service-admin     | http://localhost:8081        |
| service-engine    | http://localhost:8082        |
| RabbitMQ UI       | http://localhost:15672       |
| PostgreSQL admin  | localhost:5432               |
| PostgreSQL engine | localhost:5433               |
