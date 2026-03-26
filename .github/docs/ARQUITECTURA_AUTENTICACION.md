# Arquitectura de Autenticación - LOYALTY SYSTEM

## Executive Summary: Hybrid Auth Model

El service-engine tiene **dos caras involucradas en tráficos completamente distintos**:

### Management Traffic (Cara de Dashboard)
- **Actor**: Admin humano via web
- **Mecanismo**: JWT (Identity Propagation)
- **Endpoints**: `POST /api/v1/discount/config`, `POST /api/v1/discount/priority`
- **Identidad**: userId viaja en JWT claims (no en headers)

### Transaction Traffic (Cara de E-commerce)
- **Actor**: E-commerce / Shopify / VTEX (servidor externo, no usuario)
- **Mecanismo**: API Key (Service-to-Service / S2S)
- **Endpoints**: `POST /api/v1/discount/calculate`
- **Identidad**: ecommerceId vinculado a API Key en BD

**Regla de Oro**: ❌ **Eliminar X-User-ID del header inmediatamente**. Si usas JWT, el ID vive en los claims. Si usas API Key, el ID del comercio está en la BD. Un header separado es un "code smell masivo".

---

## 1. MAPA DE ENDPOINTS Y AUTENTICACIÓN

### Service-Admin (Puerto 8081)

#### AuthController - `/api/v1/auth`
| Endpoint | Método | Auth | Props | Propósito |
|----------|--------|------|-------|-----------|
| `/login` | POST | ❌ Pública | username, password | Genera JWT |
| `/logout` | POST | ✅ JWT | Authorization: Bearer {token} | Invalida sesión |
| `/me` | GET | ✅ JWT | Authorization: Bearer {token} | Datos del usuario autenticado |

#### ApiKeyController - `/api/v1/ecommerces/{ecommerceId}/api-keys`
| Endpoint | Método | Auth | Props | Propósito |
|----------|--------|------|-------|-----------|
| `/` | POST | ✅ JWT | ecommerceId | Crear API Key |
| `/` | GET | ✅ JWT | ecommerceId | Listar API Keys |
| `/{keyId}` | DELETE | ✅ JWT | ecommerceId, keyId | Eliminar API Key |

### Service-Engine (Puerto 8082) - HYBRID AUTH

#### DiscountConfigController - `/api/v1/discount`
| Endpoint | Método | Tráfico | Auth | Token | Propósito |
|----------|--------|---------|------|-------|-----------|
| `/config` | POST | **Management** | ✅ JWT | `Authorization: Bearer {JWT}` | Crear/actualizar config (Admin) |
| `/config` | GET | **Management** | ✅ JWT | `Authorization: Bearer {JWT}` | Obtener config vigente |
| `/priority` | POST | **Management** | ✅ JWT | `Authorization: Bearer {JWT}` | Guardar prioridades (Admin) |
| `/priority` | GET | **Management** | ✅ JWT | `Authorization: Bearer {JWT}` | Obtener prioridades |
| `/calculate` | POST | **Transaction** | ✅ API Key | `Authorization: Bearer {API_KEY}` | Calcular descuentos (E-commerce) |

**Nota Crítica**: No hay `X-User-ID` en headers. El userId viene en los JWT claims.

---

## 2. ANÁLISIS DE PATRONES: Hybrid Auth Model

### ❌ Patrón Actual (Incorrecto)
- Dashboard (Admin) + E-commerce API → Mismo mecanismo (API Key)
- Confusión de identidades (usuario vs. servidor)

### ✅ Patrón Propuesto (Hybrid - Correcto)

```
┌─ Admin (Web Dashboard)
│  └─ POST /api/v1/auth/login (credentials)
│     └─ Retorna JWT (userId en claims)
│        └─ Usa JWT en todos los endpoints Management
│           └─ POST /api/v1/discount/config (Management)
│           └─ POST /api/v1/discount/priority (Management)
│
└─ E-commerce (Shopify, VTEX, etc.)
   └─ Tiene API Key (vinculado a ecommerceId en BD)
      └─ Usa API Key solo en endpoint Transaction
         └─ POST /api/v1/discount/calculate (Transaction)
```

**Dos mecanismos distintos para dos tipos de tráfico**.

---

## 3. ARQUITECTURA DE IMPLEMENTACIÓN: Dual-Filter Strategy

El `SecurityConfig` de Spring debe ser **inteligente** y reconocer qué credential viene en el header:

### SecurityConfig con Dual Authentication

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private final JwtUtil jwtUtil;
    private final ApiKeyService apiKeyService;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Autorización por endpoint
            .authorizeHttpRequests(auth -> auth
                // Management endpoints: Solo JWT + rol ADMIN
                .requestMatchers(HttpMethod.POST, "/api/v1/discount/config").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/discount/priority").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/discount/config").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/discount/priority").hasRole("ADMIN")
                
                // Transaction endpoints: Solo API Key (S2S)
                .requestMatchers(HttpMethod.POST, "/api/v1/discount/calculate").authenticated()
                
                .anyRequest().authenticated()
            )
            
            // Filters en orden específico
            // 1. JWT Filter (para Management Traffic)
            .addFilterBefore(
                new JwtAuthenticationFilter(jwtUtil), 
                UsernamePasswordAuthenticationFilter.class
            )
            // 2. API Key Filter (para Transaction Traffic)
            // Se ejecuta después si JWT falla
            .addFilterBefore(
                new ApiKeyAuthenticationFilter(apiKeyService), 
                JwtAuthenticationFilter.class
            );
        
        return http.build();
    }
}
```

### JwtAuthenticationFilter (Extract Identity from Claims)

```java
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    private final JwtUtil jwtUtil;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) 
            throws ServletException, IOException {
        
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            
            try {
                // Validar token JWT
                String userId = jwtUtil.extractUserId(token);
                String username = jwtUtil.extractUsername(token);
                List<String> roles = jwtUtil.extractRoles(token);
                
                // Crear Authentication con roles
                GrantedAuthority[] authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .toArray(GrantedAuthority[]::new);
                
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(userId, null, Arrays.asList(authorities));
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("JWT validated for user: {}", username);
                
            } catch (JwtException e) {
                log.warn("JWT validation failed: {}", e.getMessage());
                // Continuar al siguiente filter (ApiKeyAuthenticationFilter)
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
```

### ApiKeyAuthenticationFilter (S2S Authentication)

```java
@Component
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    private final ApiKeyService apiKeyService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) 
            throws ServletException, IOException {
        
        // Solo procesar si JWT no autenticó
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String authHeader = request.getHeader(AUTHORIZATION_HEADER);
            
            if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                String apiKey = authHeader.substring(BEARER_PREFIX.length());
                
                if (apiKeyService.validateKey(apiKey)) {
                    // Obtener ecommerceId asociado a la API Key
                    String ecommerceId = apiKeyService.getEcommerceId(apiKey);
                    
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(ecommerceId, null, List.of());
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("API Key validated for ecommerce: {}", ecommerceId);
                }
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
```

---

## 4. IDENTITY PROPAGATION: Extraer UserId del JWT

En el controlador, **no necesitas X-User-ID en header**. Spring extrae la identidad automáticamente:

```java
@RestController
@RequestMapping("/api/v1/discount")
public class DiscountConfigController {
    
    private final DiscountConfigService discountConfigService;
    
    /**
     * Management endpoint: Crear/actualizar configuración
     * Auth: JWT (Admin)
     * Identity: userId extraído del JWT claims
     */
    @PostMapping("/config")
    public ResponseEntity<DiscountConfigResponse> updateConfig(
        @Valid @RequestBody DiscountConfigCreateRequest request,
        Authentication authentication  // ← Spring inyecta el Authentication del JWT
    ) {
        // Extraer userId del JWT (está en el principal)
        String userId = authentication.getName(); 
        
        log.info("Updating discount config. User: {}", userId);
        
        DiscountConfigResponse response = discountConfigService.updateConfig(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Transaction endpoint: Calcular descuentos
     * Auth: API Key (E-commerce S2S)
     * Identity: ecommerceId extraído de la BD
     */
    @PostMapping("/calculate")
    public ResponseEntity<DiscountCalculateResponse> calculateDiscounts(
        @Valid @RequestBody DiscountCalculateRequest request,
        Authentication authentication  // ← Spring inyecta el Authentication del API Key
    ) {
        // Extraer ecommerceId del API Key
        String ecommerceId = authentication.getName();
        
        log.info("Calculating discounts for ecommerce: {}", ecommerceId);
        
        DiscountCalculateResponse response = discountCalculationEngine.calculate(request);
        return ResponseEntity.ok(response);
    }
}
```

**Nota**: `DiscountConfigCreateRequest` NO contiene userId. Solo datos del negocio:

```java
public record DiscountConfigCreateRequest(
    BigDecimal maxDiscountLimit,
    String currencyCode
) {}
```

---

## 5. CONFIGURACIÓN: Shared JWT Secret (MVP) vs. Asymmetric Signing (Production)

### MVP: Symmetric Signing (Mismo secret en ambos servicios)

```properties
# .env
JWT_SECRET=loyalty-super-secret-key-change-in-production-${timestamp}
JWT_EXPIRY=3600
```

**Riesgo**: Si una llave se compromete, todo falla. OK para MVP, NO para producción.

### Production: Asymmetric Signing (RSA Keys)

```properties
JWT_PRIVATE_KEY=/path/to/private.key  # En service-admin
JWT_PUBLIC_KEY=/path/to/public.key    # En service-engine
```

- **Admin firma** con llave privada
- **Engine valida** con llave pública (no puede falsificar)
- Más seguro, pero más complejo

---

## 6. CRITICAL RISKS & MITIGATIONS

---

### Risk Matrix

| Risk | Severity | Mitigation | Priority |
|------|----------|-----------|----------|
| **Secret Leak (JWT_SECRET)** | CRÍTICO | AWS Secrets Manager, Vault, rotate regularly | P0 |
| **Token Expiration** | ALTO | Implement refresh token flow, handle 401 in frontend | P1 |
| **API Key Brute Force** | ALTO | Rate Limiting (Bucket4j), 10 req/min per IP | P1 |
| **Token Tampering** | MEDIO | Use asymmetric signing (RSA) en production | P2 |
| **Compromised E-commerce API Key** | MEDIO | Audit logging, immediate revocation, alert | P1 |

---

## 7. IMPLEMENTATION PLAN: Fases de Desarrollo

### Fase 1: MVP - Symmetric Signing (Esta semana)

#### 1.1 Infraestructura JWT en service-engine
- [ ] Copiar `JwtUtil` desde service-admin → service-engine
- [ ] Copiar variable `JWT_SECRET` a application.properties de engine
- [ ] Crear `JwtAuthenticationFilter` en service-engine
- [ ] Mantener `ApiKeyAuthenticationFilter` existente

#### 1.2 Actualizar SecurityConfig
- [ ] Registrar ambos filters (JWT primero, API Key después)
- [ ] Configurar roles: `/config/**` y `/priority/**` requieren `ADMIN`
- [ ] `/calculate` abierto a API Key

#### 1.3 Refactor DiscountConfigController
- [ ] ❌ Remover parámetro `@RequestHeader("X-User-ID")`
- [ ] ✅ Agregar parámetro `Authentication authentication`
- [ ] Extraer userId de `authentication.getName()`
- [ ] Test que POST funcione con JWT

#### 1.4 Tests
- [ ] Crear JwtAuthenticationFilterTest
- [ ] Actualizar DiscountConfigControllerTest para usar JWT
- [ ] Verificar que API Key sigue funcionando en `/calculate`

---

### Fase 2: Production Hardening (Próxima sprint)

- [ ] Implementar Asymmetric Signing (RSA Keys)
- [ ] Agregar Refresh Token flow
- [ ] Implementar Rate Limiting (Bucket4j) en `/calculate`
- [ ] Audit logging de cambios de configuración
- [ ] API Key rotation strategy

---

## 8. FLUJO FINAL: Escenarios Reales

### Escenario 1: Admin actualiza límite de descuentos

```bash
# 1. Admin hace login
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "pass123"}'

# Respuesta:
# {
#   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
#   "user": { "id": "550e8400-e29b-41d4-a716-446655440000", "username": "admin" }
# }

# 2. Admin usa JWT en Engine para cambiar config
curl -X POST http://localhost:8082/api/v1/discount/config \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{"max_discount_limit": 150.00, "currency_code": "USD"}'

# Respuesta:
# {
#   "uid": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
#   "maxDiscountLimit": 150.00,
#   "currencyCode": "USD",
#   "isActive": true,
#   "createdAt": "2026-03-26T20:30:00Z"
# }

# Nota: Service-engine extrae userId de JWT claims
# El admin se autentica como manager/admin en el sistema
```

### Escenario 2: Shopify calcula descuentos

```bash
# 1. Shopify (servidor) hace request con API Key
# (Não es un usuario, es un ecommerce identificado por su API Key)

curl -X POST http://localhost:8082/api/v1/discount/calculate \
  -H "Authorization: Bearer 4a67c9b6-9fd0-4398-a9fc-c7c7cad902da" \
  -H "Content-Type: application/json" \
  -d '{
    "transaction_id": "shop_123456",
    "order_value": 500.00,
    "discounts": [
      {"type": "LOYALTY_POINTS", "amount": 50.00},
      {"type": "COUPON", "amount": 75.00}
    ]
  }'

# Respuesta:
# {
#   "transaction_id": "shop_123456",
#   "original_total": 125.00,
#   "applied_total": 100.00,  # Capped a max_limit
#   "max_limit": 100.00,
#   "exceeded": true
# }

# Nota: API Key autentica el comercio, no un usuario
# El ecommerceId viene de la BD, no de headers
```

---

## 9. DECISIÓN FINAL: HYBRID AUTH MODEL

| Aspecto | Implementation |
|---------|-----------------|
| **Management Endpoints** | JWT (Identity Propagation) + Roles |
| **Transaction Endpoints** | API Key (S2S Authentication) |
| **UserId Location** | JWT Claims (no headers) |
| **EcommerceId Location** | BD (vinculado a API Key) |
| **X-User-ID Header** | ❌ ELIMINADO COMPLETAMENTE |
| **Shared Secret** | JWT_SECRET en ambos servicios (MVP) |
| **Critical Risks** | Secret Leak, Token Expiration, API Key Brute Force |
| **Next Steps** | Implementar Fase 1 (MVP) esta semana |
