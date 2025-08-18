package com.lithespeed.hellojava06.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lithespeed.hellojava06.entity.User;
import com.lithespeed.hellojava06.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@Testcontainers
@Transactional
@Disabled("Requires Docker - disabled for CI/CD environments without Docker")
class UserIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
    }

    @Test
    void createUser_ShouldPersistUserToDatabase() throws Exception {
        // When
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        // Then
        assertThat(userRepository.findByUsername("testuser")).isPresent();
    }

    @Test
    void getAllUsers_ShouldReturnUsersFromDatabase() throws Exception {
        // Given
        userRepository.save(testUser);

        User anotherUser = new User();
        anotherUser.setUsername("anotheruser");
        anotherUser.setEmail("another@example.com");
        anotherUser.setFirstName("Another");
        anotherUser.setLastName("User");
        userRepository.save(anotherUser);

        // When & Then
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].username").exists())
                .andExpect(jsonPath("$[1].username").exists());
    }

    @Test
    void getUserById_ShouldReturnUserFromDatabase() throws Exception {
        // Given
        User savedUser = userRepository.save(testUser);

        // When & Then
        mockMvc.perform(get("/api/users/" + savedUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedUser.getId()))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void updateUser_ShouldUpdateUserInDatabase() throws Exception {
        // Given
        User savedUser = userRepository.save(testUser);

        User updatedUser = new User();
        updatedUser.setUsername("testuser");
        updatedUser.setEmail("updated@example.com");
        updatedUser.setFirstName("Updated");
        updatedUser.setLastName("User");

        // When
        mockMvc.perform(put("/api/users/" + savedUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("updated@example.com"))
                .andExpect(jsonPath("$.firstName").value("Updated"));

        // Then
        User userFromDb = userRepository.findById(savedUser.getId()).orElse(null);
        assertThat(userFromDb).isNotNull();
        assertThat(userFromDb.getEmail()).isEqualTo("updated@example.com");
        assertThat(userFromDb.getFirstName()).isEqualTo("Updated");
    }

    @Test
    void deleteUser_ShouldRemoveUserFromDatabase() throws Exception {
        // Given
        User savedUser = userRepository.save(testUser);
        Long userId = savedUser.getId();

        // When
        mockMvc.perform(delete("/api/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User deleted successfully"));

        // Then
        assertThat(userRepository.findById(userId)).isEmpty();
    }

    @Test
    void searchUsersByName_ShouldReturnMatchingUsers() throws Exception {
        // Given
        userRepository.save(testUser);

        User anotherUser = new User();
        anotherUser.setUsername("johnsmith");
        anotherUser.setEmail("john@example.com");
        anotherUser.setFirstName("John");
        anotherUser.setLastName("Smith");
        userRepository.save(anotherUser);

        // When & Then - Search by first name
        mockMvc.perform(get("/api/users/search")
                .param("name", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].firstName").value("Test"));

        // Search by last name
        mockMvc.perform(get("/api/users/search")
                .param("name", "Smith"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].lastName").value("Smith"));
    }

    @Test
    void createUser_WithDuplicateUsername_ShouldReturnConflict() throws Exception {
        // Given
        userRepository.save(testUser);

        User duplicateUser = new User();
        duplicateUser.setUsername("testuser"); // Same username
        duplicateUser.setEmail("different@example.com");
        duplicateUser.setFirstName("Different");
        duplicateUser.setLastName("User");

        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateUser)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already exists: testuser"));
    }

    @Test
    void createUser_WithDuplicateEmail_ShouldReturnConflict() throws Exception {
        // Given
        userRepository.save(testUser);

        User duplicateUser = new User();
        duplicateUser.setUsername("differentuser");
        duplicateUser.setEmail("test@example.com"); // Same email
        duplicateUser.setFirstName("Different");
        duplicateUser.setLastName("User");

        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateUser)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists: test@example.com"));
    }

    @Test
    void createUser_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        // Given - User with blank username
        User invalidUser = new User();
        invalidUser.setUsername(""); // Invalid
        invalidUser.setEmail("invalid@example.com");
        invalidUser.setFirstName("Invalid");
        invalidUser.setLastName("User");

        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").exists());
    }
}
