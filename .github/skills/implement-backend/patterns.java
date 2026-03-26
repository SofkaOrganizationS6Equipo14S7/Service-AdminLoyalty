---
name: patterns.java
description: Patrones de código Java para Clean Architecture en Spring Boot
---

# Patrones de Referencia — Código Java

Este archivo contiene ejemplos de código de referencia para implementar features.

---

## Domain Layer

### Entity (JPA)

```java
package com.loyalty.service_admin.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_keys", indexes = {
    @Index(name = "idx_key_string", columnList = "key_string", unique = true),
    @Index(name = "idx_ecommerce_id", columnList = "ecommerce_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "key_string", unique = true, nullable = false, length = 36)
    private String keyString;
    
    @Column(name = "ecommerce_id", nullable = false)
    private UUID ecommerceId;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
```

### Repository Interface

```java
package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.ApiKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, UUID> {
    Optional<ApiKeyEntity> findByKeyString(String keyString);
    List<ApiKeyEntity> findByEcommerceId(UUID ecommerceId);
}
```

---

## Application Layer

### DTO (Java Records)

```java
package com.loyalty.service_admin.application.dto;

import java.time.Instant;

public record ApiKeyResponse(
    String uid,
    String maskedKey,
    String ecommerceId,
    Instant createdAt,
    Instant updatedAt
) {}
```

```java
package com.loyalty.service_admin.application.dto;

public record ApiKeyCreateRequest() {}
```

### Service

```java
package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.ApiKeyResponse;
import com.loyalty.service_admin.domain.entity.ApiKeyEntity;
import com.loyalty.service_admin.domain.repository.ApiKeyRepository;
import com.loyalty.service_admin.infrastructure.rabbitmq.ApiKeyEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
public class ApiKeyService {
    
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyEventPublisher apiKeyEventPublisher;
    private final EcommerceService ecommerceService;
    
    public ApiKeyService(
        ApiKeyRepository apiKeyRepository,
        ApiKeyEventPublisher apiKeyEventPublisher,
        EcommerceService ecommerceService
    ) {
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyEventPublisher = apiKeyEventPublisher;
        this.ecommerceService = ecommerceService;
    }
    
    @Transactional
    public ApiKeyResponse createApiKey(UUID ecommerceId) {
        ecommerceService.validateEcommerceExists(ecommerceId);
        
        String keyString = UUID.randomUUID().toString();
        
        ApiKeyEntity entity = new ApiKeyEntity();
        entity.setKeyString(keyString);
        entity.setEcommerceId(ecommerceId);
        
        ApiKeyEntity saved = apiKeyRepository.save(entity);
        
        apiKeyEventPublisher.publishApiKeyCreated(...);
        
        log.info("API Key created for ecommerce: {}", ecommerceId);
        
        return toApiKeyResponse(saved);
    }
    
    private ApiKeyResponse toApiKeyResponse(ApiKeyEntity entity) {
        return new ApiKeyResponse(
            entity.getId().toString(),
            maskKey(entity.getKeyString()),
            entity.getEcommerceId().toString(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
    
    private String maskKey(String keyString) {
        if (keyString == null || keyString.length() < 4) return "****";
        return "****" + keyString.substring(keyString.length() - 4);
    }
}
```

---

## Infrastructure Layer

### Custom Exception

```java
package com.loyalty.service_admin.infrastructure.exception;

public class ApiKeyNotFoundException extends RuntimeException {
    public ApiKeyNotFoundException(String message) {
        super(message);
    }
}
```

### Exception Handler

```java
package com.loyalty.service_admin.infrastructure.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ApiKeyNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleApiKeyNotFound(ApiKeyNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
            "error", "API_KEY_NOT_FOUND",
            "message", ex.getMessage(),
            "timestamp", Instant.now().toString()
        ));
    }
}
```

---

## Presentation Layer

### Controller

```java
package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.ApiKeyCreateRequest;
import com.loyalty.service_admin.application.dto.ApiKeyResponse;
import com.loyalty.service_admin.application.service.ApiKeyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ecommerces/{ecommerceId}/api-keys")
@Slf4j
public class ApiKeyController {
    
    private final ApiKeyService apiKeyService;
    
    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }
    
    @PostMapping
    public ResponseEntity<ApiKeyResponse> createApiKey(
        @PathVariable UUID ecommerceId,
        @RequestBody(required = false) ApiKeyCreateRequest request
    ) {
        log.info("Creating API Key for ecommerce: {}", ecommerceId);
        ApiKeyResponse response = apiKeyService.createApiKey(ecommerceId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

---

## Reglas

- DTOs siempre Java Records
- Constructor Injection obligatorio
- Moneda siempre BigDecimal
- Timestamps siempre Instant