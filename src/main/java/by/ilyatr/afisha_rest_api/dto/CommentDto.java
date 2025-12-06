package by.ilyatr.afisha_rest_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class CommentDto{
    private String id;
    private String userId;
    private String eventId;
    private String text;
    private Instant createdAt;
    private Instant updatedAt;
}
