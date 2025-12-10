package by.ilyatr.afisha_rest_api.unit.controller;

import by.ilyatr.afisha_rest_api.Exception.EventNotFoundException;
import by.ilyatr.afisha_rest_api.controllers.rest.EventRestControllerV1;
import by.ilyatr.afisha_rest_api.dto.EventDto;
import by.ilyatr.afisha_rest_api.entities.Category;
import by.ilyatr.afisha_rest_api.services.EventService;

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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventRestControllerV1.class)
@AutoConfigureMockMvc
public class EventControllerUnitTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    CacheManager cacheManager;

    @MockitoBean
    private EventService eventService;

    @Autowired
    private ObjectMapper objectMapper ;



    @Test
    void getEvent_ShouldReturnEventWithStatus200() throws Exception {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        EventDto mockEvent = new EventDto(eventId, "Test Concert", Instant.now(), Category.Concert, BigDecimal.valueOf(50.0));
        when(eventService.getEventById(anyString())).thenReturn(mockEvent);

        // Act & Assert
        mockMvc.perform(get("/api/v1/events/{id}", eventId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(eventId))
                .andExpect(jsonPath("$.title").value("Test Concert"))
                .andExpect(jsonPath("$.category").value("Concert"))
                .andExpect(jsonPath("$.price").value(50.0));
    }

    @Test
    void getEvent_ShouldReturnEventWithStatus404() throws Exception {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        when(eventService.getEventById(anyString())).thenThrow(new EventNotFoundException("Event not found"+eventId));

        // Act & Assert
        mockMvc.perform(get("/api/v1/events/{id}", eventId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void createEvent_ShouldReturnEventWithStatus201() throws Exception {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        EventDto inputEvent = new EventDto(null, "New Festival", Instant.now(), Category.Festival, BigDecimal.valueOf(100.0));
        EventDto savedEvent = new EventDto(eventId, "New Festival", Instant.now(), Category.Festival, BigDecimal.valueOf(100.0));
        when(eventService.createEvent(any(EventDto.class))).thenReturn(savedEvent);

        // Act & Assert
        mockMvc.perform(post("/api/v1/events/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputEvent)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(eventId))
                .andExpect(jsonPath("$.title").value("New Festival"))
                .andExpect(jsonPath("$.category").value("Festival"));
    }

    @Test
    void updateEvent_ShouldReturnEventWithStatus200() throws Exception {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        EventDto updateEvent = new EventDto(null, "Updated Theater Show", Instant.now(), Category.Theater, BigDecimal.valueOf(75.0));
        EventDto updatedEvent = new EventDto(eventId, "Updated Theater Show", Instant.now(), Category.Theater, BigDecimal.valueOf(75.0));
        when(eventService.updateEvent(anyString(), any(EventDto.class))).thenReturn(updatedEvent);

        // Act & Assert
        mockMvc.perform(put("/api/v1/events/{id}/update", eventId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateEvent)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(eventId))
                .andExpect(jsonPath("$.title").value("Updated Theater Show"))
                .andExpect(jsonPath("$.category").value("Theater"));
    }

    @Test
    void deleteEvent_ShouldReturnEventWithStatusNo_Content() throws Exception {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        when(eventService.deleteEvent(anyString())).thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/events/{id}/delete", eventId))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateEvent_ShouldReturnEventWithStatus404() throws Exception {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        EventDto updateEvent = new EventDto(eventId, "Nonexistent Show", Instant.now(), Category.Comedy, BigDecimal.valueOf(30.0));
        when(eventService.updateEvent(anyString(), any(EventDto.class))).thenThrow(new EventNotFoundException("Event not found" + eventId));

        // Act & Assert
        mockMvc.perform(put("/api/v1/events/{id}/update", eventId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateEvent)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createEvent_ShouldReturnEventWithStatus404() throws Exception {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        EventDto inputEvent = new EventDto(eventId, "Invalid Event", Instant.now(), Category.Concert, BigDecimal.valueOf(50.0));
        when(eventService.createEvent(any(EventDto.class))).thenThrow(new EventNotFoundException("Creation failed"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/events/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputEvent)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPopularEvents_ShouldReturnEventsWithStatus200() throws Exception {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        EventDto popularEvent = new EventDto(eventId, "Popular Exhibition", Instant.now(), Category.Exhibition, BigDecimal.valueOf(40.0));
        when(eventService.getPopularEvents()).thenReturn(List.of(popularEvent));

        // Act & Assert
        mockMvc.perform(get("/api/v1/events/popular")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(eventId))
                .andExpect(jsonPath("$[0].title").value("Popular Exhibition"));
    }

    @Test
    void getPopularEvents_ShouldReturnEventsWithStatus404() throws Exception {
        // Arrange
        when(eventService.getPopularEvents()).thenThrow(new EventNotFoundException("No popular events found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/events/popular")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getLastEvents_ShouldReturnEventsWithStatus200() throws Exception {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        EventDto lastEvent = new EventDto(eventId, "Recent Cinema", Instant.now(), Category.Cinema, BigDecimal.valueOf(25.0));
        Page<EventDto> page = new PageImpl<>(List.of(lastEvent));
        when(eventService.getLastEvents(any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/v1/events/last")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(eventId))
                .andExpect(jsonPath("$.content[0].title").value("Recent Cinema"));
    }

    @Test
    void getLastEvents_ShouldReturnEventsWithStatus404() throws Exception {
        // Arrange
        when(eventService.getLastEvents(any(Pageable.class))).thenThrow(new EventNotFoundException("No last events found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/events/last")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
