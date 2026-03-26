package com.loyalty.controller;

import com.loyalty.dto.FeatureCreateDTO;
import com.loyalty.dto.FeatureResponseDTO;
import com.loyalty.service.FeatureService;
import com.loyalty.exception.FeatureNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FeatureController.class)
class FeatureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FeatureService service;

    // ─── Happy Path ─────────────────────────────────────────────────────────

    @Test
    void createFeature_validInput_returns201() throws Exception {
        FeatureResponseDTO response = new FeatureResponseDTO();
        response.setUid("uuid-123");
        response.setName("Test Feature");
        response.setCreatedAt(LocalDateTime.now());

        when(service.create(any(FeatureCreateDTO.class), anyString()))
            .thenReturn(response);

        mockMvc.perform(post("/api/v1/features")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-API-Key", "test-api-key")
                .content("""
                    {
                        "name": "Test Feature",
                        "description": "Description"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.uid").value("uuid-123"))
            .andExpect(jsonPath("$.name").value("Test Feature"));
    }

    @Test
    void getFeature_exists_returns200() throws Exception {
        FeatureResponseDTO response = new FeatureResponseDTO();
        response.setUid("uuid-123");
        response.setName("Test");
        response.setCreatedAt(LocalDateTime.now());

        when(service.getByUid("uuid-123")).thenReturn(response);

        mockMvc.perform(get("/api/v1/features/uuid-123")
                .header("X-API-Key", "test-api-key"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.uid").value("uuid-123"));
    }

    // ─── Error Path ─────────────────────────────────────────────────────────

    @Test
    void getFeature_notFound_returns404() throws Exception {
        when(service.getByUid("nonexistent"))
            .thenThrow(new FeatureNotFoundException("Feature not found"));

        mockMvc.perform(get("/api/v1/features/nonexistent")
                .header("X-API-Key", "test-api-key"))
            .andExpect(status().isNotFound());
    }

    @Test
    void createFeature_missingApiKey_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "Test Feature"
                    }
                    """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void createFeature_invalidInput_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/features")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-API-Key", "test-api-key")
                .content("""
                    {
                        "name": ""
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    @Test
    void deleteFeature_success_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/features/uuid-123")
                .header("X-API-Key", "test-api-key"))
            .andExpect(status().isNoContent());
    }
}
