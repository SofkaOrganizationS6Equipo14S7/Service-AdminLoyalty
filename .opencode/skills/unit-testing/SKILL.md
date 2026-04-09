---
name: unit-testing
description: Genera tests unitarios para backend (Java/Spring Boot) y/o frontend (React). Lee la spec y el código implementado. Requiere spec APPROVED e implementación completa.
argument-hint: "<nombre-feature> [backend|frontend|ambos]"
---

# Unit Testing

## Definition of Done — verificar al completar

- [ ] Cobertura ≥ 80% en lógica de negocio (quality gate bloqueante)
- [ ] Tests aislados — sin conexión a DB real (siempre mocks con Mockito)
- [ ] Escenario feliz + errores de negocio + validaciones cubiertos
- [ ] Los cambios no rompen contratos existentes del módulo

## Prerequisito — Lee en paralelo

```
.opencode/specs/<feature>.spec.md        (criterios de aceptación)
código implementado en backend/ y/o frontend/
.opencode/instructions/backend.instructions.md    (JUnit 5 + Mockito)
.opencode/instructions/frontend.instructions.md   (Vitest + Testing Library)
```

## Output por scope

### Backend (Java/Spring Boot) → `backend/src/test/java/`

| Archivo | Capa | Cubre |
|---------|------|-------|
| `controller/FeatureControllerTest.java` | Controller | HTTP: 200/201, 400, 401, 404 |
| `service/FeatureServiceTest.java` | Service | Lógica: happy path + errores de negocio |
| `repository/FeatureRepositoryTest.java` | Repository | Queries con @DataJpaTest |

### Frontend (React) → `frontend/src/__tests__/`

| Archivo | Cubre |
|---------|-------|
| `components/<Feature>.test.jsx` | Render + interacciones (click, submit) |
| `hooks/use<Feature>.test.js` | Estado inicial + respuesta API + error handling |
| `pages/<Feature>Page.test.jsx` | Render completo con providers |

## Patrones core

### Backend — JUnit 5 + Mockito

```java
@ExtendWith(MockitoExtension.class)
class FeatureServiceTest {

    @Mock
    private FeatureRepository repository;

    @InjectMocks
    private FeatureService service;

    @Test
    void createFeature_success() {
        // Arrange
        FeatureCreateDTO dto = new FeatureCreateDTO();
        dto.setName("Test Feature");
        
        when(repository.save(any(Feature.class))).thenAnswer(invocation -> {
            Feature f = invocation.getArgument(0);
            f.setUid("uuid-123");
            return f;
        });

        // Act
        FeatureResponseDTO result = service.create(dto, "user-uid");

        // Assert
        assertEquals("uuid-123", result.getUid());
        assertEquals("Test Feature", result.getName());
        verify(repository).save(any(Feature.class));
    }
}
```

### Frontend — Vitest + Testing Library

```js
vi.mock('../../services/featureService');
getFeatures.mockResolvedValue([{ uid: '1', name: 'Test' }]);

const { result } = renderHook(() => useFeature());
await waitFor(() => expect(result.current.data).toHaveLength(1));
```

## Restricciones

- Backend: solo en `src/test/java/`. No modificar código fuente.
- Frontend: solo en `__tests__/`. No modificar código fuente.
- **Nunca conectar a DB real** — usar `@DataJpaTest` con H2 o mocks con Mockito.
- Cobertura mínima ≥ 80% en lógica de negocio.