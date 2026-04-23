package com.socialguardrail.guardrailapi.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.socialguardrail.guardrailapi.repository.PostRepository;
import com.socialguardrail.guardrailapi.Entity.Comment;
import com.socialguardrail.guardrailapi.repository.CommentRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private NotificationService notificationService;


    public void viralityScore(Long postId, int points) {
        String key = "post:" + postId + ":virality_score";
        redisTemplate.opsForValue().increment(key, points);
    }


    private boolean isCooldown(Long botId, Long userId) {
        String key = "cooldown:bot_" + botId + ":user_" + userId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private void setCooldown(Long botId, Long userId) {
        String key = "cooldown:bot_" + botId + ":user_" + userId;
        redisTemplate.opsForValue().set(key, "1", Duration.ofMinutes(10));
    }


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

            String script =
                    "local current = redis.call('GET', KEYS[1]) " +
                            "if not current then current = 0 else current = tonumber(current) end " +
                            "if current >= 100 then return -1 end " +
                            "return redis.call('INCR', KEYS[1])";

            Long result = redisTemplate.execute(
                    new DefaultRedisScript<>(script, Long.class),
                    Collections.singletonList(key)
            );

            if (result == null || result == -1) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Bot limit reached"
                );
            }
          setCooldown(botId, userId);
            viralityScore(postId, 1);

           notificationService.handleNotification(userId, "Bot " + botId + " replied to your post");
            return commentRepository.save(comment);
        }


        viralityScore(postId, 50);
        return commentRepository.save(comment);
    }


    public String getViralityScore(Long postId) {
        String key = "post:" + postId + ":virality_score";
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? value : "0";
    }
}