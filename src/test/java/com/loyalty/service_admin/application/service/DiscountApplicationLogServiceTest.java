package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.discountlog.DiscountApplicationLogResponse;
import com.loyalty.service_admin.domain.entity.DiscountApplicationLogEntity;
import com.loyalty.service_admin.domain.repository.DiscountApplicationLogRepository;
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
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiscountApplicationLogService Unit Tests")
class DiscountApplicationLogServiceTest {

    @Mock private DiscountApplicationLogRepository discountLogRepository;

    @InjectMocks
    private DiscountApplicationLogService service;

    private UUID logId;
    private UUID ecommerceId;
    private DiscountApplicationLogEntity entity;

    @BeforeEach
    void setUp() {
        logId = UUID.randomUUID();
        ecommerceId = UUID.randomUUID();

        entity = new DiscountApplicationLogEntity();
        entity.setId(logId);
        entity.setEcommerceId(ecommerceId);
        entity.setExternalOrderId("#ORDER-12345");
        entity.setOriginalAmount(new BigDecimal("100.00"));
        entity.setDiscountApplied(new BigDecimal("15.00"));
        entity.setFinalAmount(new BigDecimal("85.00"));
        entity.setAppliedRulesDetails("{\"rules\":[]}");
        entity.setCreatedAt(Instant.now());
    }

    @Test
    @DisplayName("testListDiscountLogs_NoFilters_ReturnsAll")
    void testListDiscountLogs_NoFilters_ReturnsAll() {
        // Arrange
        Page<DiscountApplicationLogEntity> page = new PageImpl<>(List.of(entity));
        when(discountLogRepository.findAll(any(Pageable.class))).thenReturn(page);

        // Act
        Page<DiscountApplicationLogResponse> result = service.listDiscountLogs(null, null, 0, 10);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(logId, result.getContent().get(0).id());
    }

    @Test
    @DisplayName("testListDiscountLogs_FilterByEcommerceId")
    void testListDiscountLogs_FilterByEcommerceId() {
        // Arrange
        Page<DiscountApplicationLogEntity> page = new PageImpl<>(List.of(entity));
        when(discountLogRepository.findByEcommerceId(eq(ecommerceId), any(Pageable.class))).thenReturn(page);

        // Act
        Page<DiscountApplicationLogResponse> result = service.listDiscountLogs(ecommerceId, null, 0, 10);

        // Assert
        assertEquals(1, result.getContent().size());
        verify(discountLogRepository).findByEcommerceId(eq(ecommerceId), any(Pageable.class));
    }

    @Test
    @DisplayName("testListDiscountLogs_FilterByExternalOrderId")
    void testListDiscountLogs_FilterByExternalOrderId() {
        // Arrange
        Page<DiscountApplicationLogEntity> page = new PageImpl<>(List.of(entity));
        when(discountLogRepository.findByExternalOrderId(eq("#ORDER-12345"), any(Pageable.class))).thenReturn(page);

        // Act
        Page<DiscountApplicationLogResponse> result = service.listDiscountLogs(null, "#ORDER-12345", 0, 10);

        // Assert
        assertEquals(1, result.getContent().size());
        verify(discountLogRepository).findByExternalOrderId(eq("#ORDER-12345"), any(Pageable.class));
    }

    @Test
    @DisplayName("testListDiscountLogs_FilterByBothEcommerceAndOrder")
    void testListDiscountLogs_FilterByBothEcommerceAndOrder() {
        // Arrange
        Page<DiscountApplicationLogEntity> page = new PageImpl<>(List.of(entity));
        when(discountLogRepository.findByEcommerceIdAndExternalOrderId(
                eq(ecommerceId), eq("#ORDER-12345"), any(Pageable.class))).thenReturn(page);

        // Act
        Page<DiscountApplicationLogResponse> result = service.listDiscountLogs(ecommerceId, "#ORDER-12345", 0, 10);

        // Assert
        assertEquals(1, result.getContent().size());
        verify(discountLogRepository).findByEcommerceIdAndExternalOrderId(
                eq(ecommerceId), eq("#ORDER-12345"), any(Pageable.class));
    }

    @Test
    @DisplayName("testListDiscountLogs_BlankExternalOrderId_TreatedAsNull")
    void testListDiscountLogs_BlankExternalOrderId_TreatedAsNull() {
        // Arrange
        Page<DiscountApplicationLogEntity> page = new PageImpl<>(List.of());
        when(discountLogRepository.findAll(any(Pageable.class))).thenReturn(page);

        // Act
        service.listDiscountLogs(null, "   ", 0, 10);

        // Assert
        verify(discountLogRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("testGetDiscountLogById_Success")
    void testGetDiscountLogById_Success() {
        // Arrange
        when(discountLogRepository.findById(logId)).thenReturn(Optional.of(entity));

        // Act
        DiscountApplicationLogResponse response = service.getDiscountLogById(logId);

        // Assert
        assertNotNull(response);
        assertEquals(logId, response.id());
        assertEquals(ecommerceId, response.ecommerceId());
        assertEquals("#ORDER-12345", response.externalOrderId());
        assertEquals(new BigDecimal("100.00"), response.originalAmount());
    }

    @Test
    @DisplayName("testGetDiscountLogById_NotFound_ThrowsResourceNotFoundException")
    void testGetDiscountLogById_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(discountLogRepository.findById(logId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> service.getDiscountLogById(logId));
    }
}
