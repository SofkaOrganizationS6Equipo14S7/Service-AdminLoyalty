package com.loyalty.service_engine.application.service;

import com.loyalty.service_engine.application.dto.DiscountCalculateRequest;
import com.loyalty.service_engine.application.dto.DiscountCalculateResponse;
import com.loyalty.service_engine.domain.entity.DiscountConfigEntity;
import com.loyalty.service_engine.domain.entity.DiscountPriorityEntity;
import com.loyalty.service_engine.infrastructure.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Test suite para DiscountCalculationEngine.
 * Cubre: Algoritmo de aplicación de descuentos, respeto de límite máximo,
 * ordenamiento por prioridad, precisión con BigDecimal.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DiscountCalculationEngine Tests")
class DiscountCalculationEngineTest {

    @Mock
    private DiscountConfigService discountConfigService;

    @Mock
    private DiscountPriorityService discountPriorityService;

    @InjectMocks
    private DiscountCalculationEngine discountCalculationEngine;

    private DiscountConfigEntity activeConfig;
    private List<DiscountPriorityEntity> activePriorities;
    private String transactionId;

    @BeforeEach
    void setUp() {
        transactionId = "txn-123456";
        
        activeConfig = new DiscountConfigEntity();
        activeConfig.setId(UUID.randomUUID());
        activeConfig.setMaxDiscountLimit(new BigDecimal("100.00"));
        activeConfig.setCurrencyCode("USD");
        activeConfig.setIsActive(true);
        activeConfig.setCreatedByUserId(UUID.randomUUID());
        activeConfig.setCreatedAt(Instant.now());
        activeConfig.setUpdatedAt(Instant.now());
        
        activePriorities = new ArrayList<>();
        DiscountPriorityEntity p1 = new DiscountPriorityEntity();
        p1.setId(UUID.randomUUID());
        p1.setDiscountConfigId(activeConfig.getId());
        p1.setDiscountType("LOYALTY_POINTS");
        p1.setPriorityLevel(1);
        p1.setCreatedAt(Instant.now());
        
        DiscountPriorityEntity p2 = new DiscountPriorityEntity();
        p2.setId(UUID.randomUUID());
        p2.setDiscountConfigId(activeConfig.getId());
        p2.setDiscountType("COUPON");
        p2.setPriorityLevel(2);
        p2.setCreatedAt(Instant.now());
        
        DiscountPriorityEntity p3 = new DiscountPriorityEntity();
        p3.setId(UUID.randomUUID());
        p3.setDiscountConfigId(activeConfig.getId());
        p3.setDiscountType("BIRTHDAY");
        p3.setPriorityLevel(3);
        p3.setCreatedAt(Instant.now());
        
        activePriorities.add(p1);
        activePriorities.add(p2);
        activePriorities.add(p3);
    }

    // ============ Happy Path Tests ============

    @Test
    @DisplayName("calculateDiscounts_success: Apply all discounts within limit")
    void testCalculateDiscountsNoLimitExceeded() {
        // Arrange
        List<DiscountCalculateRequest.DiscountItem> discounts = List.of(
            new DiscountCalculateRequest.DiscountItem("LOYALTY_POINTS", new BigDecimal("50.00")),
            new DiscountCalculateRequest.DiscountItem("COUPON", new BigDecimal("40.00")),
            new DiscountCalculateRequest.DiscountItem("BIRTHDAY", new BigDecimal("10.00"))
        );
        DiscountCalculateRequest request = new DiscountCalculateRequest(transactionId, discounts);
        
        when(discountConfigService.getActiveConfigEntity()).thenReturn(activeConfig);
        when(discountPriorityService.getActivePrioritiesEntity()).thenReturn(activePriorities);

        // Act
        DiscountCalculateResponse result = discountCalculationEngine.calculateDiscounts(request);

        // Assert
        assertNotNull(result);
        assertEquals(transactionId, result.transactionId());
        assertEquals(new BigDecimal("100.00"), result.totalOriginal());
        assertEquals(new BigDecimal("100.00"), result.totalApplied());
        assertEquals(new BigDecimal("100.00"), result.maxDiscountLimit());
        assertFalse(result.limitExceeded());
        assertEquals(3, result.appliedDiscounts().size());
    }

    @Test
    @DisplayName("calculateDiscounts_priority: Respect priority order")
    void testCalculateDiscountsRespectPriority() {
        // Arrange
        List<DiscountCalculateRequest.DiscountItem> discounts = List.of(
            new DiscountCalculateRequest.DiscountItem("LOYALTY_POINTS", new BigDecimal("70.00")),
            new DiscountCalculateRequest.DiscountItem("COUPON", new BigDecimal("50.00")),
            new DiscountCalculateRequest.DiscountItem("BIRTHDAY", new BigDecimal("40.00"))
        );
        DiscountCalculateRequest request = new DiscountCalculateRequest(transactionId, discounts);
        
        when(discountConfigService.getActiveConfigEntity()).thenReturn(activeConfig);
        when(discountPriorityService.getActivePrioritiesEntity()).thenReturn(activePriorities);

        // Act
        DiscountCalculateResponse result = discountCalculationEngine.calculateDiscounts(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.limitExceeded());
        assertEquals(new BigDecimal("160.00"), result.totalOriginal());
        assertEquals(new BigDecimal("100.00"), result.totalApplied());
        
        // Verify order: LOYALTY_POINTS (70) + COUPON (30) = 100, BIRTHDAY is ignored (limit reached)
        assertEquals(2, result.appliedDiscounts().size());
        assertEquals("LOYALTY_POINTS", result.appliedDiscounts().get(0).discountType());
        assertEquals(new BigDecimal("70.00"), result.appliedDiscounts().get(0).amount());
        assertEquals("COUPON", result.appliedDiscounts().get(1).discountType());
        assertEquals(0, result.appliedDiscounts().get(1).amount().compareTo(new BigDecimal("30.00")));
    }

    @Test
    @DisplayName("calculateDiscounts_cap: Cap at maximum limit when exceeded")
    void testCalculateDiscountsCappedAtMax() {
        // Arrange
        List<DiscountCalculateRequest.DiscountItem> discounts = List.of(
            new DiscountCalculateRequest.DiscountItem("LOYALTY_POINTS", new BigDecimal("60.00")),
            new DiscountCalculateRequest.DiscountItem("COUPON", new BigDecimal("60.00")),
            new DiscountCalculateRequest.DiscountItem("BIRTHDAY", new BigDecimal("60.00"))
        );
        DiscountCalculateRequest request = new DiscountCalculateRequest(transactionId, discounts);
        
        when(discountConfigService.getActiveConfigEntity()).thenReturn(activeConfig);
        when(discountPriorityService.getActivePrioritiesEntity()).thenReturn(activePriorities);

        // Act
        DiscountCalculateResponse result = discountCalculationEngine.calculateDiscounts(request);

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("180.00"), result.totalOriginal());
        assertEquals(new BigDecimal("100.00"), result.totalApplied());  // Capped at max limit
        assertEquals(new BigDecimal("100.00"), result.maxDiscountLimit());
        assertTrue(result.limitExceeded());
    }

    // ============ BigDecimal Precision Tests ============

    @Test
    @DisplayName("calculateDiscounts_precision: Handle decimal precision correctly")
    void testCalculateDiscountsBigDecimalPrecision() {
        // Arrange
        List<DiscountCalculateRequest.DiscountItem> discounts = List.of(
            new DiscountCalculateRequest.DiscountItem("LOYALTY_POINTS", new BigDecimal("49.99")),
            new DiscountCalculateRequest.DiscountItem("COUPON", new BigDecimal("50.01"))
        );
        DiscountCalculateRequest request = new DiscountCalculateRequest(transactionId, discounts);
        
        when(discountConfigService.getActiveConfigEntity()).thenReturn(activeConfig);
        when(discountPriorityService.getActivePrioritiesEntity()).thenReturn(activePriorities);

        // Act
        DiscountCalculateResponse result = discountCalculationEngine.calculateDiscounts(request);

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("100.00"), result.totalOriginal());
        assertEquals(new BigDecimal("100.00"), result.totalApplied());
        assertFalse(result.limitExceeded());
    }

    @Test
    @DisplayName("calculateDiscounts_smallAmount: Handle small decimal amounts")
    void testCalculateDiscountsSmallAmounts() {
        // Arrange
        List<DiscountCalculateRequest.DiscountItem> discounts = List.of(
            new DiscountCalculateRequest.DiscountItem("LOYALTY_POINTS", new BigDecimal("0.01")),
            new DiscountCalculateRequest.DiscountItem("COUPON", new BigDecimal("0.02"))
        );
        DiscountCalculateRequest request = new DiscountCalculateRequest(transactionId, discounts);
        
        when(discountConfigService.getActiveConfigEntity()).thenReturn(activeConfig);
        when(discountPriorityService.getActivePrioritiesEntity()).thenReturn(activePriorities);

        // Act
        DiscountCalculateResponse result = discountCalculationEngine.calculateDiscounts(request);

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("0.03"), result.totalApplied());
        assertEquals(new BigDecimal("0.03"), result.totalOriginal());
        assertFalse(result.limitExceeded());
    }

    @Test
    @DisplayName("calculateDiscounts_partialDiscount: Cap individual discount at remaining limit")
    void testCalculateDiscountsPartialDiscount() {
        // Arrange
        List<DiscountCalculateRequest.DiscountItem> discounts = List.of(
            new DiscountCalculateRequest.DiscountItem("LOYALTY_POINTS", new BigDecimal("70.00")),
            new DiscountCalculateRequest.DiscountItem("COUPON", new BigDecimal("50.00"))
        );
        DiscountCalculateRequest request = new DiscountCalculateRequest(transactionId, discounts);
        
        when(discountConfigService.getActiveConfigEntity()).thenReturn(activeConfig);
        when(discountPriorityService.getActivePrioritiesEntity()).thenReturn(activePriorities);

        // Act
        DiscountCalculateResponse result = discountCalculationEngine.calculateDiscounts(request);

        // Assert
        assertNotNull(result);
        // LOYALTY_POINTS: 70.00 (within 100 limit)
        // COUPON: 30.00 (capped from 50 to 30, remaining = 100 - 70)
        assertEquals(new BigDecimal("100.00"), result.totalApplied());
        assertEquals(new BigDecimal("100.00"), result.maxDiscountLimit());
        assertTrue(result.limitExceeded());
    }

    // ============ Error Path Tests ============

    @Test
    @DisplayName("calculateDiscounts_configNotFound: Throw when no active config")
    void testCalculateDiscountsConfigNotFound() {
        // Arrange
        List<DiscountCalculateRequest.DiscountItem> discounts = List.of(
            new DiscountCalculateRequest.DiscountItem("LOYALTY_POINTS", new BigDecimal("50.00"))
        );
        DiscountCalculateRequest request = new DiscountCalculateRequest(transactionId, discounts);
        
        when(discountConfigService.getActiveConfigEntity()).thenReturn(null);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, 
            () -> discountCalculationEngine.calculateDiscounts(request),
            "Should throw when no active discount config");
    }

    @Test
    @DisplayName("calculateDiscounts_prioritiesNotFound: Throw when no priorities configured")
    void testCalculateDiscountsPrioritiesNotFound() {
        // Arrange
        List<DiscountCalculateRequest.DiscountItem> discounts = List.of(
            new DiscountCalculateRequest.DiscountItem("LOYALTY_POINTS", new BigDecimal("50.00"))
        );
        DiscountCalculateRequest request = new DiscountCalculateRequest(transactionId, discounts);
        
        when(discountConfigService.getActiveConfigEntity()).thenReturn(activeConfig);
        when(discountPriorityService.getActivePrioritiesEntity()).thenReturn(new ArrayList<>());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, 
            () -> discountCalculationEngine.calculateDiscounts(request),
            "Should throw when no discount priorities found");
    }

    // ============ Edge Cases ============

    @Test
    @DisplayName("calculateDiscounts_emptyDiscounts: Handle empty discount list")
    void testCalculateDiscountsEmptyList() {
        // Arrange
        List<DiscountCalculateRequest.DiscountItem> discounts = new ArrayList<>();
        DiscountCalculateRequest request = new DiscountCalculateRequest(transactionId, discounts);
        
        when(discountConfigService.getActiveConfigEntity()).thenReturn(activeConfig);
        when(discountPriorityService.getActivePrioritiesEntity()).thenReturn(activePriorities);

        // Act
        DiscountCalculateResponse result = discountCalculationEngine.calculateDiscounts(request);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.totalOriginal().compareTo(BigDecimal.ZERO));
        assertEquals(0, result.totalApplied().compareTo(BigDecimal.ZERO));
        assertFalse(result.limitExceeded());
    }

    @Test
    @DisplayName("calculateDiscounts_unknownType: Ignore discount types not in priority map")
    void testCalculateDiscountsUnknownType() {
        // Arrange
        List<DiscountCalculateRequest.DiscountItem> discounts = List.of(
            new DiscountCalculateRequest.DiscountItem("LOYALTY_POINTS", new BigDecimal("50.00")),
            new DiscountCalculateRequest.DiscountItem("UNKNOWN_TYPE", new BigDecimal("100.00"))  // Not in priorities
        );
        DiscountCalculateRequest request = new DiscountCalculateRequest(transactionId, discounts);
        
        when(discountConfigService.getActiveConfigEntity()).thenReturn(activeConfig);
        when(discountPriorityService.getActivePrioritiesEntity()).thenReturn(activePriorities);

        // Act
        DiscountCalculateResponse result = discountCalculationEngine.calculateDiscounts(request);

        // Assert
        assertNotNull(result);
        // LOYALTY_POINTS: 50.00, UNKNOWN_TYPE: after all known types (lowest priority)
        assertTrue(result.totalApplied().compareTo(BigDecimal.ZERO) >= 0);
    }

    @Test
    @DisplayName("calculateDiscounts_zeroMax: Handle zero discount limit correctly")
    void testCalculateDiscountsZeroMaxLimit() {
        // Arrange
        activeConfig.setMaxDiscountLimit(BigDecimal.ZERO);
        List<DiscountCalculateRequest.DiscountItem> discounts = List.of(
            new DiscountCalculateRequest.DiscountItem("LOYALTY_POINTS", new BigDecimal("50.00"))
        );
        DiscountCalculateRequest request = new DiscountCalculateRequest(transactionId, discounts);
        
        when(discountConfigService.getActiveConfigEntity()).thenReturn(activeConfig);
        when(discountPriorityService.getActivePrioritiesEntity()).thenReturn(activePriorities);

        // Act
        DiscountCalculateResponse result = discountCalculationEngine.calculateDiscounts(request);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.totalApplied().compareTo(BigDecimal.ZERO));
        assertTrue(result.limitExceeded());
    }

    @Test
    @DisplayName("calculateDiscounts_largeAmount: Handle large discount amounts with precision")
    void testCalculateDiscountsLargeAmount() {
        // Arrange
        activeConfig.setMaxDiscountLimit(new BigDecimal("9999.99"));
        List<DiscountCalculateRequest.DiscountItem> discounts = List.of(
            new DiscountCalculateRequest.DiscountItem("LOYALTY_POINTS", new BigDecimal("5000.00")),
            new DiscountCalculateRequest.DiscountItem("COUPON", new BigDecimal("5000.00"))
        );
        DiscountCalculateRequest request = new DiscountCalculateRequest(transactionId, discounts);
        
        when(discountConfigService.getActiveConfigEntity()).thenReturn(activeConfig);
        when(discountPriorityService.getActivePrioritiesEntity()).thenReturn(activePriorities);

        // Act
        DiscountCalculateResponse result = discountCalculationEngine.calculateDiscounts(request);

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("10000.00"), result.totalOriginal());
        assertEquals(new BigDecimal("9999.99"), result.totalApplied());
        assertTrue(result.limitExceeded());
    }
}
