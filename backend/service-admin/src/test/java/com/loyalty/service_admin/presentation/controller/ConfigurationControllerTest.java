package com.loyalty.service_admin.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyalty.service_admin.application.dto.configuration.ConfigurationCreateRequest;
import com.loyalty.service_admin.application.port.in.ConfigurationUseCase;
import com.loyalty.service_admin.application.dto.configuration.ConfigurationWriteData;
import com.loyalty.service_admin.domain.model.CapAppliesTo;
import com.loyalty.service_admin.domain.model.CapType;
import com.loyalty.service_admin.domain.model.RoundingRule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ConfigurationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ConfigurationControllerTest.TestSecurityConfig.class)
class ConfigurationControllerTest {

    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConfigurationUseCase configurationService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateConfiguration() throws Exception {
        UUID configId = UUID.randomUUID();
        UUID ecommerceId = UUID.randomUUID();

        when(configurationService.createConfiguration(any())).thenReturn(new ConfigurationWriteData(configId, 1L));

        ConfigurationCreateRequest request = new ConfigurationCreateRequest(
                ecommerceId,
                "COP",
                RoundingRule.HALF_UP,
                new com.loyalty.service_admin.application.dto.configuration.CapRequest(
                        CapType.PERCENTAGE, new BigDecimal("20"), CapAppliesTo.SUBTOTAL),
                List.of(
                        new com.loyalty.service_admin.application.dto.configuration.DiscountPriorityRequest("SEASONAL", 1),
                        new com.loyalty.service_admin.application.dto.configuration.DiscountPriorityRequest("LOYALTY", 2)
                )
        );

        mockMvc.perform(post("/api/v1/configurations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.configId").value(configId.toString()))
                .andExpect(jsonPath("$.data.version").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldPatchConfiguration() throws Exception {
        UUID configId = UUID.randomUUID();
        UUID ecommerceId = UUID.randomUUID();

        when(configurationService.patchConfiguration(any(), any())).thenReturn(new ConfigurationWriteData(configId, 2L));

        String patchBody = """
                {
                  "roundingRule": "DOWN"
                }
                """;

        mockMvc.perform(patch("/api/v1/configurations/{ecommerceId}", ecommerceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.version").value(2));
    }
}
