package by.ilyatr.afisha_rest_api.Exception;

public class UserNotFoundException extends RuntimeException{
    public UserNotFoundException(String id) {
        super("User not found : " + id);
    }
}
