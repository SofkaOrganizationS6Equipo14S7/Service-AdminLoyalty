package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.auth.ChangePasswordRequest;
import com.loyalty.service_admin.application.dto.auth.LoginResponse;
import com.loyalty.service_admin.application.dto.user.*;
import com.loyalty.service_admin.application.port.in.*;
import com.loyalty.service_admin.application.service.UserService;
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
@DisplayName("UserController Unit Tests")
class UserControllerTest {

    @Mock
    private UserCreateUseCase userCreateUseCase;
    @Mock
    private UserListUseCase userListUseCase;
    @Mock
    private UserGetByIdUseCase userGetByIdUseCase;
    @Mock
    private UserUpdateUseCase userUpdateUseCase;
    @Mock
    private UserDeleteUseCase userDeleteUseCase;
    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private UserResponse buildUserResponse() {
        return new UserResponse(
                UUID.randomUUID(), "testuser", UUID.randomUUID(), "STORE_ADMIN",
                "test@test.com", UUID.randomUUID(), true, Instant.now(), Instant.now()
        );
    }

    @Test
    @DisplayName("createUser returns 201 Created")
    void createUser_returns201() {
        UserCreateRequest request = new UserCreateRequest("newuser", "new@test.com", "Password12345!", UUID.randomUUID(), UUID.randomUUID());
        UserResponse response = buildUserResponse();
        when(userCreateUseCase.createUser(any())).thenReturn(response);

        ResponseEntity<UserResponse> result = userController.createUser(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(response, result.getBody());
        verify(userCreateUseCase).createUser(request);
    }

    @Test
    @DisplayName("listUsers returns 200 OK with paginated results")
    void listUsers_returns200() {
        UUID ecommerceId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserResponse> page = new PageImpl<>(List.of(buildUserResponse()));
        when(userListUseCase.listUsers(eq(ecommerceId), any(Pageable.class))).thenReturn(page);

        ResponseEntity<Page<UserResponse>> result = userController.listUsers(ecommerceId, pageable);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().getTotalElements());
    }

    @Test
    @DisplayName("listUsers without ecommerceId filter returns 200")
    void listUsers_noFilter_returns200() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserResponse> page = new PageImpl<>(List.of());
        when(userListUseCase.listUsers(eq(null), any(Pageable.class))).thenReturn(page);

        ResponseEntity<Page<UserResponse>> result = userController.listUsers(null, pageable);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    @DisplayName("getUser returns 200 OK with user")
    void getUser_returns200() {
        UUID uid = UUID.randomUUID();
        UserResponse response = buildUserResponse();
        when(userGetByIdUseCase.getUserById(uid)).thenReturn(response);

        ResponseEntity<UserResponse> result = userController.getUser(uid);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    @DisplayName("updateUser returns 200 OK with updated user")
    void updateUser_returns200() {
        UUID uid = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest("updated", "upd@test.com", null, null, null, null);
        UserResponse response = buildUserResponse();
        when(userUpdateUseCase.updateUser(eq(uid), any())).thenReturn(response);

        ResponseEntity<UserResponse> result = userController.updateUser(uid, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    @DisplayName("deleteUser returns 204 No Content")
    void deleteUser_returns204() {
        UUID uid = UUID.randomUUID();
        doNothing().when(userDeleteUseCase).hardDeleteUser(uid);

        ResponseEntity<Void> result = userController.deleteUser(uid);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(userDeleteUseCase).hardDeleteUser(uid);
    }

    @Test
    @DisplayName("updateProfile returns 200 OK")
    void updateProfile_returns200() {
        UpdateProfileRequest request = new UpdateProfileRequest("newemail@test.com");
        UserResponse response = buildUserResponse();
        when(userService.updateProfile(any())).thenReturn(response);

        ResponseEntity<UserResponse> result = userController.updateProfile(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    @DisplayName("changePassword returns 200 OK with new token")
    void changePassword_returns200() {
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass12345!", "NewPass12345!", "NewPass12345!");
        LoginResponse loginResponse = new LoginResponse("jwt-token", "Bearer", "user", "STORE_ADMIN");
        when(userService.changePassword(any())).thenReturn(loginResponse);

        ResponseEntity<LoginResponse> result = userController.changePassword(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("jwt-token", result.getBody().token());
    }
}
