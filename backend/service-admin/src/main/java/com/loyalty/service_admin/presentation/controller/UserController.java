package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.user.UserCreateRequest;
import com.loyalty.service_admin.application.dto.user.UserResponse;
import com.loyalty.service_admin.application.dto.user.UserUpdateRequest;
import com.loyalty.service_admin.application.dto.user.UpdateProfileRequest;
import com.loyalty.service_admin.application.dto.auth.ChangePasswordRequest;
import com.loyalty.service_admin.application.dto.auth.LoginResponse;
import com.loyalty.service_admin.application.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    
    private final UserService userService;
    
    /**
     * @param request user data (role, username, email, password, ecommerceId)
     * @return HTTP 201 Created with UserResponse
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * @param ecommerceId filter parameter (optional)
     * @return HTTP 200 OK with list of UserResponse
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> listUsers(
            @RequestParam(name = "ecommerceId", required = false) UUID ecommerceId) {
        List<UserResponse> users = userService.listUsers(ecommerceId);
        return ResponseEntity.ok(users);
    }
    
    /**
     * @param uid user identifier
     * @return HTTP 200 OK with UserResponse
     */
    @GetMapping("/{uid}")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID uid) {
        UserResponse user = userService.getUserByUid(uid);
        return ResponseEntity.ok(user);
    }
    
    /**
     * Actualizar usuario - CRITERIO-1.3, CRITERIO-1.4
     * @param uid user identifier
     * @param request user data to update
     *        Optional fields: username, email, password
     *        SUPER_ADMIN only: ecommerceId, active
     *        PROHIBITED (immutable): roleId → returns HTTP 400 if sent
     * @return HTTP 200 OK with updated UserResponse
     */
    @PutMapping("/{uid}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID uid,
            @Valid @RequestBody UserUpdateRequest request) {
        UserResponse updated = userService.updateUser(uid, request);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * @param uid user identifier
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{uid}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID uid) {
        userService.deleteUser(uid);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * @param request profile data to update (name, email)
     * @return HTTP 200 OK with updated UserResponse
     */
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        UserResponse updated = userService.updateProfile(request);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * @param request password change data (currentPassword, newPassword, confirmPassword)
     * @return HTTP 200 OK with new JWT token
     */
    @PutMapping("/me/password")
    public ResponseEntity<LoginResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        LoginResponse response = userService.changePassword(request);
        return ResponseEntity.ok(response);
    }
}
