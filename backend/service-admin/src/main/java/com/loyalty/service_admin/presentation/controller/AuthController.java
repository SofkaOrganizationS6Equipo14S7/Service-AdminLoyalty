package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.auth.LoginRequest;
import com.loyalty.service_admin.application.dto.auth.LoginResponse;
import com.loyalty.service_admin.application.dto.user.UserResponse;
import com.loyalty.service_admin.application.port.in.AuthUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final AuthUseCase authUseCase;
    
    /**
     * @param request credentials (username, password)
     * @return HTTP 200 OK with LoginResponse containing JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authUseCase.login(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * @param authHeader Authorization header with Bearer token
     * @return HTTP 204 No Content
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            authUseCase.logout(token);
        }
        return ResponseEntity.noContent().build();
    }
    
    /**
     * @param authHeader Authorization header with Bearer token
     * @return HTTP 200 OK with user data or 401 if unauthorized
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String token = authHeader.substring(7);
        UserResponse user = authUseCase.getCurrentUser(token);
        return ResponseEntity.ok(user);
    }
}
