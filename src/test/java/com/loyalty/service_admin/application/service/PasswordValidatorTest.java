package com.loyalty.service_admin.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PasswordValidator Unit Tests")
class PasswordValidatorTest {

    private final PasswordValidator validator = new PasswordValidator();

    @Test
    @DisplayName("testIsValid_ValidPassword_ReturnsTrue")
    void testIsValid_ValidPassword_ReturnsTrue() {
        assertTrue(validator.isValid("SecurePass123"));
    }

    @Test
    @DisplayName("testIsValid_ExactlyMinLength_ReturnsTrue")
    void testIsValid_ExactlyMinLength_ReturnsTrue() {
        // 12 chars: Aaaaaaaaaa1a
        assertTrue(validator.isValid("Aaaaaaaaaa1a"));
    }

    @Test
    @DisplayName("testIsValid_TooShort_ReturnsFalse")
    void testIsValid_TooShort_ReturnsFalse() {
        assertFalse(validator.isValid("Short1A"));
    }

    @Test
    @DisplayName("testIsValid_NoUppercase_ReturnsFalse")
    void testIsValid_NoUppercase_ReturnsFalse() {
        assertFalse(validator.isValid("securepass123"));
    }

    @Test
    @DisplayName("testIsValid_NoLowercase_ReturnsFalse")
    void testIsValid_NoLowercase_ReturnsFalse() {
        assertFalse(validator.isValid("SECUREPASS123"));
    }

    @Test
    @DisplayName("testIsValid_NoDigit_ReturnsFalse")
    void testIsValid_NoDigit_ReturnsFalse() {
        assertFalse(validator.isValid("SecurePassword"));
    }

    @Test
    @DisplayName("testIsValid_NullPassword_ReturnsFalse")
    void testIsValid_NullPassword_ReturnsFalse() {
        assertFalse(validator.isValid(null));
    }

    @Test
    @DisplayName("testGetErrorMessage_ValidPassword_ReturnsNull")
    void testGetErrorMessage_ValidPassword_ReturnsNull() {
        assertNull(validator.getErrorMessage("SecurePass123"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("testGetErrorMessage_NullOrEmpty_ReturnsNotEmptyMessage")
    void testGetErrorMessage_NullOrEmpty_ReturnsNotEmptyMessage(String password) {
        String msg = validator.getErrorMessage(password);
        assertNotNull(msg);
        assertTrue(msg.contains("vacía"));
    }

    @Test
    @DisplayName("testGetErrorMessage_TooShort_ReturnsMinimoMessage")
    void testGetErrorMessage_TooShort_ReturnsMinimoMessage() {
        String msg = validator.getErrorMessage("Short1A");
        assertNotNull(msg);
        assertTrue(msg.contains("mínimo"));
        assertTrue(msg.contains("12"));
    }

    @Test
    @DisplayName("testGetErrorMessage_NoUppercase_ReturnsMayusculaMessage")
    void testGetErrorMessage_NoUppercase_ReturnsMayusculaMessage() {
        String msg = validator.getErrorMessage("securepass123");
        assertNotNull(msg);
        assertTrue(msg.contains("mayúscula"));
    }

    @Test
    @DisplayName("testGetErrorMessage_NoLowercase_ReturnsMinusculaMessage")
    void testGetErrorMessage_NoLowercase_ReturnsMinusculaMessage() {
        String msg = validator.getErrorMessage("SECUREPASS123");
        assertNotNull(msg);
        assertTrue(msg.contains("minúscula"));
    }

    @Test
    @DisplayName("testGetErrorMessage_NoDigit_ReturnsNumeroMessage")
    void testGetErrorMessage_NoDigit_ReturnsNumeroMessage() {
        String msg = validator.getErrorMessage("SecurePassword");
        assertNotNull(msg);
        assertTrue(msg.contains("número"));
    }

    @Test
    @DisplayName("testMinLengthConstant_IsCorrect")
    void testMinLengthConstant_IsCorrect() {
        assertEquals(12, PasswordValidator.MIN_LENGTH);
    }
}
