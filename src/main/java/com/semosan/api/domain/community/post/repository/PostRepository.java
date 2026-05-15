package com.semosan.api.domain.community.post.repository;

import com.semosan.api.domain.community.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    Optional<Post> findByIdAndDeletedFalse(Long id);
}
