package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.configuration.*;
import com.loyalty.service_admin.application.port.in.ConfigurationUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfigurationController Unit Tests")
class ConfigurationControllerTest {

    @Mock
    private ConfigurationUseCase configurationService;

    @InjectMocks
    private ConfigurationController configurationController;

    @Test
    @DisplayName("create returns 201 Created with ApiResponse wrapping ConfigurationWriteData")
    void create_returns201() {
        ConfigurationCreateRequest request = mock(ConfigurationCreateRequest.class);
        ConfigurationWriteData writeData = new ConfigurationWriteData(UUID.randomUUID(), 1L);
        when(configurationService.createConfiguration(any())).thenReturn(writeData);

        ResponseEntity<ApiResponse<ConfigurationWriteData>> result = configurationController.create(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertTrue(result.getBody().success());
        assertEquals(writeData, result.getBody().data());
    }

    @Test
    @DisplayName("patch returns 200 OK with ApiResponse wrapping ConfigurationWriteData")
    void patch_returns200() {
        UUID ecommerceId = UUID.randomUUID();
        ConfigurationPatchRequest request = mock(ConfigurationPatchRequest.class);
        ConfigurationWriteData writeData = new ConfigurationWriteData(UUID.randomUUID(), 2L);
        when(configurationService.patchConfiguration(eq(ecommerceId), any())).thenReturn(writeData);

        ResponseEntity<ApiResponse<ConfigurationWriteData>> result = configurationController.patch(ecommerceId, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().success());
        assertEquals(writeData, result.getBody().data());
    }
}
