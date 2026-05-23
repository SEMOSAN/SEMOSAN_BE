package com.semosan.api.domain.community.post.repository;

import com.semosan.api.domain.community.post.entity.Post;
import com.semosan.api.domain.community.post.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    List<PostImage> findByPostOrderBySortOrderAsc(Post post);

    @Query("SELECT pi FROM PostImage pi WHERE pi.post.id IN :postIds AND pi.main = true")
    List<PostImage> findMainImagesByPostIds(@Param("postIds") List<Long> postIds);

    @Query("SELECT pi.post.id, COUNT(pi) FROM PostImage pi WHERE pi.post.id IN :postIds GROUP BY pi.post.id")
    List<Object[]> countByPostIdsGrouped(@Param("postIds") List<Long> postIds);
}
