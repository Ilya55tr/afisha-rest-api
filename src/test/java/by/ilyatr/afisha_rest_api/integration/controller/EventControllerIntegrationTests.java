package by.ilyatr.afisha_rest_api.integration.controller;


import by.ilyatr.afisha_rest_api.configuration.TestcontainersConfiguration;
import by.ilyatr.afisha_rest_api.dto.EventDto;
import by.ilyatr.afisha_rest_api.entities.Comment;
import by.ilyatr.afisha_rest_api.entities.Event;
import by.ilyatr.afisha_rest_api.repositories.EventRepository;
import by.ilyatr.afisha_rest_api.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

import static by.ilyatr.afisha_rest_api.entities.Category.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
@Import(TestcontainersConfiguration.class)
public class EventControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper ;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private UserService userService;

    Event event1;
    Event event2;

    @BeforeEach
    void setUp(){
        eventRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        cacheManager.getCache("events").clear();

        String eventId1 = UUID.randomUUID().toString();
        event1 = new Event(eventId1,
                "Haski",
                Instant.now(),
                Concert,
                BigDecimal.valueOf(1000),
                new ArrayList<Comment>());
        String eventId2 = UUID.randomUUID().toString();
        event2 = new Event(eventId2,
                "Djon Garik",
                Instant.now().plusSeconds(3600),
                Concert,
                BigDecimal.valueOf(2000),
                new ArrayList<Comment>());
        eventRepository.save(event1);
        eventRepository.save(event2);

        redisTemplate.opsForZSet().incrementScore("events:popular", event1.getId(), 5);
        redisTemplate.opsForZSet().incrementScore("events:popular", event2.getId(), 10);
    }

    @Test
    void shouldReturnPopularEventsUsingRedisAndCache() throws Exception {

        mockMvc.perform(get("/api/v1/events/popular")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(event2.getId()))
                .andExpect(jsonPath("$[1].id").value(event1.getId()));

        mockMvc.perform(get("/api/v1/events/popular"))
                .andExpect(status().isOk());

        Cache cache = cacheManager.getCache("events");
        assertThat(cache.get(event1.getId())).isNotNull();
        assertThat(cache.get(event2.getId())).isNotNull();

        mockMvc.perform(get("/api/v1/events/popular"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnLastEventsUsingRedisAndCache() throws Exception {

        mockMvc.perform(get("/api/v1/events/last?page=0&size=2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(event2.getId()))
                .andExpect(jsonPath("$.content[1].id").value(event1.getId()));

        mockMvc.perform(get("/api/v1/events/last?page=0&size=2"))
                .andExpect(status().isOk());

        Cache cache = cacheManager.getCache("events");
        assertThat(cache.get(event1.getId())).isNotNull();
        assertThat(cache.get(event2.getId())).isNotNull();



    }

    @Test
    void createEvent_ShouldPersistEventAndReturn201() throws Exception {
        EventDto eventDto = new EventDto(
                null,
                "New Event",
                Instant.now().plusSeconds(3600),
                Concert,
                BigDecimal.valueOf(1500)
        );

        mockMvc.perform(post("/api/v1/events/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventDto)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("New Event"))
                .andExpect(jsonPath("$.price").value(1500));
    }

}
