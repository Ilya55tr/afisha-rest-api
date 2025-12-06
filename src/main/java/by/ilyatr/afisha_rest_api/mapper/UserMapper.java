package by.ilyatr.afisha_rest_api.mapper;

import by.ilyatr.afisha_rest_api.dto.UserDto;
import by.ilyatr.afisha_rest_api.entities.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toUserDto(User user);

    User toUser(UserDto userDto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    User updateUser(UserDto userDto, @MappingTarget User user);

}
