package by.ilyatr.afisha_rest_api.repositories;

import by.ilyatr.afisha_rest_api.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {


}
