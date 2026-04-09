# Hexagonal Architecture - Modelo de Implementación

## Propósito
Establecer el modelo estándar para implementar cualquier feature del microservicio service-admin siguiendo **Hexagonal Architecture (Ports & Adapters)** de manera agnóstica, garantizando consistencia y bajo acoplamiento.

## Aplicabilidad
Este modelo aplica a **TODO feature nuevo** del service-admin que involucre:
- Persistencia en base de datos (JPA)
- Integración con sistemas externos (RabbitMQ, APIs externas)
- Operaciones de dominio complejas

---

## Modelo de Carpetas

```
src/main/java/com/loyalty/service_admin/
├── application/
│   ├── dto/<feature>/
│   │   ├── <Feature>Request.java       # DTO de entrada
│   │   ├── <Feature>Response.java     # DTO de salida
│   │   └── <Feature>EventPayload.java # DTO para eventos (si aplica)
│   ├── port/
│   │   ├── in/
│   │   │   └── <Feature>UseCase.java  # Interface casos de uso
│   │   └── out/
│   │       ├── <Feature>PersistencePort.java  # Puerto persistencia
│   │       └── <Feature>EventPort.java        # Puerto eventos (si aplica)
│   └── service/
│       └── <Feature>ServiceImpl.java   # Implementación del use case
│
├── domain/
│   ├── entity/
│   │   └── <Feature>Entity.java       # Entidad JPA
│   ├── model/
│   │   └── <Feature>.java             # Modelo de dominio (opcional)
│   └── repository/
│       └── <Feature>Repository.java   # Interface JPA Repository
│
├── infrastructure/
│   ├── persistence/
│   │   └── jpa/
│   │       └── Jpa<Feature>Adapter.java    # Adapter persistencia
│   ├── messaging/
│   │   └── <Feature>EventAdapter.java      # Adapter eventos (si aplica)
│   └── rabbitmq/
│       └── <Feature>EventPublisher.java    # Broker concreto
│
└── presentation/
    └── controller/
        └── <Feature>Controller.java    # REST API
```

---

## Definición de Componentes

### 1. Puerto de Entrada (`in/<Feature>UseCase.java`)
Define los casos de uso del feature como interfaz.

**Propósito**: Abstraer la implementación del servicio del controller.

```java
public interface <Feature>UseCase {
    // Métodos representan operaciones de negocio
    <Feature>Response create(<Feature>Request request);
    <Feature>Response update(UUID id, <Feature>Request request);
    void delete(UUID id);
    <Feature>Response findById(UUID id);
    List<<Feature>Response> findAll();
}
```

### 2. Puerto de Salida - Persistencia (`out/<Feature>PersistencePort.java`)
Define operaciones de acceso a datos.

**Propósito**: Aislar la implementación de base de datos.

```java
public interface <Feature>PersistencePort {
    <Feature>Entity save(<Feature>Entity entity);
    Optional<<Feature>Entity> findById(UUID id);
    List<<Feature>Entity> findByCondition(...);
    void delete(<Feature>Entity entity);
    boolean existsByCondition(...);
}
```

### 3. Puerto de Salida - Eventos (`out/<Feature>EventPort.java`)
Define operaciones de mensajería asíncrona.

**Propósito**: Aislar el broker de mensajes (RabbitMQ, Kafka, etc.).

```java
public interface <Feature>EventPort {
    void publishCreated(<Feature>EventPayload event);
    void publishUpdated(<Feature>EventPayload event);
    void publishDeleted(<Feature>EventPayload event);
}
```

### 4. Implementación del Use Case (`service/<Feature>ServiceImpl.java`)
Contiene la lógica de negocio.

**Propósito**: Coordinación entre puertos y lógica de dominio.

```java
@Service
@Slf4j
public class <Feature>ServiceImpl implements <Feature>UseCase {
    
    private final <Feature>PersistencePort persistencePort;
    private final <Feature>EventPort eventPort;        // Si aplica
    private final OtherFeatureService otherService;    // Si necesita coordinación
    
    // Constructor con DI
}
```

### 5. Adapter de Persistencia (`persistence/jpa/Jpa<Feature>Adapter.java`)
Implementa el puerto de persistencia.

```java
@Component
@RequiredArgsConstructor
public class Jpa<Feature>Adapter implements <Feature>PersistencePort {
    
    private final <Feature>Repository repository;
    
    // Delegar operaciones al repositorio JPA
}
```

### 6. Adapter de Eventos (`messaging/<Feature>EventAdapter.java`)
Implementa el puerto de eventos.

```java
@Component
@RequiredArgsConstructor
public class <Feature>EventAdapter implements <Feature>EventPort {
    
    private final <Feature>EventPublisher publisher;
    
    // Delegar al broker concreto
}
```

---

## Reglas de Implementación

### Obligatorio para todo feature nuevo:

1. **Nunca inyectar implementaciones concretas** en servicios:
   - ❌ `private final ApiKeyRepository repository;` (JPA directo)
   - ✅ `private final ApiKeyPersistencePort persistencePort;` (Puerto)

2. **Nunca inyectar adapters** en servicios:
   - ❌ `private final JpaApiKeyAdapter adapter;`
   - ✅ `private final ApiKeyPersistencePort adapter;`

3. **Puerto de entrada siempre**:
   - El controller debe inyectar `<Feature>UseCase`, nunca `<Feature>ServiceImpl`

4. **Puerto de salida según necesidad**:
   - Si hay BD → `<Feature>PersistencePort`
   - Si hay mensajería → `<Feature>EventPort`

5. **Un adapter por puerto**:
   - `Jpa<Feature>Adapter` implementa `<Feature>PersistencePort`
   - `<Feature>EventAdapter` implementa `<Feature>EventPort`

---

## Checklist de Cumplimiento

| # | Criterio | Verificación |
|---|----------|--------------|
| 1 | Existe `<Feature>UseCase` en `application/port/in/` | [ ] |
| 2 | Existe `<Feature>PersistencePort` en `application/port/out/` | [ ] |
| 3 | Controller inyecta `<Feature>UseCase` | [ ] |
| 4 | Service implementa `<Feature>UseCase` | [ ] |
| 5 | Service depende de puertos (no implementaciones) | [ ] |
| 6 | Adapter implementa puerto correspondiente | [ ] |
| 7 | Repository vive en `domain/repository/` | [ ] |

---

## Ejemplo Aplicado: ApiKey

| Componente | Ruta | ¿Existe? |
|------------|------|----------|
| Puerto entrada | `application/port/in/ApiKeyUseCase.java` | ❌ Crear |
| Puerto persistencia | `application/port/out/ApiKeyPersistencePort.java` | ❌ Crear |
| Puerto eventos | `application/port/out/ApiKeyEventPort.java` | ❌ Crear |
| Service impl | `application/service/ApiKeyServiceImpl.java` | ❌ Crear |
| Adapter persistencia | `infrastructure/persistence/jpa/JpaApiKeyAdapter.java` | ❌ Crear |
| Adapter eventos | `infrastructure/messaging/ApiKeyEventAdapter.java` | ❌ Crear |
| Controller | `presentation/controller/ApiKeyController.java` | ✅ Existe (actualizar DI) |

---

## Notas

- **Features existentes**: Los features actuales (ApiKey, Ecommerce, User, etc.) deberán migrarse gradualmente a este modelo cuando se requieran modificaciones.
- **Excepciones**: Features simples con solo CRUD básico pueden simplificar usando solo PersistencePort sin EventPort.
- **Coordinación**: Si un feature requiere otro servicio, inyectar el `UseCase` del otro feature, no su `ServiceImpl`.
