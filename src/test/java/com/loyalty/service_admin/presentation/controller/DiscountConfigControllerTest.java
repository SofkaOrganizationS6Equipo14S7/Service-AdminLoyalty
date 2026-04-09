package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.rules.discount.*;
import com.loyalty.service_admin.application.port.in.DiscountConfigUseCase;
import com.loyalty.service_admin.application.port.in.DiscountLimitPriorityUseCase;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiscountConfigController Unit Tests")
class DiscountConfigControllerTest {

    @Mock
    private DiscountConfigUseCase discountConfigUseCase;
    @Mock
    private DiscountLimitPriorityUseCase priorityUseCase;

    @InjectMocks
    private DiscountConfigController discountConfigController;

    @Test
    @DisplayName("updateDiscountConfig returns 201 Created")
    void updateDiscountConfig_returns201() {
        DiscountConfigCreateRequest request = mock(DiscountConfigCreateRequest.class);
        DiscountConfigResponse response = mock(DiscountConfigResponse.class);
        when(discountConfigUseCase.updateConfig(any())).thenReturn(response);

        ResponseEntity<DiscountConfigResponse> result = discountConfigController.updateDiscountConfig(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    @DisplayName("getDiscountConfig returns 200 OK")
    void getDiscountConfig_returns200() {
        UUID ecommerceId = UUID.randomUUID();
        DiscountConfigResponse response = mock(DiscountConfigResponse.class);
        when(discountConfigUseCase.getActiveConfig(ecommerceId)).thenReturn(response);

        ResponseEntity<DiscountConfigResponse> result = discountConfigController.getDiscountConfig(ecommerceId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    @DisplayName("savePriorities returns 201 Created")
    void savePriorities_returns201() {
        DiscountLimitPriorityRequest request = mock(DiscountLimitPriorityRequest.class);
        DiscountLimitPriorityResponse response = mock(DiscountLimitPriorityResponse.class);
        when(priorityUseCase.savePriorities(any())).thenReturn(response);

        ResponseEntity<DiscountLimitPriorityResponse> result = discountConfigController.savePriorities(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    @DisplayName("getPriorities returns 200 OK")
    void getPriorities_returns200() {
        UUID discountSettingId = UUID.randomUUID();
        DiscountLimitPriorityResponse response = mock(DiscountLimitPriorityResponse.class);
        when(priorityUseCase.getPriorities(discountSettingId)).thenReturn(response);

        ResponseEntity<DiscountLimitPriorityResponse> result = discountConfigController.getPriorities(discountSettingId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }
}
