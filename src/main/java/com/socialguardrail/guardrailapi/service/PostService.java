package com.socialguardrail.guardrailapi.service;

import com.socialguardrail.guardrailapi.Entity.Post;
import com.socialguardrail.guardrailapi.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    public Post createPost(Post post) {
        return postRepository.save(post);
    }
}