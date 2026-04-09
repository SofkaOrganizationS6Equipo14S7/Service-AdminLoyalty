package com.loyalty.service_admin.domain.model;

import com.loyalty.service_admin.domain.model.ecommerce.EcommerceStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Domain Model Enum Tests")
class DomainModelEnumTest {

    @Nested
    @DisplayName("DiscountType")
    class DiscountTypeTests {

        @Test
        void testFidelityDescription() {
            assertEquals("Descuento por fidelización", DiscountType.FIDELITY.getDescription());
        }

        @Test
        void testSeasonalDescription() {
            assertEquals("Descuento estacional", DiscountType.SEASONAL.getDescription());
        }

        @Test
        void testPromotionalDescription() {
            assertEquals("Descuento promocional", DiscountType.PROMOTIONAL.getDescription());
        }

        @Test
        void testValueOf() {
            assertEquals(DiscountType.FIDELITY, DiscountType.valueOf("FIDELITY"));
            assertEquals(DiscountType.SEASONAL, DiscountType.valueOf("SEASONAL"));
            assertEquals(DiscountType.PROMOTIONAL, DiscountType.valueOf("PROMOTIONAL"));
        }

        @Test
        void testValues() {
            assertEquals(3, DiscountType.values().length);
        }
    }

    @Nested
    @DisplayName("EcommerceStatus")
    class EcommerceStatusTests {

        @Test
        void testActiveDescription() {
            assertEquals("Ecommerce activo y operando", EcommerceStatus.ACTIVE.getDescription());
        }

        @Test
        void testInactiveDescription() {
            assertEquals("Ecommerce desactivado, usuarios sin acceso", EcommerceStatus.INACTIVE.getDescription());
        }

        @Test
        void testValueOf() {
            assertEquals(EcommerceStatus.ACTIVE, EcommerceStatus.valueOf("ACTIVE"));
            assertEquals(EcommerceStatus.INACTIVE, EcommerceStatus.valueOf("INACTIVE"));
        }

        @Test
        void testValues() {
            assertEquals(2, EcommerceStatus.values().length);
        }

        @Test
        void testInvalidValue_throwsIllegalArgument() {
            assertThrows(IllegalArgumentException.class, () -> EcommerceStatus.valueOf("UNKNOWN"));
        }
    }
}
