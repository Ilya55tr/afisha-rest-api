package by.ilyatr.afisha_rest_api.controllers.rest;

import by.ilyatr.afisha_rest_api.dto.CommentDto;
import by.ilyatr.afisha_rest_api.entities.Comment;
import by.ilyatr.afisha_rest_api.services.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentRestControllerV1 {
    private final CommentService commentService;

    @GetMapping("{id}")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto getComment(@PathVariable String id){
        return commentService.getComment(id);
    }

    @PostMapping("create")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto createComment(CommentDto commentDto){
        return commentService.createComment(commentDto);
    }

    @PutMapping("{id}/update")
    @ResponseStatus(HttpStatus.OK)
    public CommentDto updateComment(@PathVariable String id, CommentDto commentDto){
        return commentService.updateComment(id, commentDto);
    }

    @DeleteMapping("{id}/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public boolean deleteComment(@PathVariable String id){
        return commentService.deleteComment(id);
    }

    @GetMapping("event/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Page<CommentDto> getCommentsByEventId(@PathVariable String id, @PageableDefault(size = 5) Pageable pageable){
        return commentService.getComments(id, pageable);
    }

}
