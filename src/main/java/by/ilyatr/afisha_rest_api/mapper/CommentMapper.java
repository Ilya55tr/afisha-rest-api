package by.ilyatr.afisha_rest_api.mapper;

import by.ilyatr.afisha_rest_api.dto.CommentDto;
import by.ilyatr.afisha_rest_api.entities.Comment;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {UserMapper.class, EventMapper.class})
public interface CommentMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "event.id", target = "eventId")
    CommentDto toCommentDto(Comment comment);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "event", ignore = true)
    Comment toComment(CommentDto commentDto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "event", ignore = true)
    Comment updateComment(CommentDto commentDto, @MappingTarget Comment comment);
}
