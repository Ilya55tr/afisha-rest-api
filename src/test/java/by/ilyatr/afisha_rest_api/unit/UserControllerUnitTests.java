package by.ilyatr.afisha_rest_api.unit;

import by.ilyatr.afisha_rest_api.Exception.UserNotFoundException;
import by.ilyatr.afisha_rest_api.controllers.rest.UserRestControllerV1;
import by.ilyatr.afisha_rest_api.dto.UserDto;
import by.ilyatr.afisha_rest_api.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserRestControllerV1.class)
@AutoConfigureMockMvc
class UserControllerUnitTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    CacheManager cacheManager;

    @MockitoBean
    private UserService userService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UserDto userDto;

    private String userId;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        userId = UUID.randomUUID().toString();
        userDto = new UserDto(userId, "John Doe", "john.doe@example.com", "password123", Instant.now());
    }


    @Test
    void createUser_ShouldReturnCreatedUserWith201Status() throws Exception {
        when(userService.createUser(any(UserDto.class))).thenReturn(userDto);
        mockMvc.perform(post("/api/v1/users/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(userId)))
                .andExpect(jsonPath("$.name", is("John Doe")))
                .andExpect(jsonPath("$.email", is("john.doe@example.com")))
                .andExpect(jsonPath("$.createdAt", notNullValue()));

        verify(userService, times(1)).createUser(any(UserDto.class));
    }

    @Test
    void updateUser_ShouldReturnUpdatedUserWith200Status() throws Exception {
        UserDto updatedUserDto = new UserDto(userId, "Jane Doe", "jane.doe@example.com", "newpassword456", Instant.now());
        when(userService.updateUser(eq(userId), any(UserDto.class))).thenReturn(updatedUserDto);

        mockMvc.perform(put("/api/v1/users/{id}/update", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(userId)))
                .andExpect(jsonPath("$.name", is("Jane Doe")))
                .andExpect(jsonPath("$.email", is("jane.doe@example.com")))
                .andExpect(jsonPath("$.createdAt", notNullValue()));

        verify(userService, times(1)).updateUser(eq(userId), any(UserDto.class));
    }

    @Test
    void getUser_ShouldReturnUserWith200Status() throws Exception {
        when(userService.getUserById(userId)).thenReturn(userDto);

        mockMvc.perform(get("/api/v1/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(userId)))
                .andExpect(jsonPath("$.name", is("John Doe")))
                .andExpect(jsonPath("$.email", is("john.doe@example.com")))
                .andExpect(jsonPath("$.createdAt", notNullValue()));

        verify(userService, times(1)).getUserById(userId);
    }

    @Test
    void deleteUser_ShouldReturnNoContentStatus() throws Exception {
        when(userService.deleteUser(userId)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/users/{id}/delete", userId))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteUser(userId);
    }

    @Test
    void getUser_WhenUserNotFound_ShouldReturn404() throws Exception {
        when(userService.getUserById("nonexistent")).thenThrow(new UserNotFoundException("nonexistent"));

        mockMvc.perform(get("/api/v1/users/{id}", "nonexistent"))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).getUserById("nonexistent");
    }

    @Test
    void updateUser_WhenUserNotFound_ShouldReturn404() throws Exception {
        when(userService.updateUser(eq("nonexistent"), any(UserDto.class)))
                .thenThrow(new UserNotFoundException("nonexistent"));

        mockMvc.perform(put("/api/v1/users/{id}/update", "nonexistent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).updateUser(eq("nonexistent"), any(UserDto.class));
    }

    @Test
    void deleteUser_WhenUserNotFound_ShouldReturnFalse() throws Exception {
        when(userService.deleteUser("nonexistent")).thenReturn(false);

        mockMvc.perform(delete("/api/v1/users/{id}/delete", "nonexistent"))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteUser("nonexistent");
    }
}