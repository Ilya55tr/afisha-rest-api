package by.ilyatr.afisha_rest_api.unit.service;

import by.ilyatr.afisha_rest_api.Exception.CommentNotFoundException;
import by.ilyatr.afisha_rest_api.dto.CommentDto;
import by.ilyatr.afisha_rest_api.entities.Category;
import by.ilyatr.afisha_rest_api.entities.Comment;
import by.ilyatr.afisha_rest_api.entities.Event;
import by.ilyatr.afisha_rest_api.entities.User;
import by.ilyatr.afisha_rest_api.mapper.CommentMapper;
import by.ilyatr.afisha_rest_api.mapper.HelperMapper;
import by.ilyatr.afisha_rest_api.repositories.CommentRepository;
import by.ilyatr.afisha_rest_api.services.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService Unit Tests")
class CommentServiceUnitTests {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private HelperMapper helperMapper;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @Mock
    private ListOperations<String, Object> listOperations;

    @InjectMocks
    private CommentService commentService;

    private Comment testComment;
    private CommentDto testCommentDto;
    private User testUser;
    private Event testEvent;

    private static final String COMMENT_ID = "test-comment-id";
    private static final String USER_ID = "test-user-id";
    private static final String EVENT_ID = "test-event-id";
    private static final String COMMENTS_CACHE = "comments";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(USER_ID)
                .name("testuser")
                .email("test@example.com")
                .password("password")
                .build();

        testEvent = Event.builder()
                .id(EVENT_ID)
                .title("Test Event")
                .date(Instant.now())
                .category(Category.Concert)
                .price(BigDecimal.valueOf(100.00))
                .comments(new ArrayList<>())
                .build();

        testComment = Comment.builder()
                .id(COMMENT_ID)
                .user(testUser)
                .event(testEvent)
                .text("Test comment text")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testCommentDto = new CommentDto(
                COMMENT_ID,
                USER_ID,
                EVENT_ID,
                "Test comment text",
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("Should create comment successfully")
    void testCreateComment() {
        // Given
        CommentDto inputDto = new CommentDto(null, USER_ID, EVENT_ID,
                "New comment", null, null);
        Comment savedComment = Comment.builder()
                .id("generated-id")
                .user(testUser)
                .event(testEvent)
                .text("New comment")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(commentMapper.toComment(any(CommentDto.class))).thenReturn(savedComment);
        doNothing().when(helperMapper).setUserAndEvent(any(CommentDto.class), any(Comment.class));
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);
        when(commentMapper.toCommentDto(any(Comment.class))).thenReturn(testCommentDto);

        // When
        CommentDto result = commentService.createComment(inputDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(inputDto.getCreatedAt()).isNotNull();
        assertThat(inputDto.getUpdatedAt()).isNotNull();

        verify(commentMapper).toComment(any(CommentDto.class));
        verify(helperMapper).setUserAndEvent(any(CommentDto.class), any(Comment.class));
        verify(commentRepository).save(any(Comment.class));
        verify(listOperations).leftPush(eq("event:" + EVENT_ID + ":comments"), anyString());
    }

    @Test
    @DisplayName("Should get comment from database when not in cache")
    void testGetComment_FromDatabase() {
        // Given
        when(cacheManager.getCache(COMMENTS_CACHE)).thenReturn(cache);
        when(cache.get(COMMENT_ID, CommentDto.class)).thenReturn(null);
        when(commentRepository.findByIdWithUserAndEvent(COMMENT_ID))
                .thenReturn(Optional.of(testComment));
        when(commentMapper.toCommentDto(testComment)).thenReturn(testCommentDto);

        // When
        CommentDto result = commentService.getComment(COMMENT_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(COMMENT_ID);
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getEventId()).isEqualTo(EVENT_ID);

        verify(commentRepository).findByIdWithUserAndEvent(COMMENT_ID);
        verify(cache).put(COMMENT_ID, testCommentDto);
    }

    @Test
    @DisplayName("Should get comment from cache when available")
    void testGetComment_FromCache() {
        // Given
        when(cacheManager.getCache(COMMENTS_CACHE)).thenReturn(cache);
        when(cache.get(COMMENT_ID, CommentDto.class)).thenReturn(testCommentDto);

        // When
        CommentDto result = commentService.getComment(COMMENT_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(COMMENT_ID);
        verify(commentRepository, never()).findByIdWithUserAndEvent(anyString());
        verify(cache, never()).put(anyString(), any());
    }

    @Test
    @DisplayName("Should throw CommentNotFoundException when comment not found")
    void testGetComment_NotFound() {
        // Given
        when(cacheManager.getCache(COMMENTS_CACHE)).thenReturn(cache);
        when(cache.get(COMMENT_ID, CommentDto.class)).thenReturn(null);
        when(commentRepository.findByIdWithUserAndEvent(COMMENT_ID))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentService.getComment(COMMENT_ID))
                .isInstanceOf(CommentNotFoundException.class);
        verify(commentRepository).findByIdWithUserAndEvent(COMMENT_ID);
    }

    @Test
    @DisplayName("Should delete comment successfully")
    void testDeleteComment_Success() {
        // Given
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(commentRepository.existsById(COMMENT_ID)).thenReturn(true);
        when(commentRepository.findByIdWithUserAndEvent(COMMENT_ID))
                .thenReturn(Optional.of(testComment));
        when(commentMapper.toCommentDto(testComment)).thenReturn(testCommentDto);

        // When
        boolean result = commentService.deleteComment(COMMENT_ID);

        // Then
        assertThat(result).isTrue();
        verify(commentRepository).existsById(COMMENT_ID);
        verify(commentRepository).deleteById(COMMENT_ID);
        verify(listOperations)
                .remove(eq("event:" + EVENT_ID + ":comments"),
                eq(1L), eq(testCommentDto));
    }

    @Test
    @DisplayName("Should return false when deleting non-existent comment")
    void testDeleteComment_NotFound() {
        // Given
        when(commentRepository.existsById(COMMENT_ID)).thenReturn(false);

        // When
        boolean result = commentService.deleteComment(COMMENT_ID);

        // Then
        assertThat(result).isFalse();
        verify(commentRepository).existsById(COMMENT_ID);
        verify(commentRepository, never()).deleteById(anyString());
    }

    @Test
    @DisplayName("Should throw exception when deleting comment that exists but can't be found")
    void testDeleteComment_ExistsButNotFound() {
        // Given
        when(commentRepository.existsById(COMMENT_ID)).thenReturn(true);
        when(commentRepository.findByIdWithUserAndEvent(COMMENT_ID))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentService.deleteComment(COMMENT_ID))
                .isInstanceOf(CommentNotFoundException.class);
        verify(commentRepository, never()).deleteById(anyString());
    }

    @Test
    @DisplayName("Should update comment successfully")
    void testUpdateComment_Success() {
        // Given
        CommentDto updateDto = new CommentDto(COMMENT_ID, USER_ID, EVENT_ID,
                "Updated comment text", testCommentDto.getCreatedAt(), null);
        Comment updatedComment = Comment.builder()
                .id(COMMENT_ID)
                .user(testUser)
                .event(testEvent)
                .text("Updated comment text")
                .createdAt(testCommentDto.getCreatedAt())
                .updatedAt(Instant.now())
                .build();

        when(commentRepository.findByIdWithUserAndEvent(COMMENT_ID))
                .thenReturn(Optional.of(testComment));
        when(commentMapper.updateComment(any(CommentDto.class), any(Comment.class)))
                .thenReturn(updatedComment);
        when(commentRepository.save(any(Comment.class))).thenReturn(updatedComment);
        when(commentMapper.toCommentDto(updatedComment)).thenReturn(updateDto);

        // When
        CommentDto result = commentService.updateComment(COMMENT_ID, updateDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(updateDto.getUpdatedAt()).isNotNull();

        verify(commentRepository).findByIdWithUserAndEvent(COMMENT_ID);
        verify(commentMapper).updateComment(any(CommentDto.class), eq(testComment));
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent comment")
    void testUpdateComment_NotFound() {
        // Given
        CommentDto updateDto = new CommentDto(COMMENT_ID, USER_ID, EVENT_ID,
                "Updated comment text", testCommentDto.getCreatedAt(), null);

        when(commentRepository.findByIdWithUserAndEvent(COMMENT_ID))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentService.updateComment(COMMENT_ID, updateDto))
                .isInstanceOf(CommentNotFoundException.class);
        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get first page of comments from cache")
    void testGetComments_FirstPageFromCache() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Object> cachedIds = Arrays.asList("comment1", "comment2", "comment3");

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range("event:" + EVENT_ID + ":comments", 0, 9))
                .thenReturn(cachedIds);
        when(cacheManager.getCache(COMMENTS_CACHE)).thenReturn(cache);
        when(commentRepository.count()).thenReturn(50L);

        CommentDto comment1 = new CommentDto("comment1", USER_ID, EVENT_ID,
                "Text 1", Instant.now(), Instant.now());
        CommentDto comment2 = new CommentDto("comment2", USER_ID, EVENT_ID,
                "Text 2", Instant.now(), Instant.now());
        CommentDto comment3 = new CommentDto("comment3", USER_ID, EVENT_ID,
                "Text 3", Instant.now(), Instant.now());

        when(cache.get("comment1", CommentDto.class)).thenReturn(comment1);
        when(cache.get("comment2", CommentDto.class)).thenReturn(comment2);
        when(cache.get("comment3", CommentDto.class)).thenReturn(comment3);

        // When
        Page<CommentDto> result = commentService.getComments(EVENT_ID, pageable);

        // Then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(50L);

        verify(listOperations).range("event:" + EVENT_ID + ":comments", 0, 9);
        verify(listOperations).trim("event:" + EVENT_ID + ":comments", 0, 9);
        verify(commentRepository, never()).findTop100ByEventIdOrderByUpdatedAtDesc(anyString(), any());
    }

    @Test
    @DisplayName("Should get comments from database when cache is empty")
    void testGetComments_FromDatabase() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range("event:" + EVENT_ID + ":comments", 0, 9))
                .thenReturn(null);

        List<Comment> comments = Arrays.asList(testComment);
        Page<Comment> commentPage = new PageImpl<>(comments, pageable, 1);

        when(commentRepository.findTop100ByEventIdOrderByUpdatedAtDesc(EVENT_ID, pageable))
                .thenReturn(commentPage);
        when(commentMapper.toCommentDto(testComment)).thenReturn(testCommentDto);

        // When
        Page<CommentDto> result = commentService.getComments(EVENT_ID, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getContent().get(0).getId()).isEqualTo(COMMENT_ID);

        verify(commentRepository).findTop100ByEventIdOrderByUpdatedAtDesc(EVENT_ID, pageable);
        verify(listOperations).leftPush("event:" + EVENT_ID + ":comments", COMMENT_ID);
    }

    @Test
    @DisplayName("Should get second page from database bypassing cache")
    void testGetComments_SecondPage() {
        // Given
        Pageable pageable = PageRequest.of(1, 10);

        when(redisTemplate.opsForList()).thenReturn(listOperations);

        List<Comment> comments = Arrays.asList(testComment);
        Page<Comment> commentPage = new PageImpl<>(comments, pageable, 20);

        when(commentRepository.findTop100ByEventIdOrderByUpdatedAtDesc(EVENT_ID, pageable))
                .thenReturn(commentPage);
        when(commentMapper.toCommentDto(testComment)).thenReturn(testCommentDto);

        // When
        Page<CommentDto> result = commentService.getComments(EVENT_ID, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(20L);

        verify(commentRepository).findTop100ByEventIdOrderByUpdatedAtDesc(EVENT_ID, pageable);
        verify(listOperations, never()).range(anyString(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("Should handle empty cache list")
    void testGetComments_EmptyCacheList() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range("event:" + EVENT_ID + ":comments", 0, 9))
                .thenReturn(Collections.emptyList());

        List<Comment> comments = Arrays.asList(testComment);
        Page<Comment> commentPage = new PageImpl<>(comments, pageable, 1);

        when(commentRepository.findTop100ByEventIdOrderByUpdatedAtDesc(EVENT_ID, pageable))
                .thenReturn(commentPage);
        when(commentMapper.toCommentDto(testComment)).thenReturn(testCommentDto);

        // When
        Page<CommentDto> result = commentService.getComments(EVENT_ID, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(commentRepository).findTop100ByEventIdOrderByUpdatedAtDesc(EVENT_ID, pageable);
    }

    @Test
    @DisplayName("Should populate cache when loading from database")
    void testGetComments_PopulatesCache() {
        // Given
        Pageable pageable = PageRequest.of(0, 3);

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range(anyString(), anyLong(), anyLong())).thenReturn(null);

        Comment comment1 = Comment.builder().id("c1").user(testUser).event(testEvent)
                .text("Text 1").createdAt(Instant.now()).updatedAt(Instant.now()).build();
        Comment comment2 = Comment.builder().id("c2").user(testUser).event(testEvent)
                .text("Text 2").createdAt(Instant.now()).updatedAt(Instant.now()).build();
        Comment comment3 = Comment.builder().id("c3").user(testUser).event(testEvent)
                .text("Text 3").createdAt(Instant.now()).updatedAt(Instant.now()).build();

        List<Comment> comments = Arrays.asList(comment1, comment2, comment3);
        Page<Comment> commentPage = new PageImpl<>(comments, pageable, 3);

        when(commentRepository.findTop100ByEventIdOrderByUpdatedAtDesc(EVENT_ID, pageable))
                .thenReturn(commentPage);
        when(commentMapper.toCommentDto(comment1))
                .thenReturn(new CommentDto("c1", USER_ID, EVENT_ID, "Text 1", Instant.now(), Instant.now()));
        when(commentMapper.toCommentDto(comment2))
                .thenReturn(new CommentDto("c2", USER_ID, EVENT_ID, "Text 2", Instant.now(), Instant.now()));
        when(commentMapper.toCommentDto(comment3))
                .thenReturn(new CommentDto("c3", USER_ID, EVENT_ID, "Text 3", Instant.now(), Instant.now()));

        // When
        Page<CommentDto> result = commentService.getComments(EVENT_ID, pageable);

        // Then
        assertThat(result.getContent()).hasSize(3);
        verify(listOperations, times(3)).leftPush(eq("event:" + EVENT_ID + ":comments"), anyString());
    }

    @Test
    @DisplayName("Should handle database returning fewer comments than page size")
    void testGetComments_FewerCommentsThanPageSize() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range(anyString(), anyLong(), anyLong())).thenReturn(null);

        List<Comment> comments = Arrays.asList(testComment);
        Page<Comment> commentPage = new PageImpl<>(comments, pageable, 1);

        when(commentRepository.findTop100ByEventIdOrderByUpdatedAtDesc(EVENT_ID, pageable))
                .thenReturn(commentPage);
        when(commentMapper.toCommentDto(testComment)).thenReturn(testCommentDto);

        // When
        Page<CommentDto> result = commentService.getComments(EVENT_ID, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(listOperations, times(1)).leftPush(anyString(), eq(COMMENT_ID));
    }

    @Test
    @DisplayName("Should set timestamps when creating comment")
    void testCreateComment_SetsTimestamps() {
        // Given
        CommentDto inputDto = new CommentDto(null, USER_ID, EVENT_ID,
                "New comment", null, null);
        Comment savedComment = Comment.builder()
                .id("new-id")
                .user(testUser)
                .event(testEvent)
                .text("New comment")
                .build();

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(commentMapper.toComment(any(CommentDto.class))).thenReturn(savedComment);
        doNothing().when(helperMapper).setUserAndEvent(any(CommentDto.class), any(Comment.class));
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);
        when(commentMapper.toCommentDto(any(Comment.class))).thenReturn(testCommentDto);

        // When
        commentService.createComment(inputDto);

        // Then
        assertThat(inputDto.getCreatedAt()).isNotNull();
        assertThat(inputDto.getUpdatedAt()).isNotNull();
        assertThat(inputDto.getCreatedAt()).isEqualTo(inputDto.getUpdatedAt());
    }

    @Test
    @DisplayName("Should update timestamp when updating comment")
    void testUpdateComment_UpdatesTimestamp() {
        // Given
        Instant originalCreatedAt = Instant.now().minusSeconds(3600);
        CommentDto updateDto = new CommentDto(COMMENT_ID, USER_ID, EVENT_ID,
                "Updated text", originalCreatedAt, null);

        when(commentRepository.findByIdWithUserAndEvent(COMMENT_ID))
                .thenReturn(Optional.of(testComment));
        when(commentMapper.updateComment(any(CommentDto.class), any(Comment.class)))
                .thenReturn(testComment);
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);
        when(commentMapper.toCommentDto(testComment)).thenReturn(updateDto);

        // When
        commentService.updateComment(COMMENT_ID, updateDto);

        // Then
        assertThat(updateDto.getUpdatedAt()).isNotNull();
        assertThat(updateDto.getCreatedAt()).isEqualTo(originalCreatedAt);
    }
}