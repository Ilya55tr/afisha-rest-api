package by.ilyatr.afisha_rest_api.repositories;

import by.ilyatr.afisha_rest_api.entities.Comment;
import by.ilyatr.afisha_rest_api.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, String> {

    Page<Comment> findTop100ByEventIdOrderByUpdatedAtDesc(String eventId, Pageable pageable);


    @Query("SELECT DISTINCT c FROM Comment c " +
           "LEFT JOIN FETCH c.user " +
           "LEFT JOIN FETCH c.event " +
           "WHERE c.id = :id")
    Optional<Comment> findByIdWithUserAndEvent(String id);

}
