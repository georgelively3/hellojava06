package com.lithespeed.hellojava06.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lithespeed.hellojava06.entity.User;


import com.lithespeed.hellojava06.service.S3Service;
import com.lithespeed.hellojava06.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MainController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
})
@ActiveProfiles("test")
class MainControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private S3Service s3Service;

    // Mock the S3AsyncClient bean to prevent dependency injection issues
    @MockBean
    private software.amazon.awssdk.services.s3.S3AsyncClient s3AsyncClient;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private List<User> userList;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("johndoe");
        testUser.setEmail("john@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        User anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setUsername("janedoe");
        anotherUser.setEmail("jane@example.com");
        anotherUser.setFirstName("Jane");
        anotherUser.setLastName("Doe");

        userList = Arrays.asList(testUser, anotherUser);
    }

    // ========== USER CRUD TESTS ==========

    @Test
    void getAllUsers_ShouldReturnUserList() throws Exception {
        // Given
        when(userService.getAllUsers()).thenReturn(userList);

        // When & Then
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].username").value("johndoe"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].username").value("janedoe"));

        verify(userService, times(1)).getAllUsers();
    }

    @Test
    void getAllUsersPaginated_ShouldReturnPagedUsers() throws Exception {
        // Given
        Page<User> userPage = new PageImpl<>(userList, PageRequest.of(0, 10), userList.size());
        when(userService.getAllUsers(any())).thenReturn(userPage);

        // When & Then
        mockMvc.perform(get("/api/users/paginated")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));

        verify(userService, times(1)).getAllUsers(any());
    }

    @Test
    void getUserById_WithValidId_ShouldReturnUser() throws Exception {
        // Given
        when(userService.getUserById(1L)).thenReturn(testUser);

        // When & Then
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("johndoe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));

        verify(userService, times(1)).getUserById(1L);
    }

    @Test
    void getUserById_WithInvalidId_ShouldReturnNotFound() throws Exception {
        // Given
        when(userService.getUserById(999L)).thenThrow(new RuntimeException("User not found"));

        // When & Then
        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("User not found"));

        verify(userService, times(1)).getUserById(999L);
    }

    @Test
    void createUser_WithValidUser_ShouldReturnCreatedUser() throws Exception {
        // Given
        User newUser = new User("newuser", "new@example.com", "New", "User");
        when(userService.createUser(any(User.class))).thenReturn(testUser);

        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("johndoe"));

        verify(userService, times(1)).createUser(any(User.class));
    }

    @Test
    void createUser_WithInvalidUser_ShouldReturnBadRequest() throws Exception {
        // Given - User with blank username
        User invalidUser = new User("", "invalid@example.com", "Invalid", "User");

        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidUser)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createUser(any(User.class));
    }

    @Test
    void createUser_WithDuplicateUsername_ShouldReturnConflict() throws Exception {
        // Given
        User duplicateUser = new User("johndoe", "new@example.com", "New", "User");
        when(userService.createUser(any(User.class)))
                .thenThrow(new RuntimeException("Username already exists"));

        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateUser)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already exists"));

        verify(userService, times(1)).createUser(any(User.class));
    }

    @Test
    void updateUser_WithValidData_ShouldReturnUpdatedUser() throws Exception {
        // Given
        User updatedUser = new User("johndoe", "john.updated@example.com", "John", "Updated");
        when(userService.updateUser(anyLong(), any(User.class))).thenReturn(testUser);

        // When & Then
        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedUser)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1));

        verify(userService, times(1)).updateUser(eq(1L), any(User.class));
    }

    @Test
    void deleteUser_WithValidId_ShouldReturnSuccessMessage() throws Exception {
        // Given
        doNothing().when(userService).deleteUser(1L);

        // When & Then
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("User deleted successfully"));

        verify(userService, times(1)).deleteUser(1L);
    }

    @Test
    void getUserCount_ShouldReturnCount() throws Exception {
        // Given
        when(userService.getUserCount()).thenReturn(10L);

        // When & Then
        mockMvc.perform(get("/api/users/count"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.count").value(10));

        verify(userService, times(1)).getUserCount();
    }

    @Test
    void healthCheck_ShouldReturnHealthStatus() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("hellojava06"));
    }
}
