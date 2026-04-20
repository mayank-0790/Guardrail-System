package com.socialguardrail.guardrailapi.repository;

import com.socialguardrail.guardrailapi.Entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}
