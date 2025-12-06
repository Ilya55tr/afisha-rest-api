package by.ilyatr.afisha_rest_api.dto;

import by.ilyatr.afisha_rest_api.entities.Comment;
import by.ilyatr.afisha_rest_api.entities.Event;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
public class UserDto{
    private String id;
    private String name;
    private String email;
    private String password;
    private Instant createdAt;
}
