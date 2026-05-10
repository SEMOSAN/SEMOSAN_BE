package com.semosan.api.domain.community.like.repository;

import com.semosan.api.domain.community.like.entity.PostLike;
import com.semosan.api.domain.community.post.entity.Post;
import com.semosan.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    boolean existsByPostAndUser(Post post, User user);

    long countByPost(Post post);

    Optional<PostLike> findByPostAndUser(Post post, User user);
}
