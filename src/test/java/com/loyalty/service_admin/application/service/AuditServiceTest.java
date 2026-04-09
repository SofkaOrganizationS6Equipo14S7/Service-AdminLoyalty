package com.loyalty.service_admin.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyalty.service_admin.domain.entity.AuditLogEntity;
import com.loyalty.service_admin.domain.entity.RoleEntity;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.domain.repository.AuditLogRepository;
import com.loyalty.service_admin.infrastructure.security.SecurityContextHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditService Unit Tests")
class AuditServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private SecurityContextHelper securityContextHelper;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private AuditService auditService;

    private UUID userId;
    private UUID actorUid;
    private UserEntity user;
    private ArgumentCaptor<AuditLogEntity> auditCaptor;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        userId = UUID.randomUUID();
        actorUid = UUID.randomUUID();
        auditCaptor = ArgumentCaptor.forClass(AuditLogEntity.class);

        user = new UserEntity();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setEcommerceId(UUID.randomUUID());
        RoleEntity role = new RoleEntity();
        role.setName("STORE_ADMIN");
        user.setRole(role);

        // ObjectMapper mock by default returns valid JSON (lenient to avoid unused stubbing in exception tests)
        lenient().when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("{\"key\":\"value\"}");
    }

    @Test
    @DisplayName("testAuditProfileUpdate_SavesCorrectEntity")
    void testAuditProfileUpdate_SavesCorrectEntity() {
        // Act
        auditService.auditProfileUpdate(userId, "new@email.com");

        // Assert
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLogEntity captured = auditCaptor.getValue();
        assertEquals(userId, captured.getUserId());
        assertEquals("PROFILE_UPDATE", captured.getAction());
        assertEquals("USER_PROFILE", captured.getEntityName());
        assertEquals(userId, captured.getEntityId());
    }

    @Test
    @DisplayName("testAuditPasswordChange_SavesCorrectEntity")
    void testAuditPasswordChange_SavesCorrectEntity() {
        // Act
        auditService.auditPasswordChange(userId, "Password changed by admin");

        // Assert
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLogEntity captured = auditCaptor.getValue();
        assertEquals("PASSWORD_CHANGE", captured.getAction());
        assertEquals("USER_PASSWORD", captured.getEntityName());
    }

    @Test
    @DisplayName("testAuditRoleChange_SavesOldAndNewValues")
    void testAuditRoleChange_SavesOldAndNewValues() {
        // Act
        auditService.auditRoleChange(userId, "STORE_USER", "STORE_ADMIN");

        // Assert
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLogEntity captured = auditCaptor.getValue();
        assertEquals("ROLE_CHANGE", captured.getAction());
        assertEquals("USER_ROLE", captured.getEntityName());
        assertNotNull(captured.getOldValue());
        assertNotNull(captured.getNewValue());
    }

    @Test
    @DisplayName("testAuditEcommerceChange_SavesOldAndNewValues")
    void testAuditEcommerceChange_SavesOldAndNewValues() {
        // Arrange
        UUID oldEcommerce = UUID.randomUUID();
        UUID newEcommerce = UUID.randomUUID();

        // Act
        auditService.auditEcommerceChange(userId, oldEcommerce, newEcommerce);

        // Assert
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLogEntity captured = auditCaptor.getValue();
        assertEquals("ECOMMERCE_CHANGE", captured.getAction());
        assertEquals("USER_ECOMMERCE", captured.getEntityName());
    }

    @Test
    @DisplayName("testAuditUserDeletion_SavesCorrectEntity")
    void testAuditUserDeletion_SavesCorrectEntity() {
        // Act
        auditService.auditUserDeletion(user, actorUid);

        // Assert
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLogEntity captured = auditCaptor.getValue();
        assertEquals("USER_DELETE", captured.getAction());
        assertEquals("USER", captured.getEntityName());
        assertEquals(user.getEcommerceId(), captured.getEcommerceId());
    }

    @Test
    @DisplayName("testAuditUserCreation_SavesCorrectEntity")
    void testAuditUserCreation_SavesCorrectEntity() {
        // Act
        auditService.auditUserCreation(user, actorUid);

        // Assert
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLogEntity captured = auditCaptor.getValue();
        assertEquals("USER_CREATE", captured.getAction());
        assertEquals("USER", captured.getEntityName());
    }

    @Test
    @DisplayName("testAuditUserUpdate_SavesCorrectEntity")
    void testAuditUserUpdate_SavesCorrectEntity() {
        // Act
        auditService.auditUserUpdate(user, actorUid);

        // Assert
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLogEntity captured = auditCaptor.getValue();
        assertEquals("USER_UPDATE", captured.getAction());
        assertEquals("USER", captured.getEntityName());
    }

    @Test
    @DisplayName("testAuditProfileUpdate_NullEmail_HandledGracefully")
    void testAuditProfileUpdate_NullEmail_HandledGracefully() {
        // Act & Assert - should not throw
        assertDoesNotThrow(() -> auditService.auditProfileUpdate(userId, null));
        verify(auditLogRepository).save(any(AuditLogEntity.class));
    }

    @Test
    @DisplayName("testAuditRoleChange_NullValues_HandledGracefully")
    void testAuditRoleChange_NullValues_HandledGracefully() {
        // Act & Assert
        assertDoesNotThrow(() -> auditService.auditRoleChange(userId, null, null));
        verify(auditLogRepository).save(any(AuditLogEntity.class));
    }

    @Test
    @DisplayName("testToJson_SerializationFailure_ReturnsFallback")
    void testToJson_SerializationFailure_ReturnsFallback() throws JsonProcessingException {
        // Arrange
        when(objectMapper.writeValueAsString(any(Map.class)))
                .thenThrow(new JsonProcessingException("serialization error") {});

        // Act
        auditService.auditProfileUpdate(userId, "test@email.com");

        // Assert - should still save with fallback JSON
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLogEntity captured = auditCaptor.getValue();
        assertEquals("{\"error\":\"serialization_failed\"}", captured.getNewValue());
    }
}
