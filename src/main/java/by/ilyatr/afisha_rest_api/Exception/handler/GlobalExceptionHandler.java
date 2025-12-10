package by.ilyatr.afisha_rest_api.Exception.handler;

import by.ilyatr.afisha_rest_api.Exception.CommentNotFoundException;
import by.ilyatr.afisha_rest_api.Exception.EventNotFoundException;
import by.ilyatr.afisha_rest_api.Exception.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleUserNotFoundException(UserNotFoundException ex){
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(EventNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleEventNotFoundException(EventNotFoundException ex){
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(CommentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleCommentNotFoundException(CommentNotFoundException ex){
        return Map.of("error", ex.getMessage());
    }
}
