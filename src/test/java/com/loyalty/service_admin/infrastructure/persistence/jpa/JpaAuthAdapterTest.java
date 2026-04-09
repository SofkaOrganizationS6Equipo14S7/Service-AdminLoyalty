package com.loyalty.service_admin.infrastructure.persistence.jpa;

import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaAuthAdapter Unit Tests")
class JpaAuthAdapterTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JpaAuthAdapter adapter;

    @Test
    void testFindByUsername_found() {
        // Arrange
        UserEntity user = new UserEntity();
        user.setUsername("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        // Act
        Optional<UserEntity> result = adapter.findByUsername("admin");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("admin", result.get().getUsername());
    }

    @Test
    void testFindByUsername_notFound() {
        // Arrange
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // Act
        Optional<UserEntity> result = adapter.findByUsername("unknown");

        // Assert
        assertTrue(result.isEmpty());
    }
}
