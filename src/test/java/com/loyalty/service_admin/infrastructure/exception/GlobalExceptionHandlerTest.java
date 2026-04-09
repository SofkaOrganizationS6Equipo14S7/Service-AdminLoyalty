package com.loyalty.service_admin.infrastructure.exception;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    private void mockRequest() {
        when(request.getRequestURI()).thenReturn("/api/v1/test");
    }

    @Test
    void testHandleMethodArgumentNotValid() {
        mockRequest();
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("obj", "email", "must not be blank");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ApiErrorResponse> result = handler.handleMethodArgumentNotValid(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("VALIDATION_ERROR", result.getBody().code());
        assertTrue(result.getBody().message().contains("email"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testHandleConstraintViolation() {
        mockRequest();
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("name");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be null");
        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<ApiErrorResponse> result = handler.handleConstraintViolation(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals("VALIDATION_ERROR", result.getBody().code());
    }

    @Test
    void testHandleJwtException() {
        mockRequest();
        JwtException ex = new JwtException("expired");

        ResponseEntity<ApiErrorResponse> result = handler.handleJwtException(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertEquals("UNAUTHORIZED", result.getBody().code());
    }

    @Test
    void testHandleUnauthorizedException() {
        mockRequest();
        UnauthorizedException ex = new UnauthorizedException("Invalid token");

        ResponseEntity<ApiErrorResponse> result = handler.handleUnauthorized(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertEquals("Invalid token", result.getBody().message());
    }

    @Test
    void testHandleAuthenticationCredentialsNotFound() {
        mockRequest();
        AuthenticationCredentialsNotFoundException ex =
                new AuthenticationCredentialsNotFoundException("No credentials");

        ResponseEntity<ApiErrorResponse> result = handler.handleUnauthorized(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    }

    @Test
    void testHandleIllegalArgument() {
        mockRequest();
        IllegalArgumentException ex = new IllegalArgumentException("Invalid param");

        ResponseEntity<ApiErrorResponse> result = handler.handleIllegalArgument(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals("BAD_REQUEST", result.getBody().code());
    }

    @Test
    void testHandleBadRequest() {
        mockRequest();
        BadRequestException ex = new BadRequestException("Something wrong");

        ResponseEntity<ApiErrorResponse> result = handler.handleBadRequest(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals("BAD_REQUEST", result.getBody().code());
    }

    @Test
    void testHandleBadRequest_validationError() {
        mockRequest();
        BadRequestException ex = new BadRequestException("VALIDATION_ERROR: field is invalid");

        ResponseEntity<ApiErrorResponse> result = handler.handleBadRequest(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals("VALIDATION_ERROR", result.getBody().code());
    }

    @Test
    void testHandleConflict() {
        mockRequest();
        ConflictException ex = new ConflictException("Already exists");

        ResponseEntity<ApiErrorResponse> result = handler.handleConflict(ex, request);

        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
        assertEquals("CONFLICT", result.getBody().code());
    }

    @Test
    void testHandleConfigurationAlreadyExists() {
        mockRequest();
        ConfigurationAlreadyExistsException ex = new ConfigurationAlreadyExistsException("Config exists");

        ResponseEntity<ApiErrorResponse> result = handler.handleConfigurationAlreadyExists(ex, request);

        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
        assertEquals("CONFIG_ALREADY_EXISTS", result.getBody().code());
    }

    @Test
    void testHandleConfigurationNotFound() {
        mockRequest();
        ConfigurationNotFoundException ex = new ConfigurationNotFoundException("Config not found");

        ResponseEntity<ApiErrorResponse> result = handler.handleConfigurationNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        assertEquals("CONFIG_NOT_FOUND", result.getBody().code());
    }

    @Test
    void testHandleAuthorization() {
        mockRequest();
        AuthorizationException ex = new AuthorizationException("Forbidden");

        ResponseEntity<ApiErrorResponse> result = handler.handleAuthorization(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
        assertEquals("FORBIDDEN", result.getBody().code());
    }

    @Test
    void testHandleResourceNotFound() {
        mockRequest();
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found");

        ResponseEntity<ApiErrorResponse> result = handler.handleNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        assertEquals("NOT_FOUND", result.getBody().code());
    }

    @Test
    void testHandleApiKeyNotFound() {
        mockRequest();
        ApiKeyNotFoundException ex = new ApiKeyNotFoundException("Key not found");

        ResponseEntity<ApiErrorResponse> result = handler.handleNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    void testHandleEcommerceNotFound() {
        mockRequest();
        EcommerceNotFoundException ex = new EcommerceNotFoundException("Ecommerce not found");

        ResponseEntity<ApiErrorResponse> result = handler.handleNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    void testHandleClassificationValidation() {
        mockRequest();
        ClassificationValidationException ex = new ClassificationValidationException("Invalid classification");

        ResponseEntity<ApiErrorResponse> result = handler.handleClassificationValidation(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals("CLASSIFICATION_VALIDATION_ERROR", result.getBody().code());
    }

    @Test
    void testHandleGenericException() {
        mockRequest();
        Exception ex = new Exception("Unexpected error");

        ResponseEntity<ApiErrorResponse> result = handler.handleGenericException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertEquals("INTERNAL_ERROR", result.getBody().code());
    }

    @Test
    void testApiErrorResponse_fields() {
        mockRequest();
        Exception ex = new Exception("test");

        ResponseEntity<ApiErrorResponse> result = handler.handleGenericException(ex, request);
        ApiErrorResponse body = result.getBody();

        assertNotNull(body);
        assertNotNull(body.timestamp());
        assertEquals(500, body.status());
        assertEquals("Internal Server Error", body.error());
        assertEquals("INTERNAL_ERROR", body.code());
        assertEquals("test", body.message());
        assertEquals("/api/v1/test", body.path());
    }
}
