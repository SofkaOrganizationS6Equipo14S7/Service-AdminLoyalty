package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.apikey.*;
import com.loyalty.service_admin.application.port.in.ApiKeyUseCase;
import com.loyalty.service_admin.infrastructure.exception.AuthorizationException;
import com.loyalty.service_admin.infrastructure.security.SecurityContextHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiKeyController Unit Tests")
class ApiKeyControllerTest {

    @Mock
    private ApiKeyUseCase apiKeyUseCase;
    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private ApiKeyController apiKeyController;

    private final UUID ecommerceId = UUID.randomUUID();

    @Test
    @DisplayName("createApiKey - SUPER_ADMIN returns 201")
    void createApiKey_superAdmin_returns201() {
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        ApiKeyCreatedResponse response = new ApiKeyCreatedResponse(
                UUID.randomUUID(), "plain-key-123", Instant.now().plusSeconds(86400),
                ecommerceId, Instant.now(), Instant.now());
        when(apiKeyUseCase.createApiKey(ecommerceId)).thenReturn(response);

        ResponseEntity<ApiKeyCreatedResponse> result = apiKeyController.createApiKey(ecommerceId, new ApiKeyCreateRequest());

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals("plain-key-123", result.getBody().key());
    }

    @Test
    @DisplayName("createApiKey - STORE_ADMIN own ecommerce returns 201")
    void createApiKey_storeAdmin_ownEcommerce_returns201() {
        when(securityContextHelper.getCurrentUserRole()).thenReturn("STORE_ADMIN");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);
        ApiKeyCreatedResponse response = new ApiKeyCreatedResponse(
                UUID.randomUUID(), "key-456", Instant.now().plusSeconds(86400),
                ecommerceId, Instant.now(), Instant.now());
        when(apiKeyUseCase.createApiKey(ecommerceId)).thenReturn(response);

        ResponseEntity<ApiKeyCreatedResponse> result = apiKeyController.createApiKey(ecommerceId, null);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
    }

    @Test
    @DisplayName("createApiKey - STORE_ADMIN other ecommerce throws AuthorizationException")
    void createApiKey_storeAdmin_otherEcommerce_throwsException() {
        UUID otherEcommerce = UUID.randomUUID();
        when(securityContextHelper.getCurrentUserRole()).thenReturn("STORE_ADMIN");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(UUID.randomUUID());

        assertThrows(AuthorizationException.class,
                () -> apiKeyController.createApiKey(otherEcommerce, null));
    }

    @Test
    @DisplayName("getApiKeys returns 200 with list")
    void getApiKeys_returns200() {
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        ApiKeyListResponse item = new ApiKeyListResponse(
                UUID.randomUUID(), "****abcd", Instant.now(), true, Instant.now(), Instant.now());
        when(apiKeyUseCase.getApiKeysByEcommerce(ecommerceId)).thenReturn(List.of(item));

        ResponseEntity<List<ApiKeyListResponse>> result = apiKeyController.getApiKeys(ecommerceId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    @Test
    @DisplayName("deleteApiKey returns 204 No Content")
    void deleteApiKey_returns204() {
        UUID keyId = UUID.randomUUID();
        when(securityContextHelper.getCurrentUserRole()).thenReturn("SUPER_ADMIN");
        doNothing().when(apiKeyUseCase).deleteApiKey(ecommerceId, keyId);

        ResponseEntity<Void> result = apiKeyController.deleteApiKey(ecommerceId, keyId);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(apiKeyUseCase).deleteApiKey(ecommerceId, keyId);
    }

    @Test
    @DisplayName("STORE_ADMIN with null ecommerceId throws AuthorizationException")
    void storeAdmin_nullEcommerceId_throwsException() {
        when(securityContextHelper.getCurrentUserRole()).thenReturn("STORE_ADMIN");
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);

        assertThrows(AuthorizationException.class,
                () -> apiKeyController.getApiKeys(ecommerceId));
    }
}
