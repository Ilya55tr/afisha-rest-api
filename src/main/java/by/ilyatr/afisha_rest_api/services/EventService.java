package by.ilyatr.afisha_rest_api.services;

import by.ilyatr.afisha_rest_api.dto.EventDto;
import by.ilyatr.afisha_rest_api.entities.Event;
import by.ilyatr.afisha_rest_api.mapper.EventMapper;
import by.ilyatr.afisha_rest_api.repositories.EventRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EventService {
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ApplicationContext applicationContext;
    private EventService self;

    @PostConstruct
    public void init() {
        self = applicationContext.getBean(EventService.class);
        preheatPopularEventsCache();
    }
    
    private void preheatPopularEventsCache() {
        log.info("Preheating popular events cache");
        List<Event> popularEvents = eventRepository.findTop10ByOrderByDateDesc();
        
        for (Event event : popularEvents) {
            redisTemplate.opsForZSet().add(POPULAR_EVENTS_KEY, event.getId(), 1);
        }
        
        log.info("Popular events cache preheated with {} events", popularEvents.size());
    }

    private static final String POPULAR_EVENTS_KEY = "events:popular";
    private static final String LAST_EVENTS_KEY = "events:last";
    private static final String EVENTS_KEY = "events";

    @Transactional
    public EventDto createEvent(EventDto eventDto) {
        String EventId = UUID.randomUUID().toString();
        eventDto.setId(EventId);
        Event event = eventMapper.toEvent(eventDto);
        log.info("create cache for event with id {}", EventId);
        redisTemplate.opsForList().leftPush(LAST_EVENTS_KEY, EventId);

        return eventMapper
                .toEventDto(eventRepository.save(event));
    }


    @Cacheable(cacheNames = EVENTS_KEY, key = "#id", unless = "#result == null")
    public EventDto getEventByIdCached(String id) {
        log.info("Getting event with id {} from MySQL db", id);
            return eventRepository
                    .findById(id)
                    .map(eventMapper::toEventDto)
                    .orElseThrow();
    }

    public EventDto getEventById(String id) {
        log.info("increment event popularity with id {}", id);
        redisTemplate.opsForZSet().incrementScore(POPULAR_EVENTS_KEY, id, 1);
        return self.getEventByIdCached(id);
    }

    @CacheEvict(cacheNames = EVENTS_KEY, key = "#id")
    @Transactional
    public EventDto updateEvent(String id, EventDto eventDto) {
        return eventRepository.findById(id).map(event -> {
            eventMapper.updateEvent(eventDto, event);
            return eventMapper.toEventDto(eventRepository.save(event));
        }).orElseThrow();
    }

    @CacheEvict(cacheNames = EVENTS_KEY, key = "#id")
    @Transactional
    public boolean deleteEvent(String id) {
        if (eventRepository.existsById(id)) {
            eventRepository.deleteById(id);
            redisTemplate.opsForZSet().remove(POPULAR_EVENTS_KEY, id);
            redisTemplate.opsForList().remove(LAST_EVENTS_KEY, 1, id);
            log.info("Event with id {} deleted", id);
            return true;
        } else
            return false;
    }

    public List<EventDto> getPopularEvents() {
        var ids = redisTemplate.opsForZSet().reverseRange(POPULAR_EVENTS_KEY, 0, 9);
        return ids.stream()
                .map(id -> self.getEventByIdCached(id.toString()))
                .toList();
    }

    public Page<EventDto> getLastEvents(Pageable pageable) {
        if (pageable.getPageNumber() == 0) {
            Page<EventDto> cached = getFirstPageFromCache(pageable);
            if (cached != null) {
                return cached;
            }
        }
        return getLastEventsFromDb(pageable);
    }

    private Page<EventDto> getFirstPageFromCache(Pageable pageable) {
        var ids = redisTemplate.opsForList()
                .range(LAST_EVENTS_KEY, 0, pageable.getPageSize() - 1);

        if (ids == null || ids.isEmpty() || ids.size() != pageable.getPageSize()){
            return null;
        }
        redisTemplate.opsForList().trim(LAST_EVENTS_KEY, 0,pageable.getPageSize() - 1);
        log.info("trim last events list");
        List<EventDto> events = ids.stream()
                .map(id -> self.getEventByIdCached(id.toString()))
                .toList();
        long total = eventRepository.count();
        return new PageImpl<>(events, pageable, total);

    }
    private Page<EventDto> getLastEventsFromDb(Pageable pageable) {

        log.info("loading last events from db");
        Page<Event> page = eventRepository.findAllByOrderByDateDesc(pageable);

        List<EventDto> events = page.getContent().stream()
                    .map(eventMapper::toEventDto).toList();


        for (int i = 0; i < pageable.getPageSize(); i++) {
            if (i >= events.size()) break;
            redisTemplate.opsForList().leftPush(LAST_EVENTS_KEY, events.get(i).getId());
        }

        return  new PageImpl<>(events, page.getPageable(), page.getTotalElements());
    }


}
