package com.loyalty.service_admin.application.validation;

import com.loyalty.service_admin.application.dto.configuration.ConfigurationCreateRequest;
import com.loyalty.service_admin.application.dto.configuration.ConfigurationPatchRequest;
import com.loyalty.service_admin.application.dto.configuration.DiscountPriorityRequest;
import com.loyalty.service_admin.domain.entity.DiscountPriorityEntity;
import com.loyalty.service_admin.domain.entity.DiscountSettingsEntity;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ConfigurationBusinessValidator Unit Tests")
class ConfigurationBusinessValidatorTest {

    private ConfigurationBusinessValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ConfigurationBusinessValidator();
    }

    @Test
    @DisplayName("testValidateCreate_ValidRequest_DoesNotThrow")
    void testValidateCreate_ValidRequest_DoesNotThrow() {
        // Arrange
        ConfigurationCreateRequest request = mock(ConfigurationCreateRequest.class);
        when(request.currency()).thenReturn("USD");
        when(request.priority()).thenReturn(List.of(
                new DiscountPriorityRequest("PRODUCT", 1),
                new DiscountPriorityRequest("SEASONAL", 2)
        ));

        // Act & Assert
        assertDoesNotThrow(() -> validator.validateCreate(request));
    }

    @Test
    @DisplayName("testValidateCreate_InvalidCurrency_ThrowsBadRequest")
    void testValidateCreate_InvalidCurrency_ThrowsBadRequest() {
        // Arrange
        ConfigurationCreateRequest request = mock(ConfigurationCreateRequest.class);
        when(request.currency()).thenReturn("INVALID");
        when(request.priority()).thenReturn(List.of());

        // Act & Assert
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> validator.validateCreate(request));
        assertTrue(ex.getMessage().contains("currency"));
    }

    @Test
    @DisplayName("testValidateCreate_DuplicatePriorityOrder_ThrowsBadRequest")
    void testValidateCreate_DuplicatePriorityOrder_ThrowsBadRequest() {
        // Arrange
        ConfigurationCreateRequest request = mock(ConfigurationCreateRequest.class);
        when(request.currency()).thenReturn("USD");
        when(request.priority()).thenReturn(List.of(
                new DiscountPriorityRequest("PRODUCT", 1),
                new DiscountPriorityRequest("SEASONAL", 1)  // duplicate order
        ));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> validator.validateCreate(request));
    }

    @Test
    @DisplayName("testValidateCreate_DuplicateType_ThrowsBadRequest")
    void testValidateCreate_DuplicateType_ThrowsBadRequest() {
        // Arrange
        ConfigurationCreateRequest request = mock(ConfigurationCreateRequest.class);
        when(request.currency()).thenReturn("USD");
        when(request.priority()).thenReturn(List.of(
                new DiscountPriorityRequest("PRODUCT", 1),
                new DiscountPriorityRequest("PRODUCT", 2)  // duplicate type
        ));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> validator.validateCreate(request));
    }

    @Test
    @DisplayName("testValidatePatch_NullCurrencyAndPriority_DoesNotValidate")
    void testValidatePatch_NullCurrencyAndPriority_DoesNotValidate() {
        // Arrange
        ConfigurationPatchRequest request = mock(ConfigurationPatchRequest.class);
        when(request.currency()).thenReturn(null);
        when(request.priority()).thenReturn(null);

        // Act & Assert
        assertDoesNotThrow(() -> validator.validatePatch(request));
    }

    @Test
    @DisplayName("testValidatePatch_WithValidCurrency_DoesNotThrow")
    void testValidatePatch_WithValidCurrency_DoesNotThrow() {
        // Arrange
        ConfigurationPatchRequest request = mock(ConfigurationPatchRequest.class);
        when(request.currency()).thenReturn("EUR");
        when(request.priority()).thenReturn(null);

        // Act & Assert
        assertDoesNotThrow(() -> validator.validatePatch(request));
    }

    @Test
    @DisplayName("testValidatePatch_InvalidCurrency_ThrowsBadRequest")
    void testValidatePatch_InvalidCurrency_ThrowsBadRequest() {
        // Arrange
        ConfigurationPatchRequest request = mock(ConfigurationPatchRequest.class);
        when(request.currency()).thenReturn("XYZ123");

        // Act & Assert
        assertThrows(BadRequestException.class, () -> validator.validatePatch(request));
    }

    @Test
    @DisplayName("testValidateEntityState_NullCurrency_ThrowsBadRequest")
    void testValidateEntityState_NullCurrency_ThrowsBadRequest() {
        // Arrange
        DiscountSettingsEntity entity = new DiscountSettingsEntity();
        entity.setCurrencyCode(null);
        entity.setPriorities(new ArrayList<>());

        // Act & Assert
        assertThrows(BadRequestException.class, () -> validator.validateEntityState(entity));
    }

    @Test
    @DisplayName("testValidateEntityState_ValidEntity_DoesNotThrow")
    void testValidateEntityState_ValidEntity_DoesNotThrow() {
        // Arrange
        DiscountSettingsEntity entity = new DiscountSettingsEntity();
        entity.setCurrencyCode("USD");

        DiscountPriorityEntity p1 = new DiscountPriorityEntity();
        p1.setDiscountTypeId(UUID.randomUUID());
        p1.setPriorityLevel(1);
        DiscountPriorityEntity p2 = new DiscountPriorityEntity();
        p2.setDiscountTypeId(UUID.randomUUID());
        p2.setPriorityLevel(2);
        entity.setPriorities(List.of(p1, p2));

        // Act & Assert
        assertDoesNotThrow(() -> validator.validateEntityState(entity));
    }

    @Test
    @DisplayName("testValidateCreate_ZeroOrder_ThrowsBadRequest")
    void testValidateCreate_ZeroOrder_ThrowsBadRequest() {
        // Arrange
        ConfigurationCreateRequest request = mock(ConfigurationCreateRequest.class);
        when(request.currency()).thenReturn("USD");
        when(request.priority()).thenReturn(List.of(
                new DiscountPriorityRequest("PRODUCT", 0)
        ));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> validator.validateCreate(request));
    }

    @Test
    @DisplayName("testValidateCreate_BlankType_ThrowsBadRequest")
    void testValidateCreate_BlankType_ThrowsBadRequest() {
        // Arrange
        ConfigurationCreateRequest request = mock(ConfigurationCreateRequest.class);
        when(request.currency()).thenReturn("USD");
        when(request.priority()).thenReturn(List.of(
                new DiscountPriorityRequest("   ", 1)
        ));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> validator.validateCreate(request));
    }
}
