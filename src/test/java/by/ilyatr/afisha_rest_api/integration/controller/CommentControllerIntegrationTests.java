package by.ilyatr.afisha_rest_api.integration.controller;

import by.ilyatr.afisha_rest_api.configuration.TestcontainersConfiguration;
import by.ilyatr.afisha_rest_api.dto.CommentDto;
import by.ilyatr.afisha_rest_api.entities.Category;
import by.ilyatr.afisha_rest_api.entities.Comment;
import by.ilyatr.afisha_rest_api.entities.Event;
import by.ilyatr.afisha_rest_api.entities.User;
import by.ilyatr.afisha_rest_api.mapper.CommentMapper;
import by.ilyatr.afisha_rest_api.repositories.CommentRepository;
import by.ilyatr.afisha_rest_api.repositories.EventRepository;
import by.ilyatr.afisha_rest_api.repositories.UserRepository;
import by.ilyatr.afisha_rest_api.services.CommentService;
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
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
@Import(TestcontainersConfiguration.class)
public class CommentControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private CommentMapper commentMapper;

    private User user;
    private Event event;
    private String userId;
    private String eventId;
    @Autowired
    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        cacheManager.getCache("comments").clear();

        userId = UUID.randomUUID().toString();
        user = User.builder()
                .id(userId)
                .name("John Doe")
                .email( "john.doe@example.com")
                .password("password123")
                .createdAt(Instant.now())
                .build();
        userRepository.save(user);

        eventId = UUID.randomUUID().toString();
        event= Event.builder()
                .id(eventId)
                .title("Concert Event")
                .date(Instant.now().plusSeconds(3600))
                .category(Category.Concert)
                .price(BigDecimal.valueOf(1000))
                .build();
        eventRepository.save(event);
    }

    @Test
    void createComment_ShouldPersistCommentAndReturn201() throws Exception {
        CommentDto commentDto = new CommentDto(
                null,
                userId,
                eventId,
                "Great concert!",
                Instant.now(),
                Instant.now()
        );

        mockMvc.perform(post("/api/v1/comments/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDto)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.text").value("Great concert!"))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.eventId").value(eventId))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        String redisKey = "event:" + eventId + ":comments";
        assertThat(redisTemplate.opsForList().range(redisKey, 0, -1)).isNotNull();

    }

    @Test
    void getCommentsByEventId_ShouldReturnCommentsUsingRedisAndCache() throws Exception {

        CommentDto commentDto = new CommentDto(
                null,
                userId,
                eventId,
                "First comment",
                Instant.now(),
                Instant.now()
        );

        Comment comment = commentMapper.toComment(commentService.createComment(commentDto));

        mockMvc.perform(get("/api/v1/comments/event/{id}?page=0&size=5", eventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(comment.getId()))
                .andExpect(jsonPath("$.content[0].text").value("First comment"))
                .andExpect(jsonPath("$.content[0].userId").value(userId))
                .andExpect(jsonPath("$.content[0].eventId").value(eventId));

        Cache cache = cacheManager.getCache("comments");
        assertThat(cache.get(comment.getId())).isNotNull();

        mockMvc.perform(get("/api/v1/comments/event/{id}?page=0&size=5", eventId))
                .andExpect(status().isOk());
    }
}