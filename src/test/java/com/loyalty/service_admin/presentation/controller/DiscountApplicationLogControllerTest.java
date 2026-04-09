package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.discountlog.DiscountApplicationLogResponse;
import com.loyalty.service_admin.application.service.DiscountApplicationLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiscountApplicationLogController Unit Tests")
class DiscountApplicationLogControllerTest {

    @Mock
    private DiscountApplicationLogService discountLogService;

    @InjectMocks
    private DiscountApplicationLogController controller;

    @Test
    @DisplayName("listDiscountLogs returns 200 with paginated results")
    void listDiscountLogs_returns200() {
        UUID ecommerceId = UUID.randomUUID();
        DiscountApplicationLogResponse logResponse = mock(DiscountApplicationLogResponse.class);
        Page<DiscountApplicationLogResponse> page = new PageImpl<>(List.of(logResponse));
        when(discountLogService.listDiscountLogs(ecommerceId, null, 0, 50)).thenReturn(page);

        ResponseEntity<Page<DiscountApplicationLogResponse>> result =
                controller.listDiscountLogs(ecommerceId, null, 0, 50);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().getTotalElements());
    }

    @Test
    @DisplayName("listDiscountLogs with externalOrderId filter returns 200")
    void listDiscountLogs_withFilter_returns200() {
        Page<DiscountApplicationLogResponse> page = new PageImpl<>(List.of());
        when(discountLogService.listDiscountLogs(null, "#ORDER-123", 0, 20)).thenReturn(page);

        ResponseEntity<Page<DiscountApplicationLogResponse>> result =
                controller.listDiscountLogs(null, "#ORDER-123", 0, 20);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    @DisplayName("getDiscountLogById returns 200")
    void getDiscountLogById_returns200() {
        UUID logId = UUID.randomUUID();
        DiscountApplicationLogResponse logResponse = mock(DiscountApplicationLogResponse.class);
        when(discountLogService.getDiscountLogById(logId)).thenReturn(logResponse);

        ResponseEntity<DiscountApplicationLogResponse> result = controller.getDiscountLogById(logId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(logResponse, result.getBody());
    }

    @Test
    @DisplayName("methodNotAllowed PUT returns 405")
    void methodNotAllowed_returns405() {
        ResponseEntity<Void> result = controller.methodNotAllowed();
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, result.getStatusCode());
    }

    @Test
    @DisplayName("methodNotAllowedDelete returns 405")
    void methodNotAllowedDelete_returns405() {
        ResponseEntity<Void> result = controller.methodNotAllowedDelete();
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, result.getStatusCode());
    }
}
