package com.socialguardrail.guardrailapi.controller;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.socialguardrail.guardrailapi.Entity.Comment;
import com.socialguardrail.guardrailapi.Entity.Post;
import com.socialguardrail.guardrailapi.service.CommentService;
import com.socialguardrail.guardrailapi.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

    @PostMapping
    public Post createPost(@RequestBody Post post) {
        return postService.createPost(post);
    }

    @GetMapping
    public List<Post> getAllPosts(){
        return postService.getAllPosts();
    }

    @PostMapping("/{postId}/comments")
    public Comment addComment(@PathVariable Long postId,@RequestBody Comment comment){
        return commentService.addComment(postId,comment);
    }

    @PostMapping("/{postId}/like")
    public Post likePost(@PathVariable Long postId){
    return postService.likePost(postId);
    }

    @GetMapping("/{postId}/virality-score")
    public String getVirality(@PathVariable Long postId) {
        return commentService.getViralityScore(postId);
    }
}