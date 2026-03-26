package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Tests unitarios para la capa de Repository (UserRepository).
 * Utiliza @DataJpaTest para testing de JPA sin cargar el contexto completo de Spring.
 */
@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.flyway.enabled=false"
})
@DisplayName("UserRepository Tests")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        testUser = UserEntity.builder()
                .username("testuser")
                .password("password123")
                .role("USER")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Debe guardar un usuario correctamente en la base de datos")
    void testSaveUser_success() {
        // Arrange
        UserEntity newUser = UserEntity.builder()
                .username("newuser")
                .password("newpass123")
                .role("USER")
                .active(true)
                .build();

        // Act
        UserEntity savedUser = userRepository.save(newUser);

        // Assert
        assertNotNull(savedUser.getId());
        assertEquals("newuser", savedUser.getUsername());
        assertEquals("USER", savedUser.getRole());
        assertTrue(savedUser.getActive());
        assertNotNull(savedUser.getCreatedAt());
    }

    @Test
    @DisplayName("Debe encontrar un usuario por nombre de usuario")
    void testFindByUsername_success() {
        // Arrange
        userRepository.save(testUser);

        // Act
        Optional<UserEntity> foundUser = userRepository.findByUsername("testuser");

        // Assert
        assertTrue(foundUser.isPresent());
        assertEquals("testuser", foundUser.get().getUsername());
        assertEquals("password123", foundUser.get().getPassword());
    }

    @Test
    @DisplayName("Debe retornar empty cuando el usuario no existe")
    void testFindByUsername_notFound() {
        // Act
        Optional<UserEntity> foundUser = userRepository.findByUsername("nonexistent");

        // Assert
        assertFalse(foundUser.isPresent());
    }

    @Test
    @DisplayName("Debe garantizar unicidad del username")
    void testFindByUsername_uniqueConstraint() {
        // Arrange
        userRepository.save(testUser);
        UserEntity duplicateUser = UserEntity.builder()
                .username("testuser")  // Mismo username
                .password("differentpass")
                .role("ADMIN")
                .active(true)
                .build();

        // Act & Assert
        assertThrows(Exception.class, () -> userRepository.save(duplicateUser));
    }

    @Test
    @DisplayName("Debe cargar un usuario existente y permitir actualización")
    void testUpdateUser_success() {
        // Arrange
        UserEntity savedUser = userRepository.save(testUser);
        Long userId = savedUser.getId();

        // Act
        savedUser.setRole("ADMIN");
        savedUser.setActive(false);
        UserEntity updatedUser = userRepository.save(savedUser);

        // Assert
        assertEquals(userId, updatedUser.getId());
        assertEquals("ADMIN", updatedUser.getRole());
        assertFalse(updatedUser.getActive());
        assertNotNull(updatedUser.getUpdatedAt());
    }

    @Test
    @DisplayName("Debe eliminar un usuario por ID")
    void testDeleteUser_success() {
        // Arrange
        UserEntity savedUser = userRepository.save(testUser);
        Long userId = savedUser.getId();

        // Act
        userRepository.deleteById(userId);

        // Assert
        Optional<UserEntity> deletedUser = userRepository.findById(userId);
        assertFalse(deletedUser.isPresent());
    }

    @Test
    @DisplayName("Debe contar el total de usuarios en la base de datos")
    void testCountUsers() {
        // Arrange
        userRepository.save(testUser);
        UserEntity secondUser = UserEntity.builder()
                .username("seconduser")
                .password("pass456")
                .role("USER")
                .active(true)
                .build();
        userRepository.save(secondUser);

        // Act
        long count = userRepository.count();

        // Assert
        assertEquals(2, count);
    }

    @Test
    @DisplayName("Debe verificar que los timestamps se generan automáticamente")
    void testTimestampsAutoGeneration() {
        // Arrange
        UserEntity newUser = UserEntity.builder()
                .username("timestamptest")
                .password("pass789")
                .role("USER")
                .active(true)
                .build();

        // Act
        UserEntity savedUser = userRepository.save(newUser);

        // Assert
        assertNotNull(savedUser.getCreatedAt());
        assertNotNull(savedUser.getUpdatedAt());
        // Ambos timestamps son generados en el mismo proceso, pero pueden tener nanosegundos de diferencia
        assertTrue(
            savedUser.getCreatedAt().isBefore(savedUser.getUpdatedAt().plusNanos(1)) &&
            savedUser.getUpdatedAt().isBefore(savedUser.getCreatedAt().plusSeconds(1)),
            "createdAt y updatedAt deben estar muy cercanos en el tiempo"
        );
    }
}
