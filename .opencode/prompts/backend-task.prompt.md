---
name: backend-task
description: Implementa una funcionalidad en el backend Spring Boot basada en una spec ASDD aprobada.
argument-hint: "<nombre-feature> (debe existir .github/specs/<nombre-feature>.spec.md)"
agent: Backend Developer
tools:
  - edit/createFile
  - edit/editFiles
  - read/readFile
  - search/listDirectory
  - search
  - execute/runInTerminal
---

Implementa el backend para el feature especificado, siguiendo la spec aprobada.

**Feature**: ${input:featureName:nombre del feature en kebab-case}

## Pasos obligatorios:

1. **Lee la spec** en `.github/specs/${input:featureName:nombre-feature}.spec.md` — si no existe, detente e informa al usuario.
2. **Revisa el código existente** en `backend/src/main/java/com/loyalty/` para entender patrones actuales.
3. **Lee la referencia de patrones**: `.github/skills/implement-backend/patterns.java`
4. **Implementa en orden**:
   - `backend/src/main/java/com/loyalty/entity/` — Entity JPA
   - `backend/src/main/java/com/loyalty/dto/` — DTOs (Java Records)
   - `backend/src/main/java/com/loyalty/repository/` — Repository JPA
   - `backend/src/main/java/com/loyalty/service/` — Service
   - `backend/src/main/java/com/loyalty/controller/` — Controller REST
   - `backend/src/main/java/com/loyalty/exception/` — Custom exceptions
5. **Crea migración Flyway** en `backend/src/main/resources/db/migration/V1__create_<feature>.sql`
6. **Verifica sintaxis** ejecutando: `cd backend && ./mvnw compile`

## Restricciones:
- Usar Constructor Injection (Spring lo maneja).
- DTOs como Java Records, nunca clases.
- Moneda siempre `BigDecimal`, nunca `double`/`float`.
- Todas las operaciones de DB con JPA (no queries crudas).
