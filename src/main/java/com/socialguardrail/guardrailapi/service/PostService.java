package com.socialguardrail.guardrailapi.service;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.socialguardrail.guardrailapi.Entity.Post;
import com.socialguardrail.guardrailapi.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private void viralityScore(Long postId,int points){
        String key = "post:" + postId + ":virality_score";
        redisTemplate.opsForValue().increment(key,points);
    }

    public Post createPost(Post post) {
        return postRepository.save(post);
    }

    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

   public Post likePost(Long postId){
        Post post=postRepository.findById(postId)
                .orElseThrow(()-> new RuntimeException("Post not found"));

        post.setLikes(post.getLikes()+1);

        viralityScore(postId,20);
        return postRepository.save(post);
   }



}