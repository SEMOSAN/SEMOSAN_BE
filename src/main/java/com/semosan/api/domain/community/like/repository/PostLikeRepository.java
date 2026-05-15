package com.semosan.api.domain.community.like.repository;

import com.semosan.api.domain.community.like.entity.PostLike;
import com.semosan.api.domain.community.post.entity.Post;
import com.semosan.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    boolean existsByPostAndUser(Post post, User user);

    long countByPost(Post post);

    Optional<PostLike> findByPostAndUser(Post post, User user);

    @Query("SELECT pl.post.id, COUNT(pl) FROM PostLike pl WHERE pl.post.id IN :postIds GROUP BY pl.post.id")
    List<Object[]> countByPostIdsGrouped(@Param("postIds") List<Long> postIds);
}
