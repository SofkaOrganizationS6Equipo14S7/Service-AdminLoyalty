package com.loyalty.service_engine.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyalty.service_engine.application.dto.configuration.ConfigurationUpdatedEvent;
import com.loyalty.service_engine.application.service.DiscountCalculationServiceV2;
import com.loyalty.service_engine.application.service.EngineConfigurationCacheService;
import com.loyalty.service_engine.infrastructure.exception.GlobalExceptionHandler;
import com.loyalty.service_engine.infrastructure.security.ApiKeyAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DiscountCalculationControllerV2.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        DiscountCalculationServiceV2.class,
        EngineConfigurationCacheService.class,
        GlobalExceptionHandler.class
})
class DiscountCalculationControllerV2IntegrationTest {

    @MockBean
    private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EngineConfigurationCacheService cacheService;

    private UUID ecommerceId;

    @BeforeEach
    void setUp() {
        ecommerceId = UUID.randomUUID();
        cacheService.upsertFromEvent(new ConfigurationUpdatedEvent(
                "CONFIG_UPDATED",
                UUID.randomUUID(),
                ecommerceId,
                1L,
                "COP",
                ConfigurationUpdatedEvent.RoundingRule.HALF_UP,
                ConfigurationUpdatedEvent.CapType.PERCENTAGE,
                new BigDecimal("20"),
                ConfigurationUpdatedEvent.CapAppliesTo.SUBTOTAL,
                List.of(
                        new ConfigurationUpdatedEvent.PriorityItem(UUID.randomUUID(), "SEASONAL", 1),
                        new ConfigurationUpdatedEvent.PriorityItem(UUID.randomUUID(), "LOYALTY", 2)
                ),
                Instant.now()
        ));
    }

    @Test
    void shouldCalculateWithCapAndPriority() throws Exception {
        String body = """
                {
                  "ecommerceId": "%s",
                  "subtotal": 1000,
                  "total": 1190,
                  "beforeTax": 1000,
                  "afterTax": 1190,
                  "discounts": [
                    {"type":"LOYALTY","amount":150},
                    {"type":"SEASONAL","amount":100}
                  ]
                }
                """.formatted(ecommerceId);

        mockMvc.perform(post("/api/v1/discounts/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capAmount").value(200.00))
                .andExpect(jsonPath("$.totalApplied").value(200.00))
                .andExpect(jsonPath("$.capped").value(true))
                .andExpect(jsonPath("$.appliedDiscounts[0].type").value("SEASONAL"));
    }

    @Test
    void shouldReturn400ForInvalidTotals() throws Exception {
        String body = """
                {
                  "ecommerceId": "%s",
                  "subtotal": 1200,
                  "total": 1000,
                  "beforeTax": 1000,
                  "afterTax": 1190,
                  "discounts": [{"type":"LOYALTY","amount":20}]
                }
                """.formatted(ecommerceId);

        mockMvc.perform(post("/api/v1/discounts/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void shouldReturn400ForBeanValidationErrors() throws Exception {
        String body = """
                {
                  "ecommerceId": "%s",
                  "subtotal": 100,
                  "total": 119,
                  "beforeTax": 100,
                  "afterTax": 119,
                  "discounts": [{"type":"","amount":0}]
                }
                """.formatted(ecommerceId);

        mockMvc.perform(post("/api/v1/discounts/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
