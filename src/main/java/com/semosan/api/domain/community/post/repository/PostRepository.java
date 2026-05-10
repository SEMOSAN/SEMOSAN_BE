package com.semosan.api.domain.community.post.repository;

import com.semosan.api.domain.community.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}
