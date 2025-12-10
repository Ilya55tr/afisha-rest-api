package by.ilyatr.afisha_rest_api.unit.service;

import by.ilyatr.afisha_rest_api.Exception.UserNotFoundException;
import by.ilyatr.afisha_rest_api.dto.UserDto;
import by.ilyatr.afisha_rest_api.entities.User;
import by.ilyatr.afisha_rest_api.mapper.UserMapper;
import by.ilyatr.afisha_rest_api.repositories.UserRepository;
import by.ilyatr.afisha_rest_api.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceUnitTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserDto userDto;
    private String userId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        userId = UUID.randomUUID().toString();
        userDto = new UserDto(userId, "John Doe", "john.doe@example.com", "password123", Instant.now());
        user = User.builder()
                .id(userId)
                .name("John Doe")
                .email("john.doe@example.com")
                .password("password123")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void createUser_ShouldReturnCreatedUser() {
        // Arrange
        when(userMapper.toUser(any(UserDto.class))).thenReturn(user);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userRepository.findById(any(String.class))).thenReturn(Optional.of(user));
        when(userMapper.toUserDto(any(User.class))).thenReturn(userDto);

        // Act
        UserDto result = userService.createUser(userDto);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("John Doe", result.getName());
        assertEquals("john.doe@example.com", result.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
        verify(userRepository, times(1)).findById(any(String.class));
    }

    @Test
    void createUser_WhenRepositoryFails_ShouldThrowException() {
        // Arrange
        when(userMapper.toUser(any(UserDto.class))).thenReturn(user);
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            userService.createUser(userDto);
        });
        
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void getUserById_ShouldReturnUserWhenFound() {
        // Arrange
        when(userRepository.findById(any(String.class))).thenReturn(Optional.of(user));
        when(userMapper.toUserDto(any(User.class))).thenReturn(userDto);

        // Act
        UserDto result = userService.getUserById(userId);

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals("John Doe", result.getName());
        verify(userRepository, times(1)).findById(any(String.class));
    }

    @Test
    void getUserById_ShouldThrowUserNotFoundExceptionWhenNotFound() {
        // Arrange
        when(userRepository.findById(any(String.class))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            userService.getUserById("nonexistent");
        });
        
        verify(userRepository, times(1)).findById("nonexistent");
    }

    @Test
    void updateUser_ShouldReturnUpdatedUserWhenFound() {
        // Arrange
        UserDto updatedUserDto = new UserDto(userId, "Jane Doe", "jane.doe@example.com", "newpassword456", Instant.now());
        User updatedUser = User.builder()
                .id(userId)
                .name("Jane Doe")
                .email("jane.doe@example.com")
                .password("newpassword456")
                .createdAt(Instant.now())
                .build();
        
        when(userRepository.findById(any(String.class))).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(userMapper.toUserDto(any(User.class))).thenReturn(updatedUserDto);

        // Act
        UserDto result = userService.updateUser(userId, updatedUserDto);

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals("Jane Doe", result.getName());
        assertEquals("jane.doe@example.com", result.getEmail());
        verify(userRepository, times(1)).findById(any(String.class));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void updateUser_ShouldThrowUserNotFoundExceptionWhenNotFound() {
        // Arrange
        when(userRepository.findById(any(String.class))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            userService.updateUser("nonexistent", userDto);
        });
        
        verify(userRepository, times(1)).findById("nonexistent");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_WhenRepositoryFails_ShouldThrowException() {
        // Arrange
        when(userRepository.findById(any(String.class))).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            userService.updateUser(userId, userDto);
        });
        
        verify(userRepository, times(1)).findById(any(String.class));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void deleteUser_ShouldReturnTrueWhenUserExists() {
        // Arrange
        when(userRepository.existsById(any(String.class))).thenReturn(true);
        doNothing().when(userRepository).deleteById(any(String.class));

        // Act
        boolean result = userService.deleteUser(userId);

        // Assert
        assertTrue(result);
        verify(userRepository, times(1)).existsById(any(String.class));
        verify(userRepository, times(1)).deleteById(any(String.class));
    }

    @Test
    void deleteUser_ShouldReturnFalseWhenUserDoesNotExist() {
        // Arrange
        when(userRepository.existsById(any(String.class))).thenReturn(false);

        // Act
        boolean result = userService.deleteUser("nonexistent");

        // Assert
        assertFalse(result);
        verify(userRepository, times(1)).existsById("nonexistent");
        verify(userRepository, never()).deleteById(any(String.class));
    }

    @Test
    void deleteUser_WhenRepositoryFails_ShouldThrowException() {
        // Arrange
        when(userRepository.existsById(any(String.class))).thenReturn(true);
        doThrow(new RuntimeException("Database error")).when(userRepository).deleteById(any(String.class));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            userService.deleteUser(userId);
        });
        
        verify(userRepository, times(1)).existsById(any(String.class));
        verify(userRepository, times(1)).deleteById(any(String.class));
    }
}