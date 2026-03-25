package com.loyalty.admin.service;

import com.loyalty.admin.dto.UserRequest;
import com.loyalty.admin.dto.UserResponse;
import com.loyalty.admin.entity.User;
import com.loyalty.admin.exception.NotFoundException;
import com.loyalty.admin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    public UserResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    public UserResponse create(UserRequest request) {
        User saved = userRepository.save(User.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .role(request.getRole())
                .active(request.isActive())
                .build());
        log.info("Usuario creado id={}", saved.getId());
        return toResponse(saved);
    }

    public UserResponse update(Long id, UserRequest request) {
        User user = getEntity(id);
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setRole(request.getRole());
        user.setActive(request.isActive());
        User saved = userRepository.save(user);
        log.info("Usuario actualizado id={}", saved.getId());
        return toResponse(saved);
    }

    public void delete(Long id) {
        User user = getEntity(id);
        userRepository.delete(user);
        log.info("Usuario eliminado id={}", id);
    }

    private User getEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .active(user.isActive())
                .build();
    }
}
