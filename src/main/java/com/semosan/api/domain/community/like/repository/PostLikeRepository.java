package com.semosan.api.domain.community.like.repository;

import com.semosan.api.domain.community.like.entity.PostLike;
import com.semosan.api.domain.community.post.entity.Post;
import com.semosan.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    boolean existsByPostAndUser(Post post, User user);

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    long countByPost(Post post);

    Optional<PostLike> findByPostAndUser(Post post, User user);

    /**
     * 유니크 제약(post_id, user_id) 충돌 시 예외 없이 무시한다. 동시 좋아요 요청에서
     * DataIntegrityViolationException을 유발하지 않기 위한 upsert.
     * @return 실제로 insert된 row 수 (0 = 이미 좋아요 존재)
     */
    @Modifying
    @Query(value = "INSERT INTO post_likes (post_id, user_id, created_at, updated_at) "
            + "VALUES (:postId, :userId, now(), now()) "
            + "ON CONFLICT (post_id, user_id) DO NOTHING", nativeQuery = true)
    int insertIgnoreConflict(@Param("postId") Long postId, @Param("userId") Long userId);

    @Query("SELECT pl.post.id, COUNT(pl) FROM PostLike pl WHERE pl.post.id IN :postIds GROUP BY pl.post.id")
    List<Object[]> countByPostIdsGrouped(@Param("postIds") List<Long> postIds);
}
