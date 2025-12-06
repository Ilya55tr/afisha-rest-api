package by.ilyatr.afisha_rest_api.dto;

import by.ilyatr.afisha_rest_api.entities.Category;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
public class EventDto{
    private String id;
    private String title;
    private Instant date;
    private Category category;
    private BigDecimal price;
}
