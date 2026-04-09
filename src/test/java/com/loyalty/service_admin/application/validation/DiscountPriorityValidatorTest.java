package com.loyalty.service_admin.application.validation;

import com.loyalty.service_admin.application.dto.rules.discount.DiscountLimitPriorityRequest;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DiscountPriorityValidator Unit Tests")
class DiscountPriorityValidatorTest {

    private DiscountPriorityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DiscountPriorityValidator();
    }

    @Test
    @DisplayName("testValidatePriorities_ValidSequential_DoesNotThrow")
    void testValidatePriorities_ValidSequential_DoesNotThrow() {
        // Arrange
        UUID configId = UUID.randomUUID();
        DiscountLimitPriorityRequest request = new DiscountLimitPriorityRequest(configId,
                List.of(
                        new DiscountLimitPriorityRequest.PriorityEntry(UUID.randomUUID(), 1),
                        new DiscountLimitPriorityRequest.PriorityEntry(UUID.randomUUID(), 2),
                        new DiscountLimitPriorityRequest.PriorityEntry(UUID.randomUUID(), 3)
                ));

        // Act & Assert
        assertDoesNotThrow(() -> validator.validatePriorities(request));
    }

    @Test
    @DisplayName("testValidatePriorities_EmptyList_ThrowsBadRequest")
    void testValidatePriorities_EmptyList_ThrowsBadRequest() {
        // Arrange
        DiscountLimitPriorityRequest request = new DiscountLimitPriorityRequest(UUID.randomUUID(), List.of());

        // Act & Assert
        assertThrows(BadRequestException.class, () -> validator.validatePriorities(request));
    }

    @Test
    @DisplayName("testValidatePriorities_NullList_ThrowsBadRequest")
    void testValidatePriorities_NullList_ThrowsBadRequest() {
        // Arrange
        DiscountLimitPriorityRequest request = new DiscountLimitPriorityRequest(UUID.randomUUID(), null);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> validator.validatePriorities(request));
    }

    @Test
    @DisplayName("testValidatePriorities_DuplicateLevels_ThrowsBadRequest")
    void testValidatePriorities_DuplicateLevels_ThrowsBadRequest() {
        // Arrange
        DiscountLimitPriorityRequest request = new DiscountLimitPriorityRequest(UUID.randomUUID(),
                List.of(
                        new DiscountLimitPriorityRequest.PriorityEntry(UUID.randomUUID(), 1),
                        new DiscountLimitPriorityRequest.PriorityEntry(UUID.randomUUID(), 1)
                ));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> validator.validatePriorities(request));
    }

    @Test
    @DisplayName("testValidatePriorities_GapInSequence_ThrowsBadRequest")
    void testValidatePriorities_GapInSequence_ThrowsBadRequest() {
        // Arrange
        DiscountLimitPriorityRequest request = new DiscountLimitPriorityRequest(UUID.randomUUID(),
                List.of(
                        new DiscountLimitPriorityRequest.PriorityEntry(UUID.randomUUID(), 1),
                        new DiscountLimitPriorityRequest.PriorityEntry(UUID.randomUUID(), 3) // gap at 2
                ));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> validator.validatePriorities(request));
    }

    @Test
    @DisplayName("testValidatePriorities_NotStartingAt1_ThrowsBadRequest")
    void testValidatePriorities_NotStartingAt1_ThrowsBadRequest() {
        // Arrange
        DiscountLimitPriorityRequest request = new DiscountLimitPriorityRequest(UUID.randomUUID(),
                List.of(
                        new DiscountLimitPriorityRequest.PriorityEntry(UUID.randomUUID(), 2),
                        new DiscountLimitPriorityRequest.PriorityEntry(UUID.randomUUID(), 3)
                ));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> validator.validatePriorities(request));
    }

    @Test
    @DisplayName("testValidatePriorities_DuplicateDiscountTypes_ThrowsBadRequest")
    void testValidatePriorities_DuplicateDiscountTypes_ThrowsBadRequest() {
        // Arrange
        UUID sameTypeId = UUID.randomUUID();
        DiscountLimitPriorityRequest request = new DiscountLimitPriorityRequest(UUID.randomUUID(),
                List.of(
                        new DiscountLimitPriorityRequest.PriorityEntry(sameTypeId, 1),
                        new DiscountLimitPriorityRequest.PriorityEntry(sameTypeId, 2) // duplicate type
                ));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> validator.validatePriorities(request));
    }

    @Test
    @DisplayName("testValidatePriorities_SingleEntry_DoesNotThrow")
    void testValidatePriorities_SingleEntry_DoesNotThrow() {
        // Arrange
        DiscountLimitPriorityRequest request = new DiscountLimitPriorityRequest(UUID.randomUUID(),
                List.of(new DiscountLimitPriorityRequest.PriorityEntry(UUID.randomUUID(), 1)));

        // Act & Assert
        assertDoesNotThrow(() -> validator.validatePriorities(request));
    }
}
