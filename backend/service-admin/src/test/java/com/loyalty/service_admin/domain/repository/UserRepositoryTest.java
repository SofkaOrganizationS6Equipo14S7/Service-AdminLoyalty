package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserRepositoryTest: Pruebas de persistencia para UserRepository
 * 
 * Utiliza:
 * - @DataJpaTest: Carga solo el contexto de JPA con H2 embedded (sin Spring Security ni Web)
 * - TestEntityManager: Para garantizar transacciones vs. queries normales
 * - Scope test: No ejecuta entre modules
 * 
 * Criterios de aceptación (SPEC-001):
 * - Buscar usuario por username debe retornar Optional<UserEntity>
 * - Si el usuario no existe, debe retornar Optional.empty()
 * - Query debe ser case-sensitive (username exact match)
 */
@DataJpaTest
@DisplayName("UserRepository Persistence Tests")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UserEntity persistedUser;
    private static final String TEST_USERNAME = "test_admin";
    private static final String TEST_PASSWORD = "$2a$11$hashed_password_here";

    @BeforeEach
    void setUp() {
        // Create and persist a test user via TestEntityManager to ensure DB state
        persistedUser = UserEntity.builder()
                .username(TEST_USERNAME)
                .password(TEST_PASSWORD)
                .role("ADMIN")
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        // Persist using TestEntityManager and flush to ensure DB state
        entityManager.persistAndFlush(persistedUser);
    }

    @Test
    @DisplayName("findByUsername retorna Optional con usuario existente")
    void testFindByUsername_userExists_returnsOptionalWithUser() {
        // Act
        Optional<UserEntity> result = userRepository.findByUsername(TEST_USERNAME);

        // Assert
        assertTrue(result.isPresent());
        UserEntity foundUser = result.get();
        assertEquals(TEST_USERNAME, foundUser.getUsername());
        assertEquals("ADMIN", foundUser.getRole());
        assertTrue(foundUser.getActive());
    }

    @Test
    @DisplayName("findByUsername retorna Optional.empty cuando usuario no existe")
    void testFindByUsername_userNotFound_returnsEmptyOptional() {
        // Act
        Optional<UserEntity> result = userRepository.findByUsername("nonexistent_user");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findByUsername es case-sensitive (username exacto)")
    void testFindByUsername_caseSensitive_exactMatchRequired() {
        // Act & Assert - uppercase username should NOT match
        Optional<UserEntity> resultUpperCase = userRepository.findByUsername(TEST_USERNAME.toUpperCase());
        assertTrue(resultUpperCase.isEmpty());

        // Act & Assert - partially matching username should NOT match
        Optional<UserEntity> resultPartial = userRepository.findByUsername("test");
        assertTrue(resultPartial.isEmpty());

        // Act & Assert - correct username should match
        Optional<UserEntity> resultExact = userRepository.findByUsername(TEST_USERNAME);
        assertTrue(resultExact.isPresent());
    }

    @Test
    @DisplayName("findByUsername con usuario inactivo retorna Optional con usuario")
    void testFindByUsername_inactiveUser_returnsOptionalWithUser() {
        // Arrange - create and persist an inactive user
        UserEntity inactiveUser = UserEntity.builder()
                .username("inactive_admin")
                .password(TEST_PASSWORD)
                .role("USER")
                .active(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        entityManager.persistAndFlush(inactiveUser);

        // Act
        Optional<UserEntity> result = userRepository.findByUsername("inactive_admin");

        // Assert - repository returns the user regardless of active status (status check is service-level)
        assertTrue(result.isPresent());
        UserEntity foundUser = result.get();
        assertEquals("inactive_admin", foundUser.getUsername());
        assertFalse(foundUser.getActive());
    }

    @Test
    @DisplayName("Múltiples usuarios pueden coexistir y ser consultados por username")
    void testFindByUsername_multipleUsers_eachReturnedByUsername() {
        // Arrange - create additional users
        UserEntity user1 = UserEntity.builder()
                .username("alice")
                .password(TEST_PASSWORD)
                .role("ADMIN")
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        UserEntity user2 = UserEntity.builder()
                .username("bob")
                .password(TEST_PASSWORD)
                .role("USER")
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        entityManager.persistAndFlush(user1);
        entityManager.persistAndFlush(user2);

        // Act & Assert
        assertTrue(userRepository.findByUsername("alice").isPresent());
        assertTrue(userRepository.findByUsername("bob").isPresent());
        assertTrue(userRepository.findByUsername(TEST_USERNAME).isPresent());
        assertTrue(userRepository.findByUsername("charlie").isEmpty());
    }

    @Test
    @DisplayName("findByUsername retorna usuario con todos sus campos íntegros")
    void testFindByUsername_returnsUserWithAllFieldsIntact() {
        // Act
        Optional<UserEntity> result = userRepository.findByUsername(TEST_USERNAME);

        // Assert
        assertTrue(result.isPresent());
        UserEntity user = result.get();
        assertNotNull(user.getId());
        assertEquals(TEST_USERNAME, user.getUsername());
        assertEquals(TEST_PASSWORD, user.getPassword());
        assertEquals("ADMIN", user.getRole());
        assertTrue(user.getActive());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }
}
