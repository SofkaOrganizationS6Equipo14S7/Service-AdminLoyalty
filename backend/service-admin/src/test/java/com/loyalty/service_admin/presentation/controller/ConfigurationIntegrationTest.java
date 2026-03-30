package com.loyalty.service_admin.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyalty.service_admin.application.dto.configuration.ConfigurationCreateRequest;
import com.loyalty.service_admin.application.mapper.ConfigurationMapper;
import com.loyalty.service_admin.application.port.out.ConfigurationEventPort;
import com.loyalty.service_admin.application.port.out.ConfigurationPersistencePort;
import com.loyalty.service_admin.application.port.out.CurrentUserPort;
import com.loyalty.service_admin.application.service.ConfigurationService;
import com.loyalty.service_admin.application.validation.ConfigurationBusinessValidator;
import com.loyalty.service_admin.domain.entity.DiscountConfigurationEntity;
import com.loyalty.service_admin.domain.model.CapAppliesTo;
import com.loyalty.service_admin.domain.model.CapType;
import com.loyalty.service_admin.domain.model.RoundingRule;
import com.loyalty.service_admin.infrastructure.exception.ConfigurationAlreadyExistsException;
import com.loyalty.service_admin.infrastructure.exception.ConfigurationNotFoundException;
import org.junit.jupiter.api.BeforeEach;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ConfigurationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        ConfigurationService.class,
        ConfigurationMapper.class,
        ConfigurationBusinessValidator.class,
        ConfigurationIntegrationTest.TestSecurityConfig.class
})
class ConfigurationIntegrationTest {

    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConfigurationPersistencePort persistencePort;
    @MockBean
    private ConfigurationEventPort eventPort;
    @MockBean
    private CurrentUserPort currentUserPort;

    @BeforeEach
    void setUp() {
        when(currentUserPort.isSuperAdmin()).thenReturn(true);
        doNothing().when(eventPort).publishConfigUpdated(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateConfigurationAndReturn201() throws Exception {
        UUID ecommerceId = UUID.randomUUID();
        when(persistencePort.existsByEcommerceId(ecommerceId)).thenReturn(false);
        when(persistencePort.save(any())).thenAnswer(invocation -> {
            DiscountConfigurationEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            entity.setVersion(1L);
            entity.setUpdatedAt(Instant.now());
            entity.getPriorities().forEach(p -> p.setId(UUID.randomUUID()));
            return entity;
        });

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
                .andExpect(jsonPath("$.data.version").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn400ForDuplicatedPriority() throws Exception {
        String body = """
                {
                  "ecommerceId": "%s",
                  "currency": "COP",
                  "roundingRule": "HALF_UP",
                  "cap": {"type":"PERCENTAGE","value":20,"appliesTo":"SUBTOTAL"},
                  "priority": [
                    {"type":"SEASONAL","order":1},
                    {"type":"LOYALTY","order":1}
                  ]
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/configurations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn404OnPatchWhenConfigMissing() throws Exception {
        UUID ecommerceId = UUID.randomUUID();
        when(persistencePort.findByEcommerceId(ecommerceId)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/v1/configurations/{ecommerceId}", ecommerceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roundingRule\":\"DOWN\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONFIG_NOT_FOUND"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn409WhenConfigurationAlreadyExists() throws Exception {
        UUID ecommerceId = UUID.randomUUID();
        when(persistencePort.existsByEcommerceId(ecommerceId)).thenReturn(true);

        String body = """
                {
                  "ecommerceId": "%s",
                  "currency": "COP",
                  "roundingRule": "HALF_UP",
                  "cap": {"type":"PERCENTAGE","value":20,"appliesTo":"SUBTOTAL"},
                  "priority": [{"type":"SEASONAL","order":1}]
                }
                """.formatted(ecommerceId);

        mockMvc.perform(post("/api/v1/configurations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFIG_ALREADY_EXISTS"));
    }
}
