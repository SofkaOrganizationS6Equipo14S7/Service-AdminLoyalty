package com.loyalty.service_admin.infrastructure.exception;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpServletRequest request) {
        String errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed: " + errors, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException e, HttpServletRequest request) {
        String errors = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", errors, request);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiErrorResponse> handleJwtException(JwtException e, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Token no válido o expirado", request);
    }

    @ExceptionHandler({UnauthorizedException.class, AuthenticationCredentialsNotFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(RuntimeException e, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", e.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage(), request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException e, HttpServletRequest request) {
        String code = e.getMessage() != null && e.getMessage().startsWith("VALIDATION_ERROR")
                ? "VALIDATION_ERROR"
                : "BAD_REQUEST";
        return build(HttpStatus.BAD_REQUEST, code, e.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException e, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "CONFLICT", e.getMessage(), request);
    }

    @ExceptionHandler(ConfigurationAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleConfigurationAlreadyExists(ConfigurationAlreadyExistsException e, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "CONFIG_ALREADY_EXISTS", e.getMessage(), request);
    }

    @ExceptionHandler(ConfigurationNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleConfigurationNotFound(ConfigurationNotFoundException e, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "CONFIG_NOT_FOUND", e.getMessage(), request);
    }

    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthorization(AuthorizationException e, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", e.getMessage(), request);
    }

    @ExceptionHandler({
            ResourceNotFoundException.class,
            ApiKeyNotFoundException.class,
            EcommerceNotFoundException.class
    })
    public ResponseEntity<ApiErrorResponse> handleNotFound(RuntimeException e, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage(), request);
    }

    @ExceptionHandler(ClassificationValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleClassificationValidation(ClassificationValidationException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "CLASSIFICATION_VALIDATION_ERROR", e.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception e, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", e.getMessage(), request);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String code, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                request.getRequestURI()
        ));
    }
}
