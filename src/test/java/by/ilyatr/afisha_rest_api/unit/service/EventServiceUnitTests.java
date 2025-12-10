package by.ilyatr.afisha_rest_api.unit.service;

import by.ilyatr.afisha_rest_api.Exception.EventNotFoundException;
import by.ilyatr.afisha_rest_api.dto.EventDto;
import by.ilyatr.afisha_rest_api.entities.Category;
import by.ilyatr.afisha_rest_api.entities.Event;
import by.ilyatr.afisha_rest_api.mapper.EventMapper;
import by.ilyatr.afisha_rest_api.repositories.EventRepository;
import by.ilyatr.afisha_rest_api.services.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventService Unit Tests")
class EventServiceUnitTests {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @Mock
    private ListOperations<String, Object> listOperations;

    @InjectMocks
    private EventService eventService;

    private Event testEvent;
    private EventDto testEventDto;
    private static final String EVENT_ID = "test-event-id";
    private static final String POPULAR_EVENTS_KEY = "events:popular";
    private static final String LAST_EVENTS_KEY = "events:last";
    private static final String EVENTS_KEY = "events";

    @BeforeEach
    void setUp() {
        testEvent = Event.builder()
                .id(EVENT_ID)
                .title("Test Event")
                .date(Instant.now())
                .category(Category.Concert)
                .price(BigDecimal.valueOf(100.00))
                .comments(new ArrayList<>())
                .build();

        testEventDto = new EventDto(
                EVENT_ID,
                "Test Event",
                Instant.now(),
                Category.Concert,
                BigDecimal.valueOf(100.00)
        );
    }

    @Test
    @DisplayName("Should create event successfully")
    void testCreateEvent() {
        // Given
        EventDto inputDto = new EventDto(null, "New Event", Instant.now(),
                Category.Concert, BigDecimal.valueOf(50.00));
        Event savedEvent = Event.builder()
                .id("generated-id")
                .title("New Event")
                .date(inputDto.getDate())
                .category(Category.Concert)
                .price(BigDecimal.valueOf(50.00))
                .build();

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(eventMapper.toEvent(any(EventDto.class))).thenReturn(savedEvent);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);
        when(eventMapper.toEventDto(any(Event.class))).thenReturn(testEventDto);

        // When
        EventDto result = eventService.createEvent(inputDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        verify(eventRepository).save(any(Event.class));
        verify(listOperations).leftPush(eq(LAST_EVENTS_KEY), anyString());
    }

    @Test
    @DisplayName("Should get event from database when not in cache")
    void testGetEvent_FromDatabase() {
        // Given
        when(cacheManager.getCache(EVENTS_KEY)).thenReturn(cache);
        when(cache.get(EVENT_ID, EventDto.class)).thenReturn(null);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(testEvent));
        when(eventMapper.toEventDto(testEvent)).thenReturn(testEventDto);

        // When
        EventDto result = eventService.getEvent(EVENT_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(EVENT_ID);
        verify(eventRepository).findById(EVENT_ID);
        verify(cache).put(EVENT_ID, testEventDto);
    }

    @Test
    @DisplayName("Should get event from cache when available")
    void testGetEvent_FromCache() {
        // Given
        when(cacheManager.getCache(EVENTS_KEY)).thenReturn(cache);
        when(cache.get(EVENT_ID, EventDto.class)).thenReturn(testEventDto);

        // When
        EventDto result = eventService.getEvent(EVENT_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(EVENT_ID);
        verify(eventRepository, never()).findById(anyString());
        verify(cache, never()).put(anyString(), any());
    }

    @Test
    @DisplayName("Should throw EventNotFoundException when event not found")
    void testGetEvent_NotFound() {
        // Given
        when(cacheManager.getCache(EVENTS_KEY)).thenReturn(cache);
        when(cache.get(EVENT_ID, EventDto.class)).thenReturn(null);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> eventService.getEvent(EVENT_ID))
                .isInstanceOf(EventNotFoundException.class);
        verify(eventRepository).findById(EVENT_ID);
    }

    @Test
    @DisplayName("Should get event by id and increment popularity")
    void testGetEventById() {
        // Given
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(cacheManager.getCache(EVENTS_KEY)).thenReturn(cache);
        when(cache.get(EVENT_ID, EventDto.class)).thenReturn(testEventDto);

        // When
        EventDto result = eventService.getEventById(EVENT_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(EVENT_ID);
        verify(zSetOperations).incrementScore(POPULAR_EVENTS_KEY, EVENT_ID, 1);
    }

    @Test
    @DisplayName("Should update event successfully")
    void testUpdateEvent() {
        // Given
        EventDto updateDto = new EventDto(EVENT_ID, "Updated Event",
                Instant.now(), Category.Theater, BigDecimal.valueOf(150.00));

        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(testEvent));
        when(eventMapper.updateEvent(updateDto, testEvent)).thenReturn(testEvent);
        when(eventRepository.save(testEvent)).thenReturn(testEvent);
        when(eventMapper.toEventDto(testEvent)).thenReturn(updateDto);

        // When
        EventDto result = eventService.updateEvent(EVENT_ID, updateDto);

        // Then
        assertThat(result).isNotNull();
        verify(eventRepository).findById(EVENT_ID);
        verify(eventMapper).updateEvent(updateDto, testEvent);
        verify(eventRepository).save(testEvent);
    }

    @Test
    @DisplayName("Should throw EventNotFoundException when updating non-existent event")
    void testUpdateEvent_NotFound() {
        // Given
        EventDto updateDto = new EventDto(EVENT_ID, "Updated Event",
                Instant.now(), Category.Theater, BigDecimal.valueOf(150.00));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> eventService.updateEvent(EVENT_ID, updateDto))
                .isInstanceOf(EventNotFoundException.class);
        verify(eventRepository).findById(EVENT_ID);
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete event successfully")
    void testDeleteEvent_Success() {
        // Given
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(eventRepository.existsById(EVENT_ID)).thenReturn(true);

        // When
        boolean result = eventService.deleteEvent(EVENT_ID);

        // Then
        assertThat(result).isTrue();
        verify(eventRepository).existsById(EVENT_ID);
        verify(eventRepository).deleteById(EVENT_ID);
        verify(zSetOperations).remove(POPULAR_EVENTS_KEY, EVENT_ID);
        verify(listOperations).remove(LAST_EVENTS_KEY, 1, EVENT_ID);
    }

    @Test
    @DisplayName("Should return false when deleting non-existent event")
    void testDeleteEvent_NotFound() {
        // Given
        when(eventRepository.existsById(EVENT_ID)).thenReturn(false);

        // When
        boolean result = eventService.deleteEvent(EVENT_ID);

        // Then
        assertThat(result).isFalse();
        verify(eventRepository).existsById(EVENT_ID);
        verify(eventRepository, never()).deleteById(anyString());
    }

    @Test
    @DisplayName("Should get popular events from Redis sorted set")
    void testGetPopularEvents() {
        // Given
        Set<Object> popularIds = new LinkedHashSet<>(Arrays.asList("id1", "id2", "id3"));
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange(POPULAR_EVENTS_KEY, 0, 9)).thenReturn(popularIds);
        when(cacheManager.getCache(EVENTS_KEY)).thenReturn(cache);

        EventDto event1 = new EventDto("id1", "Event 1", Instant.now(), Category.Concert, BigDecimal.TEN);
        EventDto event2 = new EventDto("id2", "Event 2", Instant.now(), Category.Theater, BigDecimal.TEN);
        EventDto event3 = new EventDto("id3", "Event 3", Instant.now(), Category.Show, BigDecimal.TEN);

        when(cache.get("id1", EventDto.class)).thenReturn(event1);
        when(cache.get("id2", EventDto.class)).thenReturn(event2);
        when(cache.get("id3", EventDto.class)).thenReturn(event3);

        // When
        List<EventDto> result = eventService.getPopularEvents();

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly(event1, event2, event3);
        verify(zSetOperations).reverseRange(POPULAR_EVENTS_KEY, 0, 9);
    }

    @Test
    @DisplayName("Should get first page of last events from cache")
    void testGetLastEvents_FirstPageFromCache() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Object> cachedIds = Arrays.asList("id1", "id2", "id3");

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range(LAST_EVENTS_KEY, 0, 9)).thenReturn(cachedIds);
        when(cacheManager.getCache(EVENTS_KEY)).thenReturn(cache);
        when(eventRepository.count()).thenReturn(100L);

        EventDto event1 = new EventDto("id1", "Event 1", Instant.now(), Category.Concert, BigDecimal.TEN);
        EventDto event2 = new EventDto("id2", "Event 2", Instant.now(), Category.Theater, BigDecimal.TEN);
        EventDto event3 = new EventDto("id3", "Event 3", Instant.now(), Category.Show, BigDecimal.TEN);

        when(cache.get("id1", EventDto.class)).thenReturn(event1);
        when(cache.get("id2", EventDto.class)).thenReturn(event2);
        when(cache.get("id3", EventDto.class)).thenReturn(event3);

        // When
        Page<EventDto> result = eventService.getLastEvents(pageable);

        // Then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(100L);
        verify(listOperations).range(LAST_EVENTS_KEY, 0, 9);
        verify(listOperations).trim(LAST_EVENTS_KEY, 0, 9);
        verify(eventRepository, never()).findAllByOrderByDateDesc(any());
    }

    @Test
    @DisplayName("Should get last events from database when cache is empty")
    void testGetLastEvents_FromDatabase() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range(LAST_EVENTS_KEY, 0, 9)).thenReturn(null);

        List<Event> events = Arrays.asList(testEvent);
        Page<Event> eventPage = new PageImpl<>(events, pageable, 1);

        when(eventRepository.findAllByOrderByDateDesc(pageable)).thenReturn(eventPage);
        when(eventMapper.toEventDto(testEvent)).thenReturn(testEventDto);

        // When
        Page<EventDto> result = eventService.getLastEvents(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1L);
        verify(eventRepository).findAllByOrderByDateDesc(pageable);
        verify(listOperations).leftPush(LAST_EVENTS_KEY, EVENT_ID);
    }

    @Test
    @DisplayName("Should get second page from database")
    void testGetLastEvents_SecondPage() {
        // Given
        Pageable pageable = PageRequest.of(1, 10);
        List<Event> events = Arrays.asList(testEvent);
        Page<Event> eventPage = new PageImpl<>(events, pageable, 20);

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(eventRepository.findAllByOrderByDateDesc(pageable)).thenReturn(eventPage);
        when(eventMapper.toEventDto(testEvent)).thenReturn(testEventDto);

        // When
        Page<EventDto> result = eventService.getLastEvents(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(20L);
        verify(eventRepository).findAllByOrderByDateDesc(pageable);
        verify(listOperations, never()).range(anyString(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("Should handle empty popular events list")
    void testGetPopularEvents_Empty() {
        // Given
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange(POPULAR_EVENTS_KEY, 0, 9)).thenReturn(Collections.emptySet());

        // When
        List<EventDto> result = eventService.getPopularEvents();

        // Then
        assertThat(result).isEmpty();
        verify(zSetOperations).reverseRange(POPULAR_EVENTS_KEY, 0, 9);
    }

    @Test
    @DisplayName("Should handle cache miss and database miss gracefully")
    void testGetEvent_CacheMissAndDatabaseMiss() {
        // Given
        String nonExistentId = "non-existent-id";
        when(cacheManager.getCache(EVENTS_KEY)).thenReturn(cache);
        when(cache.get(nonExistentId, EventDto.class)).thenReturn(null);
        when(eventRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> eventService.getEvent(nonExistentId))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessageContaining(nonExistentId);
    }
}