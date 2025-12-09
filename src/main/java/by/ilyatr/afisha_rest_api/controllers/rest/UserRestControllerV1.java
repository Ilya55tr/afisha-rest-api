package by.ilyatr.afisha_rest_api.controllers.rest;

import by.ilyatr.afisha_rest_api.dto.UserDto;
import by.ilyatr.afisha_rest_api.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserRestControllerV1 {
    private final UserService userService;

    @PostMapping("create")
    public ResponseEntity<UserDto> createUser(UserDto userDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(userDto));
    }

    @PutMapping("{id}/update")
    public UserDto updateUser(@PathVariable String id, @RequestBody UserDto userDto) {
        return userService.updateUser(id, userDto);
    }

    @GetMapping("{id}")
    public UserDto getUser(@PathVariable String id) {
        return userService.getUserById(id);
    }

    @DeleteMapping("{id}/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public boolean deleteUser(@PathVariable String id) {
        return userService.deleteUser(id);
    }
}
