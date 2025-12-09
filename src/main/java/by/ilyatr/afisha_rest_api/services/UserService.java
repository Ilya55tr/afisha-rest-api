package by.ilyatr.afisha_rest_api.services;

import by.ilyatr.afisha_rest_api.Exception.UserNotFoundException;
import by.ilyatr.afisha_rest_api.dto.UserDto;
import by.ilyatr.afisha_rest_api.entities.User;
import by.ilyatr.afisha_rest_api.mapper.UserMapper;
import by.ilyatr.afisha_rest_api.repositories.UserRepository;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserDto createUser(UserDto userDto) {
        String userId = UUID.randomUUID().toString();
        userDto.setId(userId);
        userDto.setCreatedAt(Instant.now());
        User user = userMapper.toUser(userDto);
        userRepository.save(user);
        log.info("User with id {} created", userId);
        return userRepository
                .findById(userId)
                .map(userMapper::toUserDto)
                .orElseThrow(()-> new UserNotFoundException(userId));
    }


    public UserDto getUserById(String id) {
        return userRepository
                .findById(id)
                .map(userMapper::toUserDto)
                .orElseThrow(()-> new UserNotFoundException(id));
    }

    @Transactional
    public UserDto updateUser(String id, UserDto userDto) {
        return userRepository.findById(id).map(user -> {
            userMapper.updateUser(userDto, user);
            userRepository.save(user);
            return userMapper.toUserDto(user);
        }).orElseThrow(()-> new UserNotFoundException(id));
    }

    @Transactional
    public boolean deleteUser(String id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            log.info("User with id {} deleted", id);
            return true;
        } else{

            return false;
        }

    }

}
