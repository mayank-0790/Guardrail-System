package com.socialguardrail.guardrailapi.repository;

import com.socialguardrail.guardrailapi.Entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment,Long> {
}
