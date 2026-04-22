package com.socialguardrail.guardrailapi.service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import com.socialguardrail.guardrailapi.repository.PostRepository;

import com.socialguardrail.guardrailapi.Entity.Comment;
import com.socialguardrail.guardrailapi.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private NotificationService notification;

    public void viralityScore(Long postId, int points) {
        String key = "post:" + postId + ":virality_score";

        redisTemplate.opsForValue().increment(key, points);
    }

    private boolean isCooldown(Long botId, Long userId) {
        String key = "cooldown:bot_" + botId + ":user_" + userId;
        return redisTemplate.hasKey(key);
    }

    private void setCooldown(Long botId, Long userId) {
        String key = "cooldown:bot_" + botId + ":user_" + userId;

        redisTemplate.opsForValue().set(
                key,
                "1",
                Duration.ofMinutes(10)
        );
    }





    @Autowired
    private PostRepository postRepository;

    public Comment addComment(Long postId, Comment comment) {

        comment.setPostId(postId);


        if (comment.getDepthLevel() > 20) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Max depth exceeded"
            );
        }

        if ("BOT".equals(comment.getAuthorType())) {

            Long botId = comment.getAuthorId();

            Long userId = postRepository.findById(postId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Post not found"
                    ))
                    .getAuthorId();
            if (isCooldown(botId, userId)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Cooldown active"
                );
            }

            String key = "post:" + postId + ":bot_reply_count";
            Long count=redisTemplate.opsForValue().increment(key);

            if(count > 100){
                redisTemplate.opsForValue().decrement(key);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Bot limit reached");
            }

            setCooldown(botId, userId);

            viralityScore(postId, 1);

            notification.handleNotification(userId,"Bot"+botId+"replied to your post");

        } else {
            viralityScore(postId, 50);
        }

        return commentRepository.save(comment);
    }

    public String getViralityScore(Long postId) {
        String key = "post:" + postId + ":virality_score";
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? value : "0";
    }


}