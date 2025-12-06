package by.ilyatr.afisha_rest_api.repositories;

import by.ilyatr.afisha_rest_api.entities.Event;
import by.ilyatr.afisha_rest_api.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, String> {

    Page<Event> findAllByOrderByDateDesc(Pageable pageable);

    List<Event> findTop10ByOrderByDateDesc();
}
