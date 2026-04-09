package com.loyalty.service_admin.infrastructure.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Exception Classes Unit Tests")
class ExceptionClassesTest {

    @Test
    void testUnauthorizedException_message() {
        UnauthorizedException ex = new UnauthorizedException("msg");
        assertEquals("msg", ex.getMessage());
    }

    @Test
    void testUnauthorizedException_messageAndCause() {
        Throwable cause = new RuntimeException("root");
        UnauthorizedException ex = new UnauthorizedException("msg", cause);
        assertEquals("msg", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void testAuthorizationException_message() {
        AuthorizationException ex = new AuthorizationException("msg");
        assertEquals("msg", ex.getMessage());
    }

    @Test
    void testAuthorizationException_messageAndCause() {
        Throwable cause = new RuntimeException("root");
        AuthorizationException ex = new AuthorizationException("msg", cause);
        assertEquals(cause, ex.getCause());
    }

    @Test
    void testBadRequestException_message() {
        BadRequestException ex = new BadRequestException("msg");
        assertEquals("msg", ex.getMessage());
    }

    @Test
    void testBadRequestException_messageAndCause() {
        Throwable cause = new RuntimeException("root");
        BadRequestException ex = new BadRequestException("msg", cause);
        assertEquals(cause, ex.getCause());
    }

    @Test
    void testConflictException_message() {
        ConflictException ex = new ConflictException("msg");
        assertEquals("msg", ex.getMessage());
    }

    @Test
    void testConflictException_messageAndCause() {
        Throwable cause = new RuntimeException("root");
        ConflictException ex = new ConflictException("msg", cause);
        assertEquals(cause, ex.getCause());
    }

    @Test
    void testResourceNotFoundException_message() {
        ResourceNotFoundException ex = new ResourceNotFoundException("msg");
        assertEquals("msg", ex.getMessage());
    }

    @Test
    void testResourceNotFoundException_messageAndCause() {
        Throwable cause = new RuntimeException("root");
        ResourceNotFoundException ex = new ResourceNotFoundException("msg", cause);
        assertEquals(cause, ex.getCause());
    }

    @Test
    void testApiKeyNotFoundException_message() {
        ApiKeyNotFoundException ex = new ApiKeyNotFoundException("msg");
        assertEquals("msg", ex.getMessage());
    }

    @Test
    void testApiKeyNotFoundException_messageAndCause() {
        Throwable cause = new RuntimeException("root");
        ApiKeyNotFoundException ex = new ApiKeyNotFoundException("msg", cause);
        assertEquals(cause, ex.getCause());
    }

    @Test
    void testEcommerceNotFoundException_message() {
        EcommerceNotFoundException ex = new EcommerceNotFoundException("msg");
        assertEquals("msg", ex.getMessage());
    }

    @Test
    void testEcommerceNotFoundException_messageAndCause() {
        Throwable cause = new RuntimeException("root");
        EcommerceNotFoundException ex = new EcommerceNotFoundException("msg", cause);
        assertEquals(cause, ex.getCause());
    }

    @Test
    void testConfigurationAlreadyExistsException() {
        ConfigurationAlreadyExistsException ex = new ConfigurationAlreadyExistsException("msg");
        assertEquals("msg", ex.getMessage());
    }

    @Test
    void testConfigurationNotFoundException() {
        ConfigurationNotFoundException ex = new ConfigurationNotFoundException("msg");
        assertEquals("msg", ex.getMessage());
    }

    @Test
    void testClassificationValidationException_message() {
        ClassificationValidationException ex = new ClassificationValidationException("msg");
        assertEquals("msg", ex.getMessage());
        assertInstanceOf(BadRequestException.class, ex);
    }

    @Test
    void testClassificationValidationException_messageAndCause() {
        Throwable cause = new RuntimeException("root");
        ClassificationValidationException ex = new ClassificationValidationException("msg", cause);
        assertEquals(cause, ex.getCause());
    }
}
