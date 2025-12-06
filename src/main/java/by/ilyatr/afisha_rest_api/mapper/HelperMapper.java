package by.ilyatr.afisha_rest_api.mapper;

import by.ilyatr.afisha_rest_api.dto.CommentDto;
import by.ilyatr.afisha_rest_api.entities.Comment;
import by.ilyatr.afisha_rest_api.repositories.EventRepository;
import by.ilyatr.afisha_rest_api.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HelperMapper {
    
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    
    public void setUserAndEvent(CommentDto commentDto, Comment comment) {
        // Устанавливаем пользователя по userId
        if (commentDto.getUserId() != null) {
            userRepository.findById(commentDto.getUserId())
                .ifPresent(comment::setUser);
        }
        
        // Устанавливаем событие по eventId
        if (commentDto.getEventId() != null) {
            eventRepository.findById(commentDto.getEventId())
                .ifPresent(comment::setEvent);
        }
    }
}