package by.ilyatr.afisha_rest_api.controllers.rest;

import by.ilyatr.afisha_rest_api.dto.EventDto;
import by.ilyatr.afisha_rest_api.services.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventRestControllerV1 {
    private final EventService eventService;

    @GetMapping("{id}")
    public EventDto getEvent(@PathVariable String id) {
        return eventService.getEventById(id);
    }

    @PostMapping("create")
    public ResponseEntity<EventDto> createEvent(EventDto eventDto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(eventService.createEvent(eventDto));
    }

    @DeleteMapping("{id}/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public boolean deleteEvent(@PathVariable String id) {
        return eventService.deleteEvent(id);
    }

    @PutMapping("{id}/update")
    public EventDto updateEvent(@PathVariable String id, EventDto eventDto) {
        return eventService.updateEvent(id, eventDto);
    }

    @GetMapping("popular")
    public List<EventDto> getPopularEvents() {
        return eventService.getPopularEvents();
    }

    @GetMapping("last")
    public Page<EventDto> getLastEvents(@PageableDefault(size = 5) Pageable pageable) {
       return eventService.getLastEvents(pageable);
    }

}
