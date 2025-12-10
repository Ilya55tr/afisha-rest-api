package by.ilyatr.afisha_rest_api.integration.controller;

import by.ilyatr.afisha_rest_api.configuration.TestcontainersConfiguration;
import by.ilyatr.afisha_rest_api.dto.UserDto;


import by.ilyatr.afisha_rest_api.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
@Import(TestcontainersConfiguration.class)
public class UserControllerIntegrationTests {


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper ;
    private UserDto userDto;

    @Autowired
    private UserRepository userRepository;
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        String userId = UUID.randomUUID().toString();
        userDto = new UserDto(
                userId,
                "John Doe",
                "john.doe@example.com",
                "password123",
                Instant.now()
        );
    }

    @Test
    void createUser_ShouldPersistUserAndReturn201()throws Exception{
        mockMvc.perform(post("/api/v1/users/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name")
                        .value(userDto.getName()))
                .andExpect(jsonPath("$.email")
                        .value(userDto.getEmail()))
                .andExpect(jsonPath("$.createdAt")
                        .exists());
    }
}
