package by.ilyatr.afisha_rest_api.unit.controller;

import by.ilyatr.afisha_rest_api.Exception.CommentNotFoundException;
import by.ilyatr.afisha_rest_api.controllers.rest.CommentRestControllerV1;
import by.ilyatr.afisha_rest_api.dto.CommentDto;
import by.ilyatr.afisha_rest_api.services.CommentService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentRestControllerV1.class)
@AutoConfigureMockMvc
class CommentControllerUnitTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    CacheManager cacheManager;

    @MockitoBean
    private CommentService commentService;

    @Autowired
    private ObjectMapper objectMapper;

    private CommentDto commentDto;

    private String commentId;
    private String eventId;
    private String userId;

    @BeforeEach
    void setUp() {
        commentId = UUID.randomUUID().toString();
        eventId = UUID.randomUUID().toString();
        userId = UUID.randomUUID().toString();
        commentDto = new CommentDto(commentId, userId, eventId, "Great event!", Instant.now(), Instant.now());
    }

    @Test
    void createComment_ShouldReturnCreatedCommentWith201Status() throws Exception {
        when(commentService.createComment(any(CommentDto.class))).thenReturn(commentDto);

        mockMvc.perform(post("/api/v1/comments/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDto)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(commentId)))
                .andExpect(jsonPath("$.text", is("Great event!")))
                .andExpect(jsonPath("$.userId", is(userId)))
                .andExpect(jsonPath("$.eventId", is(eventId)))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()));

        verify(commentService, times(1)).createComment(any(CommentDto.class));
    }

    @Test
    void getComment_ShouldReturnCommentWith200Status() throws Exception {
        when(commentService.getComment(commentId)).thenReturn(commentDto);

        mockMvc.perform(get("/api/v1/comments/{id}", commentId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(commentId)))
                .andExpect(jsonPath("$.text", is("Great event!")))
                .andExpect(jsonPath("$.userId", is(userId)))
                .andExpect(jsonPath("$.eventId", is(eventId)))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()));

        verify(commentService, times(1)).getComment(commentId);
    }

    @Test
    void updateComment_ShouldReturnUpdatedCommentWith200Status() throws Exception {
        CommentDto updatedCommentDto = new CommentDto(commentId, userId, eventId, "Updated comment", Instant.now(), Instant.now());
        when(commentService.updateComment(eq(commentId), any(CommentDto.class))).thenReturn(updatedCommentDto);

        mockMvc.perform(put("/api/v1/comments/{id}/update", commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDto)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(commentId)))
                .andExpect(jsonPath("$.text", is("Updated comment")))
                .andExpect(jsonPath("$.userId", is(userId)))
                .andExpect(jsonPath("$.eventId", is(eventId)))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()));

        verify(commentService, times(1)).updateComment(eq(commentId), any(CommentDto.class));
    }

    @Test
    void deleteComment_ShouldReturnNoContentStatus() throws Exception {
        when(commentService.deleteComment(commentId)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/comments/{id}/delete", commentId))
                .andExpect(status().isNoContent());

        verify(commentService, times(1)).deleteComment(commentId);
    }

    @Test
    void getCommentsByEventId_ShouldReturnCommentsWith200Status() throws Exception {
        CommentDto comment1 = new CommentDto(UUID.randomUUID().toString(), userId, eventId, "First comment", Instant.now(), Instant.now());
        CommentDto comment2 = new CommentDto(UUID.randomUUID().toString(), userId, eventId, "Second comment", Instant.now(), Instant.now());
        Page<CommentDto> page = new PageImpl<>(List.of(comment1, comment2));
        when(commentService.getComments(eq(eventId), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/comments/event/{id}", eventId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].text", is("First comment")))
                .andExpect(jsonPath("$.content[1].text", is("Second comment")))
                .andExpect(jsonPath("$.page.totalElements", is(2)));

        verify(commentService, times(1)).getComments(eq(eventId), any(Pageable.class));
    }

    @Test
    void createComment_WhenCreationFails_ShouldReturn404() throws Exception {
        when(commentService.createComment(any(CommentDto.class))).thenThrow(new CommentNotFoundException("Comment not found"));

        mockMvc.perform(post("/api/v1/comments/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDto)))
                .andExpect(status().isNotFound());

        verify(commentService, times(1)).createComment(any(CommentDto.class));
    }

    @Test
    void getComment_WhenCommentNotFound_ShouldReturn404() throws Exception {
        when(commentService.getComment("nonexistent")).thenThrow(new CommentNotFoundException("Comment not found"));

        mockMvc.perform(get("/api/v1/comments/{id}", "nonexistent"))
                .andExpect(status().isNotFound());

        verify(commentService, times(1)).getComment("nonexistent");
    }

    @Test
    void updateComment_WhenCommentNotFound_ShouldReturn404() throws Exception {
        when(commentService.updateComment(eq("nonexistent"), any(CommentDto.class)))
                .thenThrow(new CommentNotFoundException("Comment not found"));

        mockMvc.perform(put("/api/v1/comments/{id}/update", "nonexistent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDto)))
                .andExpect(status().isNotFound());

        verify(commentService, times(1)).updateComment(eq("nonexistent"), any(CommentDto.class));
    }

    @Test
    void deleteComment_WhenCommentNotFound_ShouldReturn404() throws Exception {
        when(commentService.deleteComment("nonexistent")).thenReturn(false);

        mockMvc.perform(delete("/api/v1/comments/{id}/delete", "nonexistent"))
                .andExpect(status().isNoContent());

        verify(commentService, times(1)).deleteComment("nonexistent");
    }
}