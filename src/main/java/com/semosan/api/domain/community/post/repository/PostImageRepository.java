package com.semosan.api.domain.community.post.repository;

import com.semosan.api.domain.community.post.entity.Post;
import com.semosan.api.domain.community.post.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    List<PostImage> findByPostOrderBySortOrderAsc(Post post);

    @Modifying
    @Query("DELETE FROM PostImage pi WHERE pi.post = :post")
    void deleteByPost(@Param("post") Post post);

    // postId, imageUrl만 직접 반환 — PostImage 엔티티로 받으면 img.getPost() 접근 시 Lazy Loading으로 건당 추가 쿼리가 발생한다.
    @Query("SELECT pi.post.id, pi.imageUrl FROM PostImage pi WHERE pi.post.id IN :postIds AND pi.main = true")
    List<Object[]> findMainImagesByPostIds(@Param("postIds") List<Long> postIds);

    @Query("SELECT pi.post.id, COUNT(pi) FROM PostImage pi WHERE pi.post.id IN :postIds GROUP BY pi.post.id")
    List<Object[]> countByPostIdsGrouped(@Param("postIds") List<Long> postIds);
}
