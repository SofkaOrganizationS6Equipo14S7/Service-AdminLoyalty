---
id: SPEC-008
status: APPROVED
feature: fidelity-ranges
created: 2026-03-30
updated: 2026-03-30
author: spec-generator
version: "1.1"
related-specs: []
---

# Spec: Rangos de Clasificación de Fidelidad (HU-08)

> **Estado:** `APPROVED` — listo para implementación.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPRECATED
> **Versión 1.1:** Agregada autonomía del Engine (Cold Start), índice único parcial, flexible continuity validation.

---

## 1. REQUERIMIENTOS

### Descripción
Gestión de rangos de clasificación de fidelidad para segmentar clientes según puntos acumulados. El sistema valida que los rangos sean no superpuestos, continuos y progresivos, almacenando la configuración en PostgreSQL y sincronizando cambios al Engine Service vía RabbitMQ para que la clasificación se ejecute en memoria sin impactar latencia.

### Requerimiento de Negocio
Como usuario de LOYALTY, quiero definir los rangos de clasificación de fidelidad, para segmentar a los clientes y sus beneficios.

### Historias de Usuario

#### HU-08: Configuración de Rangos de Fidelidad

```
Como:        Administrador de ecommerce
Quiero:      Crear, actualizar y validar rangos de fidelidad (Bronce, Plata, Oro, Platino)
Para:        Segmentar clientes automáticamente según puntos acumulados y asignar beneficios

Prioridad:   Alta
Estimación:  M
Dependencias: HU-01 (Autenticación), HU-02 (Autorización), HU-07 (Descuentos)
Capa:        Backend (Admin Service) + Engine Service Consumer
```

#### Criterios de Aceptación — HU-08

**CRITERIO-8.1: Crear configuración exitosa de rangos no superpuestos**
```gherkin
Dado que:   existen rangos propuestos Bronce [0-999], Plata [1000-4999], Oro [5000-9999], Platino [10000+]
Cuando:     POST /api/v1/fidelity-ranges con estructura válida y no superpuesta
Entonces:   responde 201 Created con uid generado
Y           la BD almacena los 4 rangos
Y           cada rango vinculado al ecommerce_id del usuario
```

**CRITERIO-8.2: Rechazar configuración con rangos superpuestos**
```gherkin
Dado que:   intento registrar Bronce [0-1000] y Plata [500-5000] (solapamiento 500-1000)
Cuando:     POST /api/v1/fidelity-ranges con rango superpuesto
Entonces:   responde 400 Bad Request
Y           mensaje: "Rango superpuesto: [500-1000] ya asignado a otro nivel"
Y           la BD no guarda cambios
```

**CRITERIO-8.3: Permitir huecos en configuración (gap tolerance)**
```gherkin
Dado que:   intento registrar Bronce [0-1000] y Oro [5000-9999] (vacío 1001-4999)
Cuando:     POST /api/v1/fidelity-ranges con gap intencional
Entonces:   responde 201 Created (gap permitido)
Y           el rango se guarda correctamente
Y           nota: Admin puede crear huecos; Engine maneja fallthrough
```

**CRITERIO-8.4: Rechazar orden jerárquico inválido**
```gherkin
Dado que:   intento registrar Platino [0-1000] y Bronce [1000-5000] (orden invertido)
Cuando:     POST /api/v1/fidelity-ranges con umbral_min de nivel superior < umbral_min de nivel inferior
Entonces:   responde 400 Bad Request
Y           mensaje: "Orden inválido: Platino (0) no puede estar antes que Bronce (1000)"
Y           la configuración no se guarda
```

**CRITERIO-8.5: Clasificación automática con fallthrough en huecos**
```gherkin
Dado que:   rangos: Bronce [0-999], Plata [1000-4999], Oro [5000-9999], Platino [10000+]
Cuando:     El Engine clasifica cliente A (2500 pts), cliente B en hueco [1500-2000], cliente C (500 pts)
Entonces:   Cliente A → Plata (rango exacto)
Y           Cliente B (en hueco [1001-4999]) → Bronce (fallthrough al nivel inferior más cercano)
Y           Cliente C (500 pts < 1000 mín) → NONE (no califica para ningún nivel)
Y           beneficios se determinan según nivel asignado (o se omiten si NONE)
```

**CRITERIO-8.6: Actualizar rango existente sin romper validaciones**
```gherkin
Dado que:   existe rango Bronce [0-999]
Cuando:     PUT /api/v1/fidelity-ranges/{uid} actualiza a Bronce [0-1200]
Entonces:   responde 200 OK
Y           la BD actualiza el rango
Y           se valida nuevamente la cadena completa (no superpuestos, continuos, progresivos)
```

**CRITERIO-8.7: Sincronizar cambios de rangos al Engine Service**
```gherkin
Dado que:   POST /api/v1/fidelity-ranges completó exitosamente
Cuando:     el Backend (Admin Service) publica evento 'FidelityRangeCreated' a RabbitMQ
Entonces:   el Engine Service consume el evento
Y           actualiza su caché Caffeine con los nuevos rangos
Y           futuras clasificaciones usan los rangos actualizados en memoria
```

**CRITERIO-8.8: Listar rangos del ecommerce autenticado**
```gherkin
Dado que:   usuario autenticado del ecommerce "MiTienda"
Cuando:     GET /api/v1/fidelity-ranges
Entonces:   responde 200 OK
Y           retorna solo los rangos de "MiTienda" (aislamiento por tenant)
Y           incluye uid, name, min_points, max_points, discount_percentage, created_at, updated_at
```

**CRITERIO-8.9: Obtener un rango específico por uid**
```gherkin
Dado que:   existe rango con uid = 550e8400-e29b-41d4-a716-446655440000
Cuando:     GET /api/v1/fidelity-ranges/550e8400-e29b-41d4-a716-446655440000
Entonces:   responde 200 OK
Y           retorna el rango completo
Y           responde 404 Not Found si el uid no existe o pertenece a otro ecommerce
```

**CRITERIO-8.10: Eliminar rango (soft-delete)**
```gherkin
Dado que:   existe rango Bronce activo
Cuando:     DELETE /api/v1/fidelity-ranges/{uid}
Entonces:   responde 204 No Content
Y           la BD marca is_active = false (soft delete)
Y           el rango no aparece en listados posteriores
Y           evento 'FidelityRangeDeleted' se publica a RabbitMQ
```

### Reglas de Negocio

1. **Validación de no superposición (ESTRICTA)**: Los rangos [min_points, max_points] de cada nivel no pueden solaparse. Admin rechaza con 400 si ocurre overlap.

2. **Validación de continuidad (FLEXIBLE)**: Se permiten huecos intencionales (ej: Bronce [0-999], Oro [5000-9999]). Admin crea sin error. Engine DEBE manejar fallthrough: 
   - Si cliente cae exactamente en un rango → asignar ese nivel
   - Si cliente cae en hueco → asignar el nivel inferior más cercano (fallthrough)
   - Si cliente < minPoints del nivel más bajo → retornar NONE (no califica para ningún nivel)

3. **Validación de progresión jerárquica (FLEXIBLE)**: min_points debe ser ascendente E no pueden solaparse, pero huecos son válidos. El Admin valida no overlap; continuidad es responsabilidad del Engine.

4. **Aislamiento por Tenant**: Cada ecommerce gestiona sus propios rangos. Un usuario solo ve/modifica rangos de su ecommerce_id.

5. **Autorización**: Solo Administrador puede crear/actualizar/eliminar rangos. Requiere permiso `fidelity:write`.

6. **Sincronización RabbitMQ**: Cada cambio en rangos (CREATE, UPDATE, DELETE) publica un evento al Engine Service. El Engine cachea en Caffeine para clasificación en memoria.

7. **Soft Delete**: Los rangos eliminados se marcan `is_active = false`, no se borran físicamente.

8. **Timestamps UTC**: `created_at` y `updated_at` siempre en UTC (Instant con `java.time.Instant`).

---

## 2. DISEÑO

### Modelos de Datos

#### Entidades afectadas

| Entidad | Almacén | Cambios | Descripción |
|---------|---------|---------|-------------|
| `FidelityRangeEntity` | **Admin**: tabla `fidelity_ranges` (source of truth) | **nueva** | Rangos de clasificación de fidelidad (Admin Service) |
| `FidelityRangeEntity` | **Engine**: tabla `fidelity_ranges` (réplica para Cold Start) | **nueva** | Replica para autonomía del Engine; se sincroniza vía RabbitMQ |
| `PermissionEntity` | tabla `permissions` | **modificada** | Agregar permisos `fidelity:read`, `fidelity:write` |

#### Campos del modelo — `fidelity_ranges`

| Campo | Tipo SQL | Obligatorio | Validación | Descripción |
|-------|----------|-------------|------------|-------------|
| `uid` | UUID | sí | auto-generado | Identificador único (PK) |
| `ecommerce_id` | UUID | sí | FK → ecommerces.uid | Tenant (aislamiento) |
| `name` | VARCHAR(255) | sí | @NotBlank, @Size(max=255) | Nombre del nivel (ej: "Bronce", "Plata") |
| `min_points` | INTEGER | sí | @NotNull, @PositiveOrZero | Mínimo de puntos incluidos (ej: 0) |
| `max_points` | INTEGER | sí | @NotNull, @Positive | Máximo de puntos incluidos (ej: 999) |
| `discount_percentage` | NUMERIC(5,2) | sí | @NotNull, @DecimalMin/Max | Descuento aplicable [0, 100] |
| `is_active` | BOOLEAN | sí | default true | Soft delete flag |
| `created_at` | TIMESTAMP WITH TIME ZONE | sí | auto-generado (CURRENT_TIMESTAMP) | Timestamp UTC de creación |
| `updated_at` | TIMESTAMP WITH TIME ZONE | sí | auto-actualizado | Timestamp UTC de última modificación |

#### Índices y Constraints

```sql
-- Índices para búsquedas frecuentes
CREATE INDEX idx_fidelity_ranges_ecommerce_id ON fidelity_ranges(ecommerce_id);
CREATE INDEX idx_fidelity_ranges_active ON fidelity_ranges(is_active);
CREATE INDEX idx_fidelity_ranges_ecommerce_active ON fidelity_ranges(ecommerce_id, is_active);
CREATE INDEX idx_fidelity_ranges_min_points ON fidelity_ranges(min_points);

-- Índice Único Parcial: Solo un nombre ACTIVO por ecommerce
-- Permite histórico de nombres eliminados (soft-delete)
CREATE UNIQUE INDEX uq_active_fidelity_name 
  ON fidelity_ranges (ecommerce_id, name) 
  WHERE (is_active IS TRUE);

-- Constraints
CONSTRAINT pk_fidelity_ranges PRIMARY KEY (uid)
CONSTRAINT fk_fidelity_ranges_ecommerce FOREIGN KEY (ecommerce_id) REFERENCES ecommerces(uid) ON DELETE CASCADE
CONSTRAINT ck_min_max_points CHECK (min_points >= 0 AND max_points > min_points)
CONSTRAINT ck_discount_range CHECK (discount_percentage BETWEEN 0 AND 100)
```

---

### API Endpoints

#### POST /api/v1/fidelity-ranges
**Crear un rango de fidelidad**

- **Descripción**: Crea un nuevo rango de fidelidad para el ecommerce autenticado. Valida que no se superponga con existentes.
- **Auth requerida**: Sí (JWT)
- **Permiso requerido**: `fidelity:write`
- **Request Body**:
  ```json
  {
    "name": "Bronce",
    "minPoints": 0,
    "maxPoints": 999,
    "discountPercentage": 5.00
  }
  ```
- **Validaciones en Request**:
  - `name`: @NotBlank, @Size(min=1, max=255)
  - `minPoints`: @NotNull, @PositiveOrZero
  - `maxPoints`: @NotNull, @Positive
  - `discountPercentage`: @NotNull, @DecimalMin("0"), @DecimalMax("100")

- **Validaciones de Negocio** (Backend):
  - Validar que `minPoints < maxPoints`
  - Validar que no existe rango superpuesto en el mismo ecommerce (overlap check ESTRICTA)
  - ~~Validar continuidad~~ REMOVIDA: Admin permite huecos; Engine maneja fallthrough
  - Validar que minPoints es ascendente (sin solapamiento)

- **Response 201**:
  ```json
  {
    "uid": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Bronce",
    "minPoints": 0,
    "maxPoints": 999,
    "discountPercentage": 5.00,
    "isActive": true,
    "createdAt": "2026-03-30T10:15:30Z",
    "updatedAt": "2026-03-30T10:15:30Z"
  }
  ```

- **Response 400**: Validación fallida
  ```json
  {
    "status": 400,
    "message": "Rango superpuesto: [500-1000] ya asignado a nivel Plata",
    "timestamp": "2026-03-30T10:15:30Z"
  }
  ```

- **Response 401**: Token ausente o expirado
- **Response 403**: Usuario sin permiso `fidelity:write`
- **Eventos publicados** (RabbitMQ):
  - `FidelityRangeCreated`: `{ uid, ecommerceId, name, minPoints, maxPoints, discountPercentage, timestamp }`

---

#### GET /api/v1/fidelity-ranges
**Listar rangos del ecommerce**

- **Descripción**: Lista todos los rangos activos del ecommerce autenticado.
- **Auth requerida**: Sí
- **Permiso requerido**: `fidelity:read`
- **Query Params**:
  - `page`: número de página (default: 0)
  - `size`: registros por página (default: 10)
  - `sort`: campo y dirección (ej: `minPoints,asc`)

- **Response 200**:
  ```json
  [
    {
      "uid": "550e8400-e29b-41d4-a716-446655440000",
      "name": "Bronce",
      "minPoints": 0,
      "maxPoints": 999,
      "discountPercentage": 5.00,
      "isActive": true,
      "createdAt": "2026-03-30T10:15:30Z",
      "updatedAt": "2026-03-30T10:15:30Z"
    },
    ...
  ]
  ```

- **Response 401**: Token ausente
- **Response 403**: Sin permiso

---

#### GET /api/v1/fidelity-ranges/{uid}
**Obtener un rango por uid**

- **Descripción**: Obtiene los detalles de un rango específico.
- **Auth requerida**: Sí
- **Permiso requerido**: `fidelity:read`
- **Path Param**: `uid` (UUID)

- **Response 200**: rango completo (mismo formato que POST response)

- **Response 404**: No encontrado o pertenece a otro ecommerce
  ```json
  {
    "status": 404,
    "message": "Rango no encontrado",
    "timestamp": "2026-03-30T10:15:30Z"
  }
  ```

- **Response 401**: Token ausente
- **Response 403**: Sin permiso

---

#### PUT /api/v1/fidelity-ranges/{uid}
**Actualizar un rango**

- **Descripción**: Actualiza un rango existente. Re-valida la cadena completa tras cambios.
- **Auth requerida**: Sí
- **Permiso requerido**: `fidelity:write`
- **Path Param**: `uid` (UUID)
- **Request Body** (campos opcionales):
  ```json
  {
    "name": "Plata",
    "minPoints": 1000,
    "maxPoints": 4999,
    "discountPercentage": 10.00
  }
  ```

- **Validaciones**: Mismas que POST + re-validación completa de cadena

- **Response 200**: rango actualizado

- **Response 400**: Validación fallida (ej: superposición tras cambio)
- **Response 404**: uid no encontrado
- **Response 401/403**: Auth/Permisos

- **Eventos publicados**:
  - `FidelityRangeUpdated`: `{ uid, ecommerceId, oldValues, newValues, timestamp }`

---

#### DELETE /api/v1/fidelity-ranges/{uid}
**Eliminar rango (soft-delete)**

- **Descripción**: Marca el rango como inactivo (soft delete).
- **Auth requerida**: Sí
- **Permiso requerido**: `fidelity:write`
- **Path Param**: `uid` (UUID)

- **Response 204**: Eliminado (sin contenido)

- **Response 404**: uid no encontrado
- **Response 401/403**: Auth/Permisos

- **Eventos publicados**:
  - `FidelityRangeDeleted`: `{ uid, ecommerceId, timestamp }`

---

### Componentes Backend

#### `FidelityRangeEntity` (Domain)
```java
// src/main/java/com/loyalty/service_admin/domain/entities/FidelityRangeEntity.java
@Entity
@Table(name = "fidelity_ranges", indexes = {
    @Index(name = "idx_fidelity_ranges_ecommerce_id", columnList = "ecommerce_id"),
    @Index(name = "idx_fidelity_ranges_active", columnList = "is_active"),
    @Index(name = "idx_fidelity_ranges_ecommerce_active", columnList = "ecommerce_id,is_active")
})
@Getter
@Setter
@NoArgsConstructor
public class FidelityRangeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uid;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "ecommerce_id", nullable = false)
    private EcommerceEntity ecommerce;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String name;

    @NotNull
    @Column(name = "min_points", nullable = false)
    private Integer minPoints;

    @NotNull
    @Column(name = "max_points", nullable = false)
    private Integer maxPoints;

    @NotNull
    @Column(name = "discount_percentage", nullable = false)
    private BigDecimal discountPercentage;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
```

#### DTOs (Application Layer)

```java
// CreateFidelityRangeRequest
public record CreateFidelityRangeRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 1, max = 255, message = "Nombre debe tener 1-255 caracteres")
    String name,

    @NotNull(message = "minPoints es obligatorio")
    @PositiveOrZero(message = "minPoints debe ser >= 0")
    Integer minPoints,

    @NotNull(message = "maxPoints es obligatorio")
    @Positive(message = "maxPoints debe ser > 0")
    Integer maxPoints,

    @NotNull(message = "discountPercentage es obligatorio")
    @DecimalMin(value = "0.00", message = "Descuento mínimo 0%")
    @DecimalMax(value = "100.00", message = "Descuento máximo 100%")
    BigDecimal discountPercentage
) { }

// UpdateFidelityRangeRequest
public record UpdateFidelityRangeRequest(
    @Size(min = 1, max = 255)
    String name,
    Integer minPoints,
    Integer maxPoints,
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    BigDecimal discountPercentage
) { }

// FidelityRangeResponse
public record FidelityRangeResponse(
    UUID uid,
    String name,
    Integer minPoints,
    Integer maxPoints,
    BigDecimal discountPercentage,
    boolean isActive,
    Instant createdAt,
    Instant updatedAt
) { }
```

#### `FidelityRangeController` (Presentation)
```java
// Estructura
@RestController
@RequestMapping("/api/v1/fidelity-ranges")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN')")
public class FidelityRangeController {
    private final FidelityRangeService service;
    private final SecurityService securityService;

    @PostMapping
    @PreAuthorize("hasAuthority('fidelity:write')")
    public ResponseEntity<FidelityRangeResponse> create(
        @Valid @RequestBody CreateFidelityRangeRequest request
    ) { /* GET ecommerce_id del usuario, validar, guardar, publicar evento */ }

    @GetMapping
    @PreAuthorize("hasAuthority('fidelity:read')")
    public ResponseEntity<Page<FidelityRangeResponse>> list(Pageable pageable) { }

    @GetMapping("/{uid}")
    @PreAuthorize("hasAuthority('fidelity:read')")
    public ResponseEntity<FidelityRangeResponse> getById(@PathVariable UUID uid) { }

    @PutMapping("/{uid}")
    @PreAuthorize("hasAuthority('fidelity:write')")
    public ResponseEntity<FidelityRangeResponse> update(
        @PathVariable UUID uid,
        @Valid @RequestBody UpdateFidelityRangeRequest request
    ) { }

    @DeleteMapping("/{uid}")
    @PreAuthorize("hasAuthority('fidelity:write')")
    public ResponseEntity<Void> delete(@PathVariable UUID uid) { }
}
```

#### `FidelityRangeService` (Application)
```java
// Responsabilidades:
// 1. Validar no superposición (ESTRICTA en cada POST/PUT)
// 2. NO validar continuidad (Admin permite huecos)
// 3. Validar progresión de min_points (ascendente)
// 4. Publicar eventos RabbitMQ
// 5. CRUD delegado a Repository

public class FidelityRangeService {
    private final FidelityRangeRepository repository;
    private final FidelityRangeEventPublisher eventPublisher;

    public FidelityRangeResponse create(UUID ecommerceId, CreateFidelityRangeRequest request) {
        // Fetch existing ranges for ecommerce
        // Validate no overlap (ESTRICTA) → 400 si choca
        // Validate min_points is ascending
        // Save entity
        // Publish FidelityRangeCreated event
        // Return DTO
    }

    public FidelityRangeResponse update(UUID uid, UpdateFidelityRangeRequest request) {
        // Fetch entity
        // Update fields (if present)
        // Re-validate no overlap (ESTRICTA)
        // Re-validate min_points progression
        // Save
        // Publish FidelityRangeUpdated event
    }

    public void delete(UUID uid) {
        // Mark is_active = false
        // Publish FidelityRangeDeleted event
    }

    private void validateNoOverlap(UUID ecommerceId, Integer minPoints, Integer maxPoints) {
        // Check no existing range overlaps with [minPoints, maxPoints]
        // Query: (min_points <= maxPoints AND max_points >= minPoints)
        // If found → throw BusinessException("Rango superpuesto")
    }

    private void validateProgression(UUID ecommerceId, Integer newMinPoints) {
        // Check all existing ranges have min_points in ascending order
    }
}
```

#### RabbitMQ Events

```java
// Domain Event (in package domain.events)
public record FidelityRangeCreatedEvent(
    UUID uid,
    UUID ecommerceId,
    String name,
    Integer minPoints,
    Integer maxPoints,
    BigDecimal discountPercentage,
    Instant timestamp
) { }

public record FidelityRangeUpdatedEvent(
    UUID uid,
    UUID ecommerceId,
    Map<String, Object> oldValues,
    Map<String, Object> newValues,
    Instant timestamp
) { }

public record FidelityRangeDeletedEvent(
    UUID uid,
    UUID ecommerceId,
    Instant timestamp
) { }

// Publisher in infrastructure.rabbitmq
@Component
@RequiredArgsConstructor
public class FidelityRangeEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publishCreated(FidelityRangeCreatedEvent event) {
        rabbitTemplate.convertAndSend("fidelity.ranges.exchange", "fidelity.range.created", event);
    }

    public void publishUpdated(FidelityRangeUpdatedEvent event) {
        rabbitTemplate.convertAndSend("fidelity.ranges.exchange", "fidelity.range.updated", event);
    }

    public void publishDeleted(FidelityRangeDeletedEvent event) {
        rabbitTemplate.convertAndSend("fidelity.ranges.exchange", "fidelity.range.deleted", event);
    }
}
```

#### `FidelityRangeRepository` (Infrastructure)
```java
// Extends JpaRepository<FidelityRangeEntity, UUID>
public interface FidelityRangeRepository extends JpaRepository<FidelityRangeEntity, UUID> {
    List<FidelityRangeEntity> findByEcommerceIdAndIsActiveTrue(UUID ecommerceId);
    List<FidelityRangeEntity> findByEcommerceIdOrderByMinPointsAsc(UUID ecommerceId);
    Optional<FidelityRangeEntity> findByUidAndEcommerceId(UUID uid, UUID ecommerceId);
    boolean existsByEcommerceIdAndName(UUID ecommerceId, String name);
}
```

### Engine Service Integration

#### Listener (Infrastructure)
```java
// backend/service-engine/src/main/java/.../infrastructure/rabbitmq/FidelityRangeListener.java
@Component
@RequiredArgsConstructor
public class FidelityRangeListener {
    private final FidelityRangeCache cache;

    @RabbitListener(queues = "fidelity.ranges.queue")
    public void handleFidelityRangeCreated(FidelityRangeCreatedEvent event) {
        cache.addRange(event.ecommerceId(), event);
    }

    @RabbitListener(queues = "fidelity.ranges.queue")
    public void handleFidelityRangeUpdated(FidelityRangeUpdatedEvent event) {
        cache.updateRange(event.uid(), event);
    }

    @RabbitListener(queues = "fidelity.ranges.queue")
    public void handleFidelityRangeDeleted(FidelityRangeDeletedEvent event) {
        cache.removeRange(event.uid());
    }
}
```

#### Cache Layer (Infrastructure)
```java
@Component
public class FidelityRangeCache {
    private final Cache<UUID, FidelityRangeDTO> cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofHours(1))
        .build();

    public void addRange(UUID ecommerceId, FidelityRangeCreatedEvent event) {
        cache.put(event.uid(), new FidelityRangeDTO(...));
    }

    public FidelityRangeDTO getRange(UUID uid) {
        return cache.getIfPresent(uid);
    }

    public List<FidelityRangeDTO> getRangesByEcommerce(UUID ecommerceId) {
        // Retorna todos los rangos activos del ecommerce desde caché
        // Usado por /calculate endpoint para clasificación en memoria
    }
}
```

#### Classification Logic with Fallthrough (Engine Service)
```java
// En el Engine Service, al calcular /calculate:
public ClassificationResult classify(UUID ecommerceId, Integer clientPoints) {
    List<FidelityRangeDTO> ranges = cache.getRangesByEcommerce(ecommerceId);
    
    if (ranges == null || ranges.isEmpty()) {
        return ClassificationResult.NONE; // Sin configuración de rangos
    }
    
    // Paso 1: Buscar rango exacto (cliente cae exactamente en un nivel)
    for (FidelityRangeDTO range : ranges.stream()
        .sorted(Comparator.comparingInt(FidelityRangeDTO::minPoints))
        .toList()) {
        if (clientPoints >= range.minPoints() && clientPoints <= range.maxPoints()) {
            return ClassificationResult.of(range);
        }
    }
    
    // Paso 2: Fallthrough — cliente en hueco
    // Buscar el nivel inferior INMEDIATAMENTE inferior (mayor maxPoints < clientPoints)
    Optional<FidelityRangeDTO> fallback = ranges.stream()
        .filter(r -> r.maxPoints() < clientPoints)
        .max(Comparator.comparingInt(FidelityRangeDTO::maxPoints));
    
    if (fallback.isPresent()) {
        // Cliente cae en hueco pero existe un nivel inferior válido
        return ClassificationResult.of(fallback.get());
    }
    
    // Paso 3: Cliente "demasiado nuevo" — NO CALIFICA para ningún nivel
    // (clientPoints < minPoints del nivel más bajo)
    // NO debería asignársele el nivel más bajo automáticamente
    return ClassificationResult.NONE;
}

// Definición de ClassificationResult
public class ClassificationResult {
    public static final ClassificationResult NONE = new ClassificationResult(null);
    
    private final FidelityRangeDTO range;
    
    public ClassificationResult(FidelityRangeDTO range) {
        this.range = range;
    }
    
    public static ClassificationResult of(FidelityRangeDTO range) {
        return range != null ? new ClassificationResult(range) : NONE;
    }
    
    public boolean isClassified() {
        return range != null;
    }
    
    public FidelityRangeDTO getRange() {
        return range;
    }
}
```

### Ejemplo de Comportamiento Esperado

**Escenario 1: Cliente en rango exacto**
- Niveles: Bronce [0-999], Plata [1000-4999]
- Cliente: 2500 pts
- Resultado: **Plata** (rango exacto)

**Escenario 2: Cliente en hueco entre rangos**
- Niveles: Bronce [0-1000], Oro [5000-9999]
- Cliente: 3000 pts
- Resultado: **Bronce** (fallthrough al nivel inferior más cercano)

**Escenario 3: Cliente "demasiado nuevo" (por debajo del mínimo)**
- Niveles: Plata [1000+], Oro [5000+]
- Cliente: 500 pts
- Resultado: **NONE** (no califica para ningún nivel)

**Escenario 4: Cliente por encima de todos los niveles**
- Niveles: Bronce [0-999], Plata [1000-4999], Oro [5000-9999]
- Cliente: 15000 pts
- Resultado: **Oro** (rango exacto si existe, de lo contrario NONE)

---

### Migraciones BD

#### Archivos a crear

**`backend/service-admin/src/main/resources/db/migration/V13__Create_fidelity_ranges_table.sql`**
```sql
CREATE TABLE IF NOT EXISTS fidelity_ranges (
    uid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ecommerce_id UUID NOT NULL REFERENCES ecommerces(uid) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    min_points INTEGER NOT NULL CHECK (min_points >= 0),
    max_points INTEGER NOT NULL CHECK (max_points > min_points),
    discount_percentage NUMERIC(5,2) NOT NULL CHECK (discount_percentage BETWEEN 0 AND 100),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_fidelity_ranges PRIMARY KEY (uid),
    CONSTRAINT fk_fidelity_ranges_ecommerce FOREIGN KEY (ecommerce_id) REFERENCES ecommerces(uid) ON DELETE CASCADE,
    CONSTRAINT ck_fidelity_discount_range CHECK (discount_percentage >= 0 AND discount_percentage <= 100)
);

CREATE INDEX idx_fidelity_ranges_ecommerce_id ON fidelity_ranges(ecommerce_id);
CREATE INDEX idx_fidelity_ranges_active ON fidelity_ranges(is_active);
CREATE INDEX idx_fidelity_ranges_ecommerce_active ON fidelity_ranges(ecommerce_id, is_active);
CREATE INDEX idx_fidelity_ranges_min_points ON fidelity_ranges(min_points);

-- Índice Único Parcial: Solo un nombre ACTIVO por ecommerce
CREATE UNIQUE INDEX uq_active_fidelity_name 
  ON fidelity_ranges (ecommerce_id, name) 
  WHERE (is_active IS TRUE);

-- Comentario documentando la tabla
COMMENT ON TABLE fidelity_ranges IS 'Source of Truth: Rangos de clasificación de fidelidad (Admin Service). Réplica en Engine vía RabbitMQ.';
COMMENT ON COLUMN fidelity_ranges.min_points IS 'Mínimo de puntos (incluido) para acceder a este nivel';
COMMENT ON COLUMN fidelity_ranges.max_points IS 'Máximo de puntos (incluido) para este nivel. Huecos permitidos.';
```

**`backend/service-admin/src/main/resources/db/migration/V14__Add_fidelity_permissions.sql`**
```sql
INSERT INTO permissions (code, module, action) VALUES
    ('fidelity:read', 'fidelity', 'read'),
    ('fidelity:write', 'fidelity', 'write')
ON CONFLICT (code) DO NOTHING;
```

**`backend/service-engine/src/main/resources/db/migration/V15__Create_fidelity_ranges_replica.sql`**
```sql
-- Replica table en Engine Service para Cold Start autonomy
CREATE TABLE IF NOT EXISTS fidelity_ranges (
    uid UUID PRIMARY KEY,
    ecommerce_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    min_points INTEGER NOT NULL CHECK (min_points >= 0),
    max_points INTEGER NOT NULL CHECK (max_points > min_points),
    discount_percentage NUMERIC(5,2) NOT NULL CHECK (discount_percentage BETWEEN 0 AND 100),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_fidelity_discount_range CHECK (discount_percentage >= 0 AND discount_percentage <= 100)
);

CREATE INDEX idx_fidelity_ranges_ecommerce_id ON fidelity_ranges(ecommerce_id);
CREATE INDEX idx_fidelity_ranges_active ON fidelity_ranges(is_active);
CREATE INDEX idx_fidelity_ranges_ecommerce_active ON fidelity_ranges(ecommerce_id, is_active);
CREATE INDEX idx_fidelity_ranges_min_points ON fidelity_ranges(min_points);

COMMENT ON TABLE fidelity_ranges IS 'Replica: Sincronizada desde Admin Service vía RabbitMQ. Pre-cargada en Caffeine al startup.';
```

---

## 3. LISTA DE TAREAS

### Backend (Admin Service)

- [x] Crear entidad `FidelityRangeEntity` en `domain/entities`
- [x] Crear DTOs: `CreateFidelityRangeRequest`, `UpdateFidelityRangeRequest`, `FidelityRangeResponse`
- [x] Crear `FidelityRangeRepository` (JpaRepository)
- [x] Crear validadores:
  - [x] `NoOverlapValidator` — verifica no superposición (ESTRICTA)
  - [x] `ProgressionValidator` — verifica progresión ascendente de min_points
- [x] Crear `FidelityRangeService` con lógica de validación y CRUD
- [x] Crear `FidelityRangeController` con endpoints REST
- [x] Crear domain events: `FidelityRangeCreatedEvent`, `FidelityRangeUpdatedEvent`, `FidelityRangeDeletedEvent`
- [x] Crear `FidelityRangeEventPublisher` (RabbitMQ producer)
- [x] Crear migraciones BD (`V14_`, `V15_`)
- [ ] Integrar autenticación/autorización (JWT + permisos `fidelity:read`, `fidelity:write`)
- [ ] Test unitarios de validadores (overlap, progression)
- [ ] Test unitarios de service
- [ ] Test de integración de endpoints
- [ ] Test de RabbitMQ publishing

### Engine Service (Autonomía & Cold Start)

- [x] Crear tabla réplica `fidelity_ranges` (`V14_`) — sincronizada desde Admin vía RabbitMQ
- [x] Crear `FidelityRangeListener` (RabbitMQ consumer) — sincroniza create/update/delete
- [x] Crear `FidelityRangeCache` (Caffeine) — en memoria para /calculate
- [x] Crear `FidelityRangeStartupLoader` — pre-carga caché desde BD al startup
- [x] Implementar lógica de fallthrough en clasificación (cliente en hueco → nivel inferior)
- [ ] Integrar cache en `/calculate` endpoint (para clasificación en memoria sin latencia BD)
- [ ] Test de consumidor RabbitMQ (eventos create/update/delete)
- [ ] Test de startup loader (caché pre-cargado correctamente)
- [ ] Test de clasificación con fallthrough (Cliente en rango exacto, cliente en hueco)
- [ ] Test de "cliente NO CALIFICA" (puntos < minPoints del nivel más bajo → NONE)
- [ ] Test de Cold Start: simulación reinicio del Engine

### Frontend (Opcional — para este HU no requerido, pero se incluye si hay UI de administración)

- [ ] Crear componente `FidelityRangeForm.jsx` (crear/editar)
- [ ] Crear componente `FidelityRangeList.jsx` (listar)
- [ ] Crear servicio `fidelityRangeService.js` (API calls)
- [ ] Integrar en ruta `/admin/fidelity-ranges` (protegida)
- [ ] Mensajes de validación en UI
- [ ] Test de componentes

### QA / Tests Funcionales

- [ ] Test de creación con validación exitosa
- [ ] Test de rechazo por superposición
- [ ] Test de rechazo por discontinuidad
- [ ] Test de rechazo por orden inválido
- [ ] Test de clasificación automática tras validación
- [ ] Test de actualización sin romper cadena
- [ ] Test de sincronización RabbitMQ
- [ ] Test de listado con aislamiento de tenant
- [ ] Test de soft-delete
- [ ] Test de permisos (no admin → 403)
- [ ] Test de permisos: fidelity:read y fidelity:write

### Documentación

- [ ] Actualizar README con instrucciones de configuración RabbitMQ
- [ ] Documentar algoritmo de validación
- [ ] Agregar ejemplos de cURL para endpoints
