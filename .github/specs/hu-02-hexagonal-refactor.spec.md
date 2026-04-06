---
id: SPEC-HU02-HEXAGONAL-REFACTOR
status: DRAFT
feature: hu-02-hexagonal-refactor
created: 2026-04-06
updated: 2026-04-06
author: Backend Developer Team
version: "1.0"
related-specs:
  - SPEC-HU02-HARD-DELETE (HU-02.5 implementado)
  - SPEC-HU02-ECOMMERCE-USERS (base actual)
tdd-approach: true
architecture: hexagonal
---

# Spec: HU-02 Refactorización Completa — Hexagonal Architecture

> **Estado:** `DRAFT` (pendiente aprobación)
> **Objetivo:** Migrar TODO el CRUD de usuarios (Create, List, Get, Update, Delete) a Arquitectura Hexagonal (Ports & Adapters)
> **Dependencias:** HU-02.5 Hard Delete ya implementado con Hexagonal
> **Timeline:** 40 horas estimadas
> **Equipo:** Backend Developer

---

## 1. DESCRIPCIÓN EJECUTIVA

### Problema
La funcionalidad de Gestión de Usuarios en `UserService` actualmente sigue el patrón **Monolítico Service-Repository**:
- Controller → ServiceImpl → Repository
- Dependencias cruzadas: Servicios dependen directamente de repositorios
- Difícil de testear (requiere mocks complejos de BD)
- Dificultad para cambiar la capa de persistencia (DDD violations)

### Solución Propuesta
Refactorizar TODO el CRUD de usuarios a **Arquitectura Hexagonal** con separación clara de responsabilidades:
- **Puertos de entrada (in):** Define casos de uso (CreateUserUseCase, ListUsersUseCase, etc.)
- **Servicios:** Implementan la lógica de negocio (validaciones, autorizaciones, auditoría)
- **Puertos de salida (out):** Abstraen persistencia
- **Adapters:** Implementan la persistencia (JPA)
- **Controller:** Inyecta puertos, NO servicios ni repositorios

### Beneficios
✅ Testabilidad mejorada (servicios sin dependencias de BD)
✅ Independencia de la capa de persistencia
✅ Separación clara de responsabilidades
✅ Facilita futuras migraciones (BD, cache, events)
✅ Código autodocumentado (puertos = contratos)

---

## 2. OPERACIONES A REFACTORIZAR

### Matriz de Puertos y Servicios

| Operación | Puerto IN | Servicio | Puerto OUT | Adapter |
|-----------|-----------|----------|-----------|---------|
| **Create User** | `UserCreateUseCase` | `UserCreateService` | `UserCreatePersistencePort` | `JpaUserCreateAdapter` |
| **List Users** | `UserListUseCase` | `UserListService` | `UserListPersistencePort` | `JpaUserListAdapter` |
| **Get User by UID** | `UserGetUseCase` | `UserGetService` | `UserGetPersistencePort` | `JpaUserGetAdapter` |
| **Update User** | `UserUpdateUseCase` | `UserUpdateService` | `UserUpdatePersistencePort` | `JpaUserUpdateAdapter` |
| **Delete User** (Hard) | `UserDeleteUseCase` ✅ | `UserDeleteService` ✅ | `UserDeletePersistencePort` ✅ | `JpaUserDeleteAdapter` ✅ |

**Nota:** Hard Delete (HU-02.5) ya está implementado. Este spec extiende la arquitectura al resto del CRUD.

---

## 3. DISEÑO DETALLADO

### 3.1 Puertos de Entrada (Cases de Uso)

#### 3.1.1 UserCreateUseCase

```java
/**
 * Puerto de entrada: Crear nuevo usuario
 * Responsabilidad: Define contrato para crear usuarios con validaciones de autorización
 * Implementado por: UserCreateService
 */
public interface UserCreateUseCase {
    /**
     * Crea un nuevo usuario en el sistema
     * @param request: DTO con username, email, password, roleId, ecommerceId
     * @return UserResponse con datos del usuario creado
     * @throws AuthorizationException si el usuario actual no tiene permisos
     * @throws ConflictException si username o email ya existen
     * @throws BadRequestException si datos son inválidos (contraseña débil, SUPER_ADMIN con ecommerce, etc.)
     * @throws ResourceNotFoundException si roleId o ecommerceId no existen
     */
    UserResponse createUser(UserCreateRequest request);
}
```

**Request (DTO):**
```java
public record UserCreateRequest(
    String username,
    String email,
    String password,
    UUID roleId,
    UUID ecommerceId  // Null si role = SUPER_ADMIN
) {}
```

**Response (DTO):**
```java
public record UserResponse(
    UUID uid,
    String username,
    String email,
    UUID roleId,
    String roleName,
    UUID ecommerceId,
    boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
```

#### 3.1.2 UserListUseCase

```java
/**
 * Puerto de entrada: Listar usuarios
 * Responsabilidad: Define contrato para listar usuarios con aislamiento por ecommerce
 * Implementado por: UserListService
 */
public interface UserListUseCase {
    /**
     * Lista usuarios según el rol del usuario actual:
     * - SUPER_ADMIN: retorna todos (o filtra por ecommerceIdParam si se proporciona)
     * - STORE_ADMIN/STANDARD: retorna usuarios de su ecommerce
     * @param ecommerceIdParam: UUID opcional para filtrar por ecommerce (solo SUPER_ADMIN)
     * @return List<UserResponse> con usuarios visibles al usuario actual
     * @throws AuthorizationException si STORE_ADMIN/STANDARD intenta acceder a otro ecommerce
     */
    List<UserResponse> listUsers(UUID ecommerceIdParam);
}
```

#### 3.1.3 UserGetUseCase

```java
/**
 * Puerto de entrada: Obtener usuario por UID
 * Responsabilidad: Define contrato para recuperar datos de un usuario
 * Implementado por: UserGetService
 */
public interface UserGetUseCase {
    /**
     * Obtiene un usuario por su UID con validación de autorización
     * @param uid: UUID del usuario
     * @return UserResponse con datos del usuario
     * @throws ResourceNotFoundException si usuario no existe
     * @throws AuthorizationException si usuario actual no tiene acceso (multitenant)
     */
    UserResponse getUserByUid(UUID uid);
}
```

#### 3.1.4 UserUpdateUseCase

```java
/**
 * Puerto de entrada: Actualizar usuario
 * Responsabilidad: Define contrato para actualizar datos del usuario
 * Implementado por: UserUpdateService
 */
public interface UserUpdateUseCase {
    /**
     * Actualiza un usuario existente
     * Restricciones:
     * - No se puede cambiar el roleId (rol es inmutable)
     * - No se puede cambiar ecommerceId (tenencia es inmutable)
     * - Validar que usuario actual tiene permisos
     * @param uid: UUID del usuario a actualizar
     * @param request: DTO con campos a actualizar (username, email, solo)
     * @return UserResponse con datos actualizados
     * @throws ResourceNotFoundException si usuario no existe
     * @throws AuthorizationException si usuario actual no tiene acceso
     * @throws ConflictException si username o email ya están en uso
     * @throws BadRequestException si intenta cambiar roleId o ecommerceId
     */
    UserResponse updateUser(UUID uid, UserUpdateRequest request);
}
```

**Request (DTO):**
```java
public record UserUpdateRequest(
    String username,
    String email
) {}
```

#### 3.1.5 UserDeleteUseCase ✅ (Existente)

Ya implementado en HU-02.5. Referencia:
```java
public interface UserDeleteUseCase {
    void hardDeleteUser(UUID uid);
}
```

### 3.2 Puertos de Salida (Persistencia)

#### 3.2.1 UserCreatePersistencePort

```java
/**
 * Puerto de salida: Persistencia para crear usuarios
 * Implementado por: JpaUserCreateAdapter
 */
public interface UserCreatePersistencePort {
    /**
     * Busca un usuario por username
     * @return Optional con usuario si existe
     */
    Optional<UserEntity> findByUsername(String username);
    
    /**
     * Busca un usuario por email
     * @return Optional con usuario si existe
     */
    Optional<UserEntity> findByEmail(String email);
    
    /**
     * Guarda un nuevo usuario en la BD
     * @param user: UserEntity a guardar
     * @return UserEntity guardada con ID asignado
     */
    UserEntity saveUser(UserEntity user);
}
```

#### 3.2.2 UserListPersistencePort

```java
/**
 * Puerto de salida: Persistencia para listar usuarios
 * Implementado por: JpaUserListAdapter
 */
public interface UserListPersistencePort {
    /**
     * Retorna todos los usuarios del sistema
     */
    List<UserEntity> findAll();
    
    /**
     * Retorna usuarios de un ecommerce específico
     * @param ecommerceId: UUID del ecommerce
     */
    List<UserEntity> findByEcommerceId(UUID ecommerceId);
}
```

#### 3.2.3 UserGetPersistencePort

```java
/**
 * Puerto de salida: Persistencia para obtener usuario
 * Implementado por: JpaUserGetAdapter
 */
public interface UserGetPersistencePort {
    /**
     * Busca un usuario por su UID
     * @param uid: UUID del usuario
     * @return Optional con usuario si existe
     */
    Optional<UserEntity> findById(UUID uid);
}
```

#### 3.2.4 UserUpdatePersistencePort

```java
/**
 * Puerto de salida: Persistencia para actualizar usuario
 * Implementado por: JpaUserUpdateAdapter
 */
public interface UserUpdatePersistencePort {
    /**
     * Busca un usuario por UID
     */
    Optional<UserEntity> findById(UUID uid);
    
    /**
     * Busca usuarios por username (para validar no-duplicación)
     */
    Optional<UserEntity> findByUsername(String username);
    
    /**
     * Busca usuarios por email (para validar no-duplicación)
     */
    Optional<UserEntity> findByEmail(String email);
    
    /**
     * Guarda cambios en un usuario existente
     * @param user: UserEntity con cambios
     * @return UserEntity actualizada
     */
    UserEntity updateUser(UserEntity user);
}
```

#### 3.2.5 UserDeletePersistencePort ✅ (Existente)

```java
public interface UserDeletePersistencePort {
    Optional<UserEntity> findById(UUID uid);
    void deleteUser(UserEntity user);
}
```

### 3.3 Servicios (Implementaciones de Puertos IN)

#### 3.3.1 UserCreateService

```java
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserCreateService implements UserCreateUseCase {
    
    private final UserCreatePersistencePort persistencePort;
    private final RoleRepository roleRepository;  // Domain layer
    private final EcommerceService ecommerceService;  // Cross-domain
    private final SecurityContextHelper securityContextHelper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;
    
    @Override
    public UserResponse createUser(UserCreateRequest request) {
        // 1. Validar autorización: solo SUPER_ADMIN/STORE_ADMIN pueden crear
        String currentRole = securityContextHelper.getCurrentUserRole();
        if (!"SUPER_ADMIN".equals(currentRole) && !"STORE_ADMIN".equals(currentRole)) {
            throw new AuthorizationException("Solo SUPER_ADMIN o STORE_ADMIN pueden crear usuarios");
        }
        
        // 2. Validar roleId existe
        RoleEntity roleEntity = roleRepository.findById(request.roleId())
            .orElseThrow(() -> new BadRequestException("El roleId proporcionado no existe"));
        
        String roleName = roleEntity.getName();
        
        // 3. Validar reglas de SUPER_ADMIN / STORE_ADMIN / STANDARD
        if ("SUPER_ADMIN".equals(roleName)) {
            if (request.ecommerceId() != null) {
                throw new BadRequestException("SUPER_ADMIN no puede tener ecommerce_id asignado");
            }
        } else {
            if (request.ecommerceId() == null) {
                throw new BadRequestException(String.format("%s requiere ecommerce_id obligatorio", roleName));
            }
            ecommerceService.validateEcommerceExists(request.ecommerceId());
        }
        
        // 4. Validar STORE_ADMIN solo puede crear en su ecommerce
        if ("STORE_ADMIN".equals(currentRole)) {
            UUID currentUserEcommerceId = securityContextHelper.getCurrentUserEcommerceId();
            if (!currentUserEcommerceId.equals(request.ecommerceId())) {
                throw new AuthorizationException("Solo puede crear usuarios dentro de su ecommerce");
            }
        }
        
        // 5. Validar username/email únicos
        if (persistencePort.findByUsername(request.username()).isPresent()) {
            throw new ConflictException("El username ya existe. Use otro.");
        }
        if (persistencePort.findByEmail(request.email()).isPresent()) {
            throw new ConflictException("El email ya existe. Use otro.");
        }
        
        // 6. Validar contraseña
        if (!passwordValidator.isValid(request.password())) {
            throw new BadRequestException(passwordValidator.getErrorMessage(request.password()));
        }
        
        // 7. Crear entidad y guardar
        UserEntity user = UserEntity.builder()
            .username(request.username())
            .email(request.email())
            .passwordHash(passwordEncoder.encode(request.password()))
            .roleId(roleEntity.getId())
            .role(roleEntity)
            .ecommerceId(request.ecommerceId())
            .isActive(true)
            .build();
        
        UserEntity saved = persistencePort.saveUser(user);
        log.info("Usuario creado: uid={}, username={}, role={}", saved.getId(), saved.getUsername(), roleName);
        
        return toResponse(saved);
    }
    
    private UserResponse toResponse(UserEntity user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRoleId(),
            user.getRole().getName(),
            user.getEcommerceId(),
            user.isActive(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
```

#### 3.3.2 UserListService

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserListService implements UserListUseCase {
    
    private final UserListPersistencePort persistencePort;
    private final SecurityContextHelper securityContextHelper;
    
    @Override
    public List<UserResponse> listUsers(UUID ecommerceIdParam) {
        if (securityContextHelper.isCurrentUserSuperAdmin()) {
            // SUPER_ADMIN: retorna todos o filtra por param
            List<UserEntity> users = ecommerceIdParam != null 
                ? persistencePort.findByEcommerceId(ecommerceIdParam)
                : persistencePort.findAll();
            return users.stream().map(this::toResponse).collect(Collectors.toList());
        } else {
            // STORE_ADMIN/STANDARD: retorna su ecommerce
            UUID userEcommerceId = securityContextHelper.getCurrentUserEcommerceId();
            if (ecommerceIdParam != null && !ecommerceIdParam.equals(userEcommerceId)) {
                throw new AuthorizationException("No tiene permiso para acceder a este ecommerce");
            }
            List<UserEntity> users = persistencePort.findByEcommerceId(userEcommerceId);
            return users.stream().map(this::toResponse).collect(Collectors.toList());
        }
    }
    
    private UserResponse toResponse(UserEntity user) { /* ... */ }
}
```

#### 3.3.3 UserGetService

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserGetService implements UserGetUseCase {
    
    private final UserGetPersistencePort persistencePort;
    private final SecurityContextHelper securityContextHelper;
    
    @Override
    public UserResponse getUserByUid(UUID uid) {
        UserEntity user = persistencePort.findById(uid)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
        // Validar acceso multitenant
        if (!securityContextHelper.isCurrentUserSuperAdmin()) {
            UUID userEcommerce = securityContextHelper.getCurrentUserEcommerceId();
            if (!userEcommerce.equals(user.getEcommerceId())) {
                throw new AuthorizationException("No tiene permiso para acceder a este usuario");
            }
        }
        
        return toResponse(user);
    }
    
    private UserResponse toResponse(UserEntity user) { /* ... */ }
}
```

#### 3.3.4 UserUpdateService

```java
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserUpdateService implements UserUpdateUseCase {
    
    private final UserUpdatePersistencePort persistencePort;
    private final SecurityContextHelper securityContextHelper;
    
    @Override
    public UserResponse updateUser(UUID uid, UserUpdateRequest request) {
        UserEntity user = persistencePort.findById(uid)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
        // Validar autorización
        boolean canAct = securityContextHelper.canActOnUser(user.getEcommerceId(), uid);
        if (!canAct) {
            throw new AuthorizationException("No tiene permiso para editar este usuario");
        }
        
        // Validar que no cambien roleId (inmutable)
        // (no hay roleId en request, por diseño)
        
        // Validar uniqueness si cambió username
        if (!user.getUsername().equals(request.username())) {
            if (persistencePort.findByUsername(request.username()).isPresent()) {
                throw new ConflictException("El username ya existe");
            }
            user.setUsername(request.username());
        }
        
        // Validar uniqueness si cambió email
        if (!user.getEmail().equals(request.email())) {
            if (persistencePort.findByEmail(request.email()).isPresent()) {
                throw new ConflictException("El email ya existe");
            }
            user.setEmail(request.email());
        }
        
        UserEntity updated = persistencePort.updateUser(user);
        log.info("Usuario actualizado: uid={}", uid);
        
        return toResponse(updated);
    }
    
    private UserResponse toResponse(UserEntity user) { /* ... */ }
}
```

#### 3.3.5 UserDeleteService ✅ (Existente)

Ya implementado en HU-02.5.

### 3.4 Adapters (Implementaciones de Puertos OUT)

#### 3.4.1 JpaUserCreateAdapter

```java
@Component
@RequiredArgsConstructor
public class JpaUserCreateAdapter implements UserCreatePersistencePort {
    
    private final UserRepository userRepository;
    
    @Override
    public Optional<UserEntity> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    @Override
    public Optional<UserEntity> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    @Override
    public UserEntity saveUser(UserEntity user) {
        return userRepository.save(user);
    }
}
```

#### 3.4.2 JpaUserListAdapter

```java
@Component
@RequiredArgsConstructor
public class JpaUserListAdapter implements UserListPersistencePort {
    
    private final UserRepository userRepository;
    
    @Override
    public List<UserEntity> findAll() {
        return userRepository.findAll();
    }
    
    @Override
    public List<UserEntity> findByEcommerceId(UUID ecommerceId) {
        return userRepository.findByEcommerceId(ecommerceId);
    }
}
```

#### 3.4.3 JpaUserGetAdapter

```java
@Component
@RequiredArgsConstructor
public class JpaUserGetAdapter implements UserGetPersistencePort {
    
    private final UserRepository userRepository;
    
    @Override
    public Optional<UserEntity> findById(UUID uid) {
        return userRepository.findById(uid);
    }
}
```

#### 3.4.4 JpaUserUpdateAdapter

```java
@Component
@RequiredArgsConstructor
public class JpaUserUpdateAdapter implements UserUpdatePersistencePort {
    
    private final UserRepository userRepository;
    
    @Override
    public Optional<UserEntity> findById(UUID uid) {
        return userRepository.findById(uid);
    }
    
    @Override
    public Optional<UserEntity> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    @Override
    public Optional<UserEntity> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    @Override
    public UserEntity updateUser(UserEntity user) {
        return userRepository.save(user);
    }
}
```

#### 3.4.5 JpaUserDeleteAdapter ✅ (Existente)

Ya implementado en HU-02.5.

### 3.5 Controlador Refactorizado

```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    
    // Inyectar SOLO puertos, NUNCA ServiceImpl o Repository
    private final UserCreateUseCase userCreateUseCase;
    private final UserListUseCase userListUseCase;
    private final UserGetUseCase userGetUseCase;
    private final UserUpdateUseCase userUpdateUseCase;
    private final UserDeleteUseCase userDeleteUseCase;
    
    // POST: Crear usuario
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody UserCreateRequest request) {
        UserResponse response = userCreateUseCase.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    // GET: Listar usuarios
    @GetMapping
    public ResponseEntity<List<UserResponse>> listUsers(
        @RequestParam(required = false) UUID ecommerceId
    ) {
        List<UserResponse> users = userListUseCase.listUsers(ecommerceId);
        return ResponseEntity.ok(users);
    }
    
    // GET: Obtener usuario por UID
    @GetMapping("/{uid}")
    public ResponseEntity<UserResponse> getUserByUid(@PathVariable UUID uid) {
        UserResponse user = userGetUseCase.getUserByUid(uid);
        return ResponseEntity.ok(user);
    }
    
    // PUT: Actualizar usuario
    @PutMapping("/{uid}")
    public ResponseEntity<UserResponse> updateUser(
        @PathVariable UUID uid,
        @RequestBody UserUpdateRequest request
    ) {
        UserResponse response = userUpdateUseCase.updateUser(uid, request);
        return ResponseEntity.ok(response);
    }
    
    // DELETE: Eliminar usuario (hard delete)
    @DeleteMapping("/{uid}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID uid) {
        userDeleteUseCase.hardDeleteUser(uid);
        return ResponseEntity.noContent().build();
    }
}
```

---

## 4. ESTRUCTURA DE CARPETAS FINAL

```
src/main/java/com/loyalty/service_admin/
├── application/
│   ├── port/
│   │   ├── in/
│   │   │   ├── UserCreateUseCase.java          ⭐ NUEVO
│   │   │   ├── UserListUseCase.java            ⭐ NUEVO
│   │   │   ├── UserGetUseCase.java             ⭐ NUEVO
│   │   │   ├── UserUpdateUseCase.java          ⭐ NUEVO
│   │   │   └── UserDeleteUseCase.java          ✅ EXISTENTE
│   │   └── out/
│   │       ├── UserCreatePersistencePort.java  ⭐ NUEVO
│   │       ├── UserListPersistencePort.java    ⭐ NUEVO
│   │       ├── UserGetPersistencePort.java     ⭐ NUEVO
│   │       ├── UserUpdatePersistencePort.java  ⭐ NUEVO
│   │       └── UserDeletePersistencePort.java  ✅ EXISTENTE
│   ├── service/
│   │   ├── user/
│   │   │    ├── UserCreateService.java              ⭐ NUEVO
│   │   │    ├── UserListService.java                ⭐ NUEVO
│   │   │    ├── UserGetService.java                 ⭐ NUEVO
│   │   │    ├── UserUpdateService.java              ⭐ NUEVO
│   │   │    ├── UserDeleteService.java              ✅ EXISTENTE
│   │   │    ├── UserService.java                    ❌ OBSOLETO (eliminar post-refactor)
│   │   └── ... (otros servicios)
│   └── dto/
│       └── user/ (DTOs ya existen, consolidar)
│
├── domain/
│   ├── entity/
│   │   └── UserEntity.java
│   └── repository/
│       └── UserRepository.java (Interface JPA, NO SE TOCA)
│
├── infrastructure/
│   ├── persistence/
│   │   └── jpa/
│   │       ├── JpaUserCreateAdapter.java       ⭐ NUEVO
│   │       ├── JpaUserListAdapter.java         ⭐ NUEVO
│   │       ├── JpaUserGetAdapter.java          ⭐ NUEVO
│   │       ├── JpaUserUpdateAdapter.java       ⭐ NUEVO
│   │       └── JpaUserDeleteAdapter.java       ✅ EXISTENTE
│   └── ... (otros)
│
└── presentation/
    └── controller/
        └── UserController.java                  🔧 REFACTORIZAR
```

---

## 5. CAMBIOS EN EL CONTROLADOR

### Antes (Actual)
```java
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;  // ❌ Monolitico, depende de repo
    
    @PostMapping
    public ResponseEntity<UserResponse> createUser(...) {
        return ResponseEntity.ok(userService.createUser(...));
    }
}
```

### Después (Refactorizado)
```java
@RestController
@RequiredArgsConstructor
public class UserController {
    // ✅ Inyectar SOLO puertos
    private final UserCreateUseCase userCreateUseCase;
    private final UserListUseCase userListUseCase;
    private final UserGetUseCase userGetUseCase;
    private final UserUpdateUseCase userUpdateUseCase;
    private final UserDeleteUseCase userDeleteUseCase;
    
    @PostMapping
    public ResponseEntity<UserResponse> createUser(...) {
        return ResponseEntity.ok(userCreateUseCase.createUser(...));
    }
}
```

---

## 6. TESTING STRATEGY

### Tests Unitarios por Componente

| Componente | Tests | Técnica |
|------------|-------|---------|
| **UserCreateService** | 8 tests | Mockear UserCreatePersistencePort, RoleRepository, EcommerceService |
| **UserListService** | 4 tests | Mockear UserListPersistencePort, SecurityContextHelper |
| **UserGetService** | 3 tests | Mockear UserGetPersistencePort, SecurityContextHelper |
| **UserUpdateService** | 5 tests | Mockear UserUpdatePersistencePort, SecurityContextHelper |
| **UserDeleteService** | 8 tests ✅ | YA EXISTEN |
| **UserController** | 5 tests | Mockear todos los casos de uso |
| **Adapters (JPA)** | 5 tests | Mockear UserRepository |

**Total:** 38+ tests unitarios

### Execution Plan
1. **Fase 1:** Crear puertos + servicios + tests unitarios (TDD)
2. **Fase 2:** Crear adapters + tests de adapter
3. **Fase 3:** Refactorizar controlador
4. **Fase 4:** Integración: verificar endpoints funcionan
5. **Fase 5:** Eliminar UserService.java (obsoleto)

---

## 7. DEPENDENCIAS EXTERNAS MAPEADAS

| Dependencia | Usado Por | Patrón |
|-------------|-----------|--------|
| **UserRepository** | Adapters | Direct (JPA) |
| **RoleRepository** | UserCreateService | Importado (Cross-domain) |
| **EcommerceService** | UserCreateService | Importado (Cross-domain) |
| **SecurityContextHelper** | Todos los servicios | Importado (Infrastructure) |
| **BCryptPasswordEncoder** | UserCreateService | Importado (Infrastructure) |
| **PasswordValidator** | UserCreateService | Importado (Infrastructure) |
| **AuditService** | UserDeleteService | Importado (Cross-domain) |

**Nota:** No se introduce NINGUNA nueva dependencia. Solo se inyectan puertos en el controlador.

---

## 8. RIESGOS Y MITIGACIONES

### Riesgo 1: Regresión Funcional (ALTO)
**Descripción:** El refactor rompe funcionalidad existente
**Mitigación:**
- ✅ Tests unitarios para cada operación (TDD)
- ✅ Pruebas de integración post-refactor
- ✅ Rollback a UserService.java hasta de aprobación

### Riesgo 2: Migración de Datos (BAJO)
**Descripción:** Cambios de estructura en BD durante refactor
**Mitigación:**
- ✅ NO cambiar DDL (schema) durante refactor
- ✅ UserEntity y UserRepository siguen igual

### Riesgo 3: Complejidad de Inyección (MEDIO)
**Descripción:** Muchos puertos en controller pueden ser confuso
**Mitigación:**
- ✅ Documentación clara con @RequiredArgsConstructor
- ✅ Naming consistente: {Operación}UseCase

### Riesgo 4: Performance (BAJO)
**Descripción:** Más capas = más overhead
**Mitigación:**
- ✅ Servicios + Adapters son thin wrappers (sin procesamiento extra)
- ✅ No hay impacto measurable en latency

---

## 9. TIMELINE ESTIMADA

| Fase | Tarea | Horas | Dependencias |
|------|-------|-------|--------------|
| 1 (Lunes) | Crear puertos IN + DTOs | 4 | None |
| 1 (Lunes) | Crear puertos OUT | 3 | Puertos IN |
| 2 (Martes) | Implementar UserCreate/List/Get/Update Services | 8 | Puertos |
| 2 (Miércoles) | Crear Adapters JPA para CRUD | 4 | Servicios |
| 3 (Jueves) | Escribir tests unitarios (TDD style) | 10 | Servicios + Adapters |
| 3 (Viernes) | Refactorizar UserController | 2 | Tests passing |
| 4 (Viernes PM) | Pruebas de integración + debugging | 4 | Controlador |
| 5 (Siguiente semana) | Eliminar UserService.java obsoleto + limpieza | 1 | Todo funcionando |
| **Total** | | **36 horas** | |

**Estimación ampliada a 40 horas considerando:**
- 2 horas debugging y ajustes inesperados
- 2 horas documentación y code review

---

## 10. LISTA DE TAREAS (CHECKLIST)

### Fase 1: Diseño de Puertos

- [ ] Crear `UserCreateUseCase.java` (port/in)
- [ ] Crear `UserListUseCase.java` (port/in)
- [ ] Crear `UserGetUseCase.java` (port/in)
- [ ] Crear `UserUpdateUseCase.java` (port/in)
- [ ] Crear `UserCreatePersistencePort.java` (port/out)
- [ ] Crear `UserListPersistencePort.java` (port/out)
- [ ] Crear `UserGetPersistencePort.java` (port/out)
- [ ] Crear `UserUpdatePersistencePort.java` (port/out)
- [ ] Consolidar DTOs (UserCreateRequest, UserUpdateRequest, UserResponse, etc.)

### Fase 2: Implementación de Servicios

- [ ] Implementar `UserCreateService` con todas las validaciones
- [ ] Implementar `UserListService` con aislamiento multitenant
- [ ] Implementar `UserGetService` con autorización
- [ ] Implementar `UserUpdateService` con validaciones
- [ ] Documentar cada servicio con javadoc

### Fase 3: Implementación de Adapters

- [ ] Crear `JpaUserCreateAdapter`
- [ ] Crear `JpaUserListAdapter`
- [ ] Crear `JpaUserGetAdapter`
- [ ] Crear `JpaUserUpdateAdapter`
- [ ] Verificar que UserRepository no requiere cambios

### Fase 4: Tests Unitarios

- [ ] Tests para UserCreateService (8 casos)
- [ ] Tests para UserListService (4 casos)
- [ ] Tests para UserGetService (3 casos)
- [ ] Tests para UserUpdateService (5 casos)
- [ ] Tests para Adapters (5 casos, mocking UserRepository)
- [ ] Verificar cobertura >95%

### Fase 5: Refactor del Controlador

- [ ] Actualizar UserController para inyectar 5 puertos
- [ ] Eliminar inyección de UserService en controller
- [ ] Mantener endpoints igual (POST, GET, GET/{uid}, PUT, DELETE)
- [ ] Actualizar javadoc

### Fase 6: Limpieza Post-Refactor

- [ ] Eliminar `UserService.java` obsoleto (SOLO después de aprobación)
- [ ] Limpiar imports no usados
- [ ] Actualizar arquivos que referenciaban UserService
- [ ] Ejecutar `mvn clean test` completo

### Fase 7: Validación

- [ ] Todos los 38+ tests pasan ✅
- [ ] No hay regresiones funcionales ✅
- [ ] Endpoints responden igual ✅
- [ ] Code review aprobado ✅
- [ ] Documentación actualizada ✅

---

## 11. CRITERIOS DE ACEPTACIÓN

### Arquitectura
- ✅ Todos los puertos (in/out) están implementados
- ✅ Todos los servicios implementan sus puertos correspondientes
- ✅ Todos los adapters implementan sus puertos OUT
- ✅ Controller inyecta SOLO puertos, NO servicios ni repos
- ✅ Zero dependencies de domain → infrastructure

### Testing
- ✅ 38+ tests unitarios, todos GREEN
- ✅ Code coverage >95% en servicios
- ✅ No hay flakiness en tests
- ✅ Tests ejecutan en <10 segundos

### Funcionalidad
- ✅ POST /api/v1/users: crear usuario funciona idéntico
- ✅ GET /api/v1/users: listar usuarios funciona idéntico
- ✅ GET /api/v1/users/{uid}: obtener usuario funciona idéntico
- ✅ PUT /api/v1/users/{uid}: actualizar usuario funciona idéntico
- ✅ DELETE /api/v1/users/{uid}: eliminar usuario funciona idéntico (hard delete)
- ✅ Todas las validaciones y autorizaciones siguen siendo correctas
- ✅ Multitenant isolation preservado

### Code Quality
- ✅ No warnings en compilación
- ✅ JavaDoc completo en puertos y servicios
- ✅ Naming consistente con proyecto
- ✅ Commits atómicos (un commit por archivo o operación lógica)
- ✅ Mensajes de commit descriptivos

---

## 12. CAMBIOS POSTERIORES (NO incluidos en este refactor)

- [ ] Migración a eventos de dominio (Domain Events) — FUTURE
- [ ] Caching de usuários — FUTURE
- [ ] GraphQL resolver de usuarios — FUTURE
- [ ] Cambio de persistencia a Redis/MongoDB — FEASIBLE después del refactor

---

## 13. REGRESIÓN CHECKING

**¿Cuándo rollback a UserService.java?**
- Si los tests no alcanzan >95% coverage
- Si hay regresiones en endpoints
- Si refactor toma >50 horas (stop loss)

**¿Cuándo marcar como APPROVED?**
- Quando tests pasan y coverage es >95%
- Quundo code review aprobado
- Cuando pruebas de integración funcionan

---

## 14. APROBADORES Y STAKEHOLDERS

| Rol | Aprobador | Responsabilidad |
|-----|-----------|-----------------|
| **Architecture** | Backend Lead | Validar hexagonal design |
| **Code Review** | Tech Lead | Revisar implementación |
| **QA** | QA Engineer | Validar tests |
| **Product** | N/A | Feature no es visible para usuario |

---

## PRÓXIMOS PASOS

### Para APROBACIÓN de spec:
1. ✅ Reviewer lee spec completa
2. ✅ Reviewer aprueba o solicita cambios
3. ✅ Spec pasa a status APPROVED
4. ✅ Backend Developer comienza Fase 1

### Para IMPLEMENTACIÓN:
Una vez APPROVED, seguir checklist de tareas secuencialmente

---

## REFERENCIAS

- [Hexagonal Architecture](../.github/requirements/hexagonal-architecture.md)
- [Backend Instructions](../.github/instructions/backend.instructions.md)
- [SPEC-HU02-HARD-DELETE](./hard-delete-usuarios.spec.md) (Hard Delete implementado)
- [SPEC-HU02-ECOMMERCE-USERS](./hu-02-ecommerce-users.spec.md) (base)
