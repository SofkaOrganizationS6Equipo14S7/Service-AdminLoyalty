package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.auditlog.AuditLogResponse;
import com.loyalty.service_admin.application.service.AuditLogService;
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
@DisplayName("AuditLogController Unit Tests")
class AuditLogControllerTest {

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuditLogController controller;

    @Test
    @DisplayName("listAuditLogs returns 200 with paginated results")
    void listAuditLogs_returns200() {
        UUID ecommerceId = UUID.randomUUID();
        AuditLogResponse logResponse = mock(AuditLogResponse.class);
        Page<AuditLogResponse> page = new PageImpl<>(List.of(logResponse));
        when(auditLogService.listAuditLogs("app_user", ecommerceId, 0, 50)).thenReturn(page);

        ResponseEntity<Page<AuditLogResponse>> result =
                controller.listAuditLogs("app_user", ecommerceId, 0, 50);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().getTotalElements());
    }

    @Test
    @DisplayName("listAuditLogs without filters returns 200")
    void listAuditLogs_noFilter_returns200() {
        Page<AuditLogResponse> page = new PageImpl<>(List.of());
        when(auditLogService.listAuditLogs(null, null, 0, 50)).thenReturn(page);

        ResponseEntity<Page<AuditLogResponse>> result =
                controller.listAuditLogs(null, null, 0, 50);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    @DisplayName("getAuditLogById returns 200")
    void getAuditLogById_returns200() {
        UUID logId = UUID.randomUUID();
        AuditLogResponse logResponse = mock(AuditLogResponse.class);
        when(auditLogService.getAuditLogById(logId)).thenReturn(logResponse);

        ResponseEntity<AuditLogResponse> result = controller.getAuditLogById(logId);

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
