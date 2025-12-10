package by.ilyatr.afisha_rest_api.services;

import by.ilyatr.afisha_rest_api.Exception.CommentNotFoundException;
import by.ilyatr.afisha_rest_api.dto.CommentDto;
import by.ilyatr.afisha_rest_api.entities.Comment;
import by.ilyatr.afisha_rest_api.mapper.CommentMapper;
import by.ilyatr.afisha_rest_api.mapper.HelperMapper;
import by.ilyatr.afisha_rest_api.repositories.CommentRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Helper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CommentService {
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final HelperMapper helperMapper;
    private final CacheManager cacheManager;

    private final String COMMENTS_CACHE = "comments";
    private String redisKey(String eventId){
        return "event:"+eventId+":comments";
    }


    @Transactional
    public CommentDto createComment(CommentDto commentDto) {
        String commentId = UUID.randomUUID().toString();
        commentDto.setId(commentId);
        commentDto.setUpdatedAt(Instant.now());
        commentDto.setCreatedAt(Instant.now());
        Comment comment = commentMapper.toComment(commentDto);
        helperMapper.setUserAndEvent(commentDto, comment);
        commentRepository.save(comment);

        log.info("Comment with id {} created", commentId);

        redisTemplate.opsForList().leftPush(redisKey(comment.getEvent().getId()), commentId);
        log.info("id {} of comment added to cache", commentId);
        return commentMapper.toCommentDto(comment);
    }

    public CommentDto getComment(String id) {
        Cache cache = cacheManager.getCache(COMMENTS_CACHE);
        if (cache.get(id, CommentDto.class) == null ) {
            log.info("Getting comment with id {} from Db", id);
            CommentDto comment = commentRepository
                    .findByIdWithUserAndEvent(id)
                    .map(commentMapper::toCommentDto)
                    .orElseThrow(()-> new CommentNotFoundException(id));
            putToCache(comment);
            return comment;
        }
        return cache.get(id, CommentDto.class);
    }

    @Transactional
    @CacheEvict(cacheNames = COMMENTS_CACHE, key = "#id")
    public boolean deleteComment(String id) {
        if (commentRepository.existsById(id)) {
            CommentDto oldComment = commentRepository
                    .findByIdWithUserAndEvent(id)
                    .map(commentMapper::toCommentDto)
                    .orElseThrow(() -> new CommentNotFoundException(id));
            commentRepository.deleteById(id);
            redisTemplate.opsForList().remove(redisKey(oldComment.getEventId()), 1,
                    oldComment);
            log.info("Comment {} deleted from DB and cache", id);
            return true;
        } else {
            log.error("Comment with id {} not deleted", id);
            return false;
        }
    }

    @Transactional
    @CacheEvict(cacheNames = COMMENTS_CACHE, key = "#id")
    public CommentDto updateComment(String id, CommentDto commentDto){
        log.info("Comment with id {} updating...", id);
        commentDto.setUpdatedAt(Instant.now());
        Comment comment =commentMapper
                .updateComment(commentDto, commentRepository
                .findByIdWithUserAndEvent(id).orElseThrow(()-> new CommentNotFoundException(id)));
        commentRepository.save(comment);
        log.info("Comment {} updated in DB", id);
        return commentMapper.toCommentDto(comment);
    }

    public Page<CommentDto> getComments(String eventId, Pageable pageable) {
        if (pageable.getPageNumber() ==0){
            Page<CommentDto> cached = getCommentsFirstPageFromCache(eventId, pageable);
            if (cached != null) {
                return cached;
            }
        }
        return getCommentsFromDb(eventId, pageable);
    }

    private Page<CommentDto> getCommentsFirstPageFromCache(String eventId,
                                                           Pageable pageable) {

        var ids= redisTemplate.opsForList().range(redisKey(eventId), 0, pageable.getPageSize() - 1);
        if (ids==null || ids.isEmpty()) {
            return null;
        }
        log.info("got comments for Event with id {} from cache", eventId);
        redisTemplate.opsForList().trim(redisKey(eventId), 0, pageable.getPageSize() - 1);
        log.info("Cache for comments was trimmed");
        List<CommentDto> comments = ids
                .stream()
                .map(id -> getComment(id.toString()))
                .toList();

        long total = commentRepository.count();
        return new PageImpl<>(comments, pageable, total);
    }

    private Page<CommentDto> getCommentsFromDb(String eventId, Pageable pageable) {
        Page<Comment> page = commentRepository
                .findTop100ByEventIdOrderByUpdatedAtDesc(eventId, pageable);
        log.info("comments was got for Event with id {} from db", eventId);
        
        List<CommentDto> comments = page
                .getContent()
                .stream()
                .map(commentMapper::toCommentDto)
                .toList();

        String redisKey = redisKey(eventId);
        for (int i = 0; i < pageable.getPageSize(); i++) {
            if (i >= comments.size()) break;
            redisTemplate.opsForList().leftPush(redisKey, comments.get(i).getId());
        }

        return new PageImpl<>(comments, pageable, page.getTotalElements());
    }

    private void putToCache(CommentDto commentDto){
       Cache cache= cacheManager.getCache(COMMENTS_CACHE);
       cache.put(commentDto.getId(), commentDto);
    }


}
