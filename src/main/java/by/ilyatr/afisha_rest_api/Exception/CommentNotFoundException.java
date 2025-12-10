package by.ilyatr.afisha_rest_api.Exception;

public class CommentNotFoundException extends RuntimeException {
    public CommentNotFoundException(String id) {
        super("Could not find comment " + id);
    }
}
