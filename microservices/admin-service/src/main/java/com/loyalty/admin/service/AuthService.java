package com.loyalty.admin.service;

import com.loyalty.admin.dto.LoginRequest;
import com.loyalty.admin.dto.LoginResponse;
import com.loyalty.admin.entity.User;
import com.loyalty.admin.exception.UnauthorizedException;
import com.loyalty.admin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;

    @Value("${app.jwt.secret}")
    private String secret;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .filter(User::isActive)
                .orElseThrow(() -> new UnauthorizedException("Usuario no valido"));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new UnauthorizedException("Credenciales invalidas");
        }

        String payload = user.getUsername() + ":" + user.getRole() + ":" + Instant.now().toEpochMilli();
        String token = Base64.getEncoder().encodeToString((payload + ":" + secret).getBytes(StandardCharsets.UTF_8));
        log.info("Login exitoso para usuario {}", user.getUsername());

        return LoginResponse.builder()
                .token(token)
                .tipo("Bearer")
                .username(user.getUsername())
                .build();
    }
}
