package by.ilyatr.afisha_rest_api.entities;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@NoArgsConstructor
@AllArgsConstructor
@Entity
@Setter
@Getter
@Builder
@Table(name = "events")
public class Event {
    @Id
    private String id;
    private String title;
    private Instant date;
    @Enumerated(EnumType.STRING)
    private Category category;
    private BigDecimal price;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

}
