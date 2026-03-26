package com.loyalty.service_engine.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyalty.service_engine.application.dto.DiscountCalculateRequest;
import com.loyalty.service_engine.application.dto.DiscountCalculateResponse;
import com.loyalty.service_engine.application.dto.DiscountConfigCreateRequest;
import com.loyalty.service_engine.application.dto.DiscountConfigResponse;
import com.loyalty.service_engine.application.dto.DiscountPriorityRequest;
import com.loyalty.service_engine.application.dto.DiscountPriorityResponse;
import com.loyalty.service_engine.application.service.DiscountCalculationEngine;
import com.loyalty.service_engine.application.service.DiscountConfigService;
import com.loyalty.service_engine.application.service.DiscountPriorityService;
import com.loyalty.service_engine.infrastructure.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test suite para DiscountConfigController.
 * Cubre: HTTP status codes (201/200/400/404/409), validaciones, respuestas JSON.
 */
@WebMvcTest(DiscountConfigController.class)
@DisplayName("DiscountConfigController Tests")
class DiscountConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DiscountConfigService discountConfigService;

    @MockitoBean
    private DiscountPriorityService discountPriorityService;

    @MockitoBean
    private DiscountCalculationEngine discountCalculationEngine;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID userId;
    private DiscountConfigResponse validConfigResponse;
    private DiscountPriorityResponse validPriorityResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        
        validConfigResponse = new DiscountConfigResponse(
            UUID.randomUUID().toString(),
            new BigDecimal("100.00"),
            "USD",
            true,
            Instant.now(),
            Instant.now()
        );
        
        validPriorityResponse = new DiscountPriorityResponse(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            Instant.now(),
            List.of(
                new DiscountPriorityResponse.DiscountPriority("LOYALTY_POINTS", 1),
                new DiscountPriorityResponse.DiscountPriority("COUPON", 2)
            )
        );
    }

    // ============ POST /api/v1/discount/config Tests ============

    @Test
    @DisplayName("updateDiscountConfig_201: Create new config returns 201 Created")
    void testUpdateDiscountConfigCreate201() throws Exception {
        // Arrange
        DiscountConfigCreateRequest request = new DiscountConfigCreateRequest(
            new BigDecimal("100.00"),
            "USD"
        );
        
        when(discountConfigService.updateConfig(any(), any())).thenReturn(validConfigResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/discount/config")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-ID", userId.toString())
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.uid").exists())
            .andExpect(jsonPath("$.maxDiscountLimit").value(100.00))
            .andExpect(jsonPath("$.currencyCode").value("USD"))
            .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    @DisplayName("updateDiscountConfig_400: Invalid max discount limit")
    void testUpdateDiscountConfigInvalidLimit() throws Exception {
        // Arrange
        DiscountConfigCreateRequest request = new DiscountConfigCreateRequest(
            BigDecimal.ZERO,  // Invalid
            "USD"
        );
        
        when(discountConfigService.updateConfig(any(), any()))
            .thenThrow(new IllegalArgumentException("max_discount_limit must be greater than 0"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/discount/config")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-ID", userId.toString())
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("updateDiscountConfig_missingField: Missing required field returns 400")
    void testUpdateDiscountConfigMissingField() throws Exception {
        // Arrange - Request missing maxDiscountLimit
        String invalidJson = "{\"currencyCode\": \"USD\"}";

        // Act & Assert
        mockMvc.perform(post("/api/v1/discount/config")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-ID", userId.toString())
                .content(invalidJson))
            .andExpect(status().isBadRequest());
    }

    // ============ GET /api/v1/discount/config Tests ============

    @Test
    @DisplayName("getActiveDiscountConfig_200: Return active config")
    void testGetActiveDiscountConfig200() throws Exception {
        // Arrange
        when(discountConfigService.getActiveConfig()).thenReturn(validConfigResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/discount/config")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-ID", userId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.uid").exists())
            .andExpect(jsonPath("$.maxDiscountLimit").value(100.00))
            .andExpect(jsonPath("$.currencyCode").value("USD"))
            .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    @DisplayName("getActiveDiscountConfig_404: Config not found returns 404")
    void testGetActiveDiscountConfigNotFound() throws Exception {
        // Arrange
        when(discountConfigService.getActiveConfig())
            .thenThrow(new ResourceNotFoundException("discount_config_not_found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/discount/config")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-ID", userId.toString()))
            .andExpect(status().isNotFound());
    }

    // ============ POST /api/v1/discount/priority Tests ============

    @Test
    @DisplayName("saveDiscountPriorities_201: Save valid priorities returns 201 Created")
    void testSaveDiscountPrioritiesCreate201() throws Exception {
        // Arrange
        DiscountPriorityRequest request = new DiscountPriorityRequest(
            UUID.randomUUID().toString(),
            List.of(
                new DiscountPriorityRequest.DiscountPriorityItem("LOYALTY_POINTS", 1),
                new DiscountPriorityRequest.DiscountPriorityItem("COUPON", 2)
            )
        );
        
        when(discountPriorityService.savePriorities(any())).thenReturn(validPriorityResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/discount/priority")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.uid").exists())
            .andExpect(jsonPath("$.priorities", hasSize(2)))
            .andExpect(jsonPath("$.priorities[0].discountType").value("LOYALTY_POINTS"))
            .andExpect(jsonPath("$.priorities[0].priorityLevel").value(1));
    }

    @Test
    @DisplayName("saveDiscountPriorities_400: Duplicate priority levels returns 400")
    void testSaveDiscountPrioritiesDuplicateLevels() throws Exception {
        // Arrange
        DiscountPriorityRequest request = new DiscountPriorityRequest(
            UUID.randomUUID().toString(),
            List.of(
                new DiscountPriorityRequest.DiscountPriorityItem("LOYALTY_POINTS", 1),
                new DiscountPriorityRequest.DiscountPriorityItem("COUPON", 1)  // Duplicate!
            )
        );
        
        when(discountPriorityService.savePriorities(any()))
            .thenThrow(new IllegalArgumentException("duplicate_priority_level"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/discount/priority")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("saveDiscountPriorities_409: Config not found returns 409 Conflict")
    void testSaveDiscountPrioritiesConfigNotFound() throws Exception {
        // Arrange
        DiscountPriorityRequest request = new DiscountPriorityRequest(
            UUID.randomUUID().toString(),
            List.of(
                new DiscountPriorityRequest.DiscountPriorityItem("LOYALTY_POINTS", 1)
            )
        );
        
        when(discountPriorityService.savePriorities(any()))
            .thenThrow(new ResourceNotFoundException("discount_config_not_found"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/discount/priority")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    // ============ GET /api/v1/discount/priority Tests ============

    @Test
    @DisplayName("getActiveDiscountPriorities_200: Return active priorities")
    void testGetActiveDiscountPrioritiesSuccess() throws Exception {
        // Arrange
        when(discountPriorityService.getActivePriorities()).thenReturn(validPriorityResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/discount/priority")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-ID", userId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.uid").exists())
            .andExpect(jsonPath("$.priorities", hasSize(2)))
            .andExpect(jsonPath("$.priorities[0].discountType").value("LOYALTY_POINTS"))
            .andExpect(jsonPath("$.priorities[1].discountType").value("COUPON"));
    }

    @Test
    @DisplayName("getActiveDiscountPriorities_404: Priorities not found returns 404")
    void testGetActiveDiscountPrioritiesNotFound() throws Exception {
        // Arrange
        when(discountPriorityService.getActivePriorities())
            .thenThrow(new ResourceNotFoundException("discount_priority_not_found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/discount/priority")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-ID", userId.toString()))
            .andExpect(status().isNotFound());
    }

    // ============ POST /api/v1/discount/calculate Tests ============

    @Test
    @DisplayName("calculateDiscounts_200: Calculate within limit returns 200 OK")
    void testCalculateDiscountsSuccess() throws Exception {
        // Arrange
        DiscountCalculateRequest request = new DiscountCalculateRequest(
            "txn-123",
            List.of(
                new DiscountCalculateRequest.DiscountItem("LOYALTY_POINTS", new BigDecimal("50.00")),
                new DiscountCalculateRequest.DiscountItem("COUPON", new BigDecimal("40.00"))
            )
        );
        
        DiscountCalculateResponse response = new DiscountCalculateResponse(
            "txn-123",
            List.of(
                new DiscountCalculateResponse.DiscountItem("LOYALTY_POINTS", new BigDecimal("50.00")),
                new DiscountCalculateResponse.DiscountItem("COUPON", new BigDecimal("40.00"))
            ),
            List.of(
                new DiscountCalculateResponse.DiscountItem("LOYALTY_POINTS", new BigDecimal("50.00")),
                new DiscountCalculateResponse.DiscountItem("COUPON", new BigDecimal("40.00"))
            ),
            new BigDecimal("90.00"),
            new BigDecimal("90.00"),
            new BigDecimal("100.00"),
            false,
            Instant.now()
        );
        
        when(discountCalculationEngine.calculateDiscounts(any())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/discount/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-ID", userId.toString())
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transaction_id").value("txn-123"))
            .andExpect(jsonPath("$.total_original").value(90.00))
            .andExpect(jsonPath("$.total_applied").value(90.00))
            .andExpect(jsonPath("$.limit_exceeded").value(false));
    }

    @Test
    @DisplayName("calculateDiscounts_200: Respect priority order")
    void testCalculateDiscountsRespectPriority() throws Exception {
        // Arrange
        DiscountCalculateRequest request = new DiscountCalculateRequest(
            "txn-456",
            List.of(
                new DiscountCalculateRequest.DiscountItem("LOYALTY_POINTS", new BigDecimal("70.00")),
                new DiscountCalculateRequest.DiscountItem("COUPON", new BigDecimal("50.00"))
            )
        );
        
        DiscountCalculateResponse response = new DiscountCalculateResponse(
            "txn-456",
            List.of(
                new DiscountCalculateResponse.DiscountItem("LOYALTY_POINTS", new BigDecimal("70.00")),
                new DiscountCalculateResponse.DiscountItem("COUPON", new BigDecimal("50.00"))
            ),
            List.of(
                new DiscountCalculateResponse.DiscountItem("LOYALTY_POINTS", new BigDecimal("70.00")),
                new DiscountCalculateResponse.DiscountItem("COUPON", new BigDecimal("30.00"))
            ),
            new BigDecimal("120.00"),
            new BigDecimal("100.00"),
            new BigDecimal("100.00"),
            true,
            Instant.now()
        );
        
        when(discountCalculationEngine.calculateDiscounts(any())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/discount/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-ID", userId.toString())
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total_original").value(120.00))
            .andExpect(jsonPath("$.total_applied").value(100.00))
            .andExpect(jsonPath("$.limit_exceeded").value(true));
    }

    @Test
    @DisplayName("calculateDiscounts_409: Config not found returns 409 Conflict")
    void testCalculateDiscountsConfigNotFound() throws Exception {
        // Arrange
        DiscountCalculateRequest request = new DiscountCalculateRequest(
            "txn-789",
            List.of(
                new DiscountCalculateRequest.DiscountItem("LOYALTY_POINTS", new BigDecimal("50.00"))
            )
        );
        
        when(discountCalculationEngine.calculateDiscounts(any()))
            .thenThrow(new ResourceNotFoundException("discount_config_not_found: please configure max_limit and priority first"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/discount/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-ID", userId.toString())
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("calculateDiscounts_missingField: Missing transaction_id returns 400")
    void testCalculateDiscountsMissingField() throws Exception {
        // Arrange - Request without transaction_id
        String invalidJson = "{\"discounts\": [{\"discountType\": \"LOYALTY_POINTS\", \"amount\": 50.00}]}";

        // Act & Assert
        mockMvc.perform(post("/api/v1/discount/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-ID", userId.toString())
                .content(invalidJson))
            .andExpect(status().isBadRequest());
    }

    // ============ Edge Cases / Validation Tests ============

    @Test
    @DisplayName("updateDiscountConfig_negative: Reject negative discount limit")
    void testUpdateDiscountConfigNegativeLimit() throws Exception {
        // Arrange
        DiscountConfigCreateRequest request = new DiscountConfigCreateRequest(
            new BigDecimal("-50.00"),
            "USD"
        );
        
        when(discountConfigService.updateConfig(any(), any()))
            .thenThrow(new IllegalArgumentException("max_discount_limit must be greater than 0"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/discount/config")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-ID", userId.toString())
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("updateDiscountConfig_precision: Accept decimals correctly")
    void testUpdateDiscountConfigDecimal() throws Exception {
        // Arrange
        DiscountConfigCreateRequest request = new DiscountConfigCreateRequest(
            new BigDecimal("99.99"),
            "USD"
        );
        
        DiscountConfigResponse response = new DiscountConfigResponse(
            UUID.randomUUID().toString(),
            new BigDecimal("99.99"),
            "USD",
            true,
            Instant.now(),
            Instant.now()
        );
        
        when(discountConfigService.updateConfig(any(), any())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/discount/config")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-ID", userId.toString())
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.maxDiscountLimit").value(99.99));
    }
}
