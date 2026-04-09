package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.auditlog.AuditLogResponse;
import com.loyalty.service_admin.domain.entity.AuditLogEntity;
import com.loyalty.service_admin.domain.repository.AuditLogRepository;
import com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogService Unit Tests")
class AuditLogServiceTest {

    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    private UUID logId;
    private UUID ecommerceId;
    private AuditLogEntity entity;

    @BeforeEach
    void setUp() {
        logId = UUID.randomUUID();
        ecommerceId = UUID.randomUUID();

        entity = AuditLogEntity.builder()
                .id(logId)
                .userId(UUID.randomUUID())
                .ecommerceId(ecommerceId)
                .action("USER_CREATE")
                .entityName("USER")
                .entityId(UUID.randomUUID())
                .oldValue("{}")
                .newValue("{\"username\":\"test\"}")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("testListAuditLogs_NoFilters_ReturnsAll")
    void testListAuditLogs_NoFilters_ReturnsAll() {
        // Arrange
        Page<AuditLogEntity> page = new PageImpl<>(List.of(entity));
        when(auditLogRepository.findAll(any(Pageable.class))).thenReturn(page);

        // Act
        Page<AuditLogResponse> result = auditLogService.listAuditLogs(null, null, 0, 10);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(logId, result.getContent().get(0).id());
        verify(auditLogRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("testListAuditLogs_FilterByEntityName")
    void testListAuditLogs_FilterByEntityName() {
        // Arrange
        Page<AuditLogEntity> page = new PageImpl<>(List.of(entity));
        when(auditLogRepository.findByEntityName(eq("USER"), any(Pageable.class))).thenReturn(page);

        // Act
        Page<AuditLogResponse> result = auditLogService.listAuditLogs("USER", null, 0, 10);

        // Assert
        assertEquals(1, result.getContent().size());
        verify(auditLogRepository).findByEntityName(eq("USER"), any(Pageable.class));
    }

    @Test
    @DisplayName("testListAuditLogs_FilterByEcommerceId")
    void testListAuditLogs_FilterByEcommerceId() {
        // Arrange
        Page<AuditLogEntity> page = new PageImpl<>(List.of(entity));
        when(auditLogRepository.findByEcommerceId(eq(ecommerceId), any(Pageable.class))).thenReturn(page);

        // Act
        Page<AuditLogResponse> result = auditLogService.listAuditLogs(null, ecommerceId, 0, 10);

        // Assert
        assertEquals(1, result.getContent().size());
        verify(auditLogRepository).findByEcommerceId(eq(ecommerceId), any(Pageable.class));
    }

    @Test
    @DisplayName("testListAuditLogs_FilterByBothEntityNameAndEcommerceId")
    void testListAuditLogs_FilterByBothEntityNameAndEcommerceId() {
        // Arrange
        Page<AuditLogEntity> page = new PageImpl<>(List.of(entity));
        when(auditLogRepository.findByEntityNameAndEcommerceId(eq("USER"), eq(ecommerceId), any(Pageable.class)))
                .thenReturn(page);

        // Act
        Page<AuditLogResponse> result = auditLogService.listAuditLogs("USER", ecommerceId, 0, 10);

        // Assert
        assertEquals(1, result.getContent().size());
        verify(auditLogRepository).findByEntityNameAndEcommerceId(eq("USER"), eq(ecommerceId), any(Pageable.class));
    }

    @Test
    @DisplayName("testListAuditLogs_BlankEntityName_TreatedAsNull")
    void testListAuditLogs_BlankEntityName_TreatedAsNull() {
        // Arrange
        Page<AuditLogEntity> page = new PageImpl<>(List.of());
        when(auditLogRepository.findAll(any(Pageable.class))).thenReturn(page);

        // Act
        Page<AuditLogResponse> result = auditLogService.listAuditLogs("   ", null, 0, 10);

        // Assert
        verify(auditLogRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("testGetAuditLogById_Success")
    void testGetAuditLogById_Success() {
        // Arrange
        when(auditLogRepository.findById(logId)).thenReturn(Optional.of(entity));

        // Act
        AuditLogResponse response = auditLogService.getAuditLogById(logId);

        // Assert
        assertNotNull(response);
        assertEquals(logId, response.id());
        assertEquals("USER_CREATE", response.action());
        assertEquals("USER", response.entityName());
    }

    @Test
    @DisplayName("testGetAuditLogById_NotFound_ThrowsResourceNotFoundException")
    void testGetAuditLogById_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(auditLogRepository.findById(logId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> auditLogService.getAuditLogById(logId));
    }
}
