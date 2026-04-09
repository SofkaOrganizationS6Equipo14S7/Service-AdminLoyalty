package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.customertier.*;
import com.loyalty.service_admin.application.port.in.CustomerTierUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerTierController Unit Tests")
class CustomerTierControllerTest {

    @Mock
    private CustomerTierUseCase customerTierUseCase;

    @InjectMocks
    private CustomerTierController customerTierController;

    private CustomerTierResponse buildTierResponse() {
        return new CustomerTierResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Gold", 1, true, Instant.now(), Instant.now());
    }

    @Test
    @DisplayName("createCustomerTier returns 201 Created")
    void createCustomerTier_returns201() {
        CustomerTierCreateRequest request = new CustomerTierCreateRequest(UUID.randomUUID(), "Gold", 1);
        CustomerTierResponse response = buildTierResponse();
        when(customerTierUseCase.create(any())).thenReturn(response);

        ResponseEntity<CustomerTierResponse> result = customerTierController.createCustomerTier(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    @DisplayName("listCustomerTiers returns 200 with paginated results")
    void listCustomerTiers_returns200() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<CustomerTierResponse> page = new PageImpl<>(List.of(buildTierResponse()));
        when(customerTierUseCase.listPaginated(any(Pageable.class), eq(true))).thenReturn(page);

        ResponseEntity<Page<CustomerTierResponse>> result = customerTierController.listCustomerTiers(pageable, true);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().getTotalElements());
    }

    @Test
    @DisplayName("listCustomerTiers with null isActive returns 200")
    void listCustomerTiers_nullFilter_returns200() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<CustomerTierResponse> page = new PageImpl<>(List.of());
        when(customerTierUseCase.listPaginated(any(Pageable.class), eq(null))).thenReturn(page);

        ResponseEntity<Page<CustomerTierResponse>> result = customerTierController.listCustomerTiers(pageable, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    @DisplayName("getCustomerTierDetails returns 200")
    void getCustomerTierDetails_returns200() {
        UUID tierId = UUID.randomUUID();
        CustomerTierResponse response = buildTierResponse();
        when(customerTierUseCase.getById(tierId)).thenReturn(response);

        ResponseEntity<CustomerTierResponse> result = customerTierController.getCustomerTierDetails(tierId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    @DisplayName("updateCustomerTier returns 200")
    void updateCustomerTier_returns200() {
        UUID tierId = UUID.randomUUID();
        CustomerTierUpdateRequest request = new CustomerTierUpdateRequest("Platinum", 2);
        CustomerTierResponse response = buildTierResponse();
        when(customerTierUseCase.update(eq(tierId), any())).thenReturn(response);

        ResponseEntity<CustomerTierResponse> result = customerTierController.updateCustomerTier(tierId, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    @DisplayName("deleteCustomerTier returns 204 No Content")
    void deleteCustomerTier_returns204() {
        UUID tierId = UUID.randomUUID();
        doNothing().when(customerTierUseCase).delete(tierId);

        ResponseEntity<Void> result = customerTierController.deleteCustomerTier(tierId);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(customerTierUseCase).delete(tierId);
    }

    @Test
    @DisplayName("activateCustomerTier returns 200")
    void activateCustomerTier_returns200() {
        UUID tierId = UUID.randomUUID();
        CustomerTierResponse response = buildTierResponse();
        when(customerTierUseCase.activate(tierId)).thenReturn(response);

        ResponseEntity<CustomerTierResponse> result = customerTierController.activateCustomerTier(tierId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }
}
