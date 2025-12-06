package by.ilyatr.afisha_rest_api.mapper;

import by.ilyatr.afisha_rest_api.dto.EventDto;
import by.ilyatr.afisha_rest_api.entities.Event;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface EventMapper {

    EventDto toEventDto(Event event);

    Event toEvent(EventDto eventDto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Event updateEvent(EventDto eventDto, @MappingTarget Event event);
}
