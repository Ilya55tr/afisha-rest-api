package by.ilyatr.afisha_rest_api.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Table(name = "comments")
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class Comment {
    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    private String text;

    private Instant createdAt;
    private Instant updatedAt;

}
