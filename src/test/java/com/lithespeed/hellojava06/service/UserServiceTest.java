package com.lithespeed.hellojava06.service;

import com.lithespeed.hellojava06.entity.User;
import com.lithespeed.hellojava06.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private User anotherUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("johndoe");
        testUser.setEmail("john@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");

        anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setUsername("janedoe");
        anotherUser.setEmail("jane@example.com");
        anotherUser.setFirstName("Jane");
        anotherUser.setLastName("Doe");
    }

    @Test
    void getAllUsers_ShouldReturnListOfUsers() {
        // Given
        List<User> expectedUsers = Arrays.asList(testUser, anotherUser);
        when(userRepository.findAll()).thenReturn(expectedUsers);

        // When
        List<User> actualUsers = userService.getAllUsers();

        // Then
        assertThat(actualUsers).hasSize(2);
        assertThat(actualUsers).containsExactlyInAnyOrder(testUser, anotherUser);
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getAllUsersWithPageable_ShouldReturnPageOfUsers() {
        // Given
        List<User> userList = Arrays.asList(testUser, anotherUser);
        Page<User> expectedPage = new PageImpl<>(userList);
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(pageable)).thenReturn(expectedPage);

        // When
        Page<User> actualPage = userService.getAllUsers(pageable);

        // Then
        assertThat(actualPage.getContent()).hasSize(2);
        assertThat(actualPage.getContent()).containsExactlyInAnyOrder(testUser, anotherUser);
        verify(userRepository, times(1)).findAll(pageable);
    }

    @Test
    void getUserById_WithValidId_ShouldReturnUser() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        User actualUser = userService.getUserById(1L);

        // Then
        assertThat(actualUser).isEqualTo(testUser);
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void getUserById_WithInvalidId_ShouldThrowRuntimeException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with id: 999");
        verify(userRepository, times(1)).findById(999L);
    }

    @Test
    void getUserByUsername_WithValidUsername_ShouldReturnUser() {
        // Given
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

        // When
        User actualUser = userService.getUserByUsername("johndoe");

        // Then
        assertThat(actualUser).isEqualTo(testUser);
        verify(userRepository, times(1)).findByUsername("johndoe");
    }

    @Test
    void getUserByUsername_WithInvalidUsername_ShouldThrowRuntimeException() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserByUsername("nonexistent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with username: nonexistent");
        verify(userRepository, times(1)).findByUsername("nonexistent");
    }

    @Test
    void getUserByEmail_WithValidEmail_ShouldReturnUser() {
        // Given
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));

        // When
        User actualUser = userService.getUserByEmail("john@example.com");

        // Then
        assertThat(actualUser).isEqualTo(testUser);
        verify(userRepository, times(1)).findByEmail("john@example.com");
    }

    @Test
    void getUserByEmail_WithInvalidEmail_ShouldThrowRuntimeException() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserByEmail("nonexistent@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with email: nonexistent@example.com");
        verify(userRepository, times(1)).findByEmail("nonexistent@example.com");
    }

    @Test
    void searchUsersByName_ShouldReturnMatchingUsers() {
        // Given
        List<User> expectedUsers = Arrays.asList(testUser);
        when(userRepository.findByNameContaining("John")).thenReturn(expectedUsers);

        // When
        List<User> actualUsers = userService.searchUsersByName("John");

        // Then
        assertThat(actualUsers).hasSize(1);
        assertThat(actualUsers).contains(testUser);
        verify(userRepository, times(1)).findByNameContaining("John");
    }

    @Test
    void createUser_WithValidUser_ShouldReturnSavedUser() {
        // Given
        User newUser = new User("newuser", "new@example.com", "New", "User");
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // When
        User savedUser = userService.createUser(newUser);

        // Then
        assertThat(savedUser).isEqualTo(newUser);
        verify(userRepository, times(1)).existsByUsername("newuser");
        verify(userRepository, times(1)).existsByEmail("new@example.com");
        verify(userRepository, times(1)).save(newUser);
    }

    @Test
    void createUser_WithDuplicateUsername_ShouldThrowRuntimeException() {
        // Given
        User newUser = new User("johndoe", "new@example.com", "New", "User");
        when(userRepository.existsByUsername("johndoe")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.createUser(newUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Username already exists: johndoe");
        verify(userRepository, times(1)).existsByUsername("johndoe");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_WithDuplicateEmail_ShouldThrowRuntimeException() {
        // Given
        User newUser = new User("newuser", "john@example.com", "New", "User");
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.createUser(newUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already exists: john@example.com");
        verify(userRepository, times(1)).existsByUsername("newuser");
        verify(userRepository, times(1)).existsByEmail("john@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_WithValidData_ShouldReturnUpdatedUser() {
        // Given
        User updatedUserData = new User("johndoe", "john.updated@example.com", "John", "Updated");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("john.updated@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.updateUser(1L, updatedUserData);

        // Then
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).existsByEmail("john.updated@example.com");
        verify(userRepository, times(1)).save(testUser);
        assertThat(testUser.getEmail()).isEqualTo("john.updated@example.com");
        assertThat(testUser.getLastName()).isEqualTo("Updated");
    }

    @Test
    void updateUser_WithNonExistentId_ShouldThrowRuntimeException() {
        // Given
        User updatedUserData = new User("johndoe", "john.updated@example.com", "John", "Updated");
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(999L, updatedUserData))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with id: 999");
        verify(userRepository, times(1)).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_WithDuplicateEmail_ShouldThrowRuntimeException() {
        // Given
        User updatedUserData = new User("johndoe", "jane@example.com", "John", "Updated");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(1L, updatedUserData))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already exists: jane@example.com");
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).existsByEmail("jane@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteUser_WithValidId_ShouldDeleteUser() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        userService.deleteUser(1L);

        // Then
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).delete(testUser);
    }

    @Test
    void deleteUser_WithInvalidId_ShouldThrowRuntimeException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.deleteUser(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with id: 999");
        verify(userRepository, times(1)).findById(999L);
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void userExists_WithValidId_ShouldReturnTrue() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(true);

        // When
        boolean exists = userService.userExists(1L);

        // Then
        assertThat(exists).isTrue();
        verify(userRepository, times(1)).existsById(1L);
    }

    @Test
    void userExists_WithInvalidId_ShouldReturnFalse() {
        // Given
        when(userRepository.existsById(999L)).thenReturn(false);

        // When
        boolean exists = userService.userExists(999L);

        // Then
        assertThat(exists).isFalse();
        verify(userRepository, times(1)).existsById(999L);
    }

    @Test
    void getUserCount_ShouldReturnCorrectCount() {
        // Given
        when(userRepository.count()).thenReturn(5L);

        // When
        long count = userService.getUserCount();

        // Then
        assertThat(count).isEqualTo(5L);
        verify(userRepository, times(1)).count();
    }
}
