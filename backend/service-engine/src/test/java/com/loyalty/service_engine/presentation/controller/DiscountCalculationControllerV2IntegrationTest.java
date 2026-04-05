package com.loyalty.service_engine.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyalty.service_engine.application.dto.DiscountCalculateResponseV2;
import com.loyalty.service_engine.application.service.DiscountCalculationServiceV2;
import com.loyalty.service_engine.infrastructure.exception.GlobalExceptionHandler;
import com.loyalty.service_engine.infrastructure.security.ApiKeyAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DiscountCalculationControllerV2.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        GlobalExceptionHandler.class
})
class DiscountCalculationControllerV2IntegrationTest {

    @MockBean
    private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

        @MockBean
        private DiscountCalculationServiceV2 discountCalculationService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID ecommerceId;

    @Test
    void shouldCalculateWithCapAndPriority() throws Exception {
        ecommerceId = UUID.randomUUID();
        when(discountCalculationService.calculate(any())).thenReturn(
            new DiscountCalculateResponseV2(
                new BigDecimal("250.0000"),
                new BigDecimal("37.50"),
                new BigDecimal("37.50"),
                new BigDecimal("212.50"),
                "Gold",
                false,
                null,
                List.of(),
                UUID.randomUUID(),
                Instant.now()
            )
        );

        String body = """
                {
                  "ecommerceId": "%s",
                  "externalOrderId": "order-123",
                  "customerId": "cust-1",
                  "totalSpent": 2500,
                  "orderCount": 15,
                  "membershipDays": 180,
                  "items": [
                    {"productId":"prod-1","quantity":2,"unitPrice":100,"category":"electronics"},
                    {"productId":"prod-2","quantity":1,"unitPrice":50,"category":"accessories"}
                  ]
                }
                """.formatted(ecommerceId);

        mockMvc.perform(post("/api/v1/engine/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalAmount").value(250.0))
                .andExpect(jsonPath("$.discountApplied").value(37.5))
                .andExpect(jsonPath("$.finalAmount").value(212.5));
    }

    @Test
    void shouldReturn400ForInvalidRequest() throws Exception {
        ecommerceId = UUID.randomUUID();
        String body = """
                {
                  "ecommerceId": "%s",
                  "externalOrderId": "",
                  "customerId": "cust-1",
                  "totalSpent": 2500,
                  "orderCount": 15,
                  "membershipDays": 180,
                  "items": []
                }
                """.formatted(ecommerceId);

        mockMvc.perform(post("/api/v1/engine/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
