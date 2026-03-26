---
applyTo: "backend/src/test/**/*.java,frontend/src/__tests__/**/*.{js,jsx}"
---

> **Scope**: Las reglas de backend aplican a proyectos con tests en Java/JUnit; las de frontend aplican a proyectos con tests en JS/JSX. En proyectos con otro stack, adaptar las herramientas y convenciones manteniendo los principios (independencia, aislamiento, Arrange-Act-Assert, cobertura ≥ 80%).

# Instrucciones para Archivos de Pruebas Unitarias

## Principios

- **Independencia**: cada test es 100% independiente — sin estado compartido entre tests.
- **Aislamiento**: mockear SIEMPRE dependencias externas (DB, APIs, servicios externos).
- **Claridad**: nombre del test debe describir la función bajo prueba y el escenario.
- **Cobertura**: cubrir happy path, error path y edge cases para cada unidad.

## Backend (JUnit 5 + Mockito)

### Estructura de archivos
```
backend/src/test/java/com/loyalty/
├── service/FeatureServiceTest.java
├── controller/FeatureControllerTest.java
└── repository/FeatureRepositoryTest.java
```

### Convenciones
- Nombre: `test[Metodo]_[escenario]` (ej: `testCreate_success`, `testCreate_duplicateEmail`)
- Usar `@Test` de JUnit 5.
- Usar `@Mock` y `@InjectMocks` de Mockito.
- Usar `@DataJpaTest` para tests de repositorio con H2 en memoria.

```java
// Ejemplo mínimo de test de servicio
@ExtendWith(MockitoExtension.class)
class FeatureServiceTest {

    @Mock
    private FeatureRepository repository;

    @InjectMocks
    private FeatureService service;

    @Test
    void testCreate_success() {
        when(repository.save(any(Feature.class))).thenAnswer(i -> i.getArgument(0));
        
        FeatureResponseDTO result = service.create(dto, "uid");
        
        assertNotNull(result.getUid());
    }
}
```

```java
// Ejemplo de test de controller
@WebMvcTest(FeatureController.class)
class FeatureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FeatureService service;

    @Test
    void testCreate_returns201() throws Exception {
        when(service.create(any(), anyString())).thenReturn(response);
        
        mockMvc.perform(post("/api/v1/features")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-API-Key", "test-key")
                .content("{\"name\": \"Test\"}"))
            .andExpect(status().isCreated());
    }
}
```

## Frontend (Vitest + Testing Library)

### Estructura de archivos
```
frontend/src/__tests__/
  [ComponentName].test.jsx
  use[HookName].test.js
```

### Convenciones
- Nombre del describe: nombre del componente/hook.
- Nombre del it/test: `[verbo] [qué hace] [condición]` (ej: `renders login button when unauthenticated`).
- Usar `vi.mock()` para mockear módulos externos.
- Siempre limpiar mocks con `beforeEach(() => vi.clearAllMocks())`.

```jsx
describe('LoginPage', () => {
  it('renders email input', () => {
    render(<LoginPage />);
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
  });
});
```

## Nunca hacer

- Tests que dependen del orden de ejecución.
- Llamadas reales a bases de datos o APIs externas.
- `System.out.println` permanentes en tests.
- Lógica condicional dentro de un test (if/else).
- Usar `Thread.sleep` para sincronización temporal.

---

> Para quality gates, pirámide de testing, TDD y nomenclatura Gherkin, ver `.github/docs/guidelines/dev-guidelines.md` y `.github/docs/guidelines/qa-guidelines.md`.

### Estructura AAA obligatoria
```
// Arrange — preparar datos y contexto
// Act     — ejecutar la acción bajo prueba
// Assert  — verificar el resultado esperado
```
